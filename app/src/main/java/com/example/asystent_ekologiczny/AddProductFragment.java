package com.example.asystent_ekologiczny;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddProductFragment extends Fragment {

    public static final String TAG = "AddProductFragment";
    private TextInputLayout tilName, tilPrice, tilExpiration, tilCategory, tilDescription, tilStore, tilPurchase;
    private TextInputEditText etName, etPrice, etExpiration, etCategory, etDescription, etStore, etPurchase;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dateFormat.setLenient(false);

        tilName = view.findViewById(R.id.til_name);
        tilPrice = view.findViewById(R.id.til_price);
        tilExpiration = view.findViewById(R.id.til_expiration);
        tilCategory = view.findViewById(R.id.til_category);
        tilDescription = view.findViewById(R.id.til_description);
        tilStore = view.findViewById(R.id.til_store);
        tilPurchase = view.findViewById(R.id.til_purchase);

        etName = view.findViewById(R.id.et_name);
        etPrice = view.findViewById(R.id.et_price);
        etExpiration = view.findViewById(R.id.et_expiration);
        etCategory = view.findViewById(R.id.et_category);
        etDescription = view.findViewById(R.id.et_description);
        etStore = view.findViewById(R.id.et_store);
        etPurchase = view.findViewById(R.id.et_purchase);

        View.OnClickListener dateClick = v -> showDatePicker((TextInputEditText) v);
        etExpiration.setOnClickListener(dateClick);
        etPurchase.setOnClickListener(dateClick);

        MaterialButton btnSave = view.findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveProduct());
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (DatePicker datePicker, int y, int m, int d) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(Calendar.YEAR, y);
            picked.set(Calendar.MONTH, m);
            picked.set(Calendar.DAY_OF_MONTH, d);
            target.setText(dateFormat.format(picked.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void saveProduct() {
        clearErrors();
        String name = text(etName);
        String priceStr = text(etPrice);
        String exp = text(etExpiration);
        String category = text(etCategory);
        String description = text(etDescription);
        String store = text(etStore);
        String purchase = text(etPurchase);

        boolean valid = true;
        if (name.isEmpty()) { tilName.setError("Wymagane"); valid = false; }
        if (priceStr.isEmpty()) { tilPrice.setError("Wymagane"); valid = false; }
        double price = 0;
        if (!priceStr.isEmpty()) {
            try { price = Double.parseDouble(priceStr.replace(",",".")); }
            catch (NumberFormatException e) { tilPrice.setError("Niepoprawna liczba"); valid = false; }
        }
        if (exp.isEmpty()) { tilExpiration.setError("Wymagane"); valid = false; }
        if (category.isEmpty()) { tilCategory.setError("Wymagane"); valid = false; }
        if (store.isEmpty()) { tilStore.setError("Wymagane"); valid = false; }
        if (purchase.isEmpty()) { tilPurchase.setError("Wymagane"); valid = false; }

        if (!exp.isEmpty() && !isValidDate(exp)) { tilExpiration.setError("Format yyyy-MM-dd"); valid = false; }
        if (!purchase.isEmpty() && !isValidDate(purchase)) { tilPurchase.setError("Format yyyy-MM-dd"); valid = false; }

        if (!valid) return;

        Product p = new Product(name, price, exp, category, description, store, purchase);
        ProductDbHelper helper = new ProductDbHelper(requireContext());
        long id = helper.insertProduct(p);
        if (id > 0) {
            Toast.makeText(requireContext(), "Zapisano", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        } else {
            Toast.makeText(requireContext(), "Błąd zapisu", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidDate(String value) {
        try { dateFormat.parse(value); return true; } catch (ParseException e) { return false; }
    }

    private String text(TextInputEditText et) { return et.getText() == null ? "" : et.getText().toString().trim(); }
    private void clearErrors() {
        tilName.setError(null);
        tilPrice.setError(null);
        tilExpiration.setError(null);
        tilCategory.setError(null);
        tilDescription.setError(null);
        tilStore.setError(null);
        tilPurchase.setError(null);
    }
}
