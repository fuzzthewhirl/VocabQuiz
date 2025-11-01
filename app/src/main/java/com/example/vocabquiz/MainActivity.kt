package com.example.vocabquiz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                val state by vm.state.collectAsState()

                if (!state.ready) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Score: ${state.score} / ${state.total - 1}", style = MaterialTheme.typography.titleMedium)

                        Text(
                            state.current?.source ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )

                        state.options.forEachIndexed { i, option ->
                            Button(onClick = { vm.answer(i) }, modifier = Modifier.fillMaxWidth()) {
                                Text(option)
                            }
                        }
                    }
                }
            }
        }
    }
}
