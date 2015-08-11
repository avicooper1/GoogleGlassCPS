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
    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private MainActivity main;
    private final JClient client = new JClient(101, 102, true);
    private int returnInt = 0;
    private boolean connectedToMFServerNode = false;

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
        try{
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    client.start();
                    connectedToMFServerNode = true;
                }
            });
            t.start();
        }
        catch (Exception e){
            Log.d("asdf mobile", "MF .start() failed");
        }
    }

    //Class methods
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
//    public void cancel() {
//        try {
//            mmSocket.close();
//        } catch (IOException e) {
//        }
//    }


    final private byte[] firstMessageHeader = "File size:".getBytes();
    final private byte[] lastMessageNotice = "FinalMessageNotice..".getBytes();
    final private byte[] intermediateMessageNotice = "IntermediateNotice..".getBytes();
    final private byte[] receivedMessageNotice = "ReceivedMessageCont.".getBytes();
    final private byte[] secondPacketsSendAttemptNotice = "SecondPacketsSend...".getBytes();
    final private byte[] noMissingPacketsNotice = "NoMoreMissingPackets".getBytes();
    final private byte[] clearBuffer = "aaaaaaaaaaaaaaaaaaaa".getBytes();
    final byte[] emptyByteArray = new byte[18];
    byte[][][] aggregatedBuffer;
    byte[][] finalizedBuffer;
    int sendCycleCounter = 0;
    int packetsAmount;
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
                //printOutBytesArray(buffer);
                Log.d("buffer mobile", new String(buffer));
            } catch (IOException e) {
            }

            if (waitingForCommand) {
                if (Arrays.equals(Arrays.copyOfRange(buffer, 0, 10), firstMessageHeader)) {
                    packetsAmount = findIntsInFilledBuffer(Arrays.copyOfRange(buffer, 10, buffer.length));
                    //resetting all variables for new message
                    aggregatedBuffer = null;
                    finalizedBuffer = null;
                    sendCycleCounter = 0;


                    aggregatedBuffer = new byte[3][packetsAmount][18];
                    finalizedBuffer = new byte[packetsAmount][18];
                    waitingForCommand = false;
                    Log.d("asdf mobile", "the size of the transmission is " + String.valueOf(packetsAmount) + " packets");
                    write(receivedMessageNotice);
                    continue;
                } else if (Arrays.equals(buffer, secondPacketsSendAttemptNotice)) {
                    waitingForCommand = false;
                    continue;
                }
            }
            if (!waitingForCommand) {
                if (Arrays.equals(buffer, intermediateMessageNotice)) {
                    sendCycleCounter++;
                    Log.d("asdf mobile", "received intermediate message");
                }
                if (Arrays.equals(buffer, lastMessageNotice)) {
                    Log.d("asdf mobile", "received final message");

                    long beginTime = System.nanoTime();

                    sendCycleCounter++;

                    write(clearBuffer);

                    ArrayList<Integer> intsOfMissingPackets = new ArrayList<>();
                    for (int x = 0; x < packetsAmount; x++) {
                        //if (Arrays.equals(finalizedBuffer[x], emptyByteArray)) {
                            if (!findCorrectPacket(x, 0, 1)) {
                                intsOfMissingPackets.add(x);
                          //  }
                        }
                    }

                    if (intsOfMissingPackets.size() == 0) {
                        write(noMissingPacketsNotice);
                        waitingForCommand = true;
                        Log.d("asdf mobile", "the total check time was: " + String.valueOf((System.nanoTime() - beginTime) / 1000000.0) + " ms.");
                        showPicture();
                    } else {
                        Log.d("asdf mobile", "the following packets were not received:");
                        ArrayList<byte[]> byteArrayPacketsOfMissingPackets = new ArrayList<>();
                        for (int mInt : intsOfMissingPackets) {
                            Log.d("asdf mobile", "packet: " + String.valueOf(mInt));
                            byteArrayPacketsOfMissingPackets.add(putIntsInFilledBuffer(mInt));
                        }

                        waitingForCommand = true;

                        Log.d("asdf mobile", String.valueOf(intsOfMissingPackets.size()) + " of " + String.valueOf(packetsAmount) + " packets, or " + String.valueOf(intsOfMissingPackets.size() * 100 / (double) packetsAmount) + "% did not get through");
                        write(secondPacketsSendAttemptNotice);
                        writeWithProgressTracker(byteArrayPacketsOfMissingPackets);
                    }

                } else {
                    try {
                        storeByteInArray(buffer);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private void storeByteInArray(byte[] byteArray) {
        aggregatedBuffer[sendCycleCounter % 3][headersAndInts(byteArray)] = headersAndIntsWithHeaderStripped(byteArray);
        //finalizedBuffer[headersAndInts(byteArray)] = headersAndIntsWithHeaderStripped(byteArray);
    }

    private boolean findCorrectPacket(int header, int checkSendCount1, int checkSendCount2) {
        if (checkSendCount2 == 3){
            return false;
        }

        if (Arrays.equals(aggregatedBuffer[checkSendCount1][header], aggregatedBuffer[checkSendCount2][header]) && !Arrays.equals(aggregatedBuffer[checkSendCount1][header], emptyByteArray)){
            finalizedBuffer[header] = aggregatedBuffer[checkSendCount1][header].clone();
            return true;
        }
        else{
            Log.d("asdf mobile", "didnt get first packet");
            return findCorrectPacket(header, checkSendCount2, checkSendCount2 + 1);
        }
    }

    private void showPicture() {
        byte[] singleByteArray = new byte[packetsAmount * 18];
        int counter = 0;
        for (byte[] byteArray : finalizedBuffer) {
            for (byte mByte : byteArray) {
                singleByteArray[counter] = mByte;
                counter++;
            }
        }

        if (connectedToMFServerNode){
            returnInt = client.sendImage(singleByteArray, singleByteArray.length);
            if (returnInt < 0) {
                Log.d("asdf mobile", "MF client send failed");
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
        for (int mInt : intsOfArray) {
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