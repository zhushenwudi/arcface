package com.ilab.arcface.util.camera

import android.hardware.Camera
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import com.ilab.arcface.widget.FaceRectView
import com.zhushenwudi.base.app.appContext
import com.zhushenwudi.base.ext.util.windowManager

object CameraUtil {
    fun adjustPreviewViewSize(
        rgbPreview: View,
        previewView: View,
        faceRectView: FaceRectView,
        previewSize: Camera.Size,
        displayOrientation: Int,
        scale: Float
    ): ViewGroup.LayoutParams {
        val layoutParams = previewView.layoutParams
        val measuredWidth = previewView.measuredWidth
        val measuredHeight = previewView.measuredHeight
        var ratio = previewSize.height.toFloat() / previewSize.width.toFloat()
        if (ratio > 1) {
            ratio = 1 / ratio
        }
        if (displayOrientation % 180 == 0) {
            layoutParams.width = measuredWidth
            layoutParams.height = (measuredWidth * ratio).toInt()
        } else {
            layoutParams.height = measuredHeight
            layoutParams.width = (measuredHeight * ratio).toInt()
        }
        if (scale < 1f) {
            val rgbParam = rgbPreview.layoutParams
            layoutParams.width = (rgbParam.width * scale).toInt()
            layoutParams.height = (rgbParam.height * scale).toInt()
        } else {
            layoutParams.width *= scale.toInt()
            layoutParams.height *= scale.toInt()
        }
        val metrics = DisplayMetrics()
        appContext.windowManager?.defaultDisplay?.getMetrics(metrics)
        if (layoutParams.width >= metrics.widthPixels) {
            val viewRatio = layoutParams.width / metrics.widthPixels.toFloat()
            layoutParams.width /= viewRatio.toInt()
            layoutParams.height /= viewRatio.toInt()
        }
        if (layoutParams.height >= metrics.heightPixels) {
            val viewRatio = layoutParams.height / metrics.heightPixels.toFloat()
            layoutParams.width /= viewRatio.toInt()
            layoutParams.height /= viewRatio.toInt()
        }
        previewView.layoutParams = layoutParams
        faceRectView.layoutParams = layoutParams
        return layoutParams
    }
}