package com.ilab.arcface.util.camera;

import android.hardware.Camera;


public interface CameraListener {
    /**
     * 当打开时执行
     *
     * @param camera             相机实例
     * @param cameraId           相机ID
     * @param displayOrientation 相机预览旋转角度
     * @param isMirror           是否镜像显示
     */
    void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror);

    /**
     * 预览数据回调
     *
     * @param data   预览数据
     * @param camera 相机实例
     */
    void onPreview(byte[] data, Camera camera);

    /**
     * 当相机关闭时执行
     */
    default void onCameraClosed() {
    }

    /**
     * 当出现异常时执行
     *
     * @param e 相机相关异常
     */
    default void onCameraError(Exception e) {
        e.printStackTrace();
    }

    /**
     * 属性变化时调用
     *
     * @param cameraID           相机ID
     * @param displayOrientation 相机旋转方向
     */
    default void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
    }
}
