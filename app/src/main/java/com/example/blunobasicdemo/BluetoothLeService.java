/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.blunobasicdemo;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.List;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothGatt mBlunoBluetoothGatt;
    BluetoothGatt mBlueSwitchBluetoothGatt;
    public String mBluetoothDeviceAddress;
    
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public int mConnectionState = STATE_DISCONNECTED;

    
    //To tell the onCharacteristicWrite call back function that this is a new characteristic, 
    //not the Write Characteristic to the device successfully.
    private static final int WRITE_NEW_CHARACTERISTIC = -1;
    //define the limited length of the characteristic.
    private static final int MAX_CHARACTERISTIC_LENGTH = 17;
    //Show that Characteristic is writing or not.
    private boolean mIsWritingCharacteristic=false;

    //class to store the Characteristic and content string push into the ring buffer.
    private class BluetoothGattCharacteristicHelper{
    	BluetoothGattCharacteristic mCharacteristic;
    	byte[] mCharacteristicValue;
    	BluetoothGattCharacteristicHelper(BluetoothGattCharacteristic characteristic, byte[] characteristicValue){
    		mCharacteristic=characteristic;
    		mCharacteristicValue=characteristicValue;
    	}
    }
    //ring buffer
    private RingBuffer<BluetoothGattCharacteristicHelper> mCharacteristicRingBuffer = new RingBuffer<BluetoothGattCharacteristicHelper>(8);
    
    public final static String ACTION_GATT_CONNECTED_BLUNO =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED_BLUNO";
    public final static String ACTION_GATT_CONNECTED_BLUESWITCH =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED_BLUESWITCH";
    public final static String ACTION_GATT_DISCONNECTED_BLUNO =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_BLUNO";
    public final static String ACTION_GATT_DISCONNECTED_BLUESWITCH =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_BLUESWITCH";
    public final static String ACTION_GATT_SERVICES_DISCOVERED_BLUNO =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED_BLUNO";
    public final static String ACTION_GATT_SERVICES_DISCOVERED_BLUESWITCH =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED_BLUESWITCH";
    public final static String ACTION_DATA_AVAILABLE_BLUNO =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE_BLUNO";
    public final static String ACTION_DATA_AVAILABLE_BLUESWITCH =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE_BLUESWITCH";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
