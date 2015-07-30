package com.avicooper.googleglasscps;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.avicooper.googleglasscps.R;


public class ReceivedMessageActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Log.d("asdf mobile", "about to show message");
        String methodToCall = intent.getStringExtra("methodToCall");
        byte[] byteArray = intent.getByteArrayExtra("byte array");
        switch (methodToCall){
            case "string": displayStringMessage(byteArray);
                break;
            case "image": displayImageMessage(byteArray);
                break;
            default: Log.d("asdf mobile", "ReceivedMessageActivity received message but type is unknown");
                break;
        }
    }

    public void displayStringMessage (byte[] bytes){
        // Create the text view
        TextView textView = new TextView(this);
        textView.setTextSize(40);
        textView.setText(new String(bytes));

        // Set the text view as the activity layout
        setContentView(textView);
    }

    public void displayImageMessage (byte[] bytes){

        // Set the text view as the activity layout
        setContentView(R.layout.activity_received_message);
        ImageView mImg;
        mImg = (ImageView) findViewById(R.id.received_image);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        mImg.setImageBitmap(bitmap);
        Log.d("asdf mobile", "hello and googbye");
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        Log.d("asdf mobile", "the size of the picture is: " + String.valueOf(height) + " by " + String.valueOf(width));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_received_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

