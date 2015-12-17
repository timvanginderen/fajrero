package be.tim.fajrero;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.whitebyte.wifihotspotutils.WifiApManager;

import org.eclipse.moquette.server.Server;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.internal.ConnectActionListener;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private MqttAndroidClient client;
    private WifiApManager wifiApManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Initialize wifi access point mananger
        wifiApManager = new WifiApManager(this);

        // Start mqtt server
//        startMqttServer();

        // Start mqtt client
//        startMqttClient();

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

    private void startMqttServer() {
        try {
            new Server().startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @OnClick(R.id.publish)
    public void publish(View view) {
        if (client == null) {
            return;
        }
        publishMessage();
    }

    @OnClick(R.id.open_access_point)
    public void openAccessPoint(View view) {
        WifiConfiguration config = getWifiConfiguration();
        wifiApManager.setWifiApEnabled(config, true);
    }
    @OnClick(R.id.close_access_point)
    public void closeAccessPoint(View view) {
        wifiApManager.setWifiApEnabled(null, false);
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

    /**
     * Publish a message on the mqtt broker
     */
    private void publishMessage() {
        MqttMessage message = new MqttMessage("Hello, I am Android Mqtt Client.".getBytes());
        message.setQos(2);
        message.setRetained(false);
        try {
            client.publish("hallo", message);
        } catch (MqttException e) {
            Log.e(this.getClass().getCanonicalName(),
                    "MqttException Occured", e);
        }
        Log.i(TAG, "Message published");
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
