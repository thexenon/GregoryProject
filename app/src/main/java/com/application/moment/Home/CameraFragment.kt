package com.application.moment.Home

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Point
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.*
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.application.moment.R
import com.application.moment.VideoGallery.PreviewActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.android.synthetic.main.fragment_camera.view.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.absoluteValue
import kotlin.math.hypot


class CameraFragment : Fragment(), AdapterView.OnItemSelectedListener{
    //WIDGETS
    private lateinit var countTimer : CountDownTimer
    private lateinit var countTimer2 : CountDownTimer

    private lateinit var videoCapture: VideoCapture
    private var camera: Camera?= null
    private var preview: Preview? = null
    private lateinit var cameraProvider: ProcessCameraProvider
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var videosFolder : File

    //var
    private var START_TIME = 60000.toLong()
    private var mTimeLeftInMillis = START_TIME
    private var mTimerLeft = 3000.toLong()
    private var listOfItems = arrayOf("60s", "45s", "30s", "15s")
    private var spinner: Spinner? = null

    //boolean
    private var isChecked = false
    private var isRecording = false
    private var torchActive = false



    companion object {
        private const val CAMERA_BACK = 1
        private const val CAMERA_FRONT = 0
        private var CAMERA_SELECTED : Int = CAMERA_BACK
        private const val WRITE_REQUEST_CODE = 101
        private const val TAG = "CameraFragment"
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        requestPermission()
        resetTimer()
        activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        val icswipe = view?.findViewById<ImageView>(R.id.ic_swipe)
        Log.d(TAG, "onCreateView: isChecked status = $isChecked")
        if (isChecked){
            icswipe?.setOnClickListener {
                if (CAMERA_SELECTED == CAMERA_BACK){
                    CAMERA_SELECTED = CAMERA_FRONT
                    startCamera()
                }else{
                    CAMERA_SELECTED = CAMERA_BACK
                    startCamera()
                }
            }

            startCamera()

        }

        spinner = view?.findViewById(R.id.spinner)

        val aa = ArrayAdapter(activity!!, R.layout.item_spinner, listOfItems)//android.R.layout.simple_spinner_item
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner!!.adapter = aa
        spinner!!.onItemSelectedListener =this
        view?.timer_on?.visibility = View.GONE
        view?.timer_off?.setOnClickListener {
            view.timer_on.visibility = View.VISIBLE
            view.timer_off.visibility = View.GONE
        }

        view?.timer_on?.setOnClickListener {
            view.timer_off.visibility = View.VISIBLE
            view.timer_on.visibility = View.GONE
        }
        return view
    }




    @SuppressLint("RestrictedApi")
    private fun startCamera(){
        try{
            val cameraProviderFuture = ProcessCameraProvider.getInstance(activity!!.applicationContext)
            cameraProviderFuture.addListener(Runnable {
                cameraProvider = cameraProviderFuture.get()
                preview = Preview.Builder().build()
                preview?.setSurfaceProvider(preview_view.createSurfaceProvider())
                val cameraSelector =
                    CameraSelector.Builder().requireLensFacing(CAMERA_SELECTED.absoluteValue)
                        .build()

                val wm = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val display = wm.defaultDisplay
                val size = Point()
                display.getSize(size)
                val width: Int = size.x
                val height: Int = size.y - 38
                videoCapture = VideoCaptureConfig.Builder().apply {
                    setAudioBitRate(48000)
                    setCameraSelector(cameraSelector)
                    setVideoFrameRate(30)
                    setTargetResolution(Size(width, height))
                    setAudioChannelCount(1)
                    setBitRate(15000000)
                    setAudioBitRate(128000)
                    setTargetRotation(Surface.ROTATION_0)
                    setAudioRecordSource(MediaRecorder.AudioSource.DEFAULT)
                    setAudioSampleRate(44100)
                    setMaxResolution(Size(width, height))
                }.build()


                cameraProvider.unbindAll()

                view!!.flash_off.visibility = View.VISIBLE
                view!!.flashOn.visibility = View.GONE
                view!!.flashAuto.visibility = View.GONE
                view!!.flashOn.setOnClickListener {
                    view!!.flashOn.visibility = View.GONE
                    view!!.flash_off.visibility = View.VISIBLE
                    torchActive = false
                    camera!!.cameraControl.enableTorch(torchActive)
                }
                view!!.flash_off.setOnClickListener {
                    view!!.flashOn.visibility = View.VISIBLE
                    view!!.flash_off.visibility = View.GONE
                    torchActive = true
                    camera!!.cameraControl.enableTorch(torchActive)
                }

                camera = cameraProvider.bindToLifecycle(
                    activity!!,
                    cameraSelector,
                    preview,
                    videoCapture
                )

                view?.record!!.setOnClickListener {
                    if (view!!.timer_on.visibility == View.GONE) {
                        startRecord()
                    } else {
                        startCounter()

                    }

                }
            }, ContextCompat.getMainExecutor(context!!.applicationContext))
        }catch (e: NullPointerException){
            e.message
        }




    }



