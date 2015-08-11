
package com.avicooper.googleglasscps;

        import android.app.Activity;
        import android.bluetooth.BluetoothAdapter;
        import android.content.ComponentName;
        import android.content.Intent;
        import android.graphics.Bitmap;
        import android.graphics.drawable.BitmapDrawable;
        import android.graphics.drawable.Drawable;
        import android.hardware.Camera;
        import android.net.Uri;
        import android.os.Bundle;
        import android.os.Environment;
        import android.os.FileObserver;
        import android.provider.MediaStore;
        import android.util.Log;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.AdapterView;

        import com.google.android.glass.content.Intents;
        import com.google.android.glass.widget.CardBuilder;
        import com.google.android.glass.widget.CardScrollAdapter;
        import com.google.android.glass.widget.CardScrollView;

        import java.io.ByteArrayOutputStream;
        import java.io.File;
        import java.io.FileNotFoundException;
        import java.io.FileOutputStream;
        import java.io.IOException;
        import java.lang.ref.SoftReference;
        import java.text.SimpleDateFormat;
        import java.util.Date;

/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p/>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class OldMainActivity extends Activity {

    /**
     * {@link CardScrollView} to use as the main content view.
     */
    private CardScrollView mCardScroller;

    /**
     * "Hello World!" {@link View} generated by {@link #buildView()}.
     */
    private View mView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d("asdf", "creating view");
        mView = buildView();
        // create a file to save the image
        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public Object getItem(int position) {
                return mView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mView;
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });

        //BTConnect = new ConnectThread(BluetoothAdapter.getDefaultAdapter().getRemoteDevice("14:89:FD:33:1F:97"), this);

        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 10);
            }
        });
        setContentView(mCardScroller);
    }

    private ConnectThread BTConnect;
    public ConnectedThread BTConnected;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            //BTConnected.largeWrite("Initially Tesla planned to start production by the end of 2013 and for deliveries to commence in 2014.[8] However, in February 2013, the company announced that production had been rescheduled to begin by late 2014 in order to focus \"on a commitment to bring profitability to the company in 2013\" and to achieve its production target of 20,000 Model S cars in 2013.[4][5][9] As of March 2013, Tesla's production target for the Model X was between 10,000 to 15,000 cars a year.[5] The prototype Tesla Model X had no side mirrors, instead having small cameras mounted on each side that displayed on the dashboard, but U.S. safety regulations were not ready, and the cameras were replaced with mirrors.[10][11] In November 2013, Tesla said it expected to deliver the Model X in small numbers by end of 2014, with high volume production planned for the second quarter of 2015.[6] However, Tesla announced in February 2014 that in order to focus on overseas rollouts, the company planned to have production design Model X prototypes by the end of 2014 and would begin high volume deliveries for retail customers in the second quarter of 2015.[12] In November 2014 Tesla again delayed the start of deliveries to retail customers, and announced that Model X deliveries would begin in the third quarter of 2015,[1] while deliveries for new reservations are to begin in early 2016.[13] Among the reasons for delay are problems with the gullwing doors and cooling the motors when hauling trailers.[11]".getBytes());
            //BTConnected.largeWrite("This is to test a really long write. I need to understand the entire string so I'm making it relatively short.".getBytes());
            String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);
            processPictureWhenReady(picturePath);
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
            a.compress(Bitmap.CompressFormat.JPEG, 50, stream);
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
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link CardBuilder} class.
     */
    private View buildView() {
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        card.setText("Establishing connection");
        return card.getView();
    }

    public void buildViewWithText (String newText){
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        card.setText(newText);
        mView = card.getView();
        mCardScroller.getAdapter().notifyDataSetChanged();
    }

}