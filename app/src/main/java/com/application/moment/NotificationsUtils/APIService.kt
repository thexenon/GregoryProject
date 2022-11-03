package com.application.moment.NotificationsUtils

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
interface APIService {
    @Headers(
        "Content-Type:application/json",
        "Authorization:key=YOUR_FIREBASE_NOTIFICATION KEY"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: NotificationSender?): Call<MyResponse?>
}
