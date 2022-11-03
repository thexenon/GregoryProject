package com.application.moment.Utils.Adapter

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.R
import com.application.moment.models.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.layout_item_left_chat.view.*
import kotlinx.android.synthetic.main.layout_item_right_chat.view.*
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class ChatRecyclerAdapter(val context: Context,
                          private var messageList: MutableList<Chat>,
                          val clickListener: SendClickListener
): RecyclerView.Adapter<ChatRecyclerAdapter.ViewHolder>() {


    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef: DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    companion object{
        private const val RIGHT_MESSAGE: Int = 0
        private const val LEFT_MESSAGE: Int = 1

    }

    interface SendClickListener {
        fun onMessageClickListener(chat: Chat)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onCreateViewHolder(view: ViewGroup, position: Int): ViewHolder {

        var cell: View? = null
        if (position == RIGHT_MESSAGE) {
            cell = LayoutInflater.from(context)
                .inflate(R.layout.layout_item_right_chat, view, false)
        } else if (position == LEFT_MESSAGE) {
            cell = LayoutInflater.from(context).inflate(R.layout.layout_item_left_chat, view, false)
        }
        return ViewHolder(cell!!)


    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference

        val message = messageList[position]
        holder.chat = message
        Log.d(TAG, "onBindViewHolder: message :$message")
        if(getItemViewType(position) == RIGHT_MESSAGE){
            if (position == messageList.size-1){
                if (message.seen){
                    holder.seen.visibility = View.VISIBLE
                    holder.delivered.visibility = View.GONE
                }else{
                    holder.delivered.visibility = View.VISIBLE
                    holder.seen.visibility = View.GONE
                }
            }else{
                holder.seen.visibility = View.GONE
                holder.delivered.visibility = View.GONE

            }

            holder.textMessage.text = message.message

            val dateCreated = getTimeStampDifference(message).toString()
            val date = getDate(message).toString()
            val timeAgo = getTime(message).toString()
            if (dateCreated != "0"){
                holder.textTime.text = date
            }else{
                holder.textTime.text = timeAgo

            }

        }else{
            holder.textMessageL.text = message.message

            val dateCreated = getTimeStampDifference(message).toString()
            val date = getDate(message).toString()
            val timeAgo = getTime(message).toString()
            if (dateCreated != "0"){

                holder.textTimeL.text = date

            }else{
                holder.textTimeL.text = timeAgo

            }
        }



        

    }

    private fun getTimeStampDifference(message: Chat): String? {
        Log.d(TAG, "getTimeStampDifference: getting timestamp difference.")
        var difference: String
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
            Log.e(TAG, "getTimeStampDifference: ParseException: ", e)
            difference = "0"
        }
        return difference
    }

    private fun getDate(message: Chat): String?{
        Log.d(TAG, "getTimeStampDifference: getting timestamp difference.")
        var difference: String
        val c= Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = (TimeZone.getTimeZone("Canada/Pacific"))
        val today: Date = c.time
        sdf.format(today)
        val timestamp: Date
        val chatTimestamp: String = message.date_created


        try {
            timestamp = sdf.parse(chatTimestamp)
            val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(timestamp)

            difference = date
        } catch (e: ParseException) {
            Log.e(TAG, "getTimeStampDifference: ParseException: ", e)
            difference = "error"
        }
        return difference
    }



    private fun getTime(chat: Chat): String? {
        var difference :String
        val c= Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = (TimeZone.getTimeZone("Canada/Pacific"))
        val today: Date = c.time
        sdf.format(today)
        val timestamp: Date
        val videoTimestamp: String = chat.date_created
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





    override fun getItemViewType(position: Int): Int {
        auth = Firebase.auth
        return if (messageList[position].sender == auth.currentUser!!.uid) {
            RIGHT_MESSAGE
        } else {
            LEFT_MESSAGE
        }
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val textMessage = itemView.text_message
        val textMessageL = itemView.text_messageL
        val textTime = itemView.time
        val textTimeL = itemView.timeL
        val seen = itemView.seen
        val delivered = itemView.delivered
        var chat: Chat ? = null


        init {
            itemView.setOnLongClickListener {
                    clickListener.onMessageClickListener(chat!!)
                false
            }


        }


    }


}