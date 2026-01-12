package com.example.asystent_ekologiczny;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.List;
import android.widget.AutoCompleteTextView;

/** Formularz dodawania / edycji opakowania kaucyjnego. */
public class AddDepositFragment extends Fragment {
    public static final String TAG = "AddDepositFragment";
    private static final String ARG_EDIT_ID = "edit_deposit_id";

    private TextInputLayout tilCategory, tilName, tilValue, tilBarcode;
    private AutoCompleteTextView actCategory; // lista kategorii
    private TextInputEditText etName, etValue, etBarcode;
    private MaterialButton btnSave;
    private DepositDbHelper dbHelper;
    private long editId = -1;

    // CameraX i ML Kit
    private FrameLayout cameraContainer;
    private PreviewView previewView;
    private ImageButton btnCloseCamera;
    private LifecycleCameraController cameraController;
    private BarcodeScanner barcodeScanner;

    // Launcher dla uprawnień
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showCamera();
                } else {
                    Toast.makeText(requireContext(), "Brak uprawnień do kamery", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_deposit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicjalizacja widoków
        tilCategory = view.findViewById(R.id.til_category);
        tilName = view.findViewById(R.id.til_name);
        tilValue = view.findViewById(R.id.til_value);
        tilBarcode = view.findViewById(R.id.til_barcode);
        actCategory = view.findViewById(R.id.act_category);
        etName = view.findViewById(R.id.et_name);
        etValue = view.findViewById(R.id.et_value);
        etBarcode = view.findViewById(R.id.et_barcode);
        btnSave = view.findViewById(R.id.btn_save);

        // Widoki kamery
        cameraContainer = view.findViewById(R.id.camera_container);
        previewView = view.findViewById(R.id.previewView);
        btnCloseCamera = view.findViewById(R.id.btn_close_camera);

        dbHelper = new DepositDbHelper(requireContext());

        // Przycisk powrotu
        View backBtn = view.findViewById(R.id.btn_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        // Obsługa ikony kamery w TextInputLayout
        tilBarcode.setEndIconOnClickListener(v -> {
            if (checkCameraPermission()) {
                showCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Zamknięcie kamery
        btnCloseCamera.setOnClickListener(v -> hideCamera());

        // Zapis
        btnSave.setOnClickListener(v -> saveDeposit());

        // Tryb edycji
        if (getArguments() != null) {
            editId = getArguments().getLong(ARG_EDIT_ID, -1);
            if (editId > 0) enterEditMode(editId);
        }

        // Inicjalizacja skanera kodów
        initBarcodeScanner();
        setupCategoryDropdown();
    }

    private void setupCategoryDropdown(){
        String[] categories = getResources().getStringArray(R.array.deposit_categories);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categories);
        actCategory.setAdapter(adapter);
        // Otwórz dropdown od razu po kliknięciu w pole
        actCategory.setOnClickListener(v -> actCategory.showDropDown());
        // Otwórz dropdown, gdy pole dostaje fokus
        actCategory.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actCategory.showDropDown(); });
        // Otwórz dropdown na dotknięcie (przed edycją), zwróć false, aby zachować standardowe zachowanie
        actCategory.setOnTouchListener((v, event) -> { if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) { actCategory.showDropDown(); } return false; });
        // Kliknięcie w cały kontener TextInputLayout też otwiera dropdown
        if (tilCategory != null) {
            tilCategory.setClickable(true);
            tilCategory.setOnClickListener(v -> { actCategory.requestFocus(); actCategory.showDropDown(); });
        }
    }

    private void initBarcodeScanner() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_CODE_128
                )
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showCamera() {
        cameraContainer.setVisibility(View.VISIBLE);
        startBarcodeScanning();
    }

    private void hideCamera() {
        cameraContainer.setVisibility(View.GONE);
        if (cameraController != null) {
            cameraController.unbind();
        }
    }

    private void startBarcodeScanning() {
        // Inicjalizacja CameraController
        cameraController = new LifecycleCameraController(requireContext());
        cameraController.bindToLifecycle(getViewLifecycleOwner());

        // Ustawienie MlKitAnalyzer
        cameraController.setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(requireContext()),
                new MlKitAnalyzer(
                        List.of(barcodeScanner),
                        CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                        ContextCompat.getMainExecutor(requireContext()),
                        result -> {
                            List<Barcode> barcodes = result.getValue(barcodeScanner);
                            if (barcodes != null && !barcodes.isEmpty()) {
                                // Pobierz pierwszy znaleziony kod
                                String barcode = barcodes.get(0).getRawValue();

                                // Wypełnij pole tekstowe
                                etBarcode.setText(barcode);

                                // Zamknij kamerę
                                hideCamera();

                                // Potwierdzenie
                                Toast.makeText(requireContext(),
                                        "Zeskanowano: " + barcode,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                )
        );

        // Podłączenie PreviewView
        previewView.setController(cameraController);
    }

    private void enterEditMode(long id) {
        DepositItem item = dbHelper.getDepositById(id);
        if (item == null) {
            Toast.makeText(requireContext(), "Nie znaleziono", Toast.LENGTH_SHORT).show();
            return;
        }
        actCategory.setText(item.getCategory(), false);
        etName.setText(item.getName());
        etValue.setText(String.valueOf(item.getValue()).replace('.', ','));
        etBarcode.setText(item.getBarcode());
        btnSave.setText(R.string.save_changes);
        View title = getView() != null ? getView().findViewById(R.id.add_deposit_title) : null;
        if (title instanceof android.widget.TextView) {
            ((android.widget.TextView) title).setText(R.string.edit_deposit_title);
        }
    }

    private void saveDeposit() {
        clearErrors();
        String category = actCategory.getText()==null?"":actCategory.getText().toString().trim();
        String name = text(etName);
        String valueStr = text(etValue);
        String barcode = text(etBarcode);
        boolean valid = true;
        if (category.isEmpty()) { tilCategory.setError("Wymagane"); valid = false; }
        if (name.isEmpty()) { tilName.setError("Wymagane"); valid = false; }
        if (valueStr.isEmpty()) { tilValue.setError("Wymagane"); valid = false; }
        double value = 0;
        if (!valueStr.isEmpty()) {
            try {
                value = Double.parseDouble(valueStr.replace(',', '.'));
                if (value <= 0) { tilValue.setError(">0"); valid = false; }
            } catch (NumberFormatException e) { tilValue.setError("Liczba"); valid = false; }
        }
        if (!valid) return;
        if (editId > 0) {
            // Zachowaj obecny stan 'returned' przy aktualizacji
            DepositItem existing = dbHelper.getDepositById(editId);
            boolean returned = existing != null && existing.isReturned();
            DepositItem updated = new DepositItem(editId, category, name, value, barcode, returned);
            boolean ok = dbHelper.updateDeposit(updated);
            if (ok) {
                Toast.makeText(requireContext(), "Zaktualizowano", Toast.LENGTH_SHORT).show();
                Bundle b = new Bundle(); b.putLong("deposit_updated_id", editId);
                requireActivity().getSupportFragmentManager().setFragmentResult("deposit_updated", b);
                requireActivity().getSupportFragmentManager().popBackStack();
            } else { Toast.makeText(requireContext(), "Błąd", Toast.LENGTH_SHORT).show(); }
        } else {
            // Nowe opakowanie nie jest zwrócone domyślnie
            DepositItem item = new DepositItem(category, name, value, barcode);
            long newId = dbHelper.insertDeposit(item);
            if (newId > 0) {
                Toast.makeText(requireContext(), "Zapisano", Toast.LENGTH_SHORT).show();
                Bundle b = new Bundle(); b.putLong("deposit_added_id", newId);
                requireActivity().getSupportFragmentManager().setFragmentResult("deposit_added", b);
                requireActivity().getSupportFragmentManager().popBackStack();
            } else { Toast.makeText(requireContext(), "Błąd zapisu", Toast.LENGTH_SHORT).show(); }
        }
    }

    private void clearErrors() {
        tilCategory.setError(null);
        tilName.setError(null);
        tilValue.setError(null);
        tilBarcode.setError(null);
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraController != null) {
            cameraController.unbind();
            cameraController = null;
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
            barcodeScanner = null;
        }
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
    }

    public static AddDepositFragment newEditInstance(long id) {
        AddDepositFragment f = new AddDepositFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_EDIT_ID, id);
        f.setArguments(b);
        return f;
    }
}
