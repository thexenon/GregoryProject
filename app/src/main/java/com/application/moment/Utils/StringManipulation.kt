package com.application.moment.Utils

import java.math.RoundingMode
import java.text.DecimalFormat

class StringManipulation{
    fun condenseUsername(username: String): String? {
        return username.replace(" ", "_")
    }
    fun condenseNumber(number: Int): String? {
         if (number >= 1000){
             if (number >= 10000){
                 if (number >= 100000){
                     if (number >= 1000000){
                         val temp = number/1000000
                         val df = DecimalFormat("#.##")
                         df.roundingMode = RoundingMode.FLOOR
                         return "${df.format(temp).toDouble()}m"
                     }else{
                         return "error"
                     }
                 }else{
                     val temp = number/10000
                     val df = DecimalFormat("##.#")
                     df.roundingMode = RoundingMode.FLOOR
                     return "${df.format(temp).toDouble()}k"
                 }
             }else{
                 val temp = number/1000
                 val df = DecimalFormat("#.##")
                 df.roundingMode = RoundingMode.FLOOR
                 return "${df.format(temp).toDouble()}k"
             }
        }else{
            return "$number"
        }

    }
}