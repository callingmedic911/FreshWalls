package com.adityaworks.freshwalls;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

/**
 * Created by callingmedic911 on 26-Dec-17.
 */

public class GenerateWallpaper extends AsyncTask<String, Void, Bitmap> {

    private static final String TAG = GenerateWallpaper.class.getSimpleName();
    private WeakReference<Activity> mActivity;
    private WeakReference<View> mView;
    private OnGenerationComplete listener;

    public GenerateWallpaper(Activity activity, View view, OnGenerationComplete listener) {
        mActivity = new WeakReference<>(activity);
        mView = new WeakReference<>(view);
        this.listener = listener;
    }

    @Override
    protected Bitmap doInBackground(String... svg) {
        Log.v(TAG, "Inside doInBackground");
        return imageFromSVG(svg[0], mActivity.get());
    }

    @Override
    protected void onPostExecute(Bitmap newWall) {
//        setNewWallpaper(newWall);
        ImageView imageView = mView.get().findViewById(R.id.output);
        imageView.setImageBitmap(newWall);
        listener.onGenerationComplete(newWall);
    }

    public static Bitmap imageFromSVG(String imageData, Activity context) {
        SVG tile = null;
        try {
            tile = SVG.getFromString(imageData);
            tile.setDocumentViewBox(0, 0, tile.getDocumentWidth(), tile.getDocumentHeight());
            tile.setDocumentWidth(2*tile.getDocumentWidth());
            tile.setDocumentHeight(2*tile.getDocumentHeight());
        } catch (SVGParseException e) {
            e.printStackTrace();
        }

        // Convert svg to a bitmap tile
        int   svgWidth = (tile.getDocumentWidth() != -1) ? (int) tile.getDocumentWidth() : 500;
        int   svgHeight = (tile.getDocumentHeight() != -1) ? (int) tile.getDocumentHeight() : 500;
        Bitmap newBM = Bitmap.createBitmap(svgWidth, svgHeight, Bitmap.Config.ARGB_8888);
        Canvas tileCanvas = new Canvas(newBM);
        tile.renderToCanvas(tileCanvas);

        //Repeat tile bitmap to new canvas
        BitmapDrawable TileMe = new BitmapDrawable(context.getResources(), newBM);
        TileMe.setTileModeX(Shader.TileMode.REPEAT);
        TileMe.setTileModeY(Shader.TileMode.REPEAT);
        Point size = new Point();
        context.getWindowManager().getDefaultDisplay().getRealSize(size);
        Bitmap wallBitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
        Canvas wallCanvas = new Canvas(wallBitmap);
        TileMe.setBounds(0, 0, size.x, size.y);
        TileMe.draw(wallCanvas);

        return wallBitmap;
    }
}
