package com.gymtracker.app.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.gymtracker.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class CloudBackupService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isConfigured(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    fun googleSignInIntent(): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .build()
        return GoogleSignIn.getClient(context, options).signInIntent
    }

    suspend fun handleGoogleSignInResult(data: Intent?): Result<String> = runCatching {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
        val token = account.idToken ?: error("Google sign-in did not return an ID token.")
        val credential = GoogleAuthProvider.getCredential(token, null)
        val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
        authResult.user?.email ?: "Signed in"
    }

    suspend fun backup(snapshotJson: String): Result<Unit> = runCatching {
        require(isConfigured()) { "Firebase is not configured for this build." }
        val user = FirebaseAuth.getInstance().currentUser ?: error("Google sign-in is required before cloud backup.")
        FirebaseFirestore.getInstance()
            .collection("gymtracker_backups")
            .document(user.uid)
            .set(
                mapOf(
                    "updatedAt" to System.currentTimeMillis(),
                    "payload" to snapshotJson,
                )
            )
            .await()
    }

    suspend fun restore(): Result<String> = runCatching {
        require(isConfigured()) { "Firebase is not configured for this build." }
        val user = FirebaseAuth.getInstance().currentUser ?: error("Google sign-in is required before cloud restore.")
        val snapshot = FirebaseFirestore.getInstance()
            .collection("gymtracker_backups")
            .document(user.uid)
            .get()
            .await()
        snapshot.getString("payload") ?: error("No cloud backup exists.")
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
}
