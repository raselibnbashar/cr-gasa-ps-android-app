package com.example.cr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText userIdInput, passwordInput;
    private MaterialButton loginBtn;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_login);

        userIdInput = findViewById(R.id.emailInput); // Note: keeping ID for consistency with your XML
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);

        databaseReference = FirebaseDatabase.getInstance().getReference("officer");
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        loginBtn.setOnClickListener(v -> {
            String userId = userIdInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (userId.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "অনুগ্রহ করে সব তথ্য দিন", Toast.LENGTH_SHORT).show();
                return;
            }

            loginBtn.setEnabled(false);
            loginBtn.setText("লগইন হচ্ছে...");

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean found = false;
                    for (DataSnapshot officerSnap : snapshot.getChildren()) {
                        String dbUserId = officerSnap.child("user_id").getValue(String.class);
                        String dbPassword = officerSnap.child("user_password").getValue(String.class);

                        if (userId.equals(dbUserId) && password.equals(dbPassword)) {
                            found = true;
                            
                            // Save login state and officer data if needed
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putString("userId", userId);
                            editor.putString("officerName", officerSnap.child("name").getValue(String.class));
                            editor.apply();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                            break;
                        }
                    }

                    if (!found) {
                        loginBtn.setEnabled(true);
                        loginBtn.setText("লগইন");
                        Toast.makeText(LoginActivity.this, "ভুল ইউজার আইডি বা পাসওয়ার্ড", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("লগইন");
                    Toast.makeText(LoginActivity.this, "ডেটাবেস ত্রুটি: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
