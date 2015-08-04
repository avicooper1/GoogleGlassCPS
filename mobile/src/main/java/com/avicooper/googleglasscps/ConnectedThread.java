package com.avicooper.googleglasscps;


import android.bluetooth.BluetoothSocket;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
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
        } catch (IOException e) {
        }

        Log.d("asdf mobile", "succeeded");
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        start();
    }

    //Class methods
    public void write(byte[] bytes){

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
        } catch (IOException e) {
        }
    }



    final private byte[] firstMessageHeader = "File size:".getBytes();
    final private byte[] lastMessageNotice = "FinalMessageNotice..".getBytes();
    final private byte[] receivedMessageNotice = "ReceivedMessageCont.".getBytes();
    final private byte[] secondPacketsSendAttemptNotice = "SecondPacketsSend...".getBytes();
    final private byte[] noMissingPacketsNotice = "NoMoreMissingPackets".getBytes();
    final private byte[] clearBuffer = "aaaaaaaaaaaaaaaaaaaa".getBytes();
    byte[][] aggregatedBuffer;
    boolean waitingForCommand = true;


    //My this class methods
    public void run() {

        byte[] buffer = new byte[20];  // buffer store for the stream
        Log.d("asdf mobile", "listening for messages");

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            // Read from the InputStream
            try {
                mmInStream.read(buffer);
            } catch (IOException e) {
            }

            if (waitingForCommand) {
                if (Arrays.equals(Arrays.copyOfRange(buffer, 0, 10), firstMessageHeader)) {
                    final int packetsAmount = findIntsInFilledBuffer(Arrays.copyOfRange(buffer, 10, buffer.length));
                    aggregatedBuffer = new byte[packetsAmount][18];
                    waitingForCommand = false;
                    Log.d("asdf mobile", "the size of the transmission is " + String.valueOf(packetsAmount) + " packets");
                    write(receivedMessageNotice);
                    continue;
                }
                else if(Arrays.equals(buffer, secondPacketsSendAttemptNotice)){
                    waitingForCommand = false;
                    continue;
                }
            }
            if (!waitingForCommand) {
                if (Arrays.equals(buffer, lastMessageNotice)) {

                    final byte[] emptyByteArray = new byte[18];

                    write(clearBuffer);

                    ArrayList<Integer> intsOfMissingPackets = new ArrayList<>();
                    for (int x = 0; x < aggregatedBuffer.length; x++) {
                        if (Arrays.equals(aggregatedBuffer[x], emptyByteArray)) {
                            intsOfMissingPackets.add(x);
                        }
                    }

                    if (intsOfMissingPackets.size() == 0){
                        write(noMissingPacketsNotice);
                        waitingForCommand = true;
                        break;
                    } else {
                        Log.d("asdf mobile", "the following packets wree not received:");
                        ArrayList<byte[]> byteArrayPacketsOfMissingPackets = new ArrayList<>();
                        for (int mInt : intsOfMissingPackets){
                            Log.d("asdf mobile", "packet: " + String.valueOf(mInt));
                            byteArrayPacketsOfMissingPackets.add(putIntsInFilledBuffer(mInt));
                        }

                        waitingForCommand = true;

                        Log.d("asdf mobile", String.valueOf(intsOfMissingPackets.size()) + " of " + String.valueOf(aggregatedBuffer.length) + " packets, or " + String.valueOf(intsOfMissingPackets.size() * 100 / (double) aggregatedBuffer.length) + "% did not get through");
                        write(secondPacketsSendAttemptNotice);
                        writeWithProgressTracker(byteArrayPacketsOfMissingPackets);
                    }

                } else {
//                    Log.d("asdf mobile", "packet number is: " + String.valueOf(headersAndInts(buffer)));
//                    printOutBytesArrayAsInts(Arrays.copyOfRange(buffer, 0, 2));
                    try{
//                        printOutBytesArrayAsInts(Arrays.copyOfRange(buffer, 0, 2));
//                        Log.d("asdf mobile", "header number is: " + String.valueOf(headersAndInts(buffer)));
                        storeByteInArray(buffer);
                    }
                    catch (Exception e){
                        //Log.d("asdf mobile", "for some reason coundn't store packet");
                    }
                }
            }
        }
        showPicture();
    }

    private void storeByteInArray(byte[] byteArray) {
        aggregatedBuffer[headersAndInts(byteArray)] = headersAndIntsWithHeaderStripped(byteArray);
    }

    private void showPicture() {
        byte[] singleByteArray = new byte[aggregatedBuffer.length * 18];
        int counter = 0;
        for (byte[] byteArray : aggregatedBuffer) {
            for (byte mByte : byteArray) {
                singleByteArray[counter] = mByte;
                counter++;
            }
        }

        main.outputMessage(singleByteArray, "image");
        //main.outputMessage(singleByteArray, "string");
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
                Thread.sleep((long) 0, 10);
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