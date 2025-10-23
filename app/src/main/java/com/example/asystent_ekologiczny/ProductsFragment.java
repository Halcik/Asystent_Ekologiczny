package com.example.asystent_ekologiczny;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductsFragment extends Fragment {
    public static final String TAG = "ProductsFragment";

    private ProductDbHelper dbHelper;
    private LinearLayout listLayout;
    private TextView tvTotal;
    private TextView tvExpiring;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final DecimalFormat priceFormat = new DecimalFormat("0.00");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new ProductDbHelper(requireContext());
        listLayout = view.findViewById(R.id.products_list);
        tvTotal = view.findViewById(R.id.products_total);
        tvExpiring = view.findViewById(R.id.products_expiring);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_product);
        fab.setOnClickListener(v -> openAddProduct());
        dateFormat.setLenient(false);
        loadProducts();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProducts();
    }

    private void openAddProduct() {
        AddProductFragment fragment = new AddProductFragment();
        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        ft.replace(R.id.fragment_container, fragment, AddProductFragment.TAG);
        ft.addToBackStack(AddProductFragment.TAG);
        ft.commit();
    }

    private void loadProducts() {
        if (listLayout == null) return;
        listLayout.removeAllViews();
        List<Product> products = dbHelper.getAllProducts();
        int expiringCount = 0; // liczymy wszystko co ma <=3 dni do końca lub już po terminie
        Date today = stripTime(new Date());
        for (Product p : products) {
            if (p.getExpirationDate() != null && !p.getExpirationDate().isEmpty()) {
                try {
                    Date exp = dateFormat.parse(p.getExpirationDate());
                    if (exp != null) {
                        long days = (exp.getTime() - today.getTime()) / (1000L*60*60*24);
                        if (days <= 3) expiringCount++; // zawiera też wartości ujemne (po terminie)
                    }
                } catch (ParseException ignored) {}
            }
            addProductCard(p, today);
        }
        tvTotal.setText(String.valueOf(products.size()));
        tvExpiring.setText(String.valueOf(expiringCount));
    }

    private Date stripTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private void addProductCard(Product p, Date today) {
        if (getContext() == null) return;
        // Określamy kolor ramki na podstawie daty ważności
        int colorRes = R.color.card_border_success; // default
        int colorCard = R.color.card_background_ok;
        if (p.getExpirationDate() != null && !p.getExpirationDate().isEmpty()) {
            try {
                Date exp = dateFormat.parse(p.getExpirationDate());
                if (exp != null) {
                    long days = (exp.getTime() - today.getTime()) / (1000L*60*60*24);
                    if (days < 0) {
                        colorRes = R.color.card_border_danger;
                        colorCard = R.color.card_background_danger;
                    } else if (days <= 3) {
                        colorRes = R.color.card_border_warning;
                    }
                }
            } catch (ParseException ignored) {}
        }
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (getResources().getDisplayMetrics().density * 8);
        card.setLayoutParams(params);
        card.setStrokeWidth((int) (getResources().getDisplayMetrics().density * 3));
        card.setStrokeColor(ContextCompat.getColor(requireContext(), colorRes));
        //card.setCardElevation(1f);
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorCard));
        card.setClickable(true);
        card.setPreventCornerOverlap(true);
        card.setContentPadding(24,24,24,24);
        card.setOnClickListener(v -> {
            ProductDetailsFragment fragment = ProductDetailsFragment.newInstance(p.getId());
            FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);
            ft.replace(R.id.fragment_container, fragment, ProductDetailsFragment.TAG);
            ft.addToBackStack(ProductDetailsFragment.TAG);
            ft.commit();
        });

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(p.getName());
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextSize(16);
        // Ustawienie koloru tekstu (dostosuje się do trybu dnia/nocy)
        tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

        TextView tvSubtitle = new TextView(requireContext());
        String formattedPrice = priceFormat.format(p.getPrice()).replace('.', ',') + " zł"; // poprawiony format
        String subtitle = "Cena: " + formattedPrice;
        if (p.getExpirationDate() != null && !p.getExpirationDate().isEmpty()) subtitle += "  •  Ważność: " + p.getExpirationDate();
        tvSubtitle.setText(subtitle);
        tvSubtitle.setTextSize(13);
        tvSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        TextView tvExtra = new TextView(requireContext());
        String extra = (p.getCategory() == null ? "" : p.getCategory()) + (p.getStore()==null?"" : ("  •  " + p.getStore()));
        tvExtra.setText(extra.trim());
        tvExtra.setTextSize(12);
        tvExtra.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        inner.addView(tvTitle);
        inner.addView(tvSubtitle);
        inner.addView(tvExtra);

        card.addView(inner);
        listLayout.addView(card);
    }
}
