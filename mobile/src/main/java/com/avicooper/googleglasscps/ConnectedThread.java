package com.avicooper.googleglasscps;


import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private MainActivity main;

    public ConnectedThread(BluetoothSocket socket, MainActivity mainPassedIn) {
        mmSocket = socket;
        main = mainPassedIn;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        Log.d("asdf mobile", "began Connected init");

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            Log.d("asdf mobile", "trying to create streams");
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        Log.d("asdf mobile", "succeeded");
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        start();
    }

    int counter = 0;
    final private byte[] firstMessageHeader = "File size:".getBytes();
    byte[][] aggregatedBuffer;
    int readCounter = 0;
    int sizeOfIncomingData;
    boolean alreadyReceivedSize = false;

    public void run() {

        byte[] buffer = new byte[20];  // buffer store for the stream
        Log.d("asdf mobile", "listning for messages");

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                mmInStream.read(buffer);
                if (!alreadyReceivedSize) {
                    if (Arrays.equals(Arrays.copyOfRange(buffer, 2, 12), firstMessageHeader)) {
                        int counter = 12;
                        while (buffer[counter] != 'a'){
                            Log.d("asdf mobile", "getting size: " + String.valueOf(counter));
                            counter++;
                        }
                        sizeOfIncomingData = Integer.valueOf(new String(Arrays.copyOfRange(buffer, 12, counter)));
                        aggregatedBuffer = new byte[sizeOfIncomingData][20];
                        alreadyReceivedSize = true;
                        write("ReceivedFileSizeMesg".getBytes());
                    }
                }
                else{
                    int arrayIndex = (int) buffer[0] * 256 + (int) buffer[1] - 1;
                    aggregatedBuffer[arrayIndex] = buffer.clone();
                    readCounter++;
                    printOutBytesArray(aggregatedBuffer);
                    if (readCounter >= sizeOfIncomingData){
                        byte[] singleByteArray = new byte[sizeOfIncomingData * 18];
                        int totalBytesCounter = 0;
                        for (byte[] byteArray : aggregatedBuffer){
                            int innerBytesCounter = 0;
                            for (byte mByte : byteArray){
                                if (innerBytesCounter != 0 && innerBytesCounter != 1){
                                    singleByteArray[totalBytesCounter] = mByte;
                                    totalBytesCounter++;
                                }
                                innerBytesCounter++;
                            }
                        }
                        printOutBytesArray(singleByteArray);
//                        main.outputMessage(singleByteArray, "image");
                    }
                }
            } catch (IOException e) {
                break;
            }
        }
    }

    private void printOutBytesArray (byte[] bytes){
        Log.d("asdf system", "bytes array is: " + new String(bytes));
    }

    private void printOutBytesArray (byte[][] doubleBytesArray){
        Log.d("asdf system", "printing out byte array.");
        for (byte[] bytesArray :  doubleBytesArray){
            Log.d("asdf system", "bytes array is: " + new String(bytesArray));
        }
        Log.d("asdf system", "done printing out byte array.");
    }


    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}