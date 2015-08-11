package com.avicooper.googleglasscps;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Paint.Align;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.Image;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONObject;

import com.google.android.glass.content.Intents;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "asdf mobile";
    private SurfaceView mPreview;
    private Camera mCamera;
    private GestureDetector mGestureDetector = null;
    static JSONObject jsonResponse;
    boolean stop = false;
    public final Handler handler = new Handler();
    SurfaceHolder holder;
    private String fileName;
    TextView mCounter;
    ImageView imgView;
    private Paint rectPaint;
    private Paint textPaint;
    private Paint background;
    Bitmap bitmap;
    private ConnectThread BTConnect;
    public ConnectedThread BTConnected;
    private View mView;
    private CardScrollView mCardScroller;

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

        //Set Paint objects
        setRectPaint();
        setTextPaint();
        setBackground();

        //to detect gestures
        //mGestureDetector = createGestureDetector(this);

        //To safely open camera

        // Start the main thread. Image is captured when preview starts.
        Sync sync = new Sync(call, 500);

        //White Samsung S2
        //BTConnect = new ConnectThread(BluetoothAdapter.getDefaultAdapter().getRemoteDevice("14:89:FD:33:1F:97"), this);

        //Black Samsung S2
        BTConnect = new ConnectThread(BluetoothAdapter.getDefaultAdapter().getRemoteDevice("9C:3A:AF:6B:ED:B1"), this);
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
                debug("Camera cannot be locked");
            }
            handler.removeCallbacks(task);
            handler.postDelayed(task, time);
        }
    }

    /**
     * Thread to capture image every 10s
     */
    final private Runnable call = new Runnable() {
        @Override
        public void run() {

            //stop variable is false when user requests further info. No more images are taken.
            if (stop == false) {

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Timer to take images every 10s
                new CountDownTimer(3000, 1000) {

                    //Countdown is displayed on the view
                    public void onTick(long millisUntilFinished) {

                        mCamera.startPreview();
                        mCounter.setVisibility(View.VISIBLE);
                        mCounter.setText(Long.toString(millisUntilFinished / 1000));
                        jsonResponse = null;
                        imgView.setVisibility(View.GONE);
                    }

                    //Image is captured after countdown and sent to server for recognition. The timer restarts after 8s
                    public void onFinish() {
                        mCounter.setVisibility(View.GONE);
                        mCamera.takePicture(null, null, mPictureCallback);
                        //Current timer thread is cancelled.
                        this.cancel();
                        handler.postDelayed(call, 10000);

                    }
                }.start();
            }
        }
    };

    /**
     * To get camera instance
     *
     * @return Camera
     */
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

    /*
     * Sets Paint object to draw rectangles.
     */
    public void setRectPaint() {
        rectPaint = new Paint();
        rectPaint.setColor(Color.GREEN);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(5);
    }

    /*
     * Sets Paint object for annotations
     */
    public void setTextPaint() {
        textPaint = new Paint();
        textPaint.setColor(Color.GREEN);
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(60);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    /*
     * Sets Paint object to draw rectangles for text background.
     */
    public void setBackground() {
        background = new Paint();
        background.setColor(Color.BLACK);
        background.setStyle(Paint.Style.FILL);
        background.setStrokeWidth(5);
    }


    /*
     * Callback when the picture is taken
     */
    private final Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Log.d(TAG, "Picture taken!");
            mCounter.setVisibility(View.GONE);

            if (BTConnect.connected && BTConnected.ready){
                Bitmap bitmapFromData = BitmapFactory.decodeByteArray(data, 0, data.length);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                Bitmap a = Bitmap.createBitmap(bitmapFromData, bitmapFromData.getWidth() / 4, 0, bitmapFromData.getWidth() / 2, bitmapFromData.getHeight());
                //Bitmap b = Bitmap.createScaledBitmap(a, a.getWidth() / 2, a.getHeight() / 2, false);
                a.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                byte[] bitmapData = stream.toByteArray();
                BTConnected.largeWrite(bitmapData);
                //BTConnected.largeWrite("This is to test a really long write. I need to understand the entire string so I'm making it relatively short.".getBytes());
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

    /*
     * Send generic motion events to the gesture detector
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }

    private void debug(String message) {
        Log.d(TAG, message);
    }
}