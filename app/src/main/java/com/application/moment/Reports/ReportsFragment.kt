package com.application.moment.Reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.R
import com.application.moment.Settings.SettingsActivity
import com.application.moment.Utils.Adapter.ReportRecyclerAdapter
import com.application.moment.models.Report
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_tickets.view.*

class ReportsFragment : Fragment(), ReportRecyclerAdapter.SendClickListener{

    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef : DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    //VAR
    private var mRecyclerView : RecyclerView ? = null
    private var mTickets: MutableList<Report> = mutableListOf()
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: ReportRecyclerAdapter?= null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tickets, container, false)
        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        mRecyclerView = view!!.findViewById(R.id.reports_recycler)
        setAdapter()
        setupRecycler()
        view.back.setOnClickListener {
            val intent = Intent(activity, SettingsActivity::class.java)
            activity?.startActivity(intent)
            activity?.finish()

        }
        return view
    }

    private fun setAdapter(){

        layoutManager = LinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL, false)

        mRecyclerView?.layoutManager = layoutManager


        if (adapter == null){
            adapter = ReportRecyclerAdapter(activity!!, mTickets, this)
            mRecyclerView?.adapter = adapter
        } else{
            adapter!!.notifyDataSetChanged()

        }




    }

    private fun setupRecycler(){
        mTickets.clear()
        val query: Query = myRef
            .child(getString(R.string.dbname_report))
        query.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (datasnapshot in snapshot.children){
                    val report = datasnapshot.getValue(Report::class.java)
                    mTickets.add(report!!)

                }
                mTickets.reverse()

                adapter!!.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onMessageClickListener(report: Report) {
        val intent = Intent(context, ReportActivity::class.java).putExtra(getString(R.string.ticket), report.ticket)
        context?.startActivity(intent)
    }

}