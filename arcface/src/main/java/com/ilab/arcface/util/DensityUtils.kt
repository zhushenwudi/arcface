/**
 * Copyright (C) 2017 Baidu Inc. All rights reserved.
 */
package com.ilab.arcface.util

import android.content.Context
import android.os.Build
import android.text.TextUtils

/**
 * 显示设备信息工具类
 */
object DensityUtils {
    /**
     * 四舍五入
     */
    private const val DOT_FIVE = 0.5f

    /**
     * portrait degree:90
     */
    private const val PORTRAIT_DEGREE_90 = 90

    /**
     * portrait degree:270
     */
    private const val PORTRAIT_DEGREE_270 = 270

    /**
     * sp转px.
     *
     * @param context context
     * @param spValue spValue
     * @return 换算后的px值
     */
    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    /**
     * dip转换成px
     *
     * @param context Context
     * @param dip     dip Value
     * @return 换算后的px值
     */
    fun dip2px(context: Context, dip: Float): Int {
        val density = getDensity(context)
        return (dip * density + DOT_FIVE).toInt()
    }

    /**
     * px转换成dip
     *
     * @param context Context
     * @param px      px Value
     * @return 换算后的dip值
     */
    fun px2dip(context: Context, px: Float): Int {
        val density = getDensity(context)
        return (px / density + DOT_FIVE).toInt()
    }

    /**
     * 得到显示宽度
     *
     * @param context Context
     * @return 宽度
     */
    fun getDisplayWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    /**
     * 得到显示高度
     *
     * @param context Context
     * @return 高度
     */
    fun getDisplayHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    /**
     * 得到显示密度
     *
     * @param context Context
     * @return 密度
     */
    fun getDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    /**
     * 得到DPI
     *
     * @param context Context
     * @return DPI
     */
    fun getDensityDpi(context: Context): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * 判断当前Android系统能否竖着屏幕取扫描二维码 2.1版本的ROM是不支持的竖屏扫描的，而且发现过一台三星-GT-S5830i也不支持竖屏扫描
     *
     * @return 当前Android系统能竖着屏幕取扫描二维码：true, 不能：false
     */
    fun supportCameraPortrait(): Boolean {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO
                && !TextUtils.equals("GT-S5830i", Build.PRODUCT))
    }// 为了更好的扩展更多的特殊设置型号，将要比较的设备型号提成一个数组，遍历这个数据。

    /**
     * 判断当前Android系统摄像头旋转多少度
     *
     * @return 当前Android系统能竖着屏幕，
     * 正常应该旋转90度，
     * 但是斐讯i700v、夏新A862W、桑菲V8526需要旋转270度
     */
    val portraitDegree: Int
        get() {
            var degree = PORTRAIT_DEGREE_90
            // 为了更好的扩展更多的特殊设置型号，将要比较的设备型号提成一个数组，遍历这个数据。
            for (model in BUILD_MODELS) {
                if (TextUtils.equals(model, Build.MODEL)) {
                    degree = PORTRAIT_DEGREE_270
                    break
                }
            }
            return degree
        }

    /**
     * 需要比较的设置型号
     */
    private val BUILD_MODELS = arrayOf(
        "i700v",  //斐讯i700v
        "A862W",  //夏新A862W
        "V8526" //桑菲V8526
    )
}