package com.example.elecount.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elecount.R;
import com.example.elecount.Hellper.DataManager;
import com.example.elecount.adapters.CategoryAdapter;
import com.example.elecount.models.Category;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

/**
 * Fragment إدارة التصنيفات
 * يعرض قائمة التصنيفات مع إمكانية الإضافة والتعديل والحذف
 * 
 * الشرح: هذا الفراجمنت يدير جميع التصنيفات
 * يمكن للمستخدم إضافة تصنيفات جديدة، تعديلها، أو حذفها
 */
public class CategoriesFragment extends Fragment implements CategoryAdapter.OnCategoryActionListener {
    
    // مراجع للعناصر في الواجهة
    private RecyclerView categoriesRecycler;
    private LinearLayout emptyLayout;
    private FloatingActionButton addCategoryFab;
    
    // مدير البيانات
    private DataManager dataManager;
    
    // محول لعرض قائمة التصنيفات
    private CategoryAdapter categoryAdapter;
    
    // قائمة التصنيفات
    private ArrayList<Category> categories = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);
        
        // تهيئة DataManager
        dataManager = new DataManager(requireContext());
        
        // ربط العناصر
        initViews(view);
        
        // إعداد RecyclerView
        setupRecyclerView();
        
        // إعداد زر الإضافة
        setupAddButton();
        
        // تحميل البيانات
        loadCategories();
        
        return view;
    }
    
    /**
     * ربط العناصر من الـ layout
     */
    private void initViews(View view) {
        categoriesRecycler = view.findViewById(R.id.categoriesRecycler);
        emptyLayout = view.findViewById(R.id.emptyLayout);
        addCategoryFab = view.findViewById(R.id.addCategoryFab);
    }
    
    /**
     * إعداد RecyclerView
     */
    private void setupRecyclerView() {
        categoriesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoryAdapter = new CategoryAdapter(requireContext(), categories);
        categoryAdapter.setOnCategoryActionListener(this);
        categoriesRecycler.setAdapter(categoryAdapter);
    }
    
    /**
     * إعداد زر إضافة تصنيف
     */
    private void setupAddButton() {
        addCategoryFab.setOnClickListener(v -> showAddEditDialog(null, -1));
    }
    
    /**
     * تحميل التصنيفات من قاعدة البيانات
     */
    private void loadCategories() {
        dataManager.getAllCategories(new DataManager.DataCallback<ArrayList<Category>>() {
            @Override
            public void onSuccess(ArrayList<Category> result) {
                categories.clear();
                categories.addAll(result);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        categoryAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateEmptyState());
                }
            }
        });
    }
    
    /**
     * تحديث حالة العرض الفارغ
     */
    private void updateEmptyState() {
        if (categories.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            categoriesRecycler.setVisibility(View.GONE);
        } else {
            emptyLayout.setVisibility(View.GONE);
            categoriesRecycler.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * عرض Dialog لإضافة/تعديل تصنيف
     * @param category التصنيف للتعديل (null للإضافة)
     * @param position موقع التصنيف في القائمة
     */
    private void showAddEditDialog(@Nullable Category category, int position) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_category);
        
        // العناصر
        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        EditText categoryNameInput = dialog.findViewById(R.id.categoryNameInput);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button saveButton = dialog.findViewById(R.id.saveButton);
        
        // إذا كان تعديل
        boolean isEdit = category != null;
        if (isEdit) {
            dialogTitle.setText("تعديل التصنيف");
            categoryNameInput.setText(category.getName());
        } else {
            dialogTitle.setText("إضافة تصنيف جديد");
        }
        
        // زر الإلغاء
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        // زر الحفظ
        saveButton.setOnClickListener(v -> {
            String name = categoryNameInput.getText().toString().trim();
            
            if (name.isEmpty()) {
                categoryNameInput.setError("أدخل اسم التصنيف");
                return;
            }
            
            if (isEdit) {
                // تعديل التصنيف
                category.setName(name);
                updateCategory(category, position);
            } else {
                // إضافة تصنيف جديد
                Category newCategory = new Category(name);
                addCategory(newCategory);
            }
            
            dialog.dismiss();
        });
        
        dialog.show();
        
        // ضبط حجم الـ dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
    
    /**
     * إضافة تصنيف جديد
     */
    private void addCategory(Category category) {
        dataManager.addCategory(category, new DataManager.DataCallback<Category>() {
            @Override
            public void onSuccess(Category result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        categories.add(result);
                        categoryAdapter.notifyItemInserted(categories.size() - 1);
                        updateEmptyState();
                        Toast.makeText(requireContext(), "تم إضافة التصنيف", Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "خطأ: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    /**
     * تحديث تصنيف موجود
     */
    private void updateCategory(Category category, int position) {
        dataManager.updateCategory(category, new DataManager.DataCallback<Category>() {
            @Override
            public void onSuccess(Category result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        categories.set(position, result);
                        categoryAdapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "تم تحديث التصنيف", Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "خطأ: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    // === تنفيذ OnCategoryActionListener ===
    
    @Override
    public void onEditCategory(Category category, int position) {
        showAddEditDialog(category, position);
    }
    
    @Override
    public void onDeleteCategory(Category category, int position) {
        // عرض تأكيد الحذف
        new AlertDialog.Builder(requireContext())
            .setTitle("حذف التصنيف")
            .setMessage("هل تريد حذف التصنيف \"" + category.getName() + "\"؟\n\nملاحظة: لن يتم حذف الأجهزة في هذا التصنيف.")
            .setPositiveButton("حذف", (dialog, which) -> deleteCategory(category, position))
            .setNegativeButton("إلغاء", null)
            .show();
    }
    
    /**
     * حذف تصنيف
     */
    private void deleteCategory(Category category, int position) {
        dataManager.deleteCategory(category.getId(), new DataManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (position >= 0 && position < categories.size()) {
                            categories.remove(position);
                            categoryAdapter.notifyItemRemoved(position);
                            categoryAdapter.notifyItemRangeChanged(position, categories.size());
                            updateEmptyState();
                        }
                        Toast.makeText(requireContext(), "تم حذف التصنيف", Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "خطأ: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dataManager != null) {
            dataManager.shutdown();
        }
    }
}
