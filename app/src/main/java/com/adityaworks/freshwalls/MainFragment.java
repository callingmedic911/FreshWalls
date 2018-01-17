package com.adityaworks.freshwalls;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.evgenii.jsevaluator.JsEvaluator;
import com.evgenii.jsevaluator.interfaces.JsCallback;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.takusemba.spotlight.SimpleTarget;
import com.takusemba.spotlight.Spotlight;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;


/**
 * Created by callingmedic911 on 26-Dec-17.
 */

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getSimpleName();
    private WallpaperManager wallpaperManager;
    private ObjectAnimator rotation;
    private FloatingActionButton floatingActionButton;
    private Bitmap newWall;
    private String randomBlock = new RandomString(new Random().nextInt(50) + 1).nextString();
    private int wallColor = 0;
    View colorBlock;
    TextView colorHex;

    private String LOG_TAG = MainFragment.class.getSimpleName();

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Set views
        final View view = getView();
        floatingActionButton = view.findViewById(R.id.floatingActionButton);
        final Button setWallpaper = view.findViewById(R.id.set_wall);
        final LinearLayout colorPicker = view.findViewById(R.id.color_picker);
        colorBlock = view.findViewById(R.id.color_block);
        colorHex = view.findViewById(R.id.color_hex);

        //First time user: Show tutorial
        SharedPreferences sharedPref = getActivity().getSharedPreferences("FreshWallPrefs", Context.MODE_PRIVATE);
        if (sharedPref.getBoolean("initial_launch", true)) {
            final Activity activity = getActivity();

            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override public void onGlobalLayout() {

                    final SimpleTarget firstTarget =
                            new SimpleTarget.Builder(activity).setPoint(floatingActionButton)
                                    .setRadius(168f)
                                    .setTitle("Refresh")
                                    .setDescription("Generate new wallpaper based on random pattern & color.")
                                    .build();

                    final SimpleTarget secondTarget =
                            new SimpleTarget.Builder(activity).setPoint(colorPicker)
                                    .setRadius(168f)
                                    .setTitle("Set Color")
                                    .setDescription("You can select specific color for currently generated wallpaper.")
                                    .build();
                    final SimpleTarget thirdTarget =
                            new SimpleTarget.Builder(activity).setPoint(setWallpaper)
                                    .setRadius(168f)
                                    .setTitle("Set Wallpaper")
                                    .setDescription("Set currently generated wallpaper as your home screen wallpaper.")
                                    .build();

                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    Spotlight.with(getActivity())
                            .setDuration(500L)
                            .setAnimation(new DecelerateInterpolator(2f))
                            .setTargets(firstTarget, secondTarget, thirdTarget)
                            .start();
                }
            });

            sharedPref.edit().putBoolean("initial_launch", false).apply();
        }

        //Current background as current wallpaper
        wallpaperManager = WallpaperManager.getInstance(getActivity());
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        view.findViewById(R.id.output).setBackground(wallpaperDrawable);

        final ColorPickerDialog colorPickerDialog = ColorPickerDialog.newBuilder()
                .setDialogId(0)
                .setShowAlphaSlider(false)
                .create();

        colorPickerDialog.setColorPickerDialogListener(new ColorPickerDialogListener() {
            @Override
            public void onColorSelected(int dialogId, int color) {
                selectedColor(color);
            }

            @Override
            public void onDialogDismissed(int dialogId) {

            }
        });

        //Find new wallpaper on start
        refreshWall();

        //Change color when new color is selected
        colorPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                colorPickerDialog.show(getActivity().getFragmentManager(),"tag");
            }
        });

        //Refresh wallpaper when "Refresh"-ed
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshWall();
            }
        });

        //Set wallpaper on "Set"
        setWallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (newWall != null)
                    setNewWallpaper(newWall);
                getActivity().moveTaskToBack(true);
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rotation.removeAllListeners();
    }

    private void refreshWall(boolean updateWall) {
        if (rotation != null)
            rotation.setRepeatCount(0);
        floatingActionButton.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        rotation = ObjectAnimator.ofFloat(floatingActionButton,"rotation", 0f, 360f);
        rotation.setInterpolator(new FastOutSlowInInterpolator());
        rotation.setDuration(700);
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        rotation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                floatingActionButton.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
        rotation.start();
        findNewWallpaper(updateWall);
    }

    private void refreshWall() {
        wallColor = 0;
        colorBlock.setBackgroundColor(Color.parseColor("#eeeeee"));
        colorHex.setText("Color");
        refreshWall(true);
    }

    private void findNewWallpaper(boolean updateWall) {

        final String geoPattern = loadGeoPattern(getActivity());
        if (updateWall)
            randomBlock = new RandomString(new Random().nextInt(50) + 1).nextString();
        Log.v(LOG_TAG, "Random Block is " + randomBlock);
        JsEvaluator jsEvaluator = new JsEvaluator(getActivity());
        String option = "var pattern = GeoPattern.generate('" + randomBlock + "');";

        if (wallColor != 0 )
            option = "var pattern = GeoPattern.generate('" + randomBlock + "',{ color:'" + String.format("#%06X", (0xFFFFFF & wallColor)) + "'});";

        jsEvaluator.callFunction(geoPattern + option, new JsCallback() {
            @Override
            public void onResult(String s) {
                new GenerateWallpaper(getActivity(), getView(), new OnGenerationComplete() {
                    @Override
                    public void onGenerationComplete(Bitmap bitmap) {
                        rotation.setRepeatCount(0);
                        newWall = bitmap;
                    }
                }).execute(s);
            }

            @Override
            public void onError(String s) {
                Log.e(TAG, "failure");
            }
        },"pattern.toString");
    }

    public static String loadGeoPattern(Activity context) {
        AssetManager assetManager = context.getAssets();
        InputStream inStream;
        try {
            inStream = assetManager.open("geopattern.js");
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            inStream.close();
            return result.toString("UTF-8");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setNewWallpaper(Bitmap wall) {
        try {
            wallpaperManager.setBitmap(wall);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void selectedColor(int color) {
        wallColor = color;
        colorBlock.setBackgroundColor(color);
        colorHex.setText(String.format("#%06X", (0xFFFFFF & color)));
        refreshWall(false);
    }
}

