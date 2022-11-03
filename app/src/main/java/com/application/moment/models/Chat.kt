package com.application.moment.models

data class Chat(val sender: String = "", val receiver: String= "", val message : String= "", val date_created : String ="", var seen: Boolean = false)