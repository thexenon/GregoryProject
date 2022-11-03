package com.application.moment.Login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.application.moment.Home.MainActivity
import com.application.moment.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(){

    //Firebase
    private lateinit var auth : FirebaseAuth


    companion object{
        private const val TAG = "LoginActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = Firebase.auth

        setupWidgets()

    }



    private fun setupWidgets(){
        val textSignUp = findViewById<TextView>(R.id.text_register)

        textSignUp.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val btnLogin = findViewById<Button>(R.id.btn_login)
        btnLogin.setOnClickListener {
            signIn()
        }

        help.setOnClickListener {
            val uri = Uri.parse("https://momentapplication.com")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
        text_forgot.setOnClickListener{
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }
    }


    private fun signIn(){
        val email = email.text.toString()
        val password = password.text.toString()
        when {
            email == "" -> {
                Toast.makeText(baseContext, "You must write your email",
                    Toast.LENGTH_SHORT).show()
            }
            password == "" -> {
                Toast.makeText(baseContext, "You must write your password",
                    Toast.LENGTH_SHORT).show()
            }
            else -> {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "signInWithEmail:success")

                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.exception)
                            Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()

                        }

                    }
            }
        }
    }

}