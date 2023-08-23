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

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.NonNull;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {


    // TAG is used for informational messages
    private final static String TAG = MainActivity.class.getSimpleName();

    String[] percentArray = {"0","25","50","75","100"};

    // Variables to access objects from the layout such as buttons, switches, values
//    private static TextView mCapsenseValue;
    private static Button start_button;
    private static Button search_button;
    private static Button connect_button;
    private static Button discover_button;
    private static Button disconnect_button;
    private ListView RedListView;
    private ListView GreenListView;
    private ListView BlueListView;
//    private static Switch led_switch;
//    private static Switch GreenLed_switch;

    private boolean forwardPressed=false;
    private boolean reversePressed=false;
    private boolean leftPressed=false;
    private boolean rightPressed=false;

    // Variables to manage BLE connection
    private static boolean mConnectState;
    private static boolean mServiceConnected;
    private static PSoCCapSenseLedService mPSoCCapSenseLedService;

    private static final int REQUEST_ENABLE_BLE = 1;

    //This is required for Android 6.0 (Marshmallow)
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // Keep track of whether CapSense Notifications are on or off
//    private static boolean CapSenseNotifyState = false;

    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and initialize the service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * This is called when the PSoCCapSenseLedService is connected
         *
         * @param componentName the component name of the service that has been connected
         * @param service service being bound
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mPSoCCapSenseLedService = ((PSoCCapSenseLedService.LocalBinder) service).getService();
            mServiceConnected = true;
            mPSoCCapSenseLedService.initialize();
        }

        /**
         * This is called when the PSoCCapSenseService is disconnected.
         *
         * @param componentName the component name of the service that has been connected
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            mPSoCCapSenseLedService = null;
        }
    };

    /**
     * This is called when the main activity is first created
     *
     * @param savedInstanceState is any state saved from prior creations of this activity
     */
    @TargetApi(Build.VERSION_CODES.M) // This is required for Android 6.0 (Marshmallow) to work
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up a variable to point to the CapSense value on the display
//        mCapsenseValue = (TextView) findViewById(R.id.capsense_value);

        // Set up variables for accessing buttons and slide switches
        start_button = (Button) findViewById(R.id.start_button);
        search_button = (Button) findViewById(R.id.search_button);
        connect_button = (Button) findViewById(R.id.connect_button);
        discover_button = (Button) findViewById(R.id.discoverSvc_button);
        disconnect_button = (Button) findViewById(R.id.disconnect_button);
//        led_switch = (Switch) findViewById(R.id.led_switch);
//        GreenLed_switch = (Switch) findViewById(R.id.GreenLed_switch);
        Button forward_button = (Button) findViewById(R.id.ForwardButton);
        Button reverse_button = (Button) findViewById(R.id.ReverseButton);
        Button left_button = (Button) findViewById(R.id.LeftButton);
        Button right_button = (Button) findViewById(R.id.RightButton);

        //RED
        RedListView = (ListView) findViewById(R.id.RedListView);
        ArrayAdapter adapterRed = new ArrayAdapter<String>(this, R.layout.activity_listview_red, percentArray);
        RedListView.setAdapter(adapterRed);
        //Title
        TextView RedTitle = new TextView(this);
        RedTitle.setText("RED");
        RedListView.addHeaderView(RedTitle);

        //GREEN
        GreenListView = (ListView) findViewById(R.id.GreenListView);
        ArrayAdapter adapterGreen = new ArrayAdapter<String>(this, R.layout.activity_listview_green, percentArray);
        GreenListView.setAdapter(adapterGreen);
        //Title
        TextView GreenTitle = new TextView(this);
        GreenTitle.setText("GREEN");
        GreenListView.addHeaderView(GreenTitle);

        //BLUE
        BlueListView = (ListView) findViewById(R.id.BlueListView);
        ArrayAdapter adapterBlue = new ArrayAdapter<String>(this, R.layout.activity_listview_blue, percentArray);
        BlueListView.setAdapter(adapterBlue);
        //Title
        TextView BlueTitle = new TextView(this);
        BlueTitle.setText("BLUE");
        BlueListView.addHeaderView(BlueTitle);

        // Initialize service and connection state variable
        mServiceConnected = false;
        mConnectState = false;

        //This section required for Android 6.0 (Marshmallow)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access ");
                builder.setMessage("Please grant location access so this app can detect devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        } //End of section for Android 6.0 (Marshmallow)

        RedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, String.valueOf(id));
