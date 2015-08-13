
package com.avicooper.googleglasscps;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback{


    private CardScrollView mCardScroller;
    public View mainView;
    public View resultsView;
    public View cameraView;
    public int stepInProcessCounter = 0;
    private ConnectThread BTConnect;
    public static ConnectedThread BTConnected;
    final View[] views = new View[] {mainView, cameraView, resultsView};

    //For camera view
    private SurfaceView mPreview;
    private Camera mCamera;
    private boolean continueCamera = true;
    public final Handler handler = new Handler();
    SurfaceHolder holder;
    TextView mCounter;
    ImageView imgView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //to prevent screen from dimming
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        buildView(0, "Main view", "Tap to start Bluetooth connection", false);

//        cameraView = View.inflate(this, R.layout.camera_activity, null);
//        mPreview = (SurfaceView) findViewById(R.id.preview);
//        mCounter = (TextView) findViewById(R.id.counter);
//        imgView = (ImageView) findViewById(R.id.image);
//
//        holder = mPreview.getHolder();
//        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        holder.addCallback(this);
//
//        surfaceCreated(holder);

        // Start the main thread. Image is captured when preview starts.
        //Sync sync = new Sync(call, 500);
        buildView(1, "Camera view", "Camera will start immediately after Bluetooth is connected", false);

        buildView(2, "Response view", "Nothing to display. Start the camera and when something is recognized, you will be brought back here", false);

        mCardScroller = new CardScrollView(this);
        mCardScroller.setHorizontalScrollBarEnabled(false);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return views.length;
            }

            @Override
            public Object getItem(int position) {
                if (position == 0) {
                    return mainView;
                } else if (position == 1) {
                    return cameraView;
                }
                else if (position == 2){
                    return resultsView;
                }
                return null;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return views[position];
            }

            @Override
            public int getPosition(Object item) {
                if (mainView.equals(item)) {
                    return 0;
                }
                else if (cameraView.equals(item)){
                    return 1;
                }
                else if (resultsView.equals(item)){
                    return 2;
                }
                return AdapterView.INVALID_POSITION;
            }
        });

        setContentView(mCardScroller);


        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //if (mCardScroller.getSelectedItemPosition() == 0) {
                    switch (stepInProcessCounter) {
                        case 0:
                            establishBTConnection();
                            break;
                        case 1:
                            establishBTConnection();
                            break;
                        case 2:
                            //Intent i = new Intent(getBaseContext(), CameraActivity.class);
                            //startActivity(i);
                            buildCameraView();
                            break;
                        default:
                            Log.d("asdf glass", "User tapped when no commands were able to be recieved.");
                            break;
                    }
                //}
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCardScroller.deactivate();
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

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    private void establishBTConnection() {
        buildView(0, "", "Creating connection", true);
        //White Samsung S2
        //BTConnect = new ConnectThread(BluetoothAdapter.getDefaultAdapter().getRemoteDevice("14:89:FD:33:1F:97"), this);

        //Black Samsung S2
        BTConnect = new ConnectThread(BluetoothAdapter.getDefaultAdapter().getRemoteDevice("9C:3A:AF:6B:ED:B1"), this);
    }

    public void buildView (int viewPosition, String footnote, String text, boolean updateCardScroller){
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        card.setText(text);
        card.setFootnote(footnote);
        views[viewPosition] = card.getView();
        if (updateCardScroller){
            mCardScroller.getAdapter().notifyDataSetChanged();
        }
    }

    public void buildView (int viewPosition, String footnote, String text, Bitmap[] images, boolean updateCardScroller){
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        card.setText(text);
        card.setFootnote(footnote);
        for (Bitmap image : images){
            card.addImage(image);
        }
        views[viewPosition] = card.getView();
        if (updateCardScroller){
            mCardScroller.getAdapter().notifyDataSetChanged();
        }
    }

    //Camera methods

    public void buildCameraView(){

        //cameraView = View.inflate(this, R.layout.camera_activity, null);
        Log.d("asdf glass", "getting to here");
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        //card.setEmbeddedLayout(R.layout.camera_activity);
        card.setFootnote("Live view");
        cameraView = card.getView();
        mCardScroller.getAdapter().notifyDataSetChanged();
//
//        mPreview = (SurfaceView) findViewById(R.id.preview);
//        mCounter = (TextView) findViewById(R.id.counter);
//        imgView = (ImageView) findViewById(R.id.image);
//        RemoteViews a = card.getRemoteViews();
//        a.addView(R.id.preview, mPreview);
//

//        mPreview = (SurfaceView) card.getView().findViewById(R.id.preview);
//        mCounter = (TextView) card.getView().findViewById(R.id.counter);
//        imgView = (ImageView) card.getView().findViewById(R.id.image);

//        holder = mPreview.getHolder();
//        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        holder.addCallback(this);

        // Start the main thread. Image is captured when preview starts.
        //Sync sync = new Sync(call, 500);
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
                buildView(0, "Main view", "The camera does not seem to be working", true);
                return;
            }
            else{
                handler.removeCallbacks(task);
                handler.postDelayed(task, time);
            }
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
            if (camera == null){
                Log.d("asdf glass", "here2");
            }
            else{
                Log.d("asdf glass", "not here");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return camera;
    }

    //Thread to capture picture every 'x' seconds
    //Thread to capture picture every 'x' seconds
    final private Runnable call = new Runnable() {
        @Override
        public void run() {

            //stop variable is false when user requests further info. No more images are taken.
            if (continueCamera) {

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
        //For some reason this does not seem to be called so the code is copied elsewhere
        Log.d("asdf glass", "this is being called");
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