package hr.in24stem.jumper;

import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.Activity;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import hr.in24stem.Constants;
import hr.in24stem.MicroBit;
import hr.in24stem.Settings;
import hr.in24stem.Utility;
import hr.in24stem.jumper.bluetooth.BleAdapterService;
import hr.in24stem.jumper.bluetooth.ConnectionStatusListener;

import hr.in24stem.Constants;
import hr.in24stem.jumper.statistics.AccelerometerData;
import hr.in24stem.jumper.statistics.Statistics;

public class CountActivity extends AppCompatActivity implements ConnectionStatusListener {
    private float[] accel_input = new float[3];
    private float[] accel_output = new float[3];

    private boolean apply_smoothing = false;

    private BleAdapterService bluetooth_le_adapter;
    private boolean exiting = false;
    private int accelerometer_period;
    private boolean notifications_on = false;
    private long start_time;
    private int minute_number;
    private int notification_count;
    private boolean calibrating = false;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetooth_le_adapter = ((BleAdapterService.LocalBinder) service).getService();
            bluetooth_le_adapter.setActivityHandler(mMessageHandler);
            connectToDevice();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetooth_le_adapter = null;
        }
    };
    private ArrayList<AccelerometerData> calibrationData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_count);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        Log.d(Constants.TAG, "MenuActivity onCreate");

        final Intent intent = getIntent();
        MicroBit.getInstance().setMicrobit_name(intent.getStringExtra(Constants.EXTRA_NAME));
        MicroBit.getInstance().setMicrobit_address(intent.getStringExtra(Constants.EXTRA_ID));
        MicroBit.getInstance().setConnection_status_listener(this);

        Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        Log.d(Constants.TAG, "SERVICE BOUNDED");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // may already have unbound. No API to check state so....
            unbindService(mServiceConnection);
        } catch (Exception e) {
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed");
        if (MicroBit.getInstance().isMicrobit_connected()) {
            try {
                bluetooth_le_adapter.disconnect();
                // may already have unbound. No API to check state so....
                unbindService(mServiceConnection);
            } catch (Exception e) {
            }
        }
        finish();
    }

    private Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;
            String service_uuid = "";
            String characteristic_uuid = "";
            byte[] b = null;
            TextView value_text = null;
            String descriptor_uuid = "";

            switch (msg.what) {
                case BleAdapterService.GATT_CONNECTED:
//                    showMsg(Utility.htmlColorGreen("Connected"));
//                    showMsg(Utility.htmlColorGreen("Discovering services..."));
                    bluetooth_le_adapter.discoverServices();
                    break;
                case BleAdapterService.GATT_DISCONNECT:
//                    showMsg(Utility.htmlColorRed("Disconnected"));
//                    ((LinearLayout) MenuActivity.this.findViewById(R.id.menu_items_area)).setVisibility(View.VISIBLE);
                    break;
                case BleAdapterService.GATT_SERVICES_DISCOVERED:
                    Log.d(Constants.TAG, "XXXX Services discovered");
//                    showMsg(Utility.htmlColorGreen("Ready"));
//                    ((LinearLayout) MenuActivity.this.findViewById(R.id.menu_items_area)).setVisibility(View.VISIBLE);
                    List<BluetoothGattService> slist = bluetooth_le_adapter.getSupportedGattServices();
                    for (BluetoothGattService svc : slist) {
                        Log.d(Constants.TAG, "UUID=" + svc.getUuid().toString().toUpperCase() + " INSTANCE=" + svc.getInstanceId());
                        MicroBit.getInstance().addService(svc);
                    }
                    MicroBit.getInstance().setMicrobit_services_discovered(true);
                    break;

//              ===========================================
                case BleAdapterService.GATT_CHARACTERISTIC_READ:
//                    Toast.makeText(CountActivity.this, "JESE VIDI?", Toast.LENGTH_SHORT).show();
                    Log.d(Constants.TAG, "Handler received characteristic read result");
                    bundle = msg.getData();
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                    b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                    Log.d(Constants.TAG, "characteristic " + characteristic_uuid + " of service " + service_uuid + " read OK");
                    Log.d(Constants.TAG, "Value=" + Utility.byteArrayAsHexString(b));
                    if (characteristic_uuid.equalsIgnoreCase(Utility.normaliseUUID(BleAdapterService.ACCELEROMETERPERIOD_CHARACTERISTIC_UUID))) {
                        boolean got_accelerometer_period = false;
                        byte [] period_bytes = new byte[2];
                        if (b.length == 2) {
                            period_bytes[0] = b[0];
                            period_bytes[1] = b[1];
                            got_accelerometer_period = true;
                        } else {
                            if (b.length == 1) {
                                period_bytes[0] = b[0];
                                period_bytes[1] = 0x00;
                                got_accelerometer_period = true;
                            } else {
                                Log.d(Constants.TAG,"Couldn't obtain value of accelerometer period");
                            }
                        }
                        if (got_accelerometer_period) {
                            accelerometer_period = (int) Utility.shortFromLittleEndianBytes(period_bytes);
                            Settings.getInstance().setAccelerometer_period((short) accelerometer_period);
//                            showAccelerometerPeriod();
//                            Toast.makeText(CountActivity.this, Integer.toString(accelerometer_period), Toast.LENGTH_SHORT).show();
                        } else {
//                            Toast.makeText(CountActivity.this, "ERROR NO PERIOD", Toast.LENGTH_SHORT).show();
                        }
                    }
                    displayStatus(R.string.a_count_initialized);
                    findViewById(R.id.b_calibration).setVisibility(View.VISIBLE);
                    bluetooth_le_adapter.setNotificationsState(Utility.normaliseUUID(BleAdapterService.ACCELEROMETERSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.ACCELEROMETERDATA_CHARACTERISTIC_UUID), true);
                    break;
                case BleAdapterService.GATT_CHARACTERISTIC_WRITTEN:
                    Log.d(Constants.TAG, "Handler received characteristic written result");
                    bundle = msg.getData();
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                    Log.d(Constants.TAG, "characteristic " + characteristic_uuid + " of service " + service_uuid + " written OK");
//                    showAccelerometerPeriod();
//                    showMsg(Utility.htmlColorGreen("Ready"));
                    break;
                case BleAdapterService.GATT_DESCRIPTOR_WRITTEN:
                    Log.d(Constants.TAG, "Handler received descriptor written result");
                    bundle = msg.getData();
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                    descriptor_uuid = bundle.getString(BleAdapterService.PARCEL_DESCRIPTOR_UUID);
                    Log.d(Constants.TAG, "descriptor " + descriptor_uuid + " of characteristic " + characteristic_uuid + " of service " + service_uuid + " written OK");
                    if (!exiting) {
//                        showMsg(Utility.htmlColorGreen("Accelerometer Data notifications ON"));
                        notifications_on=true;
                        start_time = System.currentTimeMillis();
                    } else {
//                        showMsg(Utility.htmlColorGreen("Accelerometer Data notifications OFF"));
                        notifications_on=false;
                        finish();
                    }
                    break;

                case BleAdapterService.NOTIFICATION_OR_INDICATION_RECEIVED:
                    bundle = msg.getData();
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                    b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                    Log.d(Constants.TAG, "Value=" + Utility.byteArrayAsHexString(b));
                    if (characteristic_uuid.equalsIgnoreCase((Utility.normaliseUUID(BleAdapterService.ACCELEROMETERDATA_CHARACTERISTIC_UUID)))) {
                        notification_count++;
                        if (System.currentTimeMillis() - start_time >= 60000) {
//                            showBenchmark();
                            notification_count = 0;
                            minute_number++;
                            start_time = System.currentTimeMillis();
                        }
                        byte[] x_bytes = new byte[2];
                        byte[] y_bytes = new byte[2];
                        byte[] z_bytes = new byte[2];
                        System.arraycopy(b, 0, x_bytes, 0, 2);
                        System.arraycopy(b, 2, y_bytes, 0, 2);
                        System.arraycopy(b, 4, z_bytes, 0, 2);
                        short raw_x = Utility.shortFromLittleEndianBytes(x_bytes);
                        short raw_y = Utility.shortFromLittleEndianBytes(y_bytes);
                        short raw_z = Utility.shortFromLittleEndianBytes(z_bytes);
//                        Log.d(Constants.TAG, "Accelerometer Data received: x=" + raw_x + " y=" + raw_y + " z=" + raw_z);


                        // range is -1024 : +1024
                        // Starting with the LED display face up and level (perpendicular to gravity) and edge connector towards your body:
                        // A negative X value means tilting left, a positive X value means tilting right
                        // A negative Y value means tilting away from you, a positive Y value means tilting towards you
                        // A negative Z value means ?

                        accel_input[0] = raw_x / 1000f;
                        accel_input[1] = raw_y / 1000f;
                        accel_input[2] = raw_z / 1000f;
                        if (apply_smoothing) {
                            accel_output = Utility.lowPass(accel_input, accel_output);
                        } else {
                            accel_output[0] = accel_input[0];
                            accel_output[1] = accel_input[1];
                            accel_output[2] = accel_input[2];
                        }

                        processData(accel_output);
//                        double pitch = Math.atan(accel_output[0] / Math.sqrt(Math.pow(accel_output[1], 2) + Math.pow(accel_output[2], 2)));
//                        double roll = Math.atan(accel_output[1] / Math.sqrt(Math.pow(accel_output[0], 2) + Math.pow(accel_output[2], 2)));
                        //convert radians into degrees
//                        pitch = pitch * (180.0 / Math.PI);
//                        roll = -1 * roll * (180.0 / Math.PI);

//                        showAccelerometerData(accel_output,pitch,roll);

                    }
                    break;
                case BleAdapterService.GATT_REMOTE_RSSI:
                    bundle = msg.getData();
                    int rssi = bundle.getInt(BleAdapterService.PARCEL_RSSI);
//                    PeripheralControlActivity.this.updateRssi(rssi);
                    break;
                case BleAdapterService.MESSAGE:
                    bundle = msg.getData();
                    String text = bundle.getString(BleAdapterService.PARCEL_TEXT);
//                    showMsg(Utility.htmlColorRed(text));
                    break;
            }
        }
    };

    private void processData(final float[] accel_output) {
        if (calibrating) {
            calibrationData.add(new AccelerometerData(accel_output));
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                float a = 0;
                for (int i = 0; i < 3; i++) {
                    a += Math.pow(accel_output[i], 2);
                }
                ((TextView) findViewById(R.id.text_acc)).setText(Float.toString(a));
            }
        });

    }

    private void connectToDevice() {
//        showMsg(Utility.htmlColorBlue("Connecting to micro:bit"));
        if (bluetooth_le_adapter.connect(MicroBit.getInstance().getMicrobit_address())) {
            Log.d(Constants.TAG, "CONNECTED!");
            // TODO: Show toast
        } else {
            Log.d(Constants.TAG, "NOT CONNECTED!");
            // TODO: Show toast
//            showMsg(Utility.htmlColorRed("onConnect: failed to connect"));
        }
    }

    @Override
    public void connectionStatusChanged(boolean connected) {
        if (connected) {
            displayStatus(R.string.a_count_connected);
        } else {
            displayStatus(R.string.a_count_disconnected);
        }
    }

    void displayStatus(final int id) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView)findViewById(R.id.text_jumps)).setText(id);
            }
        });
    }

    @Override
    public void serviceDiscoveryStatusChanged(boolean new_state) {

    }

    public void startCalibrating(final View view) {
        calibrating = true;
        ((Button)view).setEnabled(false);
        ((Button)view).setText(R.string.a_count_b_calib);
        new CountDownTimer(10000, 200) {
            int secondsLeft = 0;
            @Override
            public void onTick(final long ms) {
                if (Math.round((float)ms / 1000.0f) != secondsLeft) {
                    secondsLeft = Math.round((float)ms / 1000.0f);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.text_jumps)).setText(String.valueOf(secondsLeft));
                        }
                    });
                }
            }

            @Override
            public void onFinish() {
                calibrating = false;
                displayStatus(R.string.a_count_done);
                ((Button)view).setText(R.string.a_count_b_re);
                view.setEnabled(true);
                Statistics.fit(calibrationData);
            }
        }.start();
    }
}
