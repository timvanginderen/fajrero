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
import java.util.concurrent.TimeUnit;

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
    private ScheduledThreadPoolExecutor threadPoolExecutor;
    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Initialize wifi access point mananger
        wifiApManager = new WifiApManager(this);
        handler = new Handler();

        displayAccessPointInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnectMqttClient();

        stopPublishing();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //try to connect mqqt client all the time ATM
        startMqttClient();
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
        displayAccessPointInfo();
    }

    @OnClick(R.id.all_in_one)
    public void allInOneClicked(View view) {
        executeAll();
    }

    private void executeAll() {
        startAccessPoint();
        startMqttServer();
        startMqttClient();

        // Publish message every 5 seconds
        startPublishing();

    }

    /**
     * Must be called from UI thread
     */
    private void startPublishing() {
        publisher.run();
    }

    private void stopPublishing() {
        handler.removeCallbacks(publisher);
    }

    Runnable publisher = new Runnable() {
        @Override
        public void run() {
            publishMessage();
            handler.postDelayed(publisher, PUBLISH_INTERVAL);
        }
    };

    private void startAccessPoint() {
        WifiConfiguration config = getWifiConfiguration();
        wifiApManager.setWifiApEnabled(config, true);
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
        conOpt.setCleanSession(true);

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
        }
        catch (MqttException e) {
            Log.e(this.getClass().getCanonicalName(),
                    "MqttException Occured", e);
        }
    }

    private void disconnectMqttClient() {
        if (client == null) {
            return;
        }

        try {
            client.disconnect();
        }
        catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void startMqttServer() {
        server = new Server();

        try {
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopMqttServer() {
        if (server == null) {
            return;
        }
        server.stopServer();
    }

    @NonNull
    private WifiConfiguration getWifiConfiguration() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "Waldo's mama";
        config.preSharedKey  = "p@ssw0rd";
//			config.hiddenSSID = true;
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return config;
    }

    private void displayAccessPointInfo() {
        wifiApManager.getClientList(false, new FinishScanListener() {

            @Override
            public void onFinishScan(final ArrayList<ClientScanResult> clients) {

                debug.setText("WifiApState: " + wifiApManager.getWifiApState() + "\n\n");
                debug.append("Clients: \n");
                for (ClientScanResult clientScanResult : clients) {
                    debug.append("-------------------\n");
                    debug.append("IpAddr: " + clientScanResult.getIpAddr() + "\n");
                    debug.append("Device: " + clientScanResult.getDevice() + "\n");
                    debug.append("HWAddr: " + clientScanResult.getHWAddr() + "\n");
                    debug.append("isReachable: " + clientScanResult.isReachable() + "\n");
                }
            }
        });
    }

    /**
     * Publish a message on the mqtt broker
     */
    private void publishMessage() {
        if (client == null) {
            displayToast("Not connected to server.");
        }

        try {
            if (!client.isConnected()) {
                displayToast("Not connected to server.");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointerException thrown in client.isConnected()", e);
//            displayToast("Not connected to server.");
        }

        MqttMessage message = new MqttMessage("Hello, I am Android Mqtt Client.".getBytes());
        message.setQos(2);
        message.setRetained(false);
        try {
            client.publish("hallo", message);
            Log.i(TAG, "Message published");
        } catch (MqttException e) {
            Log.e(this.getClass().getCanonicalName(),  "MqttException Occured", e);
        } catch (NullPointerException e) {
            Log.e(this.getClass().getCanonicalName(),  "NullPointerException Occured", e);
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
