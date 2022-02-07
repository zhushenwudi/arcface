package com.ilab.arcface.viewmodel

import android.graphics.Point
import android.graphics.Rect
import android.hardware.Camera
import android.view.TextureView
import androidx.lifecycle.MutableLiveData
import com.arcsoft.face.*
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.arcsoft.imageutil.ArcSoftImageFormat
import com.ilab.arcface.Const
import com.ilab.arcface.R
import com.ilab.arcface.util.DensityUtils
import com.ilab.arcface.util.FaceRectTransformer
import com.ilab.arcface.util.FaceUtil
import com.ilab.arcface.util.camera.CameraListener
import com.ilab.arcface.util.camera.DualCameraHelper
import com.ilab.arcface.util.face.FaceHelper
import com.ilab.arcface.util.face.constants.LivenessType
import com.ilab.arcface.util.face.constants.RecognizeColor
import com.ilab.arcface.util.face.model.FacePreviewInfo
import com.ilab.arcface.util.face.model.PreviewConfig
import com.ilab.arcface.util.face.model.RecognizeConfiguration
import com.ilab.arcface.widget.FaceRectView.DrawInfo
import com.zhushenwudi.base.app.appContext
import com.zhushenwudi.base.ext.util.windowManager
import com.zhushenwudi.base.mvvm.vm.BaseAppViewModel
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock

class FaceAppViewModel : BaseAppViewModel() {
    // App版本号
    val mutableVersion = MutableLiveData(appContext.resources.getString(R.string.version, BuildConfig.VERSION_NAME))

    // 激活 sdk 状态
    val activeFaceSDKState = MutableLiveData(-1)

    // 引擎 和 帮助类
    private var flEngine: FaceEngine? = null
    private var ftEngine: FaceEngine? = null
    private var faceHelper: FaceHelper? = null
    private var rgbCameraHelper: DualCameraHelper? = null
    private var irCameraHelper: DualCameraHelper? = null

    // 引擎可执行功能
    private var livenessMask = 0

    // Ir 相机数据
    private var irNv21: ByteArray? = null

    // 显示画布大小
    private var previewSize: Camera.Size? = null

    // 识别框左边距
    private var leftMargin: Int = 0

    // 人脸检测框区域
    lateinit var faceDetectRect: Rect

    // 当人脸框需要偏移时，需要更新位置后显示
    private var needUpdateFaceData = false

    // 活体检测线程池
    private var livenessExecutor: ExecutorService? = null

    // Rgb 活体数据集合
    private var rgbLivenessMap: ConcurrentHashMap<Int, Int> = ConcurrentHashMap()

    // Ir 活体数据集合
    private var irLivenessMap: ConcurrentHashMap<Int, Int> = ConcurrentHashMap()

    // 活体检测时的锁
    private val livenessDetectLock = ReentrantLock()

    // 人脸识别配置
    private val config = RecognizeConfiguration()

    // 人脸识别状态
    val faceStatus = MutableLiveData<Pair<Int, String?>>()

    // 流程处理状态
    val processStatus = MutableLiveData(Status.READY)

    // 引擎异常
    val engineErrorCode = MutableLiveData<Pair<String, Int>>()

    // 其他异常
    val othersErrorCode = MutableLiveData<String>()

    // 流程处理状态 枚举类
    enum class Status {
        READY,          // 整装待发
        REQUEST,        // 联网检测阶段
    }

    /**
     * 初始化
     *
     * @param canOpenDualCamera 是否存在相机
     */
    fun init(canOpenDualCamera: Boolean) {
        livenessMask = if (canOpenDualCamera) {
            FaceEngine.ASF_LIVENESS or FaceEngine.ASF_IR_LIVENESS or FaceEngine.ASF_FACE_DETECT
        } else {
            FaceEngine.ASF_LIVENESS
        }
        if (config.dualCameraHorizontalOffset != 0 || config.dualCameraVerticalOffset != 0) {
            needUpdateFaceData = true
            livenessMask = livenessMask or FaceEngine.ASF_UPDATE_FACEDATA
        }

        ftEngine = FaceEngine()
        ftEngine?.apply {
            engineErrorCode.postValue(
                Pair(
                    "ftEngineError", init(
                        appContext,
                        DetectMode.ASF_DETECT_MODE_VIDEO,
                        DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                        1,
                        FaceEngine.ASF_FACE_DETECT
                    )
                )
            )
        }

        flEngine = FaceEngine()
        flEngine?.apply {
            engineErrorCode.postValue(
                Pair(
                    "flEngineError", init(
                        appContext,
                        DetectMode.ASF_DETECT_MODE_IMAGE,
                        DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                        1,
                        livenessMask
                    )
                )
            )
            setLivenessParam(config.livenessParam)
        }

        livenessExecutor = ThreadPoolExecutor(
            1, 1,
            0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue()
        ) { r: Runnable? ->
            val t = Thread(r)
            t.name = "flThread-" + t.id
            t
        }
    }

