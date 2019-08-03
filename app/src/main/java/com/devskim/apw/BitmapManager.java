package com.devskim.apw;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Trace;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapManager {

    private static final int INPUT_SIZE = 512;
    private int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
    private float[] floatValues = new float[INPUT_SIZE * INPUT_SIZE * 3];
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output_new";
    private final Matrix matrix;

    public BitmapManager() {
        matrix = new Matrix();
        matrix.postScale(1f, 1f);
        matrix.postRotate(90);
    }

    private Bitmap scaleBitmap(Bitmap origin, int newWidth, int newHeight) {
        if (origin == null) {
            return null;
        }
        int height = origin.getHeight();
        int width = origin.getWidth();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newBitmap = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (!origin.isRecycled()) {
            origin.recycle();
        }
        return newBitmap;
    }

    public Bitmap convertBitmap(TensorFlowInferenceInterface inferenceInterface, Bitmap input_bitmap) {

        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
        input_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray() ;

        Bitmap bitmap0 = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        Bitmap bitmap = Bitmap.createScaledBitmap(bitmap0, INPUT_SIZE, INPUT_SIZE, false);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,true);
        return stylizeImage(inferenceInterface, bitmap);
    }


    public Bitmap stylizeImage(TensorFlowInferenceInterface inferenceInterface, Bitmap bitmap) {

        Bitmap scaledBitmap = scaleBitmap(bitmap, INPUT_SIZE, INPUT_SIZE);
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = ((val >> 16) & 0xFF) * 1.0f;
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) * 1.0f;
            floatValues[i * 3 + 2] = (val & 0xFF) * 1.0f;
        }

        Trace.beginSection("feed");
        inferenceInterface.feed(INPUT_NAME, floatValues, INPUT_SIZE, INPUT_SIZE, 3);
        Trace.endSection();

        Trace.beginSection("run");
        inferenceInterface.run(new String[]{OUTPUT_NAME});
        Trace.endSection();

        Trace.beginSection("fetch");
        inferenceInterface.fetch(OUTPUT_NAME, floatValues);
        Trace.endSection();

        for (int i = 0; i < intValues.length; ++i) {
            intValues[i] =
                    0xFF000000
                            | (((int) (floatValues[i * 3 + 0])) << 16)
                            | (((int) (floatValues[i * 3 + 1])) << 8)
                            | ((int) (floatValues[i * 3 + 2]));
        }
        scaledBitmap.setPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());
        return scaledBitmap;
    }

    public void saveBitmap(String path, Bitmap bitmap) {
        try {
            FileOutputStream out = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Bitmap getBitmap(String path) {
        try {
            Bitmap bitmap=null;
            File f= new File(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Drawable getImageByName(String nameOfTheDrawable, Activity a){
        Drawable drawFromPath;
        int path = a.getResources().getIdentifier(nameOfTheDrawable,"drawable", a.getPackageName());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap source = BitmapFactory.decodeResource(a.getResources(), path, options);

        drawFromPath = new BitmapDrawable(source);

        return drawFromPath;
    }

    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
        }

        return bitmap;
    }

}
