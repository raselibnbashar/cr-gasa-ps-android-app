package com.example.cr;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.List;
import java.util.Map;

public class ListFragment extends Fragment {

    private EditText searchBarMain;
    private TextView searchTotalItem;
    private RecyclerView accusedRecyclerView;
    private AccusedAdapter accusedAdapter;
    private List<Accused> accusedList;
    private List<Accused> allActiveAccusedList;
    private List<Accused> currentFilterList;
    private DatabaseReference databaseReference;
    
    private Button btnRunningFilter, btnDoneFilter, btnTotalFilter;
    private String currentMode = "running"; // Default mode

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
        fetchData();

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
