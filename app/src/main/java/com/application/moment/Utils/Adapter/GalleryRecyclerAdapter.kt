package com.application.moment.Utils.Adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.R
import com.application.moment.models.Video
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.layout_video_card.view.*
import java.io.File

class GalleryRecyclerAdapter(val context: Context,
                             private var listVideo: MutableList<Video>,
                             private val cardSize: Int,
                             val clickListener: SendClickListener
): RecyclerView.Adapter<GalleryRecyclerAdapter.GalleryVH>() {

    interface SendClickListener{
        fun onVideoClickListener(video: Video)
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): GalleryVH {
        val cell = LayoutInflater.from(context).inflate(R.layout.layout_video_card, p0, false)
        return GalleryVH(cell)
    }

    override fun getItemCount(): Int {
        return listVideo.size
    }



    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onBindViewHolder(p0: GalleryVH, p1: Int) {
        val video = listVideo[p1]
        p0.video = video

        Glide
            .with(context)
            .asBitmap()
            .load(Uri.fromFile(File(video.uri.path)))
            .apply(RequestOptions().override(cardSize,cardSize).centerCrop())
            .transition(BitmapTransitionOptions.withCrossFade())
            .into(p0.imageView)

        val allDuration = (video.duration) / 1000
        val sec: Int = allDuration % 60
        val min: Int = (allDuration / 60) % 60
        val seconds : String
        seconds = if (sec < 10){
            ("0$sec").toString()
        }else{
            sec.toString()
        }

        p0.txtDuration.text = ("$min:$seconds")

    }


    inner class GalleryVH(itemView: View): RecyclerView.ViewHolder(itemView){
        val imageView = itemView.videoThumb
        var video: Video? = null
        var txtDuration = itemView.tv_duration


        init {
            imageView.setOnClickListener {
                clickListener.onVideoClickListener(video!!)
            }



        }
    }

}