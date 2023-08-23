/*
Copyright (c) 2016, Cypress Semiconductor Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.



For more information on Cypress BLE products visit:
http://www.cypress.com/products/bluetooth-low-energy-ble
 */

package com.cypress.academy.ble101;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing the BLE data connection with the GATT database.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP) // This is required to allow us to use the lollipop and later scan APIs
public class PSoCCapSenseLedService extends Service {
    private final static String TAG = PSoCCapSenseLedService.class.getSimpleName();

    // Bluetooth objects that we need to interact with
    private static BluetoothManager mBluetoothManager;
    private static BluetoothAdapter mBluetoothAdapter;
    private static BluetoothLeScanner mLEScanner;
    private static BluetoothDevice mLeDevice;
    private static BluetoothGatt mBluetoothGatt;

    // Bluetooth characteristics that we need to read/write
    private static BluetoothGattCharacteristic mRedLedCharacterisitc;
    private static BluetoothGattCharacteristic mGreenLedCharacteristic;
    private static BluetoothGattCharacteristic mBlueLedCharacteristic;
    private static BluetoothGattCharacteristic mForwardCharacteristic;
    private static BluetoothGattCharacteristic mReverseCharacteristic;
    private static BluetoothGattCharacteristic mLeftCharacteristic;
    private static BluetoothGattCharacteristic mRightCharacteristic;
//    private static BluetoothGattDescriptor mCapSenseCccd;

    // UUIDs for the service and characteristics that the custom CapSenseLED service uses
    private final static String baseUUID = "55F4EC0E-D13D-4EC3-B088-29B6874864C";
    private final static String LedServiceUUID = baseUUID + "0";
    public final static String RedLedCharacteristicUUID = baseUUID + "1";
    public final static String GreenLedCharacteristicUUID = baseUUID + "2";
    public final static String BlueLedCharacteristicUUID = baseUUID + "3";
    public final static String ForwardCharacteristicUUID = baseUUID + "4";
    public final static String ReverseCharacteristicUUID = baseUUID + "5";
    public final static String LeftCharacteristicUUID = baseUUID + "6";
    public final static String RightCharacteristicUUID = baseUUID + "7";

    private final static String CccdUUID = "00002902-0000-1000-8000-00805f9b34fb";

    // Variables to keep track of the LED switch state and CapSense Value
    private static int mRedLedSwitchState = 0;
    private static int mGreenLedSwitchState = 0;
    private static int mBlueLedSwitchState = 0;
    private static boolean mForwardState = false;
    private static boolean mReverseState = false;
    private static boolean mLeftState = false;
    private static boolean mRightState = false;
//    private static String mCapSenseValue = "-1"; // This is the No Touch value (0xFFFF)

    // Actions used during broadcasts to the main activity
    public final static String ACTION_BLESCAN_CALLBACK =
            "com.cypress.academy.ble101.ACTION_BLESCAN_CALLBACK";
    public final static String ACTION_CONNECTED =
            "com.cypress.academy.ble101.ACTION_CONNECTED";
    public final static String ACTION_DISCONNECTED =
            "com.cypress.academy.ble101.ACTION_DISCONNECTED";
    public final static String ACTION_SERVICES_DISCOVERED =
            "com.cypress.academy.ble101.ACTION_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_RECEIVED =
            "com.cypress.academy.ble101.ACTION_DATA_RECEIVED";

    public PSoCCapSenseLedService() {
    }

    /**
     * This is a binder for the PSoCCapSenseLedService
     */
    public class LocalBinder extends Binder {
        PSoCCapSenseLedService getService() {
            return PSoCCapSenseLedService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // The BLE close method is called when we unbind the service to free up the resources.
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
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
            Log.d("DEBUG", "BLUETOOTHMANAGER INITIALIZED");
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        Log.d("DEBUG", "BLUETOOTHADAPTER OBTAINED");

        return true;
    }

