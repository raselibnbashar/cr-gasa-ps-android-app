package com.example.cr;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserPresenceActivity extends AppCompatActivity {

    private RecyclerView rvUserPresence;
    private ProgressBar progressBar;
    private TextView tvTotalDevices;
    private DatabaseReference presenceRef, officerRef;
    private PresenceAdapter adapter;
    private List<UserPresence> presenceList = new ArrayList<>();
    private Map<String, String> userIdToName = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_presence);

        ImageView backBtn = findViewById(R.id.backBtn);
        rvUserPresence = findViewById(R.id.rvUserPresence);
        progressBar = findViewById(R.id.progressBar);
        tvTotalDevices = findViewById(R.id.tvTotalDevices);

        backBtn.setOnClickListener(v -> finish());

        rvUserPresence.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PresenceAdapter();
        rvUserPresence.setAdapter(adapter);

        presenceRef = FirebaseDatabase.getInstance().getReference("presence");
        officerRef = FirebaseDatabase.getInstance().getReference("officer");

        fetchOfficerNames();
    }

    private void fetchOfficerNames() {
        progressBar.setVisibility(View.VISIBLE);
        officerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userIdToName.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String userId = ds.child("user_id").getValue(String.class);
                        String name = ds.child("name_rank").getValue(String.class);
                        if (userId != null && name != null) {
                            userIdToName.put(userId.replace(".", "_"), name);
                        }
                    }
                }
                listenToPresence();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void listenToPresence() {
        presenceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                presenceList.clear();
                long totalDevices = 0;

                if (snapshot.exists()) {
                    totalDevices = snapshot.getChildrenCount();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String userId = ds.child("user_id").getValue(String.class);
                        if (userId != null) {
                            UserPresence up = new UserPresence(userId, userIdToName.getOrDefault(userId, "Unknown Officer"));
                            up.model = ds.child("model").getValue(String.class);
                            up.deviceId = ds.child("device_id").getValue(String.class);
                            up.imei = ds.child("imei").getValue(String.class);
                            up.mobile = ds.child("mobile_number").getValue(String.class);
                            
                            Object latObj = ds.child("lat").getValue();
                            Object lngObj = ds.child("lng").getValue();
                            if (latObj != null && lngObj != null) {
                                up.lat = Double.parseDouble(latObj.toString());
                                up.lng = Double.parseDouble(lngObj.toString());
                            }
                            
                            presenceList.add(up);
                        }
                    }
                }

                tvTotalDevices.setText(String.valueOf(totalDevices));
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private static class UserPresence {
        String userId;
        String userName;
        String model;
        String deviceId;
        String imei;
        String mobile;
        double lat = 0, lng = 0;

        UserPresence(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }
    }

    private class PresenceAdapter extends RecyclerView.Adapter<PresenceAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_presence, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserPresence up = presenceList.get(position);
            holder.tvUserName.setText(up.userName);
            holder.tvUserId.setText(up.userId.replace("_", "."));
            holder.tvDeviceModel.setText(up.model != null ? up.model : "Unknown Device");
            
            String deviceIdInfo = "ID: " + (up.deviceId != null ? up.deviceId : "N/A");
            if (up.imei != null) deviceIdInfo += " (IMEI: " + up.imei + ")";
            holder.tvDeviceId.setText(deviceIdInfo);
            
            holder.tvMobile.setText("Mobile: " + (up.mobile != null ? up.mobile : "Unknown"));
            
            if (up.lat != 0 && up.lng != 0) {
                holder.tvLocation.setText(String.format("Location: %.5f, %.5f", up.lat, up.lng));
                holder.tvLocation.setOnClickListener(v -> {
                    String geoUri = "http://maps.google.com/maps?q=loc:" + up.lat + "," + up.lng;
                    Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(geoUri));
                    v.getContext().startActivity(intent);
                });
            } else {
                holder.tvLocation.setText("Location: Not Available");
            }
        }

        @Override
        public int getItemCount() {
            return presenceList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvUserId, tvDeviceModel, tvLocation, tvDeviceId, tvMobile;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvUserId = itemView.findViewById(R.id.tvUserId);
                tvDeviceModel = itemView.findViewById(R.id.tvDeviceModel);
                tvLocation = itemView.findViewById(R.id.tvLocation);
                tvDeviceId = itemView.findViewById(R.id.tvDeviceId);
                tvMobile = itemView.findViewById(R.id.tvMobile);
            }
        }
    }
}
