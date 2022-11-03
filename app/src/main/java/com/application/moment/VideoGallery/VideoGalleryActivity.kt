package com.application.moment.VideoGallery

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.Home.MainActivity
import com.application.moment.Login.WelcomeActivity
import com.application.moment.Notifications.NotificationsActivity
import com.application.moment.Popular.PopularActivity
import com.application.moment.Profile.ProfileActivity
import com.application.moment.R
import com.application.moment.Utils.Adapter.GalleryRecyclerAdapter
import com.application.moment.Utils.SpacesItemDecoration
import com.application.moment.models.Notifications
import com.application.moment.models.Video
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_video_gallery.*
import kotlinx.android.synthetic.main.layout_bell_active.*
import kotlinx.android.synthetic.main.layout_bottom_navigation.*

class VideoGalleryActivity : AppCompatActivity(),
    GalleryRecyclerAdapter.SendClickListener{
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    // PERMISSIONS
    private var checkPermission: Boolean = false


    // WIDGETS
    private var adapter: GalleryRecyclerAdapter? = null
    private var layoutManager: RecyclerView.LayoutManager? = null

    //VAR
    private var videos: MutableList<Video> = mutableListOf()

    companion object{
        private const val TAG = "VideoGalleryActivity"
        private const val WRITE_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_gallery)
        auth = Firebase.auth
        setupFirebaseAuth()
        setupBottomNavigation()
        getNumberOfColumns()
        requestPermission()


        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        scrollView.smoothScrollTo(0, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.navigationBarColor = getColor(R.color.colorBottomNavigation)
        }
    }

    private fun getNumberOfColumns(){
        val width = resources.displayMetrics.widthPixels
        val dpWith = width / resources.displayMetrics.density
        val scale = 100
        val numOfColumns: Int = (dpWith / scale).toInt()
        val newMeasuresDp = (dpWith / numOfColumns)
        val newMeasuresPixel = newMeasuresDp * resources.displayMetrics.density
        val newPadding = (newMeasuresPixel * 0.05)

        //layoutManager = GridLayoutManager(this,numOfColumns)
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false)
        galleryRV.layoutManager = layoutManager
        galleryRV.addItemDecoration(SpacesItemDecoration(newPadding.toInt(), 100))

        if (adapter == null){
            adapter = GalleryRecyclerAdapter(this,videos,newMeasuresPixel.toInt(),this)
            galleryRV.adapter = adapter
        } else{
            adapter!!.notifyDataSetChanged()
        }

    }

    private fun requestPermission(){
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission == PackageManager.PERMISSION_GRANTED){
            checkPermission = true
            getAllVideosFromGallery()
        } else {
            checkPermission = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_REQUEST_CODE)
            }
        }
    }

    @Suppress("deprecation")
    private fun getAllVideosFromGallery(){
        videos.clear()
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID, MediaStore.Video.Media.HEIGHT,MediaStore.Video.Media.WIDTH, MediaStore.Video.Media.DURATION)
        } else {
            arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID, MediaStore.Video.Media.HEIGHT,MediaStore.Video.Media.WIDTH, "0")
        }

        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
            null, null , null
        )

        if (cursor!!.moveToFirst()){
            do{
                val videoPath = cursor.getString(0)
                val videoID = cursor.getInt(1)
                val videoH = cursor.getInt(2)
                val videoW = cursor.getInt(3)
                val duration = cursor.getInt(4)
                val video = Video(
                    videoPath,
                    Uri.parse(videoPath),
                    videoID,
                    videoH,
                    videoW,
                    duration
                )
                videos.add(video)
            } while (cursor.moveToNext())
        }

        videos.reverse()
        adapter!!.notifyDataSetChanged()
        cursor.close()
    }

    override fun onVideoClickListener(video: Video) {
        startVideo(video)
    }

    private fun startVideo(video: Video){
        val videoView : VideoView = findViewById(R.id.videoView)
        videoView.setOnPreparedListener { mp -> mp.isLooping = true }
        videoView.setVideoURI(video.uri)
        videoView.start()
        ready.setOnClickListener {
            if (video.duration < 60000){
                val intent = Intent(this, PreviewActivity::class.java)
                intent.putExtra(getString(R.string.video_file), video.path)
                intent.putExtra(getString(R.string.video_duration), (video.duration / 1000).toString())
                startActivity(intent)
            }else{
                Toast.makeText(this, "Your video should be under or equal \n  to 60 seconds", Toast.LENGTH_SHORT).show()
            }

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
                                val intent3 = Intent(this@VideoGalleryActivity, NotificationsActivity::class.java)
                                startActivity(intent3)
                            }
                        }
                        else -> {
                            notification.visibility = View.GONE
                            bell_active.visibility = View.VISIBLE
                            text_notseen.text = "+99"
                            bell_active.setOnClickListener {
                                val intent3 = Intent(this@VideoGalleryActivity, NotificationsActivity::class.java)
                                startActivity(intent3)
                            }
                        }
                    }
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        ic_gallery.visibility = View.GONE
        ic_gallery_active.visibility = View.VISIBLE
        ic_home.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        ic_popular.setOnClickListener {
            val intent2 = Intent(this, PopularActivity::class.java)
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
    }



}