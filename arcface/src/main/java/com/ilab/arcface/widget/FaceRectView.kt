package com.ilab.arcface.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.arcsoft.face.AgeInfo
import com.arcsoft.face.GenderInfo
import com.arcsoft.face.LivenessInfo
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 用于显示人脸信息的控件
 */
class FaceRectView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private val drawInfoList: CopyOnWriteArrayList<DrawInfo> = CopyOnWriteArrayList()

    // 画笔，复用
    private val paint: Paint = Paint()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawInfoList.size > 0) {
            for (i in drawInfoList.indices) {
                drawFaceRect(
                    canvas,
                    drawInfoList[i], DEFAULT_FACE_RECT_THICKNESS, paint
                )
            }
        }
    }

    fun clearFaceInfo() {
        drawInfoList.clear()
        postInvalidate()
    }

    fun addFaceInfo(faceInfo: DrawInfo) {
        drawInfoList.add(faceInfo)
        postInvalidate()
    }

    fun addFaceInfo(faceInfoList: List<DrawInfo>) {
        drawInfoList.addAll(faceInfoList)
        postInvalidate()
    }

    fun drawRealtimeFaceInfo(drawInfoList: List<DrawInfo>?) {
        clearFaceInfo()
        if (drawInfoList == null || drawInfoList.isEmpty()) {
            return
        }
        addFaceInfo(drawInfoList)
    }

    class DrawInfo {
        lateinit var rect: Rect
        var sex: Int = 0
        var age: Int = 0
        var liveness: Int = 0
        var color: Int = 0
        var name: String? = null

        constructor(rect: Rect, sex: Int, age: Int, liveness: Int, color: Int, name: String?) {
            this.rect = rect
            this.sex = sex
            this.age = age
            this.liveness = liveness
            this.color = color
            this.name = name
        }

        constructor(drawInfo: DrawInfo?) {
            if (drawInfo == null) {
                return
            }
            rect = drawInfo.rect
            sex = drawInfo.sex
            age = drawInfo.age
            liveness = drawInfo.liveness
            color = drawInfo.color
            name = drawInfo.name
        }
    }

    companion object {
        // 默认人脸框厚度
        private const val DEFAULT_FACE_RECT_THICKNESS = 6

        /**
         * 绘制数据信息到view上，若 [com.arcsoft.arcfacedemo.widget.FaceRectView.DrawInfo.getName] 不为null则绘制 [com.arcsoft.arcfacedemo.widget.FaceRectView.DrawInfo.getName]
         *
         * @param canvas            需要被绘制的view的canvas
         * @param drawInfo          绘制信息
         * @param faceRectThickness 人脸框厚度
         * @param paint             画笔
         */
        private fun drawFaceRect(
            canvas: Canvas?,
            drawInfo: DrawInfo?,
            faceRectThickness: Int,
            paint: Paint
        ) {
            if (canvas == null || drawInfo == null) {
                return
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = faceRectThickness.toFloat()
            paint.color = drawInfo.color
            paint.isAntiAlias = true
            val mPath = Path()
            // 左上
            val rect = drawInfo.rect
            mPath.moveTo(rect.left.toFloat(), (rect.top + rect.height() / 4).toFloat())
            mPath.lineTo(rect.left.toFloat(), rect.top.toFloat())
            mPath.lineTo((rect.left + rect.width() / 4).toFloat(), rect.top.toFloat())
            // 右上
            mPath.moveTo((rect.right - rect.width() / 4).toFloat(), rect.top.toFloat())
            mPath.lineTo(rect.right.toFloat(), rect.top.toFloat())
            mPath.lineTo(rect.right.toFloat(), (rect.top + rect.height() / 4).toFloat())
            // 右下
            mPath.moveTo(rect.right.toFloat(), (rect.bottom - rect.height() / 4).toFloat())
            mPath.lineTo(rect.right.toFloat(), rect.bottom.toFloat())
            mPath.lineTo((rect.right - rect.width() / 4).toFloat(), rect.bottom.toFloat())
            // 左下
            mPath.moveTo((rect.left + rect.width() / 4).toFloat(), rect.bottom.toFloat())
            mPath.lineTo(rect.left.toFloat(), rect.bottom.toFloat())
            mPath.lineTo(rect.left.toFloat(), (rect.bottom - rect.height() / 4).toFloat())
            canvas.drawPath(mPath, paint)

            // 绘制文字，用最细的即可，避免在某些低像素设备上文字模糊
            paint.strokeWidth = 1f
            if (drawInfo.name == null) {
                paint.style = Paint.Style.FILL_AND_STROKE
                paint.textSize = (rect.width() / 12).toFloat()
                val str =
                    ((if (drawInfo.sex == GenderInfo.MALE) "MALE" else if (drawInfo.sex == GenderInfo.FEMALE) "FEMALE" else "UNKNOWN")
                            + ","
                            + (if (drawInfo.age == AgeInfo.UNKNOWN_AGE) "UNKNOWN" else drawInfo.age)
                            + ","
                            + if (drawInfo.liveness == LivenessInfo.ALIVE) "ALIVE" else if (drawInfo.liveness == LivenessInfo.NOT_ALIVE) "NOT_ALIVE" else "UNKNOWN")
                canvas.drawText(str, rect.left.toFloat(), (rect.top - 10).toFloat(), paint)
            } else {
                paint.style = Paint.Style.FILL_AND_STROKE
                paint.textSize = (rect.width() / 12).toFloat()
                canvas.drawText(
                    drawInfo.name!!,
                    rect.left.toFloat(),
                    (rect.top - 10).toFloat(),
                    paint
                )
            }
        }
    }

}