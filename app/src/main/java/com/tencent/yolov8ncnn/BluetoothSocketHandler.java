package com.tencent.yolov8ncnn;

import android.bluetooth.BluetoothSocket;

public class BluetoothSocketHandler {
    private static BluetoothSocket bluetoothSocket;

    public static void setSocket(BluetoothSocket socket) {
        bluetoothSocket = socket;
    }

    public static BluetoothSocket getSocket() {
        return bluetoothSocket;
    }
}
