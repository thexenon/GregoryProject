package com.application.moment.Settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.Login.WelcomeActivity
import com.application.moment.R
import com.application.moment.Utils.ImageManager
import com.application.moment.Utils.Adapter.PhotoGalleryRecyclerAdapter
import com.application.moment.Utils.SpacesItemDecoration
import com.application.moment.models.Photo
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_photo.*
import kotlinx.android.synthetic.main.activity_video_gallery.galleryRV
import java.io.File

class PhotoActivity : AppCompatActivity(), PhotoGalleryRecyclerAdapter.sendClickListener{

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    private lateinit var mStorageReference: StorageReference

    // BOOLEAN
    private var checkPermission: Boolean = false

    // WIDGETS
    private var adapter: PhotoGalleryRecyclerAdapter? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var photos: MutableList<Photo> = mutableListOf()

    companion object{
        private const val WRITE_REQUEST_CODE = 101
        private const val TAG = "PhotoActivity"

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mStorageReference = FirebaseStorage.getInstance().reference
        myRef = mFirebaseDatabase.reference
        setupFirebaseAuth()
        getNumberOfColumns()
        requestPermission()

        back_arrow.setOnClickListener {
            finish()
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
            adapter = PhotoGalleryRecyclerAdapter(this,photos,newMeasuresPixel.toInt(),this)
            galleryRV.adapter = adapter
        } else{
            adapter!!.notifyDataSetChanged()
        }

    }

    private fun requestPermission(){
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission == PackageManager.PERMISSION_GRANTED){
            checkPermission = true
            //val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            //videosFolder = File(path,videosFolderName)
            //videosFolder!!.mkdirs()
            getAllPhotosFromGallery()
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
    private fun getAllPhotosFromGallery(){
        photos.clear()
        val projection = arrayOf(
            MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID, MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.WIDTH)

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
            null, null , null
        )

        if (cursor!!.moveToFirst()){
            do{
                val photoPaths = cursor.getString(0)
                val photoID = cursor.getInt(1)
                val photoH = cursor.getInt(2)
                val photoW = cursor.getInt(3)
                val photo = Photo(
                    Uri.parse(photoPaths),
                    photoID,
                    photoH,
                    photoW
                )
                photos.add(photo)
            } while (cursor.moveToNext())
        }

        photos.reverse()
        adapter!!.notifyDataSetChanged()
        cursor.close()
    }

    override fun onPhotoClickListener(photo: Photo) {
        selectPhoto(photo)
    }

    private fun selectPhoto(photo: Photo){

        Glide
            .with(this)
            .asBitmap()
            .load(Uri.fromFile(File(photo.uri.path.toString())))
            .into(profile_photo)

        save.setOnClickListener {
            Log.d(TAG, "selectPhoto: uri =" + photo.uri.toString())
            addPhotoToDatabase(Uri.parse(photo.uri.toString()).toString())
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
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

    /**
     * Add the photo on the Firebase Storage
     */

    private fun addPhotoToDatabase(imgUrl: String){
        Log.d(TAG, "uploadNewPhoto: attempting to upload new photo.")

        Log.d(TAG, "uploadNewPhoto: uploading new PROFILE photo")
        val storageReference: StorageReference = mStorageReference
            .child("USERS_STORAGE" + "/" + auth.currentUser!!.uid + "/profile_photo")


        //convert image url to bitmap
        val bm: Bitmap? = ImageManager.getBitmap(imgUrl)
        val bytes: ByteArray = ImageManager.getBytesFromBitmap(bm, 100)
        val uploadTask: UploadTask?
        uploadTask = storageReference.putBytes(bytes)
        uploadTask.addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot?> { //Task<Uri> firebaseUrl = taskSnapshot.getStorage().getDownloadUrl();
            Toast.makeText(this@PhotoActivity, "photo upload success", Toast.LENGTH_SHORT).show()

            storageReference.downloadUrl
                .addOnSuccessListener { uri -> setProfilePhoto(uri.toString()) }
        }).addOnFailureListener {
            Log.d(TAG, "onFailure: Photo upload failed.")
            Toast.makeText(this@PhotoActivity, "Photo upload failed ", Toast.LENGTH_SHORT).show()
        }.addOnProgressListener { }


    }

    private fun setProfilePhoto(imgUrl : String){
        myRef.child(getString(R.string.dbname_users))
            .child(auth.currentUser!!.uid)
            .child(getString(R.string.profile_photo))
            .removeValue()

        myRef.child(getString(R.string.dbname_users))
            .child(auth.currentUser!!.uid)
            .child(getString(R.string.profile_photo))
            .setValue(imgUrl)

        Log.d(TAG, "setProfilePhoto: photo url set on the firebase database")
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