package com.ilab.arcface

import android.Manifest

object Const {
    const val FACE_APP_ID = "5BVpM8i3Q25zi1PMJAua7GfP7RTwncjzUVbzk6mb7Khd"
    const val FACE_SDK_KEY = "GkkAL4WCgCusbLrsfDxeNtSRcXsuY1w14ihMxLnZBXv3"

    const val VERIFY_OVER = "人脸验证通过"
    const val FOUND_FACE = "已拿到人脸图片"
    const val SERVER_ERROR = "服务器异常"
    const val NEED_DANGEROUS_MANAGER = "请危化品保管员认证"
    const val NEED_ANOTHER_DANGEROUS_MANAGER = "请其他危化品保管员认证"
    const val NEED_USER_MANAGER = "请任意领取人识别认证"
    const val NEED_ANOTHER_USER_MANAGER = "请其他领取人识别认证"
    const val NEED_LOGINER_MANAGER = "请当前登录用户识别认证"
    const val TIME_OUT_TEXT = "采集超时，请重试"

    const val CODE_BACKWARD = -8
    const val CODE_LEFT = -9
    const val CODE_UP = -10
    const val CODE_RIGHT = -11
    const val CODE_DOWN = -12
    const val SINGLE_CHECK_SUCCESS = "人脸识别成功"
    const val SINGLE_CHECK_FAILED = "请尝试其他角度"
    const val MULTI_PEOPLE = "框内存在多人"
    const val SINGLE_NOT_FOUND_FACE = "把脸移入框内"
    const val POSITIVE_SCREEN = "请正对屏幕"
    const val UP_FACE = "请向上"
    const val DOWN_FACE = "请向下"
    const val LEFT_FACE = "请向左"
    const val RIGHT_FACE = "请向右"
    const val BACKWARD_FACE = "请向后"
    const val FORWARD_FACE = "请向前"
    const val FULL_LIGHT = "光线太强了"

    val COMMON_PERMISSION = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
}