    private fun startCounter(){
        countTimer2 = object: CountDownTimer(mTimerLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                mTimerLeft = millisUntilFinished
                updateCounter()

            }

            override fun onFinish() {

            }
        }.start()

    }
    @SuppressLint("SetTextI18n")
    private fun updateCounter(){
        val seconds =((mTimerLeft /1000) %60).toInt()
        view?.timer_count!!.visibility = View.VISIBLE
        val txtTimer = view?.findViewById<TextView>(R.id.timer_count)
        txtTimer?.text = "$seconds s"
        if (seconds == 0) {
            Log.d(TAG, "startCamera: condition ok.")
            startRecord()
            view?.timer_count!!.visibility = View.GONE
        }

    }


    private fun startTimer(){
        countTimer = object: CountDownTimer(mTimeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                mTimeLeftInMillis = millisUntilFinished
                updateCountDownText()

            }

            override fun onFinish() {

            }
        }.start()

    }

    private fun resetTimer(){
        mTimeLeftInMillis = START_TIME
        updateCountDownText()

    }

    @SuppressLint("SetTextI18n")
    private fun updateCountDownText(){
        val seconds =((mTimeLeftInMillis /1000) %60).toInt()
        view?.timer?.visibility = View.VISIBLE
        view?.spinner?.visibility = View.GONE
        val txtTimer = view?.findViewById<TextView>(R.id.timer)
        txtTimer?.text = "$seconds s"
        if (seconds  == 0){
            stopRecord()
        }
    }

    private fun requestPermission(){
        val permissionW = ContextCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val permissionR = ContextCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val permissionC = ContextCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.CAMERA
        )
        val permissionA = ContextCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissionW == PackageManager.PERMISSION_GRANTED ||
            permissionR == PackageManager.PERMISSION_GRANTED ||
            permissionC == PackageManager.PERMISSION_GRANTED ||
            permissionA == PackageManager.PERMISSION_GRANTED){
            startCamera()
            isChecked = true
        } else {
            isChecked = false
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ),
                WRITE_REQUEST_CODE
            )
        }
    }





    @SuppressLint("RestrictedApi", "SimpleDateFormat")
    private fun startRecord(){
        val path = File(context!!.externalCacheDir!!.absolutePath)
        videosFolder = File(path, "MOMENT")
        if (!videosFolder.exists()){
            videosFolder.mkdirs()
        }


        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val prepend = "VIDEO_" + timestamp + "_"
        val videoFile = File.createTempFile(prepend, ".mp4", videosFolder)

        if (!isRecording) {
            revealRecordCircle()
            startTimer()
            isRecording = true

            videoCapture.startRecording(
                videoFile,
                executor,
                object : VideoCapture.OnVideoSavedCallback {
                    override fun onVideoSaved(file: File) {
                        Log.w("onVideoSaved", file.name + " is saved")
                        val intent = Intent(activity!!, PreviewActivity::class.java)
                        intent.putExtra(getString(R.string.video_file), videoFile.toString())
                        intent.putExtra(
                            getString(R.string.video_duration),
                            (mTimeLeftInMillis - START_TIME).toString()
                        )
                        activity?.startActivity(intent)
                        isRecording = false
                        countTimer.cancel()
                    }

                    override fun onError(
                        videoCaptureError: Int,
                        message: String,
                        cause: Throwable?
                    ) {
                        Log.w("onError", "$videoCaptureError $message")
                        isRecording = false
                        countTimer.cancel()
                        resetTimer()

                    }
                })

            view?.record!!.setOnClickListener {
                videoCapture.stopRecording()
                hideRecordCircle()
                isRecording = false
                countTimer.cancel()
                resetTimer()
            }
        }

        else {
            videoCapture.stopRecording()
            hideRecordCircle()
            isRecording = false
        }




    }

    @SuppressLint("RestrictedApi")
    private fun stopRecord(){
        if (isRecording){
            videoCapture.stopRecording()
        }
    }





    private fun revealRecordCircle() {
        val view = view?.findViewById<FloatingActionButton>(R.id.fab)
        val cx = view?.width?.div(2)
        val cy = view?.height?.div(2)
        val finalRadius =
            hypot(cx!!.toDouble(), cy!!.toDouble()).toFloat()
        val anim: Animator = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius)
        view.visibility = View.VISIBLE
        anim.start()
    }

    private fun hideRecordCircle() {
        val view = view?.findViewById<FloatingActionButton>(R.id.fab)
        val cx = view?.width?.div(2)
        val cy = view?.height?.div(2)
        val initRadius =
            hypot(cx!!.toDouble(), cy!!.toDouble()).toFloat()
        val anim: Animator =
            ViewAnimationUtils.createCircularReveal(view, cx, cy, initRadius, 0f)
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                view.visibility = View.INVISIBLE
            }
        })
        anim.start()
    }


    override fun onResume() {
        super.onResume()
        HomeFragment().pauseVideo()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (position) {
            0 -> {
                START_TIME = 60000
                mTimeLeftInMillis = 60000
            }
            1 -> {
                START_TIME = 45000
                mTimeLeftInMillis = 45000
            }
            2 -> {
                START_TIME = 30000
                mTimeLeftInMillis = 30000
            }
            3 -> {
                START_TIME = 15000
                mTimeLeftInMillis = 15000
            }

        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        START_TIME = 60000
        mTimeLeftInMillis = 60000
    }


}
