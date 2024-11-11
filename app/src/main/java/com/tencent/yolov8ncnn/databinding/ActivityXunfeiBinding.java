package com.tencent.yolov8ncnn.databinding;


import static android.system.Os.bind;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;

import com.tencent.yolov8ncnn.R;

public final class ActivityXunfeiBinding implements ViewBinding{
    @NonNull
    private final LinearLayout rootView;

    @NonNull
    public final Button btnStart;

    @NonNull
    public final LinearLayout main;

    @NonNull
    public final TextView tvResult;

    public ActivityXunfeiBinding(@NonNull LinearLayout rootView, @NonNull Button btnStart, @NonNull LinearLayout main, @NonNull TextView tvResult) {
        this.rootView = rootView;
        this.btnStart = btnStart;
        this.main = main;
        this.tvResult = tvResult;
    }

    @Override
    @NonNull
    public LinearLayout getRoot() {
        return rootView;
    }

    @NonNull
    public static ActivityXunfeiBinding inflate(@NonNull LayoutInflater inflater) {
        return inflate(inflater, null, false);
    }

    @NonNull
    public static ActivityXunfeiBinding inflate(@NonNull LayoutInflater inflater,
                                              @Nullable ViewGroup parent, boolean attachToParent) {
        View root = inflater.inflate(R.layout.activity_xun_fei, parent, false);
        if (attachToParent) {
            parent.addView(root);
        }
        return bind(root);
    }

    @NonNull
    public static ActivityXunfeiBinding bind(@NonNull View rootView) {
        // The body of this method is generated in a way you would not otherwise write.
        // This is done to optimize the compiled bytecode for size and performance.
        int id;
        missingId: {
            id = R.id.btn_start;
            Button btnStart = ViewBindings.findChildViewById(rootView, id);
            if (btnStart == null) {
                break missingId;
            }

            LinearLayout main = (LinearLayout) rootView;

            id = R.id.tv_result;
            TextView tvResult = ViewBindings.findChildViewById(rootView, id);
            if (tvResult == null) {
                break missingId;
            }

            return new ActivityXunfeiBinding((LinearLayout) rootView, btnStart, main, tvResult);
        }
        String missingId = rootView.getResources().getResourceName(id);
        throw new NullPointerException("Missing required view with ID: ".concat(missingId));
    }

}