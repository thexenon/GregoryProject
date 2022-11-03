package com.application.moment.Settings

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.application.moment.Login.WelcomeActivity
import com.application.moment.Profile.ProfileActivity
import com.application.moment.R
import com.application.moment.Reports.ReportsFragment
import com.application.moment.models.Chat
import com.application.moment.models.Report
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_settings.*
import java.lang.IllegalStateException

class SettingsActivity : AppCompatActivity(){

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    private var isAdmin = false

    companion object{
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference

        setupFirebaseAuth()
        //checkIfAdmin()
        setupListInformation()
        checkIfAdmin()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.navigationBarColor = getColor(R.color.colorBottomNavigation)
        }



    }
    private fun checkIfAdmin(){
        val query: Query = myRef
            .child(getString(R.string.dbname_admin))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    isAdmin = true
                    reports.visibility = View.VISIBLE
                    getReportCounter()
                }else{
                    isAdmin = false
                    reports.visibility = View.GONE

                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getReportCounter(){
        val reference = FirebaseDatabase.getInstance().getReference(getString(R.string.dbname_report))
        reference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    var unread = 0
                    for (snapshot in dataSnapshot.children) {
                        val report = snapshot.getValue(Report::class.java)!!
                        if (!report.seen) {
                            unread++
                        }
                    }
                    if (unread == 0) {
                        tv_report.text = "REPORT"
                    } else {
                        tv_report.text = "REPORT ($unread)"
                    }

                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
    @Suppress("deprecation")
    private fun setupListInformation(){
        at.text = "@"
        val query: Query = myRef
            .child(getString(R.string.dbname_users))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val usernameF = dataSnapshot.child(getString(R.string.username)).value
                    val displaynameF = dataSnapshot.child(getString(R.string.display_name)).value
                    val profilephotoF = dataSnapshot.child(getString(R.string.profile_photo)).value
                    val emailF = dataSnapshot.child(getString(R.string.email)).value
                    display_name.text = displaynameF.toString()
                    username.text = usernameF.toString()
                    email.text = emailF.toString()

                    Glide
                        .with(this@SettingsActivity)
                        .load(Uri.parse(profilephotoF.toString()))
                        .into(change_profile_photo)
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

        relLayout2.setOnClickListener{
            val intent = Fragment.instantiate(this,
                UsernameFragment::class.java.name
            ) as UsernameFragment
            supportFragmentManager.beginTransaction().replace(R.id.container, intent).commit()
        }

        relLayout3.setOnClickListener{
            val intent = Fragment.instantiate(this,
                NameFragment::class.java.name
            ) as NameFragment
            supportFragmentManager.beginTransaction().replace(R.id.container, intent).commit()
        }

        relLayout6.setOnClickListener {
            val intent = Intent(this, PhotoActivity::class.java)
            startActivity(intent)
        }
        relLayout7.setOnClickListener {
            val uri = Uri.parse("https://momentapplication.com/terms.html")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
        relLayout8.setOnClickListener {
            val uri = Uri.parse("https://momentapplication.com/privacy.html")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
        relLayout9.setOnClickListener {
            val uri = Uri.parse("https://momentapplication.com/support.html")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        sign_out.setOnClickListener{
            val intent = Fragment.instantiate(this,
                SignOutFragment::class.java.name
            ) as SignOutFragment
            supportFragmentManager.beginTransaction().replace(R.id.container, intent).commit()

        }
        back_arrow.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        remove_account.setOnClickListener{
            val intent = Fragment.instantiate(this,
                RemoveAccountFragment::class.java.name
            ) as RemoveAccountFragment
            supportFragmentManager.beginTransaction().replace(R.id.container, intent).commit()
        }

        reports.setOnClickListener{
            val intent = Fragment.instantiate(this,
                ReportsFragment::class.java.name
            ) as ReportsFragment
            supportFragmentManager.beginTransaction().replace(R.id.container, intent).commit()
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