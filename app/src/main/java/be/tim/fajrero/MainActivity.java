package be.tim.fajrero;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.eclipse.moquette.server.Server;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            new Server().startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