    /**
     * 当 Rgb 相机打开时
     */
    fun onRgbCameraOpened(camera: Camera, rgbFaceRectTransformer: FaceRectTransformer?) {
        val lastPreviewSize = previewSize
        previewSize = camera.parameters.previewSize
        val screen = appContext.windowManager?.defaultDisplay
        screen?.let {
            leftMargin =
                (screen.width - DensityUtils.dip2px(appContext, previewSize!!.width.toFloat())) / 2
        }
        // 切换相机的时候可能会导致预览尺寸发生变化
        initFaceHelper(lastPreviewSize)
        faceHelper?.setRgbFaceRectTransformer(rgbFaceRectTransformer)
    }

    /**
     * 当 Ir 相机打开时
     */
    fun onIrCameraOpened(camera: Camera, irFaceRectTransformer: FaceRectTransformer?) {
        val lastPreviewSize = previewSize
        previewSize = camera.parameters.previewSize
        val screen = appContext.windowManager?.defaultDisplay
        screen?.let {
            leftMargin =
                (screen.width - DensityUtils.dip2px(appContext, previewSize!!.width.toFloat())) / 2
        }
        // 切换相机的时候可能会导致预览尺寸发生变化
        initFaceHelper(lastPreviewSize)
        faceHelper?.setDualCameraFaceInfoTransformer { faceInfo ->
            faceInfo.rect.offset(
                config.dualCameraHorizontalOffset,
                config.dualCameraVerticalOffset
            )
            faceInfo
        }
        faceHelper?.setIrFaceRectTransformer(irFaceRectTransformer)
    }

