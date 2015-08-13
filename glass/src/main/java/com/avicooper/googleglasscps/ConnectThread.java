package com.avicooper.googleglasscps;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {
    private BluetoothSocket mmSocket;
    private MainActivity main;
    public boolean connected = false;

    public ConnectThread(final BluetoothDevice device, final MainActivity mainBeingPassedIn) {

        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        main = mainBeingPassedIn;
        Log.d("asdf", "glass beginning Connect init");
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            Log.d("asdf", "glass trying to connect");
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("22b5256d-131c-466c-8e44-5f54565cce4a"));
        } catch (IOException ignored) {
        }
        mmSocket = tmp;
        run();
    }

    public void run() {
        Log.d("asdf", "glass trying to enter socket");

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            Log.d("asdf", "glass is socketed");

            mmSocket.connect();
            connected = true;
            Log.d("asdf", "glass connection succeeded");

            main.buildView(0, "Main view", "Tap to start camera", true);
            main.stepInProcessCounter = 2;
            Log.d("asdf", "glass will now create Connected object");
            main.BTConnected = new ConnectedThread(mmSocket);
        } catch (IOException connectException) {

            Log.d("asdf", "glass could not socket");
            Log.d("asdf", connectException.toString());

            main.buildView(0, "Main view", "Connection failed. Tap to try again", true);
            main.stepInProcessCounter = 1;

            Log.d("asdf", "glass connection failed");
            // Unable to connect; close the socket and get out
            try {
                mmSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Will cancel an in-progress connection, and close the socket
     */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }
}