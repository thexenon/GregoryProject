package com.application.moment.Search

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.Home.HomeFragment
import com.application.moment.Login.WelcomeActivity
import com.application.moment.Notifications.NotificationsActivity
import com.application.moment.Popular.PopularActivity
import com.application.moment.Profile.ProfileActivity
import com.application.moment.R
import com.application.moment.Utils.Adapter.SearchRecyclerAdapter
import com.application.moment.VideoGallery.VideoGalleryActivity
import com.application.moment.models.Notifications
import com.application.moment.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.layout_bell_active.*
import kotlinx.android.synthetic.main.layout_bottom_navigation.*
import java.util.*


class SearchActivity : AppCompatActivity(), SearchRecyclerAdapter.SendClickListener{
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var adapter: SearchRecyclerAdapter?= null
    private var userInfo: MutableList<User> = mutableListOf()
    private var layoutManager: RecyclerView.LayoutManager? = null

    companion object{
        private const val TAG = "SearchActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        setupFirebaseAuth()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.navigationBarColor = getColor(R.color.colorBottomNavigation)
        }

        back_arrow.setOnClickListener {

            finish()
        }
        setupBottomNavigation()
        setAdapter()

        val searchInput = findViewById<EditText>(R.id.search)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val text = searchInput.text.toString().toLowerCase(Locale.getDefault())
                searchUsers(text)
            }
        })
    }

    private fun searchUsers(keyword: String){
        userInfo.clear()
        if (keyword.isNotEmpty()) {
            val query: Query = myRef.child(getString(R.string.dbname_users))
                .orderByChild(getString(R.string.username))
                .startAt(keyword)
                .endAt(keyword + "\uf8ff")
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (singleSnapshot in dataSnapshot.children) {
                        val userID = singleSnapshot.key
                        Log.d(TAG, "onDataChange: userID: $userID")
                        if (userID != null){
                            val query2: Query = myRef.child(getString(R.string.dbname_users))
                                .child(userID)
                            query2.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {

                                    val username = dataSnapshot.child(getString(R.string.username)).value
                                    val profilePhoto = dataSnapshot.child(getString(R.string.profile_photo)).value
                                    val displayname = dataSnapshot.child(getString(R.string.display_name)).value

                                    val user = User(userID, username.toString(), displayname.toString(), "",  profilePhoto.toString())
                                    userInfo.add(user)

                                    Log.d(TAG, "onDataChange: user info:$user")
                                    adapter!!.notifyDataSetChanged()

                                }

                                override fun onCancelled(databaseError: DatabaseError) {}
                            })
                        }

                    }


                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }


    }



    private fun setAdapter(){

        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false)
        search_recycler_view.layoutManager = layoutManager

        if (adapter == null){
            adapter = SearchRecyclerAdapter(this, userInfo, auth.currentUser!!.uid,this)
            search_recycler_view.adapter = adapter
        } else{
            adapter!!.notifyDataSetChanged()
        }
    }

    private fun setupBottomNavigation(){
        val reference = FirebaseDatabase.getInstance().getReference(getString(R.string.dbname_notifications)).child(auth.currentUser!!.uid)
        reference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    var unread = 0
                    for (snapshot in dataSnapshot.children) {
                        val notif = snapshot.getValue(Notifications::class.java)!!
                        if(!notif.seen){
                            unread++
                        }

                    }
                    if (unread == 0) {
                        notification.visibility = View.VISIBLE
                        bell_active.visibility = View.GONE
                    } else if (unread <= 99){
                        notification.visibility = View.GONE
                        bell_active.visibility = View.VISIBLE
                        text_notseen.text = "$unread"
                        bell_active.setOnClickListener {
                            val intent3 = Intent(this@SearchActivity, NotificationsActivity::class.java)
                            startActivity(intent3)
                        }
                    }else{
                        notification.visibility = View.GONE
                        bell_active.visibility = View.VISIBLE
                        text_notseen.text = "+99"
                        bell_active.setOnClickListener {
                            val intent3 = Intent(this@SearchActivity, NotificationsActivity::class.java)
                            startActivity(intent3)
                        }
                    }
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        ic_home.visibility = View.GONE
        ic_home_active.visibility = View.VISIBLE
        ic_popular.setOnClickListener {
            val intent = Intent(this, PopularActivity::class.java)
            startActivity(intent)
        }
        ic_gallery.setOnClickListener {
            val intent2 = Intent(this, VideoGalleryActivity::class.java)
            startActivity(intent2)
        }

        notification.setOnClickListener {
            val intent3 = Intent(this, NotificationsActivity::class.java)
            startActivity(intent3)
        }

        ic_profile.setOnClickListener {
            val intent4 = Intent(this, ProfileActivity::class.java)
            startActivity(intent4)
        }
    }

    override fun onProfileClickListener(user: User) {
        if (user.userID == auth.currentUser!!.uid){
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }else{
            val intent = Intent(this, ViewProfileActivity::class.java).putExtra(getString(R.string.user_id), user.userID)
            startActivity(intent)
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

    override fun onResume() {
        super.onResume()
        HomeFragment().pauseVideo()
    }

}