package com.application.moment.Settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.application.moment.Login.WelcomeActivity
import com.application.moment.R
import com.application.moment.models.Chat
import com.application.moment.models.Notifications
import com.application.moment.models.VideoDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class RemoveAccountFragment : Fragment(){
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    //WIDGETS
    private var mBack : ImageView ? = null
    private var mYes: Button? = null
    private var mNo: Button? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_remove_account, container, false)
        mBack = view?.findViewById(R.id.back)
        mYes = view.findViewById(R.id.yes)
        mNo = view.findViewById(R.id.no)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference

        mBack?.setOnClickListener {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity?.startActivity(intent)
            activity?.finish()

        }

        mNo?.setOnClickListener {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity?.startActivity(intent)
            activity?.finish()

        }

        mYes?.setOnClickListener{
            deleteAllData()
        }
        return view
    }

    private fun deleteAllData(){
        removeTokens()
        removeFollowers()
        removeFollowing()
        removeMessageOrder()
        removeNotifications()
        removeMessages()
        removeVideos()
        removeUser()


        auth.currentUser!!.delete()

        val intent = Intent(activity, WelcomeActivity::class.java)
        activity?.startActivity(intent)
        activity?.finish()

    }
    private fun removeUser(){
        /**
         * Remove dbname_user
         */
        myRef.child(getString(R.string.dbname_users))
            .child(auth.currentUser!!.uid)
            .removeValue()
    }
    private fun removeTokens(){
        /**
         * Remove tokens
         */

        myRef.child(getString(R.string.dbname_tokens))
            .child(auth.currentUser!!.uid)
            .removeValue()
    }

    /**
     * Remove notifications
     */

    private fun removeNotifications(){
        val query : Query = myRef
            .child(getString(R.string.dbname_users))
        query.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (singleSnapshot in snapshot.children){
                    val uID = singleSnapshot.key.toString()
                    val query3: Query = myRef
                        .child(getString(R.string.dbname_notifications))
                        .child(uID)
                    query3.addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (snap in snapshot.children){
                                val mNotification = snap.getValue(Notifications::class.java)!!
                                val key = snap.key.toString()
                                // I remove me to his followers
                                if (mNotification.userID == auth.currentUser!!.uid){
                                    myRef.child(getString(R.string.dbname_notifications))
                                        .child(uID)
                                        .child(key)
                                        .removeValue()

                                }



                            }

                        }
                        override fun onCancelled(error: DatabaseError) {

                        }


                    })

                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

    }

    /**
     * Remove message order
     */
    private fun removeMessageOrder(){
        val query : Query = myRef
            .child(getString(R.string.dbname_message_order))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                for (singleSnapshot in snapshot.children){

                    val uID = singleSnapshot.key.toString()
                    myRef.child(getString(R.string.dbname_message_order))
                        .child(uID)
                        .child(auth.currentUser!!.uid)
                        .removeValue()


                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
    private fun removeMessages(){
        /**
         * Remove all the messages
         */

        val query: Query = myRef
            .child(getString(R.string.dbname_chat))
        query.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                for (singleSnapshot in snapshot.children){
                    val myMessage = singleSnapshot.getValue(Chat::class.java)!!
                    if (myMessage.sender == auth.currentUser!!.uid || myMessage.receiver == auth.currentUser!!.uid){
                        val keyID = singleSnapshot.key.toString()
                        myRef.child(getString(R.string.dbname_chat))
                            .child(keyID)
                            .removeValue()

                    }


                }
            }
            override fun onCancelled(error: DatabaseError) {

            }


        })
    }
    private fun removeFollowers(){
        /**
         * Remove Followers
         */


        val query3: Query = myRef
            .child(getString(R.string.followers))
            .child(auth.currentUser!!.uid)
        query3.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (singleSnapshot in snapshot.children){
                    val userID = singleSnapshot.child(getString(R.string.user_id)).value.toString()

                    // I remove me to his followers
                    myRef.child(getString(R.string.following))
                        .child(userID)
                        .child(auth.currentUser!!.uid)
                        .removeValue()


                }
                myRef.child(getString(R.string.followers))
                    .child(auth.currentUser!!.uid)
                    .removeValue()
            }
            override fun onCancelled(error: DatabaseError) {

            }


        })
    }

    private fun removeFollowing(){
        /**
         * Remove following
         */

        val query2: Query = myRef
            .child(getString(R.string.following))
            .child(auth.currentUser!!.uid)
        query2.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (singleSnapshot in snapshot.children){
                    val userID = singleSnapshot.child(getString(R.string.user_id)).value.toString()

                    // I remove me to his followers
                    myRef.child(getString(R.string.followers))
                        .child(userID)
                        .child(auth.currentUser!!.uid)
                        .removeValue()


                }
                myRef.child(getString(R.string.following))
                    .child(auth.currentUser!!.uid)
                    .removeValue()
            }
            override fun onCancelled(error: DatabaseError) {

            }


        })
    }
    private fun removeVideos(){
        /**
         * Remove all the videos
         */

        val query4: Query = myRef
            .child(getString(R.string.dbname_videos))
            .child(auth.currentUser!!.uid)
        query4.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (singleSnapshot in snapshot.children){
                    val mVideo = singleSnapshot.getValue(VideoDatabase::class.java)!!


                    // I remove me to his followers
                    myRef.child(getString(R.string.dbname_categories))
                        .child(mVideo.category)
                        .child(mVideo.video_id)
                        .removeValue()


                }
                myRef.child(getString(R.string.dbname_videos))
                    .child(auth.currentUser!!.uid)
                    .removeValue()
            }
            override fun onCancelled(error: DatabaseError) {

            }


        })
    }
}