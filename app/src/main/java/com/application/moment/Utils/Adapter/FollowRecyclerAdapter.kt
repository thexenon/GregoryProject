package com.application.moment.Utils.Adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.R
import com.application.moment.models.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.layout_item_following.view.*
import kotlinx.android.synthetic.main.layout_item_search.view.at
import kotlinx.android.synthetic.main.layout_item_search.view.display_name
import kotlinx.android.synthetic.main.layout_item_search.view.profile_photo
import kotlinx.android.synthetic.main.layout_item_search.view.username

class FollowRecyclerAdapter(val context: Context,
                            private val userInfo: MutableList<User>,
                            val clickListener: SendClickListener
): RecyclerView.Adapter<FollowRecyclerAdapter.ProfileView>() {
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    interface SendClickListener{
        fun onProfileClickListener(user: User)
    }

    override fun getItemCount(): Int {
        return userInfo.size
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ProfileView {

        val cell =  LayoutInflater.from(context).inflate(R.layout.layout_item_following, p0, false)

        return ProfileView(cell!!)


    }

    override fun onBindViewHolder(holder: ProfileView, position: Int) {
        //Profile info
        val user = userInfo[position]
        holder.userInfo = user
        holder.at.text = "@"
        holder.username.text = user.username
        holder.displayName.text = user.display_name
        Glide
            .with(context.applicationContext)
            .load(Uri.parse(user.profile_photo))
            .into(holder.profilePhoto)



        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference


    }





    inner class ProfileView(itemView: View): RecyclerView.ViewHolder(itemView){


        val profilePhoto  = itemView.profile_photo
        val at  = itemView.at
        val username = itemView.username
        val displayName = itemView.display_name
        private val mItem = itemView.relLayout1
        var userInfo : User? = null


        init {
            mItem.setOnClickListener {
                clickListener.onProfileClickListener(userInfo!!)

            }



        }
    }
}