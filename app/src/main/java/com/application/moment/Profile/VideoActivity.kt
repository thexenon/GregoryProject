@file:Suppress("DEPRECATION")

package com.application.moment.Profile

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.application.moment.R
import com.application.moment.models.Notifications
import com.application.moment.models.VideoDatabase
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.custom_controller.*
import kotlinx.android.synthetic.main.fragment_video.*
import kotlinx.android.synthetic.main.layout_widget_video.*
import java.text.SimpleDateFormat
import java.util.*


class VideoActivity : AppCompatActivity(){

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var videoDatabase: VideoDatabase? = null
    private var video : PlayerView ? = null
    private var progressBar : ProgressBar? = null
    private var simpleExoPlayer: SimpleExoPlayer ? = null



    //VARS
    private var userID  = ""
    private var videoID = ""

    //BOOLEAN
    private var mLikeByCurrentUser = false
    private var mSeeByCurrentUser = false

    companion object{
        private val DECELERATE_INTERPOLATOR = DecelerateInterpolator()
        private val ACCELERATE_INTERPOLATOR = AccelerateInterpolator()
        private const val TAG = "VideoActivity"
    }
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_video)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference

        userID = if (intent.hasExtra(getString(R.string.user_id))){
            intent.getStringExtra(getString(R.string.user_id)).toString()
        }else{
            auth.currentUser!!.uid
        }
        videoID = intent.getStringExtra(getString(R.string.video_id)).toString()
        video = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.progressBar)
        searchVideo()

        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)


        back_arrow.setOnClickListener {
            simpleExoPlayer?.stop()
            finish()
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        simpleExoPlayer?.stop()
    }

    private fun setupWidget(){
        checkLike()
        checkSees()
        category.text = videoDatabase?.category

        heart.setOnClickListener {
            toggleLike()
            val mNotification = Notifications(auth.currentUser!!.uid, "Like", getTimestamp().toString(), videoDatabase?.title.toString(), false)
            myRef.child(getString(R.string.dbname_notifications)).child(videoDatabase?.user_id.toString()).push().setValue(mNotification)
        }
        heart_red.setOnClickListener {
            toggleLike()
        }
        if(!mSeeByCurrentUser){
            addNewSee()
        }
    }
    private fun getTimestamp(): String? {

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = TimeZone.getTimeZone("Canada/Pacific")
        return sdf.format(Date())
    }

    /**
     * Add new See
     */
    private fun addNewSee(){
        val newSeeID = myRef.push().key
        myRef.child(getString(R.string.dbname_videos))
            .child(userID)
            .child(videoID)
            .child(getString(R.string.sees))
            .child(auth.currentUser!!.uid)
            .setValue(newSeeID)
    }

    /**
     * See widget
     */


    private fun checkSees(){

        var seeCounter = 0
        val query: Query = myRef
            .child(getString(R.string.dbname_videos))
            .child(userID)
            .child(videoID)
            .child(getString(R.string.sees))
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    for (singleSnapshot in dataSnapshot.children) {
                        seeCounter++
                        val userid = singleSnapshot.child(getString(R.string.user_id)).value.toString()
                        if (userid == auth.currentUser!!.uid){
                            mSeeByCurrentUser = true
                        }
                    }
                    tvSee.text = "$seeCounter"

                }else{
                    tvSee.text = "0"
                    mSeeByCurrentUser = false
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    /**
     * Like widget
     */

    private fun checkLike(){

        var likeCounter = 0
        val query: Query = myRef
            .child(getString(R.string.dbname_videos))
            .child(userID)
            .child(videoID)
            .child(getString(R.string.likes))
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    for (singleSnapshot in dataSnapshot.children) {
                        likeCounter++
                        val userId = singleSnapshot.key.toString()
                        Log.d(TAG, "onDataChange: userID : $userId")
                        if(userId == auth.currentUser!!.uid){
                            mLikeByCurrentUser = true
                            heart_red.visibility = View.VISIBLE
                            heart.visibility = View.GONE
                        }
                    }
                    tvLike.text = "$likeCounter"

                }else{
                    tvLike.text = "0"
                    heart_red.visibility = View.GONE
                    heart.visibility = View.VISIBLE
                }


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


    }

    private fun toggleLike(){
        val animationSet = AnimatorSet()
        if(heart_red.visibility == View.GONE){
            Log.d(TAG, "toggleLike: toggling red heart on")
            heart_red.scaleX = 0.1f
            heart_red.scaleY = 0.1f
            val scaleDownY = ObjectAnimator.ofFloat(heart_red, "scaleY", 0.1f, 1f)
            scaleDownY.duration = 300
            scaleDownY.interpolator = DECELERATE_INTERPOLATOR
            val scaleDownX = ObjectAnimator.ofFloat(heart_red, "scaleX", 0.1f, 1f)
            scaleDownX.duration = 300
            scaleDownX.interpolator = DECELERATE_INTERPOLATOR
            heart_red.visibility = View.VISIBLE
            heart.visibility = View.GONE
            animationSet.playTogether(scaleDownY, scaleDownX)
            val newLikeID = myRef.push().key
            myRef.child(getString(R.string.dbname_videos))
                .child(userID)
                .child(videoID)
                .child(getString(R.string.likes))
                .child(auth.currentUser!!.uid)
                .setValue(newLikeID)


        }
        else{
            Log.d(TAG, "toggleLike: toggling red heart off")
            heart_red.scaleX = 0.1f
            heart_red.scaleY = 0.1f
            val scaleDownY = ObjectAnimator.ofFloat(heart_red, "scaleY", 1f, 0.1f)
            scaleDownY.duration = 300
            scaleDownY.interpolator = ACCELERATE_INTERPOLATOR
            val scaleDownX = ObjectAnimator.ofFloat(heart_red, "scaleX", 1f, 0.1f)
            scaleDownX.duration = 300
            scaleDownX.interpolator = ACCELERATE_INTERPOLATOR
            heart_red.visibility = View.GONE
            heart.visibility = View.VISIBLE
            animationSet.playTogether(scaleDownY, scaleDownX)
            myRef.child(getString(R.string.dbname_videos))
                .child(userID)
                .child(videoID)
                .child(getString(R.string.likes))
                .child(auth.currentUser!!.uid)
                .removeValue()


        }
        animationSet.start()
        checkLike()
        if (exo_pause.visibility == View.VISIBLE || exo_play.visibility == View.VISIBLE){
            layout_widget.visibility = View.VISIBLE
        }else{
            layout_widget.visibility = View.GONE
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun searchVideo(){
        val query: Query = myRef
            .child(getString(R.string.dbname_videos))
            .child(userID)
            .child(videoID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val objectMap =
                    dataSnapshot.value as HashMap<String?, Any?>?
                val userID = objectMap?.get(getString(R.string.user_id)).toString()
                val videoPath = objectMap?.get(getString(R.string.video_path)).toString()
                val videoID = objectMap?.get(getString(R.string.video_id)).toString()
                val description = objectMap?.get(getString(R.string.description)).toString()
                val dateCreated = objectMap?.get(getString(R.string.date_created)).toString()
                val category = objectMap?.get(getString(R.string.category)).toString()
                val thumbnail = objectMap?.get(getString(R.string.video_thumbnail)).toString()
                val title = objectMap?.get(getString(R.string.title)).toString()
                val duration = objectMap?.get(getString(R.string.video_duration)).toString()

                videoDatabase = VideoDatabase(userID, videoPath, thumbnail, videoID, category, dateCreated, duration, title, description)
                initVideo(videoDatabase!!)
                setupWidget()



            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }



    @Suppress("deprecation")
    private fun initVideo(videoDatabase: VideoDatabase){

        val loadControl: LoadControl = DefaultLoadControl()
        val videoUri: Uri = Uri.parse(videoDatabase.video_path)
        val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter()
        val trackSelector: TrackSelector =
            DefaultTrackSelector(AdaptiveTrackSelection.Factory(bandwidthMeter))

        simpleExoPlayer =
            ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl)
        val factory = DefaultHttpDataSourceFactory("exoplayer_video")
        val extractorsFactory: ExtractorsFactory = DefaultExtractorsFactory()
        val mediaSource: MediaSource =
            ExtractorMediaSource(videoUri, factory, extractorsFactory, null, null)

        video!!.player = simpleExoPlayer
        video!!.keepScreenOn = true
        simpleExoPlayer!!.prepare(mediaSource)
        simpleExoPlayer!!.playWhenReady = true
        simpleExoPlayer!!.addListener(object : Player.EventListener {
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
                    progressBar!!.visibility = View.VISIBLE
                } else if (playbackState == Player.STATE_READY) {
                    progressBar!!.visibility = View.GONE
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

    override fun onPause() {
        super.onPause()
        simpleExoPlayer?.playWhenReady = false
    }

    override fun onStop() {
        super.onStop()
        simpleExoPlayer?.stop()
    }
}