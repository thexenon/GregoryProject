@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")

package com.application.moment.Reports

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.application.moment.Dialogs.BottomSheetAdmin
import com.application.moment.R
import com.application.moment.Search.ViewProfileActivity
import com.application.moment.models.Report
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
import kotlinx.android.synthetic.main.activity_report.*
import kotlinx.android.synthetic.main.custom_controller2.*
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class ReportActivity : AppCompatActivity(){

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var video : PlayerView? = null
    private var progressBar : ProgressBar? = null
    private var simpleExoPlayer: SimpleExoPlayer ? = null

    //VAR

    private var mTicket =""
    private var userID =""
    private var videoID =""
    private var mVideo : VideoDatabase ? = null
    companion object{
        private const val TAG = "ReportActivity"
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        video = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.progressBar)
        mTicket = intent.getStringExtra(getString(R.string.ticket)).toString()
        Log.d(TAG, "onCreate: mTicket = $mTicket")
        val query: Query = myRef
            .child(getString(R.string.dbname_report))
            .child(mTicket)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mReport = snapshot.getValue(Report::class.java)
                userID = mReport!!.user_id
                videoID = mReport.video_id
                ticket.text = mTicket
                report_for.text = "Report for:" + mReport.report_for
                Log.d(TAG, "onDataChange: mReport = $mReport")
                initVideo(mReport)
                val dateCreated = getTimeStampDifference(mReport).toString()
                val dateF = getDate(mReport).toString()
                val timeAgo = getTime(mReport).toString()
                if (dateCreated != "0"){

                    date.text = dateF

                }else{
                    date.text = timeAgo

                }
                user_profile.setOnClickListener {
                    val intent = Intent(this@ReportActivity, ViewProfileActivity::class.java).putExtra(getString(R.string.user_id), mReport.user_id)
                    startActivity(intent)
                }



            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        back_arrow.setOnClickListener {
            finish()
        }




        video!!.setOnClickListener{
            simpleExoPlayer?.playWhenReady = simpleExoPlayer?.playWhenReady != true

        }

        checkIfSeen()
        switch_button.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
               myRef.child(getString(R.string.dbname_report))
                   .child(mTicket)
                   .child(getString(R.string.seen))
                   .setValue(true)
            } else {
                myRef.child(getString(R.string.dbname_report))
                    .child(mTicket)
                    .child(getString(R.string.seen))
                    .setValue(false)
            }
        }


        setupBottomSheet()

    }

    private fun checkIfSeen(){
        val query: Query = myRef
            .child(getString(R.string.dbname_report))
            .child(mTicket)
        query.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                switch_button.isChecked = snapshot.child(getString(R.string.seen)).value == true
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun setupBottomSheet(){

        val query2: Query = myRef
            .child(getString(R.string.dbname_videos))
            .child(userID)
            .child(videoID)
        query2.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange: Video Info: check.")
                mVideo = snapshot.getValue(VideoDatabase::class.java)!!
                Log.d(TAG, "onDataChange: mVideo = $mVideo")
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        more.setOnClickListener {
            val bottomSheet = BottomSheetAdmin()
            val args = Bundle()
            args.putString(getString(R.string.video_id), mVideo?.video_id)
            args.putString(getString(R.string.user_id), mVideo?.user_id)
            args.putString(getString(R.string.category), mVideo?.category)
            bottomSheet.arguments = args
            bottomSheet.show(supportFragmentManager, "tag")
        }
    }
    private fun getTimeStampDifference(message: Report): String? {
        Log.d(ContentValues.TAG, "getTimeStampDifference: getting timestamp difference.")
        var difference: String
        val c= Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = (TimeZone.getTimeZone("Canada/Pacific"))
        val today: Date = c.time
        sdf.format(today)
        val timestamp: Date
        val chatTimestamp: String = message.date_created
        try {
            timestamp = sdf.parse(chatTimestamp)
            difference =
                java.lang.String.valueOf(((today.time - timestamp.time).toDouble() / 1000 / 60 / 60 / 24).roundToInt())
        } catch (e: ParseException) {
            Log.e(ContentValues.TAG, "getTimeStampDifference: ParseException: ", e)
            difference = "0"
        }
        return difference
    }

    private fun getDate(message: Report): String?{
        Log.d(ContentValues.TAG, "getTimeStampDifference: getting timestamp difference.")
        var difference: String
        val c= Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = (TimeZone.getTimeZone("Canada/Pacific"))
        val today: Date = c.time
        sdf.format(today)
        val timestamp: Date
        val chatTimestamp: String = message.date_created


        try {
            timestamp = sdf.parse(chatTimestamp)
            val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(timestamp)

            difference = date
        } catch (e: ParseException) {
            Log.e(ContentValues.TAG, "getTimeStampDifference: ParseException: ", e)
            difference = "error"
        }
        return difference
    }



    private fun getTime(chat: Report): String? {
        var difference :String
        val c= Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = (TimeZone.getTimeZone("Canada/Pacific"))
        val today: Date = c.time
        sdf.format(today)
        val timestamp: Date
        val videoTimestamp: String = chat.date_created
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
            difference = "0"
        }
        return difference
    }




    @Suppress("deprecation")
    private fun initVideo(report: Report){

        val loadControl: LoadControl = DefaultLoadControl()
        val videoUri: Uri = Uri.parse(report.video_path)
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