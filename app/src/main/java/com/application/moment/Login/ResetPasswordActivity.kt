package com.application.moment.Login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.application.moment.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_reset_password.*

class ResetPasswordActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        back_arrow.setOnClickListener {
            finish()
        }
        reset.setOnClickListener {
            resetPassword()
            progress_bar.visibility = View.VISIBLE
        }

    }

    private fun resetPassword(){
        val fAuth = FirebaseAuth.getInstance()

        fAuth.sendPasswordResetEmail(email.text.toString()).addOnCompleteListener { listener ->
            if (listener.isSuccessful) {
                progress_bar.visibility = View.GONE
                Toast.makeText(this, "Password reset successful", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                progress_bar.visibility = View.GONE
                Toast.makeText(this, "Password reset failed", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}