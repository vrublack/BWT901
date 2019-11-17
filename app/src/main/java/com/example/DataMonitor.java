package com.example;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class DataMonitor extends FragmentActivity implements OnClickListener {

    boolean slideAction = false;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothService mBluetoothService = null;
	private String mConnectedDeviceName = null;

	private TextView mTitle;
	private boolean recordStartorStop=false;

	private DataFragment dataFragment;

	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	protected static final String TAG = null;
	private short sOffsetAccX,sOffsetAccY,sOffsetAccZ;

	boolean[] selected;
	String[] SelectItemFields;

	private final Handler mHandler = new Handler() {
		// 匿名内部类写法，实现接口Handler的一些方法
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothService.STATE_CONNECTED:
					mTitle.setText(R.string.title_connected_to);
                    ((Button) findViewById(R.id.BtnRecord)).setEnabled(true);
                    ((Button) findViewById(R.id.BtnRate)).setEnabled(true);
                    ((Button) findViewById(R.id.BtnOutput)).setEnabled(true);
                    mTitle.append(mConnectedDeviceName);
					break;
				case BluetoothService.STATE_CONNECTING:
					mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothService.STATE_LISTEN:
				case BluetoothService.STATE_NONE:
                    ((Button) findViewById(R.id.BtnRecord)).setEnabled(false);
                    ((Button) findViewById(R.id.BtnRate)).setEnabled(false);
                    ((Button) findViewById(R.id.BtnOutput)).setEnabled(false);
                    mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_READ:
				try {
					float [] fData=msg.getData().getFloatArray("Data");
					switch (RunMode){
						case 0:
							switch (iCurrentGroup){
								case 0:
									((TextView)findViewById(R.id.tvNum1)).setText(msg.getData().getString("Date"));
									((TextView)findViewById(R.id.tvNum2)).setText(msg.getData().getString("Time"));
									break;
								case 1:
									((TextView)findViewById(R.id.tvNum1)).setText(String.format("% 10.2fg", fData[0]));
									((TextView)findViewById(R.id.tvNum2)).setText(String.format("% 10.2fg", fData[1]));
									((TextView)findViewById(R.id.tvNum3)).setText(String.format("% 10.2fg", fData[2]));
									((TextView)findViewById(R.id.tvNum4)).setText(String.format("% 10.2f℃", fData[17]));
									break;
								case 2:
									((TextView)findViewById(R.id.tvNum1)).setText(String.format("% 10.2f°/s", fData[3]));
									((TextView)findViewById(R.id.tvNum2)).setText(String.format("% 10.2f°/s", fData[4]));
									((TextView)findViewById(R.id.tvNum3)).setText(String.format("% 10.2f°/s", fData[5]));
									((TextView)findViewById(R.id.tvNum4)).setText(String.format("% 10.2f℃", fData[17]));
									break;
								case 3:
									((TextView)findViewById(R.id.tvNum1)).setText(String.format("% 10.2f°", fData[6]));
									((TextView)findViewById(R.id.tvNum2)).setText(String.format("% 10.2f°", fData[7]));
									((TextView)findViewById(R.id.tvNum3)).setText(String.format("% 10.2f°", fData[8]));
									((TextView)findViewById(R.id.tvNum4)).setText(String.format("% 10.2f℃", fData[17]));
									break;
								case 4://磁场
									((TextView)findViewById(R.id.tvNum1)).setText(String.format("% 10.0f", fData[9]));
									((TextView)findViewById(R.id.tvNum2)).setText(String.format("% 10.0f", fData[10]));
									((TextView)findViewById(R.id.tvNum3)).setText(String.format("% 10.0f", fData[11]));
									((TextView)findViewById(R.id.tvNum4)).setText(String.format("% 10.2f℃", fData[17]));
									break;
								case 5://端口
									((TextView)findViewById(R.id.tvNum1)).setText(String.format("% 10.2f", fData[12]));
									((TextView)findViewById(R.id.tvNum2)).setText(String.format("% 10.2f", fData[13]));
									((TextView)findViewById(R.id.tvNum3)).setText(String.format("% 10.2f", fData[14]));
									((TextView)findViewById(R.id.tvNum4)).setText(String.format("% 10.2f", fData[15]));
									break;
								case 6://气压
									((TextView)findViewById(R.id.tvNum1)).setText(String.format("% 10.2fPa", fData[16]));
									((TextView)findViewById(R.id.tvNum2)).setText(String.format("% 10.2fm", fData[17]));
									break;
								case 7://经纬度
									((TextView)findViewById(R.id.tvNum1)).setText(String.format("% 14.6f°", fData[18]));
									((TextView)findViewById(R.id.tvNum2)).setText(String.format("% 14.6f°", fData[19]));
									break;
								case 8://地速
									((TextView)findViewById(R.id.tvNum1)).setText(String.format("% 10.2m", fData[20]));
									((TextView)findViewById(R.id.tvNum2)).setText(String.format("% 10.2°", fData[21]));
									((TextView)findViewById(R.id.tvNum3)).setText(String.format("% 10.2m/s", fData[22]));
									break;
								case 9://四元数
									((TextView)findViewById(R.id.tvNum1)).setText(String.format("% 7.3f", fData[23]));
									((TextView)findViewById(R.id.tvNum2)).setText(String.format("% 7.3f", fData[24]));
									((TextView)findViewById(R.id.tvNum3)).setText(String.format("% 7.3f", fData[25]));
									((TextView)findViewById(R.id.tvNum4)).setText(String.format("% 7.3f", fData[26]));
									break;
								case 10:
									((TextView)findViewById(R.id.tvNum1)).setText(String.format("% 5.0f", fData[27]));
									((TextView)findViewById(R.id.tvNum2)).setText(String.format("% 7.1f", fData[28]));
									((TextView)findViewById(R.id.tvNum3)).setText(String.format("% 7.1f", fData[29]));
									((TextView)findViewById(R.id.tvNum4)).setText(String.format("% 7.1f", fData[30]));
									break;
							}
							break;
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
				break;
			case MESSAGE_DEVICE_NAME:
				mConnectedDeviceName = msg.getData().getString("device_name");
				Toast.makeText(getApplicationContext(),"Connected to " + mConnectedDeviceName,Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	private static final int REQUEST_CONNECT_DEVICE = 1;
    private int mSampleRate;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mTitle = (TextView) findViewById(R.id.title_right_text);
		SelectFragment(0);

		selected = new boolean[]{false,true,true,true,false,false,false,false,false,false,false};
		SelectItemFields = new String[]{getString(R.string.time),getString(R.string.acc),getString(R.string.angv),getString(R.string.ang),
				getString(R.string.magn),getString(R.string.port), getString(R.string.pressure), getString(R.string.long_lat), getString(R.string.speed), getString(R.string.quaternion), getString(R.string.val16)};

		try {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) {
				Toast.makeText(this, getString(R.string.msg1), Toast.LENGTH_LONG).show();
				//finish();
				return;
			}

			if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
			if (mBluetoothService == null)
				mBluetoothService = new BluetoothService(this, mHandler); // 用来管理蓝牙的连接
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		}
		catch (Exception err){}
    }

	public void onClickedBTSet(View v){
		try {
			if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
			if (mBluetoothService == null)
				mBluetoothService = new BluetoothService(this, mHandler); // 用来管理蓝牙的连接
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		}
		catch (Exception err){}
	}
	@SuppressLint("NewApi")
	private void SelectFragment(int Index) {
		// TODO Auto-generated method stub
		android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction transaction = manager.beginTransaction();

		if (dataFragment==null) {dataFragment = new DataFragment();transaction.add(R.id.id_content, dataFragment);}
		transaction.show(dataFragment);
		transaction.commit();
	}

	@Override
	public void onStart() {
		super.onStart();
		try{
			GetSelectedFields();
		}
		catch (Exception err){}

	}

	public synchronized void onResume() {
		super.onResume();

		if (mBluetoothService != null) {
			if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
				mBluetoothService.start();
			}
		}
	}

	@Override
	public synchronized void onPause() {
		super.onPause();

	}

	@Override
	public void onStop() {
		super.onStop();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mBluetoothService != null) mBluetoothService.stop();
	}
	public BluetoothDevice device;
	// 利用startActivityForResult 和 onActivityResult在activity间传递数据
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);// Get the device MAC address
				device = mBluetoothAdapter.getRemoteDevice(address);// Get the BLuetoothDevice object
				mBluetoothService.connect(device);// Attempt to connect to the device
			}
			break;
		}
	}

	public void RefreshButtonStatus(){
		if (selected[0]) ((TextView)findViewById(R.id.button0)).setTextColor(Color.BLACK); else ((TextView)findViewById(R.id.button0)).setTextColor(Color.GRAY);
		if (selected[1]) ((TextView)findViewById(R.id.button1)).setTextColor(Color.BLACK); else ((TextView)findViewById(R.id.button1)).setTextColor(Color.GRAY);
		if (selected[2]) ((TextView)findViewById(R.id.button2)).setTextColor(Color.BLACK); else ((TextView)findViewById(R.id.button2)).setTextColor(Color.GRAY);
		if (selected[3]) ((TextView)findViewById(R.id.button3)).setTextColor(Color.BLACK); else ((TextView)findViewById(R.id.button3)).setTextColor(Color.GRAY);
		if (selected[4]) ((TextView)findViewById(R.id.button4)).setTextColor(Color.BLACK); else ((TextView)findViewById(R.id.button4)).setTextColor(Color.GRAY);
		if (selected[5]) ((TextView)findViewById(R.id.button5)).setTextColor(Color.BLACK); else ((TextView)findViewById(R.id.button5)).setTextColor(Color.GRAY);
		if (selected[6]) ((TextView)findViewById(R.id.button6)).setTextColor(Color.BLACK); else ((TextView)findViewById(R.id.button6)).setTextColor(Color.GRAY);
		if (selected[7]) ((TextView)findViewById(R.id.button7)).setTextColor(Color.BLACK); else ((TextView)findViewById(R.id.button7)).setTextColor(Color.GRAY);
		if (selected[8]) ((TextView)findViewById(R.id.button8)).setTextColor(Color.BLACK); else ((TextView)findViewById(R.id.button8)).setTextColor(Color.GRAY);
		if (selected[9]) ((TextView)findViewById(R.id.button9)).setTextColor(Color.BLACK); else ((TextView)findViewById(R.id.button9)).setTextColor(Color.GRAY);
		if (selected[10]) ((TextView)findViewById(R.id.buttonA)).setTextColor(Color.BLACK); else ((TextView)findViewById(R.id.buttonA)).setTextColor(Color.GRAY);
	}

	public void GetSelectedFields(){
		SharedPreferences mySharedPreferences= getSharedPreferences("Output", Activity.MODE_PRIVATE);
		try{
			int iOut = Integer.parseInt(mySharedPreferences.getString("Out", "15"));
			for (int i=0;i<selected.length;i++){
				selected[i]=((iOut>>i)&0x01)==0x01;
			}
			RefreshButtonStatus();
		}
		catch (Exception err){}
	}

    public void OnClickConfig(View v) {

		GetSelectedFields();
		new AlertDialog.Builder(this)
				.setTitle(R.string.msg2)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMultiChoiceItems(SelectItemFields, selected, new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i, boolean b) {
						selected[i] = b;
					}
				})
				.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						byte[] buffer = new byte[5];
						buffer[0] = (byte) 0xff;
						buffer[1] = (byte) 0xaa;
						buffer[2] = (byte) 0x02;
						short sOut = 0;
						for (int i = 0; i < selected.length; i++) {
							if (selected[i]) sOut |= 0x01 << i;
						}
						buffer[3] = (byte) (sOut&0xff);
						buffer[4] = (byte) (sOut>>8);
						SharedPreferences mySharedPreferences= getSharedPreferences("Output",Activity.MODE_PRIVATE);
						SharedPreferences.Editor editor = mySharedPreferences.edit();
						editor.putString("Out",String.format("%d",sOut));
						editor.commit();
						RefreshButtonStatus();
						mBluetoothService.Send(buffer);
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();

	}

    public void OnClickRate(View v) {

	    final String[] options = new String[] {"0.1Hz", "0.5Hz", "1Hz", "2Hz", "5Hz", "10Hz", "20Hz", "50Hz", "100Hz", "200Hz", "Single", "No output"};
        SharedPreferences mySharedPreferences= getSharedPreferences("Output", Activity.MODE_PRIVATE);
        mSampleRate = Integer.parseInt(mySharedPreferences.getString("Rate", "5"));

        new AlertDialog.Builder(this)
                .setTitle(R.string.sampling_rate)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(options, mSampleRate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mSampleRate = i;
                    }
                })
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        byte[] buffer = new byte[5];
                        buffer[0] = (byte) 0xff;
                        buffer[1] = (byte) 0xaa;
                        buffer[2] = (byte) 0x03;
                        buffer[3] = (byte) (mSampleRate + 1);
                        buffer[4] = (byte) 0x00;
                        SharedPreferences mySharedPreferences= getSharedPreferences("Output",Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = mySharedPreferences.edit();
                        editor.putString("Rate", Integer.toString(mSampleRate));
                        editor.commit();
                        mBluetoothService.Send(buffer);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    int RunMode = 0;

	int iCurrentGroup=3;
	public void ControlClick(View v) {
		switch (v.getId()) {
			case R.id.button0:
				if (selected[0]==false) return;
				iCurrentGroup=0;
				((TextView)findViewById(R.id.tvDataName1)).setText(R.string.date);((TextView)findViewById(R.id.tvNum1)).setText("2015-1-1");
				((TextView)findViewById(R.id.tvDataName2)).setText(R.string.time_);((TextView)findViewById(R.id.tvNum2)).setText("00:00:00.0");
				((TextView)findViewById(R.id.tvDataName3)).setText("");((TextView)findViewById(R.id.tvNum3)).setText("");
				((TextView)findViewById(R.id.tvDataName4)).setText("");((TextView)findViewById(R.id.tvNum4)).setText("");
				break;
			case R.id.button1:
				if (selected[1]==false) return;
				iCurrentGroup=1;
				((TextView)findViewById(R.id.tvDataName1)).setText(String.format("X%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum1)).setText("0");
				((TextView)findViewById(R.id.tvDataName2)).setText(String.format("Y%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum2)).setText("0");
				((TextView)findViewById(R.id.tvDataName3)).setText(String.format("Z%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum3)).setText("0");
				((TextView)findViewById(R.id.tvDataName4)).setText(String.format("%s：", getString(R.string.degrees)));((TextView)findViewById(R.id.tvNum4)).setText("25℃");
				break;
			case R.id.button2:
				if (selected[2]==false) return;
				iCurrentGroup=2;
				((TextView)findViewById(R.id.tvDataName1)).setText(String.format("X%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum1)).setText("0");
				((TextView)findViewById(R.id.tvDataName2)).setText(String.format("Y%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum2)).setText("0");
				((TextView)findViewById(R.id.tvDataName3)).setText(String.format("Z%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum3)).setText("0");
				((TextView)findViewById(R.id.tvDataName4)).setText(String.format("%s：", getString(R.string.degrees)));((TextView)findViewById(R.id.tvNum4)).setText("25℃");
				break;
			case R.id.button3:
				if (selected[3]==false) return;
				iCurrentGroup=3;
				((TextView)findViewById(R.id.tvDataName1)).setText(String.format("X%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum1)).setText("0");
				((TextView)findViewById(R.id.tvDataName2)).setText(String.format("Y%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum2)).setText("0");
				((TextView)findViewById(R.id.tvDataName3)).setText(String.format("Z%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum3)).setText("0");
				((TextView)findViewById(R.id.tvDataName4)).setText(String.format("%s：", getString(R.string.degrees)));((TextView)findViewById(R.id.tvNum4)).setText("25℃");
				break;
			case R.id.button4:
				if (selected[4]==false) return;
				iCurrentGroup=4;
				((TextView)findViewById(R.id.tvDataName1)).setText(String.format("X%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum1)).setText("0");
				((TextView)findViewById(R.id.tvDataName2)).setText(String.format("Y%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum2)).setText("0");
				((TextView)findViewById(R.id.tvDataName3)).setText(String.format("Z%s：", getString(R.string.axis)));((TextView)findViewById(R.id.tvNum3)).setText("0");
				((TextView)findViewById(R.id.tvDataName4)).setText(String.format("%s：", getString(R.string.degrees)));((TextView)findViewById(R.id.tvNum4)).setText("25℃");
				break;
			case R.id.button5:
				if (selected[5]==false) return;
				iCurrentGroup=5;
				((TextView)findViewById(R.id.tvDataName1)).setText("D0：");((TextView)findViewById(R.id.tvNum1)).setText("0");
				((TextView)findViewById(R.id.tvDataName2)).setText("D1：");((TextView)findViewById(R.id.tvNum2)).setText("0");
				((TextView)findViewById(R.id.tvDataName3)).setText("D2：");((TextView)findViewById(R.id.tvNum3)).setText("0");
				((TextView)findViewById(R.id.tvDataName4)).setText("D3：");((TextView)findViewById(R.id.tvNum4)).setText("0");
				break;
			case R.id.button6:
				if (selected[6]==false) return;
				iCurrentGroup=6;
				((TextView)findViewById(R.id.tvDataName1)).setText(String.format("%s：", getString(R.string.pressure)));((TextView)findViewById(R.id.tvNum1)).setText("0");
				((TextView)findViewById(R.id.tvDataName2)).setText(String.format("%s：", getString(R.string.altitude)));((TextView)findViewById(R.id.tvNum2)).setText("0");
				((TextView)findViewById(R.id.tvDataName3)).setText("");((TextView)findViewById(R.id.tvNum3)).setText("");
				((TextView)findViewById(R.id.tvDataName4)).setText("");((TextView)findViewById(R.id.tvNum4)).setText("");
				break;
			case R.id.button7:
				if (selected[7]==false) return;
				iCurrentGroup=7;
				((TextView)findViewById(R.id.tvDataName1)).setText(String.format("%s：", getString(R.string.longitude)));((TextView)findViewById(R.id.tvNum1)).setText("0");
				((TextView)findViewById(R.id.tvDataName2)).setText(String.format("%s：", getString(R.string.latitude))); ((TextView)findViewById(R.id.tvNum2)).setText("0");
				((TextView)findViewById(R.id.tvDataName3)).setText("");((TextView)findViewById(R.id.tvNum3)).setText("");
				((TextView)findViewById(R.id.tvDataName4)).setText("");((TextView)findViewById(R.id.tvNum4)).setText("");
				break;
			case R.id.button8:
				if (selected[8]==false) return;
				iCurrentGroup=8;
				((TextView)findViewById(R.id.tvDataName1)).setText(String.format("%s：", getString(R.string.speed)));((TextView)findViewById(R.id.tvNum1)).setText("0");
				((TextView)findViewById(R.id.tvDataName2)).setText(String.format("%s：", getString(R.string.course)));((TextView)findViewById(R.id.tvNum2)).setText("0");
				((TextView)findViewById(R.id.tvDataName3)).setText("");((TextView)findViewById(R.id.tvNum3)).setText("");
				((TextView)findViewById(R.id.tvDataName4)).setText("");((TextView)findViewById(R.id.tvNum4)).setText("");
				break;
			case R.id.button9:
				if (selected[9]==false) return;
				iCurrentGroup=9;
				((TextView)findViewById(R.id.tvDataName1)).setText("q0：");((TextView)findViewById(R.id.tvNum1)).setText("0");
				((TextView)findViewById(R.id.tvDataName2)).setText("q1：");((TextView)findViewById(R.id.tvNum2)).setText("0");
				((TextView)findViewById(R.id.tvDataName3)).setText("q2：");((TextView)findViewById(R.id.tvNum3)).setText("0");
				((TextView)findViewById(R.id.tvDataName4)).setText("q3：");((TextView)findViewById(R.id.tvNum4)).setText("0");
				break;
			case R.id.buttonA:
				if (selected[10]==false) return;
				iCurrentGroup=10;
				((TextView)findViewById(R.id.tvDataName1)).setText(String.format("%s：", getString(R.string.val16)));((TextView)findViewById(R.id.tvNum1)).setText("0");
				((TextView)findViewById(R.id.tvDataName2)).setText("PDOP：");((TextView)findViewById(R.id.tvNum2)).setText("0");
				((TextView)findViewById(R.id.tvDataName3)).setText("HDOP：");((TextView)findViewById(R.id.tvNum3)).setText("0");
				((TextView)findViewById(R.id.tvDataName4)).setText("VDOP：");((TextView)findViewById(R.id.tvNum4)).setText("0");
				break;
		}
		((Button) findViewById(R.id.button0)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) findViewById(R.id.button1)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) findViewById(R.id.button2)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) findViewById(R.id.button3)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) findViewById(R.id.button4)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) findViewById(R.id.button5)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) findViewById(R.id.button6)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) findViewById(R.id.button7)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) findViewById(R.id.button8)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) findViewById(R.id.button9)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) findViewById(R.id.buttonA)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) findViewById(R.id.buttonB)).setBackgroundResource(R.drawable.ic_preference_single_normal);
		((Button) v).setBackgroundResource(R.drawable.ic_preference_single_pressed);
	}
	public void onRecordBtnClick(View v) {
		if (!this.recordStartorStop)
		{
            this.recordStartorStop = true;
            mBluetoothService.setRecord(true);
            ((Button) findViewById(R.id.BtnRecord)).setText(R.string.stop);
            ((Button) findViewById(R.id.BtnRecord)).setTextColor(Color.RED);
		}
		else{
			this.recordStartorStop = false;
			mBluetoothService.setRecord(false);
			((Button)findViewById(R.id.BtnRecord)).setText(R.string.record);
			((Button)v).setTextColor(Color.WHITE);
			new AlertDialog.Builder(this)
					.setTitle(R.string.prompt)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage(String.format("%s：%s\n%s？", getString(R.string.msg3), mBluetoothService.myFile.path.toString(), getString(R.string.msg4)))
					.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							Uri uri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", mBluetoothService.myFile.path);
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setData(uri);
							intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
							startActivity(intent);
						}
					})
					.setNegativeButton(R.string.cancel, null)
					.show();
		}
	}

    @Override
	public void onClick(View v) {

	}
}
