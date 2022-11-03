package com.application.moment.Utils.Adapter


import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.R
import com.application.moment.models.Photo
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.item_photo.view.*
import java.io.File

class PhotoGalleryRecyclerAdapter(val context: Context,
                                  var listPhoto: MutableList<Photo>,
                                  val cardSize: Int,
                                  val clickListener: sendClickListener
): RecyclerView.Adapter<PhotoGalleryRecyclerAdapter.GalleryVH>() {

    interface sendClickListener{
        fun onPhotoClickListener(photo: Photo)
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): GalleryVH {
        val cell = LayoutInflater.from(context).inflate(R.layout.item_photo, p0, false)
        return GalleryVH(cell)
    }

    override fun getItemCount(): Int {
        return listPhoto.size
    }

    inner class GalleryVH(itemView: View): RecyclerView.ViewHolder(itemView){
        val imageView = itemView.imageThumb
        var photo: Photo? = null


        init {
            imageView.setOnClickListener {
                clickListener.onPhotoClickListener(photo!!)
            }



        }
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onBindViewHolder(p0: GalleryVH, position: Int) {
        val photo = listPhoto[position]
        p0.photo = photo

        Glide
            .with(context)
            .asBitmap()
            .load(Uri.fromFile(File(photo.uri.path)))
            .apply(RequestOptions().override(cardSize,cardSize).centerCrop())
            .transition(BitmapTransitionOptions.withCrossFade())
            .into(p0.imageView)


    }

}