package com.example.cr;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class OfficerAdapter extends RecyclerView.Adapter<OfficerAdapter.OfficerViewHolder> {

    private List<Officer> officerList;
    private List<Officer> filteredList;
    private OnOfficerClickListener listener;

    public interface OnOfficerClickListener {
        void onOfficerClick(Officer officer);
    }

    public OfficerAdapter(List<Officer> officerList, OnOfficerClickListener listener) {
        this.officerList = officerList;
        this.filteredList = new ArrayList<>(officerList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public OfficerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_officer, parent, false);
        return new OfficerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfficerViewHolder holder, int position) {
        Officer officer = filteredList.get(position);
        holder.name.setText(officer.getName_rank() != null ? officer.getName_rank() : "Unknown");
        holder.mobile.setText(officer.getMobile() != null ? officer.getMobile() : "");
        holder.totalRecords.setText(String.valueOf(officer.getTotalRecords()));
        holder.runningRecords.setText(String.valueOf(officer.getRunningRecords()));
        holder.doneRecords.setText(String.valueOf(officer.getDoneRecords()));
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOfficerClick(officer);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(officerList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Officer officer : officerList) {
                if (officer.getName_rank() != null && officer.getName_rank().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(officer);
                } else if (officer.getMobile() != null && officer.getMobile().contains(lowerCaseQuery)) {
                    filteredList.add(officer);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateList(List<Officer> newList) {
        this.officerList = new ArrayList<>(newList);
        this.filteredList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    static class OfficerViewHolder extends RecyclerView.ViewHolder {
        TextView name, mobile, totalRecords, runningRecords, doneRecords;

        public OfficerViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.officerName);
            mobile = itemView.findViewById(R.id.officerMobile);
            totalRecords = itemView.findViewById(R.id.officerTotalRecords);
            runningRecords = itemView.findViewById(R.id.tvRunningCount);
            doneRecords = itemView.findViewById(R.id.tvDoneCount);
        }
    }
}
