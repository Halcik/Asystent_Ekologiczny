package com.example.asystent_ekologiczny;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddProductFragment extends Fragment {
    public static final String TAG = "AddProductFragment";

    private TextInputLayout tilName, tilPrice, tilExpiration, tilCategory, tilDescription, tilStore, tilPurchase;
    private TextInputEditText etName, etPrice, etExpiration, etDescription, etPurchase;
    private MaterialAutoCompleteTextView etCategory, etStore;
    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> storeAdapter;
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
        // Inicjalizacja layoutów
        tilName = view.findViewById(R.id.til_name);
        tilPrice = view.findViewById(R.id.til_price);
        tilExpiration = view.findViewById(R.id.til_expiration);
        tilCategory = view.findViewById(R.id.til_category);
        tilDescription = view.findViewById(R.id.til_description);
        tilStore = view.findViewById(R.id.til_store);
        tilPurchase = view.findViewById(R.id.til_purchase);
        // Pola
        etName = view.findViewById(R.id.et_name);
        etPrice = view.findViewById(R.id.et_price);
        etExpiration = view.findViewById(R.id.et_expiration);
        etCategory = view.findViewById(R.id.et_category);
        etDescription = view.findViewById(R.id.et_description);
        etStore = view.findViewById(R.id.et_store);
        etPurchase = view.findViewById(R.id.et_purchase);

        // Domyślna data zakupu = dziś
        etPurchase.setText(dateFormat.format(new Date()));

        ProductDbHelper helper = new ProductDbHelper(requireContext());
        List<String> categories = new ArrayList<>(helper.getDistinctCategories());
        List<String> stores = new ArrayList<>(helper.getDistinctStores());
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categories);
        storeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, stores);
        etCategory.setAdapter(categoryAdapter);
        etStore.setAdapter(storeAdapter);

        View.OnClickListener dateClick = v -> showDatePicker((TextInputEditText) v);
        etExpiration.setOnClickListener(dateClick);
        etPurchase.setOnClickListener(dateClick);

        MaterialButton btnSave = view.findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveProduct());
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        try {
            String current = target.getText() == null ? null : target.getText().toString();
            if (current != null && current.matches("\\d{4}-\\d{2}-\\d{2}")) {
                Date parsed = dateFormat.parse(current);
                if (parsed != null) cal.setTime(parsed);
            }
        } catch (ParseException ignored) {}
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

        if (valid && isValidDate(purchase)) {
            try {
                Date pDate = dateFormat.parse(purchase);
                Date today = stripTime(new Date());
                if (pDate != null && pDate.after(today)) {
                    tilPurchase.setError("Nie może być w przyszłości");
                    valid = false;
                }
            } catch (ParseException ignored) {}
        }

        if (!valid) return;

        Product p = new Product(name, price, exp, category, description, store, purchase);
        ProductDbHelper helper = new ProductDbHelper(requireContext());
        long id = helper.insertProduct(p);
        if (id > 0) {
            addIfNew(categoryAdapter, category);
            addIfNew(storeAdapter, store);
            Toast.makeText(requireContext(), "Zapisano", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        } else {
            Toast.makeText(requireContext(), "Błąd zapisu", Toast.LENGTH_SHORT).show();
        }
    }

    private void addIfNew(ArrayAdapter<String> adapter, String value) {
        if (value == null || value.isEmpty()) return;
        boolean exists = false;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (value.equalsIgnoreCase(adapter.getItem(i))) { exists = true; break; }
        }
        if (!exists) {
            adapter.add(value);
            adapter.sort(String::compareToIgnoreCase);
            adapter.notifyDataSetChanged();
        }
    }

    private boolean isValidDate(String value) {
        try { dateFormat.parse(value); return true; } catch (ParseException e) { return false; }
    }

    private String text(TextInputEditText et) { return et.getText() == null ? "" : et.getText().toString().trim(); }
    private String text(MaterialAutoCompleteTextView et) { return et.getText() == null ? "" : et.getText().toString().trim(); }

    private Date stripTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

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
