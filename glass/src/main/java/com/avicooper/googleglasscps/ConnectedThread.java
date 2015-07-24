package com.avicooper.googleglasscps;

import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
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
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                //The following methods need to be implemented
                //###mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {
                break;
            }
        }
    }

    public void sendPicture(Bitmap imageBitmap) throws InterruptedException {
        ByteArrayOutputStream a = new ByteArrayOutputStream();
        try {
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, a);
        }
        catch (Exception b){
            Log.d("asdf glass", "it is failing here");
        }
        byte[] bytearray = a.toByteArray();
        try {
            largeWrite(bytearray);
        } catch (InterruptedException e) {
            Log.d("asdf glass", "failed to largeWrite image");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFile (File file){

        byte[] fileBytes = new byte[(int) file.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(fileBytes);
            fileInputStream.close();
            largeWrite(fileBytes);

        } catch (FileNotFoundException e) {
            System.out.println("File Not Found.");
            e.printStackTrace();
        }
        catch (IOException e1) {
            System.out.println("Error Reading The File.");
            e1.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
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
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    public void largeWrite(byte[] bytes) throws InterruptedException, IOException {
        Log.d("asdf glass", "will try to send largeWrite");

        String beginStreamString = new String("File size:" + String.valueOf((bytes.length / 18) + 1));
        Log.d("asdf glass", "size of stream is: " + beginStreamString);
        byte[] beginStreamBytes = new byte[20];
        for (int x = 0; x < 18; x++){
            if (x < beginStreamString.length()){
                beginStreamBytes[x + 2] = (byte) beginStreamString.charAt(x);
            }
            else{
                beginStreamBytes[x + 2] = (byte) 'a';
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
                arrayOfByteArrays[outerByteArrayCounter][0] = (byte) ((byte) (outerByteArrayCounter / (127 * 127) - 127));
                arrayOfByteArrays[outerByteArrayCounter][1] = (byte) ((byte) (outerByteArrayCounter / 127) - 127);
                arrayOfByteArrays[outerByteArrayCounter][2] = (byte) ((byte) (outerByteArrayCounter % 127) - 127);
                byteCounter += 3;
            }
            arrayOfByteArrays[outerByteArrayCounter][byteCounter % 20] = mByte;
            byteCounter++;
        }
        Log.d("asdf glass", "finished splitting up bytes. waiting for confirmation of size message");

        byte[] buffer = new byte[20];  // buffer store for the stream
        final byte[] receivedBytes = "ReceivedFileSizeMesg".getBytes();

        //Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                mmInStream.read(buffer);
                Log.d("asdf glass", "got message:");
                printOutBytesArray(buffer);
                if (buffer == receivedBytes){
                    Log.d("asdf glass", "server recieved size message");
                    break;
                }
            } catch (IOException e) {
                Log.d("asdf glass", "could not receive confirmation that server received size message.");
                return;
            }
        }

        for (byte[] byteArray: arrayOfByteArrays){
            write(byteArray);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Log.d("asdf system", "could not sleep");
            }
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}