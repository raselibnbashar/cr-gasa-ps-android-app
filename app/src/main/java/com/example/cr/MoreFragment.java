package com.example.cr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoreFragment extends Fragment {

    private TextView userName, userMobile, tvTotalRecords, tvRunningRecords, tvDoneRecords, tvOfficerCount;
    private AppCompatButton btnBackup;
    private LinearLayout officerWiseTotalRecord;
    private MaterialButton logoutBtn;
    private FloatingActionButton btnAddOfficer;
    private DatabaseReference dataRef, officerRef;
    private SharedPreferences sharedPreferences;
    private int currentOfficerSlNo = -1;

    private RecyclerView rvOfficers;
    private OfficerAdapter officerAdapter;
    private List<Officer> officerList = new ArrayList<>();
    private TextInputEditText etSearchOfficer;

    private String jsonBackupData = "";
    private final ActivityResultLauncher<String> createDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null && !jsonBackupData.isEmpty()) {
                    saveJsonToUri(uri);
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        userName = view.findViewById(R.id.userName);
        userMobile = view.findViewById(R.id.userMobile);
        tvTotalRecords = view.findViewById(R.id.totalRecords);
        tvRunningRecords = view.findViewById(R.id.runningRecords);
        tvDoneRecords = view.findViewById(R.id.doneRecords);
        tvOfficerCount = view.findViewById(R.id.tvOfficerCount);
        logoutBtn = view.findViewById(R.id.logoutBtn);
        btnBackup = view.findViewById(R.id.btnBackup);
        btnAddOfficer = view.findViewById(R.id.btnAddOfficer);
        officerWiseTotalRecord = view.findViewById(R.id.officerWiseTotalRecord);
        rvOfficers = view.findViewById(R.id.rvOfficers);
        etSearchOfficer = view.findViewById(R.id.etSearchOfficer);

        sharedPreferences = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        
        dataRef = FirebaseDatabase.getInstance().getReference("data");
        officerRef = FirebaseDatabase.getInstance().getReference("officer");

        setupRecyclerView();
        fetchOfficerProfile();
        fetchAllOfficers();

        officerWiseTotalRecord.setOnClickListener(v -> {
            if (currentOfficerSlNo != -1) {
                Intent intent = new Intent(getActivity(), OfficerRecordsActivity.class);
                intent.putExtra("officer_sl_no", currentOfficerSlNo);
                startActivity(intent);
            }
        });

        btnBackup.setOnClickListener(v -> backupDatabase());

        btnAddOfficer.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddOfficerActivity.class);
            startActivity(intent);
        });

        logoutBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        etSearchOfficer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (officerAdapter != null) {
                    officerAdapter.filter(s.toString());
                    updateOfficerCountDisplay();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void setupRecyclerView() {
        officerAdapter = new OfficerAdapter(officerList, officer -> {
            Intent intent = new Intent(getActivity(), OfficerRecordsActivity.class);
            intent.putExtra("officer_sl_no", officer.getSl_no());
            startActivity(intent);
        });
        rvOfficers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOfficers.setAdapter(officerAdapter);
    }

    private void fetchAllOfficers() {
        officerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                officerList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot officerSnap : snapshot.getChildren()) {
                        Officer officer = officerSnap.getValue(Officer.class);
                        if (officer != null) {
                            officerList.add(officer);
                        }
                    }
                    fetchTotalRecordsForAll();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchTotalRecordsForAll() {
        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<Integer, Long> totalCounts = new HashMap<>();
                Map<Integer, Long> runningCounts = new HashMap<>();
                Map<Integer, Long> doneCounts = new HashMap<>();

                for (DataSnapshot dataSnap : snapshot.getChildren()) {
                    Object slNoObj = dataSnap.child("officer_sl_no").getValue();
                    if (slNoObj != null) {
                        try {
                            int slNo = Integer.parseInt(String.valueOf(slNoObj));
                            totalCounts.put(slNo, totalCounts.getOrDefault(slNo, 0L) + 1);

                            DataSnapshot statusSnap = dataSnap.child("status");
                            if (statusSnap.exists()) {
                                Object activeVal = statusSnap.child("active").getValue();
                                if (activeVal != null) {
                                    int active = Integer.parseInt(activeVal.toString());
                                    if (active == 1) {
                                        runningCounts.put(slNo, runningCounts.getOrDefault(slNo, 0L) + 1);
                                    } else if (active == 0) {
                                        doneCounts.put(slNo, doneCounts.getOrDefault(slNo, 0L) + 1);
                                    }
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
                for (Officer officer : officerList) {
                    int slNo = officer.getSl_no();
                    officer.setTotalRecords(totalCounts.getOrDefault(slNo, 0L));
                    officer.setRunningRecords(runningCounts.getOrDefault(slNo, 0L));
                    officer.setDoneRecords(doneCounts.getOrDefault(slNo, 0L));
                }
                officerAdapter.updateList(officerList);
                updateOfficerCountDisplay();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateOfficerCountDisplay() {
        if (officerAdapter != null) {
            int count = officerAdapter.getItemCount();
            tvOfficerCount.setText("মোট অফিসার: " + count);
        }
    }

    private void backupDatabase() {
        Toast.makeText(getContext(), "Fetching data for backup...", Toast.LENGTH_SHORT).show();
        FirebaseDatabase.getInstance().getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object data = snapshot.getValue();
                if (data != null) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    jsonBackupData = gson.toJson(data);
                    createDocumentLauncher.launch("cr_backup_" + System.currentTimeMillis() + ".json");
                } else {
                    Toast.makeText(getContext(), "No data found to backup", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveJsonToUri(Uri uri) {
        try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
            if (outputStream != null) {
                outputStream.write(jsonBackupData.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(getContext(), "Backup saved successfully", Toast.LENGTH_SHORT).show();
                jsonBackupData = ""; // Clear after saving
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to save backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchOfficerProfile() {
        String loggedInUserId = sharedPreferences.getString("userId", "");

        if (loggedInUserId.isEmpty()) return;

        officerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot officerSnap : snapshot.getChildren()) {
                        String dbUserId = officerSnap.child("user_id").getValue(String.class);
                        if (loggedInUserId.equals(dbUserId)) {
                            String name = officerSnap.child("name_rank").getValue(String.class);
                            String mobile = officerSnap.child("mobile").getValue(String.class);
                            
                            Object slNoObj = officerSnap.child("sl_no").getValue();
                            if (slNoObj != null) {
                                try {
                                    currentOfficerSlNo = Integer.parseInt(String.valueOf(slNoObj));
                                } catch (NumberFormatException e) {
                                    currentOfficerSlNo = -1;
                                }
                            }

                            userName.setText(name != null ? name : "Unknown");
                            userMobile.setText(mobile != null ? mobile : loggedInUserId);
                            
                            sharedPreferences.edit().putString("officerName", name).apply();
                            
                            if (currentOfficerSlNo != -1) {
                                fetchTotalRecords(currentOfficerSlNo);
                            } else {
                                tvTotalRecords.setText("0");
                                tvRunningRecords.setText("0");
                                tvDoneRecords.setText("0");
                            }
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchTotalRecords(int officerSlNo) {
        dataRef.orderByChild("officer_sl_no").equalTo(officerSlNo).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long total = 0;
                long running = 0;
                long done = 0;

                if (snapshot.exists()) {
                    total = snapshot.getChildrenCount();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        DataSnapshot statusSnap = child.child("status");
                        if (statusSnap.exists()) {
                            Object activeVal = statusSnap.child("active").getValue();
                            if (activeVal != null) {
                                int active = Integer.parseInt(activeVal.toString());
                                if (active == 1) running++;
                                else if (active == 0) done++;
                            }
                        }
                    }
                }
                
                tvTotalRecords.setText(String.valueOf(total));
                tvRunningRecords.setText(String.valueOf(running));
                tvDoneRecords.setText(String.valueOf(done));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
