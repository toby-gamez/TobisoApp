package com.tobiso.tobisoappnative.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** Outcome of asking the user to pick a Google account. */
sealed interface GoogleSignInResult {
    data class Token(val idToken: String) : GoogleSignInResult
    /** The user closed the account sheet — not an error. */
    object Cancelled : GoogleSignInResult
    data class Failed(val message: String) : GoogleSignInResult
}

/**
 * "Sign in with Google" through Android Credential Manager. [serverClientId] must be the web OAuth client ID
 * (the audience the Tobiso server validates). [activityContext] has to be an Activity context.
 */
suspend fun requestGoogleIdToken(activityContext: Context, serverClientId: String): GoogleSignInResult {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
        .build()
    return try {
        val result = CredentialManager.create(activityContext).getCredential(activityContext, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleSignInResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleSignInResult.Failed("Přihlášení přes Google není na tomto zařízení dostupné.")
        }
    } catch (e: GetCredentialCancellationException) {
        timber.log.Timber.w(e, "Google sign-in cancelled")
        GoogleSignInResult.Cancelled
    } catch (e: NoCredentialException) {
        timber.log.Timber.w(e, "Google sign-in: no credential")
        GoogleSignInResult.Failed("Na zařízení není žádný účet Google.")
    } catch (e: GetCredentialException) {
        timber.log.Timber.e(e, "Google sign-in failed")
        GoogleSignInResult.Failed("Přihlášení přes Google se nezdařilo.")
    } catch (e: GoogleIdTokenParsingException) {
        GoogleSignInResult.Failed("Přihlášení přes Google se nezdařilo.")
    }
}
