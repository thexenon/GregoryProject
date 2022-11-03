package com.application.moment.Home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.Dialogs.BottomSheet
import com.application.moment.Dialogs.BottomSheetViewProfile
import com.application.moment.Notifications.NotificationsActivity
import com.application.moment.NotificationsUtils.Token
import com.application.moment.Popular.PopularActivity
import com.application.moment.Profile.ProfileActivity
import com.application.moment.R
import com.application.moment.Search.SearchActivity
import com.application.moment.Utils.Adapter.HomeRecyclerAdapter
import com.application.moment.VideoGallery.VideoGalleryActivity
import com.application.moment.models.Chat
import com.application.moment.models.Notifications
import com.application.moment.models.VideoDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.layout_bell_active.view.*
import kotlinx.android.synthetic.main.layout_bottom_navigation.view.*
import kotlinx.android.synthetic.main.layout_item_view_video.*
import kotlinx.android.synthetic.main.snippet_home_toolbar.view.*


class HomeFragment : Fragment(), HomeRecyclerAdapter.SendClickListener{

    private val user = Firebase.auth.currentUser
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //VAR
    private var myFollowingVideos: MutableList<VideoDatabase> = mutableListOf()
    private var mPaginatedVideo: MutableList<VideoDatabase> = mutableListOf()
    private var adapter: HomeRecyclerAdapter?= null
    private var mRecyclerView: RecyclerView? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var numFollowing = 0
    private var mResults = 0
    private var chatCount : TextView? =null

    companion object{
        private const val TAG = "HomeFragment"
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: started.")
        auth = Firebase.auth

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        setupBottomNavigation(view)
        val messages = view.findViewById<RelativeLayout>(R.id.ic_messages)
        val camera = view.findViewById<RelativeLayout>(R.id.ic_camera)
        chatCount = view.findViewById(R.id.chat_count)
        messages.setOnClickListener {
            val intent = Intent(activity, MainActivity::class.java)
            intent.putExtra(
                getString(R.string.message_fragment),
                getString(R.string.message_fragment)
            )
            activity?.startActivity(intent)
        }
        camera.setOnClickListener {
            val intent1 = Intent(activity, MainActivity::class.java).putExtra(
                getString(R.string.camera_fragment), getString(
                    R.string.camera_fragment
                )
            )
            activity?.startActivity(intent1)
        }
        if (user != null){
            val emailVerified = user.isEmailVerified
            val warningEmail = view.findViewById<RelativeLayout>(R.id.warning_email)
            if(!emailVerified){
                warningEmail.visibility = View.VISIBLE
            }else if (emailVerified){
                warningEmail.visibility = View.GONE
            }
        }

        view.ic_search.setOnClickListener {
            val intent = Intent(activity, SearchActivity::class.java)
            activity?.startActivity(intent)
        }





        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        updateToken()
        mRecyclerView = view?.findViewById(R.id.main_recycler)
        getFollowing()
        setAdapter()
        setupRecycler()


        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(mRecyclerView)
        snapHelper.findSnapView(layoutManager)

        view.refresh.setOnRefreshListener{
            setupRecycler()
            view.refresh.isRefreshing = false

        }
        checkChat()


        
        return view
    }

    private fun checkChat(){
        val reference = FirebaseDatabase.getInstance().getReference(getString(R.string.dbname_chat))
        reference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    var unread = 0
                    for (snapshot in dataSnapshot.children) {
                        val chat = snapshot.getValue(Chat::class.java)!!
                        if (chat.receiver == auth.currentUser!!.uid && !chat.seen) {
                            unread++
                        }
                    }
                    if (unread == 0) {
                        view?.message?.visibility = View.VISIBLE
                        view?.message_active?.visibility = View.GONE
                    } else {
                        if (unread < 100) {
                            view?.message?.visibility = View.GONE
                            view?.message_active?.visibility = View.VISIBLE
                            chatCount?.text = "$unread"
                        } else {
                            view?.message?.visibility = View.GONE
                            view?.message_active?.visibility = View.VISIBLE
                            chatCount?.text = "+99"
                        }
                    }

                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    @Suppress("deprecation")
    private fun updateToken(){
        val refreshToken:String= FirebaseInstanceId.getInstance().token.toString()
        val mToken = Token(refreshToken)
        Log.d(TAG, "updateToken: my token = $mToken")
        myRef.child(getString(R.string.dbname_tokens))
            .child(auth.currentUser!!.uid)
            .setValue(mToken)

    }


    private fun setAdapter(){

        layoutManager = LinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL, false)

        mRecyclerView?.layoutManager = layoutManager


        if (adapter == null){
            adapter = HomeRecyclerAdapter(activity!!, mPaginatedVideo, this)
            mRecyclerView?.adapter = adapter
        } else{
            adapter!!.notifyDataSetChanged()


        }




    }




