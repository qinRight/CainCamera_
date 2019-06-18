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
import android.support.annotation.Nullable;
import android.util.Log;

import com.cgfay.facedetectlibrary.listener.FaceTrackerCallback;
import com.cgfay.facedetectlibrary.utils.ConUtil;
import com.cgfay.facedetectlibrary.utils.SensorEventUtil;
import com.cgfay.landmarklibrary.LandmarkEngine;
import com.cgfay.landmarklibrary.OneFace;

import org.wysaid.nativePort.CGEFaceTracker;

import java.util.List;
//import com.megvii.facepp.sdk.Facepp;
//import com.megvii.licensemanager.sdk.LicenseManager;

/**
 * 人脸检测器
 */
public final class FaceTracker {

    private static final String TAG = "FaceTracker";
    private static final boolean VERBOSE = false;

    private final Object mSyncFence = new Object();

    // 人脸检测参数
    private FaceTrackParam mFaceTrackParam;

    // 检测线程
    private TrackerThread mTrackerThread;

    private static class FaceTrackerHolder {
        private static FaceTracker instance = new FaceTracker();
    }



    private FaceTracker() {
        mFaceTrackParam = FaceTrackParam.getInstance();

    }

    public static FaceTracker getInstance() {
        return FaceTrackerHolder.instance;
    }

    /**
     * 检测回调
     * @param callback
     * @return
     */
    public FaceTrackerBuilder setFaceCallback(FaceTrackerCallback callback) {
        return new FaceTrackerBuilder(this, callback);
    }

    /**
     * 准备检测器
     */
    void initTracker() {
        synchronized (mSyncFence) {
            mTrackerThread = new TrackerThread("FaceTrackerThread");
            mTrackerThread.start();
        }
    }

    /**
     * 初始化人脸检测
     * @param context       上下文
     * @param orientation   图像角度
     * @param width         图像宽度
     * @param height        图像高度
     */
    public void prepareFaceTracker(Context context, int orientation, int width, int height) {
        synchronized (mSyncFence) {
            if (mTrackerThread != null) {
                mTrackerThread.prepareFaceTracker(context, orientation, width, height);
            }
        }
    }

    public void prepareFaceTracker(Context context) {
        synchronized (mSyncFence) {
            if (mTrackerThread != null) {
                mTrackerThread.prepareFaceTracker(context, 0,1,1);
            }
        }
    }

//    /**
//     * 检测人脸
//     * @param data
//     * @param width
//     * @param height
//     */
//    public void trackFace(Context context,byte[] data, int width, int height) {
//        synchronized (mSyncFence) {
//            if (mTrackerThread != null) {
//                mTrackerThread.trackFace(context,data, width, height);
//            }
//        }
//    }

    public void trackFace(final Bitmap bitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mTrackerThread != null) {
                    mTrackerThread.trackFaceWithBitmap(bitmap);
                }
            }
        }).start();;

    }

    /**
     * 销毁检测器
     */
    public void destroyTracker() {
        synchronized (mSyncFence) {
            mTrackerThread.quitSafely();
        }

    }

    /**
     * 是否后置摄像头
     * @param backCamera
     * @return
     */
    public FaceTracker setBackCamera(boolean backCamera) {
        mFaceTrackParam.isBackCamera = backCamera;
        return this;
    }

    /**
     * 是否允许3D姿态角
     * @param enable
     * @return
     */
    public FaceTracker enable3DPose(boolean enable) {
        mFaceTrackParam.enable3DPose = enable;
        return this;
    }

    /**
     * 是否允许区域检测
     * @param enable
     * @return
     */
    public FaceTracker enableROIDetect(boolean enable) {
        mFaceTrackParam.enableROIDetect = enable;
        return this;
    }

    /**
     * 是否允许106个关键点
     * @param enable
     * @return
     */
    public FaceTracker enable106Points(boolean enable) {
        mFaceTrackParam.enable106Points = enable;
        return this;
    }

    /**
     * 是否允许多人脸检测
     * @param enable
     * @return
     */
    public FaceTracker enableMultiFace(boolean enable) {
        mFaceTrackParam.enableMultiFace = enable;
        return this;
    }

    /**
     * 是否允许人脸年龄检测
     * @param enable
     * @return
     */
    public FaceTracker enableFaceProperty(boolean enable) {
        mFaceTrackParam.enableFaceProperty = enable;
        return this;
    }

    /**
     * 最小检测人脸大小
     * @param size
     * @return
     */
    public FaceTracker minFaceSize(int size) {
        mFaceTrackParam.minFaceSize = size;
        return this;
    }

    /**
     * 检测时间间隔
     * @param interval
     * @return
     */
    public FaceTracker detectInterval(int interval) {
        mFaceTrackParam.detectInterval = interval;
        return this;
    }

    /**
     * 检测模式
     * @param mode
     * @return
     */
    public FaceTracker trackMode(int mode) {
        mFaceTrackParam.trackMode = mode;
        return this;
    }

    /**
     * Face++SDK联网请求验证
     */

    public void requestFaceNetwork(Context context) {

        FaceTrackParam.getInstance().canFaceTrack = true;
    }


    /**
     * 检测线程
     */
    private class TrackerThread extends Thread {

        // 人脸检测实体
//        private Facepp facepp;
        // 传感器监听器
        private SensorEventUtil mSensorUtil;

        private Looper mLooper;
        private @Nullable Handler mHandler;
        CGEFaceTracker facetracker;// = CGEFaceTracker.createFaceTracker();
        public TrackerThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (this) {
                mLooper = Looper.myLooper();
                notifyAll();
                mHandler = new Handler(mLooper);
            }
            Looper.loop();
            synchronized (this) {
                release();
                mHandler.removeCallbacksAndMessages(null);
                mHandler = null;
            }
        }

        /**
         * 安全退出
         * @return
         */
        public boolean quitSafely() {
            Looper looper = getLooper();
            if (looper != null) {
                looper.quitSafely();
                return true;
            }
            return false;
        }

        /**
         * 获取Looper
         * @return
         */
        public Looper getLooper() {
            if (!isAlive()) {
                return null;
            }
            synchronized (this) {
                while (isAlive() && mLooper == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return mLooper;
        }

        /**
         * 获取线程Handler
         * @return
         */
        public Handler getThreadHandler() {
            return mHandler;
        }

        /**
         * 初始化人脸检测
         * @param context       上下文
         * @param orientation   图像角度
         * @param width         图像宽度
         * @param height        图像高度
         */
        public void prepareFaceTracker(final Context context, final int orientation,
                                       final int width, final int height) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    internalPrepareFaceTracker(context, orientation, width, height);
                }
            });
        }

        public void prepareFaceTracker(final Context context) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    internalPrepareFaceTracker(context, 0, 1, 1);
                }
            });
        }

