package com.application.moment.Settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.application.moment.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_settings.view.back_arrow
import kotlinx.android.synthetic.main.fragment_email.view.*

class EmailFragment : Fragment(){

    //Firebase
    private lateinit var auth : FirebaseAuth
    private lateinit var refUsers : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    private var emailField : EditText ? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_email, container, false)

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        refUsers = mFirebaseDatabase.reference
        emailField = view.findViewById(R.id.email)


        view.back_arrow.setOnClickListener {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity?.startActivity(intent)
            activity?.finish()
        }

        init()
        view.save.setOnClickListener {
            updateEmail()
        }
        return view
    }

    private fun init(){
        val query: Query = refUsers
            .child(getString(R.string.dbname_users))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val emailF = dataSnapshot.child(getString(R.string.email)).value
                    emailField?.setText(emailF.toString())
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun updateEmail(){
        if (emailField?.text.toString() != ""){
            val user = auth.currentUser
            user?.updateEmail(emailField?.text.toString())
            user?.sendEmailVerification()

            refUsers.child(getString(R.string.dbname_users))
                .child(auth.currentUser!!.uid)
                .child(getString(R.string.email))
                .removeValue()

            refUsers.child(getString(R.string.dbname_users))
                .child(auth.currentUser!!.uid)
                .child(getString(R.string.email))
                .setValue(emailField?.text.toString())


            val intent = Intent(activity, SettingsActivity::class.java)
            activity?.startActivity(intent)
            activity?.finish()
        }
    }
}