//    public final static UUID UUID_HEART_RATE_MEASUREMENT =
//            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            
            System.out.println("BluetoothGattCallback----onConnectionStateChange"+newState);
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                
                if(gatt.getDevice().getAddress().equals(BlunoLibrary.BlunoMacAddr))
            		broadcastUpdate(ACTION_GATT_CONNECTED_BLUNO);
            	
            	if(gatt.getDevice().getAddress().equals(BlunoLibrary.BlueSwitchMacAddr_Grey) ||
            			gatt.getDevice().getAddress().equals(BlunoLibrary.BlueSwitchMacAddr_White))
            		broadcastUpdate(ACTION_GATT_CONNECTED_BLUESWITCH);
                
                if(gatt.discoverServices())
                {
                    Log.i(TAG, "Attempting to start service discovery:");

                }
                else{
                    Log.i(TAG, "Attempting to start service discovery:not success");

                }


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                
                if(gatt.getDevice().getAddress().equals(BlunoLibrary.BlunoMacAddr))
            		broadcastUpdate(ACTION_GATT_DISCONNECTED_BLUNO);
            	
            	if(gatt.getDevice().getAddress().equals(BlunoLibrary.BlueSwitchMacAddr_Grey) ||
            			gatt.getDevice().getAddress().equals(BlunoLibrary.BlueSwitchMacAddr_White))
            		broadcastUpdate(ACTION_GATT_DISCONNECTED_BLUESWITCH);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	
            	if(gatt.getDevice().getAddress().equals(BlunoLibrary.BlunoMacAddr))
            		broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED_BLUNO);
            	
            	if(gatt.getDevice().getAddress().equals(BlunoLibrary.BlueSwitchMacAddr_Grey) ||
            			gatt.getDevice().getAddress().equals(BlunoLibrary.BlueSwitchMacAddr_White))
            		broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED_BLUESWITCH);
            }
        }
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
        	//this block should be synchronized to prevent the function overloading
			synchronized(this)
			{
				//CharacteristicWrite success
	        	if(status == BluetoothGatt.GATT_SUCCESS)
	        	{
	        		System.out.println("onCharacteristicWrite success:"+ new String(characteristic.getValue()));
            		if(mCharacteristicRingBuffer.isEmpty())
            		{
    	        		mIsWritingCharacteristic = false;
            		}
            		else
	            	{
	            		BluetoothGattCharacteristicHelper bluetoothGattCharacteristicHelper = mCharacteristicRingBuffer.next();
	            		if(bluetoothGattCharacteristicHelper.mCharacteristicValue.length > MAX_CHARACTERISTIC_LENGTH)
	            		{
	            	       
		            			bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue);

	            	    
	            			
	            	        if(gatt.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic))
	            	        {
	            	        	System.out.println("writeCharacteristic init "+new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue())+ ":success");
	            	        }else{
	            	        	System.out.println("writeCharacteristic init "+new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue())+ ":failure");
	            	        }
	            			bluetoothGattCharacteristicHelper.mCharacteristicValue = bluetoothGattCharacteristicHelper.mCharacteristicValue;
	            		}
	            		else
	            		{
	            	       
	            	        	bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue);
	            	       
	            			
	            	        if(gatt.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic))
	            	        {
	            	        	System.out.println("writeCharacteristic init "+new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue())+ ":success");
	            	        }else{
	            	        	System.out.println("writeCharacteristic init "+new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue())+ ":failure");
	            	        }
	            			bluetoothGattCharacteristicHelper.mCharacteristicValue = null;
	            			mCharacteristicRingBuffer.pop();
	            		}
	            	}
	        	}
	        	//WRITE a NEW CHARACTERISTIC
	        	else if(status == WRITE_NEW_CHARACTERISTIC)
	        	{
	        		if((!mCharacteristicRingBuffer.isEmpty()) && mIsWritingCharacteristic==false)
	            	{
	            		BluetoothGattCharacteristicHelper bluetoothGattCharacteristicHelper = mCharacteristicRingBuffer.next();
	            		if(bluetoothGattCharacteristicHelper.mCharacteristicValue.length > MAX_CHARACTERISTIC_LENGTH)
	            		{
	            			
	            	       
		            			bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue);
	            	    
	            			
	            	        if(gatt.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic))
	            	        {
	            	        	System.out.println("writeCharacteristic init "+new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue())+ ":success");
	            	        }else{
	            	        	System.out.println("writeCharacteristic init "+new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue())+ ":failure");
	            	        }
	            			bluetoothGattCharacteristicHelper.mCharacteristicValue = bluetoothGattCharacteristicHelper.mCharacteristicValue;
	            		}
	            		else
	            		{
	            	       
		            			bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue);
	            	        
	            			

	            	        if(gatt.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic))
	            	        {
	            	        	System.out.println("writeCharacteristic init "+new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue())+ ":success");
	            	        }else{
	            	        	System.out.println("writeCharacteristic init "+new String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue())+ ":failure");
	            	        }
	            			bluetoothGattCharacteristicHelper.mCharacteristicValue = null;
		            		mCharacteristicRingBuffer.pop();
	            		}
	            	}
	        		
    	        	mIsWritingCharacteristic = true;
    	        	
    	        	//clear the buffer to prevent the lock of the mIsWritingCharacteristic
    	        	if(mCharacteristicRingBuffer.isFull())
    	        	{
    	        		mCharacteristicRingBuffer.clear();
        	        	mIsWritingCharacteristic = false;
    	        	}
	        	}
	        	else
					//CharacteristicWrite fail
	        	{
	        		mCharacteristicRingBuffer.clear();
	        		System.out.println("onCharacteristicWrite fail:"+ new String(characteristic.getValue()));
	        		System.out.println(status);
	        	}
			}
        }
        
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	System.out.println("onCharacteristicRead  "+characteristic.getUuid().toString());
            	if(gatt.getDevice().getAddress().equals(BlunoLibrary.BlunoMacAddr))
            	{
            		broadcastUpdate(ACTION_DATA_AVAILABLE_BLUNO, characteristic);
            	}
            	if(gatt.getDevice().getAddress().equals(BlunoLibrary.BlueSwitchMacAddr_Grey) || 
            			gatt.getDevice().getAddress().equals(BlunoLibrary.BlueSwitchMacAddr_White))
            	{
            		broadcastUpdate(ACTION_DATA_AVAILABLE_BLUESWITCH, characteristic);
            	}
            }
        }
        @Override
        public void  onDescriptorWrite(BluetoothGatt gatt, 
        								BluetoothGattDescriptor characteristic,
        								int status){
        	System.out.println("onDescriptorWrite  "+characteristic.getUuid().toString()+" "+status);
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	System.out.println("onCharacteristicChanged  "+new String(characteristic.getValue()));
        	if(gatt.getDevice().getAddress().equals(BlunoLibrary.BlunoMacAddr))
        	{
        		broadcastUpdate(ACTION_DATA_AVAILABLE_BLUNO, characteristic);
        	}
        	if(gatt.getDevice().getAddress().equals(BlunoLibrary.BlueSwitchMacAddr_Grey) || 
        			gatt.getDevice().getAddress().equals(BlunoLibrary.BlueSwitchMacAddr_White))
        	{
        		broadcastUpdate(ACTION_DATA_AVAILABLE_BLUESWITCH, characteristic);
        	}
        }
    };
    
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        System.out.println("BluetoothLeService broadcastUpdate");

            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                intent.putExtra(EXTRA_DATA, data);
        		sendBroadcast(intent);
            }
