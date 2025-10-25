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
        bindProduct(view);
    }

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
        ((TextView) root.findViewById(R.id.tv_price_value)).setText(priceFormat.format(p.getPrice()).replace('.', ',') + " zł");
        ((TextView) root.findViewById(R.id.tv_expiration_value)).setText(emptyOr(p.getExpirationDate()));
        ((TextView) root.findViewById(R.id.tv_category_value)).setText(emptyOr(p.getCategory()));
        ((TextView) root.findViewById(R.id.tv_description_value)).setText(emptyOr(p.getDescription()));
        ((TextView) root.findViewById(R.id.tv_store_value)).setText(emptyOr(p.getStore()));
        ((TextView) root.findViewById(R.id.tv_purchase_value)).setText(emptyOr(p.getPurchaseDate()));
    }

    private String emptyOr(String v) {
        if (v == null || v.trim().isEmpty()) return getString(R.string.not_available);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbHelper != null) { dbHelper.close(); dbHelper = null; }
    }
}
