package com.application.moment.Home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.Dialogs.BottomSheetMessages
import com.application.moment.Messages.ChatActivity
import com.application.moment.R
import com.application.moment.Utils.Adapter.MessagesRecyclerAdapter
import com.application.moment.models.Chat
import com.application.moment.models.MessageOrder
import com.application.moment.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.util.*


class MessagesFragment : Fragment(), MessagesRecyclerAdapter.SendClickListener{
    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase
    private var adapter: MessagesRecyclerAdapter?= null
    private var mRecyclerView: RecyclerView ? = null
    private var unRead: TextView ? = null

    private var mPaginatedChat: MutableList<User> = mutableListOf()
    private var mMessageOrderList: MutableList<MessageOrder> = mutableListOf()
    private var layoutManager: RecyclerView.LayoutManager? = null


    companion object{
        private const val TAG = "MessagesFragment"
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_messages, container, false)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        mRecyclerView = view?.findViewById(R.id.messages_recycler)
        unRead = view.findViewById(R.id.text_unread)
        setAdapter()
        Log.d(TAG, "onCreateView: started.")

        // Refresh the chat and give unread number
        val reference = FirebaseDatabase.getInstance().getReference(getString(R.string.dbname_chat))
        reference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    var unread = 0
                    for (snapshot in dataSnapshot.children) {
                        val chat = snapshot.getValue(Chat::class.java)!!
                        if (chat.receiver == auth.currentUser!!.uid && !chat.seen) {
                            unread++
                        }
                    }
                    if (unread == 0) {
                        unRead?.text = ""
                    } else {
                        unRead?.text = "($unread)"
                    }
                    try{
                        setupRecycler()
                    }catch (e : IllegalStateException){
                        e.message
                    }
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        return view
    }


    private fun setAdapter(){

        layoutManager = LinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL,false)
        mRecyclerView?.layoutManager = layoutManager

        if (adapter == null){
            adapter = MessagesRecyclerAdapter(activity!!,mPaginatedChat,this)
            mRecyclerView?.adapter = adapter
        } else{
            adapter!!.notifyDataSetChanged()
        }
    }


    private fun setupRecycler() {
        mMessageOrderList.clear()
        val query: Query = myRef
            .child(getString(R.string.dbname_message_order))
            .child(auth.currentUser!!.uid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val mMessageOrder = singleSnapshot.getValue(MessageOrder::class.java)
                    mMessageOrderList.add(mMessageOrder!!)
                }
                getOrder()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getOrder(){
        mPaginatedChat.clear()
        mMessageOrderList.sortWith(Comparator { o1, o2 -> o2!!.date_created.compareTo(o1!!.date_created) })
        for (i in 0 until mMessageOrderList.size){

            val query3: Query = myRef
                .child(getString(R.string.dbname_users))
                .child(mMessageOrderList[i].user_id)
            query3.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    try{
                        val user = dataSnapshot.getValue(User::class.java)
                        mPaginatedChat.add(user!!)
                        setAdapter()
                    }catch (e: NullPointerException){
                        e.message
                    }


                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })


        }
    }





    override fun onMessageClickListener(user: User) {
        val intent = Intent(activity, ChatActivity::class.java).putExtra(getString(R.string.user_id), user.userID)
        activity!!.startActivity(intent)
    }

    override fun onMessageLongClickListener(user: User) {
        val bottomSheetMessages = BottomSheetMessages()
        val args = Bundle()
        args.putString(getString(R.string.username), user.username)
        args.putString(getString(R.string.user_id), user.userID)
        args.putString(getString(R.string.display_name), user.display_name)
        args.putString(getString(R.string.profile_photo), user.profile_photo)
        bottomSheetMessages.arguments = args
        bottomSheetMessages.show(activity!!.supportFragmentManager, user.userID)
    }

    override fun onResume() {
        super.onResume()
        HomeFragment().pauseVideo()
    }
}