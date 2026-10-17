package com.gymtracker.app.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.gymtracker.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class GoogleUser(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val idToken: String? = null,
)

@Singleton
class GoogleAuthService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val googleSignInClient: GoogleSignInClient by lazy {
        val webClientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrNull()
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        if (!webClientId.isNullOrBlank() && webClientId != "replace-with-google-web-client-id") {
            runCatching { builder.requestIdToken(webClientId) }
        }

        GoogleSignIn.getClient(context, builder.build())
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun handleSignInResult(data: Intent?): Result<GoogleUser> = withContext(Dispatchers.IO) {
        val playServicesResult = runCatching {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val email = account.email ?: error("No email returned from Google account.")
            val displayName = account.displayName ?: email.substringBefore("@")
            val photoUrl = account.photoUrl?.toString()
            val idToken = account.idToken
            val id = account.id ?: ("google_" + kotlin.math.abs(email.hashCode()))

            // Optionally sync with Firebase Auth if configured
            if (FirebaseApp.getApps(context).isNotEmpty() && idToken != null) {
                runCatching {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).await()
                }
            }

            GoogleUser(
                id = id,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                idToken = idToken,
            )
        }

        playServicesResult
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            runCatching { googleSignInClient.signOut().await() }
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                runCatching { FirebaseAuth.getInstance().signOut() }
            }
            Unit
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { continuation.resume(it) }
            addOnFailureListener { continuation.resumeWithException(it) }
            addOnCanceledListener { continuation.cancel() }
        }
}
