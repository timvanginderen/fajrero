package be.tim.fajrero;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pwittchen.prefser.library.Prefser;
import com.whitebyte.wifihotspotutils.ClientScanResult;
import com.whitebyte.wifihotspotutils.FinishScanListener;
import com.whitebyte.wifihotspotutils.WifiApManager;

import org.eclipse.moquette.server.Server;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnFocusChange;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final int PUBLISH_INTERVAL = 5000;
    public static final String MQTT_TOPIC = "/setup";
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    public static final String AP_SSID_NAME = "CSsetupwifi";
    public static final String AP_PASSWORD = "cheapspark";

    @Bind(R.id.debug) TextView debug;
    @Bind(R.id.ssid) EditText ssid;
    @Bind(R.id.password) EditText password;
    @Bind(R.id.broker) EditText broker;
    @Bind(R.id.clientName) EditText clientName;
    @Bind(R.id.showPassword) CheckBox showPassword;
    @Bind(R.id.all_in_one) Button allInOne;
    @Bind(R.id.stop_all_in_one) Button stopAllInOne;
    @Bind(R.id.progress) ProgressBar progress;
    @Bind(R.id.ssid_spinner) Spinner ssidSpinner;

    private WifiApManager wifiApManager;
    private MqttAndroidClient client;
    private Server server;
    private Handler handler;
    private int publishCount = 0;
    private Prefser prefser;
    private boolean isRunning;

    private BroadcastReceiver scanResultsReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Initialize wifi scan broadcast receiver
        scanResultsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final WifiManager wifiManager =
                                (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        List<ScanResult> results = wifiManager.getScanResults();
                        Prefs.putSsids(getApplicationContext(), results);

                        // TODO: 23.12.15 refactor to use populateSpinner()
                        final List<String> ssids = Prefs.getSsids(getApplicationContext());
                        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),
                               R.layout.ssid_spinner_item, ssids);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                ssidSpinner.setAdapter(adapter);

                            }
                        });
                    }
                }).start();
            }
        };

        // Initialize wifi access point mananger
        wifiApManager = new WifiApManager(this);

        // Initialize handler for Main
        handler = new Handler();

        // Initialize SharedPreferences helper used to store EditText values
        prefser = new Prefser(this);

        // Populate ssid spinner
        populateSsidSpinner();

        restoreSetupInfo();
        refreshViews();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (isRunning) {
                        executeAll();
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    private void refreshViews() {
        refreshDebugInfo();
        refreshActionButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(scanResultsReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(scanResultsReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.getScanResults();


    }

    @OnClick(R.id.publish)
    public void publishClicked(View view) {
        publishMessage();
    }

    @OnClick(R.id.start_access_point)
    public void startAccessPointClicked(View view) {
        startAccessPoint();
    }

    @OnClick(R.id.stop_access_point)
    public void stopAccessPointClicked(View view) {
        stopAccessPoint();
    }

    @OnClick(R.id.start_server)
    public void startServerClicked(View view) {
        startMqttServer();
    }

    @OnClick(R.id.stop_server)
    public void stopServerClicked(View view) {
        stopMqttServer();
    }

    @OnClick(R.id.start_client)
    public void startCLientClicked(View view) {
        startMqttClient();
    }

    @OnClick(R.id.stop_client)
    public void stopClientClicked(View view) {
        disconnectMqttClient();
    }

    @OnClick(R.id.refresh_debug)
    public void refreshDebugClicked(View view) {
        refreshDebugInfo();
    }

    @OnClick(R.id.all_in_one)
    public void allInOneClicked(View view) {
        allInOne.setVisibility(View.GONE);
        executeAll();
    }

    @OnClick(R.id.stop_all_in_one)
    public void stopAllInOneClicked(View view) {
        stopAll();
    }

    @OnCheckedChanged(R.id.showPassword)
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) {
            password.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        savePasswordState();
    }

    @OnFocusChange({R.id.ssid, R.id.password, R.id.broker, R.id.clientName})
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            if (getString(R.string.tag_ssid).equals(v.getTag().toString())) {
                saveSsid();
            } else if (getString(R.string.tag_password).equals(v.getTag().toString())) {
                savePassword();
            } else if (getString(R.string.tag_broker).equals(v.getTag().toString())) {
                saveBroker();
            } else if (getString(R.string.tag_clientName).equals(v.getTag().toString())) {
                saveClientName();
            }
        }
    }

    private void saveBroker() {
        prefser.put(Prefs.KEY_BROKER, broker.getText().toString().trim());
    }

    private void saveClientName() {
        prefser.put(Prefs.KEY_CLIENT_NAME, clientName.getText().toString().trim());
    }

    private void savePasswordState() {
        prefser.put(Prefs.KEY_PASSWORD_HIDDEN, !showPassword.isChecked());
    }

    private void savePassword() {
        prefser.put(Prefs.KEY_PASSWORD, password.getText().toString());
    }

    private void saveSsid() {
        prefser.put(Prefs.KEY_SSID, ssid.getText().toString().trim());
    }

    private void restoreSetupInfo() {
        ssid.setText(prefser.get(Prefs.KEY_SSID, String.class, ""));
        password.setText(prefser.get(Prefs.KEY_PASSWORD, String.class, ""));
        broker.setText(prefser.get(Prefs.KEY_BROKER, String.class, ""));
        clientName.setText(prefser.get(Prefs.KEY_CLIENT_NAME, String.class, ""));
        showPassword.setChecked(!prefser.get(Prefs.KEY_PASSWORD_HIDDEN, Boolean.class, true));
    }

    private void populateSsidSpinner() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<String> ssids = Prefs.getSsids(getApplicationContext());
                final ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),
                        R.layout.ssid_spinner_item, ssids);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ssidSpinner.setAdapter(adapter);

                    }
                });
            }
        }).start();
    }


    private void executeAll() {
        progress.setVisibility(View.VISIBLE);
        isRunning = true;

        if (checkAndRequestPermissions()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    startAccessPoint();
                    startMqttServer();
                    startMqttClient();

                    // Publish message every 5 seconds
                    startPublishing();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            refreshViews();
                        }
                    });

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refreshViews();
                            progress.setVisibility(View.GONE);
                        }
                    }, 1500);
                }
            }).start();
        }
    }

    private void stopAll() {
        progress.setVisibility(View.VISIBLE);
        isRunning = false;

        stopPublishing();
        disconnectMqttClient();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopMqttServer();
                stopAccessPoint();

                refreshViews();
                progress.setVisibility(View.GONE);
            }
        }, 1500);
    }

    /**
     * Must be called from UI thread
     */
    private void startPublishing() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                publisher.run();
            }
        }, 5000);
    }

    private void stopPublishing() {
        handler.removeCallbacksAndMessages(null);
    }

    Runnable publisher = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Publishing message, run() called");
            publishMessage();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    refreshViews();
                }
            });
            handler.postDelayed(publisher, PUBLISH_INTERVAL);
        }
    };

    private void startAccessPoint() {
        Log.d(TAG, "startAccessPoint() called");
        WifiConfiguration config = getWifiConfiguration();
        wifiApManager.setWifiApEnabled(config, true);
        Log.d(TAG, "startAccessPoint() finished");
    }

    private void stopAccessPoint() {
        wifiApManager.setWifiApEnabled(null, false);
    }

    private void startMqttClient() {
        createMqttClient();
        connectMqttClient();
    }

    private void createMqttClient() {
        final String brokerUri = "tcp://0.0.0.0:1883";
        final String clientId = "Fajrero";

        client = createClient(this, brokerUri, clientId);
    }

    private void connectMqttClient() {
        MqttConnectOptions conOpt = new MqttConnectOptions();

        try {
            client.connect(conOpt, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "onSuccess() called with: " + "asyncActionToken = ["
                            + asyncActionToken + "]");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "onFailure() called with: " + "asyncActionToken = ["
                            + asyncActionToken + "], exception = [" + exception + "]");
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "MqttException Occured", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception Occured", e);
        }
    }

    private void disconnectMqttClient() {
        if (client == null) {
            return;
        }

        try {
            client.unregisterResources();
            client.disconnect();
        } catch (MqttException e) {
            Log.e(TAG, "MqttException Occured", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception Occured", e);
        }
    }

    private void startMqttServer() {
        if (checkAndRequestPermissions()) {
            server = new Server();

            try {
                server.startServer();
            } catch (IOException e) {
                Log.e(TAG, "MqttException Occured", e);
                server = null;
            }
        }
    }

    /**
     * Check WRITE_EXTERNAL_STORAGE permission
     * @return wether or not the permission is granted
     */
    private boolean checkAndRequestPermissions() {
        // Check write external storage permission
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                // TODO: 21.12.15 show explanation instead
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                return false;

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                return false;

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        return true;
    }

    private void stopMqttServer() {
        if (server == null) {
            return;
        }
        server.stopServer();
        server = null;  //to check status
    }

    @NonNull
    private WifiConfiguration getWifiConfiguration() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = AP_SSID_NAME;
        config.preSharedKey  = AP_PASSWORD;
        config.hiddenSSID = true;
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return config;
    }

    private void refreshActionButtons() {
        allInOne.setVisibility(isRunning ? View.GONE : View.VISIBLE);
        stopAllInOne.setVisibility(isRunning ? View.VISIBLE : View.GONE);
    }

    private void refreshDebugInfo() {

        final StringBuilder builder = new StringBuilder();

        wifiApManager.getClientList(false, new FinishScanListener() {

            @Override
            public void onFinishScan(final ArrayList<ClientScanResult> clients) {

                final String accessPoint = "AccessPoint status: " + wifiApManager.getWifiApState();
                builder.append(accessPoint);
                builder.append("\n");

                final String clientsConnected = "Clients connnected: " + clients.size();
                final String messagesPublished = "Messages published: " + publishCount;
                builder.append(clientsConnected + "  |  " + messagesPublished);
                builder.append("\n");

                final String serverStatus = "MQTT server status: "
                        + (MainActivity.this.server == null ? "stopped" : "started");
                builder.append(serverStatus);
                builder.append("\n");

                final String clientStatus = "MQTT client status: "
                        + (isClientConnected() ? "connected" : "disconnected");
                builder.append(clientStatus);
                builder.append("\n");
                builder.append("\n");
                builder.append("\n");

                if (clients.size() > 0) {
                    builder.append("Connected devices: " + "\n");
                    for (ClientScanResult clientScanResult : clients) {
                        builder.append("-------------------\n");
                        builder.append("IpAddr: " + clientScanResult.getIpAddr() + "\n");
                        builder.append("Device: " + clientScanResult.getDevice() + "\n");
                        builder.append("HWAddr: " + clientScanResult.getHWAddr() + "\n");
                        builder.append("isReachable: " + clientScanResult.isReachable() + "\n");
                    }
                }

                debug.setText(builder.toString());
            }
        });
    }

    private boolean isClientConnected() {
        try {
            return (client != null && client.isConnected());
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in isClientConnected", e);
            return false;
        }
    }

    /**
     * Publish a message on the mqtt broker
     */
    private void publishMessage() {
        if (client == null) {
            displayToast("Not connected to server. client = null");
            stopPublishing();
            return;
        }

        try {
            if (!client.isConnected()) {
                stopPublishing();
                displayToast("Not connected to server. !client.isConnected()");
                return;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointerException thrown in publishMessage", e);
            return;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException thrown in publishMessage", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown in publishMessage", e);
        }

        JSONObject setupInfo = getJsonSetupInfo();

        MqttMessage message = new MqttMessage(setupInfo.toString().getBytes());
        message.setQos(2);
        message.setRetained(false);
        try {
            client.publish(MQTT_TOPIC, message);
            publishCount++;
            Log.i(TAG, "Message published: " + setupInfo.toString());
        } catch (MqttException e) {
            Log.e(TAG,  "MqttException Occured", e);
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointerException Occured", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception Occured", e);
        }
    }

    @NonNull
    private JSONObject getJsonSetupInfo() {
        JSONObject setupInfo = new JSONObject();
        try {
            setupInfo.put("SSID", ssid.getText().toString().trim());
            setupInfo.put("password", password.getText().toString());
            setupInfo.put("broker ip", broker.getText().toString().trim());
            setupInfo.put("client name", clientName.getText().toString().trim());
        } catch (JSONException e) {
            Log.e(TAG, "JSONException Occured", e);
        }
        return setupInfo;
    }

    private void displayToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Create a fully initialised <code>MqttAndroidClient</code> for the parameters given
     * @param context The Applications context
     * @param serverURI The ServerURI to connect to
     * @param clientId The clientId for this client
     * @return new instance of MqttAndroidClient
     */
    private MqttAndroidClient createClient(Context context, String serverURI, String clientId) {
        MqttAndroidClient client = new MqttAndroidClient(context, serverURI, clientId);
        return client;
    }

}
