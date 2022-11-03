package com.application.moment.Dialogs

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.application.moment.R
import com.application.moment.models.Report
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.layout_item_dialog_viewprofile.view.*
import java.text.SimpleDateFormat
import java.util.*

class BottomSheetViewProfile : BottomSheetDialogFragment(){
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //VAR
    private var mIntent = ""
    private var email = ""
    private var username = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_item_dialog_viewprofile, container, false)

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        mIntent = tag.toString()
        val mArg = arguments
        val mVideoID = mArg?.getString(getString(R.string.video_id))
        val userID = mArg?.getString(getString(R.string.user_id))
        Log.d(TAG, "onCreateView: myVideoID: $mVideoID")
        val query: Query = myRef
            .child(getString(R.string.dbname_users))
            .child(userID.toString())
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                email = dataSnapshot.child(getString(R.string.email)).value.toString()
                username = dataSnapshot.child(getString(R.string.username)).value.toString()

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

        setupWidgets(view)
        return view
    }

    private fun setupWidgets(view: View ){

        val mArg = arguments
        val mVideoID = mArg?.getString(getString(R.string.video_id))
        val mVideoPath = mArg?.getString(getString(R.string.video_path))
        val mDesc = mArg?.getString(getString(R.string.description))
        val mTitle = mArg?.getString(getString(R.string.title))
        val userID = mArg?.getString(getString(R.string.user_id))
        var ticketCount = 0
        val query2: Query = myRef
            .child(getString(R.string.dbname_report))
        query2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    ticketCount++
                    Log.d(TAG, "onDataChange: for in datasnapshot work !")
                }



            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })


        ticketCount += 1

        view.item1.setOnClickListener {
            val reportContext = Report(mVideoID.toString(), mVideoPath.toString(), mTitle.toString(), mDesc.toString(),
                userID.toString(), username, email, "Nudity", "TICKET N${ticketCount}", getTimestamp().toString(), false)
            addTicket(ticketCount, reportContext)
        }
        view.item2.setOnClickListener {
            val reportContext = Report(mVideoID.toString(), mVideoPath.toString(), mTitle.toString(), mDesc.toString(),
                userID.toString(), username, email, "Harassment", "TICKET N${ticketCount}", getTimestamp().toString(), false)
            addTicket(ticketCount, reportContext)
        }

        view.item3.setOnClickListener {
            val reportContext = Report(mVideoID.toString(), mVideoPath.toString(), mTitle.toString(), mDesc.toString(),
                userID.toString(), username, email, "Drugs","TICKET N${ticketCount}", getTimestamp().toString(), false)
            addTicket(ticketCount, reportContext)
        }
        view.item2.setOnClickListener {
            val reportContext = Report(mVideoID.toString(), mVideoPath.toString(), mTitle.toString(), mDesc.toString(),
                userID.toString(), username, email, "Violent/Disturbing","TICKET N${ticketCount}", getTimestamp().toString(), false)
            addTicket(ticketCount, reportContext)
        }

    }
    private fun addTicket(ticketCount: Int, reportContext: Report){
        myRef.child(getString(R.string.dbname_report))
            .child("TICKET N${ticketCount}")
            .setValue(reportContext)
        Toast.makeText(context, "We will check the content. Thanks.",Toast.LENGTH_SHORT).show()
        activity?.supportFragmentManager?.popBackStack()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.MyDialog)
    }

    private fun getTimestamp(): String? {

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA)
        sdf.timeZone = TimeZone.getTimeZone("Canada/Pacific")
        return sdf.format(Date())
    }





}