@file:Suppress("DEPRECATION")

package com.example.vocabquiz

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vocabquiz.data.SpreadsheetInfo
import com.example.vocabquiz.data.LoadIssue
import com.example.vocabquiz.model.LanguageCatalog
import com.example.vocabquiz.model.LanguagePair
import com.example.vocabquiz.ui.QuizState
import com.example.vocabquiz.ui.QuizViewModel
import com.example.vocabquiz.ui.SettingsError
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import java.net.URLEncoder
import java.util.Locale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.vocabquiz.ui.theme.VocabQuizTheme
import android.app.Activity

private enum class Screen {
    Main,
    Settings
}

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val acct = GoogleSignIn.getLastSignedInAccount(this)
        val sheetsScope = Scope("https://www.googleapis.com/auth/spreadsheets.readonly")
        val driveScope = Scope("https://www.googleapis.com/auth/drive.readonly")

        if (acct == null || !GoogleSignIn.hasPermissions(acct, sheetsScope, driveScope)) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        setContent {

            // Use system dark mode automatically:
            val dark = isSystemInDarkTheme()

            VocabQuizTheme (darkTheme = dark) {
                var screen by remember { mutableStateOf(Screen.Main) }
                val vm: QuizViewModel = viewModel()
                val st by vm.state.collectAsState()

                when (screen) {
                    Screen.Settings -> SettingsScreen(on = vm, onBack = { screen = Screen.Main })
                    Screen.Main -> {
                        when (st.status) {
                            QuizState.Status.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = initPhaseLabel(st.initPhase),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                    Button(
                                        onClick = { screen = Screen.Settings },
                                        modifier = Modifier.padding(top = 16.dp)
                                    ) {
                                        Text(stringResource(R.string.settings_open))
                                    }
                                }
                            }
                            QuizState.Status.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val errorMessage = when (st.loadIssue) {
                                        LoadIssue.NoValidRows -> stringResource(R.string.data_error_no_valid_rows)
                                        LoadIssue.NoPairs -> stringResource(R.string.data_error_no_pairs)
                                        null -> stringResource(R.string.no_data)
                                    }
                                    Text(errorMessage)
                                    Text(
                                        text = initPhaseLabel(st.initPhase),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                    Button(
                                        onClick = { screen = Screen.Settings },
                                        modifier = Modifier.padding(top = 16.dp)
                                    ) {
                                        Text(stringResource(R.string.settings_open))
                                    }
                                }
                            }
                            QuizState.Status.Ready -> FlashcardScreen(
                                st = st,
                                on = vm,
                                onOpenSettings = { screen = Screen.Settings }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(st: QuizState, on: QuizViewModel, onOpenSettings: () -> Unit) {
    val outerScroll = rememberScrollState()
    var translateUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fa_gear),
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { on.prevPage() }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.prev_chunk))
                    }
                    OutlinedButton(onClick = { on.nextPage() }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.next_chunk))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .padding(20.dp)
                    .verticalScroll(outerScroll),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 🔽 Single dropdown for pair selection
                if (st.availablePairs.isNotEmpty()) {
                    PairDropdown(
                        label = stringResource(R.string.language_pair),
                        items = st.availablePairs,
                        selected = st.currentPair(),
                        onSelect = { pair ->
                            on.setLangs(pair.src, pair.tgt)
                        }
                    )
                } else {
                    Text(
                        stringResource(R.string.no_language_pairs),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                st.loadReport?.let { report ->
                    if (report.skippedRows > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.data_warning_rows_skipped,
                                    report.validRows,
                                    report.skippedRows
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Progress
                AssistChip(
                    onClick = {},
                    label = { Text("${st.index + 1} / ${st.pool.size}") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Prompt
                Text(
                    st.promptText,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                // Answer bubble (tap to reveal / advance)
                key(st.index) {
                    AnswerBubble(
                        st = st,
                        onNext = { on.nextCard() },
                        onToggleReveal = { on.toggleReveal() }
                    )
                }

            // Card paging
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { on.prevCard() }) { Text(stringResource(R.string.prev)) }
                Button(onClick = { on.nextCard() }) { Text(stringResource(R.string.next)) }
            }

            val translateText = if (st.revealed) st.answerText else st.promptText
            val canTranslate = st.pool.isNotEmpty() && translateText.isNotBlank()
            OutlinedButton(
                enabled = canTranslate,
                onClick = {
                    val pair = st.currentPair() ?: return@OutlinedButton
                    val sourceName = if (st.revealed) pair.tgt else pair.src
                    val targetName = if (st.revealed) pair.src else pair.tgt
                    val sourceCode = LanguageCatalog.toTranslateCode(sourceName) ?: "auto"
                    val targetCode = LanguageCatalog.toTranslateCode(targetName) ?: return@OutlinedButton
                    val encoded = URLEncoder.encode(translateText, "UTF-8")
                    translateUrl = "https://translate.google.com/?sl=$sourceCode&tl=$targetCode&text=$encoded&op=translate"
                }
            ) {
                Text(stringResource(R.string.translate_open))
            }
            val hintText = remember(translateText) {
                val max = 32
                if (translateText.length <= max) translateText else translateText.take(max) + "..."
            }
            Text(
                text = stringResource(R.string.translate_hint, hintText),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    translateUrl?.let { url ->
        Dialog(
            onDismissRequest = { translateUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.translate_title)) },
                        navigationIcon = {
                            TextButton(onClick = { translateUrl = null }) {
                                Text(stringResource(R.string.translate_close))
                            }
                        }
                    )
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                @SuppressLint("SetJavaScriptEnabled")
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                            }
                        },
                        update = { view ->
                            view.loadUrl(url)
                        }
                    )
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(on: QuizViewModel, onBack: () -> Unit) {
    val ui by on.settingsState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        on.ensureSpreadsheetsLoaded()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_app_version_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(stringResource(R.string.settings_spreadsheet), style = MaterialTheme.typography.labelLarge)
                        if (ui.spreadsheets.isNotEmpty()) {
                            SpreadsheetDropdown(
                                label = stringResource(R.string.settings_spreadsheet),
                                items = ui.spreadsheets,
                                selectedId = ui.spreadsheetId,
                                onSelect = { on.selectSpreadsheet(it) }
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(onClick = { on.refreshSpreadsheets() }) {
                                Text(stringResource(R.string.settings_refresh))
                            }
                            if (ui.loadingSpreadsheets) {
                                CircularProgressIndicator(modifier = Modifier.heightIn(max = 20.dp), strokeWidth = 2.dp)
                                Text(stringResource(R.string.settings_loading_spreadsheets))
                            }
                            if (ui.loadingSheets) {
                                CircularProgressIndicator(modifier = Modifier.heightIn(max = 20.dp), strokeWidth = 2.dp)
                                Text(stringResource(R.string.settings_loading_sheets))
                            }
                        }
                    }
                }

                if (ui.sheetNames.isNotEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SheetDropdown(
                                label = stringResource(R.string.settings_sheet),
                                items = ui.sheetNames,
                                selected = ui.selectedSheet,
                                onSelect = { on.selectSheet(it) }
                            )
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_ui_language_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                        val systemDefaultLabel = stringResource(R.string.settings_ui_language_system_default)
                        val uiLanguageTags = remember {
                            listOf(
                                "en",
                                "ca",
                                "cs",
                                "cy",
                                "da",
                                "de",
                                "el",
                                "es",
                                "eu",
                                "fi",
                                "fr",
                                "ga",
                                "gl",
                                "hu",
                                "is",
                                "it",
                                "nl",
                                "no",
                                "pl",
                                "pt",
                                "ro",
                                "sk",
                                "sv"
                            )
                        }
                        val uiLanguageOptions = remember(systemDefaultLabel) {
                            val languageOptions = uiLanguageTags
                                .map { tag -> UiLanguageOption(tag, uiLanguageLabel(tag)) }
                                .sortedBy { it.label }
                            listOf(UiLanguageOption(null, systemDefaultLabel)) + languageOptions
                        }
                        UiLanguageDropdown(
                            label = stringResource(R.string.settings_ui_language_label),
                            items = uiLanguageOptions,
                            selectedTag = ui.uiLanguageTag,
                            onSelect = { on.setUiLanguage(it) }
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.settings_supported_languages_label), style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = stringResource(R.string.settings_supported_languages_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                        val supportedText = remember {
                            LanguageCatalog.supportedLanguageNames().joinToString(separator = "\n")
                        }
                        val supportedScroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp, max = 200.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                                .verticalScroll(supportedScroll)
                        ) {
                            Text(
                                text = supportedText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                ui.error?.let { error ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = settingsErrorLabel(error),
                                color = MaterialTheme.colorScheme.error
                            )
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(context, SignInActivity::class.java))
                                    (context as? Activity)?.finish()
                                }
                            ) {
                                Text(stringResource(R.string.settings_reauth))
                            }
                        }
                    }
                }

                ui.lastErrorAt?.let { at ->
                    Text(
                        text = stringResource(R.string.settings_last_error_time, at),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ui.lastErrorMessage?.let { msg ->
                    Text(
                        text = stringResource(R.string.settings_last_error_message, msg),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canSave =
                        ui.spreadsheetId.isNotBlank() &&
                            ui.selectedSheet != null &&
                            !ui.loadingSheets &&
                            !ui.loadingSpreadsheets &&
                            ui.spreadsheets.isNotEmpty()
                    Button(
                        onClick = { on.saveSettingsAndReload(); onBack() },
                        enabled = canSave
                    ) {
                        Text(stringResource(R.string.settings_save))
                    }
                    OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnswerBubble(st: QuizState, onNext: () -> Unit, onToggleReveal: () -> Unit) {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val innerScroll = rememberScrollState()

    Surface(
        // remove onClick from Surface and use combinedClickable on the modifier:
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp, max = 220.dp)
            .padding(horizontal = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = { if (st.revealed) onNext() else onToggleReveal() },
                onLongClick = {
                    // copy revealed answer if available; otherwise copy the prompt
                    val textToCopy = if (st.revealed) st.answerText else st.promptText
                    if (textToCopy.isNotBlank()) {
                        clipboard.setText(AnnotatedString(textToCopy))
                        Toast.makeText(
                            context,
                            context.getString(R.string.copied_to_clipboard),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(innerScroll),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = st.revealed,
                enter = fadeIn() + scaleIn(initialScale = 0.99f),
                exit = ExitTransition.None
            ) {
                Text(
                    st.answerText,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                    modifier = Modifier.padding(16.dp)
                )
            }
            AnimatedVisibility(
                visible = !st.revealed,
                enter = fadeIn() + scaleIn(initialScale = 0.99f),
                exit = fadeOut() + scaleOut(targetScale = 0.99f)
            ) {
                Text(
                    stringResource(R.string.tap_to_reveal),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

fun QuizState.currentPair(): LanguagePair? {
    val src = sourceLang ?: return null
    val tgt = targetLang ?: return null
    return LanguagePair(src, tgt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairDropdown(
    label: String,
    items: List<LanguagePair>,
    selected: LanguagePair?,
    onSelect: (LanguagePair) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) {
        val locales = configuration.locales
        if (locales.isEmpty) Locale.getDefault() else locales[0]
    }
    val currentLabel = selected?.let { localizedPairLabel(it, locale) }
        ?: stringResource(R.string.select)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = "$label: $currentLabel",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { pair ->
                DropdownMenuItem(
                    text = { Text(localizedPairLabel(pair, locale)) },
                    onClick = {
                        expanded = false
                        onSelect(pair)
                    }
                )
            }
        }
    }
}

private fun localizedPairLabel(pair: LanguagePair, locale: Locale): String {
    val srcName = LanguageCatalog.displayName(pair.src, locale)
    val tgtName = LanguageCatalog.displayName(pair.tgt, locale)
    return "$srcName → $tgtName"
}

@Composable
private fun initPhaseLabel(phase: QuizState.InitPhase): String {
    return when (phase) {
        QuizState.InitPhase.Starting -> stringResource(R.string.init_starting)
        QuizState.InitPhase.ReadingSettings -> stringResource(R.string.init_reading_settings)
        QuizState.InitPhase.LoadingSheets -> stringResource(R.string.init_loading_sheets)
        QuizState.InitPhase.LoadingData -> stringResource(R.string.init_loading_data)
        QuizState.InitPhase.ChoosingPair -> stringResource(R.string.init_choosing_pair)
        QuizState.InitPhase.LoadingChunk -> stringResource(R.string.init_loading_chunk)
        QuizState.InitPhase.Ready -> stringResource(R.string.init_ready)
        QuizState.InitPhase.Error -> stringResource(R.string.init_error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetDropdown(
    label: String,
    items: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = selected ?: stringResource(R.string.select)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = "$label: $currentLabel",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        expanded = false
                        onSelect(name)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpreadsheetDropdown(
    label: String,
    items: List<SpreadsheetInfo>,
    selectedId: String,
    onSelect: (SpreadsheetInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = items.firstOrNull { it.id == selectedId }
    val currentLabel = selected?.name ?: stringResource(R.string.select)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = "$label: $currentLabel",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { info ->
                DropdownMenuItem(
                    text = { Text(info.name) },
                    onClick = {
                        expanded = false
                        onSelect(info)
                    }
                )
            }
        }
    }
}

private data class UiLanguageOption(
    val tag: String?,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UiLanguageDropdown(
    label: String,
    items: List<UiLanguageOption>,
    selectedTag: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = items.firstOrNull { it.tag == selectedTag } ?: items.first()
    val currentLabel = selected.label

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = "$label: $currentLabel",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSelect(option.tag)
                    }
                )
            }
        }
    }
}

private fun uiLanguageLabel(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    val nativeName = titleCase(locale.getDisplayName(locale), locale)
    val englishName = titleCase(locale.getDisplayName(Locale.ENGLISH), Locale.ENGLISH)
    return "$nativeName ($englishName)"
}

private fun titleCase(text: String, locale: Locale): String {
    if (text.isBlank()) return text
    return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

@Composable
private fun settingsErrorLabel(error: SettingsError): String {
    return when (error) {
        SettingsError.MissingSpreadsheetId -> stringResource(R.string.settings_error_missing_id)
        SettingsError.FetchFailed -> stringResource(R.string.settings_error_fetch_failed)
        SettingsError.NoSheets -> stringResource(R.string.settings_error_no_sheets)
        SettingsError.SheetMissing -> stringResource(R.string.settings_error_sheet_missing)
        SettingsError.DataLoadFailed -> stringResource(R.string.settings_error_data_load_failed)
        SettingsError.FolderMissing -> stringResource(R.string.settings_error_folder_missing)
        SettingsError.FolderEmpty -> stringResource(R.string.settings_error_folder_empty)
        SettingsError.DriveFetchFailed -> stringResource(R.string.settings_error_drive_failed)
    }
}
