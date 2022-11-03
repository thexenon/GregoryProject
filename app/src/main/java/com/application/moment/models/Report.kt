package com.application.moment.models

data class Report(val video_id: String="", val video_path: String="", val title: String="", val description : String="",
val user_id:String ="", val username:String="", val email: String = "", val report_for: String ="", val ticket: String ="",
val date_created: String = "", val seen: Boolean = false)