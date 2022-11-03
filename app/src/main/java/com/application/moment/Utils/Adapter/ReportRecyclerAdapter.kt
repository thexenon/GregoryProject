package com.application.moment.Utils.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.application.moment.R
import com.application.moment.models.Report
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.layout_item_notifications.view.relLayout1
import kotlinx.android.synthetic.main.layout_item_report.view.*

class ReportRecyclerAdapter(val context: Context,
                           private val reportList: MutableList<Report>,
                           val clickListener: SendClickListener
): RecyclerView.Adapter<ReportRecyclerAdapter.ViewHolder>() {


    //FIREBASE
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef: DatabaseReference
    private lateinit var mFirebaseDatabase: FirebaseDatabase

    interface SendClickListener {
        fun onMessageClickListener(report: Report)
    }

    override fun getItemCount(): Int {
        return reportList.size
    }

    override fun onCreateViewHolder(view: ViewGroup, position: Int): ViewHolder {
        val cell = LayoutInflater.from(context).inflate(R.layout.layout_item_report, view, false)
        return ViewHolder(cell)


    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val report = reportList[position]

        auth = Firebase.auth
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        myRef = mFirebaseDatabase.reference
        holder.tvTicket.text = report.ticket

        val query: Query = myRef
            .child(context.getString(R.string.dbname_report))
            .child(report.ticket)
        query.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child(context.getString(R.string.seen)).value == true){
                    holder.isSeen.visibility = View.VISIBLE
                }else{
                    holder.isSeen.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        holder.relLayout.setOnClickListener {
            clickListener.onMessageClickListener(report)
        }


    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvTicket = itemView.tv_ticket
        val isSeen = itemView.check
        val relLayout = itemView.relLayout1


    }

}