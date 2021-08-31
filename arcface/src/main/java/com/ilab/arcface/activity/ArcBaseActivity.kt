package com.ilab.arcface.activity

import android.graphics.LinearGradient
import android.graphics.Shader
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.arcsoft.face.ErrorInfo
import com.arcsoft.face.LivenessInfo
import com.ftd.livepermissions.LivePermissions
import com.ftd.livepermissions.PermissionResult
import com.ilab.arcface.BuildConfig
import com.ilab.arcface.Const
import com.ilab.arcface.Const.COMMON_PERMISSION
import com.ilab.arcface.R
import com.ilab.arcface.databinding.ActivityFaceBinding
import com.ilab.arcface.util.FaceRectTransformer
import com.ilab.arcface.util.FaceUtil
import com.ilab.arcface.util.camera.CameraListener
import com.ilab.arcface.util.camera.CameraUtil
import com.ilab.arcface.util.camera.DualCameraHelper
import com.ilab.arcface.util.face.constants.LivenessType
import com.ilab.arcface.util.face.model.FacePreviewInfo
import com.ilab.arcface.viewmodel.FaceAppViewModel
import com.zhushenwudi.base.ext.view.clickNoRepeat
import com.zhushenwudi.base.mvvm.v.BaseVmDbActivity
import com.zhushenwudi.base.mvvm.vm.BaseAppViewModel
import com.zhushenwudi.base.network.manager.NetState

