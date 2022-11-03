package com.application.moment.Utils.Adapter

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.NotificationsUtils.*
import com.application.moment.R
import com.application.moment.models.MessageOrder
import com.application.moment.models.Notifications
import com.application.moment.models.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.layout_item_search.view.*
import kotlinx.android.synthetic.main.layout_item_search.view.at
import kotlinx.android.synthetic.main.layout_item_search.view.display_name
import kotlinx.android.synthetic.main.layout_item_search.view.profile_photo
import kotlinx.android.synthetic.main.layout_item_search.view.username
import kotlinx.android.synthetic.main.layout_item_search.view.certified
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class SearchRecyclerAdapter(val context: Context,
                            private val userInfo: MutableList<User>,
                            val userID: String,
                            val clickListener: SendClickListener
): RecyclerView.Adapter<SearchRecyclerAdapter.ProfileView>() {
    private var mFollowByCurrentUser = false
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //NOTIFICATIONS
    private lateinit var apiService: APIService

    //VAR
    private var mUsername = ""

    companion object{
        private const val TAG = "SearchRecyclerAdapter"
    }

    interface SendClickListener{
        fun onProfileClickListener(user: User)
    }

    override fun getItemCount(): Int {
        return userInfo.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ProfileView {

        val cell =  LayoutInflater.from(context).inflate(R.layout.layout_item_search, p0, false)

        return ProfileView(cell!!)


    }

    override fun onBindViewHolder(holder: ProfileView, position: Int) {
        //Profile info
        val user = userInfo[position]
        holder.userInfo = user
        holder.at.text = "@"
        holder.username.text = user.username
        holder.displayName.text = user.display_name

        Glide
            .with(context)
            .load(Uri.parse(user.profile_photo))
            .into(holder.profilePhoto)

        if (user.userID == userID){
            holder.follow.visibility = View.GONE
        }else{
            holder.follow.visibility = View.VISIBLE
        }

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        myInformations()
        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService::class.java)

        if (user.userID != userID){
            checkFollowers(holder, user)
        }else{
            holder.follow.visibility = View.GONE
            holder.unfollow.visibility = View.GONE
        }

        isCertified(user, holder)


        holder.follow.setOnClickListener {
            holder.follow.visibility = View.GONE
            holder.unfollow.visibility = View.VISIBLE

            myRef.child(context.getString(R.string.followers))
                .child(user.userID)
                .child(auth.currentUser!!.uid)
                .child(context.getString(R.string.user_id))
                .setValue(auth.currentUser!!.uid)

            myRef.child(context.getString(R.string.following))
                .child(auth.currentUser!!.uid)
                .child(user.userID)
                .child(context.getString(R.string.user_id))
                .setValue(user.userID)

            val mNotification = Notifications(auth.currentUser!!.uid, "Follow", getTimestamp().toString(), "", false)
            myRef.child(context.getString(R.string.dbname_notifications)).child(user.userID).push().setValue(mNotification)
            checkFollowers(holder, user)

            addToMessageOrder(user.userID)

            val query: Query = myRef
                .child(context.getString(R.string.dbname_tokens))
                .child(user.userID)
            query.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()){
                        val usertoken= dataSnapshot.child(context.getString(R.string.token)).value.toString()
                        Log.d(TAG, "onDataChange: userToken = $usertoken")
                        sendNotification(usertoken, "GoLyve".trim() ,"$mUsername started to follow you !".trim())
                    }

                }
                override fun onCancelled(error: DatabaseError) {

                }


            })
        }
        holder.unfollow.setOnClickListener {
            holder.unfollow.visibility = View.GONE
            holder.follow.visibility = View.VISIBLE
            myRef.child(context.getString(R.string.followers))
                .child(user.userID)
                .child(auth.currentUser!!.uid)
                .child(context.getString(R.string.user_id))
                .removeValue()

            myRef.child(context.getString(R.string.following))
                .child(auth.currentUser!!.uid)
                .child(user.userID)
                .child(context.getString(R.string.user_id))
                .removeValue()
            checkFollowers(holder, user)
        }


    }

    private fun addToMessageOrder(userID: String){

        val addUser = MessageOrder(userID, getTimestamp().toString())
        val addMe = MessageOrder(auth.currentUser!!.uid, getTimestamp().toString())
        //Add me -> his message list
        myRef.child(context.getString(R.string.dbname_message_order))
            .child(userID)
            .child(auth.currentUser!!.uid)
            .setValue(addMe)


        //Add this user -> my message list
        myRef.child(context.getString(R.string.dbname_message_order))
            .child(auth.currentUser!!.uid)
            .child(userID)
            .setValue(addUser)

    }

    private fun myInformations(){
        val query: Query = myRef
            .child(context.getString(R.string.dbname_users))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val usernameF = dataSnapshot.child(context.getString(R.string.username)).value
                mUsername = usernameF.toString()

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun sendNotification(usertoken:String,title: String,message: String){
        val data= Data(title,message)
        val sender = NotificationSender(data,usertoken)
        apiService.sendNotification(sender).enqueue(object : Callback<MyResponse?> {
            override fun onResponse(call: Call<MyResponse?>, response: Response<MyResponse?>) {
                if (response.code() == 200) {
                    if (response.body()!!.success != 1) {
                        Log.d(TAG, "onResponse: Failed ")
                    }
                }
            }

            override fun onFailure(call: Call<MyResponse?>, t: Throwable?) {

            }
        })
    }


    private fun getTimestamp(): String? {

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = TimeZone.getTimeZone("Canada/Pacific")
        return sdf.format(Date())
    }

    private fun checkFollowers(holder: ProfileView, user: User){
        val query: Query = myRef
            .child(context.getString(R.string.followers))
            .child(user.userID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    for (singleSnapshot in dataSnapshot.children) {
                        //val userid = singleSnapshot.child("user_id").value.toString()
                        val userId = singleSnapshot.key.toString()
                        Log.d(TAG, "onDataChange: userID : $userId")
                        if(userId == auth.currentUser!!.uid){
                            mFollowByCurrentUser = true
                            holder.unfollow.visibility = View.VISIBLE
                            holder.follow.visibility = View.GONE
                        }
                    }

                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


    }

    private fun isCertified(user: User, holder: ProfileView){
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



    inner class ProfileView(itemView: View): RecyclerView.ViewHolder(itemView){


        val profilePhoto = itemView.profile_photo
        val at = itemView.at
        val username = itemView.username
        val displayName = itemView.display_name
        val follow = itemView.follow
        val unfollow = itemView.unFollow
        val certified = itemView.certified
        var userInfo : User? = null



        init {
            itemView.setOnClickListener {
                clickListener.onProfileClickListener(userInfo!!)

            }



        }
    }
}