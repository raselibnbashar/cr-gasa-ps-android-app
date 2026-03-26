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
    private TextView headerTitle;
    private DatabaseReference officerRef;
    private Officer editOfficer;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_officer);

        officerRef = FirebaseDatabase.getInstance().getReference("officer");

        headerTitle = findViewById(R.id.headerTitle);
        inputName = findViewById(R.id.inputOfficerName);
        inputMobile = findViewById(R.id.inputOfficerMobile);
        inputWard = findViewById(R.id.inputOfficerWard);
        inputUserId = findViewById(R.id.inputOfficerUserId);
        inputPassword = findViewById(R.id.inputOfficerPassword);
        saveBtn = findViewById(R.id.saveOfficerBtn);
        backBtn = findViewById(R.id.backBtn);

        editOfficer = (Officer) getIntent().getSerializableExtra("officer");
        if (editOfficer != null) {
            isEditMode = true;
            if (headerTitle != null) headerTitle.setText("অফিসার আপডেট করুন");
            saveBtn.setText("আপডেট করুন");
            populateFields();
        }

        backBtn.setOnClickListener(v -> finish());
        saveBtn.setOnClickListener(v -> saveOfficer());
    }

    private void populateFields() {
        inputName.setText(editOfficer.getName_rank());
        inputMobile.setText(editOfficer.getMobile());
        inputWard.setText(editOfficer.getWard_no());
        inputUserId.setText(editOfficer.getUser_id());
        inputPassword.setText(editOfficer.getUser_password());
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
//        if (TextUtils.isEmpty(userId)) {
//            inputUserId.setError("ইউজার আইডি প্রয়োজন");
//            return;
//        }
        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("পাসওয়ার্ড প্রয়োজন");
            return;
        }

        saveBtn.setEnabled(false);
        saveBtn.setText(isEditMode ? "আপডেট করা হচ্ছে..." : "সংরক্ষণ করা হচ্ছে...");

        if (isEditMode) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name_rank", name);
            updates.put("mobile", mobile);
            updates.put("ward_no", ward);
            updates.put("user_id", userId);
            updates.put("user_password", password);

            officerRef.child(String.valueOf(editOfficer.getSl_no())).updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(AddOfficerActivity.this, "অফিসার আপডেট করা হয়েছে", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    saveBtn.setEnabled(true);
                    saveBtn.setText("আপডেট করুন");
                    Toast.makeText(AddOfficerActivity.this, "ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
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
}
