package com.application.moment.Login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.application.moment.Home.MainActivity
import com.application.moment.R
import com.application.moment.Utils.StringManipulation
import com.application.moment.models.Followers
import com.application.moment.models.Following
import com.application.moment.models.MessageOrder
import com.application.moment.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_register.*
import java.text.SimpleDateFormat
import java.util.*


class RegisterActivity : AppCompatActivity(){

    //Firebase
    private lateinit var auth : FirebaseAuth
    private lateinit var refUsers : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    //var
    private var userID : String = ""

    //boolean
    private var usernameExist = false

    companion object{
        private const val TAG = "RegisterActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //SETUP FIREBASE STUFF
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        refUsers = mFirebaseDatabase.reference

        setupWidgets()

    }

    private fun setupWidgets(){
        val icCheck = findViewById<ImageView>(R.id.check)
        val notCheck = findViewById<ImageView>(R.id.check_empty)
        val btnSignUp = findViewById<Button>(R.id.sign_up)

        //Check box
        notCheck.setOnClickListener {
            notCheck.visibility = View.GONE
            icCheck.visibility = View.VISIBLE
        }

        icCheck.setOnClickListener {
            icCheck.visibility = View.GONE
            notCheck.visibility = View.VISIBLE

        }
        btnSignUp.setOnClickListener{
            if (icCheck.visibility == View.VISIBLE){
                signUpUser()
            }else{
                Toast.makeText(baseContext, "You must accept the terms to sign up.",
                    Toast.LENGTH_SHORT).show()
            }
        }

        back_arrow.setOnClickListener {
            finish()
        }

        accept.setOnClickListener {
            val uri = Uri.parse("https://momentapplication.com/terms.html")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    private fun checkIfUsernameExist(){

        val username = username.text.toString()


        val reference = FirebaseDatabase.getInstance().reference
        val query: Query = reference
            .child(getString(R.string.dbname_users))
            .orderByChild(getString(R.string.username))
            .equalTo(StringManipulation().condenseUsername(username).toString().toLowerCase(
                Locale.getDefault()))
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    if (singleSnapshot.exists()) {
                        usernameExist = true
                        Toast.makeText(baseContext, "Username already exist.",
                            Toast.LENGTH_SHORT).show()
                        val intent = Intent(
                            this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()

                    }else{
                        usernameExist = false

                    }
                }
                if(!usernameExist){
                    createUser()
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun signUpUser(){

        val email = email.text.toString()
        val password = password.text.toString()
        val confirmPassword = confirm_password.text.toString()
        val username = username.text.toString()

        if (email =="" || username =="" || password=="" || confirmPassword ==""){
            Toast.makeText(baseContext, "You can't have a blank information.",
                Toast.LENGTH_SHORT).show()
        }else{
            if (confirmPassword != password){
                Toast.makeText(baseContext, "You don't have write the same password",
                    Toast.LENGTH_SHORT).show()
            }else {
                checkIfUsernameExist()

            }

        }

    }
    private fun createUser(){

        val email = email.text.toString()
        val password = password.text.toString()
        val username = username.text.toString()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    // Add the user to the database
                    userID = auth.currentUser!!.uid
                    val saveUser = User(userID,StringManipulation().condenseUsername(username).toString().toLowerCase(
                        Locale.getDefault()
                    ),
                        username, email,
                        "https://firebasestorage.googleapis.com/v0/b/moment-plateform.appspot.com/o/default_profile_photo.png?alt=media&token=fb985b71-c989-490b-9bfe-2a4839a8c58b"
                    )

                    refUsers.child(getString(R.string.dbname_users))
                        .child(userID)
                        .setValue(saveUser)


                    val time = getTimestamp()
                    val follower = Followers(auth.currentUser!!.uid, time.toString())
                    val following = Following("iuYnUGfJdoVIbVK4Aq5hhHsoUnp2", time.toString())
                    refUsers.child(getString(R.string.followers))
                        .child("iuYnUGfJdoVIbVK4Aq5hhHsoUnp2")
                        .child(auth.currentUser!!.uid)
                        .setValue(follower)

                    refUsers.child(getString(R.string.following))
                        .child(auth.currentUser!!.uid)
                        .child("iuYnUGfJdoVIbVK4Aq5hhHsoUnp2")
                        .setValue(following)

                    addToMessageOrder()


                    val user = Firebase.auth.currentUser
                    user!!.sendEmailVerification()
                        .addOnCompleteListener { mTask ->
                            if (mTask.isSuccessful) {
                                Log.d(TAG, "Email sent.")
                            }
                        }

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()




                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

    }

    private fun addToMessageOrder(){

        val addUser = MessageOrder("iuYnUGfJdoVIbVK4Aq5hhHsoUnp2", getTimestamp().toString())
        val addMe = MessageOrder(auth.currentUser!!.uid, getTimestamp().toString())
        //Add me -> his message list
        refUsers.child(getString(R.string.dbname_message_order))
            .child("iuYnUGfJdoVIbVK4Aq5hhHsoUnp2")
            .child(auth.currentUser!!.uid)
            .setValue(addMe)


        //Add this user -> my message list
        refUsers.child(getString(R.string.dbname_message_order))
            .child(auth.currentUser!!.uid)
            .child("iuYnUGfJdoVIbVK4Aq5hhHsoUnp2")
            .setValue(addUser)

    }

    private fun getTimestamp(): String? {

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = TimeZone.getTimeZone("Canada/Pacific")
        return sdf.format(Date())
    }





}