    /**
     * Scans for BLE devices that support the service we are looking for
     */
    public void scan() {
        /* Scan for devices and look for the one with the service that we want */
        UUID capsenseLedService = UUID.fromString(LedServiceUUID);
        UUID[] capsenseLedServiceArray = {capsenseLedService};

        // Use old scan method for versions older than lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.d("DEBUG", "SEARCHING FOR capsenseLedService via DEPRECATED BLUETOOTH PROTOCOL");
            //noinspection deprecation
            mBluetoothAdapter.startLeScan(capsenseLedServiceArray, mLeScanCallback);
        } else { // New BLE scanning introduced in LOLLIPOP
            Log.d("DEBUG", "SEARCHING FOR capsenseLedService via NEW LOLLIPOP BLUETOOTH PROTOCOL");
            ScanSettings settings;
            List<ScanFilter> filters;
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();
            // We will scan just for the CAR's UUID
            ParcelUuid PUuid = new ParcelUuid(capsenseLedService);
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(PUuid).build();
            filters.add(filter);
            mLEScanner.startScan(filters, settings, mScanCallback);
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            Log.d("DEBUG", "BLUETOOTHADAPTER NOT INITIALIZED");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = mLeDevice.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        return true;
    }

    /**
     * Runs service discovery on the connected device.
     */
    public void discoverServices() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            Log.d("DEBUG", "BLUETOOTHADAPTER NOT INITIALIZED");
            return;
        }
        mBluetoothGatt.discoverServices();
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            Log.d("DEBUG", "BLUETOOTHADAPTER NOT INITIALIZED");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * This method is used to read the state of the LED from the device
     */
    public void readRedLedCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            Log.d("DEBUG", "BLUETOOTHADAPTER NOT INITIALIZED");
            return;
        }
        mBluetoothGatt.readCharacteristic(mRedLedCharacterisitc);
    }

    public void readGreenLedCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            Log.d("DEBUG", "BLUETOOTHADAPTER NOT INITIALIZED");
            return;
        }
        mBluetoothGatt.readCharacteristic(mGreenLedCharacteristic);
    }

    public void readBlueLedCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            Log.d("DEBUG", "BLUETOOTHADAPTER NOT INITIALIZED");
            return;
        }
        mBluetoothGatt.readCharacteristic(mBlueLedCharacteristic);
    }

    public void readForwardCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            Log.d("DEBUG", "BLUETOOTHADAPTER NOT INITIALIZED");
            return;
        }
        mBluetoothGatt.readCharacteristic(mForwardCharacteristic);
    }

    public void readReverseCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            Log.d("DEBUG", "BLUETOOTHADAPTER NOT INITIALIZED");
            return;
        }
        mBluetoothGatt.readCharacteristic(mReverseCharacteristic);
    }

    public void readLeftCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            Log.d("DEBUG", "BLUETOOTHADAPTER NOT INITIALIZED");
            return;
        }
        mBluetoothGatt.readCharacteristic(mLeftCharacteristic);
    }

    public void readRightCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            Log.d("DEBUG", "BLUETOOTHADAPTER NOT INITIALIZED");
            return;
        }
        mBluetoothGatt.readCharacteristic(mRightCharacteristic);
    }

    public void writeRedLedCharacteristicList(int value) {
        value = (value+4)%5;
        byte[] byteVal = new byte[1];
        if (value==0) {
            byteVal[0] = (byte) 0;
        } else if(value==1) {
            byteVal[0] = (byte) 25;
        } else if(value==2) {
            byteVal[0] = (byte) 50;
        } else if (value==3) {
            byteVal[0] = (byte) 75;
        } else if (value==4){
            byteVal[0] = (byte) 100;
        }
        Log.i(TAG, "LED " + value);
        mRedLedSwitchState = value;

        mRedLedCharacterisitc.setValue(byteVal);
        mBluetoothGatt.writeCharacteristic(mRedLedCharacterisitc);
    }

    public void writeGreenLedCharacteristicList(int value) {
        value = (value+4)%5;
        byte[] byteVal = new byte[1];
        if (value==0) {
            byteVal[0] = (byte) (0);
        } else if(value==1) {
            byteVal[0] = (byte) (25);
        } else if(value==2) {
            byteVal[0] = (byte) (50);
        } else if (value==3) {
            byteVal[0] = (byte) (75);
        } else if (value==4){
            byteVal[0] = (byte) (100);
        }
        Log.i(TAG, "LED " + value);
        mGreenLedSwitchState = value;

        mGreenLedCharacteristic.setValue(byteVal);
        mBluetoothGatt.writeCharacteristic(mGreenLedCharacteristic);
    }

    public void writeBlueLedCharacteristicList(int value) {
        value = (value+4)%5;
        byte[] byteVal = new byte[1];
        if (value==0) {
            byteVal[0] = (byte) (0);
        } else if(value==1) {
            byteVal[0] = (byte) (25);
        } else if(value==2) {
            byteVal[0] = (byte) (50);
        } else if (value==3) {
            byteVal[0] = (byte) (75);
        } else if (value==4){
            byteVal[0] = (byte) (100);
        }
        Log.i(TAG, "LED " + value);
        mBlueLedSwitchState = value;

        mBlueLedCharacteristic.setValue(byteVal);
        mBluetoothGatt.writeCharacteristic(mBlueLedCharacteristic);
    }

    public void writeForwardCharacteristic(boolean value) {

        byte[] byteVal = new byte[1];
        if(value){
            byteVal[0] = (byte) 1;
        }else{
            byteVal[0] = (byte) 0;
        }
        Log.i(TAG, "FORWARD VALUE SENT: " + value);
        mForwardState = value;

        mForwardCharacteristic.setValue(byteVal);
        mBluetoothGatt.writeCharacteristic(mForwardCharacteristic);
    }

    public void writeReverseCharacteristic(boolean value) {

        byte[] byteVal = new byte[1];
        if(value){
            byteVal[0] = (byte) 1;
        }else{
            byteVal[0] = (byte) 0;
        }
        Log.i(TAG, "REVERSE VALUE SENT: " + value);
        mReverseState = value;

        mReverseCharacteristic.setValue(byteVal);
        mBluetoothGatt.writeCharacteristic(mReverseCharacteristic);
    }

    public void writeLeftCharacteristic(boolean value) {

        byte[] byteVal = new byte[1];
        if(value){
            byteVal[0] = (byte) 1;
        }else{
            byteVal[0] = (byte) 0;
        }
        Log.i(TAG, "LEFT VALUE SENT: " + value);
        mLeftState = value;

        mLeftCharacteristic.setValue(byteVal);
        mBluetoothGatt.writeCharacteristic(mLeftCharacteristic);
    }

    public void writeRightCharacteristic(boolean value) {

        byte[] byteVal = new byte[1];
        if(value){
            byteVal[0] = (byte) 1;
        }else{
            byteVal[0] = (byte) 0;
        }
        Log.i(TAG, "RIGHT VALUE SENT: " + value);
        mRightState = value;

        mRightCharacteristic.setValue(byteVal);
        mBluetoothGatt.writeCharacteristic(mRightCharacteristic);
    }



    /**
     * This method returns the state of the LED switch
     *
     * @return the value of the LED swtich state
     */
    public int getRedLedSwitchState() {
        return mRedLedSwitchState;
    }

    public int getGreenLedSwitchState() {
        return mGreenLedSwitchState;
    }

    public int getBlueLedSwitchState() {
        return mBlueLedSwitchState;
    }

    public boolean getForwardState() {
        return mForwardState;
    }

    public boolean getReverseState() {
        return mReverseState;
    }

    public boolean getLeftState() {
        return mLeftState;
    }

    public boolean getRightState() {
        return mRightState;
    }

    /**
     * Implements the callback for when scanning for devices has found a device with
     * the service we are looking for.
     * <p>
     * This is the callback for BLE scanning on versions prior to Lollipop
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mLeDevice = device;
                    //noinspection deprecation
                    mBluetoothAdapter.stopLeScan(mLeScanCallback); // Stop scanning after the first device is found
                    broadcastUpdate(ACTION_BLESCAN_CALLBACK); // Tell the main activity that a device has been found
                }
            };

    /**
     * Implements the callback for when scanning for devices has faound a device with
     * the service we are looking for.
     * <p>
     * This is the callback for BLE scanning for LOLLIPOP and later
     */
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            mLeDevice = result.getDevice();
            mLEScanner.stopScan(mScanCallback); // Stop scanning after the first device is found
            broadcastUpdate(ACTION_BLESCAN_CALLBACK); // Tell the main activity that a device has been found
        }
    };


    /**
     * Implements callback methods for GATT events that the app cares about.  For example,
     * connection change and services discovered.
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                broadcastUpdate(ACTION_CONNECTED);
                Log.i(TAG, "Connected to GATT server.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(ACTION_DISCONNECTED);
            }
        }

        /**
         * This is called when a service discovery has completed.
         *
         * It gets the characteristics we are interested in and then
         * broadcasts an update to the main activity.
         *
         * @param gatt The GATT database object
         * @param status Status of whether the write was successful.
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // Get just the service that we are looking for
            BluetoothGattService mService = gatt.getService(UUID.fromString(LedServiceUUID));
            /* Get characteristics from our desired service */
            mRedLedCharacterisitc = mService.getCharacteristic(UUID.fromString(RedLedCharacteristicUUID));
            mGreenLedCharacteristic = mService.getCharacteristic(UUID.fromString(GreenLedCharacteristicUUID));
            mBlueLedCharacteristic = mService.getCharacteristic(UUID.fromString(BlueLedCharacteristicUUID));
            mForwardCharacteristic = mService.getCharacteristic(UUID.fromString(ForwardCharacteristicUUID));
            mReverseCharacteristic = mService.getCharacteristic(UUID.fromString(ReverseCharacteristicUUID));
            mLeftCharacteristic = mService.getCharacteristic(UUID.fromString(LeftCharacteristicUUID));
            mRightCharacteristic = mService.getCharacteristic(UUID.fromString(RightCharacteristicUUID));
            /* Get the CapSense CCCD */
