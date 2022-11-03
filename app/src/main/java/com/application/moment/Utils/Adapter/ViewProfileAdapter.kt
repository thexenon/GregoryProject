package com.application.moment.Utils.Adapter

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.Dialogs.BottomSheetFollowers
import com.application.moment.Dialogs.BottomSheetFollowing
import com.application.moment.Messages.ChatActivity
import com.application.moment.NotificationsUtils.*
import com.application.moment.R
import com.application.moment.Search.ViewProfileActivity
import com.application.moment.Utils.StringManipulation
import com.application.moment.models.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.layout_first_item_profile.view.at
import kotlinx.android.synthetic.main.layout_first_item_profile.view.display_name
import kotlinx.android.synthetic.main.layout_first_item_profile.view.profile_photo
import kotlinx.android.synthetic.main.layout_first_item_profile.view.username
import kotlinx.android.synthetic.main.layout_item__viewprofile.view.*
import kotlinx.android.synthetic.main.layout_item_video.view.*
import kotlinx.android.synthetic.main.layout_top_profile.view.*
import kotlinx.android.synthetic.main.layout_widget_post.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class ViewProfileAdapter(
    val context: Context,
    private var videoList: MutableList<VideoDatabase>,
    private val userInfo: MutableList<User>,
    val clickListener: SendClickListener, val clickListenerInfo: ViewProfileActivity
): RecyclerView.Adapter<ViewProfileAdapter.GridVideo>() {

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase




    private var mLikeByCurrentUser = false
    private var mSeeByCurrentUser = false
    private var mFollowByCurrentUser = false
    private var isCertified = false


    //Notifications
    private lateinit var apiService: APIService
    private var mUsername = ""

    companion object{
        private const val TAG = "ViewProfileAdapter"
        private const val POSITION_PROFILE: Int = 0
        private const val POSITION_VIDEO: Int = 1

        private val DECELERATE_INTERPOLATOR = DecelerateInterpolator()
        private val ACCELERATE_INTERPOLATOR = AccelerateInterpolator()
    }

    interface SendClickListener{
        fun onVideoClickListener(video: VideoDatabase)
        fun onMoreInfoClickListener(video:VideoDatabase)
    }

    override fun getItemCount(): Int {
        return (userInfo.size + videoList.size)
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): GridVideo {
        var cell: View? = null
        if (p1 == POSITION_PROFILE){
            cell = LayoutInflater.from(context)
                .inflate(R.layout.layout_item__viewprofile, p0, false)
        }else if (p1 == POSITION_VIDEO){
            cell = LayoutInflater.from(context).inflate(R.layout.layout_item_video, p0, false)
        }
        return GridVideo(cell!!, p1)


    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: GridVideo, position: Int) {
        //Profile info
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        if(getItemViewType(position) == POSITION_PROFILE){
            try {
                auth = Firebase.auth
                mFirebaseDatabase = FirebaseDatabase.getInstance()
                myRef = mFirebaseDatabase.reference
                myInformations()
                apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService::class.java)
                val user = userInfo[0]
                holder.userInfo = user
                holder.at.text = "@"
                holder.username.text = user.username
                holder.displayName.text = user.display_name
                isCertified(user, holder)
                Glide
                    .with(context)
                    .load(Uri.parse(user.profile_photo))
                    .into(holder.profilePhoto)

                holder.videoCount.text = videoList.size.toString()

                holder.backArrow.setOnClickListener {
                    (context as Activity).finish()
                }

                holder.btnFollowing.setOnClickListener {
                    val bottomSheet = BottomSheetFollowing()
                    bottomSheet.show((context as AppCompatActivity).supportFragmentManager, user.userID)
                }
                holder.btnFollowers.setOnClickListener {
                    val bottomSheet = BottomSheetFollowers()
                    bottomSheet.show((context as AppCompatActivity).supportFragmentManager, user.userID)
                }

                holder.sendMessage.setOnClickListener{
                    val intent = Intent(context, ChatActivity::class.java).putExtra(context.getString(R.string.user_id), user.userID)
                    context.startActivity(intent)
                }

                checkFollowers(holder, user)
                checkFollowing(holder, user)
                adminPanel(user, holder)
                holder.follow.setOnClickListener {
                    holder.follow.visibility = View.GONE
                    holder.unfollow.visibility = View.VISIBLE
                    val time = getTimestamp()
                    val follower = Followers(auth.currentUser!!.uid, time.toString())
                    val following = Following(user.userID, time.toString())
                    myRef.child(context.getString(R.string.followers))
                        .child(user.userID)
                        .child(auth.currentUser!!.uid)
                        .setValue(follower)

                    myRef.child(context.getString(R.string.following))
                        .child(auth.currentUser!!.uid)
                        .child(user.userID)
                        .setValue(following)

                    val mNotification = Notifications(auth.currentUser!!.uid, "Follow", getTimestamp().toString(), "", false)
                    myRef.child(context.getString(R.string.dbname_notifications)).child(user.userID).push().setValue(mNotification)

                    checkFollowers(holder, user)


                    val query: Query = myRef
                        .child(context.getString(R.string.dbname_tokens))
                        .child(user.userID)
                    query.addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()){
                                val usertoken= dataSnapshot.child(context.getString(R.string.token)).value.toString()
                                Log.d(TAG, "onDataChange: userToken = $usertoken")
                                sendNotification(usertoken, "GoLyve".trim() ,"$mUsername started to follow you !".trim())
                            }



                        }
                        override fun onCancelled(error: DatabaseError) {

                        }


                    })


                    // Add this user to -> message list

                    addToMessageOrder(user.userID)

                }
                holder.unfollow.setOnClickListener {
                    holder.unfollow.visibility = View.GONE
                    holder.follow.visibility = View.VISIBLE
                    myRef.child(context.getString(R.string.followers))
                        .child(user.userID)
                        .child(auth.currentUser!!.uid)
                        .removeValue()

                    myRef.child(context.getString(R.string.following))
                        .child(auth.currentUser!!.uid)
                        .child(user.userID)
                        .removeValue()

                    checkFollowers(holder, user)
                }
            }catch (e: ArrayIndexOutOfBoundsException){
                Log.e(TAG, "onBindViewHolder: ${e.message}")
            }
            catch (e: IndexOutOfBoundsException) {
                Log.e(TAG, "onBindViewHolder: ${e.message}")

            }
            catch (e: NullPointerException){
                Log.e(TAG, "onBindViewHolder: ${e.message}")

            }


        }else{


            val video = videoList[position-1]
            holder.video = video
            auth = Firebase.auth
            mFirebaseDatabase = FirebaseDatabase.getInstance()
            myRef = mFirebaseDatabase.reference

            val options = RequestOptions()
            val centerCrop = options.centerCrop()
            Glide
                .with(context)
                .load(Uri.parse(video.video_thumbnail))
                .apply(centerCrop)
                .into(holder.imageView)


            checkLike(holder, video)
            checkSees(holder, video)



            holder.heart.setOnClickListener {
                toggleLike(holder, video)
                val mNotification = Notifications(auth.currentUser!!.uid, "Like", getTimestamp().toString(), video.title, false)
                myRef.child(context.getString(R.string.dbname_notifications)).child(video.user_id).push().setValue(mNotification)
            }
            holder.heartRed.setOnClickListener {
                toggleLike(holder, video)
            }

            val seconds = (video.duration.toInt())
            //Video grid stuff
            holder.txtduration.text = ("$seconds s")
            holder.txtTitle.text = video.title
            holder.textdescription.text = video.description
            holder.txtcategory.text = video.category
            val dateCreated = getTimeStampDifference(video).toString()
            val timeAgo = getTime(video).toString()
            if (dateCreated != "0"){
                if(dateCreated.toInt() < 7){
                    if (dateCreated.toInt() == 1){
                        holder.dateCreated.text = "$dateCreated day ago."
                    }else{
                        holder.dateCreated.text = "$dateCreated days ago."

                    }
                }else{
                    val weekNum = (dateCreated.toInt() /7)
                    if (weekNum == 1){
                        holder.dateCreated.text = "$weekNum week ago."
                    }
                    else{
                        holder.dateCreated.text = "$weekNum weeks ago."

                    }

                }
            }else{
                holder.dateCreated.text = timeAgo

            }
        }

    }

    private fun getTime(video: VideoDatabase): String? {
        Log.d(ContentValues.TAG, "getTimeStampDifference: getting timestamp difference.")
        var difference :String
        val c= Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = (TimeZone.getTimeZone("Canada/Pacific"))
        val today: Date = c.time
        sdf.format(today)
        val timestamp: Date
        val videoTimestamp: String = video.date_created
        try {
            timestamp = sdf.parse(videoTimestamp)
            val hours =
                java.lang.String.valueOf(((today.time - timestamp.time).toDouble() / 60 / 60 / 1000 ).roundToInt())
            if (hours.toInt() < 1){
                val min =
                    java.lang.String.valueOf(((today.time - timestamp.time).toDouble() / 60 / 1000 ).roundToInt())
                if(min.toInt() < 1){
                    val sec =
                        java.lang.String.valueOf(((today.time - timestamp.time).toDouble() / 1000 ).roundToInt())
                    difference = "$sec sec ago."
                }else{
                    difference = "$min min ago."
                }
            }else{
                difference = "$hours h ago."
            }
        } catch (e: ParseException) {
            Log.e(TAG, "getTimeStampDifference: ParseException: ", e)
            difference = "0"
        }
        return difference
    }

    private fun adminPanel(user: User, holder: ViewProfileAdapter.GridVideo){
        val query: Query = myRef
            .child(context.getString(R.string.dbname_admin))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    if(isCertified){
                        holder.addCertification.visibility = View.GONE
                        holder.deleteCertification.visibility = View.VISIBLE

                    }else{
                        holder.addCertification.visibility = View.VISIBLE
                        holder.deleteCertification.visibility = View.GONE

                    }

                    holder.addCertification.setOnClickListener {
                        myRef.child(context.getString(R.string.dbname_certified))
                            .child(user.userID)
                            .child(context.getString(R.string.user_id))
                            .setValue(user.userID)

                        val query2: Query = myRef
                            .child(context.getString(R.string.dbname_tokens))
                            .child(user.userID)
                        query2.addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (dataSnapshot.exists()){
                                    val usertoken= dataSnapshot.child(context.getString(R.string.token)).value.toString()
                                    Log.d(TAG, "onDataChange: userToken = $usertoken")
                                    sendNotification(usertoken, "GoLyve".trim() ," GoLyve certified you !".trim())
                                }



                            }
                            override fun onCancelled(error: DatabaseError) {

                            }


                        })
                    }
                    holder.deleteCertification.setOnClickListener {
                        myRef.child(context.getString(R.string.dbname_certified))
                            .child(user.userID)
                            .removeValue()
                    }
                } else {
                    holder.addCertification.visibility = View.GONE
                    holder.deleteCertification.visibility = View.GONE

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
    private fun isCertified(user: User, holder: ViewProfileAdapter.GridVideo){
        val query: Query = myRef
            .child(context.getString(R.string.dbname_certified))
            .child(user.userID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    holder.certified.visibility = View.VISIBLE
                    Log.d(TAG, "onDataChange: is certified.")

                } else {
                    holder.certified.visibility = View.GONE

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun addToMessageOrder(userID: String){

        val addUser = MessageOrder(userID, getTimestamp().toString())
        val addMe = MessageOrder(auth.currentUser!!.uid, getTimestamp().toString())
        //Add me -> his message list
        myRef.child(context.getString(R.string.dbname_message_order))
            .child(userID)
            .child(auth.currentUser!!.uid)
            .setValue(addMe)


        //Add this user -> my message list
        myRef.child(context.getString(R.string.dbname_message_order))
            .child(auth.currentUser!!.uid)
            .child(userID)
            .setValue(addUser)

    }



    private fun myInformations(){
        val query: Query = myRef
            .child(context.getString(R.string.dbname_users))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val usernameF = dataSnapshot.child(context.getString(R.string.username)).value
                mUsername = usernameF.toString()

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun sendNotification(usertoken:String,title: String,message: String){
        val data= Data(title,message)
        val sender = NotificationSender(data,usertoken)
        apiService.sendNotification(sender).enqueue(object : Callback<MyResponse?> {
            override fun onResponse(call: Call<MyResponse?>, response: Response<MyResponse?>) {
                if (response.code() == 200) {
                    if (response.body()!!.success != 1) {
                        Log.d(TAG, "onResponse: Failed.")
                    }
                }
            }

            override fun onFailure(call: Call<MyResponse?>, t: Throwable?) {

            }
        })
    }

    private fun getTimestamp(): String? {

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = TimeZone.getTimeZone("Canada/Pacific")
        return sdf.format(Date())
    }
    /**
     * Check followers
     */


    private fun checkFollowers(holder: GridVideo, user: User){
        var followersCounter = 0
        val query: Query = myRef
            .child(context.getString(R.string.followers))
            .child(user.userID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    for (singleSnapshot in dataSnapshot.children) {
                        followersCounter++
                        //val userid = singleSnapshot.child("user_id").value.toString()
                        val userId = singleSnapshot.key.toString()
                        Log.d(ContentValues.TAG, "onDataChange: userID : $userId")
                        if(userId == auth.currentUser!!.uid){
                            mFollowByCurrentUser = true
                            holder.unfollow.visibility = View.VISIBLE
                            holder.follow.visibility = View.GONE
                        }
                    }
                    holder.followerCount.text = StringManipulation().condenseNumber(followersCounter).toString()

                }else{
                    holder.followerCount.text = "0"
                    mFollowByCurrentUser = false
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    /**
     * Check followers
     */


    private fun checkFollowing(holder: GridVideo, user: User){
        var followingCounter = 0
        val query: Query = myRef
            .child(context.getString(R.string.following))
            .child(user.userID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    for (singleSnapshot in dataSnapshot.children) {
                        followingCounter++

                    }
                    holder.followingCount.text = StringManipulation().condenseNumber(followingCounter).toString()

                }else{
                    holder.followingCount.text = "0"
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
    /**
     * Like widget
     */

    private fun checkLike(holder: GridVideo, video: VideoDatabase){
        var likeCounter = 0
        val query: Query = myRef
            .child(context.getString(R.string.dbname_videos))
            .child(video.user_id)
            .child(video.video_id)
            .child(context.getString(R.string.likes))
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    for (singleSnapshot in dataSnapshot.children) {
                        likeCounter++
                        val userId = singleSnapshot.key.toString()
                        Log.d(ContentValues.TAG, "onDataChange: userID : $userId")
                        if (userId == auth.currentUser!!.uid){
                            mLikeByCurrentUser = true
                            holder.heartRed.visibility = View.VISIBLE
                            holder.heart.visibility = View.GONE
                        }

                    }
                    holder.likeCount.text = StringManipulation().condenseNumber(likeCounter).toString()

                }else{
                    holder.likeCount.text = "0"
                    mLikeByCurrentUser = false
                    holder.heartRed.visibility = View.GONE
                    holder.heart.visibility = View.VISIBLE
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


    }

    private fun toggleLike(holder: GridVideo, video: VideoDatabase){
        val animationSet = AnimatorSet()
        if(holder.heartRed.visibility == View.GONE){
            Log.d(ContentValues.TAG, "toggleLike: toggling red heart on")
            holder.heartRed.scaleX = 0.1f
            holder.heartRed.scaleY = 0.1f
            val scaleDownY = ObjectAnimator.ofFloat(holder.heartRed, "scaleY", 0.1f, 1f)
            scaleDownY.duration = 300
            scaleDownY.interpolator = DECELERATE_INTERPOLATOR
            val scaleDownX = ObjectAnimator.ofFloat(holder.heartRed, "scaleX", 0.1f, 1f)
            scaleDownX.duration = 300
            scaleDownX.interpolator = DECELERATE_INTERPOLATOR
            holder.heartRed.visibility = View.VISIBLE
            holder.heart.visibility = View.GONE
            animationSet.playTogether(scaleDownY, scaleDownX)
            val newLikeID = myRef.push().key
            myRef.child(context.getString(R.string.dbname_videos))
                .child(video.user_id)
                .child(video.video_id)
                .child(context.getString(R.string.likes))
                .child(auth.currentUser!!.uid)
                .setValue(newLikeID)


        }
        else{
            Log.d(ContentValues.TAG, "toggleLike: toggling red heart off")
            holder.heartRed.scaleX = 0.1f
            holder.heartRed.scaleY = 0.1f
            val scaleDownY = ObjectAnimator.ofFloat(holder.heartRed, "scaleY", 1f, 0.1f)
            scaleDownY.duration = 300
            scaleDownY.interpolator = ACCELERATE_INTERPOLATOR
            val scaleDownX = ObjectAnimator.ofFloat(holder.heartRed, "scaleX", 1f, 0.1f)
            scaleDownX.duration = 300
            scaleDownX.interpolator = ACCELERATE_INTERPOLATOR
            holder.heartRed.visibility = View.GONE
            holder.heart.visibility = View.VISIBLE
            animationSet.playTogether(scaleDownY, scaleDownX)
            myRef.child(context.getString(R.string.dbname_videos))
                .child(video.user_id)
                .child(video.video_id)
                .child(context.getString(R.string.likes))
                .child(auth.currentUser!!.uid)
                .removeValue()


        }
        animationSet.start()
        checkLike(holder, video)
    }

    /**
     * See widget
     */

    private fun checkSees(holder: GridVideo, video: VideoDatabase){

        var seeCounter = 0
        val query: Query = myRef
            .child(context.getString(R.string.dbname_videos))
            .child(video.user_id)
            .child(video.video_id)
            .child(context.getString(R.string.sees))
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    for (singleSnapshot in dataSnapshot.children) {
                        seeCounter++
                        val userID = singleSnapshot.child(context.getString(R.string.user_id)).value.toString()
                        if (userID == auth.currentUser!!.uid){
                            mSeeByCurrentUser = true
                        }
                    }
                    holder.seeCount.text = StringManipulation().condenseNumber(seeCounter).toString()

                }else{
                    holder.seeCount.text = "0"
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    /**
     * widget timeStamp
     */

    private fun getTimeStampDifference(video: VideoDatabase): String? {
        Log.d(ContentValues.TAG, "getTimeStampDifference: getting timestamp difference.")
        var difference : String
        val c= Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = (TimeZone.getTimeZone("Canada/Pacific"))
        val today: Date = c.time
        sdf.format(today)
        val timestamp: Date
        val videoTimestamp: String = video.date_created
        try {
            timestamp = sdf.parse(videoTimestamp)
            difference =
                java.lang.String.valueOf(((today.time - timestamp.time).toDouble() / 1000 / 60 / 60 / 24).roundToInt())
        } catch (e: ParseException) {
            Log.e(ContentValues.TAG, "getTimeStampDifference: ParseException: ", e)
            difference = "0"
        }
        return difference
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0){
            POSITION_PROFILE
        }else{
            POSITION_VIDEO
        }
    }

    inner class GridVideo(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView){

        // All the user videos
        val imageView = itemView.thumbnail
        val txtduration = itemView.txt_duration
        val txtcategory = itemView.txt_category
        val txtTitle = itemView.txt_title
        val dateCreated = itemView.txt_date_created
        val textdescription = itemView.txt_description
        val likeCount = itemView.tvLike
        val seeCount = itemView.tvSee
        val heart = itemView.heart
        val heartRed = itemView.heart_red
        val moreInfo = itemView.more
        var video: VideoDatabase? = null

        // All the profile information

        val profilePhoto = itemView.profile_photo
        val at = itemView.at
        val username = itemView.username
        val displayName = itemView.display_name
        val follow = itemView.follow
        val unfollow = itemView.unFollow
        val followerCount = itemView.num_followers
        val followingCount = itemView.num_following
        val videoCount = itemView.num_videos
        val backArrow = itemView.back_arrow
        var userInfo : User? = null
        val btnFollowers = itemView.followers
        val btnFollowing = itemView.following
        val certified = itemView.certified
        val sendMessage = itemView.send_message

        //Admin panel
        val addCertification = itemView.add_certification
        val deleteCertification = itemView.already_certified


        init {
            if(p1 == POSITION_VIDEO){
                itemView.setOnClickListener {
                    clickListener.onVideoClickListener(video!!)
                }
                moreInfo.setOnClickListener {
                    clickListenerInfo.onMoreInfoClickListener(video!!)
                }

            }



        }
    }

}