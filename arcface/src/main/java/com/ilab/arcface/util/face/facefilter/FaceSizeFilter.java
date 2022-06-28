package com.ilab.arcface.util.face.facefilter;

import android.graphics.Rect;

import com.arcsoft.face.FaceInfo;
import com.ilab.arcface.util.face.model.FacePreviewInfo;

import java.util.List;

/**
 * 人脸尺寸过滤器：
 * 仅保留人脸宽度大于{@link FaceSizeFilter#horizontalSize}，且人脸高度大于{@link FaceSizeFilter#verticalSize}的人脸。
 */
public class FaceSizeFilter implements FaceRecognizeFilter {
    private final int horizontalSize;
    private final int verticalSize;

    private static final String TAG = "FaceSizeFilter";

    public FaceSizeFilter(int horizontalSize, int verticalSize) {
        this.horizontalSize = horizontalSize;
        this.verticalSize = verticalSize;
    }

    @Override
    public void filter(List<FacePreviewInfo> facePreviewInfoList) {
        for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
            if (!facePreviewInfo.isQualityPass()) {
                continue;
            }
            final FaceInfo faceInfo = facePreviewInfo.getFaceInfoRgb();
            if (faceInfo == null) {
                continue;
            }
            Rect rgbRect = faceInfo.getRect();
            Rect irRect = facePreviewInfo.getFaceInfoIr() == null ? null : facePreviewInfo.getFaceInfoIr().getRect();
            boolean rgbRectValid = rgbRect == null || (rgbRect.width() > horizontalSize && rgbRect.height() > verticalSize);
            boolean irRectValid = irRect == null || (irRect.width() > horizontalSize && irRect.height() > verticalSize);
            facePreviewInfo.setQualityPass(rgbRectValid && irRectValid);
        }
    }
}
