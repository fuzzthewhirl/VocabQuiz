package com.example.vocabquiz.ui

import android.app.Application
import android.app.LocaleManager
import android.os.LocaleList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vocabquiz.data.FolderNotFoundException
import com.example.vocabquiz.data.LoadIssue
import com.example.vocabquiz.data.LoadIssueResolver
import com.example.vocabquiz.data.LoadReport
import com.example.vocabquiz.data.SettingsStore
import com.example.vocabquiz.data.SpreadsheetInfo
import android.util.Log
import com.example.vocabquiz.data.VocabRepository
import com.example.vocabquiz.model.Direction
import com.example.vocabquiz.model.LanguageCatalog
import com.example.vocabquiz.model.LanguagePair
import com.example.vocabquiz.model.Vocab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.math.max

data class QuizState(
    val status: Status = Status.Loading,
    val initPhase: InitPhase = InitPhase.Starting,
    val sourceLang: String? = null,
    val targetLang: String? = null,
    val direction: Direction = Direction.SRC_TO_TGT,

    val pageOffset: Int = 0,
    val pageSize: Int = 10, // <= 10-word chunks

    val pool: List<Vocab> = emptyList(),
    val availablePairs: List<LanguagePair> = emptyList(),
    val index: Int = 0,
    val promptText: String = "",
    val answerText: String = "",
    val revealed: Boolean = false,
    val loadReport: LoadReport? = null,
    val loadIssue: LoadIssue? = null
) {
    enum class Status { Loading, Ready, Error }
    enum class InitPhase {
        Starting,
        ReadingSettings,
        LoadingSheets,
        LoadingData,
        ChoosingPair,
        LoadingChunk,
        Ready,
        Error
    }
}

data class SettingsUiState(
    val spreadsheetId: String,
    val spreadsheets: List<SpreadsheetInfo> = emptyList(),
    val sheetNames: List<String> = emptyList(),
    val selectedSheet: String? = null,
    val uiLanguageTag: String? = null,
    val loadingSpreadsheets: Boolean = false,
    val loadingSheets: Boolean = false,
    val error: SettingsError? = null,
    val lastErrorMessage: String? = null,
    val lastErrorAt: String? = null
)

enum class SettingsError {
    MissingSpreadsheetId,
    FetchFailed,
    NoSheets,
    SheetMissing,
    DataLoadFailed,
    FolderMissing,
    FolderEmpty,
    DriveFetchFailed
}

internal data class SheetSelection(
    val selected: String?,
    val missing: Boolean
)

internal fun selectSheetForStartup(saved: String?, names: List<String>): SheetSelection {
    if (names.isEmpty()) return SheetSelection(null, missing = false)
    if (saved == null) return SheetSelection(names.first(), missing = false)
    if (names.contains(saved)) return SheetSelection(saved, missing = false)
    return SheetSelection(null, missing = true)
}

internal fun selectSheetForRefresh(current: String?, names: List<String>): SheetSelection {
    if (names.isEmpty()) return SheetSelection(null, missing = false)
    if (current != null && names.contains(current)) return SheetSelection(current, missing = false)
    return SheetSelection(null, missing = current != null)
}

class QuizViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)

    private val repo = VocabRepository(app)

    private val _state = MutableStateFlow(QuizState())
    val state = _state.asStateFlow()

    private val _settingsState = MutableStateFlow(
        SettingsUiState(spreadsheetId = DEFAULT_SPREADSHEET_ID)
    )
    val settingsState = _settingsState.asStateFlow()

    // single-flight gate to avoid overlapping loads
    private var loading = false

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(initPhase = QuizState.InitPhase.ReadingSettings)
            val snap = withTimeoutOrNull(1500) { settings.snapshot.first() } ?: SettingsStore.Snapshot()

            val spreadsheetId = snap.spreadsheetId?.ifBlank { null } ?: DEFAULT_SPREADSHEET_ID
            val savedSheetTab = snap.sheetTab?.ifBlank { null }
            val uiLanguageTag = snap.uiLanguageTag?.ifBlank { null }

            applyUiLanguage(uiLanguageTag)

            _settingsState.value = _settingsState.value.copy(
                spreadsheetId = spreadsheetId,
                selectedSheet = savedSheetTab,
                uiLanguageTag = uiLanguageTag
            )

            _state.value = _state.value.copy(initPhase = QuizState.InitPhase.LoadingSheets)
            val result = repo.getSheetNames(spreadsheetId)
            val names = result.getOrNull().orEmpty()
            if (result.isFailure) {
                recordSettingsError(SettingsError.FetchFailed, result.exceptionOrNull()?.message)
                _state.value = _state.value.copy(
                    status = QuizState.Status.Error,
                    initPhase = QuizState.InitPhase.Error
                )
                return@launch
            }
            if (names.isEmpty()) {
                recordSettingsError(SettingsError.NoSheets, null)
                _state.value = _state.value.copy(
                    status = QuizState.Status.Error,
                    initPhase = QuizState.InitPhase.Error
                )
                return@launch
            }

            val selection = selectSheetForStartup(savedSheetTab, names)
            _settingsState.value = _settingsState.value.copy(
                sheetNames = names,
                selectedSheet = selection.selected
            )

            if (selection.selected == null) {
                if (selection.missing) {
                    recordSettingsError(SettingsError.SheetMissing, "Saved sheet not found")
                }
                _state.value = _state.value.copy(
                    status = QuizState.Status.Error,
                    initPhase = QuizState.InitPhase.Error
                )
                return@launch
            }

            if (savedSheetTab == null) {
                settings.saveSheetTab(selection.selected)
            }

            val sheetTab = selection.selected

            _state.value = _state.value.copy(initPhase = QuizState.InitPhase.LoadingData)
            val outcome = repo.loadAll(spreadsheetId, sheetTab)
            if (outcome.isFailure) {
                recordSettingsError(SettingsError.DataLoadFailed, outcome.exceptionOrNull()?.message)
                _state.value = _state.value.copy(
                    status = QuizState.Status.Error,
                    initPhase = QuizState.InitPhase.Error
                )
                return@launch
            }

            val report = outcome.getOrNull()?.report
            val issue = report?.let { LoadIssueResolver.resolve(it) }
            if (report != null) {
                _state.value = _state.value.copy(loadReport = report, loadIssue = issue)
            }
            if (issue == LoadIssue.NoValidRows) {
                recordSettingsError(SettingsError.DataLoadFailed, "No valid vocab rows loaded")
                _state.value = _state.value.copy(
                    status = QuizState.Status.Error,
                    initPhase = QuizState.InitPhase.Error,
                    loadIssue = issue
                )
                return@launch
            }

            val availablePairs = repo.availablePairs()
            if (availablePairs.isEmpty()) {
                val noPairsIssue = issue ?: LoadIssue.NoPairs
                recordSettingsError(SettingsError.DataLoadFailed, "No language pairs available")
                _state.value = _state.value.copy(
                    status = QuizState.Status.Error,
                    initPhase = QuizState.InitPhase.Error,
                    loadIssue = noPairsIssue
                )
                return@launch
            }

            _state.value = _state.value.copy(initPhase = QuizState.InitPhase.ChoosingPair)
            val startSrc = LanguageCatalog.normalize(snap.src)
            val startTgt = LanguageCatalog.normalize(snap.tgt)
            val desiredPair = if (startSrc != null && startTgt != null) {
                LanguagePair(startSrc, startTgt)
            } else {
                null
            }
            val pair = if (desiredPair != null && availablePairs.contains(desiredPair)) {
                desiredPair
            } else {
                availablePairs.first()
            }

            _state.value = _state.value.copy(
                sourceLang = pair.src,
                targetLang = pair.tgt,
                availablePairs = availablePairs
            )

            val total = repo.pairSize(pair)
            val size  = _state.value.pageSize
            val hasResume = snap.offset in 0 until total
            val maxStart = max(0, total - size)
            val startOffset = if (hasResume) {
                (snap.offset / size) * size
            } else {
                if (total <= 0) 0 else Random.nextInt(0, maxStart + 1)
            }

            settings.savePair(pair.src, pair.tgt)
            settings.saveOffset(startOffset)
            if (!hasResume) settings.saveIndex(0)

            _state.value = _state.value.copy(initPhase = QuizState.InitPhase.LoadingChunk)
            val desiredIndex = if (hasResume) snap.index else 0
            loadChunkFor(pair, _state.value.direction, startOffset, desiredIndex)
        }
    }

    fun setLangs(src: String, tgt: String) {
        if (src == tgt) return
        val pair = LanguagePair(src, tgt)
        if (!_state.value.availablePairs.contains(pair)) return

        viewModelScope.launch {
            // randomize a fresh page whenever pair changes
            val total = repo.pairSize(pair)
            val size  = _state.value.pageSize
            val maxStart = max(0, total - size)
            val randomOffset = if (total <= 0) 0 else Random.nextInt(0, maxStart + 1)

            settings.savePair(src, tgt)
            settings.saveOffset(randomOffset)
            settings.saveIndex(0)

            _state.value = _state.value.copy(sourceLang = src, targetLang = tgt, revealed = false)
            loadChunkFor(pair, _state.value.direction, randomOffset, desiredIndex = 0)
        }
    }

    fun selectSpreadsheet(info: SpreadsheetInfo) {
        Log.d("QuizVM", "Selected spreadsheet id=${info.id}")
        _settingsState.value = _settingsState.value.copy(
            spreadsheetId = info.id,
            spreadsheets = _settingsState.value.spreadsheets,
            sheetNames = emptyList(),
            selectedSheet = null,
            error = null
        )
        refreshSheetNames()
    }

    fun selectSheet(sheet: String) {
        _settingsState.value = _settingsState.value.copy(selectedSheet = sheet)
    }

    fun setUiLanguage(tag: String?) {
        _settingsState.value = _settingsState.value.copy(uiLanguageTag = tag)
        applyUiLanguage(tag)
        viewModelScope.launch { settings.saveUiLanguageTag(tag) }
    }

    fun refreshSpreadsheets() {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(
                loadingSpreadsheets = true,
                error = null
            )

            val result = repo.getSpreadsheetsInFolder(FOLDER_NAME)
            val spreadsheets = result.getOrNull().orEmpty()
            if (result.isFailure) {
                val error = if (result.exceptionOrNull() is FolderNotFoundException) {
                    SettingsError.FolderMissing
                } else {
                    SettingsError.DriveFetchFailed
                }
                recordSettingsError(error, result.exceptionOrNull()?.message)
                _settingsState.value = _settingsState.value.copy(
                    loadingSpreadsheets = false,
                    spreadsheets = emptyList(),
                    sheetNames = emptyList(),
                    selectedSheet = null
                )
                return@launch
            }

            if (spreadsheets.isEmpty()) {
                recordSettingsError(SettingsError.FolderEmpty, null)
                _settingsState.value = _settingsState.value.copy(
                    loadingSpreadsheets = false,
                    spreadsheets = emptyList(),
                    sheetNames = emptyList(),
                    selectedSheet = null
                )
                return@launch
            }

            val currentId = _settingsState.value.spreadsheetId
            val selected = spreadsheets.firstOrNull { it.id == currentId } ?: spreadsheets.first()

            Log.d("QuizVM", "Loaded spreadsheets, using id=${selected.id}")

            _settingsState.value = _settingsState.value.copy(
                loadingSpreadsheets = false,
                spreadsheets = spreadsheets,
                spreadsheetId = selected.id,
                sheetNames = emptyList(),
                selectedSheet = null,
                error = null
            )

            refreshSheetNames()
        }
    }

    fun refreshSheetNames() {
        val spreadsheetId = _settingsState.value.spreadsheetId.trim()
        if (spreadsheetId.isBlank()) {
            recordSettingsError(SettingsError.MissingSpreadsheetId, null)
            return
        }

        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(loadingSheets = true, error = null)
            val result = repo.getSheetNames(spreadsheetId)
            val names = result.getOrNull().orEmpty()
            if (result.isFailure) {
                recordSettingsError(SettingsError.FetchFailed, result.exceptionOrNull()?.message)
                _settingsState.value = _settingsState.value.copy(
                    loadingSheets = false,
                    sheetNames = emptyList(),
                    selectedSheet = null
                )
                return@launch
            }
            if (names.isEmpty()) {
                recordSettingsError(SettingsError.NoSheets, null)
                _settingsState.value = _settingsState.value.copy(
                    loadingSheets = false,
                    sheetNames = emptyList(),
                    selectedSheet = null
                )
                return@launch
            }

            val current = _settingsState.value.selectedSheet
            val selection = selectSheetForRefresh(current, names)
            if (selection.missing) {
                recordSettingsError(SettingsError.SheetMissing, "Saved sheet not found")
            }
            _settingsState.value = _settingsState.value.copy(
                loadingSheets = false,
                sheetNames = names,
                selectedSheet = selection.selected,
                error = if (selection.missing) SettingsError.SheetMissing else null,
                lastErrorMessage = _settingsState.value.lastErrorMessage,
                lastErrorAt = _settingsState.value.lastErrorAt
            )
        }
    }

    fun ensureSpreadsheetsLoaded() {
        val current = _settingsState.value
        if (current.spreadsheets.isEmpty() && !current.loadingSpreadsheets) {
            refreshSpreadsheets()
        }
    }

    fun saveSettingsAndReload() {
        val current = _settingsState.value
        val spreadsheetId = current.spreadsheetId.trim()
        val sheetTab = current.selectedSheet
        if (spreadsheetId.isBlank() || sheetTab == null) return
        if (!current.sheetNames.contains(sheetTab)) return

        viewModelScope.launch {
            settings.saveSpreadsheetId(spreadsheetId)
            settings.saveSheetTab(sheetTab)
            reloadData(spreadsheetId, sheetTab)
        }
    }

    private fun recordSettingsError(error: SettingsError, detail: String?) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        _settingsState.value = _settingsState.value.copy(
            error = error,
            lastErrorMessage = detail,
            lastErrorAt = timestamp
        )
    }

    private fun applyUiLanguage(tag: String?) {
        val manager = getApplication<Application>().getSystemService(LocaleManager::class.java)
        if (manager != null) {
            val locales = if (tag.isNullOrBlank()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(tag)
            }
            manager.applicationLocales = locales
        }
    }

    private suspend fun reloadData(spreadsheetId: String, sheetTab: String) {
        _state.value = _state.value.copy(
            status = QuizState.Status.Loading,
            initPhase = QuizState.InitPhase.LoadingData,
            pool = emptyList(),
            index = 0,
            revealed = false,
            loadReport = null,
            loadIssue = null
        )

        val outcome = repo.loadAll(spreadsheetId, sheetTab)
        if (outcome.isFailure) {
            recordSettingsError(SettingsError.DataLoadFailed, outcome.exceptionOrNull()?.message)
            _state.value = _state.value.copy(
                status = QuizState.Status.Error,
                initPhase = QuizState.InitPhase.Error
            )
            return
        }

        val report = outcome.getOrNull()?.report
        val issue = report?.let { LoadIssueResolver.resolve(it) }
        if (report != null) {
            _state.value = _state.value.copy(loadReport = report, loadIssue = issue)
        }
        if (issue == LoadIssue.NoValidRows) {
            recordSettingsError(SettingsError.DataLoadFailed, "No valid vocab rows loaded")
            _state.value = _state.value.copy(
                status = QuizState.Status.Error,
                initPhase = QuizState.InitPhase.Error,
                loadIssue = issue
            )
            return
        }

        val availablePairs = repo.availablePairs()
        if (availablePairs.isEmpty()) {
            val noPairsIssue = issue ?: LoadIssue.NoPairs
            recordSettingsError(SettingsError.DataLoadFailed, "No language pairs available")
            _state.value = _state.value.copy(
                status = QuizState.Status.Error,
                initPhase = QuizState.InitPhase.Error,
                loadIssue = noPairsIssue
            )
            return
        }

        val snap = withTimeoutOrNull(1500) { settings.snapshot.first() } ?: SettingsStore.Snapshot()

        _state.value = _state.value.copy(initPhase = QuizState.InitPhase.ChoosingPair)
        val startSrc = LanguageCatalog.normalize(snap.src)
        val startTgt = LanguageCatalog.normalize(snap.tgt)
        val desiredPair = if (startSrc != null && startTgt != null) {
            LanguagePair(startSrc, startTgt)
        } else {
            null
        }
        val pair = if (desiredPair != null && availablePairs.contains(desiredPair)) {
            desiredPair
        } else {
            availablePairs.first()
        }

        _state.value = _state.value.copy(
            sourceLang = pair.src,
            targetLang = pair.tgt,
            availablePairs = availablePairs
        )

        val total = repo.pairSize(pair)
        val size  = _state.value.pageSize
        val hasResume = snap.offset in 0 until total
        val maxStart = max(0, total - size)
        val startOffset = if (hasResume) {
            (snap.offset / size) * size
        } else {
            if (total <= 0) 0 else Random.nextInt(0, maxStart + 1)
        }

        settings.savePair(pair.src, pair.tgt)
        settings.saveOffset(startOffset)
        if (!hasResume) settings.saveIndex(0)

        _state.value = _state.value.copy(initPhase = QuizState.InitPhase.LoadingChunk)
        val desiredIndex = if (hasResume) snap.index else 0
        loadChunkFor(pair, _state.value.direction, startOffset, desiredIndex)
    }

    fun nextPage() {
        val s = _state.value
        val pair = currentPair() ?: return
        val total = repo.pairSize(pair)
        if (total == 0) return

        val newOffset = s.pageOffset + s.pageSize
        if (newOffset >= total) {
            Log.d("QuizVM", "No next chunk for $pair (total=$total)")
            return
        }

        viewModelScope.launch { settings.saveOffset(newOffset); settings.saveIndex(0) }
        loadChunkFor(pair, s.direction, newOffset, desiredIndex = 0)
    }

    fun prevPage() {
        val s = _state.value
        val pair = currentPair() ?: return
        val newOffset = (s.pageOffset - s.pageSize).coerceAtLeast(0)
        if (newOffset == s.pageOffset) return

        viewModelScope.launch { settings.saveOffset(newOffset); settings.saveIndex(0) }
        loadChunkFor(pair, s.direction, newOffset, desiredIndex = 0)
    }

    fun prevCard() {
        val s = _state.value
        if (s.pool.isEmpty()) return
        val prev = (s.index - 1 + s.pool.size).mod(s.pool.size)
        setCard(prev, reveal = false)
        viewModelScope.launch { settings.saveIndex(prev) }
    }

    fun nextCard() {
        val s = _state.value
        if (s.pool.isEmpty()) return
        val next = (s.index + 1).mod(s.pool.size)
        setCard(next, reveal = false)
        viewModelScope.launch { settings.saveIndex(next) }
    }

    fun toggleReveal() {
        _state.value = _state.value.copy(revealed = !_state.value.revealed)
    }

    private fun currentPair(): LanguagePair? {
        val s = _state.value
        val src = s.sourceLang ?: return null
        val tgt = s.targetLang ?: return null
        return LanguagePair(src, tgt)
    }

    // now accepts desiredIndex (will be clamped to chunk size)
    private fun loadChunkFor(
        pair: LanguagePair,
        direction: Direction,
        offset: Int,
        desiredIndex: Int = 0
    ) {
        if (loading) return
        loading = true
        viewModelScope.launch {
            try {
                val size = _state.value.pageSize
                val chunk = repo.getChunk(pair, offset, size)
                if (chunk.isEmpty()) {
                Log.d("QuizVM", "Empty chunk at offset=$offset for $pair")
                    recordSettingsError(
                        SettingsError.DataLoadFailed,
                        "No data for pair ${pair.src}-${pair.tgt}"
                    )
                    _state.value = _state.value.copy(
                        status = QuizState.Status.Error,
                        initPhase = QuizState.InitPhase.Error
                    )
                    return@launch
                }
                Log.d("QuizVM", "Chunk with direction=$direction for $pair")
                _state.value = _state.value.copy(
                    status = QuizState.Status.Ready,
                    initPhase = QuizState.InitPhase.Ready,
                    pageOffset = offset,
                    pool = chunk,
                    index = 0,
                    revealed = false
                )
                val idx = desiredIndex.coerceIn(0, chunk.size - 1)
                setCard(idx, reveal = false)
            } finally {
                loading = false
            }
        }
    }

    private fun setCard(i: Int, reveal: Boolean) {
        val s = _state.value
        val v = s.pool.getOrNull(i) ?: return
        val (prompt, answer) = when (s.direction) {
            Direction.SRC_TO_TGT -> v.source to v.target
            Direction.TGT_TO_SRC -> v.target to v.source
        }
        _state.value = s.copy(index = i, promptText = prompt, answerText = answer, revealed = reveal)
    }

    companion object {
        private const val DEFAULT_SPREADSHEET_ID = "1HI8QRSYkGNsXvyO2Grx3o1wFe6Q9uscyfAO31Xe50QQ"
        private const val FOLDER_NAME = "VocabQuiz"
    }
}
