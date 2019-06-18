package com.cgfay.filterlibrary.glfilter.face;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import com.cgfay.filterlibrary.glfilter.base.GLImageDrawElementsFilter;
import com.cgfay.filterlibrary.glfilter.beauty.bean.BeautyParam;
import com.cgfay.filterlibrary.glfilter.beauty.bean.IBeautify;
import com.cgfay.filterlibrary.glfilter.utils.OpenGLUtils;
import com.cgfay.filterlibrary.glfilter.utils.TextureRotationUtils;
import com.cgfay.landmarklibrary.FacePointsUtils;
import com.cgfay.landmarklibrary.LandmarkEngine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 脸型调整变换
 */
public class GLImageFaceReshapeFilter extends GLImageDrawElementsFilter implements IBeautify {

    // 122个关键点
    private final int IndicesLength = 76 * 2;
    // 106 个关键点
    private final int FacePoints = 76;
    // 顶点坐标
    private float[] mVertices = new float[76 * 2];
    // 纹理坐标
    private float[] mTextureVertices = new float[76 * 2];
    // 笛卡尔坐标系
    private float[] mCartesianVertices = new float[76 * 2];
    // 脸型程度
    private float[] mReshapeIntensity = new float[12];

    // 顶点坐标缓冲
    private FloatBuffer mVertexBuffer;
    // 纹理坐标缓冲
    private FloatBuffer mTextureBuffer;
    // 笛卡尔坐标缓冲
    private FloatBuffer mCartesianBuffer;

    private int mCartesianPointsHandle;
    private int mReshapeIntensityHandle;
    private int mTextureWidthHandle;
    private int mTextureHeightHandle;
    private int mEnableReshapeHandle;
    private int mSlimRadiusHandle;
    private int mAspectRatioHandle;
    public GLImageFaceReshapeFilter(Context context) {
        super(context, OpenGLUtils.getShaderFromAssets(context, "shader/face/vertex_face_reshape.glsl"),
                OpenGLUtils.getShaderFromAssets(context, "shader/face/fragment_face_reshape.glsl"));
    }

    @Override
    public void initProgramHandle() {
        super.initProgramHandle();
        if (mProgramHandle != OpenGLUtils.GL_NOT_INIT) {
            mCartesianPointsHandle = GLES30.glGetUniformLocation(mProgramHandle, "cartesianPoints");
            mReshapeIntensityHandle = GLES30.glGetUniformLocation(mProgramHandle, "reshapeIntensity");
            mTextureWidthHandle = GLES30.glGetUniformLocation(mProgramHandle, "textureWidth");
            mTextureHeightHandle = GLES30.glGetUniformLocation(mProgramHandle, "textureHeight");
            mEnableReshapeHandle = GLES30.glGetUniformLocation(mProgramHandle, "enableReshape");
            mSlimRadiusHandle = GLES30.glGetUniformLocation(mProgramHandle, "slimRadius");
            mAspectRatioHandle = GLES30.glGetUniformLocation(mProgramHandle, "aspectRatio");
        }
    }

