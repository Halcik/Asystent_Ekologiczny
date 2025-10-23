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
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
    private int bottomExtraPaddingPx = 0;
    private boolean suppressNextFullReload = false; // flaga pomijająca pełne odświeżenie po dodaniu
    private int totalCount = 0; // nowe liczniki
    private int expiringCount = 0;

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
        adapter.setGridMode(isGrid); // ustawienia paddings
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new FadeInItemAnimator()); // animator fade-in dla dodawania

        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.post(() -> {
                int navHeight = bottomNav.getHeight();
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
                        adapter.setGridMode(isGrid);
                        requireActivity().invalidateOptionsMenu();
                    }
                }
            });
        }
        loadProducts();

        // Listener na wynik dodania produktu – używamy viewLifecycleOwner aby uniknąć wywołań po zniszczeniu widoku
        getParentFragmentManager().setFragmentResultListener("product_added", getViewLifecycleOwner(), (key, bundle) -> {
            long newId = bundle.getLong("newProductId", -1);
            if (newId != -1) {
                addNewProduct(newId); // fade-in
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        applyBottomPadding();
        if (suppressNextFullReload) {
            suppressNextFullReload = false; // pomijamy jednorazowo
        } else {
            loadProducts();
        }
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
        int newExpiring = 0;
        Date today = stripTime(new Date());
        for (Product p : products) {
            if (p.getExpirationDate() != null && !p.getExpirationDate().isEmpty()) {
                try {
                    Date exp = dateFormat.parse(p.getExpirationDate());
                    if (exp != null) {
                        long days = (exp.getTime() - today.getTime()) / (1000L*60*60*24);
                        if (days <= 3) newExpiring++;
                    }
                } catch (ParseException ignored) {}
            }
        }
        adapter.replaceData(products);
        totalCount = products.size();
        expiringCount = newExpiring;
        updateCounters();
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
        int spacingPx = (int) (getResources().getDisplayMetrics().density * 8);
        recyclerView.addItemDecoration(new ProductSpacingDecoration(spacingPx, isGrid, isGrid ? 2 : 1));
    }

    private void applyBottomPadding() {
        if (recyclerView == null) return;
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(), recyclerView.getPaddingRight(), bottomExtraPaddingPx);
    }

    // NOWA METODA: wstawia pojedynczy produkt z animacją
    public void addNewProduct(long id) {
        if (!isAdded()) return;
        Product p = dbHelper.getProductById(id);
        if (p == null || adapter == null) return;
        adapter.addProductAtTop(p);
        totalCount += 1;
        if (p.getExpirationDate() != null && !p.getExpirationDate().isEmpty()) {
            try {
                Date exp = dateFormat.parse(p.getExpirationDate());
                Date today = stripTime(new Date());
                if (exp != null) {
                    long days = (exp.getTime() - today.getTime()) / (1000L*60*60*24);
                    if (days <= 3) {
                        expiringCount += 1;
                    }
                }
            } catch (Exception ignored) {}
        }
        updateCounters();
        suppressNextFullReload = true;
        animateFirstItem(); // ręczna animacja
    }

    private void animateFirstItem() {
        if (recyclerView == null) return;
        recyclerView.post(() -> {
            if (recyclerView == null) return;
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(0);
            if (vh == null) {
                // Spróbuj po krótkim opóźnieniu jeśli jeszcze nie zlayoutowane
                recyclerView.postDelayed(this::animateFirstItem, 32);
                return;
            }
            View v = vh.itemView;
            v.setAlpha(0f);
            v.setTranslationY(dp(36));
            v.setScaleX(0.95f);
            v.setScaleY(0.95f);
            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(280)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        });
    }
    private int dp(int d) { return (int)(getResources().getDisplayMetrics().density * d); }

    private void updateCounters() {
        if (!isAdded()) return;
        tvTotal.setText(String.valueOf(totalCount));
        tvExpiring.setText(String.valueOf(expiringCount));
    }
}
