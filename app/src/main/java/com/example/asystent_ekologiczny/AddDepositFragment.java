package com.example.asystent_ekologiczny;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Formularz dodawania / edycji opakowania kaucyjnego. */
public class AddDepositFragment extends Fragment {
    public static final String TAG = "AddDepositFragment";
    private static final String ARG_EDIT_ID = "edit_deposit_id";

    private TextInputLayout tilType, tilValue, tilBarcode;
    private TextInputEditText etType, etValue, etBarcode;
    private MaterialButton btnSave;
    private DepositDbHelper dbHelper;
    private long editId = -1;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_deposit, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tilType = view.findViewById(R.id.til_type);
        tilValue = view.findViewById(R.id.til_value);
        tilBarcode = view.findViewById(R.id.til_barcode);
        etType = view.findViewById(R.id.et_type);
        etValue = view.findViewById(R.id.et_value);
        etBarcode = view.findViewById(R.id.et_barcode);
        btnSave = view.findViewById(R.id.btn_save);
        dbHelper = new DepositDbHelper(requireContext());

        View backBtn = view.findViewById(R.id.btn_back);
        if (backBtn != null) backBtn.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        btnSave.setOnClickListener(v -> saveDeposit());

        if (getArguments() != null) {
            editId = getArguments().getLong(ARG_EDIT_ID, -1);
            if (editId > 0) enterEditMode(editId);
        }
    }

    private void enterEditMode(long id) {
        DepositItem item = dbHelper.getDepositById(id);
        if (item == null) { Toast.makeText(requireContext(), "Nie znaleziono", Toast.LENGTH_SHORT).show(); return; }
        etType.setText(item.getType());
        etValue.setText(String.valueOf(item.getValue()).replace('.', ','));
        etBarcode.setText(item.getBarcode());
        btnSave.setText(R.string.save_changes);
        View title = getView()!=null? getView().findViewById(R.id.add_deposit_title):null;
        if (title instanceof android.widget.TextView) {
            ((android.widget.TextView) title).setText(R.string.edit_deposit_title);
        }
    }

    private void saveDeposit() {
        clearErrors();
        String type = text(etType);
        String valueStr = text(etValue);
        String barcode = text(etBarcode);
        boolean valid = true;
        if (type.isEmpty()) { tilType.setError("Wymagane"); valid=false; }
        if (valueStr.isEmpty()) { tilValue.setError("Wymagane"); valid=false; }
        double value = 0;
        if (!valueStr.isEmpty()) {
            try { value = Double.parseDouble(valueStr.replace(',', '.')); if (value <= 0) { tilValue.setError(">0"); valid=false;} } catch (NumberFormatException e) { tilValue.setError("Liczba"); valid=false; }
        }
        if (!valid) return;
        if (editId > 0) {
            DepositItem updated = new DepositItem(editId, type, value, barcode);
            boolean ok = dbHelper.updateDeposit(updated);
            if (ok) {
                Toast.makeText(requireContext(), "Zaktualizowano", Toast.LENGTH_SHORT).show();
                Bundle b = new Bundle(); b.putLong("deposit_updated_id", editId);
                requireActivity().getSupportFragmentManager().setFragmentResult("deposit_updated", b);
                requireActivity().getSupportFragmentManager().popBackStack();
            } else Toast.makeText(requireContext(), "Błąd", Toast.LENGTH_SHORT).show();
        } else {
            DepositItem item = new DepositItem(type, value, barcode);
            long newId = dbHelper.insertDeposit(item);
            if (newId > 0) {
                Toast.makeText(requireContext(), "Zapisano", Toast.LENGTH_SHORT).show();
                Bundle b = new Bundle(); b.putLong("deposit_added_id", newId);
                requireActivity().getSupportFragmentManager().setFragmentResult("deposit_added", b);
                requireActivity().getSupportFragmentManager().popBackStack();
            } else Toast.makeText(requireContext(), "Błąd zapisu", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearErrors() {
        tilType.setError(null); tilValue.setError(null); tilBarcode.setError(null);
    }
    private String text(TextInputEditText et) { return et.getText()==null?"":et.getText().toString().trim(); }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) { dbHelper.close(); dbHelper = null; }
    }

    public static AddDepositFragment newEditInstance(long id) {
        AddDepositFragment f = new AddDepositFragment();
        Bundle b = new Bundle(); b.putLong(ARG_EDIT_ID, id); f.setArguments(b); return f;
    }
}

