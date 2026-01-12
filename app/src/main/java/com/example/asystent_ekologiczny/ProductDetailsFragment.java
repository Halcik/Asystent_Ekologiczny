package com.example.asystent_ekologiczny;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.DecimalFormat;

/**
 * Fragment wyświetlający szczegóły pojedynczego produktu (tylko do odczytu).
 * Pobiera produkt na podstawie ID przekazanego w argumencie newInstance.
 */
public class ProductDetailsFragment extends Fragment {
    public static final String TAG = "ProductDetailsFragment";
    private static final String ARG_PRODUCT_ID = "product_id";

    private ProductDbHelper dbHelper;
    private long productId = -1;
    private final DecimalFormat priceFormat = new DecimalFormat("0.00");

    public static ProductDetailsFragment newInstance(long id) {
        ProductDetailsFragment f = new ProductDetailsFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_PRODUCT_ID, id);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getLong(ARG_PRODUCT_ID, -1);
        }
        dbHelper = new ProductDbHelper(requireContext());
        View back = view.findViewById(R.id.btn_back_details);
        if (back != null) back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        // Przycisk edycji
        View btnEdit = view.findViewById(R.id.btn_update);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> openEdit());
        }
        View btnDuplicate = view.findViewById(R.id.btn_duplicate);
        if (btnDuplicate != null) {
            btnDuplicate.setOnClickListener(v -> duplicateCurrent());
        }
        View btnDelete = view.findViewById(R.id.btn_delete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> confirmDelete());
        }
        // Usunięto obsługę cb_used_details (checkbox został usunięty z layoutu)
        View btnToggleUsed = view.findViewById(R.id.btn_toggle_used);
        TextView toggleLabel = view.findViewById(R.id.tv_toggle_used_label);
        if (btnToggleUsed != null) {
            btnToggleUsed.setOnClickListener(v -> {
                if (productId > 0) {
                    Product current = dbHelper.getProductById(productId);
                    if (current != null) {
                        boolean newState = !current.isUsed();
                        dbHelper.setUsed(productId, newState);
                        if (toggleLabel != null) {
                            toggleLabel.setText(newState ? R.string.label_used : R.string.label_active);
                        }
                        // Zmiana ikony
                        if (btnToggleUsed instanceof android.widget.ImageButton) {
                            ((android.widget.ImageButton) btnToggleUsed).setImageResource(newState ? R.drawable.ic_close_24 : R.drawable.ic_used);
                        }
                        bindProduct(view);
                        Bundle b = new Bundle();
                        b.putLong("updatedProductId", productId);
                        getParentFragmentManager().setFragmentResult("product_updated", b);
                    }
                }
            });
        }
        // Nasłuch wyniku aktualizacji aby odświeżyć dane po powrocie
        getParentFragmentManager().setFragmentResultListener("product_updated", getViewLifecycleOwner(), (key, bundle) -> {
            long updatedId = bundle.getLong("updatedProductId", -1);
            if (updatedId == productId) {
                bindProduct(view); // odśwież
            }
        });
        bindProduct(view);
    }

    /** Otwiera formularz w trybie edycji bieżącego produktu. */
    private void openEdit() {
        if (productId < 0) return;
        AddProductFragment editFragment = AddProductFragment.newEditInstance(productId);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.fragment_container, editFragment, AddProductFragment.TAG)
                .addToBackStack(AddProductFragment.TAG)
                .commit();
    }

    /** Ładuje produkt z DB i przypisuje pola. Jeśli brak – zamyka fragment. */
    private void bindProduct(View root) {
        if (productId < 0) {
            Toast.makeText(requireContext(), R.string.details_not_found, Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }
        Product p = dbHelper.getProductById(productId);
        if (p == null) {
            Toast.makeText(requireContext(), R.string.details_not_found, Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }
        ((TextView) root.findViewById(R.id.tv_name_value)).setText(emptyOr(p.getName()));
        ((TextView) root.findViewById(R.id.tv_price_value)).setText(getString(R.string.price_format, priceFormat.format(p.getPrice()).replace('.', ',')));
        ((TextView) root.findViewById(R.id.tv_expiration_value)).setText(emptyOr(p.getExpirationDate()));
        ((TextView) root.findViewById(R.id.tv_category_value)).setText(emptyOr(p.getCategory()));
        // Ikona kategorii
        View iconView = root.findViewById(R.id.iv_category_big);
        if (iconView instanceof android.widget.ImageView) {
            int iconRes = resolveCategoryIcon(p.getCategory());
            if (iconRes != 0) {
                ((android.widget.ImageView) iconView).setImageResource(iconRes);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
        }
        ((TextView) root.findViewById(R.id.tv_description_value)).setText(emptyOr(p.getDescription()));
        ((TextView) root.findViewById(R.id.tv_store_value)).setText(emptyOr(p.getStore()));
        ((TextView) root.findViewById(R.id.tv_purchase_value)).setText(emptyOr(p.getPurchaseDate()));
        // Usunięto checkbox cb_used_details – stan prezentowany jako etykieta toggle
        TextView toggleLabel = root.findViewById(R.id.tv_toggle_used_label);
        if (toggleLabel != null) toggleLabel.setText(p.isUsed() ? R.string.label_used : R.string.label_active);
        View toggleBtn = root.findViewById(R.id.btn_toggle_used);
        if (toggleBtn instanceof android.widget.ImageButton) {
            ((android.widget.ImageButton) toggleBtn).setImageResource(p.isUsed() ? R.drawable.ic_close_24 : R.drawable.ic_used);
        }
    }

    private String emptyOr(String v) {
        if (v == null || v.trim().isEmpty()) return getString(R.string.not_available);
        return v;
    }

    private int resolveCategoryIcon(String category) {
        if (category == null) return 0;
        String c = category.trim().toLowerCase();
        if (c.contains("owoc") || c.contains("fruit")) return R.drawable.ic_category_fruits;
        if (c.contains("warzyw") || c.contains("vegetable")) return R.drawable.ic_category_vegetables;
        if (c.contains("napoj") || c.contains("drink") || c.contains("sok")) return R.drawable.ic_category_drinks;
        if (c.contains("nabial") || c.contains("mlecz") || c.contains("dairy")) return R.drawable.ic_category_dairy;
        if (c.contains("pieczy") || c.contains("bread")) return R.drawable.ic_category_bread;
        return R.drawable.ic_category_other; // domyślna ikona dla pozostałych
    }

    private void duplicateCurrent() {
        if (productId < 0) return;
        long newId = dbHelper.duplicateProduct(productId);
        if (newId > 0) {
            Toast.makeText(requireContext(), R.string.duplicate_success, Toast.LENGTH_SHORT).show();
            Bundle b = new Bundle();
            b.putLong("newProductId", newId);
            getParentFragmentManager().setFragmentResult("product_added", b);
        } else {
            Toast.makeText(requireContext(), R.string.duplicate_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        if (productId < 0) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.delete_yes, (d, which) -> performDelete())
                .setNegativeButton(R.string.delete_no, null)
                .show();
    }

    private void performDelete() {
        boolean ok = dbHelper.deleteProduct(productId);
        if (ok) {
            Toast.makeText(requireContext(), R.string.delete_success, Toast.LENGTH_SHORT).show();
            Bundle b = new Bundle();
            b.putLong("deletedProductId", productId);
            getParentFragmentManager().setFragmentResult("product_deleted", b);
            requireActivity().getSupportFragmentManager().popBackStack();
        } else {
            Toast.makeText(requireContext(), R.string.delete_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) { dbHelper.close(); dbHelper = null; }
    }
}
