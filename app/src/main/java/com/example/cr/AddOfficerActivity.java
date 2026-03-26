package com.example.cr;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AddOfficerActivity extends AppCompatActivity {

    private TextInputEditText inputName, inputMobile, inputWard, inputUserId, inputPassword;
    private MaterialButton saveBtn;
    private ImageView backBtn;
    private DatabaseReference officerRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_officer);

        officerRef = FirebaseDatabase.getInstance().getReference("officer");

        inputName = findViewById(R.id.inputOfficerName);
        inputMobile = findViewById(R.id.inputOfficerMobile);
        inputWard = findViewById(R.id.inputOfficerWard);
        inputUserId = findViewById(R.id.inputOfficerUserId);
        inputPassword = findViewById(R.id.inputOfficerPassword);
        saveBtn = findViewById(R.id.saveOfficerBtn);
        backBtn = findViewById(R.id.backBtn);

        backBtn.setOnClickListener(v -> finish());
        saveBtn.setOnClickListener(v -> saveOfficer());
    }

    private void saveOfficer() {
        String name = inputName.getText().toString().trim();
        String mobile = inputMobile.getText().toString().trim();
        String ward = inputWard.getText().toString().trim();
        String userId = inputUserId.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            inputName.setError("নাম প্রয়োজন");
            return;
        }
        if (TextUtils.isEmpty(mobile)) {
            inputMobile.setError("মোবাইল নম্বর প্রয়োজন");
            return;
        }
        if (TextUtils.isEmpty(userId)) {
            inputUserId.setError("ইউজার আইডি প্রয়োজন");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("পাসওয়ার্ড প্রয়োজন");
            return;
        }

        saveBtn.setEnabled(false);
        saveBtn.setText("সংরক্ষণ করা হচ্ছে...");

        officerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int maxSlNo = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Object slNoObj = child.child("sl_no").getValue();
                    if (slNoObj != null) {
                        try {
                            int slNo = Integer.parseInt(String.valueOf(slNoObj));
                            if (slNo > maxSlNo) maxSlNo = slNo;
                        } catch (NumberFormatException ignored) {}
                    }
                }

                int newSlNo = maxSlNo + 1;
                Map<String, Object> officer = new HashMap<>();
                officer.put("name_rank", name);
                officer.put("mobile", mobile);
                officer.put("ward_no", ward);
                officer.put("user_id", userId);
                officer.put("user_password", password);
                officer.put("sl_no", newSlNo);

                officerRef.child(String.valueOf(newSlNo)).setValue(officer).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(AddOfficerActivity.this, "অফিসার যোগ করা হয়েছে", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        saveBtn.setEnabled(true);
                        saveBtn.setText("অফিসার যোগ করুন");
                        Toast.makeText(AddOfficerActivity.this, "ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                saveBtn.setEnabled(true);
                saveBtn.setText("অফিসার যোগ করুন");
            }
        });
    }
}
