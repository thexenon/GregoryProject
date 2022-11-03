package com.application.moment.Dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import com.application.moment.R
import com.application.moment.models.Chat
import com.application.moment.models.Notifications
import com.application.moment.models.VideoDatabase
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.*

class BottomSheetAdmin : BottomSheetDialogFragment(){

    //FIREBASE
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var deleteVideo : RelativeLayout ? = null
    private var deleteUser : RelativeLayout ? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_item_dialog_admin, container, false)
        deleteVideo = view!!.findViewById(R.id.relLayout1)
        deleteUser = view.findViewById(R.id.relLayout2)
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        val mArg = arguments
        val videoID = mArg?.getString(getString(R.string.video_id))
        val category = mArg?.getString(getString(R.string.category))
        val userID = mArg?.getString(getString(R.string.user_id))
        deleteUser?.setOnClickListener {
            deleteThisUser(userID.toString())
        }

        deleteVideo?.setOnClickListener {
            delete(videoID.toString(), category.toString(), userID.toString())
        }


        return view
    }

    private fun deleteThisUser(userID: String){
        addToBanList(userID)
        removeTokens(userID)
        removeFollowers(userID)
        removeFollowing(userID)
        removeMessages(userID)
        removeMessageOrder(userID)
        removeNotifications(userID)
        removeVideos(userID)

    }

    private fun addToBanList(userID: String){
        myRef.child(getString(R.string.dbname_ban))
            .child(userID)
            .child(getString(R.string.user_id))
            .setValue(userID)
    }


    private fun removeTokens(userID: String){
        /**
         * Remove tokens
         */

        myRef.child(getString(R.string.dbname_tokens))
            .child(userID)
            .removeValue()
    }

    /**
     * Remove notifications
     */

    private fun removeNotifications(userID: String){
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
                                if (mNotification.userID == userID){
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
    private fun removeMessageOrder(userID: String){
        val query : Query = myRef
            .child(getString(R.string.dbname_message_order))
            .child(userID)
        query.addListenerForSingleValueEvent(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                for (singleSnapshot in snapshot.children){

                    val uID = singleSnapshot.key.toString()
                    myRef.child(getString(R.string.dbname_message_order))
                        .child(uID)
                        .child(userID)
                        .removeValue()


                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
    private fun removeMessages(userID: String){
        /**
         * Remove all the messages
         */

        val query: Query = myRef
            .child(getString(R.string.dbname_chat))
        query.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                for (singleSnapshot in snapshot.children){
                    val myMessage = singleSnapshot.getValue(Chat::class.java)!!
                    if (myMessage.sender == userID|| myMessage.receiver == userID){
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
    private fun removeFollowers(muserID: String){
        /**
         * Remove Followers
         */


        val query3: Query = myRef
            .child(getString(R.string.followers))
            .child(muserID)
        query3.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (singleSnapshot in snapshot.children){
                    val userID = singleSnapshot.child(getString(R.string.user_id)).value.toString()

                    // I remove me to his followers
                    myRef.child(getString(R.string.following))
                        .child(userID)
                        .child(muserID)
                        .removeValue()


                }
                myRef.child(getString(R.string.followers))
                    .child(muserID)
                    .removeValue()
            }
            override fun onCancelled(error: DatabaseError) {

            }


        })
    }

    private fun removeFollowing(muserID: String){
        /**
         * Remove following
         */

        val query2: Query = myRef
            .child(getString(R.string.following))
            .child(muserID)
        query2.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (singleSnapshot in snapshot.children){
                    val userID = singleSnapshot.child(getString(R.string.user_id)).value.toString()

                    // I remove me to his followers
                    myRef.child(getString(R.string.followers))
                        .child(userID)
                        .child(muserID)
                        .removeValue()


                }
                myRef.child(getString(R.string.following))
                    .child(muserID)
                    .removeValue()
            }
            override fun onCancelled(error: DatabaseError) {

            }


        })
    }
    private fun removeVideos(muserID: String){
        /**
         * Remove all the videos
         */

        val query4: Query = myRef
            .child(getString(R.string.dbname_videos))
            .child(muserID)
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
                    .child(muserID)
                    .removeValue()
            }
            override fun onCancelled(error: DatabaseError) {

            }


        })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.MyDialog)
    }


    /**
     * Delete just the video.
     */
    private fun delete(videoID: String, category:String, muserID: String){
        myRef.child(getString(R.string.dbname_videos))
            .child(muserID)
            .child(videoID)
            .removeValue()


        myRef.child(getString(R.string.dbname_categories))
            .child(category)
            .child(videoID)
            .removeValue()


        Toast.makeText(context, "Delete video success.", Toast.LENGTH_SHORT).show()


    }


}