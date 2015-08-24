package com.example.blunobasicdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public abstract class BlunoLibrary extends Activity {

	private Context mainContext = this;

	public static String BlunoMacAddr = "D0:39:72:C4:CD:D3";
	public static String BlueSwitchMacAddr_Grey = "D0:39:72:A0:05:B6";
	public static String BlueSwitchMacAddr_White = "78:A5:04:8F:6B:6C";
	// D0:39:72:A0:05:B6 ȸ��
	// 78:A5:04:8F:6B:6C ���

	public abstract void onConectionStateChange(int state);
	public abstract void onSerialReceived(byte data);

	public void serialSend(byte[] theString) {
		if (isBlueSwitchState == connectionStateEnum.isConnected) {
			mSCharacteristic_blueswitch.setValue(theString);
			mBluetoothLeService.writeCharacteristic(mSCharacteristic_blueswitch);
		}
	}

	static class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
	}
	
	

	public int mOperation = BlueSwitchProtocol.NO_OPERATION;
	public BlueSwitchProtocol mBlueSwitchProtocol;

	public static connectionStateEnum isBlunoState;
	public static connectionStateEnum isBlueSwitchState;

	private static BluetoothGattCharacteristic mSCharacteristic_bluno, mModelNumberCharacteristic_bluno,
			mSerialPortCharacteristic_bluno;
	private static BluetoothGattCharacteristic mSCharacteristic_blueswitch, mModelNumberCharacteristic_blueswitch,
			mSerialPortCharacteristic_blueswitch;
	BluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics_bluno = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics_blueswitch = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private LeDeviceListAdapter mLeDeviceListAdapter = null;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning = false;

	public enum connectionStateEnum {
		isNull, isScanning, isToScan, isConnecting, isConnected, isDisconnecting
	};

	public connectionStateEnum mConnectionState = connectionStateEnum.isNull;
	private static final int REQUEST_ENABLE_BT = 1;

	public boolean mConnected = false;

	private final static String TAG = BlunoLibrary.class.getSimpleName();

	public static final String BlunoSerialPortUUID = "0000dfb1-0000-1000-8000-00805f9b34fb";
	public static final String BlueSwitchSerialPortUUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
	public static final String BlunoCommandUUID = "0000dfb2-0000-1000-8000-00805f9b34fb";
	public static final String BlunoModelNumberStringUUID = "00002a24-0000-1000-8000-00805f9b34fb";

	public void onCreateProcess() {
		
		mBlueSwitchProtocol = new BlueSwitchProtocol();
		
		isBlunoState = connectionStateEnum.isNull;
		isBlueSwitchState = connectionStateEnum.isNull;

		if (!initiate()) {
			Toast.makeText(mainContext, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			((Activity) mainContext).finish();
		}

		Intent gattServiceIntent = new Intent(mainContext, BluetoothLeService.class);
		mainContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

		// Initializes and show the scan Device Dialog
		mLeDeviceListAdapter = new LeDeviceListAdapter();
		ListView _scanningListView = (ListView) findViewById(R.id.scanningListView);
		_scanningListView.setAdapter(mLeDeviceListAdapter);
		
		
	}

	public void onResumeProcess() {
		System.out.println("BlUNOActivity onResume");
		// Ensures Bluetooth is enabled on the device. If Bluetooth is not
		// currently enabled,
		// fire an intent to display a dialog asking the user to grant
		// permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				((Activity) mainContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
		mainContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		//mainContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

	}

	public void onPauseProcess() {
		System.out.println("BLUNOActivity onPause");
		scanLeDevice(false);
		mainContext.unregisterReceiver(mGattUpdateReceiver);
		mLeDeviceListAdapter.clear();
		mConnectionState = connectionStateEnum.isToScan;

		if (mBluetoothLeService != null) {
			mBluetoothLeService.disconnect();

			// mBluetoothLeService.close();
		}
		mSCharacteristic_bluno = null;
		mSCharacteristic_blueswitch = null;
	}

	public void onStopProcess() {
		System.out.println("MiUnoActivity onStop");
		if (mBluetoothLeService != null) {
			// mBluetoothLeService.disconnect();
			// mHandler.postDelayed(mDisonnectingOverTimeRunnable, 10000);
			mBluetoothLeService.close();
		}
		mSCharacteristic_bluno = null;
		mSCharacteristic_blueswitch = null;
	}

	public void onDestroyProcess() {
		mainContext.unregisterReceiver(mGattUpdateReceiver);
		mainContext.unbindService(mServiceConnection);
		mBluetoothLeService = null;
	}

	public void onActivityResultProcess(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
			((Activity) mainContext).finish();
			return;
		}
	}

	boolean initiate() {
		// Use this check to determine whether BLE is supported on the device.
		// Then you can
		// selectively disable BLE-related features.
		if (!mainContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			return false;
		}

		// Initializes a Bluetooth adapter. For API level 18 and above, get a
		// reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager = (BluetoothManager) mainContext
				.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			return false;
		}
		return true;
	}

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device. This can be a
	// result of read
	// or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@SuppressLint("DefaultLocale")
		@Override
		public void onReceive(Context context, Intent intent) {
			
			final String action = intent.getAction();
			
			if (BluetoothLeService.ACTION_GATT_CONNECTED_BLUNO.equals(action)) {
				isBlunoState = connectionStateEnum.isConnected;
				onConectionStateChange(1);
				
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED_BLUNO.equals(action)) {
				isBlunoState = connectionStateEnum.isNull;
				onConectionStateChange(2);
				scanLeDevice(true);
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED_BLUNO.equals(action)) {
				getGattServicesBluno(mBluetoothLeService.getSupportedBlunoGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE_BLUNO.equals(action)) {
				if (mSCharacteristic_bluno == mModelNumberCharacteristic_bluno) {
					mSCharacteristic_bluno = mSerialPortCharacteristic_bluno;
					mBluetoothLeService.setCharacteristicNotification(mSCharacteristic_bluno, true);
				} else if (mSCharacteristic_bluno == mSerialPortCharacteristic_bluno) {
					byte[] ddd = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
					mOperation = ddd[0];
					serialSend(mBlueSwitchProtocol.getReadPacket());
				}
			}

			
			if (BluetoothLeService.ACTION_GATT_CONNECTED_BLUESWITCH.equals(action)) {
				isBlueSwitchState = connectionStateEnum.isConnected;
				onConectionStateChange(3);
				
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED_BLUESWITCH.equals(action)) {
				isBlueSwitchState = connectionStateEnum.isNull;
				onConectionStateChange(4);
				scanLeDevice(true);
			}
			else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED_BLUESWITCH.equals(action)) {
				getGattServicesBlueSwitch(mBluetoothLeService.getSupportedBlueSwitchGattServices());
			}
			else if (BluetoothLeService.ACTION_DATA_AVAILABLE_BLUESWITCH.equals(action)) {
				if (mSCharacteristic_blueswitch == mModelNumberCharacteristic_blueswitch) {
					mSCharacteristic_blueswitch = mSerialPortCharacteristic_blueswitch;
					mBluetoothLeService.setCharacteristicNotification(mSCharacteristic_blueswitch, true);
				} else if (mSCharacteristic_blueswitch == mSerialPortCharacteristic_blueswitch) {
					onConectionStateChange(5);
					byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

					if (data != null) {
						if (data.length > 1) {
							if ((byte) (data[1] & 0x80) == BlueSwitchProtocol.READ_DATA_BYTE) {
								serialSend(mBlueSwitchProtocol.getWritePacketForApp(data[1], mOperation));
								onSerialReceived(data[1]);
							}
						}
					}
				}
			}
		}
	};

	void scanLeDevice(final boolean enable) {
		if (enable) {

			System.out.println("mBluetoothAdapter.startLeScan");

			if (mLeDeviceListAdapter != null) {
				mLeDeviceListAdapter.clear();
				mLeDeviceListAdapter.notifyDataSetChanged();
			}

			if (!mScanning) {
				mScanning = true;
				mBluetoothAdapter.startLeScan(mLeScanCallback);
			}
		} else {
			if (mScanning) {
				mScanning = false;
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
			}
		}
	}

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			System.out.println("mServiceConnection onServiceConnected");
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				((Activity) mainContext).finish();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			System.out.println("mServiceConnection onServiceDisconnected");
			mBluetoothLeService = null;
		}
	};

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			((Activity) mainContext).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if(isBlueSwitchState == connectionStateEnum.isConnected && isBlunoState == connectionStateEnum.isConnected) {
						mLeDeviceListAdapter.clear();
						scanLeDevice(false);
						return;
					}
					mLeDeviceListAdapter.addDevice(device);
					mLeDeviceListAdapter.notifyDataSetChanged();
					if (isBlunoState == connectionStateEnum.isNull && device.getAddress().equals(BlunoMacAddr)) {
						isBlunoState = connectionStateEnum.isConnecting;
						mBluetoothLeService.connect(BlunoMacAddr);
					}
					if (isBlueSwitchState == connectionStateEnum.isNull
							&& (device.getAddress().equals(BlueSwitchMacAddr_Grey)
									|| device.getAddress().equals(BlueSwitchMacAddr_White))) {
						isBlueSwitchState = connectionStateEnum.isConnecting;
						if (device.getAddress().equals(BlueSwitchMacAddr_Grey))
							mBluetoothLeService.connect(BlueSwitchMacAddr_Grey);
						if (device.getAddress().equals(BlueSwitchMacAddr_White))
							mBluetoothLeService.connect(BlueSwitchMacAddr_White);
					}
				}
			});
		}
	};

	private void getGattServicesBluno(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		String uuid = null;
		mModelNumberCharacteristic_bluno = null;
		mSerialPortCharacteristic_bluno = null;
		mGattCharacteristics_bluno = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			uuid = gattService.getUuid().toString();
			System.out.println("displayGattServices + uuid=" + uuid);

			List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				uuid = gattCharacteristic.getUuid().toString();
				if (uuid.equals(BlunoModelNumberStringUUID)) {
					mModelNumberCharacteristic_bluno = gattCharacteristic;
					System.out.println(
							"mModelNumberCharacteristic  " + mModelNumberCharacteristic_bluno.getUuid().toString());
				} else if (uuid.equals(BlunoSerialPortUUID)) {
					mSerialPortCharacteristic_bluno = gattCharacteristic;
					System.out.println(
							"mSerialPortCharacteristic  " + mSerialPortCharacteristic_bluno.getUuid().toString());
				}
			}
			mGattCharacteristics_bluno.add(charas);
		}

		if (mModelNumberCharacteristic_bluno == null || mSerialPortCharacteristic_bluno == null) {
			Toast.makeText(mainContext, "Please select DFRobot devices", Toast.LENGTH_SHORT).show();
			mConnectionState = connectionStateEnum.isToScan;
		} else {
			mSCharacteristic_bluno = mModelNumberCharacteristic_bluno;
			mBluetoothLeService.setCharacteristicNotification(mSCharacteristic_bluno, true);
			mBluetoothLeService.readCharacteristic(mSCharacteristic_bluno);
		}

	}

	private void getGattServicesBlueSwitch(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		String uuid = null;
		mModelNumberCharacteristic_blueswitch = null;
		mSerialPortCharacteristic_blueswitch = null;
		mGattCharacteristics_blueswitch = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			uuid = gattService.getUuid().toString();
			System.out.println("displayGattServices + uuid=" + uuid);

			List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				uuid = gattCharacteristic.getUuid().toString();
				if (uuid.equals(BlunoModelNumberStringUUID)) {
					mModelNumberCharacteristic_blueswitch = gattCharacteristic;
					System.out.println("mModelNumberCharacteristic  "
							+ mModelNumberCharacteristic_blueswitch.getUuid().toString());
				} else if (uuid.equals(BlueSwitchSerialPortUUID)) {
					mSerialPortCharacteristic_blueswitch = gattCharacteristic;
					System.out.println(
							"mSerialPortCharacteristic  " + mSerialPortCharacteristic_blueswitch.getUuid().toString());
				}
			}
			mGattCharacteristics_blueswitch.add(charas);
		}

		if (mModelNumberCharacteristic_blueswitch == null || mSerialPortCharacteristic_blueswitch == null) {
			Toast.makeText(mainContext, "Please select DFRobot devices", Toast.LENGTH_SHORT).show();
			mConnectionState = connectionStateEnum.isToScan;
		} else {
			mSCharacteristic_blueswitch = mModelNumberCharacteristic_blueswitch;
			mBluetoothLeService.setCharacteristicNotification(mSCharacteristic_blueswitch, true);
			mBluetoothLeService.readCharacteristic(mSCharacteristic_blueswitch);
		}

	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED_BLUNO);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED_BLUESWITCH);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED_BLUNO);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED_BLUESWITCH);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED_BLUNO);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED_BLUESWITCH);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_BLUNO);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_BLUESWITCH);
		return intentFilter;
	}

	private class LeDeviceListAdapter extends BaseAdapter {
		private ArrayList<BluetoothDevice> mLeDevices;
		private LayoutInflater mInflator;

		public LeDeviceListAdapter() {
			super();
			mLeDevices = new ArrayList<BluetoothDevice>();
			mInflator = ((Activity) mainContext).getLayoutInflater();
		}

		public void addDevice(BluetoothDevice device) {
			if (!mLeDevices.contains(device)) {
				mLeDevices.add(device);
			}
		}

		public BluetoothDevice getDevice(int position) {
			return mLeDevices.get(position);
		}

		public void clear() {
			mLeDevices.clear();
		}

		@Override
		public int getCount() {
			return mLeDevices.size();
		}

		@Override
		public Object getItem(int i) {
			return mLeDevices.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			ViewHolder viewHolder;
			// General ListView optimization code.
			if (view == null) {
				view = mInflator.inflate(R.layout.listitem_device, null);
				viewHolder = new ViewHolder();
				viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
				viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
				System.out.println("mInflator.inflate  getView");
				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			BluetoothDevice device = mLeDevices.get(i);
			final String deviceName = device.getName();
			if (deviceName != null && deviceName.length() > 0)
				viewHolder.deviceName.setText(deviceName);
			else
				viewHolder.deviceName.setText(R.string.unknown_device);
			viewHolder.deviceAddress.setText(device.getAddress());

			return view;
		}
	}
}
