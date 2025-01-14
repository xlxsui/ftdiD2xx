package com.ftdi.javad2xx.services;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {

    private static MyAccessibilityService instance;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i("MyAccessibilityService", "无障碍服务已连接");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 处理无障碍事件（这里无需处理）
    }

    @Override
    public void onInterrupt() {
        Log.w("MyAccessibilityService", "无障碍服务被中断");
    }

    public static MyAccessibilityService getInstance() {
        return instance;
    }
}

