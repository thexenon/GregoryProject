package com.application.moment.Utils

import android.graphics.Bitmap

import android.graphics.BitmapFactory
import android.util.Log
import java.io.*


object ImageManager {
    private const val TAG = "ImageManager"
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun getBitmap(imgUrl: String?): Bitmap? {
        val imageFile = File(imgUrl)
        var fis: FileInputStream? = null
        var bitmap: Bitmap? = null
        try {
            fis = FileInputStream(imageFile)
            bitmap = BitmapFactory.decodeStream(fis)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "getBitmap: FileNotFoundException: $e")
        } finally {
            try {
                fis?.close()
            } catch (e: IOException) {
                Log.e(TAG, "getBitmap: FileNotFoundException: $e")
            }
        }
        return bitmap
    }

    /**
     * return byte array from a bitmap
     * quality is greater than 0 but less than 100
     * @param bm
     * @param quality
     * @return
     */
    fun getBytesFromBitmap(bm: Bitmap?, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bm?.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}