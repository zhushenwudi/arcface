package com.ilab.arcface.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Base64
import android.view.View
import com.arcsoft.face.ActiveFileInfo
import com.arcsoft.face.ErrorInfo
import com.arcsoft.face.FaceEngine
import com.arcsoft.imageutil.ArcSoftImageFormat
import com.arcsoft.imageutil.ArcSoftImageUtil
import com.arcsoft.imageutil.ArcSoftImageUtilError
import com.arcsoft.imageutil.ArcSoftRotateDegree
import java.io.ByteArrayOutputStream
import java.lang.reflect.Modifier


object FaceUtil {
    /**
     * 将ArcFace错误码转换为对应的错误码常量名，便于理解
     * TODO:目前每次都遍历，如果使用频繁，建议将Field缓存处理，避免每次都反射
     *
     * @param code 错误码
     * @return 错误码常量名
     */
    fun arcFaceErrorCodeToFieldName(code: Int): String? {
        val declaredFields = ErrorInfo::class.java.declaredFields
        for (declaredField in declaredFields) {
            try {
                if (Modifier.isFinal(declaredField.modifiers) && declaredField[ErrorInfo::class.java] as Int == code) {
                    return declaredField.name
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        return "unknown error"
    }

    /**
     * 将ArcSoftImageUtil错误码转换为对应的错误码常量名，便于理解
     * TODO:目前每次都遍历，如果使用频繁，建议将Field缓存处理，避免每次都反射
     *
     * @param code 错误码
     * @return 错误码常量名
     */
    fun imageUtilErrorCodeToFieldName(code: Int): String? {
        val declaredFields = ArcSoftImageUtilError::class.java.declaredFields
        for (declaredField in declaredFields) {
            try {
                if (Modifier.isFinal(declaredField.modifiers) && declaredField[ArcSoftImageUtilError::class.java] as Int == code) {
                    return declaredField.name
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        return "unknown error"
    }

    fun isActivated(context: Context?): Boolean {
        return FaceEngine.getActiveFileInfo(context, ActiveFileInfo()) == ErrorInfo.MOK
    }

    /**
     * 将图像中需要截取的Rect向外扩张一倍，若扩张一倍会溢出，则扩张到边界，若Rect已溢出，则收缩到边界
     *
     * @param width   图像宽度
     * @param height  图像高度
     * @param srcRect 原Rect
     * @return 调整后的Rect
     */
    fun getBestRect(width: Int, height: Int, srcRect: Rect?): Rect? {
        if (srcRect == null) {
            return null
        }
        val rect = Rect(srcRect)

        // 原rect边界已溢出宽高的情况
        val maxOverFlow = (-rect.left).coerceAtLeast(
            (-rect.top).coerceAtLeast(
                (rect.right - width).coerceAtLeast(rect.bottom - height)
            )
        )
        if (maxOverFlow >= 0) {
            rect.inset(maxOverFlow, maxOverFlow)
            return rect
        }

        // 原rect边界未溢出宽高的情况
        var padding = rect.height() / 2

        // 若以此padding扩张rect会溢出，取最大padding为四个边距的最小值
        if (!(rect.left - padding > 0 && rect.right + padding < width && rect.top - padding > 0 && rect.bottom + padding < height)) {
            padding = rect.left.coerceAtMost(width - rect.right).coerceAtMost(height - rect.bottom)
                .coerceAtMost(rect.top)
        }
        rect.inset(-padding, -padding)
        return rect
    }

    /**
     * 截取合适的头像并旋转，保存为注册头像
     *
     * @param originImageData 原始的BGR24数据
     * @param width           BGR24图像宽度
     * @param height          BGR24图像高度
     * @param orient          人脸角度
     * @param cropRect        裁剪的位置
     * @param imageFormat     图像格式
     * @return 头像的图像数据
     */
    fun getHeadImage(
        originImageData: ByteArray,
        width: Int,
        height: Int,
        orient: Int,
        cropRect: Rect,
        imageFormat: ArcSoftImageFormat
    ): Bitmap? {
        val headImageData =
            ArcSoftImageUtil.createImageData(cropRect.width(), cropRect.height(), imageFormat)
        val cropCode = ArcSoftImageUtil.cropImage(
            originImageData,
            headImageData,
            width,
            height,
            cropRect,
            imageFormat
        )
        if (cropCode != ArcSoftImageUtilError.CODE_SUCCESS) {
            throw RuntimeException("crop image failed, code is $cropCode")
        }

        //判断人脸旋转角度，若不为0度则旋转注册图
        var rotateHeadImageData: ByteArray? = null
        val cropImageWidth: Int
        val cropImageHeight: Int
        // 90度或270度的情况，需要宽高互换
        if (orient == FaceEngine.ASF_OC_90 || orient == FaceEngine.ASF_OC_270) {
            cropImageWidth = cropRect.height()
            cropImageHeight = cropRect.width()
        } else {
            cropImageWidth = cropRect.width()
            cropImageHeight = cropRect.height()
        }
        var rotateDegree: ArcSoftRotateDegree? = null
        when (orient) {
            FaceEngine.ASF_OC_90 -> rotateDegree = ArcSoftRotateDegree.DEGREE_270
            FaceEngine.ASF_OC_180 -> rotateDegree = ArcSoftRotateDegree.DEGREE_180
            FaceEngine.ASF_OC_270 -> rotateDegree = ArcSoftRotateDegree.DEGREE_90
            FaceEngine.ASF_OC_0 -> rotateHeadImageData = headImageData
            else -> rotateHeadImageData = headImageData
        }
        // 非0度的情况，旋转图像
        if (rotateDegree != null) {
            rotateHeadImageData = ByteArray(headImageData.size)
            val rotateCode = ArcSoftImageUtil.rotateImage(
                headImageData,
                rotateHeadImageData,
                cropRect.width(),
                cropRect.height(),
                rotateDegree,
                imageFormat
            )
            if (rotateCode != ArcSoftImageUtilError.CODE_SUCCESS) {
                throw RuntimeException(
                    "rotate image failed, code is : $rotateCode, code description is : " + imageUtilErrorCodeToFieldName(
                        rotateCode
                    )
                )
            }
        }
        // 将创建一个Bitmap，并将图像数据存放到Bitmap中
        val headBmp = Bitmap.createBitmap(cropImageWidth, cropImageHeight, Bitmap.Config.RGB_565)
        val imageDataToBitmapCode =
            ArcSoftImageUtil.imageDataToBitmap(rotateHeadImageData, headBmp, imageFormat)
        if (imageDataToBitmapCode != ArcSoftImageUtilError.CODE_SUCCESS) {
            throw RuntimeException(
                "failed to transform image data to bitmap, code is : $imageDataToBitmapCode, code description is : " + imageUtilErrorCodeToFieldName(
                    imageDataToBitmapCode
                )
            )
        }
        return headBmp
    }

    fun bitmapToBase64(bitmap: Bitmap?): String? {
        var quality = 100
        var result: String? = null
        var baos: ByteArrayOutputStream? = null
        try {
            if (bitmap != null) {
                baos = ByteArrayOutputStream()
                while (result == null || result.length > 240000) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    val bitmapBytes: ByteArray = baos.toByteArray()
                    result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT)
                    baos.reset()
                    quality -= 5
                }
            }
        } finally {
            try {
                if (baos != null) {
                    baos.flush()
                    baos.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }

    fun getRectInScreen(v: View): Rect {
        var view: View
        val r = Rect()
        val w = v.width
        val h = v.height
        r.left = v.left
        r.top = v.top
        r.right = r.left + w
        r.bottom = r.top + h
        var p = v.parent
        while (p is View) {
            view = p
            p = view.parent
            r.left += view.left
            r.top += view.top
            r.right += view.left
            r.bottom += view.top
        }
        return r
    }
}