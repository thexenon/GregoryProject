package com.application.moment.Messages

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.Home.MainActivity
import com.application.moment.Login.WelcomeActivity
import com.application.moment.NotificationsUtils.*
import com.application.moment.R
import com.application.moment.Search.ViewProfileActivity
import com.application.moment.Utils.Adapter.ChatRecyclerAdapter
import com.application.moment.Utils.Adapter.MessagesRecyclerAdapter
import com.application.moment.models.Chat
import com.application.moment.models.MessageOrder
import com.application.moment.models.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_chat.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap



class ChatActivity : AppCompatActivity(), ChatRecyclerAdapter.SendClickListener{

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var adapter: ChatRecyclerAdapter?= null
    private var messagesFound: MutableList<Chat> = mutableListOf()
    private var layoutManager: RecyclerView.LayoutManager? = null

    //VAR
    private var userID = ""
    private var mUsername = ""


    //Notifications
    private lateinit var apiService: APIService

    companion object{
        private const val TAG = "ChatActivity"
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        userID = intent.getStringExtra(getString(R.string.user_id)).toString()
        setupFirebaseAuth()
        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService::class.java)
        myInformations()
        userInformations()
        setAdapter()

        //Refresh the messages
        val reference = FirebaseDatabase.getInstance().getReference(getString(R.string.dbname_chat))
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                try {
                    readMessages()
                    seenMessage()
                }catch (e: IllegalStateException){
                    e.message
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


        //All the widgets
        send_message.setOnClickListener {
            if (editText_message.text.toString() != ""){
                sendMessage(auth.currentUser!!.uid, userID, editText_message.text.toString())
            }else{
                Toast.makeText(this, "You can't send a blank message.", Toast.LENGTH_SHORT).show()
            }
        }


        back_arrow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).putExtra(getString(R.string.message_fragment), getString(R.string.message_fragment))
            startActivity(intent)
            finish()
        }
        profile_photo.setOnClickListener{
            val intent = Intent(this, ViewProfileActivity::class.java).putExtra(getString(R.string.user_id), userID)
            startActivity(intent)
        }
        username.setOnClickListener {
            val intent = Intent(this, ViewProfileActivity::class.java).putExtra(getString(R.string.user_id), userID)
            startActivity(intent)
        }
        display_name.setOnClickListener {
            val intent = Intent(this, ViewProfileActivity::class.java).putExtra(getString(R.string.user_id), userID)
            startActivity(intent)
        }
        isCertified(userID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.navigationBarColor = getColor(R.color.colorBottomNavigation)
        }


    }




    private fun userInformations(){
        val query: Query = myRef
            .child(getString(R.string.dbname_users))
            .child(userID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val displayname = dataSnapshot.child(getString(R.string.display_name)).value
                val usernameF = dataSnapshot.child(getString(R.string.username)).value
                val profilePhoto = dataSnapshot.child(getString(R.string.profile_photo)).value
                display_name.text = displayname.toString()
                username.text = usernameF.toString()
                at.text = "@"
                Glide
                    .with(this@ChatActivity)
                    .load(Uri.parse(profilePhoto.toString()))
                    .into(profile_photo)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    private fun myInformations(){
        val query: Query = myRef
            .child(getString(R.string.dbname_users))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val usernameF = dataSnapshot.child(getString(R.string.username)).value
                mUsername = usernameF.toString()

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun setAdapter(){

        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false)
        chat_recycler.layoutManager = layoutManager

        if (adapter == null){
            adapter = ChatRecyclerAdapter(this, messagesFound ,this)
            chat_recycler.adapter = adapter
        } else{
            adapter!!.notifyDataSetChanged()
        }
    }

    private fun seenMessage() {
        val reference: DatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.dbname_chat))
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val messages = snapshot.getValue(Chat::class.java)
                    if (messages?.receiver
                            .equals(FirebaseAuth.getInstance().currentUser!!.uid) &&
                        messages?.sender.equals(userID)
                    ) {
                        val hashMap =
                            HashMap<String, Any>()
                        hashMap[getString(R.string.seen)] = true
                        snapshot.ref.updateChildren(hashMap)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    private fun readMessages(){


        messagesFound.clear()
        val query: Query = myRef
            .child(getString(R.string.dbname_chat))
        query.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                for (singleSnapshot in snapshot.children){
                    val myMessage = singleSnapshot.getValue(Chat::class.java)

                    val mChat = Chat(myMessage!!.sender, myMessage.receiver, myMessage.message,myMessage.date_created, myMessage.seen)
                    if (myMessage.sender == auth.currentUser!!.uid && myMessage.receiver == userID || myMessage.sender == userID && myMessage.receiver == auth.currentUser!!.uid){
                        messagesFound.add(mChat)
                        adapter!!.notifyDataSetChanged()
                    }

                    adapter!!.notifyDataSetChanged()
                    chat_recycler.smoothScrollToPosition(adapter!!.itemCount)

                }
            }
            override fun onCancelled(error: DatabaseError) {

            }


        })


    }
    private fun getTimestamp(): String? {

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = TimeZone.getTimeZone("Canada/Pacific")
        return sdf.format(Date())
    }


    @Suppress("UnusedEquals")
    private fun sendMessage(
        sender: String,
        receiver: String,
        message: String
    ) {
        val myMessage = Chat(sender, receiver, message, getTimestamp().toString(),false)
        myRef.child(getString(R.string.dbname_chat)).push().setValue(myMessage)
        editText_message.text.clear()
        editText_message.text.equals("")
        hideSoftKeyboard()

        val query: Query = myRef
            .child(getString(R.string.dbname_tokens))
            .child(userID)
        query.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    val usertoken= dataSnapshot.child(getString(R.string.token)).value.toString()
                    Log.d(TAG, "onDataChange: userToken = $usertoken")
                    sendNotification(usertoken, "GoLyve".trim() ,"$mUsername has sent you a message".trim())

                    addToMessageOrder(userID)
                }



            }
            override fun onCancelled(error: DatabaseError) {

            }


        })


    }

    private fun addToMessageOrder(userID: String){

        val addUser = MessageOrder(userID, getTimestamp().toString())
        val addMe = MessageOrder(auth.currentUser!!.uid, getTimestamp().toString())
        //Add me -> his message list
        myRef.child(getString(R.string.dbname_message_order))
            .child(userID)
            .child(auth.currentUser!!.uid)
            .setValue(addMe)


        //Add this user -> my message list
        myRef.child(getString(R.string.dbname_message_order))
            .child(auth.currentUser!!.uid)
            .child(userID)
            .setValue(addUser)

    }

    private fun hideSoftKeyboard() {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
    }

    private fun isCertified(userID: String){
        val query: Query = myRef
            .child(getString(R.string.dbname_certified))
            .child(userID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    certified.visibility = View.VISIBLE

                } else {
                    certified.visibility = View.GONE

                }
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
                        Log.d(TAG, "onResponse: Failed.")
                    }
                }
            }

            override fun onFailure(call: Call<MyResponse?>, t: Throwable?) {

            }
        })
    }
    /**
     *                                                  FIREBASE AUTHENTICATION
     */

    /**
     * checks to see if the @param 'user' is logged in
     * @param user
     */
    private fun checkCurrentUser(user: FirebaseUser?) {
        Log.d(TAG, "checkCurrentUser: checking if user is logged in.")
        if (user == null) {
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Setup the firebase auth object
     */
    private fun setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.")
        auth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser

            //check if the user is logged in
            checkCurrentUser(user)
            if (user != null) {
                // User is signed in
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid)
            } else {
                // User is signed out
                Log.d(TAG, "onAuthStateChanged:signed_out")
            }
            // ...
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(mAuthListener!!)
        checkCurrentUser(auth.currentUser)
    }


    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            auth.removeAuthStateListener(mAuthListener!!)
        }
    }

    override fun onMessageClickListener(chat: Chat) {
        relative_delete.visibility = View.VISIBLE
        no.setOnClickListener{
            relative_delete.visibility = View.GONE
        }
        yes.setOnClickListener{

            val query: Query = myRef
                .child(getString(R.string.dbname_chat))
            query.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (singleSnapshot in snapshot.children){
                        val chatList = singleSnapshot.getValue(Chat::class.java)
                         if (chatList!! == chat){
                             val keyID = singleSnapshot.key.toString()
                             myRef.child(getString(R.string.dbname_chat))
                                 .child(keyID)
                                 .removeValue()
                             relative_delete.visibility = View.GONE
                         }
                        }



                }
                override fun onCancelled(error: DatabaseError) {

                }


            })
        }


    }
}