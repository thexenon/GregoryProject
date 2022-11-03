package com.application.moment.Home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.application.moment.Login.WelcomeActivity
import com.application.moment.R
import com.application.moment.Utils.Adapter.HomeRecyclerAdapter
import com.application.moment.Utils.Adapter.ViewPagerAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_item_view_video.*


class MainActivity : AppCompatActivity(), HomeRecyclerAdapter.OnLoadMoreItemsListener{
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private var mAuthListener: AuthStateListener? = null
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase


    companion object{
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        setupFirebaseAuth()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.navigationBarColor = getColor(R.color.colorBottomNavigation)
        }



    }




    private fun setupViewPager(){
        val fragmentList = arrayListOf(
            MessagesFragment(),
            HomeFragment(),
            CameraFragment()
        )

        val adapter = ViewPagerAdapter(
            fragmentList,
            this.supportFragmentManager,
            lifecycle
        )

        pager.adapter = adapter

        val limit = if (adapter.itemCount > 1) adapter.itemCount - 1 else 1
        val intentExtra = intent.getStringExtra(getString(R.string.message_fragment))
        val intentExtra2 = intent.getStringExtra(getString(R.string.camera_fragment))

        when {
            intentExtra != null -> {
                pager.setCurrentItem(0, false)
                HomeFragment().pauseVideo()
            }
            intentExtra2 != null -> {
                pager.setCurrentItem(2, false)
                HomeFragment().pauseVideo()
            }
            else -> {
                pager.setCurrentItem(1, false)
            }
        }
        pager.offscreenPageLimit = limit



    }
    override fun onLoadMoreItems() {
        Log.d(TAG, "onLoadMoreItems: displaying more videos")

        val fragment =  supportFragmentManager.findFragmentByTag("f" + pager.currentItem)
        if ( fragment is HomeFragment)
        {
            Log.d(TAG, "onLoadMoreItems: success")

            fragment.displayMoreVideos()
        }
        Log.d(TAG, "onLoadMoreItems: fragment =$fragment")
    }

    override fun onNextVideo(position: Int) {

        val fragment =  supportFragmentManager.findFragmentByTag("f" + pager.currentItem)
        if ( fragment is HomeFragment)
        {
            Log.d(TAG, "onLoadMoreItems: success")

            fragment.nextVideo(position)
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
        mAuthListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser

            //check if the user is logged in
            checkCurrentUser(user)
            if (user != null) {
                setupViewPager()
                // User is signed in
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid)
            } else {
                // User is signed out
                Log.d(TAG, "onAuthStateChanged:signed_out")
                val intent = Intent(this, WelcomeActivity::class.java)
                startActivity(intent)
                finish()
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
    }

    override fun onPause() {
        super.onPause()
        if(player_view != null){
            player_view.player?.playWhenReady = false
        }

    }



}