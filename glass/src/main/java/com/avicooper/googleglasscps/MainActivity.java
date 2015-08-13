
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

public class MainActivity extends Activity {


    private CardScrollView mCardScroller;
    public View mainView;
    public View resultsView;
    public View cameraView;
    public int stepInProcessCounter = 0;
    private ConnectThread BTConnect;
    public static ConnectedThread BTConnected;
    final View[] views = new View[] {mainView, cameraView, resultsView};

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        buildView(0, "Main view", "Tap to start Bluetooth connection", false);

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
                Log.d("adsf glass", "getting called");
                switch (stepInProcessCounter) {
                    case 0: establishBTConnection();
                        break;
                    case 1:
                        establishBTConnection();
                        break;
                    case 2:
                        Intent i = new Intent(getBaseContext(), CameraActivity.class);
                        startActivity(i);
                        break;
                    default:
                        Log.d("asdf glass", "User tapped when no commands were able to be recieved.");
                        break;
                }
            }
        });
    }

    private void establishBTConnection() {
        buildView(0, "", "Creating connection", true);
        //White Samsung S2
        //BTConnect = new ConnectThread(BluetoothAdapter.getDefaultAdapter().getRemoteDevice("14:89:FD:33:1F:97"), this);

        //Black Samsung S2
        BTConnect = new ConnectThread(BluetoothAdapter.getDefaultAdapter().getRemoteDevice("9C:3A:AF:6B:ED:B1"), this);
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

}