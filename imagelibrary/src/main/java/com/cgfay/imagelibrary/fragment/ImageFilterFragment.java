package com.cgfay.imagelibrary.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import com.cgfay.facedetectlibrary.engine.FaceTracker;
import com.cgfay.facedetectlibrary.engine.FaceTrackerWithImage;
import com.cgfay.facedetectlibrary.listener.FaceTrackerCallback;
import com.cgfay.filterlibrary.glfilter.resource.FilterHelper;
import com.cgfay.filterlibrary.glfilter.resource.bean.ResourceData;
import com.cgfay.filterlibrary.manager.ImageParam;
import com.cgfay.filterlibrary.widget.GLImageSurfaceView;
import com.cgfay.imagelibrary.R;
import com.cgfay.imagelibrary.adapter.ImageFilterAdapter;
import com.cgfay.utilslibrary.utils.BitmapUtils;


import java.io.File;
import java.nio.ByteBuffer;

/**
 * 滤镜编辑页面
 */
public class ImageFilterFragment extends Fragment implements View.OnClickListener {

    private View mContentView;

    private Button mBtnInternal;
    private Button mBtnCustomize;
    private Button mBtnCollection;
    private Button mBtnSave;
    private Button mBtnAdd;
    private Button mBtnSetting;

    private FrameLayout mLayoutFilterContent;
    private GLImageSurfaceView mCainImageView;
    private RecyclerView mFiltersView;
    private LinearLayoutManager mLayoutManager;

    private Activity mActivity;
    private Handler mMainHandler;