//            mCapSenseCccd = mGreenLedCharacteristic.getDescriptor(UUID.fromString(CccdUUID));

            // Read the current state of the LED from the device
            readRedLedCharacteristic();
            readGreenLedCharacteristic();
            readBlueLedCharacteristic();
            readForwardCharacteristic();
            readReverseCharacteristic();
            readLeftCharacteristic();
            readRightCharacteristic();

            // Broadcast that service/characteristic/descriptor discovery is done
            broadcastUpdate(ACTION_SERVICES_DISCOVERED);
        }

        /**
         * This is called when a read completes
         *
         * @param gatt the GATT database object
         * @param characteristic the GATT characteristic that was read
         * @param status the status of the transaction
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Verify that the read was the LED state
                String uuid = characteristic.getUuid().toString();
                // In this case, the only read the app does is the LED state.
                // If the application had additional characteristics to read we could
                // use a switch statement here to operate on each one separately.
                if (uuid.equalsIgnoreCase(RedLedCharacteristicUUID)) {
                    final byte[] data = characteristic.getValue();
                    // Set the LED switch state variable based on the characteristic value ttat was read
                    mRedLedSwitchState = data[0];
                }
                if (uuid.equalsIgnoreCase(GreenLedCharacteristicUUID)) {
                    final byte[] data = characteristic.getValue();
                    // Set the LED switch state variable based on the characteristic value ttat was read
                    mGreenLedSwitchState = data[0];
                }

                if (uuid.equalsIgnoreCase(BlueLedCharacteristicUUID)) {
                    final byte[] data = characteristic.getValue();
                    // Set the LED switch state variable based on the characteristic value ttat was read
                    mBlueLedSwitchState = data[0];
                }

//                if (uuid.equalsIgnoreCase(ForwardCharacteristicUUID)) {
//                    final byte[] data = characteristic.getValue();
//                    // Set the LED switch state variable based on the characteristic value ttat was read
//                    mForwardState = data[0];
//                }

                // Notify the main activity that new data is available
                broadcastUpdate(ACTION_DATA_RECEIVED);
            }
        }

    }; // End of GATT event callback methods

    /**
     * Sends a broadcast to the listener in the main activity.
     *
     * @param action The type of action that occurred.
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

}