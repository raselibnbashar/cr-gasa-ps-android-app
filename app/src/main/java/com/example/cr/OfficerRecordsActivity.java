package com.example.cr;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
    private TextView searchTotalItem;
    private RecyclerView accusedRecyclerView;
    private AccusedAdapter accusedAdapter;
    private List<Accused> accusedList;
    private List<Accused> allActiveAccusedList;
    private List<Accused> currentFilterList;
    private DatabaseReference databaseReference;
    
    private Button btnRunningFilter, btnDoneFilter, btnTotalFilter;
    private String currentMode = "running";
    private int officerSlNo;

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
        accusedRecyclerView = findViewById(R.id.accusedFrontList);
        btnRunningFilter = findViewById(R.id.btnRunningFilter);
        btnDoneFilter = findViewById(R.id.btnDoneFilter);
        btnTotalFilter = findViewById(R.id.btnTotalFilter);

        backBtn.setOnClickListener(v -> finish());

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
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Accused accused = child.getValue(Accused.class);
                        if (accused != null) {
                            accused.setKey(child.getKey());
                            DataSnapshot statusSnap = child.child("status");
                            if (statusSnap.exists()) {
                                Object activeVal = statusSnap.child("active").getValue();
                                if (activeVal != null) {
                                    accused.setActive(Integer.parseInt(activeVal.toString()));
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
