package com.ilab.arcface.util.face.model

import com.arcsoft.face.LivenessParam

/**
 * 人脸识别认证配置
 */
data class RecognizeConfiguration(
    // 产生特征提取失败示语的特征提取次数（小于该值不提示）
    val extractRetryCount: Int = 3,

    // 产生活体检测失败示语的活体检测次数（小于该值不提示）
    val livenessRetryCount: Int = 3,

    // 最大人脸检测数量
    val maxDetectFaces: Int = 1,

    // 识别阈值
    val similarThreshold: Float = 0.8F,

    // 图像质量检测阈值：适用于不戴口罩且人脸识别场景
    val imageQualityNoMaskRecognizeThreshold: Float = 0.5F,

    // 图像质量检测阈值：适用于戴口罩且人脸识别场景
    val imageQualityMaskRecognizeThreshold: Float = 0.3F,

    // 识别失败重试间隔
    val recognizeFailedRetryInterval: Int = 0,

    // 活体检测未通过重试间隔
    val livenessFailedRetryInterval: Int = 0,

    // 启用活体
    val enableLiveness: Boolean = true,

    // 启用图像质量检测
    val enableImageQuality: Boolean = true,

    // 识别区域限制
    val enableFaceAreaLimit: Boolean = true,

    // 仅识别最大人脸
    val keepMaxFace: Boolean = true,

    // 启用人脸边长限制
    val enableFaceSizeLimit: Boolean = false,

    // 启用人脸移动限制
    val enableFaceMoveLimit: Boolean = false,

    // 人脸边长限制值
    val faceSizeLimit: Int = 0,

    // 人脸上下针移动限制值
    val faceMoveLimit: Int = 0,

    // 活体阈值设置
    val livenessParam: LivenessParam = LivenessParam(0.6F, 0.3F),

    // 照相机横向识别偏移值
    val dualCameraHorizontalOffset: Int = 130,

    // 照相机纵向识别偏移值
    val dualCameraVerticalOffset: Int = 0
)
