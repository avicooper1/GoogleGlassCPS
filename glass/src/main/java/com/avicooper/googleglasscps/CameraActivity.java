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

public class CameraActivity extends Activity implements SurfaceHolder.Callback{

	private static final String TAG = "CameraActivity";
	private SurfaceView mPreview;
	private Camera mCamera;
	private GestureDetector mGestureDetector = null;
	static JSONObject jsonResponse;
	boolean stop=false;
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
		Sync sync = new Sync(call,500);

//        mView = buildView();
//        // create a file to save the image
//        mCardScroller = new CardScrollView(this);
//        mCardScroller.setAdapter(new CardScrollAdapter() {
//            @Override
//            public int getCount() {
//                return 1;
//            }
//
//            @Override
//            public Object getItem(int position) {
//                return mView;
//            }
//
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                return mView;
//            }
//
//            @Override
//            public int getPosition(Object item) {
//                if (mView.equals(item)) {
//                    return 0;
//                }
//                return AdapterView.INVALID_POSITION;
//            }
//        });
//
//        BTConnect = new ConnectThread(BluetoothAdapter.getDefaultAdapter().getRemoteDevice("14:89:FD:33:1F:97"), this);
//
//        // Handle the TAP event.
//        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(intent, 10);
//            }
//        });
	}

	private View buildView() {
		CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
		card.setText("Establishing connection");
		return card.getView();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK){
			//BTConnected.largeWrite("Initially Tesla planned to start production by the end of 2013 and for deliveries to commence in 2014.[8] However, in February 2013, the company announced that production had been rescheduled to begin by late 2014 in order to focus \"on a commitment to bring profitability to the company in 2013\" and to achieve its production target of 20,000 Model S cars in 2013.[4][5][9] As of March 2013, Tesla's production target for the Model X was between 10,000 to 15,000 cars a year.[5] The prototype Tesla Model X had no side mirrors, instead having small cameras mounted on each side that displayed on the dashboard, but U.S. safety regulations were not ready, and the cameras were replaced with mirrors.[10][11] In November 2013, Tesla said it expected to deliver the Model X in small numbers by end of 2014, with high volume production planned for the second quarter of 2015.[6] However, Tesla announced in February 2014 that in order to focus on overseas rollouts, the company planned to have production design Model X prototypes by the end of 2014 and would begin high volume deliveries for retail customers in the second quarter of 2015.[12] In November 2014 Tesla again delayed the start of deliveries to retail customers, and announced that Model X deliveries would begin in the third quarter of 2015,[1] while deliveries for new reservations are to begin in early 2016.[13] Among the reasons for delay are problems with the gullwing doors and cooling the motors when hauling trailers.[11]".getBytes());
			String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);
			processPictureWhenReady(picturePath);
			Intent intent = new Intent(this, CameraActivity.class);
			startActivity(intent);
		}
	}

	private void processPictureWhenReady(final String picturePath) {
		final File pictureFile = new File(picturePath);

		if (pictureFile.exists()) {
			// The picture is ready; process it.
			Log.d("asdf glass", "the picture is ready to be used");
			Drawable drawable = Drawable.createFromPath(picturePath);
			Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
			ByteArrayOutputStream stream = new ByteArrayOutputStream();

			Bitmap a = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, false);
			a.compress(Bitmap.CompressFormat.JPEG, 30, stream);
			byte[] bitmapdata = stream.toByteArray();
			BTConnected.largeWrite(bitmapdata);
		} else {
			Log.d("asdf glass", "the picture is not ready to be used");
			// The file does not exist yet. Before starting the file observer, you
			// can update your UI to let the user know that the application is
			// waiting for the picture (for example, by displaying the thumbnail
			// image and a progress indicator).

			final File parentDirectory = pictureFile.getParentFile();
			FileObserver observer = new FileObserver(parentDirectory.getPath(),
					FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
				// Protect against additional pending events after CLOSE_WRITE
				// or MOVED_TO is handled.
				private boolean isFileWritten;

				@Override
				public void onEvent(int event, String path) {
					if (!isFileWritten) {
						// For safety, make sure that the file that was created in
						// the directory is actually the one that we're expecting.
						File affectedFile = new File(parentDirectory, path);
						isFileWritten = affectedFile.equals(pictureFile);

						if (isFileWritten) {
							stopWatching();

							// Now that the file is ready, recursively call
							// processPictureWhenReady again (on the UI thread).
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									processPictureWhenReady(picturePath);
								}
							});
						}
					}
				}
			};
			observer.startWatching();
		}
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
			for(int i=0; i < 3; i++)
			{
				mCamera = getCameraInstance();
				if(mCamera != null) break;
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if(mCamera == null)
			{
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

			//stop variable is false when user requests further info. No more iages are taken.
			if (stop==false) {

				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// Timer to take images every 10s
				new CountDownTimer(10000, 1000) {

					//Countdown is displayed on the view
					public void onTick(long millisUntilFinished) {

						mCamera.startPreview();
						mCounter.setVisibility(View.VISIBLE);
						mCounter.setText(Long.toString(millisUntilFinished / 1000));
						jsonResponse=null;
						imgView.setVisibility(View.GONE);
					}

					//Image is captured after countdown and sent to server for recognition. The timer restarts after 8s
					public void onFinish() {
						mCounter.setVisibility(View.GONE);
						mCamera.takePicture(null, null, mPictureCallback);
						//Current timer thread is cancelled.
						this.cancel();
						handler.postDelayed(call,7*1000);

					}
				}.start();
			}
		}
	};

	/**
	 * To get camera instance
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
		rectPaint=new Paint();
		rectPaint.setColor(Color.GREEN);
		rectPaint.setStyle(Paint.Style.STROKE);
		rectPaint.setStrokeWidth(5);
	}

	/*
     * Sets Paint object for annotations
     */
	public void setTextPaint() {
		textPaint=new Paint();
		textPaint.setColor(Color.GREEN);
		textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		textPaint.setTextSize(60);
		textPaint.setTextAlign(Paint.Align.CENTER);
	}

	/*
     * Sets Paint object to draw rectangles for text background.
     */
	public void setBackground() {
		background=new Paint();
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
			fileName = Environment.getExternalStorageDirectory().getPath()+"/picture.jpg"; //saved with filename picture
			FileOutputStream imageFileOS;
			try {
				imageFileOS = new FileOutputStream(fileName);
				imageFileOS.write(data);
				imageFileOS.flush();
				imageFileOS.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getPath() + "/picture.jpg")
					.copy(Bitmap.Config.ARGB_8888, true);
			//AsyncTask method to access server.
			//new LongOperation().execute(fileName);

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

	private void debug(String message){
		Log.d(TAG, message);
	}
}