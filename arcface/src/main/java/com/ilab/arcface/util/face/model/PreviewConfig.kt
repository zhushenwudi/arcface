package com.ilab.arcface.util.face.model

import android.hardware.Camera

object PreviewConfig {
    const val rgbCameraId = Camera.CameraInfo.CAMERA_FACING_BACK

    const val irCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT

    const val rgbAdditionalDisplayOrientation: Int = 0

    const val irAdditionalDisplayOrientation: Int = 0
}