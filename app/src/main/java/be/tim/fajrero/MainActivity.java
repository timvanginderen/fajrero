package be.tim.fajrero;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
//    private MqttAndroidClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        try {
//            new Server().startServer();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


//        final String brokerUri = "tcp://0.0.0.0:1883";
        final String brokerUri = "tcp://192.168.1.121:1883";
        final String clientId = "Fajrero";


        final MqttAndroidClient client;
        client = createClient(this, brokerUri, clientId);

        // create a client handle
        final String clientHandle = brokerUri + clientId;
        final boolean ssl = false;
        final int port = 1883;
//        final String host = "0.0.0.0";
        final String host = "192.168.1.121";

        Connection connection = new Connection(clientHandle, clientId, host, port, this, client, ssl);

        connection.registerChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                Log.d(TAG, "propertyChange() called with: " + "event = [" + event + "]");
            }
        });
        // connect client

        String[] actionArgs = new String[1];
        actionArgs[0] = clientId;

        MqttConnectOptions conOpt = new MqttConnectOptions();
        conOpt.setCleanSession(true);

        try {
            client.connect(conOpt, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "onSuccess() called with: " + "asyncActionToken = [" + asyncActionToken + "]");

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






//
//        final MqttAndroidClient mqttAndroidClient = new MqttAndroidClient(getApplicationContext(),
//                brokerUri, clientId);
//
//        try {
//            mqttAndroidClient.connect();
//            mqttAndroidClient.setCallback(new MqttCallback() {
//                @Override
//                public void connectionLost(Throwable throwable) {
//                    Log.d(TAG, "connectionLost() called with: "
//                            + "throwable = [" + throwable + "]");
//                }
//
//                @Override
//                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
//                    Log.d(TAG, "messageArrived() called with: "
//                            + "s = [" + s + "], mqttMessage = [" + mqttMessage + "]");
//                }
//
//                @Override
//                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
//                    Log.d(TAG, "deliveryComplete() called with: "
//                            + "iMqttDeliveryToken = [" + iMqttDeliveryToken + "]");
//                }
//            });
//
//            final String topic = "joep";
//            final int qos = 2;
//
//            mqttAndroidClient.subscribe(topic, qos, getApplicationContext(), new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken iMqttToken) {
//                    Log.d(TAG, "onSuccess() called with: "
//                            + "iMqttToken = [" + iMqttToken + "]");
//                }
//
//                @Override
//                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
//                    Log.d(TAG, "onFailure() called with: "
//                            + "iMqttToken = [" + iMqttToken + "], throwable = [" + throwable + "]");
//                }
//            });
//
////            new Handler().postDelayed(new Runnable() {
////                @Override
////                public void run() {
////                    String message = "joep";
////                    try {
////                        mqttAndroidClient.publish("test", new MqttMessage(message.getBytes()));
////                    } catch (MqttException e) {
////                        e.printStackTrace();
////                    }
////                }
////            }, 5000);
//
//
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }




//        Log.i(TAG, "MQTT Start");
//        MemoryPersistence memPer = new MemoryPersistence();
//        client = new MqttAndroidClient(getApplicationContext(), "tcp://192.168.1.121:1883", clientId, memPer);
//
//
//        new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        client.connect(getApplicationContext(), new IMqttActionListener() {
//
//                            @Override
//                            public void onSuccess(IMqttToken mqttToken) {
//                                Log.i(TAG, "Client connected");
//                                Log.i(TAG, "Topics=" + mqttToken.getTopics());
//
//                                MqttMessage message = new MqttMessage("Hello, I am Android Mqtt Client.".getBytes());
//                                message.setQos(2);
//                                message.setRetained(false);
//
//                                try {
//                                    client.publish("messages", message);
//
//                                    Log.i(TAG, "Message published");
//
//                                    client.disconnect();
//                                    Log.i(TAG, "client disconnected");
//                                } catch (MqttPersistenceException e) {
//                                    // TODO Auto-generated catch block
//                                    e.printStackTrace();
//                                } catch (MqttException e) {
//                                    // TODO Auto-generated catch block
//                                    e.printStackTrace();
//                                }
//
//
//                            }
//
//                            @Override
//                            public void onFailure(IMqttToken arg0, Throwable arg1) {
//                                // TODO Auto-generated method stub
//                                Log.i(TAG, "Client connection failed: " + arg1.getMessage());
//
//                            }
//                        });
//                    } catch (MqttException e) {
//                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//
//                }
//            }, 5000);


    }




    /**
     * Create a fully initialised <code>MqttAndroidClient</code> for the parameters given
     * @param context The Applications context
     * @param serverURI The ServerURI to connect to
     * @param clientId The clientId for this client
     * @return new instance of MqttAndroidClient
     */
    private MqttAndroidClient createClient(Context context, String serverURI, String clientId)
    {
        MqttAndroidClient client = new MqttAndroidClient(context, serverURI, clientId);
        return client;
    }

}
