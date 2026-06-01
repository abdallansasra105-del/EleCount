package com.example.elecount.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elecount.R;
import com.example.elecount.models.UsageRecord;

import java.util.ArrayList;
import java.util.Locale;

public class UsageRecordAdapter extends RecyclerView.Adapter<UsageRecordAdapter.RecordViewHolder> {

    private final Context context;
    private final ArrayList<UsageRecord> records;

    public UsageRecordAdapter(Context context, ArrayList<UsageRecord> records) {
        this.context = context;
        this.records = records;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_usage_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        UsageRecord record = records.get(position);
        holder.recordDateText.setText(record.getDateTimeText());
        holder.recordDurationText.setText(context.getString(
                R.string.usage_record_duration, record.getDurationText()));
        holder.recordEnergyText.setText(context.getString(
                R.string.usage_record_energy, record.getEnergyConsumed()));
        holder.recordCostText.setText(record.getCostText());
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView recordDateText;
        TextView recordDurationText;
        TextView recordEnergyText;
        TextView recordCostText;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            recordDateText = itemView.findViewById(R.id.recordDateText);
            recordDurationText = itemView.findViewById(R.id.recordDurationText);
            recordEnergyText = itemView.findViewById(R.id.recordEnergyText);
            recordCostText = itemView.findViewById(R.id.recordCostText);
        }
    }
}
