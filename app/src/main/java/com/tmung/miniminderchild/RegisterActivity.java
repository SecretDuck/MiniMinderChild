package com.tmung.miniminderchild;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private EditText edtTxtEmail, edtTxtPass, edtTxtParentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);  // Using the correct layout file

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        edtTxtEmail = findViewById(R.id.edtTxtEmail);
        edtTxtPass = findViewById(R.id.edtTxtPass);
        edtTxtParentEmail = findViewById(R.id.edtTxtParentEmail);
    }

    public void goToLogin(View view) {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
    }

    public void register(View view) {
        String email = edtTxtEmail.getText().toString().trim();
        String password = edtTxtPass.getText().toString().trim();
        final String parentEmail = edtTxtParentEmail.getText().toString().trim();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Link child to parent
                            linkChildToParent(parentEmail);

                            // Sign up success
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        } else {
                            // If sign up fails, display a message to the user.
                            String errorMessage = task.getException().getMessage();
                            Toast.makeText(getApplicationContext(), "Authentication failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void linkChildToParent(final String parentEmail) {
        Query query = databaseReference.orderByChild("email").equalTo(parentEmail);  // Assuming you store email under each user
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Found parent user
                    String childUserId = firebaseAuth.getCurrentUser().getUid();
                    String parentUserId = "";  // Initialize

                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        parentUserId = userSnapshot.getKey();

                        // Check if this parent is already linked with any child
                        if (userSnapshot.hasChild("linkedAccounts")) {
                            Toast.makeText(RegisterActivity.this, "This parent email is already linked with another child.", Toast.LENGTH_SHORT).show();
                            return;  // exit the method early
                        }
                    }

                    if (!parentUserId.isEmpty()) {
                        // Set up the child
                        databaseReference.child(childUserId).child("role").setValue("child");
                        Map<String, Object> linkedAccounts = new HashMap<>();
                        linkedAccounts.put(parentUserId, true);
                        databaseReference.child(childUserId).child("linkedAccounts").setValue(linkedAccounts);

                        // Update the parent account
                        databaseReference.child(parentUserId).child("linkedAccounts").child(childUserId).setValue(true);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error finding parent account.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Parent email not found.", Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RegisterActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
