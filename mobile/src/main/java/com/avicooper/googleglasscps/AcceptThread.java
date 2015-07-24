package com.avicooper.googleglasscps;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class AcceptThread extends Thread {
    private final BluetoothServerSocket mmServerSocket;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final MainActivity main;

    public AcceptThread(MainActivity activityPassedIn) {
        Log.d("asdf mobile", "began Accept init");
        main = activityPassedIn;
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        try {
            Log.d("asdf mobile", "trying to create server");
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Google Glass CPS", UUID.fromString("22b5256d-131c-466c-8e44-5f54565cce4a"));
            Log.d("asdf", "succeeded");
        } catch (IOException e) { }
        Log.d("asdf mobile", "suceeded");
        mmServerSocket = tmp;
        start();
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                Log.d("asdf mobile", "trying to connect client");
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.d("asdf mobile", "failed");
                Log.d("asdf mobile", e.toString());
                break;
            }
            Log.d("asdf mobile", "connection succeeded");
            // If a connection was accepted
            if (socket != null) {
                Log.d("asdf mobile", "succeeded");
                // Do work to manage the connection (in a separate thread)
                //The following method needs to be implemented
                main.BTConnected = new ConnectedThread(socket, main);
                main.BTConnected.run();
                try {
                    mmServerSocket.close();
                } catch (IOException e) { }
                break;
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) { }
    }
}