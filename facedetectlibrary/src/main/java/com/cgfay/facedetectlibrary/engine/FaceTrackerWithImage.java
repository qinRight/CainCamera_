package com.cgfay.facedetectlibrary.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.cgfay.facedetectlibrary.R;
import com.cgfay.facedetectlibrary.listener.FaceTrackerCallback;
import com.cgfay.facedetectlibrary.utils.ConUtil;
import com.cgfay.facedetectlibrary.utils.SensorEventUtil;
import com.cgfay.landmarklibrary.LandmarkEngine;
import com.cgfay.landmarklibrary.OneFace;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.FileUtils;
import com.tzutalin.dlib.PedestrianDet;
import com.tzutalin.dlib.VisionDetRet;

import java.io.File;
import java.util.List;
//import com.megvii.facepp.sdk.Facepp;
//import com.megvii.licensemanager.sdk.LicenseManager;

/**
 * 人脸检测器
 */
public final class FaceTrackerWithImage {

    private static final String TAG = "FaceTracker";
    FaceDet mFaceDet;
    PedestrianDet mPersonDet;

    private static class FaceTrackerHolder {
        private static FaceTrackerWithImage instance = new FaceTrackerWithImage();
    }



    private FaceTrackerWithImage() {


    }

    public static FaceTrackerWithImage getInstance() {
        return FaceTrackerHolder.instance;
    }

    public boolean trackFaceWithBitmap(final Context context, final Bitmap bitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String targetPath = Constants.getFaceShapeModelPath();
                if (!new File(targetPath).exists()) {
                    FileUtils.copyFileFromAssetToOthers(context, "shape_predictor_68_face_landmarks.dat",targetPath);
                }
                // Init
                if (mPersonDet == null) {
                    mPersonDet = new PedestrianDet();
                }
                if (mFaceDet == null) {
                    mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
                }
                List<VisionDetRet> detRets =  mFaceDet.detect(bitmap);
                // 计算人脸关键点
                if (detRets != null && detRets.size() > 0) {
                    for (int index = 0; index < detRets.size(); index++) {
                        VisionDetRet tempdetret  =detRets.get(index);
                        OneFace oneFace = LandmarkEngine.getInstance().getOneFace(index);
                        oneFace.confidence = tempdetret.getConfidence();
                        oneFace.mLeft = tempdetret.getLeft();
                        oneFace.mRight = tempdetret.getRight();
                        oneFace.mBottom = tempdetret.getBottom();
                        oneFace.mTop = tempdetret.getTop();
                        // 获取一个人的关键点坐标
                        if (oneFace.vertexPoints == null || oneFace.vertexPoints.length != tempdetret.getFaceLandmarks().size() * 2) {
                            oneFace.vertexPoints = new float[tempdetret.getFaceLandmarks().size() * 2];
                        }
                        for (int i = 0; i < tempdetret.getFaceLandmarks().size(); i++) {
                            // orientation = 0、3 表示竖屏，1、2 表示横屏
                            float x = (tempdetret.getFaceLandmarks().get(i).x / (float)bitmap.getWidth())*2  - 1;
                            float y = (tempdetret.getFaceLandmarks().get(i).y / (float)bitmap.getHeight())*2 - 1;
                            float[] point = new float[] {x, -y};
                            oneFace.vertexPoints[2 * i] = point[0];
                            oneFace.vertexPoints[2 * i + 1] = point[1];
                        }
                        // 插入人脸对象
                        LandmarkEngine.getInstance().putOneFace(index, oneFace);
                    }
                }
                // 设置人脸个数
                LandmarkEngine.getInstance().setFaceSize(detRets!= null ? detRets.size() : 0);
            }
        }).start();;
        return true;
    }

    /**
     * 释放资源
     */
    private void release(){
        if (mFaceDet != null){
            mFaceDet.release();
            mFaceDet = null;
        }
    }

}
