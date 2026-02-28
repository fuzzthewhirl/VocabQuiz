@file:Suppress("DEPRECATION")

package com.example.vocabquiz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vocabquiz.model.Lang
import com.example.vocabquiz.model.LanguagePair
import com.example.vocabquiz.ui.QuizState
import com.example.vocabquiz.ui.QuizViewModel
import com.example.vocabquiz.ui.SettingsError
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.vocabquiz.ui.theme.VocabQuizTheme

private enum class Screen {
    Main,
    Settings
}

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (GoogleSignIn.getLastSignedInAccount(this) == null) {
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
                                    Text(stringResource(R.string.no_data))
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

    // All possible ordered pairs
    val pairs = listOf(
        LanguagePair("fi", "es"),
        LanguagePair("es", "fi"),
        LanguagePair("en", "es"),
        LanguagePair("es", "en"),
        LanguagePair("fi", "en"),
        LanguagePair("en", "fi"),
    )

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
            PairDropdown(
                label = stringResource(R.string.language_pair),
                items = pairs,
                selected = st.currentPair(),
                onSelect = { pair ->
                    val src = Lang.valueOf(pair.src.uppercase())
                    val tgt = Lang.valueOf(pair.tgt.uppercase())
                    on.setLangs(src, tgt)
                }
            )

            // Progress
            Text("${st.index + 1} / ${st.pool.size}", style = MaterialTheme.typography.labelLarge)

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
            AnswerBubble(
                st = st,
                onNext = { on.nextCard() },
                onToggleReveal = { on.toggleReveal() }
            )

            // Card paging
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { on.prevCard() }) { Text(stringResource(R.string.prev)) }
                Button(onClick = { on.nextCard() }) { Text(stringResource(R.string.next)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(on: QuizViewModel, onBack: () -> Unit) {
    val ui by on.settingsState.collectAsState()

    LaunchedEffect(Unit) {
        on.ensureSheetNamesLoaded()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .systemBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(stringResource(R.string.settings_spreadsheet_id), style = MaterialTheme.typography.labelLarge)
            TextField(
                value = ui.spreadsheetId,
                onValueChange = { on.onSpreadsheetIdChange(it) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { on.refreshSheetNames() }) {
                    Text(stringResource(R.string.settings_refresh))
                }
                if (ui.loading) {
                    CircularProgressIndicator(modifier = Modifier.heightIn(max = 20.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.settings_loading_sheets))
                }
            }

            ui.error?.let { error ->
                Text(
                    text = settingsErrorLabel(error),
                    color = MaterialTheme.colorScheme.error
                )
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

            if (ui.sheetNames.isNotEmpty()) {
                SheetDropdown(
                    label = stringResource(R.string.settings_sheet),
                    items = ui.sheetNames,
                    selected = ui.selectedSheet,
                    onSelect = { on.selectSheet(it) }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canSave = ui.spreadsheetId.isNotBlank() && ui.selectedSheet != null && !ui.loading
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
            if (st.revealed) {
                Text(
                    st.answerText,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
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
    return LanguagePair(src.code, tgt.code)
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
    val currentLabel = selected?.toString() ?: stringResource(R.string.select)

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
                    text = { Text(pair.toString()) },
                    onClick = {
                        expanded = false
                        onSelect(pair)
                    }
                )
            }
        }
    }
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

@Composable
private fun settingsErrorLabel(error: SettingsError): String {
    return when (error) {
        SettingsError.MissingSpreadsheetId -> stringResource(R.string.settings_error_missing_id)
        SettingsError.FetchFailed -> stringResource(R.string.settings_error_fetch_failed)
        SettingsError.NoSheets -> stringResource(R.string.settings_error_no_sheets)
    }
}
