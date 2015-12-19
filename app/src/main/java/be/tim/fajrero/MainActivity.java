package be.tim.fajrero;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnFocusChange;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final int PUBLISH_INTERVAL = 5000;

    @Bind(R.id.debug) TextView debug;
    @Bind(R.id.ssid) EditText ssid;
    @Bind(R.id.password) EditText password;
    @Bind(R.id.broker) EditText broker;
    @Bind(R.id.clientName) EditText clientName;
    @Bind(R.id.showPassword) CheckBox showPassword;
    @Bind(R.id.all_in_one) Button allInOne;
    @Bind(R.id.stop_all_in_one) Button stopAllInOne;
    @Bind(R.id.progress) ProgressBar progress;

    private WifiApManager wifiApManager;
    private MqttAndroidClient client;
    private Server server;
    private Handler handler;
    private int publishCount = 0;
    private Prefser prefser;
    private boolean isRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Initialize wifi access point mananger
        wifiApManager = new WifiApManager(this);

        // Initialize handler for Main
        handler = new Handler();

        // Initialize SharedPreferences helper used to store EditText values
        prefser = new Prefser(this);

        restoreSetupInfo();
        refreshViews();
    }

    private void refreshViews() {
        refreshDebugInfo();
        refreshActionButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    private void executeAll() {
        progress.setVisibility(View.VISIBLE);
        isRunning = true;

        startAccessPoint();
        startMqttServer();
        startMqttClient();

        // Publish message every 5 seconds
        startPublishing();

        refreshViews();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshViews();
                progress.setVisibility(View.GONE);
            }
        }, 1500);
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
                    Log.d(TAG, "onSuccess() called with: " + "asyncActionToken = [" + asyncActionToken + "]");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "onFailure() called with: " + "asyncActionToken = [" + asyncActionToken + "], exception = [" + exception + "]");
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
        server = new Server();

        try {
            server.startServer();
        } catch (IOException e) {
            Log.e(TAG, "MqttException Occured", e);
            server = null;
        }
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
        config.SSID = "CSsetupwifi";
        config.preSharedKey  = "cheapspark";
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

        MqttMessage message = new MqttMessage("Hello, I am Android Mqtt Client.".getBytes());
        message.setQos(2);
        message.setRetained(false);
        try {
            client.publish("hallo", message);
            publishCount++;
            Log.i(TAG, "Message published");
        } catch (MqttException e) {
            Log.e(TAG,  "MqttException Occured", e);
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointerException Occured", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception Occured", e);
        }
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
