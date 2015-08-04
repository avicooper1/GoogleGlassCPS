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

    //Class methods
    public void run() {
        byte[] buffer = new byte[20];
        while (true) {
            try {
                mmInStream.read(buffer);
            } catch (IOException e) {
                break;
            }
        }
    }

    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep((long) 0, 100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }



    final private byte[] firstMessageHeader = "File size:".getBytes();
    final private byte[] lastMessageNotice = "FinalMessageNotice..".getBytes();
    final private byte[] receivedMessageNotice = "ReceivedMessageCont.".getBytes();
    final private byte[] secondPacketsSendAttemptNotice = "SecondPacketsSend...".getBytes();
    final private byte[] noMissingPacketsNotice = "NoMoreMissingPackets".getBytes();
    final private byte[] clearBuffer = "aaaaaaaaaaaaaaaaaaaa".getBytes();
    final private byte[] emptyByteArray = new byte[20];
    byte[][] aggregatedBuffer;
    boolean waitingForCommand = true;



    //My this class methods
    public void sendString (String stringToSend){
        if (stringToSend.length() <= 20){
            write(stringToSend.getBytes());
        }
        else{
            largeWrite(stringToSend.getBytes());
        }
    }

    public void largeWrite(byte[] bytes){

        boolean fileSizeNeedsExtraEndPacket;

        if (bytes.length % 18 == 0){
            fileSizeNeedsExtraEndPacket = false;
        }
        else{
            fileSizeNeedsExtraEndPacket = true;
        }

        byte[] fileSizeByteArrayBeginner;
        if (fileSizeNeedsExtraEndPacket){
            fileSizeByteArrayBeginner = ("File size:" + String.valueOf(((bytes.length / 18) + 1))).getBytes();

        }
        else{
            fileSizeByteArrayBeginner = ("File size:" + String.valueOf((bytes.length / 18))).getBytes();
        }

        writeInitialMessage(fileSizeByteArrayBeginner);

        ArrayList<byte[]> aggregatedByteArrays = new ArrayList<>();
        byte[] tempByteArray = new byte[18];
        boolean tempBytesArrayStored = true;

        for (int x = 0; x < bytes.length; x++){
            tempByteArray[x % 18] = bytes[x];
            tempBytesArrayStored = false;
            if (x != 0 && x % 18 == 17){
                aggregatedByteArrays.add(headersAndInts(x / 18, tempByteArray));
                tempByteArray = Arrays.copyOfRange(emptyByteArray, 0, 18);
                tempBytesArrayStored = true;
            }
        }

        if (!tempBytesArrayStored){
            aggregatedByteArrays.add(headersAndInts((bytes.length / 18), tempByteArray));
        }

        byte[] buffer = new byte[20];  // buffer store for the stream

        while (true) {
            try {
                mmInStream.read(buffer);
                if (Arrays.equals(buffer, receivedMessageNotice)) {
                    break;
                }
            } catch (IOException e) {
                Log.d("asdf glass", "could not receive confirmation that server received size message.");
                return;
            }
        }

        writeWithProgressTracker(aggregatedByteArrays);

        ArrayList<Integer> arrayOfMissingPackets = new ArrayList<Integer>();
        while (true) {
            try {
                mmInStream.read(buffer);
            } catch (IOException e) {
                Log.d("asdf glass", "not getting any message about lost bytes");
                break;
            }

            if (waitingForCommand){
                if(Arrays.equals(buffer, secondPacketsSendAttemptNotice)){
                    waitingForCommand = false;
                    continue;
                }
                else if (Arrays.equals(buffer, noMissingPacketsNotice)){
                    break;
                }
            }
            if (!waitingForCommand){
                if (Arrays.equals(buffer, lastMessageNotice)) {
                    waitingForCommand = true;
                    Log.d("asdf glass", "about to send packets again");
                    write(secondPacketsSendAttemptNotice);
                    for (int intOfArray: arrayOfMissingPackets){
                        write(aggregatedByteArrays.get(intOfArray));
                    }
                    writeFinishedTransmission();
                } else {
                    arrayOfMissingPackets.add(findIntsInFilledBuffer(buffer));
                }
            }
        }
    }

    //This class methods
    void writeInitialMessage (byte[] initialMessage){
        byte[] fileSizeByteArray = new byte[20];
        for (int x = 0; x < initialMessage.length; x++){
            fileSizeByteArray[x] = initialMessage[x];
        }

        for (int x = initialMessage.length; x < 20; x++){
            fileSizeByteArray[x] = (byte) 'a';
        }
        printOutBytesArray(fileSizeByteArray);
        write(fileSizeByteArray);
    }

    //My secondary methods
    private void printOutBytesArray(byte[] bytes) {
        Log.d("asdf system", "bytes array is: " + new String(bytes));
    }

    private void printOutBytesArray(byte[][] doubleBytesArray) {
        Log.d("asdf system", "printing out byte array.");
        for (byte[] bytesArray : doubleBytesArray) {
            Log.d("asdf system", "bytes array is: " + new String(bytesArray));
        }
        Log.d("asdf system", "done printing out byte array.");
    }

    private void printOutBytesArray(ArrayList<byte[]> doubleBytesArrays) {
        Log.d("asdf system", "printing out byte array.");
        for (byte[] bytesArray : doubleBytesArrays) {
            Log.d("asdf system", "bytes array is: " + new String(bytesArray));
        }
        Log.d("asdf system", "done printing out byte array.");
    }

    private void printOutBytesArrayAsInts(byte[] bytes) {
        StringBuilder string = new StringBuilder();
        for (byte mByte : bytes) {
            string.append(" " + mByte + " ||");
        }
        Log.d("asdf glass", "bytes array is: " + string);
    }

    private void printOutBytesArrayAsInts(byte[][] doubleBytesArray) {
        Log.d("asdf system", "printing out byte array.");
        for (byte[] bytesArray : doubleBytesArray) {
            StringBuilder string = new StringBuilder();
            for (byte mByte : bytesArray) {
                string.append(" " + mByte + " ||");
            }
            Log.d("asdf glass", "bytes array is: " + string);
        }
        Log.d("asdf system", "done printing out byte array.");
    }

    private int headersAndInts(byte[] byteArray) {
        final int firstNumber = (((int) byteArray[0] + 128) * 256);
        final int secondNumber = ((int) byteArray[1] + 128);
        return firstNumber + secondNumber;
    }

    private byte[] headersAndIntsWithHeaderStripped(byte[] byteArray) {
        return Arrays.copyOfRange(byteArray, 2, byteArray.length);
    }

    private byte[] headersAndInts(int headerInt) {
        byte[] headerArray = new byte[2];
        headerArray[0] = (byte) ((byte) (headerInt / 256) - 128);
        headerArray[1] = (byte) ((byte) (headerInt % 256) - 128);
        return headerArray;
    }

    private byte[] headersAndInts(int header, byte[] bytes) {
        byte[] byteArray = new byte[20];
        int counter = 0;
        for (byte mByte : headersAndInts(header)) {
            byteArray[counter] = mByte;
            counter++;
        }
        for (byte mByte : bytes) {
            byteArray[counter] = mByte;
            counter++;
        }
        return byteArray;
    }

    private int findIntsInFilledBuffer(byte[] byteArray) {
        ArrayList<Integer> intsOfArray = new ArrayList<>();
        for (byte mByte : byteArray) {
            if ((char) mByte != 'a') {
                intsOfArray.add(Character.getNumericValue(mByte));
            } else {
                break;
            }
        }

        int intValueToReurn = 0;
        for (int mInt : intsOfArray){
            intValueToReurn *= 10;
            intValueToReurn += mInt;
        }

        return intValueToReurn;
    }

    private byte[] putIntsInFilledBuffer(int intsToPut) {
        byte[] byteArray = new byte[20];
        String intAsString = String.valueOf(intsToPut);
        for (int x = 0; x < 20; x++) {
            if (x < intAsString.length()) {
                byteArray[x] = (byte) intAsString.charAt(x);
            } else {
                byteArray[x] = (byte) 'a';
            }
        }
        return byteArray;
    }

    private void writeWithProgressTracker(ArrayList<byte[]> arrayOfBytes) {
        final long beginTime = System.nanoTime();
        for (byte[] bytes : arrayOfBytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep((long) 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        writeFinishedTransmission();

        final double totalTime = ((System.nanoTime() - beginTime) / 1000000.0);
        Log.d("asdf glass", "transmission finished in " + String.valueOf(totalTime) + " milliseconds");
    }

    private void writeFinishedTransmission() {
        write(lastMessageNotice);
    }
}