package com.tmung.miniminderchild;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.security.keystore.KeyProperties;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class LocationService extends Service {

    private SecretKey aesKey;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    private final String CHANNEL_ID = "123"; // define your own channel ID
    private final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // Get the best most recent location.
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Encrypt location data before saving it to Firebase.
                    try {
                        String encryptedLocation = encrypt(location.getLatitude(), location.getLongitude());
                        DatabaseReference encryptedLocationRef = FirebaseDatabase.getInstance().getReference("encryptedLocation");
                        encryptedLocationRef.setValue(new LocationData(encryptedLocation)); // Note that LocationData will need to be modified to handle a single String instead of two Doubles.
                    } catch (Exception e) {
                        // Handle any encryption errors.
                        // ...
                    }
                }
            }
        };

        createNotificationChannel();
    } // End of onCreate

    public String encrypt(double lat, double lon) throws Exception {
        // Convert the doubles to a string
        String plainText = lat + "," + lon;

        Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Prepend the IV to the ciphertext
        byte[] iv = cipher.getIV();
        byte[] ivAndCiphertext = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, ivAndCiphertext, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, ivAndCiphertext, iv.length, encryptedBytes.length);

        // Encode the IV and ciphertext bytes to a Base64 string
        return Base64.getEncoder().encodeToString(ivAndCiphertext);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // provide your own icon
                .setContentTitle("Location Service")
                .setContentText("Tracking child location...")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Retrieve the AES key from the intent
        if (intent != null && intent.hasExtra("aesKey")) {
            byte[] aesKeyBytes = intent.getByteArrayExtra("aesKey");
            this.aesKey = new SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");
        }


        startForeground(NOTIFICATION_ID, builder.build());

        startLocationUpdates();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(
                LocationRequest.create().setInterval(1000).setFastestInterval(500).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void createNotificationChannel() {
        CharSequence name = "LocationService";
        String description = "Used for getting child's location";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}