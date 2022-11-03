package com.application.moment.Dialogs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import com.application.moment.Profile.ProfileActivity
import com.application.moment.R
import com.application.moment.Settings.EditVideoActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class BottomSheet : BottomSheetDialogFragment(){

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var modifyVideo : RelativeLayout ? = null
    private var deleteVideo : RelativeLayout ? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_item_dialog_profile, container, false)
        modifyVideo = view!!.findViewById(R.id.relLayout1)
        deleteVideo = view.findViewById(R.id.relLayout2)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        val mArg = arguments
        val videoID = mArg?.getString(getString(R.string.video_id))
        val category = mArg?.getString(getString(R.string.category))
        modifyVideo?.setOnClickListener {
            val intent = Intent(activity, EditVideoActivity::class.java).putExtra(getString(R.string.video_id), tag)
            activity!!.startActivity(intent)
        }

        deleteVideo?.setOnClickListener {
            delete(videoID.toString(), category.toString())
        }


        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.MyDialog)
    }

    private fun delete(videoID: String, category:String){
        myRef.child(getString(R.string.dbname_videos))
            .child(auth.currentUser!!.uid)
            .child(videoID)
            .removeValue()


        myRef.child(getString(R.string.dbname_categories))
            .child(category)
            .child(videoID)
            .removeValue()


        Toast.makeText(context, "Delete success, refresh your profile.", Toast.LENGTH_SHORT).show()

        val intent = Intent(activity, ProfileActivity::class.java)
        activity?.startActivity(intent)
        activity?.finish()

    }


}