package com.witsensor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Queue;

//import org.apache.http.util.EncodingUtils;

public class BluetoothReader {

	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String NAME = "BluetoothData";

	private final BluetoothAdapter mAdapter;
	private Context mContext;
	private final Handler mHandler;
	private AcceptThread mAcceptThread;// 请求连接的监听进程
	private ConnectThread mConnectThread;// 连接一个设备的进程
	public ConnectedThread mConnectedThread;// 已经连接之后的管理进程
	private int mState;// 当前状态
    private int reconnectTries;
    private final static int MAX_RECONNECT_TRIES = 5;

	// 指明连接状态的常量
	public static final int STATE_NONE = 0;
	public static final int STATE_LISTEN = 1;
	public static final int STATE_CONNECTING = 2;
	public static final int STATE_CONNECTED = 3;
    public static final int STATE_RECONNECTING = 4;

	private Queue<Byte> queueBuffer = new LinkedList<Byte>();

	private byte[] packBuffer = new byte[11];

	BluetoothDevice mBluetoothDevice;


	public BluetoothReader(Context context, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
		mContext = context;
	}
	public void Send(byte[] buffer){
		if (mState==STATE_CONNECTED)
			mConnectedThread.write(buffer);
	}
	private synchronized void setState(int state) {
		mState = state;
		// Give the new state to the Handler so the UI Activity can update
		mHandler.obtainMessage(DataMonitor.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	public synchronized int getState() {
		return mState;
	}

	public synchronized void start() {

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Start the thread to listen on a BluetoothServerSocket
		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}
		setState(STATE_LISTEN);
	}

	public synchronized void connect(BluetoothDevice device) {
	    reconnectTries = 0;

		// Cancel any thread currently running a connection
		disconnect();

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);

		mBluetoothDevice = device;
	}

    private synchronized void reconnect() {
	    if (mBluetoothDevice == null)
	        return;
	    if (reconnectTries >= MAX_RECONNECT_TRIES)
	        return;
	    reconnectTries++;
	    setState(STATE_RECONNECTING);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// Cancel any thread currently running a connection
		disconnect();

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(mBluetoothDevice);
		mConnectThread.start();
		mState = STATE_CONNECTING;
    }

