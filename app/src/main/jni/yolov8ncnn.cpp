// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include <platform.h>
#include <benchmark.h>
#include "yolo.h"
#include "ndkcamera.h"
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON
JavaVM* g_jvm = nullptr;  // 全局 JavaVM 指针


void log_objects_to_logcat(const std::vector<Object>& objects) {
    for (const auto& obj : objects) {
        __android_log_print(ANDROID_LOG_DEBUG, "YOLO_RESULT", "Label: %d, Confidence: %.2f, Rect: [x=%.2f, y=%.2f, width=%.2f, height=%.2f]",
                            obj.label, obj.prob, obj.rect.x, obj.rect.y, obj.rect.width, obj.rect.height);
    }
}
// 实现 sendResultsViaBluetooth
void sendResultsViaBluetooth(JavaVM* jvm, jobject thiz, const std::vector<Object>& objects) {
    if (objects.empty()) {
        __android_log_print(ANDROID_LOG_DEBUG, "YOLO_DETECTION", "No objects detected, skipping Bluetooth send.");
        return;  // 如果没有检测到对象，不发送
    }
    if (!jvm) {
        __android_log_print(ANDROID_LOG_ERROR, "sendResultsViaBluetooth", "JavaVM pointer is null");
        return;
    }

    JNIEnv* env = nullptr;
    bool attached = false;

    // 动态附加当前线程
    if (jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (jvm->AttachCurrentThread(&env, nullptr) == JNI_OK) {
            attached = true;
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "sendResultsViaBluetooth", "Failed to attach thread to JVM");
            return;
        }
    }

    // 将推理结果转换为 JSON 字符串
    std::string jsonResult = "{ \"objects\": [";
    for (const auto& obj : objects) {
        jsonResult += "{ \"label\": " + std::to_string(obj.label) +
                      ", \"confidence\": " + std::to_string(obj.prob) +
                      ", \"rect\": { \"x\": " + std::to_string(obj.rect.x) +
                      ", \"y\": " + std::to_string(obj.rect.y) +
                      ", \"width\": " + std::to_string(obj.rect.width) +
                      ", \"height\": " + std::to_string(obj.rect.height) + " } },";
    }
    jsonResult.pop_back();  // 移除最后一个逗号
    jsonResult += "] }";

    // 将 JSON 字符串转换为 jstring 并调用 Java 方法
    jstring jResult = env->NewStringUTF(jsonResult.c_str());
    if (jResult == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "sendResultsViaBluetooth", "Failed to create NewStringUTF");
    } else {
        jclass clazz = env->GetObjectClass(thiz);
        jmethodID methodID = env->GetMethodID(clazz, "sendInferenceResult", "(Ljava/lang/String;)V");
        if (methodID) {
            env->CallVoidMethod(thiz, methodID, jResult);
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "sendResultsViaBluetooth", "Failed to find sendInferenceResult method");
        }
        env->DeleteLocalRef(jResult);
    }

    // 分离当前线程以释放资源
    if (attached) {
        jvm->DetachCurrentThread();
    }
}




