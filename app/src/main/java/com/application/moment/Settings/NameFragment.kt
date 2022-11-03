package com.application.moment.Settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.application.moment.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_signout.view.*

class NameFragment : Fragment(){
    //Firebase
    private lateinit var auth : FirebaseAuth
    private lateinit var refUsers : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var name : EditText? = null
    private var btnSave : Button? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_name, container, false)

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        refUsers = mFirebaseDatabase.reference

        name = view.findViewById(R.id.name)
        btnSave = view.findViewById(R.id.save)
        init()

        view.back_arrow.setOnClickListener {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity?.startActivity(intent)
            activity?.finish()
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
                    val displayName = dataSnapshot.child(getString(R.string.display_name)).value
                    name?.setText(displayName.toString())
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        btnSave?.setOnClickListener {
            if (name?.text.toString() != ""){
                refUsers.child(getString(R.string.dbname_users))
                    .child(auth.currentUser!!.uid)
                    .child(getString(R.string.display_name))
                    .removeValue()

                refUsers.child(getString(R.string.dbname_users))
                    .child(auth.currentUser!!.uid)
                    .child(getString(R.string.display_name))
                    .setValue(name?.text.toString())

                val intent = Intent(activity, SettingsActivity::class.java)
                activity?.startActivity(intent)
                activity?.finish()
            }else{
                Toast.makeText(
                    activity, "Name field can't be blank.",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

    }
}