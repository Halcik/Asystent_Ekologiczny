package com.example.asystent_ekologiczny;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment listy produktów.
 * Funkcje:
 *  - przełączanie układu lista/siatka
 *  - sortowanie po cenie (przycisk w UI)
 *  - wyszukiwarka nazw (animowane rozwijanie)
 *  - zliczanie produktów oraz tych z krótkim terminem (<=3 dni)
 *  - szybkie dodanie nowego produktu z animacją (wynik setFragmentResult)
 */
public class ProductsFragment extends Fragment {
    public static final String TAG = "ProductsFragment";
    private static final String KEY_GRID = "grid_mode";
    private static final String KEY_SORT_ACTIVE = "sort_active";
    private static final String KEY_SORT_ASC = "sort_asc";
    private static final String KEY_SEARCH_VISIBLE = "search_visible";

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
    private boolean priceSortActive = false; // czy aktywne sortowanie po cenie
    private boolean priceSortAscending = true; // kierunek sortowania
    private MaterialButton btnSortPrice; // przycisk sortowania
    private EditText searchInput;
    private String currentQuery = "";
    private java.util.List<Product> allProducts = new java.util.ArrayList<>();
    private View searchContainer;
    private View btnSearchToggle;
    private View btnSearchClear;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // usunięto setHasOptionsMenu – sortowanie przeniesione do przycisku w layoucie
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
            priceSortActive = savedInstanceState.getBoolean(KEY_SORT_ACTIVE, false);
            priceSortAscending = savedInstanceState.getBoolean(KEY_SORT_ASC, true);
            boolean vis = savedInstanceState.getBoolean(KEY_SEARCH_VISIBLE, false);
            if (vis) {
                // pokaż bez animacji i przywróć zapytanie
                searchContainer.setVisibility(View.VISIBLE);
                btnSearchToggle.setVisibility(View.GONE);
            }
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
                        // usunięto invalidateOptionsMenu
                    }
                }
            });
        }
        btnSortPrice = view.findViewById(R.id.btn_sort_price);
        if (btnSortPrice != null) {
            updateSortButtonUi();
            btnSortPrice.setOnClickListener(v -> {
                if (!priceSortActive) {
                    priceSortActive = true; // pierwsze kliknięcie – aktywuj sortowanie rosnące
                    priceSortAscending = true;
                } else {
                    priceSortAscending = !priceSortAscending; // kolejne kliknięcia zmieniają kierunek
                }
                if (adapter != null) adapter.sortByPrice(priceSortAscending);
                updateSortButtonUi();
            });
            btnSortPrice.setOnLongClickListener(v -> {
                // Dodatkowo: długie przytrzymanie wyłącza sortowanie (opcjonalne ułatwienie)
                if (priceSortActive) {
                    priceSortActive = false;
                    priceSortAscending = true; // reset kierunku
                    loadProducts(); // załaduj w oryginalnej kolejności (po ID DESC)
                    updateSortButtonUi();
                }
                return true;
            });
        }
        searchInput = view.findViewById(R.id.search_input);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = s == null ? "" : s.toString().trim();
                    applyFilterAndDisplay();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        btnSearchToggle = view.findViewById(R.id.btn_search_toggle);
        searchContainer = view.findViewById(R.id.search_container);
        btnSearchClear = view.findViewById(R.id.btn_search_clear);
        if (savedInstanceState != null) {
            boolean vis = savedInstanceState.getBoolean(KEY_SEARCH_VISIBLE, false);
            if (vis) {
                // pokaż bez animacji i przywróć zapytanie
                searchContainer.setVisibility(View.VISIBLE);
                btnSearchToggle.setVisibility(View.GONE);
            }
        }
        if (btnSearchToggle != null) {
            btnSearchToggle.setOnClickListener(v -> expandSearch(true));
        }
        if (btnSearchClear != null) {
            btnSearchClear.setOnClickListener(v -> {
                if (searchInput != null) {
                    searchInput.setText("");
                }
                collapseSearch(true);
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
        // Listener na wynik edycji – pełne przeładowanie aby uwzględnić zmiany w sortowaniu/filtrze/licznikach
        getParentFragmentManager().setFragmentResultListener("product_updated", getViewLifecycleOwner(), (key, bundle) -> {
            long updatedId = bundle.getLong("updatedProductId", -1);
            if (updatedId != -1) {
                loadProducts();
            }
        });
        getParentFragmentManager().setFragmentResultListener("product_deleted", getViewLifecycleOwner(), (key, bundle) -> {
            long deletedId = bundle.getLong("deletedProductId", -1);
            if (deletedId != -1) {
                loadProducts();
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
        outState.putBoolean(KEY_SORT_ACTIVE, priceSortActive);
        outState.putBoolean(KEY_SORT_ASC, priceSortAscending);
        outState.putBoolean(KEY_SEARCH_VISIBLE, searchContainer != null && searchContainer.getVisibility() == View.VISIBLE);
    }

    private void openAddProduct() {
        AddProductFragment fragment = new AddProductFragment();
        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.fragment_container, fragment, AddProductFragment.TAG)
                .addToBackStack(AddProductFragment.TAG)
                .commit();
    }

    /**
     * Wczytuje dane z DB, aktualizuje liczniki i stosuje filtr.
     */
    private void loadProducts() {
        if (adapter == null) return;
        java.util.List<Product> products = dbHelper.getAllProducts();
        allProducts.clear();
        allProducts.addAll(products);
        int newExpiring = 0;
        Date today = stripTime(new Date());
        for (Product p : products) {
            if (p.isUsed()) continue; // zużyte nie liczą się do wygasających
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
        // Nie ustawiamy danych bezpośrednio – filtr zastosuje właściwą listę
        totalCount = allProducts.size();
        expiringCount = newExpiring;
        updateCounters();
        applyFilterAndDisplay();
    }

    /**
     * Zastosowanie filtra nazwy + opcjonalne sortowanie.
     */
    private void applyFilterAndDisplay() {
        if (adapter == null) return;
        String q = currentQuery.toLowerCase(java.util.Locale.getDefault());
        java.util.List<Product> filtered;
        if (q.isEmpty()) {
            filtered = new java.util.ArrayList<>(allProducts);
        } else {
            filtered = new java.util.ArrayList<>();
            for (Product p : allProducts) {
                if (p.getName() != null && p.getName().toLowerCase(java.util.Locale.getDefault()).contains(q)) {
                    filtered.add(p);
                }
            }
        }
        adapter.replaceData(filtered);
        if (priceSortActive) {
            adapter.sortByPrice(priceSortAscending);
        }
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

    /**
     * Wstawia nowy produkt (animacja jeśli brak filtra i sortowania).
     */
    public void addNewProduct(long id) {
        if (!isAdded()) return;
        Product p = dbHelper.getProductById(id);
        if (p == null || adapter == null) return;
        // Dodaj do pełnej listy źródłowej
        allProducts.add(0, p); // na początku listy źródłowej (bo domyślny porządek to ID DESC)
        totalCount += 1;
        if (p.getExpirationDate() != null && !p.getExpirationDate().isEmpty() && !p.isUsed()) {
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

        boolean canAnimateDirect = currentQuery.isEmpty() && !priceSortActive;
        if (canAnimateDirect) {
            // bez filtra i sortowania – szybka ścieżka z animacją
            adapter.addProductAtTop(p);
            animateFirstItem();
        } else {
            // w innym wypadku – przefiltruj / posortuj ponownie
            applyFilterAndDisplay();
        }
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

    /**
     * Aktualizacja tekstowych liczników nagłówkowych.
     */
    private void updateCounters() {
        if (!isAdded()) return;
        tvTotal.setText(String.valueOf(totalCount));
        tvExpiring.setText(String.valueOf(expiringCount));
    }

    /**
     * Aktualizacja ikon / stanu przycisku sortowania.
     */
    private void updateSortButtonUi() {
        if (btnSortPrice == null) return;
        btnSortPrice.setText("Cena");
        if (!priceSortActive) {
            btnSortPrice.setIcon(null);
        } else {
            int iconRes = priceSortAscending ? R.drawable.ic_arrow_up_bold : R.drawable.ic_arrow_down_bold;
            btnSortPrice.setIcon(requireContext().getDrawable(iconRes));
        }
    }

    private void expandSearch(boolean animate) {
        if (searchContainer == null || btnSearchToggle == null) return;
        if (searchContainer.getVisibility() == View.VISIBLE) return;
        if (searchContainer.getVisibility() == View.VISIBLE) return;
        searchContainer.setVisibility(View.VISIBLE);
        btnSearchToggle.setVisibility(View.GONE);
        if (animate) {
            searchContainer.setAlpha(0f);
            searchContainer.setScaleX(0.85f);
            searchContainer.setScaleY(0.85f);
            searchContainer.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
        if (searchInput != null) {
            searchInput.requestFocus();
        }
    }

    private void collapseSearch(boolean animate) {
        if (searchContainer == null || btnSearchToggle == null) return;
        if (searchContainer.getVisibility() != View.VISIBLE) return;
        if (animate) {
            searchContainer.animate()
                    .alpha(0f)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(140)
                    .withEndAction(() -> {
                        searchContainer.setVisibility(View.GONE);
                        searchContainer.setAlpha(1f);
                        searchContainer.setScaleX(1f);
                        searchContainer.setScaleY(1f);
                        btnSearchToggle.setVisibility(View.VISIBLE);
                    })
                    .start();
        } else {
            searchContainer.setVisibility(View.GONE);
            btnSearchToggle.setVisibility(View.VISIBLE);
        }
    }
}
