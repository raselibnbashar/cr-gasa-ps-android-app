package com.example.cr;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ListFragment extends Fragment {

    private EditText searchBarMain;
    private TextView searchTotalItem;
    private RecyclerView accusedRecyclerView;
    private AccusedAdapter accusedAdapter;
    private List<Accused> accusedList;
    private List<Accused> allActiveAccusedList;
    private List<Accused> currentFilterList;
    private DatabaseReference databaseReference, wardReference;
    
    private Button btnRunningFilter, btnDoneFilter, btnTotalFilter;
    private String currentMode = "running"; // Default mode
    
    private Spinner wardSpinner, villageSpinner;
    private List<String> wardList = new ArrayList<>();
    private List<String> villageList = new ArrayList<>();
    private ArrayAdapter<String> wardAdapter, villageAdapter;
    private String selectedWard = "সকল ওয়ার্ড";
    private String selectedVillage = "সকল গ্রাম/মহল্লা";
    private Map<String, List<String>> wardToVillages = new HashMap<>();

    public static ListFragment newInstance(String filterMode) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putString("filter_mode", filterMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        searchBarMain = view.findViewById(R.id.searchBarMain);
        searchTotalItem = view.findViewById(R.id.searchTotalItem);
        accusedRecyclerView = view.findViewById(R.id.accusedFrontList);
        
        btnRunningFilter = view.findViewById(R.id.btnRunningFilter);
        btnDoneFilter = view.findViewById(R.id.btnDoneFilter);
        btnTotalFilter = view.findViewById(R.id.btnTotalFilter);
        
        wardSpinner = view.findViewById(R.id.wardSpinner);
        villageSpinner = view.findViewById(R.id.villageSpinner);

        setupSpinners();

        accusedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        accusedList = new ArrayList<>();
        allActiveAccusedList = new ArrayList<>();
        currentFilterList = new ArrayList<>();
        accusedAdapter = new AccusedAdapter(accusedList);
        accusedRecyclerView.setAdapter(accusedAdapter);

        accusedAdapter.setOnItemClickListener(accused -> {
            Intent intent = new Intent(getActivity(), AccusedDetailActivity.class);
            intent.putExtra("accused", accused);
            startActivity(intent);
        });

        databaseReference = FirebaseDatabase.getInstance().getReference("data");
        wardReference = FirebaseDatabase.getInstance().getReference("ward");
        fetchData();
        fetchWardData();

        btnRunningFilter.setOnClickListener(v -> updateFilterMode("running"));
        btnDoneFilter.setOnClickListener(v -> updateFilterMode("done"));
        btnTotalFilter.setOnClickListener(v -> updateFilterMode("total"));

        // Check for arguments to set the initial filter mode
        if (getArguments() != null && getArguments().containsKey("filter_mode")) {
            currentMode = getArguments().getString("filter_mode");
        }
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

        return view;
    }

    private void setupSpinners() {
        wardList.add("সকল ওয়ার্ড");
        villageList.add("সকল গ্রাম/মহল্লা");

        wardAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, wardList);
        wardAdapter.setDropDownViewResource(R.layout.spinner_item);
        wardSpinner.setAdapter(wardAdapter);

        villageAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, villageList);
        villageAdapter.setDropDownViewResource(R.layout.spinner_item);
        villageSpinner.setAdapter(villageAdapter);

        wardSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedWard = wardList.get(position);
                updateVillageSpinner(selectedWard);
                applyFilter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        villageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedVillage = villageList.get(position);
                applyFilter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchWardData() {
        wardReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                wardList.clear();
                wardToVillages.clear();
                wardList.add("সকল ওয়ার্ড");
                
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
                                        String village = String.valueOf(vObj);
                                        villages.add(village);
                                    }
                                }
                            }
                            wardToVillages.put(wardNo, villages);
                        }
                    }
                }
                wardAdapter.notifyDataSetChanged();
                updateVillageSpinner(selectedWard);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateVillageSpinner(String wardNo) {
        villageList.clear();
        villageList.add("সকল গ্রাম/মহল্লা");
        
        if (wardNo.equals("সকল ওয়ার্ড")) {
            TreeSet<String> allVillages = new TreeSet<>();
            for (List<String> villages : wardToVillages.values()) {
                allVillages.addAll(villages);
            }
            villageList.addAll(allVillages);
        } else {
            List<String> villages = wardToVillages.get(wardNo);
            if (villages != null) {
                Collections.sort(villages);
                villageList.addAll(villages);
            }
        }
        villageAdapter.notifyDataSetChanged();
        
        // Reset village selection if it's no longer in the list
        if (!villageList.contains(selectedVillage)) {
            villageSpinner.setSelection(0);
            selectedVillage = "সকল গ্রাম/মহল্লা";
        } else {
            villageSpinner.setSelection(villageList.indexOf(selectedVillage));
        }
    }

    private void updateFilterMode(String mode) {
        currentMode = mode;
        
        if (getContext() == null) return;

        // Update Button UI
        int dark = ContextCompat.getColor(getContext(), R.color.slate_800);
        int emerald = ContextCompat.getColor(getContext(), R.color.emerald_400);

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
        databaseReference.addValueEventListener(new ValueEventListener() {
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
            // Status Filter
            boolean statusMatch = false;
            if (currentMode.equals("running")) {
                statusMatch = (accused.getActive() == 1);
            } else if (currentMode.equals("done")) {
                statusMatch = (accused.getActive() == 0);
            } else {
                statusMatch = true;
            }

            if (!statusMatch) continue;

            // Ward Filter
            if (!selectedWard.equals("সকল ওয়ার্ড")) {
                if (accused.getWard() == null || !accused.getWard().equals(selectedWard)) {
                    continue;
                }
            }

            // Village Filter
            if (!selectedVillage.equals("সকল গ্রাম/মহল্লা")) {
                // Check if address contains village name or is equal
                String address = accused.getAddress();
                if (address == null || !address.contains(selectedVillage)) {
                    continue;
                }
            }

            currentFilterList.add(accused);
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
                boolean matchesProcess = false;
                Map<String, String> processMap = item.getProcces();
                if (processMap != null) {
                    for (String value : processMap.values()) {
                        if (value != null && value.toLowerCase().contains(query)) {
                            matchesProcess = true;
                            break;
                        }
                    }
                }

                if ((item.getName() != null && item.getName().toLowerCase().contains(query)) ||
                    (item.getGuardian() != null && item.getGuardian().toLowerCase().contains(query)) ||
                    (item.getCase_number() != null && item.getCase_number().toLowerCase().contains(query)) ||
                    matchesProcess) {
                    accusedList.add(item);
                }
            }
        }
        accusedAdapter.notifyDataSetChanged();
        searchTotalItem.setText(accusedList.size() + " টি তথ্য পাওয়া গেছে");
    }
}
