// BluetoothConnectionService.java
package com.tencent.yolov8ncnn;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothConnectionService extends Service {

    private static final String TAG = "BluetoothConnectionService";
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private static boolean isConnected = false;
    public static final String ACTION_BLUETOOTH_CONNECTED = "com.tencent.yolov8ncnn.ACTION_BLUETOOTH_CONNECTED";


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isConnected) {
            connectToBluetoothDevice(); // 确保尝试连接蓝牙设备
        }

        // 检查 Intent 和数据传递的有效性
        if (intent != null && "SEND_DATA".equals(intent.getAction())) {
            String resultJson = intent.getStringExtra("INFERENCE_JSON");
            if (resultJson != null && isConnected) {
                sendData(resultJson); // 确保蓝牙连接后再发送数据
            } else {
                Log.e(TAG, "蓝牙未连接或推理结果数据为空");
            }
        }
        return START_STICKY;
    }


    private void connectToBluetoothDevice() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = sharedPreferences.getString("bluetooth_address", null);

        if (deviceAddress != null) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

            new Thread(() -> {
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    isConnected = true;
                    sendBroadcast(new Intent(ACTION_BLUETOOTH_CONNECTED));

                    Log.d(TAG, "已连接到蓝牙设备 " + deviceAddress);
                } catch (IOException e) {
                    Log.e(TAG, "蓝牙连接失败", e);
                }
            }).start();
        }
    }

    private void sendData(String resultJson) {
        try {
            if (outputStream != null) {
                outputStream.write(resultJson.getBytes());
                outputStream.flush();
                Log.d(TAG, "JSON 数据已发送: " + resultJson);
            }
        } catch (IOException e) {
            Log.e(TAG, "数据发送失败", e);
        }
    }
    public static boolean isConnected() {
        return isConnected;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        isConnected = false;
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                Log.d(TAG, "蓝牙连接已关闭");
            }
        } catch (IOException e) {
            Log.e(TAG, "关闭蓝牙连接失败", e);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
