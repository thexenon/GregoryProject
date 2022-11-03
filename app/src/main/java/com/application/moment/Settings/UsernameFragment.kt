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
import com.application.moment.Utils.StringManipulation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_username.view.*
import java.util.*

class UsernameFragment : Fragment(){

    //Firebase
    private lateinit var auth : FirebaseAuth
    private lateinit var refUsers : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase


    private var usernameExist : Boolean = false
    private var usernameField : EditText ? =null
    private var btnSave : Button?= null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_username, container, false)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        refUsers = mFirebaseDatabase.reference

        usernameField = view?.findViewById(R.id.username)
        btnSave = view?.findViewById(R.id.save)

        view.back_arrow.setOnClickListener {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity?.startActivity(intent)
            activity?.finish()
        }

        init()



        return view
    }

    private fun init(){

        val query: Query = refUsers
            .child(getString(R.string.dbname_users))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val usernameF = dataSnapshot.child(getString(R.string.username)).value
                    usernameField?.setText(usernameF.toString().toLowerCase(Locale.getDefault()))
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

        btnSave?.setOnClickListener {
            checkIfUsernameExist(view!!.username.toString())
            if (usernameField?.text.toString() != "") {
                if (!usernameExist) {
                    Toast.makeText(
                        activity, "Your username changed.",
                        Toast.LENGTH_SHORT
                    ).show()

                    refUsers.child(getString(R.string.dbname_users))
                        .child(auth.currentUser!!.uid)
                        .child(getString(R.string.username))
                        .removeValue()

                    refUsers.child(getString(R.string.dbname_users))
                        .child(auth.currentUser!!.uid)
                        .child(getString(R.string.username))
                        .setValue(StringManipulation().condenseUsername(usernameField?.text.toString()))



                    val intent = Intent(activity, SettingsActivity::class.java)
                    activity?.startActivity(intent)
                    activity?.finish()
                } else {
                    Toast.makeText(
                        activity, "This username already exist.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }else{
                Toast.makeText(
                    activity, "Username field can't be blank.",
                    Toast.LENGTH_SHORT
                ).show()

        }

        }
    }

    private fun checkIfUsernameExist(username: String){
        val reference = FirebaseDatabase.getInstance().reference
        val query: Query = reference
            .child(getString(R.string.dbname_users))
            .orderByChild(getString(R.string.username))
            .equalTo(username)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    if (singleSnapshot.exists()) {
                        usernameExist = true
                        Toast.makeText(activity, "Username already exist.",
                            Toast.LENGTH_SHORT).show()

                    }else{
                        usernameExist = false
                    }
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
}