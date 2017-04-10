package hr.in24stem.jumper;

import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import hr.in24stem.Constants;
import hr.in24stem.MicroBit;
import hr.in24stem.jumper.bluetooth.BleAdapterService;
import hr.in24stem.jumper.bluetooth.ConnectionStatusListener;

import hr.in24stem.Constants;

public class CountActivity extends AppCompatActivity implements ConnectionStatusListener {

    private BleAdapterService bluetooth_le_adapter;
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
            }
        }
    };

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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView)findViewById(R.id.text_jumps)).setText(R.string.a_count_connected);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView)findViewById(R.id.text_jumps)).setText(R.string.a_count_disconnected);
                }
            });
        }
    }

    @Override
    public void serviceDiscoveryStatusChanged(boolean new_state) {

    }
}
