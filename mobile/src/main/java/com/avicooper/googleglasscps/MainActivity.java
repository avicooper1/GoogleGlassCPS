package com.avicooper.googleglasscps;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.example.avicooper.googleglasscps.R;

import java.io.ByteArrayOutputStream;


public class MainActivity extends Activity {

    public final static String EXTRA_MESSAGE = "com.avicooper.googleglasscps.MESSAGE";

    AcceptThread BTAccept = new AcceptThread(this);
    public ConnectedThread BTConnected;
    ImageView mImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("asdf", "began main");
        setContentView(R.layout.activity_main);
        //BTConnected = new ConnectedThread();

//        mImg = (ImageView) findViewById(R.id.received_image);
//        Log.d("asdf mobile", "starting array allocation");
//        long beginTime = System.nanoTime();
//        byte[][] testArray = new byte[10][20];
//        long endTime = System.nanoTime();
//        Log.d("asdf mobile", "finished allocation with time: " + String.valueOf((endTime - beginTime) / 1000000.0) + "" +
//                " ms.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Drawable drawable = getResources().getDrawable(R.drawable.forest);
        Log.d("asdf mobile", "should be displaying received message");
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 0, stream);
        byte[] bitmapdata = stream.toByteArray();
        outputMessage(bitmapdata, "image");

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void outputMessage(byte[] message, String typeOfMessage){
        Log.d("asdf mobile", "outputMessage() called");
        Intent intent = new Intent(this, ReceivedMessageActivity.class);
        intent.putExtra("methodToCall", typeOfMessage);
        intent.putExtra("byte array", message);
        startActivity(intent);
    }
}
