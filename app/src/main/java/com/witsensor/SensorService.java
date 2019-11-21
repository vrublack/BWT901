package com.witsensor;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import static com.witsensor.DataMonitor.MESSAGE_DEVICE_NAME;
import static com.witsensor.DataMonitor.MESSAGE_READ;
import static com.witsensor.DataMonitor.MESSAGE_STATE_CHANGE;
import static com.witsensor.DataMonitor.MESSAGE_TOAST;

public class SensorService extends Service {
    public static final String CHANNEL_ID = "SensorServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    public static boolean isRunning = false;
    private BluetoothReader mBluetoothReader = null;
    private String mConnectedDeviceName = "(none)";
    private BluetoothAdapter mBluetoothAdapter = null;
    private boolean isRecording = false;
    private boolean isConnected = false;

    private final IBinder binder = new SensorBinder();

    private Handler mUiHandler;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothReader.STATE_CONNECTED:
                            Log.d(SensorService.class.getCanonicalName(), "STATE_CONNECTED");
                            passNotification(getString(R.string.title_connected_to) + mConnectedDeviceName);
                            isConnected = true;
                            break;
                        case BluetoothReader.STATE_CONNECTING:
                            break;
                        case BluetoothReader.STATE_LISTEN:
                        case BluetoothReader.STATE_NONE:
                            isConnected = false;
                            isRecording = false;
                            Log.d(SensorService.class.getCanonicalName(), "STATE_NONE");
                            passNotification(getString(R.string.title_not_connected));
                            break;
                        case BluetoothReader.STATE_RECONNECTING:
                            Log.d(SensorService.class.getCanonicalName(), "STATE_RECONNECTING");
                            passNotification(getString(R.string.reconnecting));
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    break;
                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString("device_name");
                    break;
                case MESSAGE_TOAST:
                    break;
            }

            // also call the activity's handler so it can update its UI (in case there is an activity attached)
            if (mUiHandler != null)
                mUiHandler.handleMessage(msg);
        }

    };

    public CharSequence getConnectedDeviceName() {
        return mConnectedDeviceName;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public BluetoothReader getBluetoothReader() {
        return mBluetoothReader;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecording() {
        isRecording = true;
        mBluetoothReader.setRecord(true);
        passNotification(getString(R.string.recording));
    }

    public void stopRecording() {
        isRecording = false;
        mBluetoothReader.setRecord(false);
        passNotification(getString(R.string.not_recording));
    }

    public boolean isConnected() {
        return isConnected;
    }

    public class SensorBinder extends Binder {
        SensorService getService() {
            return SensorService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        isRunning = true;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.msg1), Toast.LENGTH_LONG).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.enable();

        if (mBluetoothReader == null)
            mBluetoothReader = new BluetoothReader(this, mHandler); // 用来管理蓝牙的连接
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, getNotification(""));

        if (mBluetoothReader != null) {
            if (mBluetoothReader.getState() == BluetoothReader.STATE_NONE) {
                mBluetoothReader.start();
            }
        }

        return START_NOT_STICKY;
    }

    private Notification getNotification(String msg) {
        Intent notificationIntent = new Intent(this, DataMonitor.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.shop_name))
                .setContentText(msg)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent).build();
    }

    private void passNotification(String msg) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, getNotification(msg));
    }

    public void setUiHandler(Handler handler) {
        mUiHandler = handler;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBluetoothReader != null)
            mBluetoothReader.stop();

        isRunning = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