static int draw_unsupported(cv::Mat& rgb)
{
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                  cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_fps(cv::Mat& rgb)
{
    // resolve moving average
    float avg_fps = 0.f;
    {
        static double t0 = 0.f;
        static float fps_history[10] = {0.f};

        double t1 = ncnn::get_current_time();
        if (t0 == 0.f)
        {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--)
        {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f)
        {
            return 0;
        }

        for (int i = 0; i < 10; i++)
        {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
    }

    char text[32];
    sprintf(text, "FPS=%.2f", avg_fps);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 0;
    int x = rgb.cols - label_size.width;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                  cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}

static Yolo* g_yolo = 0;
static ncnn::Mutex lock;

class MyNdkCamera : public NdkCameraWindow {
public:
    MyNdkCamera(JNIEnv* env, jobject instance);
    ~MyNdkCamera();
    virtual void on_image_render(cv::Mat& rgb) const;
    // 新增 set_fps_range 函数 new next
    void set_fps_range(int min_fps, int max_fps);

private:
    jobject javaInstance;  // 存储 Java 层的 Yolov8Ncnn 实例
    JNIEnv* jniEnv;  // 存储 JNIEnv* 引用

    // 新增用于控制帧率的变量 new 190-192
    mutable std::chrono::steady_clock::time_point last_frame_time;
    int target_fps;
};

// 实现 set_fps_range 函数 new
void MyNdkCamera::set_fps_range(int min_fps, int max_fps) {
    target_fps = (min_fps == max_fps) ? min_fps : 10;  // 设置目标帧率
}

// MyNdkCamera 构造函数和析构函数实现
MyNdkCamera::MyNdkCamera(JNIEnv* env, jobject instance) : jniEnv(env) {
    javaInstance = env->NewGlobalRef(instance);
}

MyNdkCamera::~MyNdkCamera() {
    if (javaInstance) {
        jniEnv->DeleteGlobalRef(javaInstance);
    }
}

// 在推理完成后，使用 on_image_render 发送推理结果
void MyNdkCamera::on_image_render(cv::Mat& rgb) const {
    ncnn::MutexLockGuard g(lock);
    // new 215-224
    if (target_fps > 0) {
        auto now = std::chrono::steady_clock::now();
        auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - last_frame_time).count();

        int frame_interval = 1000 / target_fps;  // 计算每帧的时间间隔
        if (elapsed < frame_interval) {
            return;  // 如果时间不足，则跳过该帧
        }
        last_frame_time = now;  // 更新最后渲染的时间
    }

    if (g_yolo) {
        std::vector<Object> objects;
        int n_class = g_yolo->detect(rgb, objects);
        g_yolo->draw(rgb, objects, n_class);

        // 将推理结果输出到日志中
        log_objects_to_logcat(objects);

        // 使用 JavaVM 调用 sendResultsViaBluetooth，以确保线程正确附加
        sendResultsViaBluetooth(g_jvm, javaInstance, objects);
    } else {
        draw_unsupported(rgb);
    }
    draw_fps(rgb);
}


static MyNdkCamera* g_camera = 0;




extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;  // 将 JavaVM 指针存储到全局变量
    return JNI_VERSION_1_6;
}


// 用于初始化相机的 JNI 方法
extern "C" JNIEXPORT void JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_initCamera(JNIEnv* env, jobject thiz) {
    // 使用 thiz 初始化 MyNdkCamera 实例
    g_camera = new MyNdkCamera(env, thiz);
}




extern "C" JNIEXPORT jboolean JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_loadModel(JNIEnv* env, jobject thiz, jobject assetManager, jint modelid, jint cpugpu) {
    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    const char* modeltypes[] = {"n", "s", "hand11", "hand12"};
    const int target_sizes[] = {320, 320, 320, 320};
    const float mean_vals[][3] = {{103.53f, 116.28f, 123.675f}, {103.53f, 116.28f, 123.675f}, {103.53f, 116.28f, 123.675f}, {103.53f, 116.28f, 123.675f}};
    const float norm_vals[][3] = {{1 / 255.f, 1 / 255.f, 1 / 255.f}, {1 / 255.f, 1 / 255.f, 1 / 255.f}, {1 / 255.f, 1 / 255.f, 1 / 255.f}};

    const char* modeltype = modeltypes[(int)modelid];
    int target_size = target_sizes[(int)modelid];
    bool use_gpu = (int)cpugpu == 1;

    ncnn::MutexLockGuard g(lock);
    if (use_gpu && ncnn::get_gpu_count() == 0) {
        delete g_yolo;
        g_yolo = nullptr;
    } else {
        if (!g_yolo) g_yolo = new Yolo;
        g_yolo->load(mgr, modeltype, target_size, mean_vals[(int)modelid], norm_vals[(int)modelid], use_gpu);
    }
    return JNI_TRUE;
}

//extern "C" JNIEXPORT jboolean JNICALL
//Java_com_tencent_yolov8ncnn_Yolov8Ncnn_openCamera(JNIEnv* env, jobject thiz, jint facing) {
//    if (facing < 0 || facing > 1)
//        return JNI_FALSE;
//
//    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);
//    g_camera->open((int)facing);
//    return JNI_TRUE;
//}
// new 301-316
extern "C" JNIEXPORT jboolean JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_openCamera(JNIEnv* env, jobject thiz, jint facing) {
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int)facing);

    // Set target FPS range to 10 FPS
    int min_fps = 10;
    int max_fps = 10;
    g_camera->set_fps_range(min_fps, max_fps);

    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_closeCamera(JNIEnv* env, jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");
    g_camera->close();
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_setOutputWindow(JNIEnv* env, jobject thiz, jobject surface) {
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);
    g_camera->set_window(win);
    return JNI_TRUE;
}



