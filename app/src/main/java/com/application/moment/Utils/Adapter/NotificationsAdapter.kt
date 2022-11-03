package com.application.moment.Utils.Adapter

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.R
import com.application.moment.models.Notifications
import com.application.moment.models.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.layout_item_notifications.view.*
import java.lang.NullPointerException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class NotificationsAdapter(val context: Context,
                           private val notificationList: MutableList<Notifications>,
                           val clickListener: sendClickListener
): RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {


    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef: DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    interface sendClickListener {
        fun onMessageClickListener(notifications: Notifications)
    }

    override fun getItemCount(): Int {
        return notificationList.size
    }

    override fun onCreateViewHolder(view: ViewGroup, position: Int): ViewHolder {
        val cell =  LayoutInflater.from(context).inflate(R.layout.layout_item_notifications, view, false)
        return ViewHolder(cell)


    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val notification = notificationList[position]

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference

        holder.at.text = "@"
        val query: Query = myRef
            .child(context.getString(R.string.dbname_users))
            .child(notification.userID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val mUser = snapshot.getValue(User::class.java)!!

                    holder.username.text = mUser.username

                    Glide
                        .with(context)
                        .load(Uri.parse(mUser.profile_photo))
                        .into(holder.profilePhoto)

                    if (notification.action == "Like"){
                        holder.actionText.text = "like your video ${notification.videoLiked}"
                    }else if(notification.action == "Follow"){
                        holder.actionText.text = "started to follow you !"
                    }
                }catch (e:NullPointerException){
                    e.message
                }



            }
            override fun onCancelled(error: DatabaseError) {

            }


        })

        holder.relLayout.setOnClickListener {
            clickListener.onMessageClickListener(notification)
        }

        val dateCreated = getTimeStampDifference(notification).toString()
        val timeAgo = getTime(notification).toString()
        if (dateCreated != "0"){
            if(dateCreated.toInt() < 7){
                if (dateCreated.toInt() == 1){
                    holder.timeStamp.text = "$dateCreated day ago."
                }else{
                    holder.timeStamp.text = "$dateCreated days ago."

                }
            }else{
                val weekNum = (dateCreated.toInt() /7)
                if (weekNum == 1){
                    holder.timeStamp.text = "$weekNum week ago."

                }else{
                    holder.timeStamp.text = "$weekNum weeks ago."

                }

            }
        }else{
            holder.timeStamp.text = timeAgo

        }



    }


    private fun getTimeStampDifference(message: Notifications): String? {
        Log.d(ContentValues.TAG, "getTimeStampDifference: getting timestamp difference.")
        var difference : String
        val c= Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = (TimeZone.getTimeZone("Canada/Pacific"))
        val today: Date = c.time
        sdf.format(today)
        val timestamp: Date
        val chatTimestamp: String = message.date_created
        try {
            timestamp = sdf.parse(chatTimestamp)
            difference =
                java.lang.String.valueOf(((today.time - timestamp.time).toDouble() / 1000 / 60 / 60 / 24).roundToInt())
        } catch (e: ParseException) {
            Log.e(ContentValues.TAG, "getTimeStampDifference: ParseException: ", e)
            difference = "0"
        }
        return difference
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "deprecation")
    private fun getTime(video: Notifications): String? {
        Log.d(ContentValues.TAG, "getTimeStampDifference: getting timestamp difference.")
        var difference :String
        val c= Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = (TimeZone.getTimeZone("Canada/Pacific"))
        val today: Date = c.time
        sdf.format(today)
        val timestamp: Date
        val videoTimestamp: String = video.date_created
        try {
            timestamp = sdf.parse(videoTimestamp)
            val hours =
                java.lang.String.valueOf(((today.time - timestamp.time).toDouble() / 60 / 60 / 1000 ).roundToInt())
            if (hours.toInt() < 1){
                val min =
                    java.lang.String.valueOf(((today.time - timestamp.time).toDouble() / 60 / 1000 ).roundToInt())
                if(min.toInt() < 1){
                    val sec =
                        java.lang.String.valueOf(((today.time - timestamp.time).toDouble() / 1000 ).roundToInt())
                    difference = "$sec sec ago."
                }else{
                    difference = "$min min ago."
                }
            }else{
                difference = "$hours h ago."
            }
        } catch (e: ParseException) {
            difference = "0"
        }
        return difference
    }







    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val at = itemView.at
        val username = itemView.username
        val timeStamp = itemView.timeStamp
        val profilePhoto = itemView.profile_photo
        val actionText = itemView.action_text
        val relLayout = itemView.relLayout1
        val notifications: Notifications? = null



    }


}