//        /**
//         * 检测人脸
//         * @param data      图像数据， NV21 或者 RGBA格式
//         * @param width     图像宽度
//         * @param height    图像高度
//         * @return          是否检测成功
//         */
//        public void trackFace(final Context context, final byte[] data, final int width, final int height) {
//            mHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    internalTrackFace(context,data, width, height);
//                }
//            });
//        }

        public void trackFace(final Bitmap bitmap) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    trackFaceWithBitmap(bitmap);
                }
            });
        }

        /**
         * 释放资源
         */
        private void release() {
//            if (facepp != null) {
//                facepp.release();
//                facepp = null;
//            }
            if (facetracker != null){
                facetracker.release();
                facetracker = null;
            }
        }

        /**
         * 初始化人脸检测
         * @param context       上下文
         * @param orientation   图像角度，预览时设置相机的角度，如果是静态图片，则为0
         * @param width         图像宽度
         * @param height        图像高度
         */
        private synchronized void internalPrepareFaceTracker(Context context, int orientation, int width, int height) {
            FaceTrackParam faceTrackParam = FaceTrackParam.getInstance();
            if (!faceTrackParam.canFaceTrack) {
                return;
            }
            release();
//            facepp = new Facepp();
            if (mSensorUtil == null) {
                mSensorUtil = new SensorEventUtil(context);
            }
            ConUtil.acquireWakeLock(context);
            if (!faceTrackParam.previewTrack) {
                faceTrackParam.rotateAngle = orientation;
            } else {
                faceTrackParam.rotateAngle = faceTrackParam.isBackCamera ? orientation : 360 - orientation;
            }

            int left = 0;
            int top = 0;
            int right = width;
            int bottom = height;
            // 限定检测区域
            if (faceTrackParam.enableROIDetect) {
                float line = height * faceTrackParam.roiRatio;
                left = (int) ((width - line) / 2.0f);
                top = (int) ((height - line) / 2.0f);
                right = width - left;
                bottom = height - top;
            }
            facetracker = CGEFaceTracker.createFaceTracker();
//            facepp.init(context, ConUtil.getFileContent(context, R.raw.megviifacepp_0_5_2_model));
//            Facepp.FaceppConfig faceppConfig = facepp.getFaceppConfig();
//            faceppConfig.interval = faceTrackParam.detectInterval;
//            faceppConfig.minFaceSize = faceTrackParam.minFaceSize;
//            faceppConfig.roi_left = left;
//            faceppConfig.roi_top = top;
//            faceppConfig.roi_right = right;
//            faceppConfig.roi_bottom = bottom;
//            faceppConfig.one_face_tracking = faceTrackParam.enableMultiFace ? 0 : 1;
//            faceppConfig.detectionMode = faceTrackParam.trackMode;
//            facepp.setFaceppConfig(faceppConfig);
        }

        /**
         * 检测人脸
         * @return          是否检测成功
         */
        private synchronized void trackFaceWithBitmap(Bitmap bitmap) {
            FaceTrackParam faceTrackParam = FaceTrackParam.getInstance();
            if (!faceTrackParam.canFaceTrack) {
                LandmarkEngine.getInstance().setFaceSize(0);
                if (faceTrackParam.trackerCallback != null) {
                    faceTrackParam.trackerCallback.onTrackingFinish();
                }
                return;
            }


            float[] faceresult = facetracker.detectFaceWithResult(bitmap, 0);

            // 计算人脸关键点
            if ( faceresult.length > 0) {
                OneFace oneFace = LandmarkEngine.getInstance().getOneFace(0);
                for (int index = 0; index < faceresult.length / 2; index++) {
                    // 获取一个人的关键点坐标
                    if (oneFace.vertexPoints == null) {
                        oneFace.vertexPoints = new float[faceresult.length * 2];
                    }
                    oneFace.vertexPoints[index * 2] = faceresult[index * 2] / bitmap.getHeight() - 1;
                    oneFace.vertexPoints[index * 2 + 1] = faceresult[index * 2 + 1] / bitmap.getWidth() - 1;

                }
                LandmarkEngine.getInstance().putOneFace(0, oneFace);
                LandmarkEngine.getInstance().setFaceSize(1);
            }else{
                LandmarkEngine.getInstance().setFaceSize(0);
            }
            // 设置人脸个数

            // 检测完成回调
            if (faceTrackParam.trackerCallback != null) {
                faceTrackParam.trackerCallback.onTrackingFinish();
            }
        }

    }



    public Allocation renderScriptNV21ToRGBA8888(Context context, int width, int height, byte[] nv21) {
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(nv21);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        return out;
    }
}
