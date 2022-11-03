package com.application.moment.Notifications

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.Home.MainActivity
import com.application.moment.Login.WelcomeActivity
import com.application.moment.Popular.PopularActivity
import com.application.moment.Profile.ProfileActivity
import com.application.moment.R
import com.application.moment.Search.ViewProfileActivity
import com.application.moment.Utils.Adapter.NotificationsAdapter
import com.application.moment.VideoGallery.VideoGalleryActivity
import com.application.moment.models.Notifications
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_notifications.*
import kotlinx.android.synthetic.main.layout_bottom_navigation.*
import java.util.HashMap

class NotificationsActivity : AppCompatActivity(), NotificationsAdapter.sendClickListener{

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var adapter: NotificationsAdapter?= null
    private var notificationsFound: MutableList<Notifications> = mutableListOf()
    private var notificationsPaginated: MutableList<Notifications> = mutableListOf()
    private var layoutManager: RecyclerView.LayoutManager? = null

    companion object{
        private const val TAG = "NotificationsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        setupFirebaseAuth()
        setupBottomNavigation()
        setAdapter()
        setupRecycler()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.navigationBarColor = getColor(R.color.colorBottomNavigation)
        }

        seenNotification()
    }


    private fun setupRecycler(){
        notificationsFound.clear()
        val query: Query = myRef
            .child(getString(R.string.dbname_notifications))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    for (singleSnapshot in snapshot.children){
                        val myNotifications = singleSnapshot.getValue(Notifications::class.java)
                        notificationsFound.add(myNotifications!!)
                        displayNotifications()

                    }
                }

            }
            override fun onCancelled(error: DatabaseError) {

            }


        })
    }


    private fun displayNotifications(){
        notificationsPaginated.clear()
        for (i in 0 until notificationsFound.size){
            notificationsPaginated.add(notificationsFound[i])
        }
        notificationsPaginated.reverse()
        adapter!!.notifyDataSetChanged()
    }
    private fun setAdapter(){

        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false)
        notification_recycler.layoutManager = layoutManager

        if (adapter == null){
            adapter = NotificationsAdapter(this, notificationsPaginated ,this)
            notification_recycler.adapter = adapter
        } else{
            adapter!!.notifyDataSetChanged()
        }
    }


    private fun seenNotification() {
        val reference: DatabaseReference = FirebaseDatabase.getInstance().getReference(getString(R.string.dbname_notifications)).child(auth.currentUser!!.uid)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    for (snapshot in dataSnapshot.children) {
                        //val notifications = snapshot.getValue(Notifications::class.java)
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

    private fun setupBottomNavigation(){
        notification.visibility = View.GONE
        ic_notification_active.visibility = View.VISIBLE
        ic_home.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        ic_gallery.setOnClickListener {
            val intent2 = Intent(this, VideoGalleryActivity::class.java)
            startActivity(intent2)
        }

        ic_popular.setOnClickListener {
            val intent3 = Intent(this, PopularActivity::class.java)
            startActivity(intent3)
        }

        ic_profile.setOnClickListener {
            val intent4 = Intent(this, ProfileActivity::class.java)
            startActivity(intent4)
        }
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

    override fun onMessageClickListener(notifications: Notifications) {
        if (notifications.userID == auth.currentUser!!.uid){
            Log.d(TAG, "onMessageClickListener: you can't touch your message")
        }else{
            val intent = Intent(this, ViewProfileActivity::class.java).putExtra(getString(R.string.user_id), notifications.userID)
            startActivity(intent)
        }
    }
}