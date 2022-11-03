package com.application.moment.Login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.application.moment.R


class WelcomeActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnWelcome = findViewById<Button>(R.id.btnWelcome)

        btnWelcome.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        setupVideo()

    }

    private fun setupVideo(){
        val videoView = findViewById<VideoView>(R.id.videoView)
        val uri= Uri.parse("android.resource://" + packageName + "/" + R.raw.moment_gif)
        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener { mp -> mp.isLooping = true }
        videoView.start()

    }
}