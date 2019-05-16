package com.tzutalin.dlib;

import android.content.Context;
import android.os.Environment;

import com.cgfay.facedetectlibrary.R;

import java.io.File;

/**
 * Created by darrenl on 2016/4/22.
 */
public final class Constants {
    private Constants() {
        // Constants should be prive
    }

    /**
     * getFaceShapeModelPath
     * @return default face shape model path
     */
    public static String getFaceShapeModelPath() {

        File sdcard = Environment.getExternalStorageDirectory();
        String targetPath = sdcard.getAbsolutePath() + File.separator + "sixeightfacelandmarks.dat";
        return targetPath;
    }

    public static void checkFaceModelState(Context context){
        final String targetPath = Constants.getFaceShapeModelPath();
        if (!new File(targetPath).exists()) {
            FileUtils.copyFileFromAssetToOthers(context, "sixeightfacelandmarks.dat", targetPath);
        }
    }
}
