package com.example.asystent_ekologiczny;

import android.app.DatePickerDialog;
import android.media.MediaPlayer;
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

/**
 * Fragment formularza dodawania / edycji produktu.
 * Tryb edycji jeśli zostanie przekazany argument ARG_EDIT_ID.
 */
public class AddProductFragment extends Fragment {
    public static final String TAG = "AddProductFragment";
    private static final String ARG_EDIT_ID = "edit_product_id"; // klucz ID edytowanego produktu

    private TextInputLayout tilName, tilPrice, tilExpiration, tilCategory, tilDescription, tilStore, tilPurchase;
    private TextInputEditText etName, etPrice, etExpiration, etDescription, etPurchase;
    private MaterialAutoCompleteTextView etCategory, etStore;
    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> storeAdapter;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private ProductDbHelper dbHelper; // dodane pole
    private long editProductId = -1; // ID produktu w trybie edycji
    private MaterialButton btnSave; // referencja do przycisku zapisu aby zmienić tekst
    private boolean editUsed = false; // zachowujemy flagę 'used' z edytowanego produktu
    private android.widget.CheckBox cbUsed; // checkbox zużycia
    private MediaPlayer mediaPlayer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicjalizacja MediaPlayera
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.save);
        mediaPlayer.setOnCompletionListener(player -> {
            player.release();  // zwolnij po zakończeniu
        });

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
        cbUsed = null; // usunięty z layoutu

        // Domyślna data zakupu = dziś (tylko w trybie dodawania)
        etPurchase.setText(dateFormat.format(new Date()));

        dbHelper = new ProductDbHelper(requireContext());
        List<String> categories = new ArrayList<>(dbHelper.getDistinctCategories());
        List<String> stores = new ArrayList<>(dbHelper.getDistinctStores());
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categories);
        storeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, stores);
        etCategory.setAdapter(categoryAdapter);
        etStore.setAdapter(storeAdapter);

        View.OnClickListener dateClick = v -> showDatePicker((TextInputEditText) v);
        etExpiration.setOnClickListener(dateClick);
        etPurchase.setOnClickListener(dateClick);

        btnSave = view.findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveProduct());

        View backBtn = view.findViewById(R.id.btn_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                // Cofnięcie do poprzedniego fragmentu
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // Sprawdź czy mamy argument edycji
        if (getArguments() != null) {
            editProductId = getArguments().getLong(ARG_EDIT_ID, -1);
            if (editProductId > 0) {
                enterEditMode(editProductId);
            }
        }
    }

    /** Ustawia fragment w tryb edycji – pobiera produkt i wypełnia pola. */
    private void enterEditMode(long id) {
        Product p = dbHelper.getProductById(id);
        if (p == null) {
            Toast.makeText(requireContext(), "Nie znaleziono produktu do edycji", Toast.LENGTH_SHORT).show();
            return; // pozostaje tryb dodawania
        }
        editUsed = p.isUsed(); // zapamiętujemy wartość zużycia (formularz jej nie zmienia)
        etName.setText(p.getName());
        etPrice.setText(String.valueOf(p.getPrice()).replace('.', ','));
        etExpiration.setText(p.getExpirationDate());
        etCategory.setText(p.getCategory(), false);
        etDescription.setText(p.getDescription());
        etStore.setText(p.getStore(), false);
        etPurchase.setText(p.getPurchaseDate());
        if (cbUsed != null) cbUsed.setChecked(editUsed);
        if (btnSave != null) {
            btnSave.setText(editProductId > 0 ? R.string.save_changes : R.string.save);
        }
        // Tytuł zmieniamy jeśli istnieje
        View title = getView() != null ? getView().findViewById(R.id.add_product_title) : null;
        if (title instanceof com.google.android.material.textview.MaterialTextView) {
            ((com.google.android.material.textview.MaterialTextView) title).setText(R.string.edit_product_title);
        } else if (title instanceof android.widget.TextView) {
            ((android.widget.TextView) title).setText(R.string.edit_product_title);
        }
    }

    /** Otwiera dialog wyboru daty i wpisuje wynik do wskazanego pola. */
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

    /** Zbiera dane z pól, waliduje i zapisuje nowy lub aktualizuje istniejący produkt. */
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
            try {
                price = Double.parseDouble(priceStr.replace(",","."));
                if (price <= 0) {tilPrice.setError("Cena musi być większa od 0"); valid = false;}
            }
            catch (NumberFormatException e) { tilPrice.setError("Niepoprawna liczba"); valid = false; }
        }
        if (exp.isEmpty()) { tilExpiration.setError("Wymagane"); valid = false; }
        if (category.isEmpty()) { tilCategory.setError("Wymagane"); valid = false; }
        if (store.isEmpty()) { tilStore.setError("Wymagane"); valid = false; }
        if (purchase.isEmpty()) { tilPurchase.setError("Wymagane"); valid = false; }

        if (!exp.isEmpty() && !isValidDate(exp)) { tilExpiration.setError("Format yyyy-MM-dd"); valid = false; }

        if (valid && isValidDate(exp)) {
            try {
                Date pDate = dateFormat.parse(exp);
                Date today = stripTime(new Date());
                if (pDate != null && pDate.before(today)) {
                    tilExpiration.setError("Nie może być w przeszłości");
                    valid = false;
                }
            } catch (ParseException ignored) {}
        }

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

        if (valid) {
            if (editProductId > 0) {
                // Tryb edycji
                Product updated = new Product(editProductId, name, price, exp, category, description, store, purchase, editUsed);
                boolean ok = dbHelper.updateProduct(updated);
                if (ok) {
                    addIfNew(categoryAdapter, category);
                    addIfNew(storeAdapter, store);
                    // dźwięk zapisu

                    mediaPlayer.start();
                    Toast.makeText(requireContext(), "Zaktualizowano", Toast.LENGTH_SHORT).show();
                    Bundle result = new Bundle();
                    result.putLong("updatedProductId", editProductId);
                    requireActivity().getSupportFragmentManager().setFragmentResult("product_updated", result);
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    Toast.makeText(requireContext(), "Błąd aktualizacji", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Dodawanie
                Product p = new Product(name, price, exp, category, description, store, purchase); // zawsze not used na starcie
                long id = dbHelper.insertProduct(p);
                if (id > 0) {
                    addIfNew(categoryAdapter, category);
                    addIfNew(storeAdapter, store);
                    // dźwięk zapisu
                    mediaPlayer.start();

                    Toast.makeText(requireContext(), "Zapisano", Toast.LENGTH_SHORT).show();
                    Bundle result = new Bundle();
                    result.putLong("newProductId", id);
                    requireActivity().getSupportFragmentManager().setFragmentResult("product_added", result);
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    Toast.makeText(requireContext(), "Błąd zapisu", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /** Czy wartość ma poprawny format daty yyyy-MM-dd. */
    private boolean isValidDate(String value) {
        try { dateFormat.parse(value); return true; } catch (ParseException e) { return false; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) { dbHelper.close(); dbHelper = null; }
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

    /** Fabryka tworząca fragment w trybie edycji istniejącego produktu. */
    public static AddProductFragment newEditInstance(long productId) {
        AddProductFragment f = new AddProductFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_EDIT_ID, productId);
        f.setArguments(b);
        return f;
    }
}