    @Override
    protected void initBuffers() {
        mVertexBuffer = ByteBuffer.allocateDirect(IndicesLength * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureBuffer = ByteBuffer.allocateDirect(IndicesLength * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mCartesianBuffer = ByteBuffer.allocateDirect(FacePoints * 2 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mIndexBuffer = OpenGLUtils.createShortBuffer(FaceImageIndicesFor68);
    }

    @Override
    protected void releaseBuffers() {
        super.releaseBuffers();
        if (mVertexBuffer != null) {
            mVertexBuffer.clear();
            mVertexBuffer = null;
        }
        if (mTextureBuffer != null) {
            mTextureBuffer.clear();
            mTextureBuffer = null;
        }
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        setInteger(mTextureWidthHandle, width);
        setInteger(mTextureHeightHandle, height);
    }

    @Override
    public boolean drawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        updateFaceVertices();
        if (LandmarkEngine.getInstance().hasFace()) {
            return super.drawFrame(textureId, mVertexBuffer, mTextureBuffer);
        } else {
            return super.drawFrame(textureId, vertexBuffer, textureBuffer);
        }
    }

    @Override
    public int drawFrameBuffer(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        updateFaceVertices();
        if (LandmarkEngine.getInstance().hasFace()) {
            return super.drawFrameBuffer(textureId, mVertexBuffer, mTextureBuffer);
        } else {
            return super.drawFrameBuffer(textureId, vertexBuffer, textureBuffer);
        }
    }

    @Override
    public void onDrawFrameBegin() {
        super.onDrawFrameBegin();
        GLES20.glUniform1fv(mReshapeIntensityHandle, 7, FloatBuffer.wrap(mReshapeIntensity));
        GLES30.glUniform2fv(mCartesianPointsHandle, FacePoints, mCartesianBuffer);
    }

    /**
     * 更新顶点
     */
    private void updateFaceVertices() {
        if (LandmarkEngine.getInstance().hasFace()) {
            LandmarkEngine.getInstance().updateFaceAdjustPointsFor68(mVertices, mTextureVertices, 0);
            mVertexBuffer.clear();
            mVertexBuffer.put(mVertices);
            mVertexBuffer.position(0);

            mTextureBuffer.clear();
            mTextureBuffer.put(mTextureVertices);
            mTextureBuffer.position(0);

            updateCartesianVertices();

            mIndexBuffer.clear();
            mIndexBuffer.put(FaceImageIndicesFor68);
            mIndexBuffer.position(0);
            mIndexLength = mIndexBuffer.capacity();

            setInteger(mEnableReshapeHandle, 1);
        } else { // 没有人脸时索引变回默认的6个
            mIndexBuffer.clear();
            mIndexBuffer.put(TextureRotationUtils.Indices);
            mIndexBuffer.position(0);
            mIndexLength = 6;
            setInteger(mEnableReshapeHandle, 0);
        }
    }

    /**
     * 更新笛卡尔坐标系
     */
    private void updateCartesianVertices() {
        for (int i = 0; i < FacePoints; i++) {
            mCartesianVertices[i * 2] = mTextureVertices[i * 2] * mImageWidth;
            mCartesianVertices[i * 2 + 1] = mTextureVertices[i * 2 + 1] * mImageHeight;
        }
        mCartesianBuffer.clear();
        mCartesianBuffer.put(mCartesianVertices);
        mCartesianBuffer.position(0);
    }


    @Override
    public void onBeauty(BeautyParam beauty) {
        if (beauty == null) {
            return;
        }
        mReshapeIntensity[0]  = beauty.faceLift;                // 瘦脸
        mReshapeIntensity[1]  = beauty.faceShave;               // 削脸
        mReshapeIntensity[2]  = beauty.faceNarrow;              // 小脸
        mReshapeIntensity[3]  = beauty.chinIntensity;           // 下巴
        mReshapeIntensity[4]  = beauty.foreheadIntensity;       // 额头
        mReshapeIntensity[5]  = beauty.eyeEnlargeIntensity;     // 大眼
        mReshapeIntensity[6]  = beauty.eyeDistanceIntensity;    // 眼距
        mReshapeIntensity[7]  = beauty.eyeCornerIntensity;      // 眼角
        mReshapeIntensity[8]  = beauty.noseThinIntensity;       // 瘦鼻
        mReshapeIntensity[9]  = beauty.alaeIntensity;           // 鼻翼
        mReshapeIntensity[10] = beauty.proboscisIntensity;      // 长鼻
        mReshapeIntensity[11] = beauty.mouthEnlargeIntensity;   // 嘴型
    }

    /**
     * 人脸图像索引(702个点)（122个关键点）
     * 具体的关键点可参考 landmarklibrary的assets目录下的三角剖.jpg
     */
    private static final short[] FaceImageIndices = {
            // 脸外索引(人脸顶部中心逆时针数)
            110, 114, 111, //0
            111, 114, 115,//1
            115, 111, 32,//2
            32, 115, 116,//3
            116, 32, 31,//4
            31, 116, 30,//5
            30, 116, 29,//6
            29, 116, 28,//7
            28, 116, 27,//8
            27, 116, 26,//9
            26, 116, 25,//10
            25, 116, 117,//11
            117, 25, 24,//12
            24, 117, 23,//13
            23, 117, 22,//14
            22, 117, 21,//15
            21, 117, 20,//16
            20, 117, 19,//17
            19, 117, 118,//18
            118, 19, 18,//19
            18, 118, 17,//20
            17, 118, 16,//21
            16, 118, 15,//22
            15, 118, 14,//23
            14, 118, 13,//24
            13, 118, 119,//25
            119, 13, 12,//26
            12, 119, 11,//27
            11, 119, 10,//28
            10, 119, 9,//29
            9, 119, 8,//30
            8, 119, 7,//31
            7, 119, 120,//32
            120, 7, 6,//33
            6, 120, 5,//34
            5, 120, 4,//35
            4, 120, 3,//36
            3, 120, 2,//37
            2, 120, 1,//38
            1, 120, 0,//39
            0, 120, 121,//40
            121, 0, 109,//41
            109, 121, 114,//42
            114, 109, 110,//43
            // 脸内部索引
            // 额头
            0, 33, 109,//44
            109, 33, 34,//45
            34, 109, 35,//46
            35, 109, 36,//47
            36, 109, 110,//48
            36, 110, 37,//49
            37, 110, 43,//50
            43, 110, 38,//51
            38, 110, 39,//52
            39, 110, 111,//53
            111, 39, 40,//54
            40, 111, 41,//55
            41, 111, 42,//56
            42, 111, 32,//57
            // 左眉毛
            33, 34, 64,//58
            64, 34, 65,//59
            65, 34, 107,//60
            107, 34, 35,//61
            35, 36, 107,//62
            107, 36, 66,//63
            66, 107, 65,//64
            66, 36, 67,//65
            67, 36, 37,//66
            37, 67, 43,//67
            // 右眉毛
            43, 38, 68,//68
            68, 38, 39,//69
            39, 68, 69,//70
            39, 40, 108,//71
            39, 108, 69,//72
            69, 108, 70,//73
            70, 108, 41,//74
            41, 108, 40,//75
            41, 70, 71,//76
            71, 41, 42,//77
            // 左眼
            0, 33, 52,//78
            33, 52, 64,//79
            52, 64, 53,//80
            64, 53, 65,//81
            65, 53, 72,//82
            65, 72, 66,//83
            66, 72, 54,//84
            66, 54, 67,//85
            54, 67, 55,//86
            67, 55, 78,//87
            67, 78, 43,//88
            52, 53, 57,//89
            53, 72, 74,//90
            53, 74, 57,//91
            74, 57, 73,//92
            72, 54, 104,//93
            72, 104, 74,//94
            74, 104, 73,//95
            73, 104, 56,//96
            104, 56, 54,//97
            54, 56, 55,//98
            // 右眼
            68, 43, 79,//99
            68, 79, 58,//100
            68, 58, 59,//101
            68, 59, 69,//102
            69, 59, 75,//103
            69, 75, 70,//104
            70, 75, 60,//105
            70, 60, 71,//106
            71, 60, 61,//107
            71, 61, 42,//108
            42, 61, 32,//109
            61, 60, 62,//110
            60, 75, 77,//111
            60, 77, 62,//112
            77, 62, 76,//113
            75, 77, 105,//114
            77, 105, 76,//115
            105, 76, 63,//116
            105, 63, 59,//117
            105, 59, 75,//118
            59, 63, 58,//119
            // 左脸颊
            0, 52, 1,
            1, 52, 2,
            2, 52, 57,
            2, 57, 3,
            3, 57, 4,
            4, 57, 112,
            57, 112, 74,
            74, 112, 56,
            56, 112, 80,
            80, 112, 82,
            82, 112, 7,
            7, 112, 6,
            6, 112, 5,
            5, 112, 4,
            56, 80, 55,
            55, 80, 78,
            // 右脸颊
            32, 61, 31,
            31, 61, 30,
            30, 61, 62,
            30, 62, 29,
            29, 62, 28,
            28, 62, 113,
            62, 113, 76,
            76, 113, 63,
            63, 113, 81,
            81, 113, 83,
            83, 113, 25,
            25, 113, 26,
            26, 113, 27,
            27, 113, 28,
            63, 81, 58,
            58, 81, 79,
            // 鼻子部分
            78, 43, 44,
            43, 44, 79,
            78, 44, 80,
            79, 81, 44,
            80, 44, 45,
            44, 81, 45,
            80, 45, 46,
            45, 81, 46,
            80, 46, 82,
            81, 46, 83,
            82, 46, 47,
            47, 46, 48,
            48, 46, 49,
            49, 46, 50,
            50, 46, 51,
            51, 46, 83,
            // 鼻子和嘴巴中间三角形
            7, 82, 84,
            82, 84, 47,
            84, 47, 85,
            85, 47, 48,
            48, 85, 86,
            86, 48, 49,
            49, 86, 87,
            49, 87, 88,
            88, 49, 50,
            88, 50, 89,
            89, 50, 51,
            89, 51, 90,
            51, 90, 83,
            83, 90, 25,
            // 上嘴唇部分
            84, 85, 96,
            96, 85, 97,
            97, 85, 86,
            86, 97, 98,
            86, 98, 87,
            87, 98, 88,
            88, 98, 99,
            88, 99, 89,
            89, 99, 100,
            89, 100, 90,
            // 下嘴唇部分
            90, 100, 91,
            100, 91, 101,
            101, 91, 92,
            101, 92, 102,
            102, 92, 93,
            102, 93, 94,
            102, 94, 103,
            103, 94, 95,
            103, 95, 96,
            96, 95, 84,
            // 唇间部分
            96, 97, 103,
            97, 103, 106,
            97, 106, 98,
            106, 103, 102,
            106, 102, 101,
            106, 101, 99,
            106, 98, 99,
            99, 101, 100,
            // 嘴巴与下巴之间的部分(关键点7 到25 与嘴巴鼻翼围起来的区域)
            7, 84, 8,
            8, 84, 9,
            9, 84, 10,
            10, 84, 95,
            10, 95, 11,
            11, 95, 12,
            12, 95, 94,
            12, 94, 13,
            13, 94, 14,
            14, 94, 93,
            14, 93, 15,
            15, 93, 16,
            16, 93, 17,
            17, 93, 18,
            18, 93, 92,
            18, 92, 19,
            19, 92, 20,
            20, 92, 91,
            20, 91, 21,
            21, 91, 22,
            22, 91, 90,
            22, 90, 23,
            23, 90, 24,
            24, 90, 25
    };

    private static final short[] FaceImageIndicesFor68 = {
            // 脸外索引(人脸顶部中心逆时针数)
            21,27,66,
            66,27,22,
            22,66,23,
            23,66,24,
            24,66,67,
            67,24,25,
            25,67,26,
            26,67,16,
            16,67,68,
            68,16,15,
            15,68,14,
            14,68,13,
            13,68,12,
            12,68,69,
            69,12,11,
            11,69,10,
            10,69,9,
            9,69,70,
            70,9,8,
            8,70,7,
            7,70,71,
            71,7,6,
            6,71,5,
            5,71,4,
            4,71,72,
            72,4,3,
            3,72,2,
            2,72,1,
            1,72,0,
            0,72,73,
            73,0,17,
            17,73,18,
            18,73,19,
            19,73,66,
            66,19,20,
            20,66,21,
            // 脸内部索引
            // 额头
            // 左眼
            36,1,0,
            0,17,36,
            36,17,37,
            37,17,18,
            18,37,38,
            38,18,19,
            19,38,20,
            20,39,21,
            21,39,27,
            36,41,37,
            37,41,40,
            40,37,38,
            38,40,39,


            // 右眼
            22,27,42,
            42,22,23,
            23,42,43,
            43,23,24,
            24,43,25,
            25,43,44,
            44,25,26,
            26,44,45,
            45,26,16,
            16,45,15,
            44,45,46,
            46,44,43,
            43,46,47,
            47,43,42,

            // 左脸颊
            1,36,41,
            41,1,2,
            2,41,40,
            40,2,29,
            29,40,28,
            28,40,39,
            39,28,27,
            2,29,3,
            3,29,31,
            // 右脸颊
            27,42,28,
            28,42,47,
            47,28,29,
            29,47,14,
            14,47,46,
            46,14,15,
            15,46,45,
            29,14,13,
            13,29,35,
            // 鼻子部分

            29,31,30,
            30,29,35,
            31,30,32,
            32,30,33,
            33,30,34,
            34,30,35,
            // 鼻子和嘴巴中间三角形
            3,31,4,
            4,31,48,
            48,31,49,
            49,31,50,
            50,31,32,
            32,50,33,
            33,50,51,
            51,33,52,
            52,33,34,
            34,52,35,
            35,52,53,
            53,35,54,
            54,35,12,
            12,35,13,
            // 上嘴唇部分
            48,49,60,
            60,49,50,
            50,60,61,
            61,50,51,
            51,61,52,
            52,61,62,
            62,52,53,
            53,62,54,
            // 下嘴唇部分
            48,59,65,
            65,59,58,
            58,65,57,
            57,65,64,
            64,57,56,
            56,64,63,
            63,56,55,
            55,63,54,
            // 唇间部分
            48,65,60,
            60,65,64,
            64,60,61,
            61,64,63,
            63,61,62,
            62,63,54,
            // 嘴巴与下巴之间的部分(关键点7 到25 与嘴巴鼻翼围起来的区域)
            4,48,5,
            5,48,6,
            6,48,59,
            59,6,7,
            7,59,58,
            58,7,8,
            8,58,57,
            57,8,56,
            56,8,9,
            9,56,55,
            55,9,10,
            10,55,54,
            54,10,11,
            11,54,12
    };

}
