package com.application.moment.Popular

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.Dialogs.BottomSheet
import com.application.moment.Dialogs.BottomSheetViewProfile
import com.application.moment.Home.MainActivity
import com.application.moment.Login.WelcomeActivity
import com.application.moment.Notifications.NotificationsActivity
import com.application.moment.Profile.ProfileActivity
import com.application.moment.R
import com.application.moment.Utils.Adapter.PopularRecyclerAdapter
import com.application.moment.VideoGallery.VideoGalleryActivity
import com.application.moment.models.Notifications
import com.application.moment.models.VideoDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_popular.*
import kotlinx.android.synthetic.main.layout_bell_active.*
import kotlinx.android.synthetic.main.layout_bottom_navigation.*
import kotlinx.android.synthetic.main.layout_item_view_video.*

class PopularActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, PopularRecyclerAdapter.SendClickListener, PopularRecyclerAdapter.OnLoadMoreItemsListener{

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var adapter: PopularRecyclerAdapter?= null
    private var spinner: Spinner? = null
    private var mVideos = mutableListOf<VideoDatabase>()
    private var mPaginatedVideo = mutableListOf<VideoDatabase>()
    private var layoutManager: RecyclerView.LayoutManager? = null

    //VAR
    private var listOfItems = arrayOf("Animation", "Art", "Challenge", "Comedy", "Cute", "Dance", "Entertainment", "Food",
        "Gaming", "Health & Fitness", "Lifestyle", "Music", "News & Politics", "Pets", "Prank", "Science & Technology",
        "Sport", "Travel")
    private var myItem = ""
    private var mResults = 0


    companion object{
        private const val TAG = "PopularActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        setupFirebaseAuth()
        setupBottomNavigation()
        spinner = findViewById(R.id.spinner)
        spinner!!.onItemSelectedListener = this
        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOfItems)
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner!!.adapter = aa
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.navigationBarColor = getColor(R.color.colorBottomNavigation)
        }

        myItem = spinner?.selectedItem.toString()

        setAdapter()


        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(popular_recycler)
        snapHelper.findSnapView(layoutManager)

        refresh.setOnRefreshListener {
            setupRecycler()
            refresh.isRefreshing = false
        }

    }

    private fun setAdapter(){

        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false)
        popular_recycler.layoutManager = layoutManager

        if (adapter == null){
            adapter = PopularRecyclerAdapter(this, mVideos, this)
            popular_recycler.adapter = adapter
        } else{
            adapter!!.notifyDataSetChanged()
        }
    }

    private fun setupRecycler(){
        mVideos.clear()
        val query: Query = myRef
            .child(getString(R.string.dbname_categories))
            .child(spinner?.selectedItem.toString())
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (singleSnapshot in snapshot.children) {
                    val videoGet = singleSnapshot.getValue(VideoDatabase::class.java)


                    val video = VideoDatabase(videoGet!!.user_id, videoGet.video_path, videoGet.video_thumbnail,
                        videoGet.video_id, videoGet.category, videoGet.date_created, videoGet.duration, videoGet.title, videoGet.description)
                    Log.d(TAG, "onDataChange: video info :$video")
                    mVideos.add(video)
                    displayVideo()


                }

            }
            override fun onCancelled(error: DatabaseError) {

            }
        })

        adapter!!.notifyDataSetChanged()
    }

    private fun displayVideo(){
        mPaginatedVideo.clear()
        try {
            mVideos.sortWith(Comparator { o1, o2 -> o2!!.date_created.compareTo(o1!!.date_created) })


            var iterations: Int = mVideos.size

            if (iterations > 5) {
                iterations = 5
            }

            mResults = 5
            for (i in 0 until iterations) {
                mPaginatedVideo.add(mVideos[i])
            }



            adapter!!.notifyDataSetChanged()

        } catch (e: NullPointerException) {
            Log.e(TAG, "displayVideos: NullPointerException", e)
        } catch (ind: IndexOutOfBoundsException) {
            Log.e(TAG, "displayVideos: IndexOutOfBoundsException", ind)
        }

    }


    fun displayMoreVideos() {

        Log.d(
            TAG, "displayMoreVideos: myFollowingVideos size = $mVideos ," +
                "mResults = $mResults ")
        try {
            if (mVideos.size > mResults && mVideos.size > 0) {
                val iterations: Int = if (mVideos.size  > mResults + 5) {
                    Log.d(TAG, "displayMoreVideos: there are greater than 5 more videos")
                    5
                } else {
                    Log.d(TAG, "displayMoreVideos: there is less than 5 more videos")
                    mVideos.size  - mResults
                }

                //add the new videos to the paginated results
                for (i in mResults until mResults + iterations) {
                    mPaginatedVideo.add(mVideos[i])
                }
                mResults += iterations

                popular_recycler.post {
                    kotlin.run {
                        adapter!!.notifyDataSetChanged()
                    }
                }
            }
        } catch (e: java.lang.NullPointerException) {
            Log.e(TAG, "displayMoreVideos: NullPointerException: " + e.message)
        } catch (e: java.lang.IndexOutOfBoundsException) {
            Log.e(TAG, "displayMoreVideos: IndexOutOfBoundsException: " + e.message)
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
                                val intent3 = Intent(this@PopularActivity, NotificationsActivity::class.java)
                                startActivity(intent3)
                            }
                        }
                        else -> {
                            notification.visibility = View.GONE
                            bell_active.visibility = View.VISIBLE
                            text_notseen.text = "+99"
                            bell_active.setOnClickListener {
                                val intent3 = Intent(this@PopularActivity, NotificationsActivity::class.java)
                                startActivity(intent3)
                            }
                        }
                    }
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        ic_popular.visibility = View.GONE
        ic_popular_active.visibility = View.VISIBLE
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
        if(player_view != null){
            player_view.player?.playWhenReady = false
            player_view.player?.stop()
        }
        if(adapter != null){
            adapter!!.pauseVideo()
        }

    }

    override fun onPause() {
        super.onPause()

        if(adapter != null){
            adapter!!.pauseVideo()
        }

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        setupRecycler()
    }

    override fun onVideoClickListener(myFollowingVideo: VideoDatabase) {

    }

    override fun onMoreClickListener(video: VideoDatabase) {
        if(video.user_id != auth.currentUser!!.uid){
            val bottomSheet = BottomSheetViewProfile()
            val args = Bundle()
            args.putString(getString(R.string.video_id), video.video_id)
            args.putString(getString(R.string.video_path), video.video_path)
            args.putString(getString(R.string.user_id), video.user_id)
            args.putString(getString(R.string.title), video.title)
            args.putString(getString(R.string.description), video.description)
            bottomSheet.arguments = args
            bottomSheet.show(supportFragmentManager, video.video_id)
        }else{
            val bottomSheet = BottomSheet()
            val args = Bundle()
            args.putString(getString(R.string.video_id), video.video_id)
            args.putString(getString(R.string.category), video.category)
            bottomSheet.arguments = args
            bottomSheet.show(supportFragmentManager, video.video_id)
        }

    }

    override fun onLoadMoreItems() {
        displayMoreVideos()
    }

    override fun onNextVideo(position: Int) {
        popular_recycler?.smoothScrollToPosition(position)
        Log.d(TAG, "nextVideo: scroll to position: $position")
    }

}