//        }
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
    	System.out.println("BluetoothLeService initialize"+mBluetoothManager);
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

		//synchronized(this)
		//{
			if(address.equals(BlunoLibrary.BlunoMacAddr))
				mBlunoBluetoothGatt = device.connectGatt(this, false, mGattCallback);
			if(address.equals(BlunoLibrary.BlueSwitchMacAddr_Grey) || address.equals(BlunoLibrary.BlueSwitchMacAddr_White))
				mBlueSwitchBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		//}

        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        
        if(mBlunoBluetoothGatt != null)
        	mBlunoBluetoothGatt.disconnect();
        if(mBlueSwitchBluetoothGatt != null)
        	mBlueSwitchBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBlunoBluetoothGatt != null) {
        	mBlunoBluetoothGatt.close();
            mBlunoBluetoothGatt = null;
        }
        if (mBlueSwitchBluetoothGatt != null) {
        	mBlueSwitchBluetoothGatt.close();
        	mBlueSwitchBluetoothGatt = null;
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if(mBlunoBluetoothGatt != null)
        	mBlunoBluetoothGatt.readCharacteristic(characteristic);
        if(mBlueSwitchBluetoothGatt != null)
        	mBlueSwitchBluetoothGatt.readCharacteristic(characteristic);
    }
    

    /**
     * Write information to the device on a given {@code BluetoothGattCharacteristic}. The content string and characteristic is 
     * only pushed into a ring buffer. All the transmission is based on the {@code onCharacteristicWrite} call back function, 
     * which is called directly in this function
     *
     * @param characteristic The characteristic to write to.
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBlueSwitchBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

    	mCharacteristicRingBuffer.push(new BluetoothGattCharacteristicHelper(characteristic,characteristic.getValue()) );
    	System.out.println("mCharacteristicRingBufferlength:"+mCharacteristicRingBuffer.size());

    	mGattCallback.onCharacteristicWrite(mBlueSwitchBluetoothGatt, characteristic, WRITE_NEW_CHARACTERISTIC);
    }    
    
    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if(mBlunoBluetoothGatt != null)
        	mBlunoBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if(mBlueSwitchBluetoothGatt != null)
        	mBlueSwitchBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedBlunoGattServices() {
        if (mBlunoBluetoothGatt == null) return null;

        return mBlunoBluetoothGatt.getServices();
    }
    
    public List<BluetoothGattService> getSupportedBlueSwitchGattServices() {
        if (mBlueSwitchBluetoothGatt == null) return null;

        return mBlueSwitchBluetoothGatt.getServices();
    }
    
    
}
