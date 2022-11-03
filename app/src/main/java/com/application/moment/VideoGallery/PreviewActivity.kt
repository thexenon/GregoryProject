package com.application.moment.VideoGallery

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.application.moment.Home.MainActivity
import com.application.moment.Login.WelcomeActivity
import com.application.moment.Notifications.NotificationsActivity
import com.application.moment.Popular.PopularActivity
import com.application.moment.Profile.ProfileActivity
import com.application.moment.R
import com.application.moment.Utils.ImageManager
import com.application.moment.models.VideoDatabase
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.layout_bottom_navigation.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class PreviewActivity : AppCompatActivity(),
    AdapterView.OnItemSelectedListener{

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    private lateinit var mStorageReference: StorageReference

    //WIDGETS
    private var videoThumbnail : String ? = null

    //VAR
    private var videoCount = 0
    private var listOfItems = arrayOf("Animation", "Art", "Challenge", "Comedy", "Cute", "Dance", "Entertainment", "Food",
    "Gaming", "Health & Fitness", "Lifestyle", "Music", "News & Politics", "Pets", "Prank", "Science & Technology",
    "Sport", "Travel")
    private var spinner: Spinner? = null
    companion object{
        private const val TAG = "PreviewActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mStorageReference = FirebaseStorage.getInstance().reference
        myRef = mFirebaseDatabase.reference
        setupFirebaseAuth()



        startVideo()
        setupBottomNavigation()
        spinner = findViewById(R.id.spinner)
        spinner!!.onItemSelectedListener = this


        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOfItems)//android.R.layout.simple_spinner_item
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner!!.adapter = aa

        val txtLength = findViewById<TextView>(R.id.stringSize)
        val description = findViewById<EditText>(R.id.description)
        description.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = description.text.toString().length
                txtLength.text = "$length / 200"
            }
        })

        val txtTitleLength = findViewById<TextView>(R.id.stringTitleSize)
        val titleVideo = findViewById<EditText>(R.id.title_video)
        titleVideo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = titleVideo.text.toString().length
                txtTitleLength.text = "$length / 40"
            }
        })

        back_arrow.setOnClickListener {
            finish()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.navigationBarColor = getColor(R.color.colorBottomNavigation)
        }
        var counter = 0
        val query: Query = myRef
            .child(getString(R.string.dbname_videos))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    counter++
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


        btn_upload.setOnClickListener {

            if (intent.hasExtra(getString(R.string.video_file))){
                val intentExtra = intent.getStringExtra(getString(R.string.video_file)).toString()

                val uriVideo = Uri.parse("file://$intentExtra")
                addVideoToDatabase(uriVideo.toString(), counter)
                progress_bar.visibility = View.VISIBLE

            }else{
                Toast.makeText(this, "Video not found.", Toast.LENGTH_SHORT).show()
            }
        }


    }



    private fun setupBottomNavigation(){
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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun startVideo(){
        val intentExtra = intent.getStringExtra(getString(R.string.video_file)).toString()
        if (intent.hasExtra(getString(R.string.video_file))){
            Log.d("Intent Extra : ", intentExtra.toString())
            val videoUri = Uri.parse("file://$intentExtra")
            Log.d("Uri : ", videoUri.toString())
            videoView.setOnPreparedListener { mp -> mp.isLooping = true }
            videoView.setVideoURI(videoUri)
            videoView.start()
        }

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    }

    /**
     *                                                  FIREBASE
     */

    //  CHECK CURRENT USER

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

        myRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                videoCount = getVideoCount(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {

            }


        })

    }

    // FIREBASE STORAGE -> UPLOAD

    /**
     * Add the photo on the Firebase Storage
     */

    private fun addVideoToDatabase(videoUri: String, counter: Int){
        Log.d(TAG, "addVideoToDatabase: attempting to upload a new video.")
        val storageReference: StorageReference = mStorageReference
            .child("USERS_STORAGE" + "/" + auth.currentUser!!.uid + "/VIDEOS" + (counter+1))

        uploadThumbnail(counter)
        val uploadTask: UploadTask?
        uploadTask = storageReference.putFile(Uri.parse(videoUri))
        uploadTask.addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot?> {
            Toast.makeText(this@PreviewActivity, "Video upload success", Toast.LENGTH_SHORT).show()

            storageReference.downloadUrl
                .addOnSuccessListener { uri -> setVideo(uri.toString())
                    progress_bar.visibility = View.GONE}
        }).addOnFailureListener {
            Log.d(TAG, "onFailure: Video upload failed.")
            Toast.makeText(this@PreviewActivity, "Video upload failed ", Toast.LENGTH_SHORT).show()
        }.addOnProgressListener { }




    }

    private fun getVideoCount(dataSnapshot: DataSnapshot): Int {
        var count = 0
        for (ds in dataSnapshot
            .child(getString(R.string.dbname_videos))
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .children) {
            count++
        }
        return count
    }

    @Suppress("deprecation", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun uploadThumbnail(counter: Int){

        val storageReference: StorageReference = mStorageReference
            .child("USERS_STORAGE" + "/" + auth.currentUser!!.uid + "/THUMBNAIL" +(counter+1))


        val file = File(intent.getStringExtra(getString(R.string.video_file))).toString()

        val bm: Bitmap? = ThumbnailUtils.createVideoThumbnail(file , MediaStore.Video.Thumbnails.FULL_SCREEN_KIND)

        val bytes = ImageManager.getBytesFromBitmap(bm, 100)
        val uploadTask: UploadTask?
        uploadTask = storageReference.putBytes(bytes)
        uploadTask.addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot?> {
            storageReference.downloadUrl
                .addOnSuccessListener { uri -> videoThumbnail = (uri.toString()) }
        }).addOnFailureListener {
            Log.d(TAG, "onFailure: Thumbnail upload failed.")
        }.addOnProgressListener { }

    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun setVideo(videoUrl: String){
        myRef.child(getString(R.string.dbname_videos)).push().key
        val duration = intent.getStringExtra(getString(R.string.video_duration)).toString().toString()
        val category = spinner?.selectedItem.toString()
        val newVideoKey = myRef.child(getString(R.string.dbname_videos)).push().key
        val inputVideoDatabase = VideoDatabase(auth.currentUser!!.uid, videoUrl, videoThumbnail.toString(), newVideoKey.toString(), category, getTimestamp().toString(),
            duration, title_video.text.toString(), description.text.toString())

        myRef.child(getString(R.string.dbname_videos))
            .child(auth.currentUser!!.uid)
            .child(newVideoKey.toString())
            .setValue(inputVideoDatabase)

        myRef.child(getString(R.string.dbname_categories))
            .child(category)
            .child(newVideoKey.toString())
            .setValue(inputVideoDatabase)

        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }


    private fun getTimestamp(): String? {

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = TimeZone.getTimeZone("Canada/Pacific")
        return sdf.format(Date())
    }


}