	/**
	 * Disconnects from connected device if connected
	 */
	public synchronized void disconnect() {
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
	}


    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel the accept thread because we only want to connect to one device
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		Message msg = mHandler.obtainMessage(DataMonitor.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString("device_name", device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}


	public synchronized void stop() {

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		setState(STATE_NONE);
	}

	private void connectionFailed() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(DataMonitor.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString("toast", mContext.getString(R.string.connect_failed));
		msg.setData(bundle);
		mHandler.sendMessage(msg);
        //reconnect();
	}

	private void connectionLost() {
		setState(STATE_LISTEN);
		Message msg = mHandler.obtainMessage(DataMonitor.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString("toast", "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
        //reconnect();
    }

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted (or until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;
			// Create a new listening server socket
			try {
				tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} 
			catch (IOException e) {}
			mmServerSocket = tmp;
		}

		public void run() {
			setName("AcceptThread");
			Log.d(BluetoothReader.class.getCanonicalName(), "AcceptThread");
			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (mState != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a successful connection or an exception
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (BluetoothReader.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:// Situation normal. Start the connected thread.							
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate new socket.
							try {
								socket.close();
							} 
							catch (IOException e) {
                                e.printStackTrace();
                            }
							break;
						}
					}
				}
			}

		}

		public void cancel() {

			try {
				mmServerSocket.close();
			} 
			catch (IOException e) {
                e.printStackTrace();
            }
		}
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);// Get a BluetoothSocket for a connection with the given BluetoothDevice
			} 
			catch (IOException e) {
                e.printStackTrace();
            }
			mmSocket = tmp;
		}

		public void run() {

			setName("ConnectThread");
			mAdapter.cancelDiscovery();// Always cancel discovery because it will slow down a connection

			// Make a connection to the BluetoothSocket
			try {				
				mmSocket.connect();// This is a blocking call and will only return on a successful connection or an exception
			} 
			catch (IOException e) {
                e.printStackTrace();
                connectionFailed();
				try {
					mmSocket.close();
				} catch (IOException e2) {
                    e.printStackTrace();
                }
				
				return;
			}
			
			synchronized (BluetoothReader.this) {// Reset the ConnectThread because we're done
				mConnectThread = null;
			}			
			connected(mmSocket, mmDevice);// Start the connected thread
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
                e.printStackTrace();
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {

			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
                e.printStackTrace();
            }

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		
		private float [] fData=new float[31];
		private String strDate,strTime;
		public void run() {
			byte[] tempInputBuffer = new byte[1024];
			int acceptedLen = 0;
			byte sHead;
			// Keep listening to the InputStream while connected
			long lLastTime = System.currentTimeMillis(); // 获取开始时间
			while (true) {

				try {
					// 每次对inputBuffer做覆盖处理
					
					acceptedLen = mmInStream.read(tempInputBuffer);
					// Log.d("BTL",""+acceptedLen);
					for (int i = 0; i < acceptedLen; i++) queueBuffer.add(tempInputBuffer[i]);// 从缓冲区读取到的数据，都存到队列里
					

					while (queueBuffer.size() >= 11) {						
						if (queueBuffer.poll() != 0x55) continue;// peek()返回对首但不删除 poll 移除并返回
							sHead = queueBuffer.poll();
							for (int j = 0; j < 9; j++) packBuffer[j] = queueBuffer.poll();
							switch (sHead) {//
								case 0x50:
									int ms = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff));
									strDate = String.format("20%02d-%02d-%02d",packBuffer[0],packBuffer[1],packBuffer[2]);
									strTime = String.format("\t%02d:%02d:%02d.%03d\t",packBuffer[3],packBuffer[4],packBuffer[5],ms);
									RecordData(sHead,strDate+strTime);
									break;
								case 0x51:
									fData[0] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f * 16;
									fData[1] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 32768.0f * 16;
									fData[2] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 32768.0f * 16;
									fData[17] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
									RecordData(sHead,String.format("%.4f\t", fData[0])+String.format("%.4f\t", fData[1])+String.format("%.4f\t", fData[2]));
									break;
								case 0x52:
									fData[3] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f * 2000;
									fData[4] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 32768.0f * 2000;
									fData[5] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 32768.0f * 2000;
									fData[17] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
									RecordData(sHead,String.format("%.4f\t", fData[3])+String.format("%.4f\t", fData[4])+String.format("%.4f\t", fData[5]));
									break;
								case 0x53:
									fData[6] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f * 180;
									fData[7] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 32768.0f * 180;
									fData[8] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 32768.0f * 180;
									fData[17] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
									RecordData(sHead,String.format("%.4f\t", fData[6])+String.format("%.4f\t", fData[7])+String.format("%.4f\t", fData[8]));
									break;
								case 0x54://磁场
									fData[9] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff));
									fData[10] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff));
									fData[11] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff));
									fData[17] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
									RecordData(sHead,String.format("%.4f\t", fData[9])+String.format("%.4f\t", fData[10])+String.format("%.4f\t", fData[11]));
									break;
								case 0x55://端口
									fData[12] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff));
									fData[13] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff));
									fData[14] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff));
									fData[15] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff));
									RecordData(sHead,String.format("%.4f\t", fData[12])+String.format("%.4f\t", fData[13])+String.format("%.4f\t", fData[14])+String.format("%.4f\t", fData[15]));
									break;
								case 0x56://气压、高度
									fData[16] = ((((long) packBuffer[3]) << 24) |(((long) packBuffer[2]) << 16) |(((long) packBuffer[1]) << 8) | (((long) packBuffer[0])));
									fData[17] = ((((long) packBuffer[7]) << 24) |(((long) packBuffer[6]) << 16) |(((long) packBuffer[5]) << 8) | (((long) packBuffer[4])));
									fData[17]/=100;
									RecordData(sHead,String.format("%.4f\t", fData[16])+String.format("%.4f\t", fData[17]));;
									break;
								case 0x57://经纬度
									long Longitude = ((((long) packBuffer[3]) << 24) |(((long) packBuffer[2]) << 16) |(((long) packBuffer[1]) << 8) | (((long) packBuffer[0])));
									fData[18]=(float) ((float)Longitude / 10000000 + ((float)(Longitude % 10000000) / 100000.0 / 60.0));
									long Latitude = ((((long) packBuffer[7]) << 24) |(((long) packBuffer[6]) << 16) |(((long) packBuffer[5]) << 8) | (((long) packBuffer[4])));
									fData[19]=(float) ((float)Latitude / 10000000 + ((float)(Latitude % 10000000) / 100000.0 / 60.0));
									RecordData(sHead,String.format("%.4f\t", fData[18])+String.format("%.4f\t", fData[19]));;
									break;
								case 0x58://海拔、航向、地速
									fData[20] = ((((long) packBuffer[3]) << 24) |(((long) packBuffer[2]) << 16) |(((long) packBuffer[1]) << 8) | (((long) packBuffer[0])))/10;
									fData[21]=((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff))/10;
									fData[22]=((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff))/1000;
									RecordData(sHead,String.format("%.4f\t", fData[20])+String.format("%.4f\t", fData[21])+String.format("%.4f\t", fData[22]));;
									break;
								case 0x59://四元数
									fData[23] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f;
									fData[24] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff))/32768.0f;
									fData[25] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff))/32768.0f;
									fData[26] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff))/32768.0f;
									RecordData(sHead,String.format("%.4f\t", fData[23])+String.format("%.4f\t", fData[24])+String.format("%.4f\t", fData[25])+String.format("%.4f\t", fData[26]));
									break;
								case 0x5a://卫星数
									fData[27] = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 32768.0f;
									fData[28] = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff))/32768.0f;
									fData[29] = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff))/32768.0f;
									fData[30] = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff))/32768.0f;
									RecordData(sHead,String.format("%.4f\t", fData[27])+String.format("%.4f\t", fData[28])+String.format("%.4f\t", fData[29])+String.format("%.4f\t", fData[30]));
									break;
							}//switch
					}//while (queueBuffer.size() >= 11)

					long lTimeNow = System.currentTimeMillis(); // 获取开始时间
					if (lTimeNow - lLastTime > 80) {
						lLastTime = lTimeNow;
						Message msg = mHandler.obtainMessage(DataMonitor.MESSAGE_READ);
						Bundle bundle = new Bundle();
						bundle.putFloatArray("Data", fData);
						bundle.putString("Date", strDate);
						bundle.putString("Time", strTime);
						msg.setData(bundle);
						mHandler.sendMessage(msg);
					}

				} catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
					break;
				}
			}
		}

		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
				mHandler.obtainMessage(DataMonitor.MESSAGE_WRITE, -1, -1,buffer).sendToTarget();// Share the sent message back to the UI Activity
			} catch (IOException e) {
                e.printStackTrace();
            }
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
                e.printStackTrace();
            }
		}
	}
	MyFile myFile;
	
	private short IDSave=0;
	private short IDNow;
	private int SaveState=-1;
	private int sDataSave=0;

	public void RecordData(byte ID,String str) throws IOException
	{
		boolean Repeat=false;
		short sData=(short) (0x01<<(ID&0x0f));
		if (((IDNow&sData)==sData)&&(sData<sDataSave)) {IDSave=IDNow;	IDNow=sData;Repeat=true;}		
		else IDNow|=sData;
		sDataSave = sData;
		switch (SaveState) {
		case 0:
		    if (myFile != null)
			    myFile.Close();
			SaveState = -1;
			break;
		case 1:
			String fname = generateFname();
			myFile = new MyFile(new File(mContext.getExternalFilesDir(null), generateFname()));
			DateFormat formatter = SimpleDateFormat.getDateTimeInstance();
			Date curDate = new Date(System.currentTimeMillis());
			String s=mContext.getString(R.string.start_time)+formatter.format(curDate)+"\n" ;
			if ((IDSave&0x02)>0) s+= String.format("%sX\t%<sY\t%<sZ\t", mContext.getString(R.string.acc));
			if ((IDSave&0x04)>0) s+= String.format("%sX\t%<sY\t%<sZ\t", mContext.getString(R.string.angv));
			if ((IDSave&0x08)>0) s+= String.format("%sX\t%<sY\t%<sZ\t", mContext.getString(R.string.ang));
			if ((IDSave&0x10)>0) s+= String.format("%sX\t$<sY\t%<sZ\t", mContext.getString(R.string.magn));
			if ((IDSave&0x20)>0) s+= String.format("%s0\t%<s1\t%<s2\t%<s3\t", mContext.getString(R.string.port));
			if ((IDSave&0x40)>0) s+= String.format("%s\t%s\t", mContext.getString(R.string.pressure), mContext.getString(R.string.height));
			if ((IDSave&0x80)>0) s+= String.format("%s\t%s\t", mContext.getString(R.string.longitude), mContext.getString(R.string.latitude));
			if ((IDSave&0x100)>0) s+= String.format("%s\t%s\t%s\t", mContext.getString(R.string.altitude), mContext.getString(R.string.course), mContext.getString(R.string.speed));
			if ((IDSave&0x200)>0) s+="q0\tq1\tq2\tq3\t";
			if ((IDSave&0x400)>0) s+= String.format("%s\tPDOP\tHDOP\tVDOP\t", mContext.getString(R.string.satellites));
			myFile.Write(s+"\r\n");
			if (Repeat)  {myFile.Write(str);SaveState = 2;}
			break;
		case 2:
			if (Repeat) myFile.Write("  \r\n");
			myFile.Write(str);
			break;
		case -1:
			break;
		default:
			break;
		} 		
	}

	private String generateFname() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
		Date date = new Date();
		return "Recording__" + dateFormat.format(new Date()) + ".txt";
	}

	public void setRecord(boolean record)
	{
		if (record) SaveState = 1;
		else SaveState = 0;
	}
}
class MyFile{
	FileOutputStream fout;
	File path;
	public MyFile(File file) throws FileNotFoundException{
	    this.path = file;
		fout = new FileOutputStream(file ,false);
	}
	public void Write( String str) throws IOException {		
			byte[] bytes = str.getBytes();
			fout.write(bytes);		
	}
	public void Close() throws IOException{
		fout.close();
		fout.flush();	
	}
}