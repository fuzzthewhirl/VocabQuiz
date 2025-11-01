package com.example.vocabquiz.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets

object SheetsServiceFactory {
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private val HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport()
    private val SCOPES = listOf("https://www.googleapis.com/auth/spreadsheets.readonly")

    fun create(context: Context): Sheets? {
        val acct = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(context, SCOPES)
        credential.selectedAccount = acct.account
        return Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName("VocabQuiz")
            .build()
    }
}
