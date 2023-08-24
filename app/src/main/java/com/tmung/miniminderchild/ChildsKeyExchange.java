package com.tmung.miniminderchild;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class ChildsKeyExchange {
    private KeyPair keyPair;
    private PublicKey parentPublicKey; // Parent's public key received from the parent's app

    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(2048); // Adjust the key size as needed
            keyPair = keyPairGenerator.generateKeyPair(); // Add this line

            PublicKey publicKey = keyPair.getPublic();
            // Send the public key to the parent's app
            String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            // ... send publicKeyString to the parent's app ...

            return keyPair;
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception as needed
            return null;
        }
    }

    public SecretKey deriveAESKey(byte[] sharedSecret, byte[] salt, int keyLength) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(Base64.getEncoder().encodeToString(sharedSecret).toCharArray(), salt, 65536, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        return key;
    }

    public void setParentPublicKey(String publicKeyString) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
            parentPublicKey = KeyFactory.getInstance("DH").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception as needed
        }
    }

    public byte[] calculateSharedSecret() {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(parentPublicKey, true);
            return keyAgreement.generateSecret();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception as needed
            return null;
        }
    }

    // Use the shared secret to derive the encryption key (e.g., AES key) for secure communication
    // ... derive the encryption key ...
}