//                String itemValue = (String) RedListView.getItemAtPosition( position );
                mPSoCCapSenseLedService.writeRedLedCharacteristicList(position);
            }
        });

        GreenListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // When clicked, show a toast with the TextView text
                Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, String.valueOf(id));
                mPSoCCapSenseLedService.writeGreenLedCharacteristicList(position);
            }
        });

        BlueListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // When clicked, show a toast with the TextView text
                Toast.makeText(getApplicationContext(), ((TextView) view).getText(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, String.valueOf(id));
                mPSoCCapSenseLedService.writeBlueLedCharacteristicList(position);
            }
        });

        forward_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Held Down
                        forwardPressed = true;
                        Log.d("DEBUG", "FORWARD PRESSED: " + forwardPressed);
                        Toast.makeText(getApplicationContext(), "FORWARD: true", Toast.LENGTH_SHORT).show();
                        mPSoCCapSenseLedService.writeForwardCharacteristic(forwardPressed);
                        return true;
                    case MotionEvent.ACTION_UP:
                        // No longer down
                        forwardPressed = false;
                        Log.d("DEBUG", "FORWARD PRESSED: " + forwardPressed);
                        Toast.makeText(getApplicationContext(), "FORWARD: false", Toast.LENGTH_SHORT).show();
                        mPSoCCapSenseLedService.writeForwardCharacteristic(forwardPressed);
                        return true;
                }
                return false;
            }
        });


        reverse_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Held Down
                        reversePressed = true;
                        Log.d("DEBUG", "REVERSE PRESSED: " + reversePressed);
                        Toast.makeText(getApplicationContext(), "REVERSE: true", Toast.LENGTH_SHORT).show();
                        mPSoCCapSenseLedService.writeReverseCharacteristic(reversePressed);
                        return true;
                    case MotionEvent.ACTION_UP:
                        // No longer down
                        reversePressed = false;
                        Log.d("DEBUG", "REVERSE PRESSED: " + reversePressed);
                        Toast.makeText(getApplicationContext(), "REVERSE: false", Toast.LENGTH_SHORT).show();
                        mPSoCCapSenseLedService.writeReverseCharacteristic(reversePressed);
                        return true;
                }
                return false;
            }
        });

        left_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Held Down
                        leftPressed = true;
                        Log.d("DEBUG", "LEFT PRESSED: " + leftPressed);
                        Toast.makeText(getApplicationContext(), "LEFT: true", Toast.LENGTH_SHORT).show();
                        mPSoCCapSenseLedService.writeLeftCharacteristic(leftPressed);
                        return true;
                    case MotionEvent.ACTION_UP:
                        // No longer down
                        leftPressed = false;
                        Log.d("DEBUG", "LEFT PRESSED: " + leftPressed);
                        Toast.makeText(getApplicationContext(), "LEFT: false", Toast.LENGTH_SHORT).show();
                        mPSoCCapSenseLedService.writeLeftCharacteristic(leftPressed);
                        return true;
                }
                return false;
            }
        });

        right_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Held Down
                        rightPressed = true;
                        Log.d("DEBUG", "RIGHT PRESSED: " + rightPressed);
                        Toast.makeText(getApplicationContext(), "RIGHT: true", Toast.LENGTH_SHORT).show();
                        mPSoCCapSenseLedService.writeRightCharacteristic(rightPressed);
                        return true;
                    case MotionEvent.ACTION_UP:
                        // No longer down
                        rightPressed = false;
                        Log.d("DEBUG", "RIGHT PRESSED: " + rightPressed);
                        Toast.makeText(getApplicationContext(), "RIGHT: false", Toast.LENGTH_SHORT).show();
                        mPSoCCapSenseLedService.writeRightCharacteristic(rightPressed);
                        return true;
                }
                return false;
            }
        });

    }

    //This method required for Android 6.0 (Marshmallow)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission for 6.0:", "Coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    } //End of section for Android 6.0 (Marshmallow)

    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver. This specified the messages the main activity looks for from the PSoCCapSenseLedService
        final IntentFilter filter = new IntentFilter();
        filter.addAction(PSoCCapSenseLedService.ACTION_BLESCAN_CALLBACK);
        filter.addAction(PSoCCapSenseLedService.ACTION_CONNECTED);
        filter.addAction(PSoCCapSenseLedService.ACTION_DISCONNECTED);
        filter.addAction(PSoCCapSenseLedService.ACTION_SERVICES_DISCOVERED);
        filter.addAction(PSoCCapSenseLedService.ACTION_DATA_RECEIVED);
        registerReceiver(mBleUpdateReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBleUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close and unbind the service when the activity goes away
        mPSoCCapSenseLedService.close();
        unbindService(mServiceConnection);
        mPSoCCapSenseLedService = null;
        mServiceConnected = false;
    }

    /**
     * This method handles the start bluetooth button
     *
     * @param view the view object
     */
    public void startBluetooth(View view) {

        // Find BLE service and adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLE);
        }

        // Start the BLE Service
        Log.d(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(this, PSoCCapSenseLedService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // Disable the start button and turn on the search  button
        start_button.setEnabled(false);
        search_button.setEnabled(true);
        Log.d(TAG, "Bluetooth is Enabled");
    }

    /**
     * This method handles the Search for Device button
     *
     * @param view the view object
     */
    public void searchBluetooth(View view) {
        if(mServiceConnected) {
            mPSoCCapSenseLedService.scan();
        }

        /* After this we wait for the scan callback to detect that a device has been found */
        /* The callback broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * This method handles the Connect to Device button
     *
     * @param view the view object
     */
    public void connectBluetooth(View view) {
        mPSoCCapSenseLedService.connect();

        /* After this we wait for the gatt callback to report the device is connected */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * This method handles the Discover Services and Characteristics button
     *
     * @param view the view object
     */
    public void discoverServices(View view) {
        /* This will discover both services and characteristics */
        mPSoCCapSenseLedService.discoverServices();

        /* After this we wait for the gatt callback to report the services and characteristics */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * This method handles the Disconnect button
     *
     * @param view the view object
     */
    public void Disconnect(View view) {
        mPSoCCapSenseLedService.disconnect();

        /* After this we wait for the gatt callback to report the device is disconnected */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * Listener for BLE event broadcasts
     */
    private final BroadcastReceiver mBleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case PSoCCapSenseLedService.ACTION_BLESCAN_CALLBACK:
                    // Disable the search button and enable the connect button
                    search_button.setEnabled(false);
                    connect_button.setEnabled(true);
                    break;

                case PSoCCapSenseLedService.ACTION_CONNECTED:
                    /* This if statement is needed because we sometimes get a GATT_CONNECTED */
                    /* action when sending Capsense notifications */
                    if (!mConnectState) {
                        // Disable the connect button, enable the discover services and disconnect buttons
                        connect_button.setEnabled(false);
                        discover_button.setEnabled(true);
                        disconnect_button.setEnabled(true);
                        mConnectState = true;
                        Log.d(TAG, "Connected to Device");
                    }
                    break;
                case PSoCCapSenseLedService.ACTION_DISCONNECTED:
                    // Disable the disconnect, discover svc, discover char button, and enable the search button
                    disconnect_button.setEnabled(false);
                    discover_button.setEnabled(false);
                    search_button.setEnabled(true);
                    // Turn off and disable the LED and CapSense switches
//                    led_switch.setChecked(false);
//                    led_switch.setEnabled(false);
//                    GreenLed_switch.setChecked(false);
//                    GreenLed_switch.setEnabled(false);
//                    RedListView.setEnabled(false);
//                    GreenListView.setEnabled(false);
//                    BlueListView.setEnabled(false);
                    mConnectState = false;
                    Log.d(TAG, "Disconnected");
                    break;
                case PSoCCapSenseLedService.ACTION_SERVICES_DISCOVERED:
                    // Disable the discover services button
                    discover_button.setEnabled(false);
                    // Enable the LED and CapSense switches
//                    led_switch.setEnabled(true);
//                    GreenLed_switch.setEnabled(true);
//                    RedListView.setEnabled(true);
//                    GreenListView.setEnabled(true);
//                    BlueListView.setEnabled(true);
                    Log.d(TAG, "Services Discovered");
                    break;
                case PSoCCapSenseLedService.ACTION_DATA_RECEIVED:
                    // This is called after a notify or a read completes
                    // Check LED switch Setting
                    if(mPSoCCapSenseLedService.getRedLedSwitchState()>0){
//                        led_switch.setChecked(true);
//                        GreenLed_switch.setChecked(true);
                    } else {
//                        led_switch.setChecked(false);
//                        GreenLed_switch.setChecked(false);
                    }
                    if(mPSoCCapSenseLedService.getGreenLedSwitchState()>0){
//                        GreenLed_switch.setChecked(true);
                    } else {
//                        GreenLed_switch.setChecked(false);
                    }
                    if(mPSoCCapSenseLedService.getBlueLedSwitchState()>0){
//                        BlueLed_switch.setChecked(true);
                    } else {
//                        BlueLed_switch.setChecked(false);
                    }
                default:
                    break;
            }
        }
    };
}
