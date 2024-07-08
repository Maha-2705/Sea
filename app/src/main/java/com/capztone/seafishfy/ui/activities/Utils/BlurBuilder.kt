package com.capztone.seafishfy.ui.activities.Utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

object BlurBuilder {
    private const val BLUR_RADIUS = 5f

    fun blur(context: Context, image: Bitmap): Bitmap {
        val outputBitmap = Bitmap.createBitmap(image)
        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, image)
        val output = Allocation.createFromBitmap(rs, outputBitmap)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(BLUR_RADIUS)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(outputBitmap)
        return outputBitmap
    }

    fun blur(context: Context, imageResId: Int): Bitmap {
        val image = BitmapFactory.decodeResource(context.resources, imageResId)
        return blur(context, image)
    }
}
