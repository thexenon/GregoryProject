package com.application.moment.Dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.application.moment.R
import com.application.moment.Search.ViewProfileActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import de.hdodenhof.circleimageview.CircleImageView

class BottomSheetMessages : BottomSheetDialogFragment(){

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var mYes: Button? = null
    private var mProfilePhoto: CircleImageView ? = null
    private var mAt: TextView ? = null
    private var mUsername: TextView ? = null
    private var mDisplayName: TextView ? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_item_dialog_messages, container, false)
        mYes= view!!.findViewById(R.id.yes)
        mProfilePhoto = view.findViewById(R.id.profile_photo)
        mUsername = view.findViewById(R.id.username)
        mDisplayName = view.findViewById(R.id.display_name)
        mAt = view.findViewById(R.id.at)
        mAt?.text = "@"
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        val mArg = arguments
        val userID = mArg?.getString(getString(R.string.user_id))
        val username = mArg?.getString(getString(R.string.username))
        val displayName = mArg?.getString(getString(R.string.display_name))
        val profilePhotoUrl = mArg?.getString(getString(R.string.profile_photo))
        mUsername?.text = username
        mDisplayName?.text = displayName

        mDisplayName?.setOnClickListener{
            val intent = Intent(activity, ViewProfileActivity::class.java).putExtra(getString(R.string.user_id), userID)
            startActivity(intent)
        }
        mUsername?.setOnClickListener{
            val intent = Intent(activity, ViewProfileActivity::class.java).putExtra(getString(R.string.user_id), userID)
            startActivity(intent)
        }
        mProfilePhoto?.setOnClickListener{
            val intent = Intent(activity, ViewProfileActivity::class.java).putExtra(getString(R.string.user_id), userID)
            startActivity(intent)
        }
        Glide
            .with(context!!.applicationContext)
            .load(Uri.parse(profilePhotoUrl))
            .into(mProfilePhoto!!)


        mYes?.setOnClickListener{
           delete(userID!!)
        }
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.MyDialog)
    }

    private fun delete(userID: String){
        myRef.child(getString(R.string.dbname_message_order))
            .child(auth.currentUser!!.uid)
            .child(userID)
            .removeValue()

        myRef.child(getString(R.string.dbname_message_order))
            .child(userID)
            .child(auth.currentUser!!.uid)
            .removeValue()

        Toast.makeText(context, "Delete success", Toast.LENGTH_SHORT).show()
        dialog!!.dismiss()


    }


}