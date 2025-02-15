package com.ftdi.javad2xx;

import android.app.Fragment;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ftdi.j2xx.D2xxManager;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends android.app.Activity {
    public static D2xxManager ftD2xx = null;
    public static int current_index = 0;
    public static int GLOBAL_FLASH_DELAY = 460;
    public static int GLOBAL_CLICK_X = -1;
    public static int GLOBAL_CLICK_Y = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            Log.e("ftd2xx-java", ex.getMessage());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_layout);
        setupD2xxLibrary();

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.setPriority(500);
        this.registerReceiver(mUsbReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    public static class DetailsActivity extends android.app.Activity {

        Map<Integer, Fragment> fragmentHashMap = new HashMap<>();

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE) {
                finish();
                return;
            }

            if (savedInstanceState == null) {
                // During initial setup, plug in the details fragment.
                Fragment f = fragmentHashMap.get(current_index);
                if (f == null) {
                    f = new GPIOFragment(this, ftD2xx);

                    fragmentHashMap.put(current_index, f);
                    f.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().add(android.R.id.content, f).commit();
                }

            }
        }
    }

    private void setupD2xxLibrary() {

        if (!ftD2xx.setVIDPID(0x0403, 0xada1))
            Log.i("ftd2xx-java", "setVIDPID Error");

    }

    public static class TitlesFragment extends ListFragment {
        int mCurCheckPosition = 0;
        Map<Integer, Fragment> map = new HashMap<Integer, Fragment>();

        public TitlesFragment() {

        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Populate list with our static array of titles.
            setListAdapter(new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_activated_1, new String[]{"GPIO"}));


            if (savedInstanceState != null) {
                // Restore last state for checked position.
                mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt("curChoice", mCurCheckPosition);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            showDetails(position);
        }

        /**
         * Helper function to show the details of a selected item, either by
         * displaying a fragment in-place in the current UI, or starting a
         * whole new activity in which it is displayed.
         */
        void showDetails(int index) {
            mCurCheckPosition = index;
            current_index = index;
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.
            Intent intent = new Intent();
            intent.setClass(getActivity(), DetailsActivity.class);
            intent.putExtra("index", index);
            startActivity(intent);

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            switch (current_index) {
                default:
                    break;
            }
        }
    }

    /***********USB broadcast receiver*******************************************/
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String TAG = "FragL";
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.i(TAG, "DETACHED...");
            }
        }
    };
}
