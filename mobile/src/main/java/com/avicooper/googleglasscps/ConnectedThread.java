package com.avicooper.googleglasscps;


import android.app.WallpaperManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.util.Log;

import com.example.avicooper.googleglasscps.R;

import java.io.ByteArrayOutputStream;
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

    int counter = 0;
    final private byte[] firstMessageHeader = "File size:".getBytes();
    final private byte[] lastMessageNotice = "FinalMessageNotice..".getBytes();
    byte[][] aggregatedBuffer;
    int sizeOfIncomingData;
    boolean alreadyReceivedSize = false;
    int arrayIndex = 0;

    public void run() {

        byte[] buffer = new byte[20];  // buffer store for the stream
        Log.d("asdf mobile", "listning for messages");

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            // Read from the InputStream
            try {
                mmInStream.read(buffer);
            } catch (IOException e) {
            }

            if (!alreadyReceivedSize) {
                if (Arrays.equals(Arrays.copyOfRange(buffer, 0, 10), firstMessageHeader)) {
                    int counter = 10;
                    printOutBytesArray(buffer);
                    while (buffer[counter] != 'a') {
                        counter++;
                    }
                    sizeOfIncomingData = Integer.valueOf(new String(Arrays.copyOfRange(buffer, 10, counter)));
                    aggregatedBuffer = new byte[sizeOfIncomingData][20];
                    alreadyReceivedSize = true;
                    try {
                        write("ReceivedFileSizeMesg".getBytes());
                    } catch (IOException e) {
                    } catch (InterruptedException e) {
                    }
                }
            } else {
                if ((int) buffer[0] == -128) {
                    arrayIndex = (((int) buffer[1] + 128) * 256) + ((int) buffer[2] + 128);
                } else {
                    arrayIndex = (((int) buffer[0] + 128) * 256 * 256) + (((int) buffer[1] + 128) * 256) + ((int) buffer[2] + 128);
                }
                arrayIndex = (((int) buffer[0] + 128) * 256 * 256) + (((int) buffer[1] + 128) * 256) + ((int) buffer[2] + 128);
                if (!(Arrays.equals(buffer, lastMessageNotice))) {
                    if (arrayIndex < aggregatedBuffer.length) {
                        storeByteInArray(buffer);
                    }
//                    else {
//                        printOutBytesArrayAsInts(buffer);
//                        Log.d("asdf mobile", "current readCounter is: " + String.valueOf(readCounter));
//                        Log.d("asdf mobile", "current arrayIndex is: " + String.valueOf(arrayIndex));
//                        Log.d("asdf mobile", "for some reason, the received message was not able to be processed");
//                    }
                } else {
//                    Log.d("asdf mobile", "in else");
                    final byte[] emptyByteArray = new byte[20];
//                    byte[] byteArrayToSend = new byte[20];
//                    boolean thereAreMissingPackets = false;
//                    try {
//                        write("aaaaaaaaaaaaaaaaaaaa".getBytes());
//                    } catch (IOException e) {
//                    } catch (InterruptedException e) {
//                    }
                    int counter = 0;
                    //Log.d("asdf mobile", "the following packets did not get through");
                    for (int x = 0; x < aggregatedBuffer.length; x++) {
                        if (Arrays.equals(aggregatedBuffer[x], emptyByteArray)) {
                            counter++;
                            Log.d("asdf mobile", String.valueOf(x));
                        }
                    }
//                            String numberAsStringToSend = String.valueOf(x);
//                            for (int y = 0; y < 20; y++) {
//                                if (y < numberAsStringToSend.length()) {
//                                    byteArrayToSend[y] = (byte) numberAsStringToSend.charAt(y);
//                                } else {
//                                    byteArrayToSend[y] = (byte) 'a';
//                                }
//                            }
//                            try {
//                                write(byteArrayToSend);
//                                printOutBytesArray(byteArrayToSend);
//                            } catch (IOException e) {
//                            } catch (InterruptedException e) {
//                            }
//                            thereAreMissingPackets = true;
//                        }
//                    }
//                    Log.d("asdf mobile", "sent out the needed missing packets");
//                    try {
//                        write(lastMessageNotice);
//                    } catch (IOException e) {
//                    } catch (InterruptedException e) {
//                    }
//                    if (oneResendAlready){
                    Log.d("asdf mobile", String.valueOf(counter * 100 / (double) aggregatedBuffer.length) + "% of the packets did not get through");
                    showPicture(aggregatedBuffer);
                    break;
                    // }
                    // oneResendAlready = true;
//                }
                }
            }
        }
    }

    private void storeByteInArray(byte[] buffer) {
        aggregatedBuffer[arrayIndex] = buffer.clone();
    }

    private void showPicture(byte[][] arrayOfByteArrays) {
        byte[] singleByteArray = new byte[sizeOfIncomingData * 17];
        int totalBytesCounter = 0;
        for (byte[] byteArray : aggregatedBuffer) {
            int innerBytesCounter = 0;
            for (byte mByte : byteArray) {
                if (innerBytesCounter != 0 && innerBytesCounter != 1 && innerBytesCounter != 2) {
                    singleByteArray[totalBytesCounter] = mByte;
                    totalBytesCounter++;
                }
                innerBytesCounter++;
            }
        }
        //Log.d("asdf mobile", "entire byte array: " + new String(singleByteArray));
        Log.d("asdf mobile", "will now display message");
        main.outputMessage(singleByteArray, "image");
    }

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


    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) throws IOException, InterruptedException {
        mmOutStream.write(bytes);
        Thread.sleep((long) 0.1);
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }
}