    /**
     * 初始化相机帮助类
     *
     * @param view 相机数据要显示在的控件
     * @param cameraListener 回调监听器
     * @param type 要绘制的相机类型
     */
    fun initCameraHelper(view: TextureView, cameraListener: CameraListener, type: LivenessType) {
        val helper = DualCameraHelper.Builder()
            .previewViewSize(Point(view.measuredWidth, view.measuredHeight))
            .specificCameraId(if (type == LivenessType.RGB) PreviewConfig.rgbCameraId else PreviewConfig.irCameraId)
            .additionalRotation(if (type == LivenessType.RGB) PreviewConfig.rgbAdditionalDisplayOrientation else PreviewConfig.irAdditionalDisplayOrientation)
            .previewSize(Point(view.measuredWidth, view.measuredHeight))
            .previewOn(view)
            .cameraListener(cameraListener)
            .build()
        try {
            helper?.init()
            helper?.start()
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
        if (type == LivenessType.RGB) {
            rgbCameraHelper = helper
        } else {
            irCameraHelper = helper
        }
    }

    /**
     * 初始化人脸帮助类
     */
    private fun initFaceHelper(lastPreviewSize: Camera.Size?) {
        if (faceHelper == null || lastPreviewSize == null || lastPreviewSize.width != previewSize?.width || lastPreviewSize.height != previewSize?.height) {
            faceHelper = FaceHelper.Builder()
                .ftEngine(ftEngine)
                .previewSize(previewSize)
                .onlyDetectLiveness(true)
                .recognizeConfiguration(config)
                .trackedFaceCount(0)
                .build()
        }
    }

    /**
     * 获取到相机数据
     *
     * @param nv21 准备转换成图像的 Rgb 数据
     */
    fun onPreviewFrame(nv21: ByteArray): List<FacePreviewInfo>? {
        val facePreviewInfoList = faceHelper?.onPreviewFrame(nv21, irNv21, false)
        if (!facePreviewInfoList.isNullOrEmpty()) {
            clearLeftFace(facePreviewInfoList)
            return processLiveness(nv21, irNv21, facePreviewInfoList)
        }
        faceStatus.postValue(Pair(LivenessInfo.UNKNOWN, null))
        return null
    }

    /**
     * 处理 Rgb 数据是否是活体
     *
     * @param nv21 准备转换成图像的 Rgb 数据
     * @param irNv21 准备转换成图像的 Ir 数据
     * @param previewInfoList 人脸信息列表
     */
    private fun processLiveness(
        nv21: ByteArray,
        irNv21: ByteArray?,
        previewInfoList: List<FacePreviewInfo>
    ): List<FacePreviewInfo> {
        if (!livenessDetectLock.isLocked) {
            livenessExecutor?.execute {
                val facePreviewInfoList = LinkedList(previewInfoList)
                if (facePreviewInfoList.isEmpty()) {
                    return@execute
                }
                livenessDetectLock.lock()
                try {
                    var processRgbLivenessCode = -1
                    flEngine?.let {
                        synchronized(it) {
                            if (facePreviewInfoList[0].faceInfoRgb != null) {
                                processRgbLivenessCode = it.process(
                                    nv21,
                                    previewSize!!.width,
                                    previewSize!!.height,
                                    FaceEngine.CP_PAF_NV21,
                                    listOf(facePreviewInfoList[0].faceInfoRgb),
                                    FaceEngine.ASF_LIVENESS
                                )
                            }
                        }
                    }
                    if (processRgbLivenessCode != ErrorInfo.MOK) {
                        othersErrorCode.postValue("processLiveness processRgbLivenessCode error: $processRgbLivenessCode")
                        return@execute
                    }
                    val rgbLivenessInfoList: List<LivenessInfo> = ArrayList()
                    val getRgbLivenessCode = flEngine?.getLiveness(rgbLivenessInfoList)
                    if (getRgbLivenessCode != ErrorInfo.MOK) {
                        othersErrorCode.postValue("processLiveness getRgbLivenessCode error: $getRgbLivenessCode")
                        return@execute
                    }
                    rgbLivenessMap[facePreviewInfoList[0].trackId] = rgbLivenessInfoList[0].liveness
                    if (livenessMask and FaceEngine.ASF_IR_LIVENESS != 0) {
                        val faceInfoList = ArrayList<FaceInfo>()
                        val irFaceInfo = facePreviewInfoList[0].faceInfoIr
                        val fdCode = flEngine?.detectFaces(
                            irNv21,
                            previewSize!!.width,
                            previewSize!!.height,
                            FaceEngine.CP_PAF_NV21,
                            faceInfoList
                        )
                        if (fdCode == ErrorInfo.MOK && FaceHelper.isFaceExists(
                                faceInfoList,
                                irFaceInfo
                            )
                        ) {
                            if (needUpdateFaceData) {
                                // 若IR人脸框有偏移，则需要对IR的人脸数据进行updateFaceData处理，再将处理后的FaceInfo信息传输给活体检测接口
                                val faceDataCode = flEngine?.updateFaceData(
                                    irNv21,
                                    previewSize!!.width,
                                    previewSize!!.height,
                                    FaceEngine.CP_PAF_NV21,
                                    ArrayList(listOf(irFaceInfo))
                                )
                                if (faceDataCode != ErrorInfo.MOK) {
                                    othersErrorCode.postValue("processLiveness faceDataCode error: $faceDataCode")
                                    return@execute
                                }
                            }
                            processIrLive(irFaceInfo, facePreviewInfoList[0].trackId)
                        } else if (fdCode != ErrorInfo.MOK && fdCode != ErrorInfo.MERR_FSDK_FACEFEATURE_MISSFACE) {
                            othersErrorCode.postValue("processLiveness fdCode error: $fdCode")
                        }
                    }
                } finally {
                    livenessDetectLock.unlock()
                }
            }
        }
        for (facePreviewInfo in previewInfoList) {
            val rgbLiveness = rgbLivenessMap[facePreviewInfo.trackId]
            rgbLiveness?.let { rgb ->
                facePreviewInfo.rgbLiveness = rgb
                val irLiveness = irLivenessMap[facePreviewInfo.trackId]
                irLiveness?.let { ir -> facePreviewInfo.irLiveness = ir }
            }

        }
        return previewInfoList
    }

    /**
     * 处理 Ir 数据是否是活体
     *
     * @param irFaceInfo Ir 相机人脸数据
     * @param trackId 人脸序号
     */
    private fun processIrLive(irFaceInfo: FaceInfo, trackId: Int) {
        var processIrLivenessCode: Int = -1
        flEngine?.let {
            synchronized(it) {
                processIrLivenessCode = it.processIr(
                    irNv21, previewSize!!.width, previewSize!!.height, FaceEngine.CP_PAF_NV21,
                    listOf(irFaceInfo), FaceEngine.ASF_IR_LIVENESS
                )
            }
        }
        if (processIrLivenessCode != ErrorInfo.MOK) {
            if (processIrLivenessCode != ErrorInfo.MERR_FSDK_FACEFEATURE_MISSFACE) {
                othersErrorCode.postValue("processIrLive processIrLivenessCode error: $processIrLivenessCode")
            }
            return
        }
        val irLivenessInfoList = ArrayList<LivenessInfo>()
        val getIrLivenessCode = flEngine?.getIrLiveness(irLivenessInfoList)
        if (getIrLivenessCode != ErrorInfo.MOK) {
            othersErrorCode.postValue("processIrLive getIrLivenessCode error: $getIrLivenessCode")
            return
        }
        irLivenessMap[trackId] = irLivenessInfoList[0].liveness
    }

    /**
     * 刷新 Ir 相机图像
     *
     * @param irNv21 准备转换成图像的 Ir 数据
     */
    fun refreshIrPreviewData(irNv21: ByteArray) {
        this.irNv21 = irNv21
    }

    /**
     * 人脸信息 -> Base64字符串
     *
     * @param facePreviewInfoList 人脸信息列表
     * @param nv21 准备转换成图像的数据
     */
    fun parseFaceInfo(
        facePreviewInfoList: List<FacePreviewInfo>,
        nv21: ByteArray
    ) {
        facePreviewInfoList.forEach {
            val isInDetectArea = checkFaceInDetectArea(it)
            if (isInDetectArea) {
                val rgb = it.rgbLiveness
                val ir = it.irLiveness

                if (rgb == LivenessInfo.ALIVE) {
                    val faceRect = FaceUtil.getBestRect(
                        previewSize!!.width,
                        previewSize!!.height,
                        it.faceInfoRgb.rect
                    )
                    faceRect?.apply {
                        left = left and 3.inv()
                        top = top and 3.inv()
                        right = right and 3.inv()
                        bottom = bottom and 3.inv()
                        val headBmp = FaceUtil.getHeadImage(
                            nv21,
                            previewSize!!.width,
                            previewSize!!.height,
                            0,
                            this,
                            ArcSoftImageFormat.NV21
                        )
                        faceStatus.postValue(
                            Pair(
                                LivenessInfo.ALIVE,
                                FaceUtil.bitmapToBase64(headBmp)
                            )
                        )
                    }
                }
//                else if (rgb == LivenessInfo.ALIVE && ir != LivenessInfo.ALIVE) {
//                    faceStatus.postValue(Pair(LivenessInfo.NOT_ALIVE, null))
//                }
                else {
                    faceStatus.postValue(Pair(rgb, null))
                }
            }
        }
    }

    /**
     * 检测人脸是否在识别区域内
     *
     * @param facePreviewInfo 人脸信息
     */
    private fun checkFaceInDetectArea(
        facePreviewInfo: FacePreviewInfo
    ): Boolean {
        val facePreviewRect = facePreviewInfo.faceInfoRgb.rect
        if (leftMargin <= 0) {
            return false
        }
        // 因为镜像翻转了，所以将facePreviewRect的坐标切换为正常坐标
        val faceRect = Rect(
            previewSize!!.width - facePreviewRect.right + leftMargin,
            facePreviewRect.top,
            previewSize!!.width - facePreviewRect.left + leftMargin,
            facePreviewRect.bottom
        )

        if ((faceRect.left < faceDetectRect.left && faceRect.right > faceDetectRect.right) ||
            (faceRect.top < faceDetectRect.top && faceRect.bottom > faceDetectRect.bottom)
        ) {
            // 人脸过大，需要向后
            faceStatus.postValue(Pair(Const.CODE_BACKWARD, null))
        } else if (faceRect.top < faceDetectRect.top) {
            // 人脸上方不全，需要向下
            faceStatus.postValue(Pair(Const.CODE_DOWN, null))
        } else if (faceRect.bottom > faceDetectRect.bottom) {
            // 人脸下方不全，需要向上
            faceStatus.postValue(Pair(Const.CODE_UP, null))
        } else if (faceRect.left < faceDetectRect.left) {
            // 人脸左侧不全，需要向右
            faceStatus.postValue(Pair(Const.CODE_RIGHT, null))
        } else if (faceRect.right > faceDetectRect.right) {
            // 人脸右侧不全，需要向左
            faceStatus.postValue(Pair(Const.CODE_LEFT, null))
        } else return true
        return false
    }

    /**
     * 移除离开显示区域的人脸
     *
     * @param facePreviewInfoList 人脸信息列表
     */
    private fun clearLeftFace(facePreviewInfoList: List<FacePreviewInfo>) {
        rgbLivenessMap.forEach { (key, _) ->
            if (!facePreviewInfoList.any { it.trackId == key }) {
                rgbLivenessMap.remove(key)
                irLivenessMap.remove(key)
            }
        }
    }

    /**
     * 根据预览信息生成绘制信息
     *
     * @param facePreviewInfoList 人脸信息列表
     * @param type 要绘制的相机类型
     */
    fun getDrawInfo(
        facePreviewInfoList: List<FacePreviewInfo>,
        type: LivenessType
    ): List<DrawInfo> {
        val drawInfoList: MutableList<DrawInfo> = ArrayList()
        for (i in facePreviewInfoList.indices) {
            val isAlive =
                if (type == LivenessType.RGB) facePreviewInfoList[i].rgbLiveness else facePreviewInfoList[i].irLiveness
            val rect =
                if (type == LivenessType.RGB) facePreviewInfoList[i].rgbTransformedRect else facePreviewInfoList[i].irTransformedRect
            // 根据识别结果和活体结果设置颜色
            var color: Int
            var name: String
            when (isAlive) {
                LivenessInfo.ALIVE -> {
                    color = RecognizeColor.COLOR_SUCCESS
                    name = "ALIVE"
                }
                LivenessInfo.NOT_ALIVE -> {
                    color = RecognizeColor.COLOR_FAILED
                    name = "NOT_ALIVE"
                }
                else -> {
                    color = RecognizeColor.COLOR_UNKNOWN
                    name = "UNKNOWN"
                }
            }
            drawInfoList.add(
                DrawInfo(
                    rect, GenderInfo.UNKNOWN,
                    AgeInfo.UNKNOWN_AGE, isAlive, color, name
                )
            )
        }
        return drawInfoList
    }

    /**
     * 离线激活虹软 SDK
     */
    fun activeOffline(filePath: String) {
        val activeResult = FaceEngine.activeOffline(appContext, filePath)
        activeFaceSDKState.postValue(activeResult)
    }

    /**
     * 在线激活虹软 SDK
     */
    fun activeOnline(activeKey: String) {
        val activeResult = FaceEngine.activeOnline(
            appContext, activeKey, Const.FACE_APP_ID, Const.FACE_SDK_KEY
        )
        activeFaceSDKState.postValue(activeResult)
    }

    /**
     * 销毁引擎
     */
    fun destroy() {
        livenessExecutor?.shutdown()
        livenessExecutor = null
        faceHelper?.release()
        faceHelper = null
        irCameraHelper?.let {
            it.release()
            null
        }
        rgbCameraHelper?.let {
            it.release()
            null
        }
        ftEngine?.let {
            synchronized(it) {
                it.unInit()
            }
        }
        flEngine?.let {
            synchronized(it) {
                it.unInit()
            }
        }
    }

    companion object {
        const val GET_LICENCE_FAIL = "获取授权license失败"
    }
}