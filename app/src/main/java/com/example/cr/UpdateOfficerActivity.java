package com.example.cr;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class UpdateOfficerActivity extends AppCompatActivity {

    private TextInputEditText inputName, inputMobile, inputWard, inputUserId, inputPassword;
    private MaterialButton updateBtn;
    private ImageView backBtn;
    private DatabaseReference officerRef;
    private Officer editOfficer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_officer);

        officerRef = FirebaseDatabase.getInstance().getReference("officer");

        inputName = findViewById(R.id.inputOfficerName);
        inputMobile = findViewById(R.id.inputOfficerMobile);
        inputWard = findViewById(R.id.inputOfficerWard);
        inputUserId = findViewById(R.id.inputOfficerUserId);
        inputPassword = findViewById(R.id.inputOfficerPassword);
        updateBtn = findViewById(R.id.updateOfficerBtn);
        backBtn = findViewById(R.id.backBtn);

        editOfficer = (Officer) getIntent().getSerializableExtra("officer");
        if (editOfficer != null) {
            populateFields();
        } else {
            Toast.makeText(this, "Officer data not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        backBtn.setOnClickListener(v -> finish());
        updateBtn.setOnClickListener(v -> updateOfficer());
    }

    private void populateFields() {
        inputName.setText(editOfficer.getName_rank());
        inputMobile.setText(editOfficer.getMobile());
        inputWard.setText(editOfficer.getWard_no());
        inputUserId.setText(editOfficer.getUser_id());
        inputPassword.setText(editOfficer.getUser_password());
    }

    private void updateOfficer() {
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
        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("পাসওয়ার্ড প্রয়োজন");
            return;
        }

        updateBtn.setEnabled(false);
        updateBtn.setText("আপডেট করা হচ্ছে...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("name_rank", name);
        updates.put("mobile", mobile);
        updates.put("ward_no", ward);
        updates.put("user_id", userId);
        updates.put("user_password", password);

        // Use key if available, fallback to sl_no
        String updateKey = editOfficer.getKey() != null ? editOfficer.getKey() : String.valueOf(editOfficer.getSl_no());

        officerRef.child(updateKey).updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(UpdateOfficerActivity.this, "অফিসার আপডেট করা হয়েছে", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                updateBtn.setEnabled(true);
                updateBtn.setText("আপডেট করুন");
                Toast.makeText(UpdateOfficerActivity.this, "ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
