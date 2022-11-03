package com.application.moment.Dialogs

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.R
import com.application.moment.Search.ViewProfileActivity
import com.application.moment.Utils.Adapter.FollowRecyclerAdapter
import com.application.moment.models.Following
import com.application.moment.models.User
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class BottomSheetFollowing : BottomSheetDialogFragment(), FollowRecyclerAdapter.SendClickListener{
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //WIDGETS
    private var mRecycler : RecyclerView ? = null
    private var adapter: FollowRecyclerAdapter?= null
    private var mUsers = mutableListOf<User>()
    private var mUsersPaginated = mutableListOf<User>()
    private var mFollowing = mutableListOf<Following>()
    private var layoutManager: RecyclerView.LayoutManager? = null

    //VAR
    private var mUID = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_item_dialog_following, container, false)

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        mUID = tag.toString()
        mRecycler = view?.findViewById(R.id.following_recycler)
        setAdapter()
        setupRecycler()
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.MyDialog)
    }

    private fun setAdapter(){

        layoutManager = LinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL,false)
        mRecycler?.layoutManager = layoutManager

        if (adapter == null){
            adapter = FollowRecyclerAdapter(activity!! , mUsersPaginated, this)
            mRecycler?.adapter = adapter
        } else{
            adapter!!.notifyDataSetChanged()
        }
    }

    private fun setupRecycler(){
        mUsers.clear()
        val query: Query = myRef
            .child(getString(R.string.following))
            .child(mUID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val myFollowing = singleSnapshot.getValue(Following::class.java)
                    mFollowing.add(myFollowing!!)
                }
                displayFollowing()


            }


            override fun onCancelled(databaseError: DatabaseError) {}


        })


    }


    private fun displayFollowing(){
        mUsersPaginated.clear()
        try {
            mFollowing.sortWith(Comparator { o1, o2 -> o2!!.date_created.compareTo(o1!!.date_created) })
            for (i in 0 until mFollowing.size){

                val query3: Query = myRef
                    .child(getString(R.string.dbname_users))
                    .child(mFollowing[i].user_id)
                query3.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val user = dataSnapshot.getValue(User::class.java)
                        mUsersPaginated.add(user!!)
                        Log.d(TAG, "onDataChange: mUserPaginated = $mUsersPaginated")
                        setAdapter()

                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })


            }




        } catch (e: NullPointerException) {
            Log.e(TAG, "displayVideos: NullPointerException", e)
        } catch (ind: IndexOutOfBoundsException) {
            Log.e(TAG, "displayVideos: IndexOutOfBoundsException", ind)
        }




    }
    override fun onProfileClickListener(user: User) {
        val intent = Intent(context!!.applicationContext, ViewProfileActivity::class.java).putExtra(getString(R.string.user_id), user.userID)
        startActivity(intent)
    }


}