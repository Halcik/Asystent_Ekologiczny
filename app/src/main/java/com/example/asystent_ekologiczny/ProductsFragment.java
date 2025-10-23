package com.example.asystent_ekologiczny;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView; // import dolnej nawigacji

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductsFragment extends Fragment {
    public static final String TAG = "ProductsFragment";
    private static final String KEY_GRID = "grid_mode";

    private ProductDbHelper dbHelper;
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private TextView tvTotal;
    private TextView tvExpiring;
    private MaterialButtonToggleGroup viewModeToggle;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private boolean isGrid = false;
    private int bottomExtraPaddingPx = 0; // dynamiczny padding

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new ProductDbHelper(requireContext());
        recyclerView = view.findViewById(R.id.products_list);
        tvTotal = view.findViewById(R.id.products_total);
        tvExpiring = view.findViewById(R.id.products_expiring);
        viewModeToggle = view.findViewById(R.id.view_mode_toggle);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_product);
        fab.setOnClickListener(v -> openAddProduct());
        dateFormat.setLenient(false);

        if (savedInstanceState != null) {
            isGrid = savedInstanceState.getBoolean(KEY_GRID, false);
        }
        applyLayoutManager();
        adapter = new ProductAdapter((FragmentActivity) requireActivity(), new java.util.ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Ustaw dolny padding po zmierzeniu BottomNavigationView
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.post(() -> {
                int navHeight = bottomNav.getHeight();
                // dodatkowy bufor 8dp
                bottomExtraPaddingPx = (int) (getResources().getDisplayMetrics().density * 8) + navHeight;
                applyBottomPadding();
            });
        }

        if (viewModeToggle != null) {
            viewModeToggle.check(isGrid ? R.id.btn_view_grid : R.id.btn_view_list);
            viewModeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    boolean newGrid = checkedId == R.id.btn_view_grid;
                    if (newGrid != isGrid) {
                        isGrid = newGrid;
                        applyLayoutManager();
                        requireActivity().invalidateOptionsMenu();
                    }
                }
            });
        }
        loadProducts();
    }

    @Override
    public void onResume() {
        super.onResume();
        applyBottomPadding(); // upewnij się że padding nie zniknął
        loadProducts();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_GRID, isGrid);
    }

    private void openAddProduct() {
        AddProductFragment fragment = new AddProductFragment();
        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.fragment_container, fragment, AddProductFragment.TAG)
                .addToBackStack(AddProductFragment.TAG)
                .commit();
    }

    private void loadProducts() {
        if (adapter == null) return;
        List<Product> products = dbHelper.getAllProducts();
        int expiringCount = 0;
        Date today = stripTime(new Date());
        for (Product p : products) {
            if (p.getExpirationDate() != null && !p.getExpirationDate().isEmpty()) {
                try {
                    Date exp = dateFormat.parse(p.getExpirationDate());
                    if (exp != null) {
                        long days = (exp.getTime() - today.getTime()) / (1000L*60*60*24);
                        if (days <= 3) expiringCount++;
                    }
                } catch (ParseException ignored) {}
            }
        }
        adapter.replaceData(products);
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

    private void applyLayoutManager() {
        if (recyclerView == null) return;
        RecyclerView.LayoutManager lm = isGrid ? new GridLayoutManager(getContext(), 2) : new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(lm);
        refreshItemDecoration();
    }

    private void refreshItemDecoration() {
        if (recyclerView == null) return;
        while (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecorationAt(0);
        }
        int spacingPx = (int) (getResources().getDisplayMetrics().density * 8); // zmniejszone odstępy 8dp
        recyclerView.addItemDecoration(new ProductSpacingDecoration(spacingPx, isGrid, isGrid ? 2 : 1));
    }

    private void applyBottomPadding() {
        if (recyclerView == null) return;
        // Zachowaj istniejące górne/stronne paddingi, zmień tylko dolny
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(), recyclerView.getPaddingRight(), bottomExtraPaddingPx);
    }

    public void toggleLayout() {
        isGrid = !isGrid;
        applyLayoutManager();
        if (viewModeToggle != null) {
            viewModeToggle.check(isGrid ? R.id.btn_view_grid : R.id.btn_view_list);
        }
        requireActivity().invalidateOptionsMenu();
    }

    public boolean isGridMode() { return isGrid; }
}