abstract class ArcBaseActivity<VM : BaseAppViewModel> :
    BaseVmDbActivity<FaceAppViewModel, ActivityFaceBinding>(),
    OnGlobalLayoutListener {
    protected var allowedHandle: Boolean = true//是否允许处理识别
    private var rgbFaceRectTransformer: FaceRectTransformer? = null
    private var irFaceRectTransformer: FaceRectTransformer? = null

    // 记录前一次的人脸识别状态，防止反复刷新界面
    private var preFaceStatus = 2

    override fun layoutId() = R.layout.activity_face

    override fun initView(savedInstanceState: Bundle?) {
        mDatabind.data = mViewModel
        mDatabind.tvTips.visibility = View.VISIBLE
        mDatabind.textureRgb.viewTreeObserver.addOnGlobalLayoutListener(this)

        initListener()
    }

    private fun initListener() {
        mDatabind.btnClose.clickNoRepeat {
            setResult(RESULT_CANCELED)
            finish()
        }

        mDatabind.modal.clickNoRepeat {
            mDatabind.flCamera.visibility = View.VISIBLE
            mDatabind.tvTips.visibility = View.VISIBLE
            mDatabind.bgFace.visibility = View.VISIBLE
            mDatabind.modal.visibility = View.INVISIBLE
            allowedHandle = true
            FaceAppViewModel.Status.REQUEST
        }
    }

    open fun setTipsText(content: String) {
        mDatabind.tvTips.text = content
    }

    open fun changeModal(drawable: Int, tip: String) {
        mDatabind.ivScanFace.setBackgroundResource(drawable)
        mDatabind.tvTips.visibility = View.GONE
        mDatabind.bgFace.visibility = View.GONE
        mDatabind.modal.visibility = View.VISIBLE
        mDatabind.flCamera.visibility = View.INVISIBLE
        mDatabind.tvHint.text = tip
        //请求结束 下一轮开始(一次请求失败/多人认证)
        changeToReady()
    }

    open fun setSecond(second: Int) {
        mDatabind.tvSecond.text = "${second}s"
    }

    override fun createObserver() {
        mViewModel.activeFaceSDKState.observe(this) {
            if (it == ErrorInfo.MOK) {
                initEngine()
            } else if (it != -1) {
                Log.e(TAG, "激活失败: state == $it")
                getActiveKey(true)
            }
        }

        mViewModel.engineErrorCode.observe(this) {
            when (it.first) {
                "ftEngineError" -> {
                    val error = getString(
                        R.string.specific_engine_failed, "ftEngine",
                        it.second, FaceUtil.arcFaceErrorCodeToFieldName(it.second)
                    )
                    showError("faceEngine: $error")
                }
            }
        }

        mViewModel.othersErrorCode.observe(this) {
            Log.e(TAG, it)
        }

        mViewModel.faceStatus.observe(this) {
            if (mViewModel.processStatus.value != FaceAppViewModel.Status.REQUEST && preFaceStatus != it.first) {
                preFaceStatus = it.first
                when (it.first) {
                    LivenessInfo.ALIVE -> {
                        it.second?.run {
                            mViewModel.processStatus.value = FaceAppViewModel.Status.REQUEST
                            mDatabind.tvTips.text = Const.SINGLE_CHECK_SUCCESS
                            onDetectSuccess(this)
                        }
                    }
                    LivenessInfo.NOT_ALIVE -> mDatabind.tvTips.text = Const.SINGLE_CHECK_FAILED
                    LivenessInfo.UNKNOWN -> mDatabind.tvTips.text = Const.SINGLE_NOT_FOUND_FACE
                    LivenessInfo.FACE_NUM_MORE_THAN_ONE -> mDatabind.tvTips.text = Const.MULTI_PEOPLE
                    LivenessInfo.FACE_TOO_SMALL -> mDatabind.tvTips.text = Const.FORWARD_FACE
                    LivenessInfo.FACE_ANGLE_TOO_LARGE -> mDatabind.tvTips.text = Const.POSITIVE_SCREEN
                    LivenessInfo.TOO_BRIGHT_IR_IMAGE -> mDatabind.tvTips.text = Const.FULL_LIGHT
                    Const.CODE_BACKWARD -> mDatabind.tvTips.text = Const.BACKWARD_FACE
                    Const.CODE_LEFT -> mDatabind.tvTips.text = Const.LEFT_FACE
                    Const.CODE_UP -> mDatabind.tvTips.text = Const.UP_FACE
                    Const.CODE_RIGHT -> mDatabind.tvTips.text = Const.RIGHT_FACE
                    Const.CODE_DOWN -> mDatabind.tvTips.text = Const.DOWN_FACE
                }
            }
        }
    }

    /**
     * 初始化 Rgb 相机
     */
    private fun initRgbCamera() {
        val rgbCameraListener = object : CameraListener {
            override fun onCameraOpened(
                camera: Camera,
                cameraId: Int,
                displayOrientation: Int,
                isMirror: Boolean
            ) {
                rgbFaceRectTransformer =
                    createTransformer(
                        camera,
                        cameraId,
                        displayOrientation,
                        isMirror,
                        LivenessType.RGB
                    )
                mViewModel.onRgbCameraOpened(camera, rgbFaceRectTransformer)
            }

            override fun onPreview(nv21: ByteArray, camera: Camera?) {
                if (!allowedHandle) return
                val facePreviewInfoList = mViewModel.onPreviewFrame(nv21)
                facePreviewInfoList?.let {
                    rgbFaceRectTransformer?.apply {
                        mViewModel.parseFaceInfo(it, nv21)
                        if (!BuildConfig.DEBUG) {
                            mDatabind.dualCameraFaceRectView.clearFaceInfo()
                            mDatabind.dualCameraFaceRectViewIr.clearFaceInfo()
//                            drawPreviewInfo(it)
                        }
                    }
                }
            }
        }
        mViewModel.initCameraHelper(mDatabind.textureRgb, rgbCameraListener, LivenessType.RGB)
    }

    /**
     * 初始化 Ir 相机
     */
    private fun initIrCamera() {
        val irCameraListener = object : CameraListener {
            override fun onCameraOpened(
                camera: Camera,
                cameraId: Int,
                displayOrientation: Int,
                isMirror: Boolean
            ) {
                irFaceRectTransformer =
                    createTransformer(
                        camera,
                        cameraId,
                        displayOrientation,
                        isMirror,
                        LivenessType.IR
                    )
                mViewModel.onIrCameraOpened(camera, irFaceRectTransformer)
            }

            override fun onPreview(nv21: ByteArray, camera: Camera?) {
                mViewModel.refreshIrPreviewData(nv21)
            }
        }
        mViewModel.initCameraHelper(mDatabind.textureIr, irCameraListener, LivenessType.IR)
    }

    /**
     * 创建人脸矩形的变压器
     */
    private fun createTransformer(
        camera: Camera,
        cameraId: Int,
        displayOrientation: Int,
        isMirror: Boolean,
        type: LivenessType
    ): FaceRectTransformer {
        val previewSize = camera.parameters.previewSize
        val layoutParams = CameraUtil.adjustPreviewViewSize(
            mDatabind.textureRgb,
            if (type == LivenessType.RGB) mDatabind.textureRgb else mDatabind.textureIr,
            if (type == LivenessType.RGB) mDatabind.dualCameraFaceRectView else mDatabind.dualCameraFaceRectViewIr,
            previewSize,
            displayOrientation,
            1f
        )
        return FaceRectTransformer(
            previewSize.width, previewSize.height,
            layoutParams.width, layoutParams.height,
            displayOrientation, cameraId, isMirror,
            type == LivenessType.RGB, false
        )
    }

    /**
     * 绘制RGB、IR画面的实时人脸信息
     *
     * @param facePreviewInfoList RGB画面的实时人脸信息
     */
    private fun drawPreviewInfo(facePreviewInfoList: List<FacePreviewInfo>) {
        rgbFaceRectTransformer?.let {
            val rgbDrawInfoList = mViewModel.getDrawInfo(facePreviewInfoList, LivenessType.RGB)
            mDatabind.dualCameraFaceRectView.drawRealtimeFaceInfo(rgbDrawInfoList)
        }
        irFaceRectTransformer?.let {
            val irDrawInfoList = mViewModel.getDrawInfo(facePreviewInfoList, LivenessType.IR)
            mDatabind.dualCameraFaceRectViewIr.drawRealtimeFaceInfo(irDrawInfoList)
        }
    }

    /**
     * 布局加载完成
     */
    override fun onGlobalLayout() {
        mDatabind.textureRgb.viewTreeObserver.removeOnGlobalLayoutListener(this)

        LivePermissions(this)
            .requestArray(COMMON_PERMISSION)
            .observe(this) {
                when (it) {
                    is PermissionResult.Grant -> {
                        //权限允许
                        val isActive = FaceUtil.isActivated(this)
                        if (!isActive) {
                            getActiveKey()
                        } else {
                            initEngine()
                        }
                    }
                    is PermissionResult.Rationale -> {
                        //权限拒绝
                        Log.e(TAG, "您拒绝了权限")
                    }
                    is PermissionResult.Deny -> {
                        //权限拒绝，且勾选了不再询问
                        Log.e(TAG, "您拒绝了权限")
                    }
                }
            }
    }

    // 联网激活key
    abstract fun getActiveKey(needReGet: Boolean = false)

    // 更新证书
//    abstract fun updateLicence(licence: String)

    // 人脸图片 base 字符串
    abstract fun onDetectSuccess(imgBase64: String)

    protected fun activeOffline(filePath: String) {
        mViewModel.activeOffline(filePath)
    }

    protected fun changeToReady() {
        mViewModel.processStatus.value = FaceAppViewModel.Status.READY
    }

    private fun initEngine() {
        mViewModel.init(DualCameraHelper.canOpenDualCamera())
        mViewModel.faceDetectRect = FaceUtil.getRectInScreen(mDatabind.faceRectView)
        // 获取识别框检测区域在屏幕的绝对位置
        mDatabind.faceRectView.getGlobalVisibleRect(mViewModel.faceDetectRect)
        initRgbCamera()
//        if (DualCameraHelper.hasDualCamera()) {
//            initIrCamera()
//        }
    }

    /**
     * 设置标题
     */
    open fun setLeftTitle(title: String) {
        val start = resources.getColor(R.color.login_top, null)
        val end = resources.getColor(R.color.login_bottom, null)
        val shader: Shader = LinearGradient(
            0f,
            0f,
            0f,
            mDatabind.tvTitle.lineHeight.toFloat(),
            start,
            end,
            Shader.TileMode.CLAMP
        )
        mDatabind.tvTitle.paint.shader = shader
        mDatabind.tvTitle.text = title
    }

    private fun showError(msg: String) {
        Log.e(TAG, msg)
    }

    override fun onNetworkStateChanged(netState: NetState) {
        super.onNetworkStateChanged(netState)
        Log.e(TAG, "${netState.isSuccess}")
    }

    protected fun getVModel(): FaceAppViewModel {
        return mViewModel
    }

    override fun showLoading(message: String) {}

    override fun dismissLoading() {}

    override fun onDestroy() {
        mViewModel.destroy()
        super.onDestroy()
    }

    companion object {
        private val TAG = ArcBaseActivity::class.java.simpleName
    }
}