package com.avicooper.googleglasscps;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CameraActivity extends Activity implements SurfaceHolder.Callback {

    private SurfaceView mPreview;
    private Camera mCamera;
    boolean stop = false;
    public final Handler handler = new Handler();
    SurfaceHolder holder;
    TextView mCounter;
    ImageView imgView;
    public ConnectedThread BTConnected = MainActivity.BTConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //to prevent screen from dimming
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.camera_activity);

        mPreview = (SurfaceView) findViewById(R.id.preview);
        mCounter = (TextView) findViewById(R.id.counter);
        imgView = (ImageView) findViewById(R.id.image);

        holder = mPreview.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(this);

        // Start the main thread. Image is captured when preview starts.
        Sync sync = new Sync(call, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    public class Sync {
        Runnable task;
        public Sync(Runnable task, long time) {
            this.task = task;
            for (int i = 0; i < 3; i++) {
                mCamera = getCameraInstance();
                if (mCamera != null) break;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (mCamera == null) {
                Log.d("asdf glass", "camera cannot be locked");
            }
            handler.removeCallbacks(task);
            handler.postDelayed(task, time);
        }
    }

    //Get camera
    private static Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();

            // Work around for Camera preview issues
            Camera.Parameters params = camera.getParameters();
            params.setPreviewFpsRange(30000, 30000);
            camera.setParameters(params);
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return camera;
    }

    //Thread to capture picture every 'x' seconds
    final private Runnable call = new Runnable() {
        @Override
        public void run() {

            //stop variable is false when user requests further info. No more images are taken.
            if (stop == false) {

                mCamera.startPreview();
                mCounter.setVisibility(View.VISIBLE);
                imgView.setVisibility(View.GONE);

                //Timer to take picture every 'x' seconds
                new CountDownTimer(3000, 1000) {

                    //Countdown is displayed on the view
                    public void onTick(long millisUntilFinished) {
                        mCounter.setText(Long.toString(millisUntilFinished / 1000));
                    }

                    //Image is captured after countdown and sent to server for recognition. The timer is restarted after 2 seconds
                    public void onFinish() {
                        mCounter.setVisibility(View.GONE);
                        mCamera.takePicture(null, null, mPictureCallback);
                        //Current timer thread is cancelled.
                        this.cancel();
                        handler.postDelayed(call, 2000);

                    }
                }.start();
            }
        }
    };

    //To call when picture is taken
    private final Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Log.d("asdf glass", "Picture taken!");
            mCounter.setVisibility(View.GONE);

            if (BTConnected.ready) {
                Bitmap bitmapFromData = BitmapFactory.decodeByteArray(data, 0, data.length);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                Bitmap a = Bitmap.createBitmap(bitmapFromData, bitmapFromData.getWidth() / 4, 0, bitmapFromData.getWidth() / 2, bitmapFromData.getHeight());
                a.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                byte[] bitmapData = stream.toByteArray();
                BTConnected.largeWrite(bitmapData);
            }
        }
    };

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            mCamera = getCameraInstance();
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}