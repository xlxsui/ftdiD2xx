package com.ftdi.javad2xx;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.ftdi.javad2xx.services.FloatingWindowService;

@SuppressLint("ValidFragment")
public class GPIOFragment extends Fragment {

    Context context;
    D2xxManager ftdiD2xx;
    FT_Device ftDevice = null;
    int mDevCount = -1;

    Button btnTXD;
    Button btnStartFloatingWindow;
    SeekBar seekBarDelay;
    EditText etFlashDelay;

    /* Constructor */
    public GPIOFragment(Context parentContext, D2xxManager ftdid2xxContext) {
        context = parentContext;
        ftdiD2xx = ftdid2xxContext;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.device_gpio, container, false);

        btnTXD = view.findViewById(R.id.btn_rts);

        btnStartFloatingWindow = view.findViewById(R.id.btn_start_floating);

        initSeekbar(view);

        btnStartFloatingWindow.setOnClickListener(v -> {
            Intent intent = new Intent(GPIOFragment.this.getActivity(), FloatingWindowService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(intent);
            }

            openCameraApp();
        });

        btnTXD.setOnClickListener(v -> {
            if (mDevCount <= 0) {
                connectFunction();
            }

            if (mDevCount > 0) {
                btnTxdClick();
            }
        });

        etFlashDelay = view.findViewById(R.id.et_flash_delay);

        // 监听输入框数值变化
        etFlashDelay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 在文字改变前的逻辑（可以忽略）
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 实时监听用户输入的数值
                try {
                    int newDelay = Integer.parseInt(s.toString());
                    if (newDelay >= 0 && newDelay <= 2000) {
                        MainActivity.GLOBAL_FLASH_DELAY = newDelay;
                        ((TextView)view.findViewById(R.id.InfoText)).setText("Delay: " + MainActivity.GLOBAL_FLASH_DELAY + " ms");
                    }
                } catch (NumberFormatException e) {
                    // 如果输入的不是数字，可以提示用户
                    Toast.makeText(getContext(), "请输入有效的数字", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 在文字改变后的逻辑（可以忽略）
            }
        });

        return view;
    }

    @SuppressLint("SetTextI18n")
    private void initSeekbar(View view) {
        seekBarDelay = view.findViewById(R.id.seekbar_delay);
        // 初始化 SeekBar
        seekBarDelay.setMax(800); // 设置最大值为 2000
        seekBarDelay.setProgress(MainActivity.GLOBAL_FLASH_DELAY); // 初始化位置
        ((TextView)view.findViewById(R.id.InfoText)).setText("Delay: " + MainActivity.GLOBAL_FLASH_DELAY + " ms");

        // SeekBar 监听器
        seekBarDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MainActivity.GLOBAL_FLASH_DELAY = progress; // 更新延迟时间
                ((TextView)view.findViewById(R.id.InfoText)).setText("Delay: " + MainActivity.GLOBAL_FLASH_DELAY + " ms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 用户开始拖动
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 用户停止拖动
            }
        });
    }

    private void moveToBackground() {
        // 通用方法
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    private void openCameraApp() {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 确保相机应用在新任务中启动
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "未找到相机应用", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onStart() {
        super.onStart();
        mDevCount = -1;
    }


    public void connectFunction() {
        int openIndex = 0;

        if (mDevCount > 0)
            return;

        mDevCount = ftdiD2xx.createDeviceInfoList(context);

        if (mDevCount > 0) {
            ftDevice = ftdiD2xx.openByIndex(context, openIndex);

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

                Toast.makeText(context, "devCount:" + mDevCount + " open index:" + openIndex, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Need to get permission!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("j2xx", "DevCount <= 0");
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (ftDevice != null && ftDevice.isOpen()) {
            ftDevice.close();
        }
    }

    public void btnTxdClick() {
        try {
            // 设置为 GPIO 模式
            ftDevice.setBitMode((byte) 0x01, D2xxManager.FT_BITMODE_SYNC_BITBANG);

            // 设置 TXD 为高电平
            ftDevice.write(new byte[]{0x01}, 1);
            Log.i("FT232", "TXD 高电平");
            Thread.sleep(5); // 保持 10 毫秒

            // 设置 TXD 为低电平
            ftDevice.write(new byte[]{0x00}, 1);
            Log.i("FT232", "TXD 低电平");
            Thread.sleep(10); // 保持 500 毫秒

            // 再次设置 TXD 为高电平
            ftDevice.write(new byte[]{0x01}, 1);
            Log.i("FT232", "TXD 高电平");
        } catch (Exception e) {
            Log.e("FT232", "控制 TXD 引脚时发生错误", e);
        }
    }


}
