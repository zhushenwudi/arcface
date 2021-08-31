package com.ilab.arcface.util

import android.os.Environment
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.model.ActiveDeviceInfo
import com.zhushenwudi.base.app.appContext
import dev.utils.common.FileIOUtils
import dev.utils.common.FileUtils

object ActiveUtil {
    private val FILE_DIR = Environment.getExternalStorageDirectory().absolutePath + "/smartlab/"
    private val DEVICE_INFO_FILENAME = FILE_DIR + "device.txt"
    private const val filterName = ".dat"

    /**
     * 生成离线激活所需的设备信息
     */
    fun generateDeviceInfo() {
        val activeDeviceInfo = ActiveDeviceInfo()
        FaceEngine.getActiveDeviceInfo(appContext, activeDeviceInfo)
        FileIOUtils.writeFileFromString(DEVICE_INFO_FILENAME, activeDeviceInfo.deviceInfo)
    }

    /**
     * 读取本地激活码
     */
    fun getActiveFilePath(): String? {
        val files = FileUtils.listFilesInDirWithFilter(FILE_DIR) { it.name.contains(filterName) }
        if (files.size == 0) {
            return null
        }
        return files[0].absolutePath
    }
}