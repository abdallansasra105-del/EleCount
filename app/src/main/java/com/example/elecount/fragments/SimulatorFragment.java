package com.example.elecount.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.elecount.MainActivity;
import com.example.elecount.R;
import com.example.elecount.Hellper.DataManager;
import com.example.elecount.Hellper.UserSession;
import com.example.elecount.models.Device;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Fragment المحاكي — للضيف: استهلاك + سعر kW + مدة | للمسجّل: اختيار جهاز + مدة
 */
public class SimulatorFragment extends Fragment {

    private LinearLayout deviceSelectSection;
    private LinearLayout guestSimulatorSection;
    private Spinner deviceSpinner;
    private EditText powerInput;
    private EditText pricePerKwInput;
    private EditText durationInput;
    private Spinner durationUnitSpinner;
    private Button calculateButton;
    private CardView resultCard;
    private CardView guestLoginCard;
    private Button guestLoginButton;
    private TextView estimatedCostText;
    private TextView energyConsumedText;

    private DataManager dataManager;
    private boolean guestMode;

    private ArrayList<Device> devices = new ArrayList<>();
    private ArrayList<String> deviceNames = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_simulator, container, false);

        dataManager = new DataManager(requireContext());
        guestMode = UserSession.isGuest(requireContext());

        initViews(view);

        if (guestMode) {
            deviceSelectSection.setVisibility(View.GONE);
            guestSimulatorSection.setVisibility(View.VISIBLE);
        } else {
            loadDevices();
        }

        setupCalculateButton();
        return view;
    }

    private void initViews(View view) {
        deviceSelectSection = view.findViewById(R.id.deviceSelectSection);
        guestSimulatorSection = view.findViewById(R.id.guestSimulatorSection);
        deviceSpinner = view.findViewById(R.id.deviceSpinner);
        powerInput = view.findViewById(R.id.powerInput);
        pricePerKwInput = view.findViewById(R.id.pricePerKwInput);
        durationInput = view.findViewById(R.id.durationInput);
        durationUnitSpinner = view.findViewById(R.id.durationUnitSpinner);
        calculateButton = view.findViewById(R.id.calculateButton);
        resultCard = view.findViewById(R.id.resultCard);
        guestLoginCard = view.findViewById(R.id.guestLoginCard);
        guestLoginButton = view.findViewById(R.id.guestLoginButton);
        estimatedCostText = view.findViewById(R.id.estimatedCostText);
        energyConsumedText = view.findViewById(R.id.energyConsumedText);

        if (guestMode) {
            guestLoginCard.setVisibility(View.VISIBLE);
            guestLoginButton.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openLoginScreen();
                }
            });
        }
    }

    private void loadDevices() {
        dataManager.getAllDevices(new DataManager.DataCallback<ArrayList<Device>>() {
            @Override
            public void onSuccess(ArrayList<Device> result) {
                devices.clear();
                if (result != null) {
                    devices.addAll(result);
                }

                deviceNames.clear();
                for (Device device : devices) {
                    deviceNames.add(device.getName());
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (devices.isEmpty()) {
                            deviceNames.add("لا توجد أجهزة. أضف جهاز من تبويب الأجهزة");
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_item,
                                deviceNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        deviceSpinner.setAdapter(adapter);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        deviceNames.clear();
                        deviceNames.add("أضف أجهزة من تبويب الأجهزة أولاً");
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_item,
                                deviceNames
                        );
                        deviceSpinner.setAdapter(adapter);
                    });
                }
            }
        });
    }

    private void setupCalculateButton() {
        calculateButton.setOnClickListener(v -> {
            Device deviceForCalc;

            if (guestMode) {
                deviceForCalc = buildGuestDevice();
                if (deviceForCalc == null) {
                    return;
                }
            } else {
                if (devices.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.simulator_no_device),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                int selectedPosition = deviceSpinner.getSelectedItemPosition();
                if (selectedPosition < 0 || selectedPosition >= devices.size()) {
                    Toast.makeText(requireContext(), getString(R.string.simulator_no_device),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                deviceForCalc = devices.get(selectedPosition);
            }

            Double totalHours = parseDurationHours();
            if (totalHours == null) {
                return;
            }

            double estimatedCost = deviceForCalc.simulateCost(totalHours);
            double energyConsumed = deviceForCalc.getPowerConsumptionKw() * totalHours;
            showResult(estimatedCost, energyConsumed);
        });
    }

    @Nullable
    private Device buildGuestDevice() {
        String powerStr = powerInput.getText().toString().trim();
        String priceStr = pricePerKwInput.getText().toString().trim();

        if (powerStr.isEmpty()) {
            powerInput.setError(getString(R.string.error_empty_field));
            return null;
        }
        if (priceStr.isEmpty()) {
            pricePerKwInput.setError(getString(R.string.error_empty_field));
            return null;
        }

        double powerKw;
        double pricePerKw;
        try {
            powerKw = Double.parseDouble(powerStr);
            if (powerKw <= 0) {
                powerInput.setError("أدخل قيمة أكبر من صفر");
                return null;
            }
        } catch (NumberFormatException e) {
            powerInput.setError("قيمة غير صحيحة");
            return null;
        }

        try {
            pricePerKw = Double.parseDouble(priceStr);
            if (pricePerKw <= 0) {
                pricePerKwInput.setError("أدخل قيمة أكبر من صفر");
                return null;
            }
        } catch (NumberFormatException e) {
            pricePerKwInput.setError("قيمة غير صحيحة");
            return null;
        }

        Device device = new Device("محاكاة", "", powerKw);
        device.setPricePerKw(pricePerKw);
        return device;
    }

    @Nullable
    private Double parseDurationHours() {
        String durationStr = durationInput.getText().toString().trim();
        if (durationStr.isEmpty()) {
            durationInput.setError(getString(R.string.error_empty_field));
            return null;
        }

        double duration;
        try {
            duration = Double.parseDouble(durationStr);
            if (duration <= 0) {
                durationInput.setError("أدخل مدة أكبر من صفر");
                return null;
            }
        } catch (NumberFormatException e) {
            durationInput.setError("قيمة غير صحيحة");
            return null;
        }

        String unit = durationUnitSpinner.getSelectedItem().toString();
        double hoursMultiplier;
        if (unit.equals("ساعات")) {
            hoursMultiplier = 1;
        } else if (unit.equals("أيام")) {
            hoursMultiplier = 24;
        } else if (unit.equals("أشهر")) {
            hoursMultiplier = 24 * 30;
        } else {
            hoursMultiplier = 1;
        }

        return duration * hoursMultiplier;
    }

    private void showResult(double cost, double energy) {
        resultCard.setVisibility(View.VISIBLE);
        estimatedCostText.setText(String.format(Locale.getDefault(), "%.2f شيكل", cost));
        energyConsumedText.setText(String.format(Locale.getDefault(), "%.2f كيلو واط/ساعة", energy));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dataManager != null) {
            dataManager.shutdown();
        }
    }
}
