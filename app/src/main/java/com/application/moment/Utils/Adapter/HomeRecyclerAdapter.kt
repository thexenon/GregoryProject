package com.application.moment.Utils.Adapter

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
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
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.Profile.ProfileActivity
import com.application.moment.R
import com.application.moment.Search.ViewProfileActivity
import com.application.moment.Utils.StringManipulation
import com.application.moment.Utils.VideoPlayerConfig
import com.application.moment.models.Notifications
import com.application.moment.models.VideoDatabase
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.formats.MediaView
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.formats.UnifiedNativeAdView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.custom_controller2.view.*
import kotlinx.android.synthetic.main.fragment_video.view.*
import kotlinx.android.synthetic.main.layout_widget_post.view.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class HomeRecyclerAdapter(
    val context: Context,
    private var videoList: MutableList<VideoDatabase>,
    val clickListener: SendClickListener
): RecyclerView.Adapter<HomeRecyclerAdapter.ViewHolder>() {


    interface OnLoadMoreItemsListener {
        fun onLoadMoreItems()
        fun onNextVideo(position: Int)
    }

    var mOnLoadMoreItemsListener: OnLoadMoreItemsListener? = null
    var mOnNextVideo: OnLoadMoreItemsListener? = null
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //BOOLEAN
    private var mLikeByCurrentUser = false
    private var mSeeByCurrentUser = false
    private var isAttached = false

    //WIDGETS
    private var mPlayerView : PlayerView? = null
    private var mView : View ? = null




    companion object {
        private const val TAG = "HomeRecyclerAdapter"
        private val DECELERATE_INTERPOLATOR = DecelerateInterpolator()
        private val ACCELERATE_INTERPOLATOR = AccelerateInterpolator()
        private const val CONTENT = 0
        private const val AD = 1
        private var LIST_AD_DELTA = 3
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        isAttached = false
        holder.simpleExoPlayer?.playWhenReady = false
        holder.simpleExoPlayer?.release()
        holder.simpleExoPlayer = null


    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        isAttached = true

        if (holder.simpleExoPlayer == null){
            onBindViewHolder(holder, holder.adapterPosition)
        }
        holder.simpleExoPlayer?.playWhenReady = true
        mPlayerView = holder.playerView


    }



    interface SendClickListener{
        fun onVideoClickListener(myFollowingVideo: VideoDatabase)
        fun onMoreClickListener(video: VideoDatabase)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position > 0 && (position % LIST_AD_DELTA) == 0) {
            AD
        }else{
            CONTENT
        }

    }
    override fun onCreateViewHolder(view: ViewGroup, p1: Int): ViewHolder {

        val cell: View
        if (p1 == CONTENT){
            cell = LayoutInflater.from(context).inflate(
                R.layout.layout_item_view_video,
                view,
                false
            )
        }else{
            cell = LayoutInflater.from(context).inflate(R.layout.layout_native_ads, view, false)
            mView = cell

        }


        return ViewHolder(cell, p1)


    }



    override fun getItemCount(): Int {

        var additionalContent = 0
        if (videoList.size > 0 && LIST_AD_DELTA > 0 && videoList.size >= LIST_AD_DELTA) {
            additionalContent = videoList.size / LIST_AD_DELTA
        }
        return videoList.size + additionalContent
    }





    @SuppressLint("SetTextI18n", "ServiceCast")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: videolist size : " + videoList.size)
        if(reachedEndOfList(position)){
            loadMoreData()
        }
        if(getItemViewType(position) == CONTENT ){
            Log.d(TAG, "onBindViewHolder: position = $position")
            val video = videoList[getRealPosition(position)]

            auth = Firebase.auth
            mFirebaseDatabase = FirebaseDatabase.getInstance()
            myRef = mFirebaseDatabase.reference
            checkLike(holder, video)
            isCertified(video, holder)
            val query2: Query = myRef
                .child(context.getString(R.string.dbname_users))
                .child(video.user_id)
            query2.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val displayname =
                        dataSnapshot.child(context.getString(R.string.display_name)).value
                    val usernameF = dataSnapshot.child(context.getString(R.string.username)).value
                    val profilePhoto =
                        dataSnapshot.child(context.getString(R.string.profile_photo)).value

                    holder.username.text = usernameF.toString()
                    holder.displayName.text = displayname.toString()


                    Glide
                        .with(context.applicationContext)
                        .load(Uri.parse(profilePhoto.toString()))
                        .into(holder.profilePhoto)
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
            holder.myFollowingVideo = video
            holder.at.text = "@"
            holder.title.text = video.title
            holder.description.text = video.description
            if (video.user_id == auth.currentUser!!.uid){
                holder.profilePhoto.setOnClickListener{
                    val intent = Intent(context, ProfileActivity::class.java)
                    context.startActivity(intent)
                }

                holder.username.setOnClickListener{
                    val intent = Intent(context, ProfileActivity::class.java)
                    context.startActivity(intent)
                }
                holder.displayName.setOnClickListener{
                    val intent = Intent(context, ProfileActivity::class.java)
                    context.startActivity(intent)
                }

            }else{
                holder.profilePhoto.setOnClickListener{
                    val intent = Intent(context, ViewProfileActivity::class.java).putExtra(
                        context.getString(
                            R.string.user_id
                        ), video.user_id
                    )
                    context.startActivity(intent)
                }

                holder.username.setOnClickListener{
                    val intent = Intent(context, ViewProfileActivity::class.java).putExtra(
                        context.getString(
                            R.string.user_id
                        ), video.user_id
                    )
                    context.startActivity(intent)
                }
                holder.displayName.setOnClickListener{
                    val intent = Intent(context, ViewProfileActivity::class.java).putExtra(
                        context.getString(
                            R.string.user_id
                        ), video.user_id
                    )
                    context.startActivity(intent)
                }
            }

            holder.category.text = video.category



            checkSees(holder, video)



            if(!mSeeByCurrentUser){
                addNewSee(video)
                checkSees(holder, video)
            }


            holder.heart.setOnClickListener {
                toggleLike(holder, video)
                val mNotification = Notifications(
                    auth.currentUser!!.uid,
                    "Like",
                    getTimestamp().toString(),
                    video.title,
                    false
                )
                myRef.child(context.getString(R.string.dbname_notifications)).child(video.user_id).push().setValue(
                    mNotification
                )
            }
            holder.heartRed.setOnClickListener {
                toggleLike(holder, video)
            }

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

                    }else{
                        holder.dateCreated.text = "$weekNum weeks ago."

                    }

                }
            }else{
                holder.dateCreated.text = timeAgo

            }


            initVideo(video, holder)








        }else{
            holder.simpleExoPlayer?.playWhenReady = false

            MobileAds.initialize(context) {}
            val adLoader: AdLoader =
                //

                AdLoader.Builder(context, "ca-app-pub-7181485920872488/6826261714")
                    .forUnifiedNativeAd { unifiedNativeAd ->
                        val unifiedNativeAdView = mView as UnifiedNativeAdView
                        mapUnifiedNativeAdToLayout(unifiedNativeAd, unifiedNativeAdView)

                    }
                    .build()
            adLoader.loadAd(AdRequest.Builder().build())





        }



    }

    /**
     * widget timeStamp
     */

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun getTimeStampDifference(video: VideoDatabase): String? {
        Log.d(TAG, "getTimeStampDifference: getting timestamp difference.")
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
            difference =
                java.lang.String.valueOf(((today.time - timestamp.time).toDouble() / 1000 / 60 / 60 / 24).roundToInt())
        } catch (e: ParseException) {
            Log.e(TAG, "getTimeStampDifference: ParseException: ", e)
            difference = "0"
        }
        return difference
    }
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "deprecation",
        "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
    )
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


    private fun reachedEndOfList(position: Int): Boolean {
        return position == itemCount - 1
    }

    private fun loadMoreData() {
        try {
            mOnLoadMoreItemsListener = context as OnLoadMoreItemsListener
        } catch (e: ClassCastException) {
            Log.e(TAG, "loadMoreData: ClassCastException: " + e.message)
        }
        try {
            mOnLoadMoreItemsListener!!.onLoadMoreItems()
        } catch (e: NullPointerException) {
            Log.e(TAG, "loadMoreData: ClassCastException: " + e.message)
        }
    }

    private fun startNextVideo(position: Int) {
        try {
            mOnNextVideo = context as OnLoadMoreItemsListener
        } catch (e: ClassCastException) {
            Log.e(TAG, "loadMoreData: ClassCastException: " + e.message)
        }
        try {
            mOnNextVideo!!.onNextVideo(position)
        } catch (e: NullPointerException) {
            Log.e(TAG, "loadMoreData: ClassCastException: " + e.message)
        }
    }

    private fun getTimestamp(): String? {

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = TimeZone.getTimeZone("Canada/Pacific")
        return sdf.format(Date())
    }

    fun pauseVideo(){
        mPlayerView?.player?.playWhenReady = false
    }

    private fun getRealPosition(position: Int): Int {
        return if (LIST_AD_DELTA == 0) {
            position
        } else {
            position - position / LIST_AD_DELTA
        }
    }


    /**
     * Like widget
     */

    private fun checkLike(holder: ViewHolder, video: VideoDatabase){
        var likeCounter = 0
        val query: Query = myRef
            .child(context.getString(R.string.dbname_videos))
            .child(video.user_id)
            .child(video.video_id)
            .child(context.getString(R.string.likes))
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (singleSnapshot in dataSnapshot.children) {
                        likeCounter++
                        val userId = singleSnapshot.key.toString()
                        Log.d(TAG, "onDataChange: userID : $userId")
                        if (userId == auth.currentUser!!.uid) {
                            mLikeByCurrentUser = true
                            holder.heartRed.visibility = View.VISIBLE
                            holder.heart.visibility = View.GONE
                        }


                    }


                    holder.likeCount.text = StringManipulation().condenseNumber(likeCounter).toString()

                } else {
                    holder.likeCount.text = "0"
                    mLikeByCurrentUser = false
                    holder.heartRed.visibility = View.GONE
                    holder.heart.visibility = View.VISIBLE
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


    }

    private fun toggleLike(holder: ViewHolder, video: VideoDatabase){
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
            Log.d(TAG, "toggleLike: toggling red heart off")
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
     * Add new See
     */
    private fun addNewSee(video: VideoDatabase){
        val newSeeID = myRef.push().key
        myRef.child(context.getString(R.string.dbname_videos))
            .child(video.user_id)
            .child(video.video_id)
            .child(context.getString(R.string.sees))
            .child(auth.currentUser!!.uid)
            .setValue(newSeeID)
    }

    /**
     * See widget
     */


    private fun checkSees(holder: ViewHolder, video: VideoDatabase){

        var seeCounter = 0
        val query: Query = myRef
            .child(context.getString(R.string.dbname_videos))
            .child(video.user_id)
            .child(video.video_id)
            .child(context.getString(R.string.sees))
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (singleSnapshot in dataSnapshot.children) {
                        seeCounter++
                        val userid =
                            singleSnapshot.child(context.getString(R.string.user_id)).value.toString()
                        if (userid == auth.currentUser!!.uid) {
                            mSeeByCurrentUser = true
                        }
                    }
                    holder.seeCount.text = StringManipulation().condenseNumber(seeCounter).toString()

                } else {
                    holder.seeCount.text = "0"
                    mSeeByCurrentUser = false
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }








    @Suppress("deprecation")
    private fun initVideo(videoDatabase: VideoDatabase, holder: ViewHolder){
        ExoPlayerFactory.newSimpleInstance(context)
        val loadControl=  DefaultLoadControl.Builder().apply {
            setAllocator(DefaultAllocator(true, 16))
            setBufferDurationsMs(
            VideoPlayerConfig.MIN_BUFFER_DURATION,
            VideoPlayerConfig.MAX_BUFFER_DURATION,
            VideoPlayerConfig.MIN_PLAYBACK_START_BUFFER,
            VideoPlayerConfig.MIN_PLAYBACK_RESUME_BUFFER)
            setTargetBufferBytes(-1)
            setPrioritizeTimeOverSizeThresholds(true)
        }.createDefaultLoadControl()
        val videoUri: Uri = Uri.parse(videoDatabase.video_path)
        val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter()
        val trackSelector: TrackSelector =
            DefaultTrackSelector(AdaptiveTrackSelection.Factory(bandwidthMeter))

        holder.simpleExoPlayer =
            ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl)
        val mediaSource = ProgressiveMediaSource.Factory(DefaultHttpDataSourceFactory("exoplayer_video")).createMediaSource(
            videoUri
        )

        holder.playerView.player = holder.simpleExoPlayer

        holder.playerView.keepScreenOn = true
        holder.simpleExoPlayer!!.prepare(mediaSource, false, true)


        holder.playerView.setOnClickListener{
            holder.simpleExoPlayer!!.playWhenReady = !holder.simpleExoPlayer!!.playWhenReady

        }




        holder.simpleExoPlayer?.addListener(object : Player.EventListener {
            override fun onTimelineChanged(
                timeline: Timeline,
                @Nullable manifest: Any?,
                reason: Int
            ) {
            }

            override fun onTracksChanged(
                trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray
            ) {
            }

            override fun onLoadingChanged(isLoading: Boolean) {}
            override fun onPlayerStateChanged(
                playWhenReady: Boolean,
                playbackState: Int
            ) {
                if (playbackState == Player.STATE_BUFFERING) {
                    holder.progressBar.visibility = View.VISIBLE
                } else if (playbackState == Player.STATE_READY) {
                    holder.progressBar.visibility = View.GONE
                }

                if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "onPlayerStateChanged: next video.")
                    startNextVideo(holder.adapterPosition+1)
                }

            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {}
            override fun onRepeatModeChanged(repeatMode: Int) {}
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
            override fun onPlayerError(error: ExoPlaybackException) {}
            override fun onPositionDiscontinuity(reason: Int) {}
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
            override fun onSeekProcessed() {}

        })

    }

    private fun isCertified(video: VideoDatabase, holder: ViewHolder){
        val query: Query = myRef
            .child(context.getString(R.string.dbname_certified))
            .child(video.user_id)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    holder.certified.visibility = View.VISIBLE

                } else {
                    holder.certified.visibility = View.GONE

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }





    private fun mapUnifiedNativeAdToLayout(
        adFromGoogle: UnifiedNativeAd,
        myAdView: UnifiedNativeAdView
    ) {
        val mediaView: MediaView = myAdView.findViewById(R.id.ad_media)
        myAdView.mediaView = mediaView
        myAdView.headlineView = myAdView.findViewById(R.id.ad_headline)
        myAdView.bodyView = myAdView.findViewById(R.id.ad_body)
        myAdView.callToActionView = myAdView.findViewById(R.id.ad_call_to_action)
        myAdView.iconView = myAdView.findViewById(R.id.ad_icon)
        myAdView.priceView = myAdView.findViewById(R.id.ad_price)
        myAdView.starRatingView = myAdView.findViewById(R.id.ad_rating)
        myAdView.storeView = myAdView.findViewById(R.id.ad_store)
        myAdView.advertiserView = myAdView.findViewById(R.id.ad_advertiser)
        (myAdView.headlineView as TextView).text = adFromGoogle.headline
        if (adFromGoogle.body == null) {
            myAdView.bodyView.visibility = View.GONE
        } else {
            (myAdView.bodyView as TextView).text = adFromGoogle.body
        }
        if (adFromGoogle.callToAction == null) {
            myAdView.callToActionView.visibility = View.GONE
        } else {
            (myAdView.callToActionView as Button).text = adFromGoogle.callToAction
        }
        if (adFromGoogle.icon == null) {
            myAdView.iconView.visibility = View.GONE
        } else {
            (myAdView.iconView as ImageView).setImageDrawable(
                adFromGoogle.icon.drawable
            )
        }
        if (adFromGoogle.price == null) {
            myAdView.priceView.visibility = View.GONE
        } else {
            (myAdView.priceView as TextView).text = adFromGoogle.price
        }
        if (adFromGoogle.starRating == null) {
            myAdView.starRatingView.visibility = View.GONE
        } else {
            (myAdView.starRatingView as RatingBar).rating = adFromGoogle.starRating.toFloat()
        }
        if (adFromGoogle.store == null) {
            myAdView.storeView.visibility = View.GONE
        } else {
            (myAdView.storeView as TextView).text = adFromGoogle.store
        }
        if (adFromGoogle.advertiser == null) {
            myAdView.advertiserView.visibility = View.GONE
        } else {
            (myAdView.advertiserView as TextView).text = adFromGoogle.advertiser
        }
        myAdView.setNativeAd(adFromGoogle)


    }




    inner class ViewHolder(itemView: View, p1: Int): RecyclerView.ViewHolder(itemView){
        val profilePhoto = itemView.profile_photo
        val username= itemView.username
        val displayName = itemView.display_name
        val at = itemView.at
        val title = itemView.title
        val description = itemView.description
        val category = itemView.category
        val likeCount = itemView.tvLike
        val seeCount = itemView.tvSee
        val heart = itemView.heart
        val heartRed = itemView.heart_red
        val certified = itemView.certified
        private val moreInfo  = itemView.more
        val dateCreated  = itemView.timeStamp
        var myFollowingVideo : VideoDatabase? = null
        var playerView = itemView.player_view
        var progressBar = itemView.progressBar
        var simpleExoPlayer: SimpleExoPlayer? = null


        init {
            if (p1 == CONTENT){

                itemView.setOnClickListener {
                    clickListener.onVideoClickListener(myFollowingVideo!!)
                }
                moreInfo.setOnClickListener {
                    clickListener.onMoreClickListener(myFollowingVideo!!)
                }


            }

        }


    }


}