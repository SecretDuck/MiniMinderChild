package com.tmung.miniminderchild;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
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

    private FirebaseAuth firebaseAuth;
    private DatabaseReference isLocationUpdatingRef;
    private ValueEventListener isLocationUpdatingListener;
    private SharedViewModel sharedViewModel;
    // Define a byte array for the salt
    byte[] salt = { (byte) 0xa9, (byte) 0x0f, (byte) 0x3b, (byte) 0x7e, (byte) 0xb3, (byte) 0x21, (byte) 0x4a, (byte) 0x6f, (byte) 0x85, (byte) 0xc9, (byte) 0xe0, (byte) 0xf1, (byte) 0x5d, (byte) 0x6c, (byte) 0x7b, (byte) 0x8a };

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
        Button logoutButton = findViewById(R.id.btnLogout);

        startTrackingButton.setOnClickListener(v -> startTracking());
        stopTrackingButton.setOnClickListener(v -> stopTracking());
        logoutButton.setOnClickListener(v -> logout());

        // Initialise FirebaseApp
        FirebaseApp.initializeApp(this);

        // Diffie-Hellman key exchange to encrypt data
        ChildsKeyExchange keyExchange = new ChildsKeyExchange();
        KeyPair keyPair = keyExchange.generateKeyPair(); // Generate the public/private key pair

        String childsPublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // Send child's public key to Firebase, so parent's app can retrieve and decrypt the data
        sendChildPublicKey(childsPublicKey);

        // Listen for changes to the isLocationUpdating flag
        isLocationUpdatingRef = FirebaseDatabase.getInstance().getReference("isLocationUpdating");
        isLocationUpdatingListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isLocationUpdating = snapshot.getValue(Boolean.class);

                    if (isLocationUpdating != null && isLocationUpdating) {
                        // If the flag is true, then send the child's public key again, just in case it send first time
                        sendChildPublicKey(childsPublicKey);

                        // Start tracking location
                        startTracking();
                    } else {
                        stopTracking();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle database read error
                Toast.makeText(MainActivity.this, "Database read error", Toast.LENGTH_SHORT).show();
            }
        };
        isLocationUpdatingRef.addValueEventListener(isLocationUpdatingListener);

        // Get parent's public key and generate aesKey
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

    // Upon destroying activity, remove the listener to prevent memory leaks
    @Override
    protected void onDestroy() {
        isLocationUpdatingRef.removeEventListener(isLocationUpdatingListener);
        super.onDestroy();
    }

    private void sendChildPublicKey(String childsPublicKey) {
        // Send child's public key to Firebase, so parent's app can retrieve and decrypt the data
        DatabaseReference childPublicKeyRef = FirebaseDatabase.getInstance().getReference("childPublicKey");
        childPublicKeyRef.setValue(childsPublicKey);
    }

    private void startTracking() {
        Intent intent = new Intent(this, LocationService.class);

        // Add AES key to the intent
        SecretKey aesKey = sharedViewModel.getAesKey().getValue();
        if (aesKey != null) {
            intent.putExtra("aesKey", aesKey.getEncoded());
        }

        this.startService(intent);
        Toast.makeText(this, "Started sending location updates", Toast.LENGTH_SHORT).show();
        Switch sw_updates = findViewById(R.id.sw_updates);
        sw_updates.setChecked(true);

        // Set the isLocationUpdating flag in the Firebase database to true
        DatabaseReference isLocationUpdatingRef = FirebaseDatabase.getInstance().getReference("isLocationUpdating");
        isLocationUpdatingRef.setValue(true);
    }

    private void stopTracking() {
        Intent intent = new Intent(this, LocationService.class);
        this.stopService(intent);
        Toast.makeText(this, "Not sending location updates", Toast.LENGTH_SHORT).show();
        Switch sw_updates = findViewById(R.id.sw_updates);
        sw_updates.setChecked(false);

        // Set the isLocationUpdating flag in the Firebase database to false
        DatabaseReference isLocationUpdatingRef = FirebaseDatabase.getInstance().getReference("isLocationUpdating");
        isLocationUpdatingRef.setValue(false);
    }

    // Method to log out of app
    public void logout() {
        // Set the isLocationUpdating flag in the Firebase database to false
        DatabaseReference isLocationUpdatingRef = FirebaseDatabase.getInstance().getReference("isLocationUpdating");
        isLocationUpdatingRef.setValue(false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signOut();

        // After logging out, navigate back to the LoginActivity
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

}