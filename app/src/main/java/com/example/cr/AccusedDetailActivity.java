package com.example.cr;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class AccusedDetailActivity extends AppCompatActivity {

    private DatabaseReference officerRef, dataRef;
    private Button btnRunning, btnTamil, btnRecall, btnOther;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accused_detail);

        ImageView backBtn = findViewById(R.id.backBtn);
        TextView name = findViewById(R.id.detailName);
        TextView guardian = findViewById(R.id.detailGuardian);
        TextView caseNo = findViewById(R.id.detailCaseNo);
        TextView section = findViewById(R.id.detailSection);
        TextView process = findViewById(R.id.detailProcess);
        TextView address = findViewById(R.id.detailAddress);
        TextView wardPsDist = findViewById(R.id.detailWardPsDist);
        TextView court = findViewById(R.id.detailCourt);
        TextView courtDist = findViewById(R.id.detailCourtDist);
        TextView officerSl = findViewById(R.id.detailOfficerSl);

        btnRunning = findViewById(R.id.btnRunning);
        btnTamil = findViewById(R.id.btnTamil);
        btnRecall = findViewById(R.id.btnRecall);
        btnOther = findViewById(R.id.btnOther);

        backBtn.setOnClickListener(v -> finish());

        Accused accused = (Accused) getIntent().getSerializableExtra("accused");

        if (accused != null && accused.getKey() != null) {
            dataRef = FirebaseDatabase.getInstance().getReference("data").child(accused.getKey()).child("status");
            
            name.setText(accused.getName());
            guardian.setText(accused.getGuardian());
            caseNo.setText(accused.getCase_number());
            section.setText(accused.getSection());
            address.setText(accused.getAddress());
            wardPsDist.setText(String.format("ওয়ার্ডঃ %s, থানাঃ %s, জেলাঃ %s", accused.getWard(), accused.getPs(), accused.getDist()));
            court.setText(accused.getCourt_name());
            courtDist.setText(accused.getCourt_district());
            
            // Display all process numbers from the 'procces' map
            Map<String, String> processMap = accused.getProcces();
            if (processMap != null && !processMap.isEmpty()) {
                // Sorting by year (key) in descending order
                TreeMap<String, String> sortedMap = new TreeMap<>((a, b) -> b.compareTo(a));
                sortedMap.putAll(processMap);
                
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                process.setText(sb.toString().trim());
            } else {
                process.setText("কোন তথ্য নেই");
            }

            officerSl.setText("অফিসার এস.এল নং: " + accused.getOfficer_sl_no());

            fetchOfficerName(accused.getOfficer_sl_no(), officerSl);
            listenToStatusChanges();

            btnRunning.setOnClickListener(v -> updateStatus(1, 1));
            btnTamil.setOnClickListener(v -> updateStatus(0, 2));
            btnRecall.setOnClickListener(v -> updateStatus(0, 3));
            btnOther.setOnClickListener(v -> updateStatus(0, 4));

            if (accused.getStep() >= 2) {
                name.setTextColor(ContextCompat.getColor(this, R.color.emerald_400));
            } else {
                name.setTextColor(ContextCompat.getColor(this, R.color.red_500));
            }
        }
    }

    private void fetchOfficerName(int slNo, TextView tv) {
        officerRef = FirebaseDatabase.getInstance().getReference("officer");
        officerRef.orderByChild("sl_no").equalTo(slNo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String nameRank = child.child("name_rank").getValue(String.class);
                                if (nameRank != null) tv.setText(nameRank);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void listenToStatusChanges() {
        dataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object activeObj = snapshot.child("active").getValue();
                    Object stepObj = snapshot.child("step").getValue();
                    if (activeObj != null && stepObj != null) {
                        int active = Integer.parseInt(activeObj.toString());
                        int step = Integer.parseInt(stepObj.toString());
                        updateButtonUI(active, step);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateButtonUI(int active, int step) {
        // Reset all buttons to default
        int defaultColor = ContextCompat.getColor(this, R.color.slate_800);
        btnRunning.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btnTamil.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btnRecall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btnOther.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));

        if (active == 1 && step == 1) {
            btnRunning.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_600)));
        } else if (active == 0) {
            int emerald = ContextCompat.getColor(this, R.color.emerald_400);
            if (step == 2) btnTamil.setBackgroundTintList(android.content.res.ColorStateList.valueOf(emerald));
            else if (step == 3) btnRecall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(emerald));
            else if (step == 4) btnOther.setBackgroundTintList(android.content.res.ColorStateList.valueOf(emerald));
        }
    }

    private void updateStatus(int active, int step) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("active", active);
        updates.put("step", step);
        dataRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
