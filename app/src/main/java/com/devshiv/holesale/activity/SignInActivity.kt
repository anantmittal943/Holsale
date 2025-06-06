package com.devshiv.holesale.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.devshiv.holesale.R
import com.devshiv.holesale.Utils
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.devshiv.holesale.databinding.ActivitySignInBinding
import kotlinx.coroutines.launch

class SignInActivity : ComponentActivity() {

    private val binding: ActivitySignInBinding by lazy {
        ActivitySignInBinding.inflate(layoutInflater)
    }
    private val firebaseAuth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(baseContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        Utils.changeSystemBarsColor(
            window, binding.root,
            ContextCompat.getColor(this, R.color.colorPrimaryVariant),
            ContextCompat.getColor(this, R.color.colorPrimaryVariant)
        )

        binding.googleBtn.setOnClickListener {
            launchCredentialManager()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        updateUI(currentUser)
    }

    private fun launchCredentialManager() {
        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.google_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@SignInActivity,
                    request = request
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(Utils.TAG, "launchCredentialManager: ${e.localizedMessage}", e)
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(Utils.TAG, "Credential is not of type Google ID!")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(Utils.TAG, "signInWithCredential:success")
                    val user = firebaseAuth.currentUser
                    val isNewUser = task.result.additionalUserInfo?.isNewUser == true
                    updateUI(user, isNewUser)
                } else {
                    Log.w(Utils.TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        this,
                        "Failed to sign you in, please try again!!!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun updateUI(currentUser: FirebaseUser?, isNewUser: Boolean = false) {
        if (currentUser != null) {
            val message =
                if (isNewUser) "Sign Up Successful!" else "Welcome Back ${currentUser.displayName}"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}