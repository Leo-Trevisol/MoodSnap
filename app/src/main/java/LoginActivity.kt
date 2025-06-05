package com.br.leo.moodsnap.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.br.leo.moodsnap.MainActivity
import com.br.leo.moodsnap.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) { // Verificar se o resultado foi OK
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount = task.getResult(Exception::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Log.e("LoginActivity", "Google Sign-In failed", e)
                Toast.makeText(this, "Falha ao fazer login com Google: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Opcional: Lidar com o cancelamento ou falha do intent de login do Google
            Log.w("LoginActivity", "Google Sign-In cancelled or failed by user.")
            // Toast.makeText(this, "Login com Google cancelado.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.br.leo.moodsnap.R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Configurar listener para o botão de voltar
        binding.btnBack.setOnClickListener {
            finish() // Fecha a LoginActivity
        }

        // Configurar listener para o botão de login com Google
        // O ID no XML é btn_google_sign_in
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login com Firebase bem-sucedido
                    val user = auth.currentUser
                    Toast.makeText(this, "Login bem-sucedido: ${user?.displayName ?: user?.email}", Toast.LENGTH_SHORT).show()
                    // Navegar para a MainActivity ou tela principal
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Limpa a pilha de activities
                    startActivity(intent)
                    finish() // Finaliza LoginActivity
                } else {
                    // Falha no login com Firebase
                    Log.w("LoginActivity", "Firebase Authentication failed", task.exception)
                    Toast.makeText(this, "Falha na autenticação: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
