package com.example.vocabquiz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vocabquiz.model.Direction
import com.example.vocabquiz.model.Lang
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashcardScreen(st: QuizState, on: QuizViewModel) {
    val langs = listOf(Lang.FI, Lang.ES, Lang.EN)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Source / Target dropdowns
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LangDropdown(
                label = "Source",
                items = langs,
                selected = st.sourceLang ?: langs.first(),
                onSelect = { l -> on.setLangs(l, st.targetLang ?: l) }
            )
            LangDropdown(
                label = "Target",
                items = langs,
                selected = st.targetLang ?: langs.first(),
                onSelect = { l -> on.setLangs(st.sourceLang ?: l, l) }
            )
        }

        // Direction toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { on.changeDirection(Direction.SRC_TO_TGT) },
                enabled = st.direction != Direction.SRC_TO_TGT
            ) { Text("Source → Target") }
            OutlinedButton(
                onClick = { on.changeDirection(Direction.TGT_TO_SRC) },
                enabled = st.direction != Direction.TGT_TO_SRC
            ) { Text("Target → Source") }
        }

        // Prompt
        Text(
            st.promptText,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
        )

        // Reveal bubble
        Surface(
            onClick = { if(st.revealed) on.nextCard() else on.toggleReveal() },
            shape = MaterialTheme.shapes.large,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .padding(horizontal = 8.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (st.revealed) {
                    Text(
                        st.answerText,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
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

        // Card navigation
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { on.prevCard() }) { Text("Prev") }
            Button(onClick = { on.nextCard() }) { Text("Next") }
        }

        // Chunk paging (optional)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { on.prevPage() }) { Text("Prev chunk") }
            OutlinedButton(onClick = { on.nextPage() }) { Text("Next chunk") }
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
