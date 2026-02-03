package com.example.Abdallansasra.adapter;

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
import com.example.Abdallansasra.Hellper.DALAppWriteConnection;
import com.example.Abdallansasra.R;
import com.example.Abdallansasra.model.Stud;

import java.util.ArrayList;

public class StdAdapter extends RecyclerView.Adapter<StdAdapter.ViewHolder> {


    ArrayList<Stud> stdList;
    DALAppWriteConnection dal;
    
    public StdAdapter(ArrayList<Stud> stdList, DALAppWriteConnection dal) {
        this.stdList = stdList;
        this.dal = dal;
    }

    @NonNull
    @Override
    public StdAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_std, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull StdAdapter.ViewHolder holder, int position) {
            Stud student = stdList.get(position);
            
            holder.tvsname.setText(student.getName());
            holder.tvage.setText(student.getAge() != null ? student.getAge().toString() : "N/A"); // Display date
            
            // تحميل الصورة مع إضافة headers للـ Appwrite
            Glide.with(holder.itemView.getContext())
                .load(new com.bumptech.glide.load.model.GlideUrl(student.getImageUrl(), 
                    new com.bumptech.glide.load.model.LazyHeaders.Builder()
                        .addHeader("X-Appwrite-Project", "69033828003328299847")
                        .addHeader("X-Appwrite-Key", "standard_2b5b7365808986dc2e7724df693d7e68b81f3ec6511ae1c7980a4be803a7b7d1a4de9e89805f53bbf1eceee468d61fc760d2eb3dcfe50647375d8b05ed16d7c911cf7f11a0ea48dfe678291aa169a29116e5adc85ff3dc7ebb9bb33c87ac975368c36a79dbd2ebe045811f459c851b59025a22c136a513c012bd3fff339386dd")
                        .build()))
                .into(holder.ivstd);
            
            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // التحقق من وجود ID
                    if (student.getId() == null || student.getId().isEmpty()) {
                        Toast.makeText(v.getContext(), "❌ لا يمكن الحذف: معرف غير صالح", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // عرض رسالة تأكيد
                    Toast.makeText(v.getContext(), "🗑️ جاري الحذف...", Toast.LENGTH_SHORT).show();
                    
                    // حذف من قاعدة البيانات في خيط منفصل
                    new Thread(() -> {
                        DALAppWriteConnection.OperationResult<Void> result = 
                            dal.deleteData("std", student.getId(), null);
                        
                        // الرجوع إلى الخيط الرئيسي لتحديث الواجهة
                        holder.itemView.post(() -> {
                            if (result.success) {
                                // حذف من القائمة وتحديث RecyclerView
                                int adapterPosition = holder.getAdapterPosition();
                                if (adapterPosition != RecyclerView.NO_POSITION) {
                                    stdList.remove(adapterPosition);
                                    notifyItemRemoved(adapterPosition);
                                    notifyItemRangeChanged(adapterPosition, stdList.size());
                                }
                                
                                Toast.makeText(v.getContext(), "✅ تم الحذف بنجاح", Toast.LENGTH_SHORT).show();
                                Log.d("StdAdapter", "تم حذف الطالب: " + student.getName());
                            } else {
                                Toast.makeText(v.getContext(), "❌ فشل الحذف: " + result.message, Toast.LENGTH_LONG).show();
                                Log.e("StdAdapter", "فشل حذف الطالب: " + result.message);
                            }
                        });
                    }).start();
                }});

    }

    @Override
    public int getItemCount() {
        return stdList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvsname;
        TextView tvage;
        ImageView ivstd;
        TextView btnDelete;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvsname= itemView.findViewById(R.id.tvsname);
            tvage= itemView.findViewById(R.id.tvage);
            ivstd= itemView.findViewById(R.id.ivstd);
            btnDelete= itemView.findViewById(R.id.btnDelete);

        }
    }
}
