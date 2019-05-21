package com.cgfay.filterlibrary.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.cgfay.filterlibrary.glfilter.base.GLImageFilter;
import com.cgfay.filterlibrary.glfilter.base.GLImageInputFilter;
import com.cgfay.filterlibrary.glfilter.color.GLImageDynamicColorFilter;
import com.cgfay.filterlibrary.glfilter.color.bean.DynamicColor;
import com.cgfay.filterlibrary.glfilter.face.GLImageFaceReshapeFilter;
import com.cgfay.filterlibrary.glfilter.resource.FilterHelper;
import com.cgfay.filterlibrary.glfilter.resource.ResourceJsonCodec;
import com.cgfay.filterlibrary.glfilter.resource.bean.ResourceData;
import com.cgfay.filterlibrary.glfilter.utils.OpenGLUtils;
import com.cgfay.filterlibrary.glfilter.utils.TextureRotationUtils;
import com.cgfay.filterlibrary.manager.RenderImageManager;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 图片GL渲染视图
 */
public class GLImageSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {

    // 输入纹理
    protected int mInputTexture = OpenGLUtils.GL_NOT_TEXTURE;

    // 输入纹理大小
    protected int mTextureWidth;
    protected int mTextureHeight;
    // 控件视图大小
    protected int mViewWidth;
    protected int mViewHeight;

    // 输入图片
    private Bitmap mBitmap;

    // UI线程Handler，主要用于更新UI等
    protected Handler mMainHandler;

    public GLImageSurfaceView(Context context) {
        this(context, null);
    }

    public GLImageSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onPause() {
        super.onPause();
        RenderImageManager.getInstance().release();
        if (mInputTexture != OpenGLUtils.GL_NOT_TEXTURE) {
            OpenGLUtils.deleteTexture(mInputTexture);
            mInputTexture = OpenGLUtils.GL_NOT_TEXTURE;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glDisable(GL10.GL_DITHER);
        GLES30.glClearColor(0,0, 0, 0);
        GLES30.glEnable(GL10.GL_CULL_FACE);
        GLES30.glEnable(GL10.GL_DEPTH_TEST);
        initFilters();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mViewWidth = width;
        mViewHeight = height;
        GLES30.glViewport(0,0,width, height);
        if (mInputTexture == OpenGLUtils.GL_NOT_TEXTURE) {
            mInputTexture = OpenGLUtils.createTexture(mBitmap, mInputTexture);
        }
        // Note: 如果此时显示输出滤镜对象为空，则表示调用了onPause方法销毁了所有GL对象资源，需要重新初始化滤镜
        if (!RenderImageManager.getInstance().hasInit()) {
            initFilters();
        }
        RenderImageManager.getInstance().setTextureSize(mTextureWidth,mTextureHeight);
        RenderImageManager.getInstance().setDisplaySize(mViewWidth,mViewHeight);
    }

    /**
     * 初始化滤镜
     */
    private void initFilters() {
        RenderImageManager.getInstance().init(getContext());
        if (mBitmap != null) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    calculateViewSize();
                }
            });
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClearColor(0,0, 0, 0);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        int currentTexture = mInputTexture;
        RenderImageManager.getInstance().drawFrame(currentTexture);
    }

    /**
     * 设置颜色滤镜
     * @param resourceData
     */
    public void setNormalFilter(final ResourceData resourceData) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                createColorFilter(resourceData);
                requestRender();
            }
        });
    }
    /**
     * 设置LUT滤镜
     * @param bitmap
     */
    public void setLookupFilter(final Bitmap bitmap) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                createLookupFilter(bitmap);
                requestRender();
            }
        });
    }

    /**
     * 拍照
     */
    public void getCaptureFrame(final CaptureCallback captureCallback) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                int width = getWidth();
                int height = getHeight();
                ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                GLES30.glReadPixels(0, 0, width, height,
                        GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buf);
                OpenGLUtils.checkGlError("glReadPixels");
                buf.rewind();
                if (captureCallback != null) {
                    captureCallback.onCapture(buf, width, height);
                }
            }
        });
    }

    /**
     * 创建颜色滤镜
     * @param resourceData
     */
    private void createColorFilter(ResourceData resourceData) {
        try {
            String folderPath = FilterHelper.getFilterDirectory(getContext()) + File.separator + resourceData.unzipFolder;
            DynamicColor dynamicColor = ResourceJsonCodec.decodeFilterData(folderPath);
            RenderImageManager.getInstance().changeDynamicFilter(dynamicColor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createLookupFilter(Bitmap bitmap) {
        try {
            RenderImageManager.getInstance().changeDynamicFilterWithLookup(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置滤镜
     * @param bitmap
     */
    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        mTextureWidth = mBitmap.getWidth();
        mTextureHeight = mBitmap.getHeight();
        requestRender();
    }

    /**
     * 计算视图大小
     */
    private void calculateViewSize() {
        if (mTextureWidth == 0 || mTextureHeight == 0) {
            return;
        }
        if (mViewWidth == 0 || mViewHeight == 0) {
            mViewWidth = getWidth();
            mViewHeight = getHeight();
        }
        float ratio = mTextureWidth * 1.0f / mTextureHeight;
        double viewAspectRatio = (double) mViewWidth / mViewHeight;
        if (ratio < viewAspectRatio) {
            mViewWidth = (int) (mViewHeight * ratio);
        } else {
            mViewHeight = (int) (mViewWidth / ratio);
        }
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = mViewWidth;
        layoutParams.height = mViewHeight;
        setLayoutParams(layoutParams);
        RenderImageManager.getInstance().setDisplaySize(mViewWidth,mViewHeight);
    }

    public void startNewRender(){
        queueEvent(new Runnable() {
            @Override
            public void run() {
                requestRender();
            }
        });
    }

    /**
     * 截帧回调
     */
    public interface CaptureCallback {
        void onCapture(ByteBuffer buffer, int width, int height);
    }
}
