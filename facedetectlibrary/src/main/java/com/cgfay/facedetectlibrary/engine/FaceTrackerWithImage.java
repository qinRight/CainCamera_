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

import org.wysaid.nativePort.CGEFaceTracker;

import java.io.File;
import java.util.List;
//import com.megvii.facepp.sdk.Facepp;
//import com.megvii.licensemanager.sdk.LicenseManager;

/**
 * 人脸检测器
 */
public final class FaceTrackerWithImage {

    private static final String TAG = "FaceTracker";
    CGEFaceTracker facetracker;

    private static class FaceTrackerHolder {
        private static FaceTrackerWithImage instance = new FaceTrackerWithImage();
    }



    private FaceTrackerWithImage() {


    }

    public static FaceTrackerWithImage getInstance() {
        return FaceTrackerHolder.instance;
    }

    public boolean trackFaceWithBitmap(final Bitmap bitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (facetracker == null){
                    facetracker = CGEFaceTracker.createFaceTracker();
                }


                float[] faceresult = facetracker.detectFaceWithResult(bitmap, 0);

                // 计算人脸关键点
                if ( faceresult.length > 0) {
                    OneFace oneFace = LandmarkEngine.getInstance().getOneFace(0);
                    oneFace.vertexPoints = new float[faceresult.length];
                    for (int index = 0; index < faceresult.length / 2; index++) {
                        // 获取一个人的关键点坐标
                        oneFace.vertexPoints[index * 2] = faceresult[index * 2] / bitmap.getWidth() ;
                        oneFace.vertexPoints[index * 2 + 1] = (faceresult[index * 2 + 1] / bitmap.getHeight() );

                    }
                    LandmarkEngine.getInstance().putOneFace(0, oneFace);
                    LandmarkEngine.getInstance().setFaceSize(1);
                }else{
                    LandmarkEngine.getInstance().setFaceSize(0);
                }
            }
        }).start();;
        return true;
    }

    /**
     * 释放资源
     */
    private void release(){
        if (facetracker != null){
            facetracker.release();
            facetracker = null;
        }
    }

}
