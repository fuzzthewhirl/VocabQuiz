import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive

object DriveServiceFactory {
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private val HTTP_TRANSPORT = NetHttpTransport()

    private val SCOPES = listOf("https://www.googleapis.com/auth/drive.readonly")

    fun create(context: Context): Drive? {
        val acct = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(context, SCOPES).apply {
            selectedAccount = acct.account
        }
        return Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName("VocabQuiz")
            .build()
    }
}
