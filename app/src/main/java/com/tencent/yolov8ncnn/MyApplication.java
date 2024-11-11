package com.tencent.yolov8ncnn;

import android.app.Application;
import android.bluetooth.BluetoothSocket;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

public class MyApplication extends Application {
    private BluetoothSocket bluetoothSocket;

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public void setBluetoothSocket(BluetoothSocket socket) {
        this.bluetoothSocket = socket;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //   5ef048e1  为在开放平台注册的APPID  注意没有空格，直接替换即可
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=999b2839");
    }
}
