package com.tmung.miniminderchild;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    private SharedViewModel sharedViewModel;
    byte[] salt = new byte[16]; // Define a byte array for the salt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        // Set the action bar title
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Child's Phone");
        }

        Button startTrackingButton = findViewById(R.id.btn_startLocUpdates);
        Button stopTrackingButton = findViewById(R.id.btn_stopLocUpdates);
        Switch sw_updates = findViewById(R.id.sw_updates);

        startTrackingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationService.class);

            // Add AES key to the intent
            SecretKey aesKey = sharedViewModel.getAesKey().getValue();
            if (aesKey != null) {
                intent.putExtra("aesKey", aesKey.getEncoded());
            }

            this.startService(intent);
            Toast.makeText(this, "STARTED sending location updates", Toast.LENGTH_SHORT).show();
            sw_updates.setChecked(true);
        });

        stopTrackingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationService.class);
            this.stopService(intent);
            Toast.makeText(this, "STOPPED sending location updates", Toast.LENGTH_SHORT).show();
            sw_updates.setChecked(false);
        });

        // Initialise FirebaseApp
        FirebaseApp.initializeApp(this);

        // Diffie-Hellman key exchange to encrypt data
        ChildsKeyExchange keyExchange = new ChildsKeyExchange();
        KeyPair keyPair = keyExchange.generateKeyPair(); // Generate the public/private key pair

        String childsPublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // Send child's public key to Firebase, so parent's app can retrieve and decrypt the data
        DatabaseReference childPublicKeyRef = FirebaseDatabase.getInstance().getReference("childPublicKey");
        childPublicKeyRef.setValue(childsPublicKey);

        DatabaseReference parentPublicKeyRef = FirebaseDatabase.getInstance().getReference("parentPublicKey");
        parentPublicKeyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String parentPublicKeyString = snapshot.getValue(String.class);
                    try {
                        // Set child's public key
                        keyExchange.setParentPublicKey(parentPublicKeyString);

                        // Now perform key agreement to obtain the shared secret
                        byte[] sharedSecret = keyExchange.calculateSharedSecret();

                        // Use sharedSecret to derive an AES key
                        SecretKey aesKey = keyExchange.deriveAESKey(sharedSecret, salt, 256); // Use 256-bit key for AES
                        // Pass it to ViewModel
                        sharedViewModel.setAesKey(aesKey);

                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        e.printStackTrace();
                        // Handle the exception as needed
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errors
            }
        });
    } // End onCreate here
}