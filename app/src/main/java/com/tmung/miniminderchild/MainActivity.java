package com.tmung.miniminderchild;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    private Button startTrackingButton, stopTrackingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the action bar title
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Child's Phone");
        }

        startTrackingButton = findViewById(R.id.btn_startLocUpdates);
        stopTrackingButton = findViewById(R.id.btn_stopLocUpdates);

        startTrackingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationService.class);
            this.startService(intent);
            Toast.makeText(this, "STARTED sending location updates", Toast.LENGTH_SHORT).show();
        });

        stopTrackingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationService.class);
            this.stopService(intent);
            Toast.makeText(this, "STOPPED sending location updates", Toast.LENGTH_SHORT).show();
        });

        // Initialise FirebaseApp
        FirebaseApp.initializeApp(this);
    }
}