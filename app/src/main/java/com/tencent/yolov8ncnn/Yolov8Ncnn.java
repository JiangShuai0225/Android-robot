package com.tencent.yolov8ncnn;

import android.content.res.AssetManager;
import android.util.Log;
import android.view.Surface;

public class Yolov8Ncnn {

    private InferenceListener inferenceListener;

    // 加载模型和初始化摄像头的本地方法
    public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
    public native boolean openCamera(int facing);
    public native boolean closeCamera();
    public native boolean setOutputWindow(Surface surface);
    public native void initCamera();

    // 静态加载 native 库
    static {
        System.loadLibrary("yolov8ncnn");
    }

    // 设置推理监听器，用于回调推理结果
    public void setInferenceListener(InferenceListener listener) {
        this.inferenceListener = listener;
    }

    // 模拟推理过程，将结果回调到监听器
    public void sendInferenceResult(String resultJson) {
        Log.d("Yolov8Ncnn", "Inference result: " + resultJson);

        if (inferenceListener != null) {
            inferenceListener.onInferenceResult(resultJson);
        } else {
            Log.e("Yolov8Ncnn", "Inference listener not set");
        }
    }

    // 推理监听器接口
    public interface InferenceListener {
        void onInferenceResult(String result);
    }
}

//// Tencent is pleased to support the open source community by making ncnn available.
////
//// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
////
//// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
//// in compliance with the License. You may obtain a copy of the License at
////
//// https://opensource.org/licenses/BSD-3-Clause
////
//// Unless required by applicable law or agreed to in writing, software distributed
//// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
//// CONDITIONS OF ANY KIND, either express or implied. See the License for the
//// specific language governing permissions and limitations under the License.
//
//package com.tencent.yolov8ncnn;
//
//import static java.security.AccessController.getContext;
//
//import android.content.res.AssetManager;
//import android.util.Log;
//import android.view.Surface;
//
//import java.io.IOException;
//import java.io.OutputStream;
//
//public class Yolov8Ncnn
//{
//    private OutputStream outputStream;
//    public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
//    public native boolean openCamera(int facing);
//    public native boolean closeCamera();
//    public native boolean setOutputWindow(Surface surface);
//    public native void initCamera();
//    static {
//        System.loadLibrary("yolov8ncnn");
//    }
//    public void setOutputStream(OutputStream outputStream) {
//        this.outputStream = outputStream;
//    }
//
//    public void sendInferenceResult(String resultJson) {
//        Log.d("Yolov8Ncnn", "Inference result: " + resultJson);
//
//        if (outputStream != null) {
//            try {
//                outputStream.write(resultJson.getBytes());
//                outputStream.flush();
//                Log.d("Bluetooth", "Sent result to Bluetooth device");
//            } catch (IOException e) {
//                Log.e("Bluetooth", "Failed to send data", e);
//            }
//        } else {
//            Log.e("Bluetooth", "Bluetooth not connected");
//        }
//    }
//
//}
