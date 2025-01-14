package com.ftdi.javad2xx.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.ftdi.javad2xx.MainActivity;
import com.ftdi.javad2xx.R;

public class FloatingWindowService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private Button btnToggleLevel;
    private Button btnAdd;
    private Button btnDec;
    private TextView dragger;


    Context context;
    D2xxManager ftdid2xx;
    FT_Device ftDevice = null;
    int DevCount = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        ftdid2xx = MainActivity.ftD2xx;

        initNotification();
        initView();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        connectFunction();
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private void initView() {
        // 初始化悬浮窗
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null);

        // 设置悬浮窗布局参数
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        // 位置初始值
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 450;
        params.y = 2200;

        windowManager.addView(floatingView, params);

        // 获取按钮引用
        btnToggleLevel = floatingView.findViewById(R.id.btn_toggle_level);
        btnAdd = floatingView.findViewById(R.id.btn_add);
        btnDec = floatingView.findViewById(R.id.btn_dec);
        dragger = floatingView.findViewById(R.id.dragger);
        dragger.setText(MainActivity.GLOBAL_FLASH_DELAY + "ms");

        // 按钮点击事件 - 改变电平状态
        btnToggleLevel.setOnClickListener(v -> {
            if (DevCount <= 0) {
                connectFunction();
            }

            if (DevCount > 0) {
                // 无障碍模拟点击该位置
                simulateClick();
                btnToggleLevel.postDelayed(this::btnTxdClick, MainActivity.GLOBAL_FLASH_DELAY);
            }
        });

        btnAdd.setOnClickListener(v -> {
            MainActivity.GLOBAL_FLASH_DELAY += 2;
            dragger.setText(MainActivity.GLOBAL_FLASH_DELAY + "ms");
        });
        btnDec.setOnClickListener(v -> {
            if (MainActivity.GLOBAL_FLASH_DELAY > 2) {
                MainActivity.GLOBAL_FLASH_DELAY -= 2;
                dragger.setText(MainActivity.GLOBAL_FLASH_DELAY + "ms");
            }
        });

        // 拖动事件
        dragger.setOnTouchListener(new View.OnTouchListener() {
            private int lastX, lastY;
            private int initialX, initialY;
            private long touchDownTime;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i("TAG", "onTouch:  ACTION_DOWN");
                        // 记录初始位置和时间
                        initialX = params.x;
                        initialY = params.y;
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        touchDownTime = System.currentTimeMillis();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // 如果是长按后拖动
                        if (isDragging || System.currentTimeMillis() - touchDownTime > 50) {
                            isDragging = true;
                            // 更新悬浮窗位置
                            params.x = initialX + (int) event.getRawX() - lastX;
                            params.y = initialY + (int) event.getRawY() - lastY;
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        Log.i("TAG", "onTouch:  ACTION_UP");
                        // 释放时重置状态
                        if (isDragging) {
                            isDragging = false;
                        }
                        return true;
                }
                return false;
            }
        });

        btnDec.setOnLongClickListener( v -> {
            // 长按事件处理
            // 在这里处理长按事件的逻辑
            Log.i("TAG", "onLongClick: 长按事件");
            // 停止服务
            stopSelf();
            windowManager.removeView(floatingView);
            return true;
        });
    }

    // 无障碍模拟点击该位置
    private void simulateClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 获取按钮的屏幕位置
            int[] location = new int[2];
            btnToggleLevel.getLocationOnScreen(location);

            // 计算目标点击位置
            int centerX = location[0] + btnToggleLevel.getWidth() / 2;
            int centerY = location[1] - 50; // 上方 50 像素位置

            Log.i("FloatingWindowService", "模拟点击位置: " + centerX + ", " + centerY);

            // 模拟点击手势
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            Path path = new Path();
            path.moveTo(centerX, centerY);
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 50));
            GestureDescription gesture = gestureBuilder.build();

            // 调用无障碍服务进行点击
            AccessibilityService accessibilityService = MyAccessibilityService.getInstance();
            if (accessibilityService != null) {
                accessibilityService.dispatchGesture(gesture, new AccessibilityService.GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        super.onCompleted(gestureDescription);
                        Log.i("FloatingWindowService", "模拟点击成功");
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        super.onCancelled(gestureDescription);
                        Log.e("FloatingWindowService", "模拟点击取消");
                    }
                }, null);
            } else {
                Log.e("FloatingWindowService", "无障碍服务未启动");
                // 引导用户开启无障碍服务
                Toast.makeText(context, "请开启无障碍服务：无障碍-通用-已下载-选中该应用", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        } else {
            Log.e("FloatingWindowService", "模拟点击需要 Android 7.0 或更高版本");
        }
    }


    private void initNotification() {
        // 创建通知频道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "default",
                    "Floating Window Service",
                    NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // 创建前台通知
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Floating Window Service")
                .setContentText("悬浮窗正在运行")
                .setSmallIcon(R.drawable.ic_launcher)
                .build();

        // 调用 startForeground()
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);


        if (ftDevice != null && ftDevice.isOpen()) {
            ftDevice.close();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void connectFunction() {
        int openIndex = 0;

        if (DevCount > 0)
            return;

        DevCount = ftdid2xx.createDeviceInfoList(context);

        if (DevCount > 0) {
            ftDevice = ftdid2xx.openByIndex(context, openIndex);

            if (ftDevice == null) {
                Toast.makeText(context, "ftDev == null", Toast.LENGTH_LONG).show();
                return;
            }

            if (ftDevice.isOpen()) {
                ftDevice.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
                ftDevice.setBaudRate(9600);
                ftDevice.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8,
                        D2xxManager.FT_STOP_BITS_1, D2xxManager.FT_PARITY_NONE);
                ftDevice.setFlowControl(D2xxManager.FT_FLOW_NONE, (byte) 0x00, (byte) 0x00);
                ftDevice.setLatencyTimer((byte) 16);
                ftDevice.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));

                Toast.makeText(context, "devCount:" + DevCount + " open index:" + openIndex, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Need to get permission!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("j2xx", "DevCount <= 0");
        }
    }


    public void btnTxdClick() {
        try {
            // 设置为 GPIO 模式
            ftDevice.setBitMode((byte) 0x01, D2xxManager.FT_BITMODE_SYNC_BITBANG);

            // 设置 TXD 为高电平
            ftDevice.write(new byte[]{0x00}, 1);
            Log.i("FT232", "TXD 低电平");
            Thread.sleep(5); // 保持 20 毫秒

            // 设置 TXD 为低电平
            ftDevice.write(new byte[]{0x01}, 1);
            Log.i("FT232", "TXD 高电平");
            Thread.sleep(20); // 保持 20 毫秒

            // 再次设置 TXD 为高电平
            ftDevice.write(new byte[]{0x00}, 1);
            Log.i("FT232", "TXD 低电平");
        } catch (Exception e) {
            Log.e("FT232", "控制 TXD 引脚时发生错误", e);
        }
    }


}

