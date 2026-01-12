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
 *  - filtrowanie po statusie (przycisk w UI)
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
    private boolean statusFilterActive = false; // czy aktywne filtrowanie po statusie
    private boolean statusFilterActiveProducts = true; // czy pokazywać aktywne produkty
    private MaterialButton btnFilterStatus; // przycisk filtrowania statusu
    private EditText searchInput;
    private String currentQuery = "";
    private java.util.List<Product> allProducts = new java.util.ArrayList<>();
    private View searchContainer;
    private View btnSearchToggle;
    private View btnSearchClear;
    private View btnImport; // nowe pola
    private View btnExport;
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> importLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        importLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                android.net.Uri uri = result.getData().getData();
                if (uri != null) {
                    importFromUri(uri);
                } else {
                    toast(R.string.import_failed);
                }
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new ProductDbHelper(requireContext());
        recyclerView = view.findViewById(R.id.products_list);
        tvTotal = view.findViewById(R.id.products_total);
        tvExpiring = view.findViewById(R.id.products_expiring);
        viewModeToggle = view.findViewById(R.id.view_mode_toggle);
        searchContainer = view.findViewById(R.id.search_container); // przeniesione wyżej przed użyciem
        btnSearchToggle = view.findViewById(R.id.btn_search_toggle);
        btnSearchClear = view.findViewById(R.id.btn_search_clear);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_product);
        fab.setOnClickListener(v -> openAddProduct());
        dateFormat.setLenient(false);

        if (savedInstanceState != null) {
            isGrid = savedInstanceState.getBoolean(KEY_GRID, false);
            priceSortActive = savedInstanceState.getBoolean(KEY_SORT_ACTIVE, false);
            priceSortAscending = savedInstanceState.getBoolean(KEY_SORT_ASC, true);
            boolean vis = savedInstanceState.getBoolean(KEY_SEARCH_VISIBLE, false);
            if (vis && searchContainer != null && btnSearchToggle != null) {
                searchContainer.setVisibility(View.VISIBLE);
                btnSearchToggle.setVisibility(View.GONE);
            }
        }
        applyLayoutManager();
        adapter = new ProductAdapter(requireActivity(), new java.util.ArrayList<>());
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
        btnFilterStatus = view.findViewById(R.id.btn_filter_status);
        if (btnFilterStatus != null) {
            updateFilterButtonUi();
            btnFilterStatus.setOnClickListener(v -> {
                if (!statusFilterActive) {
                    statusFilterActive = true; // pierwsze kliknięcie – aktywuj filtr
                    statusFilterActiveProducts = true; // domyślnie pokazuj aktywne produkty
                }
                // Aktualizuj tekst przycisku
                if (statusFilterActive) {
                    statusFilterActiveProducts = !statusFilterActiveProducts; // przełącz co pokazywać
                    String statusText = getString(R.string.status_label);
                    btnFilterStatus.setText(statusText);
                }
                // Zastosuj filtr
                if (adapter != null) {
                    java.util.List<Product> filtered = new java.util.ArrayList<>();
                    for (Product p : allProducts) {
                        if (statusFilterActive) {
                            if (statusFilterActiveProducts && !p.isUsed()) {
                                filtered.add(p);
                            } else if (!statusFilterActiveProducts && p.isUsed()) {
                                filtered.add(p);
                            }
                        }
                    }
                    adapter.replaceData(filtered);
                    updateFilterButtonUi();
                    // Ponownie zastosuj sortowanie jeśli aktywne
                    if (priceSortActive) {
                        adapter.sortByPrice(priceSortAscending);
                        updateFilterButtonUi();
                    }
                }
            });
            // tego nie ruszamy
            btnFilterStatus.setOnLongClickListener(v -> {
                // Dodatkowo: długie przytrzymanie wyłącza sortowanie (opcjonalne ułatwienie)
                if (statusFilterActive) {
                    statusFilterActive = false;
                    statusFilterActiveProducts = true; // reset kierunku
                    loadProducts(); // załaduj w oryginalnej kolejności (po ID DESC)
                    updateFilterButtonUi();
                }
                return true;
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
        btnExport = view.findViewById(R.id.btn_export);
        btnImport = view.findViewById(R.id.btn_import);
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> showExportDialog());
        }
        if (btnImport != null) {
            btnImport.setOnClickListener(v -> startImportFlow());
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
        btnSortPrice.setText(R.string.price_label);
        if (!priceSortActive) {
            btnSortPrice.setIcon(null);
        } else {
            int iconRes = priceSortAscending ? R.drawable.ic_arrow_up_bold : R.drawable.ic_arrow_down_bold;
            btnSortPrice.setIcon(androidx.appcompat.content.res.AppCompatResources.getDrawable(requireContext(), iconRes));
        }
    }

    private void updateFilterButtonUi() {
        if (btnFilterStatus == null) return;
        btnFilterStatus.setText(R.string.status_label);
        if (!statusFilterActive) {
            btnFilterStatus.setIcon(null);
        } else {
            int iconRes = statusFilterActiveProducts ? R.drawable.ic_used : R.drawable.ic_close_24;
            btnFilterStatus.setIcon(androidx.appcompat.content.res.AppCompatResources.getDrawable(requireContext(), iconRes));
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

    private void showExportDialog() {
        if (!isAdded()) return;
        String[] formats = {getString(R.string.export_format_csv), getString(R.string.export_format_json)};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.export_choose_format_title)
                .setItems(formats, (d, which) -> {
                    if (which == 0) {
                        exportCsv();
                    } else {
                        exportJson();
                    }
                })
                .show();
    }

    private void exportCsv() {
        try {
            java.util.List<Product> products = dbHelper.getAllProducts();
            StringBuilder sb = new StringBuilder();
            sb.append("id,name,price,expiration_date,category,description,store,purchase_date,used\n");
            for (Product p : products) {
                sb.append(p.getId()).append(',')
                        .append(escapeCsv(p.getName())).append(',')
                        .append(p.getPrice()).append(',')
                        .append(escapeCsv(p.getExpirationDate())).append(',')
                        .append(escapeCsv(p.getCategory())).append(',')
                        .append(escapeCsv(p.getDescription())).append(',')
                        .append(escapeCsv(p.getStore())).append(',')
                        .append(escapeCsv(p.getPurchaseDate())).append(',')
                        .append(p.isUsed() ? 1 : 0)
                        .append('\n');
            }
            String fileName = "products_export_" + System.currentTimeMillis() + ".csv";
            writeSharedFile(fileName, sb.toString(), "text/csv");
            saveToDownloads(fileName, sb.toString(), "text/csv");
            toast(R.string.export_success);
        } catch (Exception e) {
            toast(R.string.export_failed);
        }
    }

    private void exportJson() {
        try {
            java.util.List<Product> products = dbHelper.getAllProducts();
            org.json.JSONArray arr = new org.json.JSONArray();
            for (Product p : products) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("id", p.getId());
                o.put("name", p.getName());
                o.put("price", p.getPrice());
                o.put("expiration_date", p.getExpirationDate());
                o.put("category", p.getCategory());
                o.put("description", p.getDescription());
                o.put("store", p.getStore());
                o.put("purchase_date", p.getPurchaseDate());
                o.put("used", p.isUsed());
                arr.put(o);
            }
            String jsonText = arr.toString(2);
            String fileName = "products_export_" + System.currentTimeMillis() + ".json";
            writeSharedFile(fileName, jsonText, "application/json");
            saveToDownloads(fileName, jsonText, "application/json");
            toast(R.string.export_success);
        } catch (Exception e) {
            toast(R.string.export_failed);
        }
    }

    private void saveToDownloads(String fileName, String content, String mime) {
        try {
            android.content.ContentResolver resolver = requireContext().getContentResolver();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.Downloads.MIME_TYPE, mime);
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 1);
                android.net.Uri collection = android.provider.MediaStore.Downloads.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY);
                android.net.Uri item = resolver.insert(collection, values);
                if (item == null) throw new IllegalStateException("Insert returned null");
                try (java.io.OutputStream os = resolver.openOutputStream(item)) {
                    if (os == null) throw new IllegalStateException("OutputStream null");
                    os.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                values.clear();
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(item, values, null, null);
                toast(R.string.export_saved_downloads);
            } else {
                java.io.File downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                if (!downloads.exists()) downloads.mkdirs();
                java.io.File out = new java.io.File(downloads, fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                    fos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                // Opcjonalne skanowanie, aby pojawiło się w menedżerze plików
                requireContext().sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, android.net.Uri.fromFile(out)));
                toast(R.string.export_saved_downloads);
            }
        } catch (Exception e) {
            toast(R.string.export_write_failed);
        }
    }

    private String escapeCsv(String v) {
        if (v == null) return "";
        String needsQuoteChars = ",\n\r\"";
        boolean needsQuotes = false;
        for (int i = 0; i < needsQuoteChars.length(); i++) {
            if (v.indexOf(needsQuoteChars.charAt(i)) >= 0) { needsQuotes = true; break; }
        }
        String out = v.replace("\"", "\"\"");
        return needsQuotes ? ('"' + out + '"') : out;
    }

    private void writeSharedFile(String fileName, String content, String mime) throws Exception {
        java.io.File cacheDir = requireContext().getCacheDir();
        java.io.File outFile = new java.io.File(cacheDir, fileName);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
            fos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", outFile);
        android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND);
        share.setType(mime);
        share.putExtra(android.content.Intent.EXTRA_STREAM, uri);
        share.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(android.content.Intent.createChooser(share, getString(R.string.export_choose_format_title)));
    }

    private void startImportFlow() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // poprawione z "*"
        String[] mimeTypes = new String[]{"text/csv", "application/json", "text/plain"};
        intent.putExtra(android.content.Intent.EXTRA_MIME_TYPES, mimeTypes);
        importLauncher.launch(intent);
    }

    private void importFromUri(android.net.Uri uri) {
        try {
            String name = queryDisplayName(uri);
            boolean isJson = name != null && name.toLowerCase().endsWith(".json");
            boolean isCsv = name != null && name.toLowerCase().endsWith(".csv");
            java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) { toast(R.string.import_failed); return; }
            byte[] bytes = readAll(is);
            String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            int imported = 0;
            if (isJson) {
                imported = importJson(text);
            } else if (isCsv) {
                imported = importCsv(text);
            } else {
                toast(R.string.import_invalid_format);
                return;
            }
            if (imported >= 0) {
                toast(getString(R.string.import_success, imported));
                loadProducts();
            } else {
                toast(R.string.import_failed);
            }
        } catch (Exception e) {
            toast(R.string.import_failed);
        }
    }

    private int importJson(String json) {
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            int count = 0;
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                String name = o.optString("name", "");
                double price = o.optDouble("price", 0);
                String exp = o.optString("expiration_date", "");
                String category = o.optString("category", "");
                String description = o.optString("description", "");
                String store = o.optString("store", "");
                String purchase = o.optString("purchase_date", "");
                boolean used = o.optBoolean("used", false);
                if (name.isEmpty() || price <= 0) continue;
                Product p = new Product(name, price, exp, category, description, store, purchase, used);
                long id = dbHelper.insertProduct(p);
                if (id > 0) count++;
            }
            return count;
        } catch (Exception e) { android.util.Log.e(TAG, "JSON import error", e); return -1; }
    }

    private int importCsv(String csv) {
        try {
            String[] lines = csv.split("\\r?\\n"); // poprawiony regex
            if (lines.length <= 1) return 0; // brak danych
            int count = 0;
            for (int i = 1; i < lines.length; i++) { // pomijamy nagłówek
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                java.util.List<String> cols = parseCsvLine(line);
                if (cols.size() < 9) continue;
                String name = cols.get(1);
                double price;
                try { price = Double.parseDouble(cols.get(2)); } catch (NumberFormatException e) { continue; }
                String exp = cols.get(3);
                String category = cols.get(4);
                String description = cols.get(5);
                String store = cols.get(6);
                String purchase = cols.get(7);
                boolean used = false;
                try { used = Integer.parseInt(cols.get(8)) == 1; } catch (NumberFormatException ignored) {}
                if (name.isEmpty() || price <= 0) continue;
                Product p = new Product(name, price, exp, category, description, store, purchase, used);
                long id = dbHelper.insertProduct(p);
                if (id > 0) count++;
            }
            return count;
        } catch (Exception e) { android.util.Log.e(TAG, "CSV import error", e); return -1; }
    }

    private java.util.List<String> parseCsvLine(String line) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { // escaped quote
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out;
    }

    private byte[] readAll(java.io.InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = is.read(buf)) != -1) {
            bos.write(buf, 0, r);
        }
        return bos.toByteArray();
    }

    private String queryDisplayName(android.net.Uri uri) {
        String name = null;
        android.content.ContentResolver cr = requireContext().getContentResolver();
        try (android.database.Cursor c = cr.query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                name = c.getString(0);
            }
        }
        return name;
    }

    private void toast(int resId) { if (isAdded()) android.widget.Toast.makeText(requireContext(), resId, android.widget.Toast.LENGTH_SHORT).show(); }
    private void toast(String msg) { if (isAdded()) android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show(); }
}
