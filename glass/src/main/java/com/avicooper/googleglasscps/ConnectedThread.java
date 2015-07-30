package com.avicooper.googleglasscps;

import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        FileOutputStream tmpFile = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[20];  // buffer store for the stream

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                mmInStream.read(buffer);
            } catch (IOException e) {
                break;
            }
        }
    }

    public void sendString (String stringToSend) throws IOException, InterruptedException {
        if (stringToSend.length() <= 20){
            write(stringToSend.getBytes());
        }
        else{
            largeWrite(stringToSend.getBytes());
        }
    }

    private void printOutBytesArray(byte[] bytes){
        Log.d("asdf system", "bytes array is: " + new String(bytes));
    }

    private void printOutBytesArray (byte[][] doubleBytesArray){
        Log.d("asdf system", "printing out byte array.");
        for (byte[] bytesArray :  doubleBytesArray){
            Log.d("asdf system", "bytes array is: " + new String(bytesArray));
        }
        Log.d("asdf system", "done printing out byte array.");
    }

    private void printOutBytesArrayAsInts (byte[] bytes){
        StringBuilder string = new StringBuilder();
        for (byte mByte : bytes){
            string.append(" " + mByte + " ||");
        }
        Log.d("asdf glass", "bytes array is: " + string);
    }

    private void printOutBytesArrayAsInts (byte[][] doubleBytesArray){
        Log.d("asdf system", "printing out byte array.");
        for (byte[] bytesArray :  doubleBytesArray){
            StringBuilder string = new StringBuilder();
            for (byte mByte : bytesArray){
                string.append(" " + mByte + " ||");
            }
            Log.d("asdf glass", "bytes array is: " + string);
        }
        Log.d("asdf system", "done printing out byte array.");
    }


    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) throws IOException, InterruptedException {
        mmOutStream.write(bytes);
        Thread.sleep((long) 1);
    }

    private void writeWithProgressTracker(byte[][] arrayOfBytes) throws IOException, InterruptedException {
//        int counter = 0;
        final int onePercent = arrayOfBytes.length / 100;
        final long beginTime = System.nanoTime();
        for (byte[] bytes : arrayOfBytes) {
            mmOutStream.write(bytes);
            Thread.sleep((long) 1, 0);
//            if (counter % onePercent == 0){
//                Log.d("asdf glass", "finished: " + String.valueOf(counter / onePercent) + "%");
//            }
//            counter++;
        }
        final double totalTime = ((System.nanoTime() - beginTime) / 1000000000.0);
        Log.d("asdf glass", "transmission finished in " + String.valueOf(totalTime) + " seconds");
    }

    private void writeFinishedTransmission() throws IOException, InterruptedException {
        write(lastMessageNotice);
    }

    final private byte[] lastMessageNotice = "FinalMessageNotice..".getBytes();

    public void largeWrite(byte[] bytes) throws InterruptedException, IOException {

        Log.d("asdf mobile", "entire byte array: " + new String(bytes));
        Log.d("asdf glass", "will try to send largeWrite");

        String beginStreamString = "File size:" + String.valueOf((bytes.length / 17) + 1);
        byte[] beginStreamBytes = new byte[20];
        for (int x = 0; x < 20; x++) {
            if (x < beginStreamString.length()){
                beginStreamBytes[x] = (byte) beginStreamString.charAt(x);
            }
            else{
                beginStreamBytes[x] = (byte) 'a';
            }
        }
        printOutBytesArray(beginStreamBytes);
        write(beginStreamBytes);

        Log.d("asdf glass", "got to splitting up file bytes into array of arrays");

        byte[][] arrayOfByteArrays = new byte[(bytes.length / 17) + 1][20];
        int outerByteArrayCounter = 0;
        int byteCounter = 0;
        for (byte mByte : bytes){
            if (byteCounter % 20 == 0){
                if (byteCounter != 0){
                    outerByteArrayCounter++;
                }
                arrayOfByteArrays[outerByteArrayCounter][0] = (byte) (outerByteArrayCounter / (256 * 256) - 128);
                if (arrayOfByteArrays[outerByteArrayCounter][0] == -128) {
                    arrayOfByteArrays[outerByteArrayCounter][1] = (byte) ((outerByteArrayCounter / 256) - 128);
                } else {
                    arrayOfByteArrays[outerByteArrayCounter][1] = (byte) ((byte) ((outerByteArrayCounter % (outerByteArrayCounter / (256 * 256))) / 256) - 128);
                }
                arrayOfByteArrays[outerByteArrayCounter][2] = (byte) ((byte) (outerByteArrayCounter % 256) - 128);
                byteCounter += 3;
            }
            arrayOfByteArrays[outerByteArrayCounter][byteCounter % 20] = mByte;
            byteCounter++;
        }

        Log.d("asdf glass", "finished splitting up bytes. waiting for confirmation of size message");

        byte[] buffer = new byte[20];  // buffer store for the stream
        final byte[] receivedBytes = "ReceivedFileSizeMesg".getBytes();
        final byte[] missingMessage = "TheMissingArraysAre:".getBytes();

        //Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                mmInStream.read(buffer);
                if (Arrays.equals(buffer, receivedBytes)) {
                    Log.d("asdf glass", "server received size message");
                    break;
                }
            } catch (IOException e) {
                Log.d("asdf glass", "could not receive confirmation that server received size message.");
                return;
            }
        }

        writeWithProgressTracker(arrayOfByteArrays);
        writeFinishedTransmission();

//        ArrayList<Integer> arrayOfMissingPackets = new ArrayList<Integer>();
//        while (true) {
//            try {
//                Log.d("asdf glass", "a");
//                mmInStream.read(buffer);
//                printOutBytesArray(buffer);
//                if (Arrays.equals(buffer, lastMessageNotice)) {
//                    break;
//                } else {
//                    StringBuilder intAsString = new StringBuilder();
//                    int counter = 0;
//                    while ((char) buffer[counter] != 'a') {
//                        intAsString.append((char) buffer[counter]);
//                        counter++;
//                    }
//                    if (intAsString.length() > 0) {
//                        arrayOfMissingPackets.add(Integer.valueOf(String.valueOf(intAsString)));
//                    }
//                }
//            } catch (IOException e) {
//                Log.d("asdf glass", "not getting any message about lost bytes");
//                return;
//            }
//        }
//
//        Log.d("asdf glass", "missing packets are:");
//        for (int intOfArray: arrayOfMissingPackets){
//            Log.d("asdf glass", String.valueOf(intOfArray));
//            write(arrayOfByteArrays[intOfArray]);
//        }
//        writeFinishedTransmission();
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}