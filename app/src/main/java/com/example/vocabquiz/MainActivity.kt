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
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vocabquiz.model.Lang
import com.example.vocabquiz.model.LanguagePair
import com.example.vocabquiz.ui.QuizState
import com.example.vocabquiz.ui.QuizViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (GoogleSignIn.getLastSignedInAccount(this) == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                val vm: QuizViewModel = viewModel()
                val st by vm.state.collectAsState()

                when (st.status) {
                    QuizState.Status.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    QuizState.Status.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No data (sign-in / sheet / internet)") }
                    QuizState.Status.Ready -> FlashcardScreen(st, on = vm)
                }
            }
        }
    }
}

@Composable
fun FlashcardScreen(st: QuizState, on: QuizViewModel) {
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
        bottomBar = {
            BottomAppBar {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { on.prevCard() }, modifier = Modifier.weight(1f)) { Text("Prev") }
                    Button(onClick = { on.nextCard() }, modifier = Modifier.weight(1f)) { Text("Next") }
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
                label = "Language pair",
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
            val innerScroll = rememberScrollState()
            Surface(
                onClick = { if (st.revealed) on.nextCard() else on.toggleReveal() },
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 220.dp)
                    .padding(horizontal = 8.dp)
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
                            "Tap to reveal",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Optional chunk paging
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { on.prevPage() }) { Text("Prev chunk") }
                OutlinedButton(onClick = { on.nextPage() }) { Text("Next chunk") }
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
    val currentLabel = selected?.toString() ?: "Select"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = "$label: $currentLabel",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor()
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LangDropdown(
    label: String,
    items: List<Lang>,
    selected: Lang,
    onSelect: (Lang) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = "${label}: ${selected.code}",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { l ->
                DropdownMenuItem(
                    text = { Text(l.code) },
                    onClick = { expanded = false; onSelect(l) }
                )
            }
        }
    }
}
