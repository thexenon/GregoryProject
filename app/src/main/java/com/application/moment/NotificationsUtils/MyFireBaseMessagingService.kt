package com.application.moment.NotificationsUtils

import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFireBaseMessagingService: FirebaseMessagingService() {
    private lateinit var title: String
    private lateinit var message: String
    companion object{
        private const val TAG = "MyFireBaseMessaging"
    }
    override fun onMessageReceived(@NonNull remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        title = remoteMessage.data["Title"].toString()
        message = remoteMessage.data["Message"].toString()
        if (SDK_INT >= Build.VERSION_CODES.O){
            sendOreoNotification()
        }else{

            sendOldVersionNotification()
        }

    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        //Don't use.
        Log.d(TAG, "onNewToken: checking token....")

    }

    @Suppress("deprecation")
    private fun sendOreoNotification(){
        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(applicationContext)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSound)

        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, builder.build())

        val oreoNotification = OreoNotification(this)
        oreoNotification.createChannel()
        val mBuilder = oreoNotification.getOreoNotification(title, message, defaultSound)
        oreoNotification.getManager()?.notify(0, mBuilder?.build())

    }

    @Suppress("deprecation")
    private fun sendOldVersionNotification(){

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(applicationContext)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSound)

        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, builder.build())
    }
}
