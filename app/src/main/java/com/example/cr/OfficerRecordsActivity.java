package com.example.cr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OfficerRecordsActivity extends AppCompatActivity {

    private EditText searchBarMain;
    private TextView searchTotalItem, titleText;
    private TextView tvOfficerName, tvOfficerMobile, tvOfficerTotalRecords, tvOfficerRunningRecords, tvOfficerDoneRecords;
    private LinearLayout layoutRunning, layoutDone, layoutTotal;
    private RecyclerView accusedRecyclerView;
    private AccusedAdapter accusedAdapter;
    private List<Accused> accusedList;
    private List<Accused> allActiveAccusedList;
    private List<Accused> currentFilterList;
    private DatabaseReference databaseReference;
    
    private Button btnRunningFilter, btnDoneFilter, btnTotalFilter;
    private String currentMode = "running";
    private int officerSlNo;
    private ImageView deleteOfficerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_records);

        officerSlNo = getIntent().getIntExtra("officer_sl_no", -1);
        if (officerSlNo == -1) {
            finish();
            return;
        }

        ImageView backBtn = findViewById(R.id.backBtn);
        searchBarMain = findViewById(R.id.searchBarMain);
        searchTotalItem = findViewById(R.id.searchTotalItem);
        titleText = findViewById(R.id.titleText);
        tvOfficerName = findViewById(R.id.tvOfficerName);
        tvOfficerMobile = findViewById(R.id.tvOfficerMobile);
        tvOfficerTotalRecords = findViewById(R.id.tvOfficerTotalRecords);
        tvOfficerRunningRecords = findViewById(R.id.tvOfficerRunningRecords);
        tvOfficerDoneRecords = findViewById(R.id.tvOfficerDoneRecords);
        deleteOfficerBtn = findViewById(R.id.deleteOfficerBtn);
        
        layoutRunning = findViewById(R.id.layoutRunning);
        layoutDone = findViewById(R.id.layoutDone);
        layoutTotal = findViewById(R.id.layoutTotal);

        accusedRecyclerView = findViewById(R.id.accusedFrontList);
        btnRunningFilter = findViewById(R.id.btnRunningFilter);
        btnDoneFilter = findViewById(R.id.btnDoneFilter);
        btnTotalFilter = findViewById(R.id.btnTotalFilter);

        backBtn.setOnClickListener(v -> finish());
        deleteOfficerBtn.setOnClickListener(v -> showDeleteConfirmation());

        // Fetch officer details for the top card
        fetchOfficerDetails(officerSlNo);

        accusedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        accusedList = new ArrayList<>();
        allActiveAccusedList = new ArrayList<>();
        currentFilterList = new ArrayList<>();
        accusedAdapter = new AccusedAdapter(accusedList);
        accusedRecyclerView.setAdapter(accusedAdapter);

        accusedAdapter.setOnItemClickListener(accused -> {
            Intent intent = new Intent(OfficerRecordsActivity.this, AccusedDetailActivity.class);
            intent.putExtra("accused", accused);
            startActivity(intent);
        });

        databaseReference = FirebaseDatabase.getInstance().getReference("data");
        fetchData();

        btnRunningFilter.setOnClickListener(v -> updateFilterMode("running"));
        btnDoneFilter.setOnClickListener(v -> updateFilterMode("done"));
        btnTotalFilter.setOnClickListener(v -> updateFilterMode("total"));

        layoutRunning.setOnClickListener(v -> updateFilterMode("running"));
        layoutDone.setOnClickListener(v -> updateFilterMode("done"));
        layoutTotal.setOnClickListener(v -> updateFilterMode("total"));

        updateFilterMode(currentMode);

        searchBarMain.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showDeleteConfirmation() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnDelete = view.findViewById(R.id.btnDelete);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            deleteOfficer();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteOfficer() {
        FirebaseDatabase.getInstance().getReference("officer")
                .orderByChild("sl_no").equalTo(officerSlNo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                ds.getRef().removeValue().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(OfficerRecordsActivity.this, "অফিসার সফলভাবে মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(OfficerRecordsActivity.this, "মুছে ফেলতে ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchOfficerDetails(int slNo) {
        FirebaseDatabase.getInstance().getReference("officer")
                .orderByChild("sl_no").equalTo(slNo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String name = ds.child("name_rank").getValue(String.class);
                                String mobile = ds.child("mobile").getValue(String.class);
                                if (name != null) {
                                    titleText.setText(name + " এর রেকর্ডসমূহ");
                                    tvOfficerName.setText(name);
                                }
                                if (mobile != null) {
                                    tvOfficerMobile.setText(mobile);
                                }
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateFilterMode(String mode) {
        currentMode = mode;
        int dark = ContextCompat.getColor(this, R.color.slate_800);
        int emerald = ContextCompat.getColor(this, R.color.emerald_400);

        btnRunningFilter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dark));
        btnDoneFilter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dark));
        btnTotalFilter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dark));

        if (mode.equals("running")) {
            btnRunningFilter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(emerald));
        } else if (mode.equals("done")) {
            btnDoneFilter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(emerald));
        } else if (mode.equals("total")) {
            btnTotalFilter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(emerald));
        }
        applyFilter();
    }

    private void fetchData() {
        databaseReference.orderByChild("officer_sl_no").equalTo(officerSlNo).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allActiveAccusedList.clear();
                long total = 0;
                long running = 0;
                long done = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Accused accused = child.getValue(Accused.class);
                        if (accused != null) {
                            accused.setKey(child.getKey());
                            total++;

                            DataSnapshot statusSnap = child.child("status");
                            if (statusSnap.exists()) {
                                Object activeVal = statusSnap.child("active").getValue();
                                if (activeVal != null) {
                                    int active = Integer.parseInt(activeVal.toString());
                                    accused.setActive(active);
                                    if (active == 1) running++;
                                    else if (active == 0) done++;
                                }
                                Object stepVal = statusSnap.child("step").getValue();
                                if (stepVal != null) {
                                    try {
                                        accused.setStep(Integer.parseInt(stepVal.toString()));
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                            allActiveAccusedList.add(accused);
                        }
                    }
                    Collections.reverse(allActiveAccusedList);
                    
                    tvOfficerTotalRecords.setText(String.valueOf(total));
                    tvOfficerRunningRecords.setText(String.valueOf(running));
                    tvOfficerDoneRecords.setText(String.valueOf(done));
                    
                    applyFilter();
                } else {
                    tvOfficerTotalRecords.setText("0");
                    tvOfficerRunningRecords.setText("0");
                    tvOfficerDoneRecords.setText("0");
                    applyFilter();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilter() {
        currentFilterList.clear();
        for (Accused accused : allActiveAccusedList) {
            if (currentMode.equals("running")) {
                if (accused.getActive() == 1) currentFilterList.add(accused);
            } else if (currentMode.equals("done")) {
                if (accused.getActive() == 0) currentFilterList.add(accused);
            } else {
                currentFilterList.add(accused);
            }
        }
        filter(searchBarMain.getText().toString());
    }

    private void filter(String text) {
        accusedList.clear();
        if (text.isEmpty()) {
            accusedList.addAll(currentFilterList);
        } else {
            String query = text.toLowerCase().trim();
            for (Accused item : currentFilterList) {
                if ((item.getName() != null && item.getName().toLowerCase().contains(query)) ||
                    (item.getGuardian() != null && item.getGuardian().toLowerCase().contains(query)) ||
                    (item.getCase_number() != null && item.getCase_number().toLowerCase().contains(query)) ||
                    (item.getAddress() != null && item.getAddress().toLowerCase().contains(query))) {
                    accusedList.add(item);
                }
            }
        }
        accusedAdapter.notifyDataSetChanged();
        searchTotalItem.setText(accusedList.size() + " টি তথ্য পাওয়া গেছে");
    }
}
