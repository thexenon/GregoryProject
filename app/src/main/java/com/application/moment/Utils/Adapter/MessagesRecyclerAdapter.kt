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
import com.application.moment.models.Chat
import com.application.moment.models.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.layout_item_message.view.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class MessagesRecyclerAdapter (val context: Context,
                               private var myFollowingList: MutableList<User>,
                               val clickListener: SendClickListener
): RecyclerView.Adapter<MessagesRecyclerAdapter.ViewHolder>() {
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    interface SendClickListener{
        fun onMessageClickListener(user: User)
        fun onMessageLongClickListener(user: User)
    }

    override fun onCreateViewHolder(view: ViewGroup, p1: Int): ViewHolder {
        val cell = LayoutInflater.from(context).inflate(R.layout.layout_item_message, view, false)
        return ViewHolder(cell)
    }

    override fun getItemCount(): Int {
        return myFollowingList.size
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val myFollowingInfo = myFollowingList[position]
        holder.myFollowing = myFollowingInfo
        holder.at.text = "@"
        holder.displayName.text = myFollowingInfo.display_name
        holder.username.text = myFollowingInfo.username
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        Glide
            .with(context)
            .load(Uri.parse(myFollowingInfo.profile_photo))
            .into(holder.profilePhoto)
        lastMessage(myFollowingInfo.userID, holder, myFollowingInfo)
        isCertified(myFollowingInfo, holder)

    }


    private fun lastMessage(userID: String, holder: ViewHolder, myFollowingInfo: User) {
        var theLastMessage = "default"
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().getReference(context.getString(R.string.dbname_chat))
        reference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val chat = snapshot.getValue(Chat::class.java)
                    if (firebaseUser != null && chat != null) {
                        if (chat.receiver == firebaseUser.uid && chat.sender == userID ||
                            chat.receiver == userID && chat.sender == firebaseUser.uid
                        ) {
                            theLastMessage = chat.message
                            val dateCreated = getTimeStampDifference(chat).toString()
                            val timeAgo = getTime(chat).toString()
                            if (dateCreated != "0"){
                                if(dateCreated.toInt() < 7){
                                    if (dateCreated.toInt() == 1){
                                        holder.textTime.text = "$dateCreated day ago."
                                    }else{
                                        holder.textTime.text = "$dateCreated days ago."

                                    }
                                }else{
                                    val weekNum = (dateCreated.toInt() /7)
                                    if (weekNum == 1){
                                        holder.textTime.text = "$weekNum week ago."
                                    }
                                    else{
                                        holder.textTime.text = "$weekNum weeks ago."

                                    }

                                }
                            }else{
                                holder.textTime.text = timeAgo

                            }
                            checkIfIsSeen(chat,  myFollowingInfo, holder)
                        }
                    }else{
                        theLastMessage = "default"
                    }
                }
                if (theLastMessage=="default") {
                    holder.lastMessage.text = "No Message Found"

                }else{
                    holder.lastMessage.text = theLastMessage
                }

                when(holder.lastMessage.text){
                    "No Message Found" -> holder.textTime.visibility = View.GONE
                    else ->holder.textTime.visibility = View.VISIBLE
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


    }

    private fun checkIfIsSeen(listChat: Chat, myFollowingInfo: User, holder: ViewHolder){
        if(listChat.sender == myFollowingInfo.userID && listChat.receiver == auth.currentUser!!.uid){
            if (!listChat.seen){
                holder.notSeen.visibility = View.VISIBLE
            }else{
                holder.notSeen.visibility = View.GONE
            }
        }
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun getTimeStampDifference(message: Chat): String? {
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
    private fun getTime(chat: Chat): String? {
        Log.d(ContentValues.TAG, "getTimeStampDifference: getting timestamp difference.")
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

    private fun isCertified(user: User, holder: ViewHolder){
        val query: Query = myRef
            .child(context.getString(R.string.dbname_certified))
            .child(user.userID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    holder.certified.visibility = View.VISIBLE

                } else {
                    holder.certified.visibility = View.GONE

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }





    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val profilePhoto = itemView.profile_photo
        val username  = itemView.username
        val displayName  = itemView.display_name
        val lastMessage = itemView.last_message
        val textTime = itemView.timeStamp
        val at = itemView.at
        var myFollowing : User ? = null
        val notSeen = itemView.not_seen
        val certified = itemView.certified


        init {
            itemView.setOnClickListener {
                clickListener.onMessageClickListener(myFollowing!!)
            }
            itemView.setOnLongClickListener {
                clickListener.onMessageLongClickListener(myFollowing!!)
                false
            }



        }
    }

}