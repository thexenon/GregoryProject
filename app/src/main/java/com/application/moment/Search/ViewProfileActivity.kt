package com.application.moment.Search

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.Dialogs.BottomSheetViewProfile
import com.application.moment.Home.MainActivity
import com.application.moment.Login.WelcomeActivity
import com.application.moment.Notifications.NotificationsActivity
import com.application.moment.Popular.PopularActivity
import com.application.moment.Profile.VideoActivity
import com.application.moment.R
import com.application.moment.Utils.Adapter.ViewProfileAdapter
import com.application.moment.VideoGallery.VideoGalleryActivity
import com.application.moment.models.Notifications
import com.application.moment.models.User
import com.application.moment.models.VideoDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_view_profile.*
import kotlinx.android.synthetic.main.layout_bell_active.*
import kotlinx.android.synthetic.main.layout_bottom_navigation.*
import java.util.HashMap

class ViewProfileActivity : AppCompatActivity(), ViewProfileAdapter.SendClickListener{


    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var adapter: ViewProfileAdapter?= null
    private var userVideos: MutableList<VideoDatabase> = mutableListOf()
    private var userInfo: MutableList<User> = mutableListOf()
    private var layoutManager: RecyclerView.LayoutManager? = null

    //VAR
    private var userID: String = ""

    companion object{
        private const val TAG = "ViewProfileActivity"
    }


    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_profile)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        setupFirebaseAuth()
        setupBottomNavigation()

        userID = intent.getStringExtra(getString(R.string.user_id)).toString()
        setAdapter()
        setupRecycler()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.navigationBarColor = getColor(R.color.colorBottomNavigation)
        }

        refresh.setOnRefreshListener {
            setupRecycler()
            refresh.isRefreshing = false
        }
    }

    @Suppress("unchecked_cast")
    private fun setupRecycler(){
        userVideos.clear()
        userInfo.clear()

        val query2: Query = myRef
            .child(getString(R.string.dbname_users))
            .child(userID)
        query2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val displayname = dataSnapshot.child(getString(R.string.display_name)).value
                val username = dataSnapshot.child(getString(R.string.username)).value
                val profilePhoto = dataSnapshot.child(getString(R.string.profile_photo)).value


                val user = User(userID, username.toString(), displayname.toString(), "", profilePhoto.toString())
                Log.d(TAG, "onDataChange: user info:$user")
                userInfo.add(user)
                adapter!!.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


        val query: Query = myRef
            .child(getString(R.string.dbname_videos))
            .child(userID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {

                    val objectMap =
                        singleSnapshot.value as HashMap<String?, Any?>?
                    val userID = objectMap?.get(getString(R.string.user_id)).toString()
                    val videoPath = objectMap?.get(getString(R.string.video_path)).toString()
                    val videoID = objectMap?.get(getString(R.string.video_id)).toString()
                    val description = objectMap?.get(getString(R.string.description)).toString()
                    val dateCreated = objectMap?.get(getString(R.string.date_created)).toString()
                    val category = objectMap?.get(getString(R.string.category)).toString()
                    val thumbnail = objectMap?.get(getString(R.string.video_thumbnail)).toString()
                    val title = objectMap?.get(getString(R.string.title)).toString()
                    val duration = objectMap?.get(getString(R.string.video_duration)).toString()

                    val video = VideoDatabase(userID, videoPath, thumbnail, videoID, category, dateCreated, duration, title, description)
                    Log.d(TAG, "onDataChange: video info:$video")
                    userVideos.add(video)
                    adapter!!.notifyDataSetChanged()
                }

                userVideos.reverse()

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })



    }



    private fun setAdapter(){

        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false)
        view_profile_recycler.layoutManager = layoutManager

        if (adapter == null){
            adapter = ViewProfileAdapter(this,userVideos, userInfo,this, this)
            view_profile_recycler.adapter = adapter
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
                    when {
                        unread == 0 -> {
                            notification.visibility = View.VISIBLE
                            bell_active.visibility = View.GONE
                        }
                        unread <= 99 -> {
                            notification.visibility = View.GONE
                            bell_active.visibility = View.VISIBLE
                            text_notseen.text = "$unread"
                            bell_active.setOnClickListener {
                                val intent3 = Intent(this@ViewProfileActivity, NotificationsActivity::class.java)
                                startActivity(intent3)
                            }
                        }
                        else -> {
                            notification.visibility = View.GONE
                            bell_active.visibility = View.VISIBLE
                            text_notseen.text = "+99"
                            bell_active.setOnClickListener {
                                val intent3 = Intent(this@ViewProfileActivity, NotificationsActivity::class.java)
                                startActivity(intent3)
                            }
                        }
                    }
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        ic_profile.visibility = View.GONE
        ic_profile_active.visibility = View.VISIBLE
        ic_home.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
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

        ic_popular.setOnClickListener {
            val intent4 = Intent(this, PopularActivity::class.java)
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

                //recyclerAdapter()
            } else {
                // User is signed out
                Log.d(TAG, "onAuthStateChanged:signed_out")
            }
            // ...
        }

        myRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
            }
            override fun onCancelled(error: DatabaseError) {

            }


        })

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

    override fun onVideoClickListener(video: VideoDatabase) {
        val intent = Intent(this, VideoActivity::class.java).putExtra(getString(R.string.video_id), video.video_id)
            .putExtra(getString(R.string.user_id), video.user_id)
        startActivity(intent)
    }

    override fun onMoreInfoClickListener(video: VideoDatabase) {
        val bottomSheet = BottomSheetViewProfile()
        val args = Bundle()
        args.putString(getString(R.string.video_id), video.video_id)
        args.putString(getString(R.string.video_path), video.video_path)
        args.putString(getString(R.string.user_id), video.user_id)
        args.putString(getString(R.string.title), video.title)
        args.putString(getString(R.string.description), video.description)
        bottomSheet.arguments = args
        //bottomSheet.dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        bottomSheet.show(supportFragmentManager, video.video_id)


    }
}