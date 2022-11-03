package com.application.moment.Settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.application.moment.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_edit_video.*
import java.util.HashMap

class EditVideoActivity : AppCompatActivity() {

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var myTitle : EditText ? = null
    private var myDesc : EditText? = null

    //VAR
    private var myVideoID = ""
    private var myCategory = ""

    @SuppressLint("CutPasteId")
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_video)

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference

        myVideoID = intent.getStringExtra(getString(R.string.video_id)).toString()
        myTitle = findViewById(R.id.title_video_edit)
        myDesc = findViewById(R.id.description_edit)



        back_arrow.setOnClickListener {
            finish()
        }


        val txtLength = findViewById<TextView>(R.id.stringSize)
        val description = findViewById<EditText>(R.id.description_edit)
        description.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = description.text.toString().length
                txtLength.text = "$length / 200"
            }
        })

        val txtTitleLength = findViewById<TextView>(R.id.stringTitleSize)
        val titleVideo = findViewById<EditText>(R.id.title_video_edit)
        titleVideo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = titleVideo.text.toString().length
                txtTitleLength.text = "$length / 40"
            }
        })

        getVideoInfo()
        modify.setOnClickListener {
            if(title_video_edit.text.toString() != "" && description_edit.text.toString() != ""){
                modifyData()
                finish()

            }
        }



    }

    private fun modifyData(){
        myRef.child(getString(R.string.dbname_videos))
            .child(auth.currentUser!!.uid)
            .child(myVideoID)
            .child(getString(R.string.description))
            .setValue(description_edit.text.toString())

        myRef.child(getString(R.string.dbname_videos))
            .child(auth.currentUser!!.uid)
            .child(myVideoID)
            .child(getString(R.string.title))
            .setValue(title_video_edit.text.toString())


        myRef.child(getString(R.string.dbname_categories))
            .child(myCategory)
            .child(myVideoID)
            .child(getString(R.string.title))
            .setValue(title_video_edit.text.toString())
        myRef.child(getString(R.string.dbname_categories))
            .child(myCategory)
            .child(myVideoID)
            .child(getString(R.string.description))
            .setValue(description_edit.text.toString())
    }

    @Suppress("unchecked_cast")
    private fun getVideoInfo(){
        val query: Query = myRef
            .child(getString(R.string.dbname_videos))
            .child(auth.currentUser!!.uid)
            .child(myVideoID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val objectMap =
                    dataSnapshot.value as HashMap<String?, Any?>?
                val descriptionF = objectMap?.get(getString(R.string.description)).toString()
                val category = objectMap?.get(getString(R.string.category)).toString()
                val title = objectMap?.get(getString(R.string.title)).toString()
                title_video_edit.setText(title)
                description_edit.setText(descriptionF)
                myCategory = category


            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

}