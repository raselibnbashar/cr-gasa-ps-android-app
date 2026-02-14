package com.example.cr;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddAccusedActivity extends AppCompatActivity {

    private EditText inputName, inputGuardian, inputAddress, inputCaseNo, inputSection, inputCourt, inputCp, inputProcess;
    private AutoCompleteTextView inputWard, inputVillage, inputCourtDist, inputOfficerSl;
    private Button saveBtn;
    private ImageView backBtn;
    private DatabaseReference databaseReference, wardReference, districtsReference, officerReference;
    
    private List<String> wardList = new ArrayList<>();
    private List<String> districtList = new ArrayList<>();
    private List<String> officerNames = new ArrayList<>();
    private Map<String, Integer> officerNameToSl = new HashMap<>();
    private Map<String, List<String>> wardToVillages = new HashMap<>();
    private Map<String, List<OfficerInfo>> wardToOfficers = new HashMap<>();
    private Map<Integer, Integer> officerSlToCount = new HashMap<>();

    private static class OfficerInfo {
        String nameRank;
        int slNo;

        OfficerInfo(String nameRank, int slNo) {
            this.nameRank = nameRank;
            this.slNo = slNo;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_accused);

        databaseReference = FirebaseDatabase.getInstance().getReference("data");
        wardReference = FirebaseDatabase.getInstance().getReference("ward");
        districtsReference = FirebaseDatabase.getInstance().getReference("districts");
        officerReference = FirebaseDatabase.getInstance().getReference("officer");

        inputName = findViewById(R.id.inputName);
        inputGuardian = findViewById(R.id.inputGuardian);
        inputAddress = findViewById(R.id.inputAddress);
        inputWard = findViewById(R.id.inputWard);
        inputVillage = findViewById(R.id.inputVillage);
        inputCaseNo = findViewById(R.id.inputCaseNo);
        inputSection = findViewById(R.id.inputSection);
        inputCourt = findViewById(R.id.inputCourt);
        inputCourtDist = findViewById(R.id.inputCourtDist);
        inputOfficerSl = findViewById(R.id.inputOfficerSl);
        inputCp = findViewById(R.id.inputCp);
        inputProcess = findViewById(R.id.inputProcess);
        saveBtn = findViewById(R.id.saveBtn);
        backBtn = findViewById(R.id.backBtn);

        // Allow showing all items when clicking the field
        inputWard.setThreshold(0);
        inputVillage.setThreshold(0);
        inputCourtDist.setThreshold(0);
        inputOfficerSl.setThreshold(0);

        fetchWardData();
        fetchDistrictData();
        fetchOfficerAndAccusedCount();

        saveBtn.setOnClickListener(v -> saveAccused());
        backBtn.setOnClickListener(v -> finish());
        
        // Show dropdown on click or focus
        inputWard.setOnClickListener(v -> inputWard.showDropDown());
        inputWard.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputWard.showDropDown();
        });

        inputVillage.setOnClickListener(v -> inputVillage.showDropDown());
        inputVillage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputVillage.showDropDown();
        });

        inputCourtDist.setOnClickListener(v -> inputCourtDist.showDropDown());
        inputCourtDist.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputCourtDist.showDropDown();
        });

        inputOfficerSl.setOnClickListener(v -> inputOfficerSl.showDropDown());
        inputOfficerSl.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputOfficerSl.showDropDown();
        });
        
        inputWard.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedWard = (String) parent.getItemAtPosition(position);
                updateVillageDropdown(selectedWard);
                updateOfficerDropdown(selectedWard);
            }
        });
    }

    private void fetchWardData() {
        wardReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                wardList.clear();
                wardToVillages.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot wardSnapshot : snapshot.getChildren()) {
                        Object wardNoObj = wardSnapshot.child("ward_no").getValue();
                        if (wardNoObj != null) {
                            String wardNo = String.valueOf(wardNoObj);
                            wardList.add(wardNo);
                            
                            List<String> villages = new ArrayList<>();
                            DataSnapshot villageSnapshot = wardSnapshot.child("village");
                            if (villageSnapshot.exists()) {
                                for (DataSnapshot vSnap : villageSnapshot.getChildren()) {
                                    Object vObj = vSnap.getValue();
                                    if (vObj != null) {
                                        villages.add(String.valueOf(vObj));
                                    }
                                }
                            }
                            wardToVillages.put(wardNo, villages);
                        }
                    }
                    
                    ArrayAdapter<String> wardAdapter = new ArrayAdapter<>(AddAccusedActivity.this,
                            android.R.layout.simple_list_item_1, wardList);
                    inputWard.setAdapter(wardAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddAccusedActivity.this, "Failed to load wards", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchDistrictData() {
        districtsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                districtList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot districtSnapshot : snapshot.getChildren()) {
                        String bnName = districtSnapshot.child("bn_name").getValue(String.class);
                        if (bnName != null) {
                            districtList.add(bnName);
                        }
                    }
                    ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(AddAccusedActivity.this,
                            android.R.layout.simple_list_item_1, districtList);
                    inputCourtDist.setAdapter(districtAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddAccusedActivity.this, "Failed to load districts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchOfficerAndAccusedCount() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                officerSlToCount.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Object slNoObj = snapshot.child("officer_sl_no").getValue();
                    if (slNoObj != null) {
                        int slNo = Integer.parseInt(String.valueOf(slNoObj));
                        officerSlToCount.put(slNo, officerSlToCount.getOrDefault(slNo, 0) + 1);
                    }
                }
                fetchOfficerData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchOfficerData() {
        officerReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                officerNames.clear();
                officerNameToSl.clear();
                wardToOfficers.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot officerSnapshot : snapshot.getChildren()) {
                        String nameRank = officerSnapshot.child("name_rank").getValue(String.class);
                        Object slNoObj = officerSnapshot.child("sl_no").getValue();
                        Object wardNoObj = officerSnapshot.child("ward_no").getValue();
                        
                        if (nameRank != null && slNoObj != null) {
                            int slNo = Integer.parseInt(String.valueOf(slNoObj));
                            int count = officerSlToCount.getOrDefault(slNo, 0);
                            String displayName = nameRank + " (" + count + ")";
                            
                            officerNames.add(displayName);
                            officerNameToSl.put(displayName, slNo);
                            
                            if (wardNoObj != null) {
                                String wardNo = String.valueOf(wardNoObj);
                                if (!wardToOfficers.containsKey(wardNo)) {
                                    wardToOfficers.put(wardNo, new ArrayList<>());
                                }
                                wardToOfficers.get(wardNo).add(new OfficerInfo(displayName, slNo));
                            }
                        }
                    }
                    updateOfficerDropdown(inputWard.getText().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddAccusedActivity.this, "Failed to load officers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateVillageDropdown(String wardNo) {
        List<String> villages = wardToVillages.get(wardNo);
        if (villages != null) {
            ArrayAdapter<String> villageAdapter = new ArrayAdapter<>(AddAccusedActivity.this,
                    android.R.layout.simple_list_item_1, villages);
            inputVillage.setAdapter(villageAdapter);
            inputVillage.setText(""); 
            inputVillage.showDropDown();
        }
    }

    private void updateOfficerDropdown(String wardNo) {
        List<OfficerInfo> filteredOfficersInfo = wardToOfficers.get(wardNo);
        if (filteredOfficersInfo != null) {
            List<String> displayNames = new ArrayList<>();
            for (OfficerInfo info : filteredOfficersInfo) {
                displayNames.add(info.nameRank);
            }
            ArrayAdapter<String> officerAdapter = new ArrayAdapter<>(AddAccusedActivity.this,
                    android.R.layout.simple_list_item_1, displayNames);
            inputOfficerSl.setAdapter(officerAdapter);
            inputOfficerSl.setText(""); 
        } else {
            inputOfficerSl.setAdapter(null);
            inputOfficerSl.setText("");
        }
    }

    private void saveAccused() {
        String name = inputName.getText().toString().trim();
        String guardian = inputGuardian.getText().toString().trim();
        String address = inputAddress.getText().toString().trim();
        String ward = inputWard.getText().toString().trim();
        String village = inputVillage.getText().toString().trim();
        String ps = "গাছা";
        String dist = "গাজীপুর মহানগর, গাজীপুর";
        String caseNo = inputCaseNo.getText().toString().trim();
        String section = inputSection.getText().toString().trim();
        String court = inputCourt.getText().toString().trim();
        String courtDist = inputCourtDist.getText().toString().trim();
        String officerDisplayName = inputOfficerSl.getText().toString().trim();
        String cp = inputCp.getText().toString().trim();
        String process = inputProcess.getText().toString().trim();

        // Validation for required fields
        if (TextUtils.isEmpty(name)) {
            inputName.setError("নাম প্রয়োজন");
            inputName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(guardian)) {
            inputGuardian.setError("অভিবাবকের নাম প্রয়োজন");
            inputGuardian.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(ward)) {
            inputWard.setError("ওয়ার্ড প্রয়োজন");
            inputWard.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(village)) {
            inputVillage.setError("গ্রাম প্রয়োজন");
            inputVillage.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(address)) {
            inputAddress.setError("ঠিকানা প্রয়োজন");
            inputAddress.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(caseNo)) {
            inputCaseNo.setError("মামলা নম্বর প্রয়োজন");
            inputCaseNo.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(section)) {
            inputSection.setError("ধারা প্রয়োজন");
            inputSection.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(court)) {
            inputCourt.setError("আদালতের নাম প্রয়োজন");
            inputCourt.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(courtDist)) {
            inputCourtDist.setError("আদালতের জেলা প্রয়োজন");
            inputCourtDist.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(cp)) {
            inputCp.setError("সিপি নম্বর প্রয়োজন");
            inputCp.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(process)) {
            inputProcess.setError("প্রসেস নম্বর প্রয়োজন");
            inputProcess.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(officerDisplayName)) {
            inputOfficerSl.setError("অফিসার প্রয়োজন");
            inputOfficerSl.requestFocus();
            return;
        }
        

        int officerSl = 0;
        if (!TextUtils.isEmpty(officerDisplayName) && officerNameToSl.containsKey(officerDisplayName)) {
            officerSl = officerNameToSl.get(officerDisplayName);
        }

        // Prepare data map
        Map<String, Object> accused = new HashMap<>();
        accused.put("name", name);
        accused.put("guardian", guardian);
        
        // Construct full address including village
        String fullAddress = address;
        if (!TextUtils.isEmpty(village)) {
            fullAddress = address + ", " + village;
        }
        accused.put("address", fullAddress);
        
        accused.put("ward", ward);
        accused.put("ps", ps);
        accused.put("dist", dist);
        accused.put("case_number", caseNo);
        accused.put("section", section);
        accused.put("court_name", court);
        accused.put("court_district", courtDist);
        accused.put("officer_sl_no", officerSl);

        Map<String, Object> cpMap = new HashMap<>();
        if (!TextUtils.isEmpty(cp)) {
            cpMap.put("2026", cp);
        }
        accused.put("cp", cpMap);

        Map<String, Object> processMap = new HashMap<>();
        if (!TextUtils.isEmpty(process)) {
            processMap.put("2026", process);
        }
        accused.put("procces", processMap);

        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("active", 1);
        statusMap.put("step", 1);
        accused.put("status", statusMap);

        databaseReference.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long lastSerial = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            lastSerial = Long.parseLong(child.getKey());
                        } catch (NumberFormatException e) {}
                    }
                }
                
                String newKey = String.valueOf(lastSerial + 1);

                databaseReference.child(newKey).setValue(accused).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(AddAccusedActivity.this, "Record saved with serial: " + newKey, Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddAccusedActivity.this, "Failed to save record", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddAccusedActivity.this, "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
