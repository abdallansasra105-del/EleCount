package com.example.elecount.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elecount.R;
import com.example.elecount.models.Category;

import java.util.ArrayList;

/**
 * محول لعرض قائمة التصنيفات في RecyclerView
 * 
 * الشرح: هذا الكلاس يربط بيانات التصنيفات مع عناصر الواجهة
 * يوفر إمكانية تعديل وحذف التصنيفات
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    
    private Context context;
    private ArrayList<Category> categories;
    private OnCategoryActionListener listener;
    
    /**
     * واجهة لإخطار الـ Fragment عند تنفيذ إجراء على التصنيف
     */
    public interface OnCategoryActionListener {
        void onEditCategory(Category category, int position);
        void onDeleteCategory(Category category, int position);
    }
    
    /**
     * المنشئ
     * @param context سياق التطبيق
     * @param categories قائمة التصنيفات
     */
    public CategoryAdapter(Context context, ArrayList<Category> categories) {
        this.context = context;
        this.categories = categories;
    }
    
    /**
     * تعيين المستمع للأحداث
     */
    public void setOnCategoryActionListener(OnCategoryActionListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category);
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    /**
     * ViewHolder لعنصر التصنيف
     */
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        
        TextView categoryName;
        TextView categoryDeviceCount;
        ImageButton editButton;
        ImageButton deleteButton;
        
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            
            categoryName = itemView.findViewById(R.id.categoryName);
            categoryDeviceCount = itemView.findViewById(R.id.categoryDeviceCount);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
        
        /**
         * ربط بيانات التصنيف مع العناصر
         */
        public void bind(Category category) {
            // اسم التصنيف
            categoryName.setText(category.getName());
            
            // عدد الأجهزة
            int count = category.getDeviceCount();
            if (count == 0) {
                categoryDeviceCount.setText("لا يوجد أجهزة");
            } else if (count == 1) {
                categoryDeviceCount.setText("جهاز واحد");
            } else if (count == 2) {
                categoryDeviceCount.setText("جهازان");
            } else if (count >= 3 && count <= 10) {
                categoryDeviceCount.setText(count + " أجهزة");
            } else {
                categoryDeviceCount.setText(count + " جهاز");
            }
            
            // زر التعديل
            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditCategory(category, getAdapterPosition());
                }
            });
            
            // زر الحذف
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteCategory(category, getAdapterPosition());
                }
            });
        }
    }
}
