package com.beem.catmap.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.beem.catmap.R
import com.beem.catmap.ui.auth.exceptions.GoogleAuthException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

class GoogleAuthClient(
    context: Context
) {

    private val appContext = context.applicationContext

    private val credentialManager: CredentialManager =
        CredentialManager.create(appContext)

    fun getCredentialManager(): CredentialManager = credentialManager

    fun buildGetCredentialRequest(): GetCredentialRequest {
        val serverClientId = appContext.getString(R.string.default_web_client_id)

        require(serverClientId.isNotBlank()) {
            "default_web_client_id boş olamaz."
        }

        Log.i(
            "GoogleAuth",
            """
        [AUTH-CONFIG]
        option=GetSignInWithGoogleOption
        package=${appContext.packageName}
        serverClientId=$serverClientId
        """.trimIndent()
        )

        val googleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = serverClientId
        ).build()


        return GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()
    }

    @Throws(GoogleAuthException::class)
    fun extractIdToken(credential: Credential): String {

        if (credential !is CustomCredential) {
            throw GoogleAuthException.UnsupportedCredential(
                credentialType = credential::class.java.simpleName
            )
        }

        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw GoogleAuthException.UnsupportedCredential(
                credentialType = credential.type
            )
        }

        val googleCredential = try {
            GoogleIdTokenCredential.createFrom(credential.data)
        } catch (e: GoogleIdTokenParsingException) {
            throw GoogleAuthException.InvalidGoogleCredential(e)
        } catch (e: Exception) {
            throw GoogleAuthException.InvalidGoogleCredential(e)
        }

        val token = googleCredential.idToken

        if (token.isBlank()) {
            throw GoogleAuthException.EmptyIdToken()
        }

        return token
    }
}

