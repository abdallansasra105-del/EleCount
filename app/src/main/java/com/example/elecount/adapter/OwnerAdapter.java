package com.example.elecount.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.elecount.Hellper.DALAppWriteConnection;
import com.example.elecount.R;
import com.example.elecount.model.Owner;

import java.util.ArrayList;

public class OwnerAdapter extends RecyclerView.Adapter<OwnerAdapter.ViewHolder> {

    ArrayList<Owner> ownerList;
    DALAppWriteConnection dal;
    
    public OwnerAdapter(ArrayList<Owner> ownerList, DALAppWriteConnection dal) {
        this.ownerList = ownerList;
        this.dal = dal;
    }

    @NonNull
    @Override
    public OwnerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_owner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OwnerAdapter.ViewHolder holder, int position) {
            Owner owner = ownerList.get(position);
            
            holder.tvsname.setText(owner.getName());
            holder.tvage.setText(owner.getAge() != null ? owner.getAge().toString() : "N/A");
            
            Glide.with(holder.itemView.getContext())
                .load(new com.bumptech.glide.load.model.GlideUrl(owner.getImageUrl(), 
                    new com.bumptech.glide.load.model.LazyHeaders.Builder()
                        .addHeader("X-Appwrite-Project", "69033828003328299847")
                        .addHeader("X-Appwrite-Key", "standard_2b5b7365808986dc2e7724df693d7e68b81f3ec6511ae1c7980a4be803a7b7d1a4de9e89805f53bbf1eceee468d61fc760d2eb3dcfe50647375d8b05ed16d7c911cf7f11a0ea48dfe678291aa169a29116e5adc85ff3dc7ebb9bb33c87ac975368c36a79dbd2ebe045811f459c851b59025a22c136a513c012bd3fff339386dd")
                        .build()))
                .into(holder.ivowner);
            
            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (owner.getId() == null || owner.getId().isEmpty()) {
                        Toast.makeText(v.getContext(), "❌ لا يمكن الحذف: معرف غير صالح", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    Toast.makeText(v.getContext(), "🗑️ جاري الحذف...", Toast.LENGTH_SHORT).show();
                    
                    new Thread(() -> {
                        DALAppWriteConnection.OperationResult<Void> result = 
                            dal.deleteData("owner", owner.getId(), null);
                        
                        holder.itemView.post(() -> {
                            if (result.success) {
                                int adapterPosition = holder.getAdapterPosition();
                                if (adapterPosition != RecyclerView.NO_POSITION) {
                                    ownerList.remove(adapterPosition);
                                    notifyItemRemoved(adapterPosition);
                                    notifyItemRangeChanged(adapterPosition, ownerList.size());
                                }
                                
                                Toast.makeText(v.getContext(), "✅ تم الحذف بنجاح", Toast.LENGTH_SHORT).show();
                                Log.d("OwnerAdapter", "تم حذف المالك: " + owner.getName());
                            } else {
                                Toast.makeText(v.getContext(), "❌ فشل الحذف: " + result.message, Toast.LENGTH_LONG).show();
                                Log.e("OwnerAdapter", "فشل حذف المالك: " + result.message);
                            }
                        });
                    }).start();
                }});
    }

    @Override
    public int getItemCount() {
        return ownerList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvsname;
        TextView tvage;
        ImageView ivowner;
        TextView btnDelete;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvsname= itemView.findViewById(R.id.tvsname);
            tvage= itemView.findViewById(R.id.tvage);
            ivowner= itemView.findViewById(R.id.ivowner);
            btnDelete= itemView.findViewById(R.id.btnDelete);
        }
    }
}
