package com.example.cr;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class AccusedAdapter extends RecyclerView.Adapter<AccusedAdapter.ViewHolder> {

    private List<Accused> accusedList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Accused accused);
    }

    public AccusedAdapter(List<Accused> accusedList) {
        this.accusedList = accusedList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.accused_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Accused accused = accusedList.get(position);
        holder.name.setText(accused.getName());
        holder.guardian.setText(accused.getGuardian());
        holder.address.setText(accused.getAddress());
        holder.section.setText(accused.getSection());
        holder.caseNo.setText(accused.getCase_number());
        holder.wardPs.setText(String.format("ওয়ার্ডঃ %s", accused.getWard()));

        // Bind process data from the "2026" key
        Map<String, String> processMap = accused.getProcces();
        if (processMap != null && processMap.containsKey("2026")) {
            holder.process.setText(processMap.get("2026"));
            holder.process.setVisibility(View.VISIBLE);
        } else {
            holder.process.setVisibility(View.GONE);
        }

        // Logic for step status
        int step = accused.getStep();
        switch (step) {
            case 1:
                holder.stepStatus.setVisibility(View.VISIBLE);
                holder.stepStatus.setText("চলমান");
                holder.stepStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.red_500)));
                holder.name.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red_500));
                holder.caseNo.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red_500));
                break;
            case 2:
                holder.stepStatus.setVisibility(View.VISIBLE);
                holder.stepStatus.setText("তামিল");
                holder.stepStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.emerald_400)));
                holder.name.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.emerald_400));
                holder.caseNo.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.emerald_400));
                break;
            case 3:
                holder.stepStatus.setVisibility(View.VISIBLE);
                holder.stepStatus.setText("রিকল");
                holder.stepStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.emerald_400)));
                holder.name.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.emerald_400));
                holder.caseNo.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.emerald_400));
                break;
            case 4:
                holder.stepStatus.setVisibility(View.VISIBLE);
                holder.stepStatus.setText("অন্যান্য");
                holder.stepStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.emerald_400)));
                holder.name.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.emerald_400));
                holder.caseNo.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.emerald_400));
                break;
            default:
                holder.stepStatus.setVisibility(View.GONE);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(accused);
            }
        });
    }

    @Override
    public int getItemCount() {
        return accusedList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, guardian, address, section, caseNo, process, wardPs, stepStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.accusedName);
            guardian = itemView.findViewById(R.id.accusedGuardian);
            address = itemView.findViewById(R.id.accusedAddress);
            section = itemView.findViewById(R.id.accusedSection);
            caseNo = itemView.findViewById(R.id.accusedCaseNo);
            process = itemView.findViewById(R.id.accusedProcess);
            wardPs = itemView.findViewById(R.id.accusedWardPs);
            stepStatus = itemView.findViewById(R.id.accusedStepStatus);
        }
    }
}
