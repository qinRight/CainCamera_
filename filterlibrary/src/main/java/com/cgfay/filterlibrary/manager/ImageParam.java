package com.cgfay.filterlibrary.manager;

import android.graphics.PointF;

import com.cgfay.filterlibrary.glfilter.beauty.bean.BeautyParam;

/**
 * 相机配置参数
 */
public final class ImageParam {
    public float lookupValue = 1;
    public float imageDepthValue = 0;

    public PointF vignetteCenter = new PointF(0.5f,0.5f);
    public float[] vignetteColor = new float[] {0.0f, 0.0f, 0.0f};
    public float vignetteStart = 0.3f;
    public float vignetteEnd = 0.75f;
    public boolean drawFacePoints;
    // 是否显示对比效果
    public boolean showCompare;

    // 是否允许景深
    public boolean enableDepthBlur;
    // 是否允许暗角
    public boolean enableVignette;
    // 美颜参数
    public BeautyParam beauty;

    private static final ImageParam mInstance = new ImageParam();

    private ImageParam() {
        reset();
    }

    /**
     * 重置为初始状态
     */
    private void reset() {

        showCompare = false;
        enableDepthBlur = false;
        enableVignette = false;
        beauty = new BeautyParam();
    }

    /**
     * 获取相机配置参数
     * @return
     */
    public static ImageParam getInstance() {
        return mInstance;
    }

}
