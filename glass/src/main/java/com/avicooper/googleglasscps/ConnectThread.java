package com.avicooper.googleglasscps;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final MainActivity main;

    public ConnectThread(BluetoothDevice device, MainActivity mainBeingPassedIn) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;
        main = mainBeingPassedIn;
        Log.d("asdf", "glass beginning Connect init");
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            Log.d("asdf", "glass trying to connect");
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("22b5256d-131c-466c-8e44-5f54565cce4a"));
        } catch (IOException e) {
            Log.d("asdf", "glass connection failed");
        }
        mmSocket = tmp;
        Log.d("asdf", "glass connection succeeded");
        run();
    }

    public void run() {
        Log.d("asdf", "glass trying to enter socket");

        // Cancel discovery because it will slow down the connection
        //The following is provided on the Google API page but seems to be crashing the app
        //This requires more research
        //mBluetoothAdapter.cancelDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            Log.d("asdf", "glass is socketed");
            mmSocket.connect();
        } catch (IOException connectException) {
            Log.d("asdf", "glass could not socket");
            Log.d("asdf", connectException.toString());
            // Unable to connect; close the socket and get out
            try {
                mmSocket.close();
            } catch (IOException closeException) {}
            return;
        }

        // Do work to manage the connection (in a separate thread)
        Log.d("asdf", "glass will now create Connected object");
        main.BTConnected = new ConnectedThread(mmSocket);
        //main.buildViewWithText("Connection esatblished. Tap to take a picture");
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}