    private fun getFollowing(){
        val query: Query = myRef
            .child(getString(R.string.following))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    numFollowing++
                }

            }


            override fun onCancelled(databaseError: DatabaseError) {}


        })
    }


    private fun setupRecycler() {
        myFollowingVideos.clear()
        var counter = 0

        val query: Query = myRef
            .child(getString(R.string.following))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val userID = singleSnapshot.child(getString(R.string.user_id)).value.toString()
                    counter++
                    val query3: Query = myRef
                        .child(getString(R.string.dbname_videos))
                        .child(userID)
                    query3.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (singleSnapshot2 in dataSnapshot.children) {
                                val videoGet = singleSnapshot2.getValue(VideoDatabase::class.java)
                                myFollowingVideos.add(videoGet!!)


                            }
                            displayVideo()


                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
                }
            }


            override fun onCancelled(databaseError: DatabaseError) {}


        })

        val query3: Query = myRef
            .child(getString(R.string.dbname_videos))
            .child(auth.currentUser!!.uid)
        query3.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot2 in dataSnapshot.children) {
                    val videoGet = singleSnapshot2.getValue(VideoDatabase::class.java)
                    myFollowingVideos.add(videoGet!!)


                }
                displayVideo()


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }





    private fun displayVideo(){
        mPaginatedVideo.clear()
        try {
            myFollowingVideos.sortWith(Comparator { o1, o2 -> o2!!.date_created.compareTo(o1!!.date_created) })


            var iterations: Int = myFollowingVideos.size

            if (iterations > 5) {
                iterations = 5
            }

            mResults = 5
            for (i in 0 until iterations) {
                mPaginatedVideo.add(myFollowingVideos[i])
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
            TAG, "displayMoreVideos: myFollowingVideos size = $myFollowingVideos ," +
                    "mResults = $mResults "
        )
        try {
            if (myFollowingVideos.size > mResults && myFollowingVideos.size > 0) {
                val iterations: Int = if (myFollowingVideos.size  > mResults + 5) {
                    Log.d(TAG, "displayMoreVideos: there are greater than 5 more videos")
                    5
                } else {
                    Log.d(TAG, "displayMoreVideos: there is less than 5 more videos")
                    myFollowingVideos.size  - mResults
                }

                //add the new videos to the paginated results
                for (i in mResults until mResults + iterations) {
                    mPaginatedVideo.add(myFollowingVideos[i])
                }
                mResults += iterations

                mRecyclerView?.post(Runnable {
                    kotlin.run {
                        adapter!!.notifyDataSetChanged()
                    }
                })
            }
        } catch (e: java.lang.NullPointerException) {
            Log.e(TAG, "displayMoreVideos: NullPointerException: " + e.message)
        } catch (e: java.lang.IndexOutOfBoundsException) {
            Log.e(TAG, "displayMoreVideos: IndexOutOfBoundsException: " + e.message)
        }



    }




    private fun setupBottomNavigation(view: View){
        val reference = FirebaseDatabase.getInstance().getReference(getString(R.string.dbname_notifications))
            .child(auth.currentUser!!.uid)
        reference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var unread = 0
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        val notif = snapshot.getValue(Notifications::class.java)!!
                        if (!notif.seen) {
                            unread++
                        }

                    }
                    when {
                        unread == 0 -> {
                            view.notification.visibility = View.VISIBLE
                            view.bell_active.visibility = View.GONE

                        }
                        unread <= 99 -> {
                            view.notification.visibility = View.GONE
                            view.bell_active.visibility = View.VISIBLE
                            view.text_notseen.text = "$unread"
                            view.bell_active.setOnClickListener {
                                val intent3 = Intent(activity, NotificationsActivity::class.java)
                                startActivity(intent3)
                            }
                        }
                        else -> {
                            view.notification.visibility = View.GONE
                            view.bell_active.visibility = View.VISIBLE
                            view.text_notseen.text = "+99"
                            view.bell_active.setOnClickListener {
                                val intent3 = Intent(activity, NotificationsActivity::class.java)
                                startActivity(intent3)
                            }
                        }
                    }
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


        view.ic_home.visibility = View.GONE
        view.ic_home_active.visibility = View.VISIBLE
        view.ic_popular.setOnClickListener {
            val intent = Intent(activity, PopularActivity::class.java)
            activity?.startActivity(intent)
        }
        view.ic_gallery.setOnClickListener {
            val intent2 = Intent(activity, VideoGalleryActivity::class.java)
            activity?.startActivity(intent2)
        }
        view.notification.setOnClickListener {
            val intent = Intent(activity, NotificationsActivity::class.java)
            activity?.startActivity(intent)
        }

        view.ic_profile.setOnClickListener {
            val intent4 = Intent(activity, ProfileActivity::class.java)
            activity?.startActivity(intent4)
        }

    }

    override fun onVideoClickListener(myFollowingVideo: VideoDatabase) {

    }

    override fun onMoreClickListener(video: VideoDatabase) {
        if(video.user_id != auth.currentUser!!.uid) {
            val bottomSheet = BottomSheetViewProfile()
            val args = Bundle()
            args.putString(getString(R.string.video_id), video.video_id)
            args.putString(getString(R.string.video_path), video.video_path)
            args.putString(getString(R.string.user_id), video.user_id)
            args.putString(getString(R.string.title), video.title)
            args.putString(getString(R.string.description), video.description)
            bottomSheet.arguments = args
            bottomSheet.show(activity!!.supportFragmentManager, video.video_id)
        }else{
            val bottomSheet = BottomSheet()
            val args = Bundle()
            args.putString(getString(R.string.video_id), video.video_id)
            args.putString(getString(R.string.category), video.category)
            bottomSheet.arguments = args
            bottomSheet.show(activity!!.supportFragmentManager, video.video_id)
        }
    }



    fun nextVideo(position: Int){
        mRecyclerView?.smoothScrollToPosition(position)
        Log.d(TAG, "nextVideo: scroll to position: $position")
    }

    override fun onPause() {
        super.onPause()
        if(adapter != null){
            adapter!!.pauseVideo()
        }
    }

    fun pauseVideo(){
        if(adapter != null){
            adapter!!.pauseVideo()
        }
    }



    override fun onStop() {
        super.onStop()
        if(player_view != null){
            player_view.player?.playWhenReady = false
            player_view.player?.stop()
        }
    }

}

