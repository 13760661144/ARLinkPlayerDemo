package com.example.ning.arlinkplayerdemo;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by 郑梦晨 on 2018/11/28.
 */
public class UsbDevices {

    private UsbManager manager;
    private UsbDevice mUsbDevice = null;
    public UsbDevices() {

    }

    private void UsbDevicesInit(Activity activity) {
        manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            return;
        } else {
        }

        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        ArrayList<String> USBDeviceList = new ArrayList<String>(); //
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();

            USBDeviceList.add(String.valueOf(device.getVendorId()));
            USBDeviceList.add(String.valueOf(device.getProductId()));
            if (device.getVendorId() == 0x0000 || device.getVendorId() == 0x04b4 || device.getVendorId() == 0x0951 ||
                    device.getVendorId() == 0x483 || device.getVendorId() == 0xAAAA) {
                mUsbDevice = device;
            }
        }
    }
}
