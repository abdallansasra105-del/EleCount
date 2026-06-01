package com.example.elecount.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.elecount.LoginActivity;
import com.example.elecount.R;
import com.example.elecount.Hellper.DataManager;
import com.example.elecount.Hellper.UserSession;
import com.example.elecount.models.User;

import java.io.File;
import java.util.Locale;

import com.google.android.material.button.MaterialButton;

/**
 * Fragment الإعدادات — الملف الشخصي، التصنيفات، والحساب
 */
public class SettingsFragment extends Fragment {

    private CardView profileCard;
    private ImageView userImage;
    private TextView userName;
    private TextView userEmail;
    private TextView profileEditHint;
    private CardView categoriesCard;
    private EditText priceInput;
    private MaterialButton savePriceButton;
    private CardView authCard;
    private TextView authText;

    private DataManager dataManager;

    private Uri selectedImageUri;
    private Uri cameraImageUri;
    private ImageView dialogImagePreview;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        updateDialogImagePreview();
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        selectedImageUri = cameraImageUri;
                        updateDialogImagePreview();
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchCamera();
                    } else {
                        Toast.makeText(requireContext(), R.string.permission_camera, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        dataManager = new DataManager(requireContext());
        initViews(view);
        refreshSessionAndUserInfo();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        profileCard = view.findViewById(R.id.profileCard);
        userImage = view.findViewById(R.id.userImage);
        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);
        profileEditHint = view.findViewById(R.id.profileEditHint);
        categoriesCard = view.findViewById(R.id.categoriesCard);
        priceInput = view.findViewById(R.id.priceInput);
        savePriceButton = view.findViewById(R.id.savePriceButton);
        authCard = view.findViewById(R.id.authCard);
        authText = view.findViewById(R.id.authText);
    }

    private void updateUserInfo() {
        User user = UserSession.load(requireContext());
        if (user != null && user.getName() != null && !user.getName().isEmpty()) {
            userName.setText(user.getName());
            userEmail.setText(user.getEmail());
            authText.setText(R.string.settings_logout);
            profileEditHint.setVisibility(View.VISIBLE);
            savePriceButton.setVisibility(View.VISIBLE);
            loadSavedPrice();

            if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                dataManager.loadImageIntoView(user.getImageUrl(), userImage,
                        android.R.drawable.ic_menu_myplaces);
            } else {
                userImage.setImageResource(android.R.drawable.ic_menu_myplaces);
            }
        } else {
            userName.setText("مستخدم ضيف");
            userEmail.setText("يعمل في وضع المحاكي");
            authText.setText(R.string.settings_login);
            profileEditHint.setVisibility(View.GONE);
            savePriceButton.setVisibility(View.GONE);
            userImage.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
    }

    private void loadSavedPrice() {
        double price = UserSession.getPricePerKw(requireContext());
        priceInput.setText(String.format(Locale.US, "%.2f", price));
    }

    private void setupListeners() {
        profileCard.setOnClickListener(v -> {
            if (UserSession.isLoggedIn(requireContext())) {
                dataManager.resolveSessionUser(requireContext(), new DataManager.DataCallback<User>() {
                    @Override
                    public void onSuccess(User result) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> showEditProfileDialog(result));
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show());
                        }
                    }
                });
            } else {
                Toast.makeText(requireContext(), R.string.settings_login_required, Toast.LENGTH_SHORT).show();
            }
        });

        categoriesCard.setOnClickListener(v -> openCategoriesFragment());

        savePriceButton.setOnClickListener(v -> saveElectricityPrice());

        authCard.setOnClickListener(v -> {
            if (UserSession.isLoggedIn(requireContext())) {
                logout();
            } else {
                goToLogin();
            }
        });
    }

    private void saveElectricityPrice() {
        if (!UserSession.isLoggedIn(requireContext())) {
            Toast.makeText(requireContext(), R.string.settings_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String priceStr = priceInput.getText().toString().trim();
        if (priceStr.isEmpty()) {
            priceInput.setError(getString(R.string.error_empty_field));
            return;
        }

        double pricePerKw;
        try {
            pricePerKw = Double.parseDouble(priceStr);
            if (pricePerKw <= 0) {
                priceInput.setError(getString(R.string.settings_price_invalid));
                return;
            }
        } catch (NumberFormatException e) {
            priceInput.setError(getString(R.string.settings_price_invalid));
            return;
        }

        savePriceButton.setEnabled(false);

        dataManager.updateUserPricePerKw(pricePerKw, requireContext(), new DataManager.DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) {
                    return;
                }
                UserSession.save(requireContext(), user);
                syncDevicesPrice(pricePerKw);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    savePriceButton.setEnabled(true);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void syncDevicesPrice(double pricePerKw) {
        dataManager.updateAllDevicesPricePerKw(pricePerKw, new DataManager.DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer updatedCount) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    savePriceButton.setEnabled(true);
                    loadSavedPrice();
                    String message = getString(R.string.settings_price_saved);
                    if (updatedCount != null && updatedCount > 0) {
                        message = message + " (" + updatedCount + " جهاز)";
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    savePriceButton.setEnabled(true);
                    Toast.makeText(requireContext(),
                            getString(R.string.settings_price_saved) + "\n" + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void refreshSessionAndUserInfo() {
        if (!UserSession.isLoggedIn(requireContext())) {
            updateUserInfo();
            return;
        }
        dataManager.resolveSessionUser(requireContext(), new DataManager.DataCallback<User>() {
            @Override
            public void onSuccess(User result) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        updateUserInfo();
                        loadSavedPrice();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> updateUserInfo());
                }
            }
        });
    }

    private void showEditProfileDialog(User user) {
        if (user == null) {
            return;
        }

        selectedImageUri = null;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile, null, false);

        dialogImagePreview = dialogView.findViewById(R.id.profileImagePreview);
        EditText nameInput = dialogView.findViewById(R.id.profileNameInput);
        nameInput.setText(user.getName());

        if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
            dataManager.loadImageIntoView(user.getImageUrl(), dialogImagePreview,
                    android.R.drawable.ic_menu_myplaces);
        }

        dialogImagePreview.setOnClickListener(v -> showImageSourceDialog());

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, which) ->
                        saveProfile(user, nameInput.getText().toString().trim()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveProfile(User user, String newName) {
        if (newName.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }

        user.setName(newName);
        Runnable saveToCloud = () -> dataManager.updateUserProfile(user,
                new DataManager.DataCallback<User>() {
                    @Override
                    public void onSuccess(User result) {
                        if (!isAdded()) {
                            return;
                        }
                        UserSession.save(requireContext(), result);
                        requireActivity().runOnUiThread(() -> {
                            updateUserInfo();
                            Toast.makeText(requireContext(),
                                    R.string.settings_profile_saved, Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onSuccess(User result, String warning) {
                        onSuccess(result);
                        if (isAdded() && warning != null && !warning.isEmpty()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), warning, Toast.LENGTH_LONG).show());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show());
                        }
                    }
                });

        if (selectedImageUri != null) {
            dataManager.uploadProfileImage(requireContext(), selectedImageUri,
                    new DataManager.DataCallback<String>() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            user.setImageUrl(imageUrl);
                            saveToCloud.run();
                        }

                        @Override
                        public void onError(String error) {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(requireContext(),
                                                "فشل رفع الصورة: " + error, Toast.LENGTH_LONG).show());
                            }
                        }
                    });
        } else {
            saveToCloud.run();
        }
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_change_photo)
                .setItems(new CharSequence[]{
                        getString(R.string.image_camera),
                        getString(R.string.image_gallery)
                }, (d, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        pickImageLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        try {
            File imageFile = new File(requireContext().getCacheDir(),
                    "profile_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    imageFile
            );
            takePictureLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "تعذر فتح الكاميرا", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDialogImagePreview() {
        if (dialogImagePreview != null && selectedImageUri != null) {
            dialogImagePreview.setImageURI(selectedImageUri);
        }
    }

    private void logout() {
        UserSession.clear(requireContext());
        dataManager.logout();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void openCategoriesFragment() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new CategoriesFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSessionAndUserInfo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dataManager != null) {
            dataManager.shutdown();
        }
    }
}
