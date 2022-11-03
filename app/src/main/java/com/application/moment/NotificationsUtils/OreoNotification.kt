package com.application.moment.NotificationsUtils

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import com.application.moment.R

class OreoNotification(base: Context) : ContextWrapper(base) {
    companion object{
        private const val CHANNEL_ID = "com.application.moment"
        private const val CHANNEL_NAME = "GoLyve"
    }

    private var notificationManager : NotificationManager ? = null
    @TargetApi(Build.VERSION_CODES.O)
   fun createChannel(){
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        channel.enableLights(false)
        channel.enableVibration(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getManager()?.createNotificationChannel(channel)
   }

    fun getManager() : NotificationManager? {

        if (notificationManager == null){
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return notificationManager
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Suppress("deprecation")
    fun getOreoNotification(title : String, body: String, soundUri: Uri) : Notification.Builder ?{
        return Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_logo)
            .setSound(soundUri)
            .setAutoCancel(true)

    }


}