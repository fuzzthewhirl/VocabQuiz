@file:Suppress("DEPRECATION")

package com.example.vocabquiz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.vocabquiz.ui.theme.VocabQuizTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

@Suppress("DEPRECATION")
class SignInActivity : ComponentActivity() {
    private var signInClient: GoogleSignInClient? = null
    private var uiState by mutableStateOf(SignInUiState())

    private val signInLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val account = runCatching { task.getResult(ApiException::class.java) }.getOrNull()
        if (account != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            val message = task.exception?.message
            uiState = SignInUiState(status = SignInStatus.Failed, detail = message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val acct = GoogleSignIn.getLastSignedInAccount(this)
        val sheetsScope = Scope("https://www.googleapis.com/auth/spreadsheets.readonly")
        val driveScope = Scope("https://www.googleapis.com/auth/drive.readonly")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(sheetsScope, driveScope)
            .build()

        signInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            val dark = isSystemInDarkTheme()
            VocabQuizTheme(darkTheme = dark) {
                SignInScreen(
                    state = uiState,
                    onRetry = { launchSignIn() }
                )
            }
        }

        if (acct != null && GoogleSignIn.hasPermissions(acct, sheetsScope, driveScope)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        launchSignIn()
    }

    private fun launchSignIn() {
        val client = signInClient ?: return
        uiState = SignInUiState(status = SignInStatus.SigningIn)
        runCatching { signInLauncher.launch(client.signInIntent) }
            .onFailure { uiState = SignInUiState(status = SignInStatus.Failed, detail = it.message) }
    }
}

private enum class SignInStatus {
    SigningIn,
    Failed
}

private data class SignInUiState(
    val status: SignInStatus = SignInStatus.SigningIn,
    val detail: String? = null
)

@Composable
private fun SignInScreen(state: SignInUiState, onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.sign_in_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.sign_in_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            when (state.status) {
                SignInStatus.SigningIn -> {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
                    Text(
                        text = stringResource(R.string.sign_in_loading),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                SignInStatus.Failed -> {
                    Text(
                        text = stringResource(R.string.sign_in_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    state.detail?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.padding(top = 20.dp)
                    ) {
                        Text(stringResource(R.string.sign_in_retry))
                    }
                }
            }
        }
    }
}
