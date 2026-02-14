package com.example.cr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MoreFragment extends Fragment {

    private TextView userName, userMobile, totalRecords;

    private LinearLayout officerWiseTotalRecord;
    private MaterialButton logoutBtn;
    private DatabaseReference dataRef, officerRef;
    private SharedPreferences sharedPreferences;
    private int currentOfficerSlNo = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        userName = view.findViewById(R.id.userName);
        userMobile = view.findViewById(R.id.userMobile);
        totalRecords = view.findViewById(R.id.totalRecords);
        logoutBtn = view.findViewById(R.id.logoutBtn);
        officerWiseTotalRecord = view.findViewById(R.id.officerWiseTotalRecord);

        sharedPreferences = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        
        dataRef = FirebaseDatabase.getInstance().getReference("data");
        officerRef = FirebaseDatabase.getInstance().getReference("officer");

        fetchOfficerProfile();

        totalRecords.setOnClickListener(v -> {
            if (currentOfficerSlNo != -1) {
                Intent intent = new Intent(getActivity(), OfficerRecordsActivity.class);
                intent.putExtra("officer_sl_no", currentOfficerSlNo);
                startActivity(intent);
            }
        });

        officerWiseTotalRecord.setOnClickListener(v -> {
            if (currentOfficerSlNo != -1) {
                Intent intent = new Intent(getActivity(), OfficerRecordsActivity.class);
                intent.putExtra("officer_sl_no", currentOfficerSlNo);
                startActivity(intent);
            }
        });

        logoutBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
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
                                totalRecords.setText("0");
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
                if (snapshot.exists()) {
                    long count = snapshot.getChildrenCount();
                    totalRecords.setText(String.valueOf(count));
                } else {
                    totalRecords.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
