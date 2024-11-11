// BluetoothConnectionActivity.java
package com.tencent.yolov8ncnn;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import android.content.BroadcastReceiver;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Set;

public class BluetoothConnectionActivity extends Activity {

    private static final String TAG = "BluetoothConnectionActivity";
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private BluetoothAdapter bluetoothAdapter;
    private ListView pairedDevicesListView;

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothConnectionService.ACTION_BLUETOOTH_CONNECTED.equals(intent.getAction())) {
                // 当蓝牙连接成功时，启动 MainActivity
                Intent mainIntent = new Intent(BluetoothConnectionActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();  // 关闭 BluetoothConnectionActivity
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        pairedDevicesListView = findViewById(R.id.pairedDevicesListView);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothPermissions();
        }

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        showPairedDevices();

        // 注册广播接收器以监听蓝牙连接
        IntentFilter filter = new IntentFilter(BluetoothConnectionService.ACTION_BLUETOOTH_CONNECTED);
        registerReceiver(bluetoothReceiver, filter);

        // 开启蓝牙服务
        Intent serviceIntent = new Intent(this, BluetoothConnectionService.class);
        startService(serviceIntent);


    }

    private void showPairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        for (BluetoothDevice device : pairedDevices) {
            adapter.add(device.getName() + "\n" + device.getAddress());
        }

        pairedDevicesListView.setAdapter(adapter);
        pairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = (String) parent.getItemAtPosition(position);
                String address = info.substring(info.length() - 17);

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(BluetoothConnectionActivity.this);
                sharedPreferences.edit().putString("bluetooth_address", address).apply();

                Toast.makeText(BluetoothConnectionActivity.this, "设备已连接", Toast.LENGTH_SHORT).show();
//                finish();  // 连接成功后关闭页面

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver); // 确保活动销毁时注销广播接收器
    }

    // 检查权限的方法
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BLUETOOTH_PERMISSION);
        }
    }

    // 重写 onRequestPermissionsResult 以处理用户的权限请求响应
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户已授予权限
                // 可以继续进行蓝牙操作
            } else {
                // 权限被拒绝，处理权限被拒绝的情况
            }
        }
    }
}