    private Bitmap mBitmap;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.fragment_image_filter, container, false);
        return mContentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView(mContentView);
    }

    /**
     * 初始化视图
     * @param view
     */
    private void initView(View view) {
        // 图片内容布局
        mCainImageView = (GLImageSurfaceView) view.findViewById(R.id.glImageView);
        if (mBitmap != null) {
            mCainImageView.setBitmap(mBitmap);
            FaceTrackerWithImage.getInstance().trackFaceWithBitmap(mBitmap);
        }
        // 滤镜内容框
        mLayoutFilterContent = (FrameLayout) view.findViewById(R.id.layout_filter_content);
        mBtnInternal = (Button) view.findViewById(R.id.btn_internal);
        mBtnInternal.setOnClickListener(this);
        mBtnCustomize = (Button) view.findViewById(R.id.btn_customize);
        mBtnCustomize.setOnClickListener(this);
        mBtnCollection = (Button) view.findViewById(R.id.btn_collection);
        mBtnCollection.setOnClickListener(this);
        mBtnSave = (Button) view.findViewById(R.id.btn_save);
        mBtnSave.setOnClickListener(this);
        mBtnAdd = (Button) view.findViewById(R.id.btn_add);
        mBtnAdd.setOnClickListener(this);
        mBtnSetting = (Button) view.findViewById(R.id.btn_setting);
        mBtnSetting.setOnClickListener(this);
        showFilters();
        Button comparebutton = view.findViewById(R.id.comparebutton);
        comparebutton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    ImageParam.getInstance().showCompare = true;
                    mCainImageView.startNewRender();
                    FaceTrackerWithImage.getInstance().trackFaceWithBitmap(mBitmap);
                }else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE){

                }else{
                    ImageParam.getInstance().showCompare = false;
                    mCainImageView.startNewRender();
                }
                return true;
            }
        });
        slimfaceseekbar = view.findViewById(R.id.slimfaceseekbar);
        slimfaceseekbar.setOnSeekBarChangeListener(onelistener);
        facesmallseekbar = view.findViewById(R.id.facesmallseekbar);
        facesmallseekbar.setOnSeekBarChangeListener(onelistener);
        bigeyeseekbar = view.findViewById(R.id.bigeyeseekbar);
        bigeyeseekbar.setOnSeekBarChangeListener(onelistener);
    }

    SeekBar slimfaceseekbar;
    SeekBar facesmallseekbar ;
    SeekBar bigeyeseekbar;

    SeekBar.OnSeekBarChangeListener onelistener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (seekBar == slimfaceseekbar) {
                ImageParam.getInstance().beauty.faceLift = seekBar.getProgress() / 100.f;
            }else if (seekBar == facesmallseekbar) {
                ImageParam.getInstance().beauty.chinIntensity = seekBar.getProgress() / 100.f;
            }else if (seekBar == bigeyeseekbar) {
                ImageParam.getInstance().beauty.eyeEnlargeIntensity = seekBar.getProgress() / 100.f;
            }
            mCainImageView.startNewRender();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (mCainImageView != null) {
            mCainImageView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCainImageView != null) {
            mCainImageView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        mContentView = null;
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_internal) {

        } else if (id == R.id.btn_customize) {

        } else if (id == R.id.btn_collection) {

        } else if (id == R.id.btn_add) {

        } else if (id == R.id.btn_setting) {

        } else if (id == R.id.btn_save) {
            if (mCainImageView != null) {
                mCainImageView.getCaptureFrame(mCaptureCallback);
            }
        }
    }

    /**
     * 重置按钮颜色
     */
    private void resetButtonColor() {
        mBtnInternal.setTextColor(Color.WHITE);
        mBtnCustomize.setTextColor(Color.WHITE);
        mBtnCollection.setTextColor(Color.WHITE);
        mBtnAdd.setTextColor(Color.WHITE);
        mBtnSetting.setTextColor(Color.WHITE);
    }

    /**
     * 显示滤镜列表
     */
    private void showFilters() {
        resetButtonColor();
        mBtnInternal.setTextColor(Color.BLUE);
        if (mFiltersView == null) {
            mFiltersView = new RecyclerView(mActivity);
            mLayoutManager = new LinearLayoutManager(getActivity());
            mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            mFiltersView.setLayoutManager(mLayoutManager);
            ImageFilterAdapter adapter = new ImageFilterAdapter(mActivity, FilterHelper.getFilterList());
            mFiltersView.setAdapter(adapter);
            adapter.addOnFilterChangeListener(new ImageFilterAdapter.OnFilterChangeListener() {
                @Override
                public void onFilterChanged(final ResourceData resourceData) {
                    if (mCainImageView != null) {
                        mCainImageView.setNormalFilter(resourceData);
                    }
                }
            });
        }
        if (mLayoutFilterContent != null) {
            mLayoutFilterContent.removeAllViews();
            mLayoutFilterContent.addView(mFiltersView);
        }
    }

    /**
     * 设置bitmap
     * @param bitmap
     */
    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        if (mCainImageView != null) {
            mCainImageView.setBitmap(mBitmap);
        }
    }

    /**
     * 是否显示GLSurfaceView，解决多重fragment时显示问题
     * @param showing
     */
    public void showGLSurfaceView(boolean showing) {
        if (mCainImageView != null) {
            mCainImageView.setVisibility(showing ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 截屏回调
     */
    private GLImageSurfaceView.CaptureCallback mCaptureCallback = new GLImageSurfaceView.CaptureCallback() {
        @Override
        public void onCapture(final ByteBuffer buffer, final int width, final int height) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    String filePath = getDCIMImagePath(mActivity);
                    BitmapUtils.saveBitmap(filePath, buffer, width, height);
                    Log.d("hahaha", "run: " + filePath);
                }
            });
        }
    };

    /**
     * 获取图片缓存绝对路径
     * @param context
     * @return
     */
    private static String getDCIMImagePath(Context context) {
        String directoryPath;
        // 判断外部存储是否可用，如果不可用则使用内部存储路径
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            directoryPath =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        } else { // 使用内部存储缓存目录
            directoryPath = context.getCacheDir().getAbsolutePath();
        }
        String path = directoryPath + File.separator + Environment.DIRECTORY_PICTURES + File.separator + "CainCamera_" + System.currentTimeMillis() + ".jpeg";
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return path;
    }
}
