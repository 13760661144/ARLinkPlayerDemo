package com.example.ning.arlinkplayerdemo;

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by 郑梦晨 on 2018/11/27.
 */
public class AccessoryLink {

    private final UsbManager mUsbManager;
    private ParcelFileDescriptor mConnection;
    private FileDescriptor mFileDescriptor;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;
    private Boolean mOpened;
    private Thread mThread=new Thread();

    private OnReceivedListener mOnReceivedListener;

    public AccessoryLink(Context context, OnReceivedListener onReceivedListener) {
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.mOnReceivedListener = onReceivedListener;
    }

    public void open() {
        UsbAccessory accessory = getAccessory();
        mConnection = mUsbManager.openAccessory(accessory);
        mFileDescriptor = mConnection.getFileDescriptor();
        mInputStream = new FileInputStream(mFileDescriptor);
        mOutputStream = new FileOutputStream(mFileDescriptor);
        if (mInputStream == null)
            return;

        if (mOutputStream == null)
            return;

        mOpened = true;

        mThread=new Thread(){
            @Override
            public void run() {
                int errorCount = 0;
                int BUFFER_SIZE =511;
                byte[] buffer = new byte[BUFFER_SIZE];

                while (mOpened) {
                    try {
                        int read = mInputStream.read(buffer);
                        if (read != 0 && read > 0) {
                            if (mOnReceivedListener == null)
                                return;
                            byte[] bytes = Arrays.copyOf(buffer, read);
                            mOnReceivedListener.onReceived(bytes);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        errorCount++;
                    }
                    if (errorCount > 5) {
                        try {
                            close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

        mThread.start();

    }

    public void close() throws IOException {

        mOpened = false;

        if (mInputStream != null) {
            mInputStream.close();
            mInputStream = null;
        }

        if (mOutputStream != null) {
            mOutputStream.close();
            mOutputStream = null;
        }

        if (mConnection != null) {
            mConnection.close();
            mConnection = null;
        }

        if (mThread!=null){
            mThread.interrupt();
            mThread=null;
        }
    }

    private UsbAccessory getAccessory() {
        UsbAccessory[] accessoryList = mUsbManager.getAccessoryList();
        if (accessoryList != null && accessoryList.length != 0) {
            return accessoryList[0];
        } else {
            return null;
        }
    }


   public void setOnReceivedListener(OnReceivedListener onReceivedListener){
        this.mOnReceivedListener=onReceivedListener;
   }

    interface OnReceivedListener {
        void onReceived(byte[] b) throws IOException;
    }

}
