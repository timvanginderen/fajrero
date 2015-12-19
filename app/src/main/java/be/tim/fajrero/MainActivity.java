package be.tim.fajrero;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final int PUBLISH_INTERVAL = 5000;
    private MqttAndroidClient client;
    private WifiApManager wifiApManager;
    @Bind(R.id.debug) TextView debug;
    private Server server;
    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Initialize wifi access point mananger
        wifiApManager = new WifiApManager(this);
        handler = new Handler();

        refreshDebugInfo();
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
        executeAll();
    }

    @OnClick(R.id.stop_all_in_one)
    public void stopAllInOneClicked(View view) {
        stopAll();
    }

    private void executeAll() {
        startAccessPoint();
        startMqttServer();
        startMqttClient();

        // Publish message every 5 seconds
        startPublishing();

        refreshDebugInfo();
    }

    private void stopAll() {
        stopPublishing();
        disconnectMqttClient();
        stopMqttServer();
        stopAccessPoint();

        refreshDebugInfo();
    }


    /**
     * Must be called from UI thread
     */
    private void startPublishing() {
        publisher.run();
    }

    private void stopPublishing() {
        handler.removeCallbacksAndMessages(null);
    }

    Runnable publisher = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Publishing message, run() called");
            publishMessage();
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
        if (client == null) {
            final String brokerUri = "tcp://0.0.0.0:1883";
            final String clientId = "Fajrero";

            client = createClient(this, brokerUri, clientId);
        }
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
        config.SSID = "Waldo's mama";
        config.preSharedKey  = "p@ssw0rd";
//        config.hiddenSSID = false;
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return config;
    }

    private void refreshDebugInfo() {

        final StringBuilder builder = new StringBuilder();

        wifiApManager.getClientList(false, new FinishScanListener() {

            @Override
            public void onFinishScan(final ArrayList<ClientScanResult> clients) {

                final String accessPoint = "AccessPoint status: " + wifiApManager.getWifiApState();
                builder.append(accessPoint);
                builder.append("\n");

                final String clientsConnected = "Clients connnected: " + clients.size() + "\n";
                builder.append(clientsConnected);

                final String serverStatus = "MQTT server status: " + (MainActivity.this.server == null ? "stopped" : "started");
                builder.append(serverStatus);
                builder.append("\n");

                final String clientStatus = "MQTT client status: " + (isClientConnected() ? "connected" : "disconnected");
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
