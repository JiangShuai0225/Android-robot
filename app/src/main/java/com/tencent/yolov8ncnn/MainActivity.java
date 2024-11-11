// MainActivity.java
package com.tencent.yolov8ncnn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Range;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CAMERA = 1;
    private Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();
    private CameraDevice cameraDevice;
    private SurfaceView cameraView;
    private int facing = 0;
    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;
    private int current_model = 0;
    private int current_cpugpu = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        cameraView = findViewById(R.id.cameraview);
        cameraView.getHolder().addCallback(this);
        yolov8ncnn.initCamera();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String deviceAddress = sharedPreferences.getString("bluetooth_address", null);

        if (deviceAddress == null) {
            Toast.makeText(this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, BluetoothConnectionActivity.class);
            startActivity(intent);
        } else {
            startBluetoothService();
        }

        // 设置屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 初始化 Yolov8Ncnn 并调用 initCamera 方法
        yolov8ncnn.initCamera();

        // 初始化相机视图
        cameraView = findViewById(R.id.cameraview);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);
        cameraView.getHolder().addCallback(this);

        // 切换相机按钮
        Button buttonSwitchCamera = findViewById(R.id.buttonSwitchCamera);
        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int new_facing = 1 - facing;
                yolov8ncnn.closeCamera();
                yolov8ncnn.openCamera(new_facing);
                facing = new_facing;
            }
        });

        // 在 onCreate 方法中添加以下代码
        Button buttonGoToXunFei = findViewById(R.id.buttonGoToXunFei);
        buttonGoToXunFei.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, XunFeiActivity.class);
                startActivity(intent);
            }
        });


        // 模型选择 Spinner
        spinnerModel = findViewById(R.id.spinnerModel);
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                if (position != current_model) {
                    current_model = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // CPU/GPU 选择 Spinner
        spinnerCPUGPU = findViewById(R.id.spinnerCPUGPU);
        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                if (position != current_cpugpu) {
                    current_cpugpu = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        reload();

        // 设置推理监听器，将 JSON 结果直接传递
        yolov8ncnn.setInferenceListener(resultJson -> sendDataToBluetoothService(resultJson));
    }

    private void startBluetoothService() {
        Intent intent = new Intent(MainActivity.this, BluetoothConnectionService.class);
        startService(intent);

        // 检查是否成功连接蓝牙
        if (!BluetoothConnectionService.isConnected()) {
            Log.e(TAG, "Bluetooth 连接失败，服务未成功连接");
        }
    }

    private void reload() {
        boolean ret_init = yolov8ncnn.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init) {
            Log.e("MainActivity", "yolov8ncnn loadModel failed");
        }
    }

    // 将 JSON 格式的推理结果数据传递给 BluetoothConnectionService
    private void sendDataToBluetoothService(String resultJson) {
        Intent intent = new Intent(MainActivity.this, BluetoothConnectionService.class);
        intent.setAction("SEND_DATA");

        intent.putExtra("INFERENCE_JSON", resultJson + "\r\n" ); // 传递 JSON 字符串
        startService(intent);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        yolov8ncnn.setOutputWindow(holder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 如需处理分辨率或格式变化可在此实现
        yolov8ncnn.setOutputWindow(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        yolov8ncnn.closeCamera();
    }
    @Override
    public void onResume() {
        super.onResume();

        // 检查相机权限
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }

        yolov8ncnn.openCamera(facing);
    }

    @Override
    public void onPause() {
        super.onPause();
        yolov8ncnn.closeCamera();
    }
}
