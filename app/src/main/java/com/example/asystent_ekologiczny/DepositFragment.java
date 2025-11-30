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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DepositFragment extends Fragment {
    public static final String TAG = "DepositFragment";
    private DepositDbHelper dbHelper;
    private RecyclerView recyclerView;
    private DepositAdapter adapter;
    private FloatingActionButton fabAdd;
    private TextView tvTotalValue; // usuń użycie (pozostaw deklarację jeśli chcesz w przyszłości, ale nie będzie inicjalizowane)
    private View emptyView;
    private long pendingAddedId = -1; // ID nowo dodanego rekordu do animacji

    private MaterialButton btnSortDeposit; // przycisk sortowania
    private boolean depositSortActive = false; // flaga sortowania
    private boolean depositSortAscending = true; // flaga kierunku sortowania

    private MaterialButton btnFilterDepositType; // przycisk filtrowania
    private String depositFilterType = "ALL"; // typ filtra
    private boolean depositFilterActive = false; // flaga filtra

    private MaterialButton btnExportDeposits;
    private TextView tvMonthlySummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_deposit_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new DepositDbHelper(requireContext());
        recyclerView = view.findViewById(R.id.deposit_recycler);
        fabAdd = view.findViewById(R.id.fab_add_deposit);
        tvTotalValue = null; // suma przeniesiona do footera adaptera
        emptyView = view.findViewById(R.id.deposit_empty);
        adapter = new DepositAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        adapter.setListener(new DepositAdapter.Listener() {
            @Override public void onItemClick(DepositItem item) { openEdit(item.getId()); }
            @Override public void onItemLongClick(DepositItem item) { openEdit(item.getId()); }
            @Override public void onItemDelete(DepositItem item) { confirmDelete(item.getId()); }
            @Override public void onItemReturnedToggle(DepositItem item, boolean returned) {
                boolean ok = dbHelper.setReturned(item.getId(), returned);
                if (!ok) {
                    Toast.makeText(requireContext(), "Błąd zapisu", Toast.LENGTH_SHORT).show();
                }
                // Odśwież dane i suma aktywnych
                loadData();
            }
        });
        fabAdd.setOnClickListener(v -> openAdd());
        getParentFragmentManager().setFragmentResultListener("deposit_added", getViewLifecycleOwner(), (k,b)->{ long id=b.getLong("deposit_added_id",-1); if(id>0){ pendingAddedId = id; loadData(); }});
        getParentFragmentManager().setFragmentResultListener("deposit_updated", getViewLifecycleOwner(), (k,b)->{ long id=b.getLong("deposit_updated_id",-1); if(id>0){ loadData(); }});
        loadData(); // na końcu po zarejestrowaniu listenerów

        btnSortDeposit = view.findViewById(R.id.btn_sort_deposit);
        if (btnSortDeposit != null) {
            updateSortButtonUi();
            btnSortDeposit.setOnClickListener(v -> {
                if (!depositSortActive) {
                    depositSortActive = true; // pierwsze kliknięcie – aktywuj sortowanie rosnące
                    depositSortAscending = true;
                } else {
                    depositSortAscending = !depositSortAscending; // kolejne kliknięcia zmieniają kierunek
                }
                if (adapter != null) adapter.sortByDeposit(depositSortAscending);
                updateSortButtonUi();
            });
            btnSortDeposit.setOnLongClickListener(v -> {
                // Dodatkowo: długie przytrzymanie wyłącza sortowanie (opcjonalne ułatwienie)
                if (depositSortActive) {
                    depositSortActive = false;
                    depositSortAscending = true; // reset kierunku
                    loadData(); // załaduj w oryginalnej kolejności (po ID DESC)
                    updateSortButtonUi();
                }
                return true;
            });
        }

        // Obsługa filtrowania po typie opakowania: klik – wybór z listy; długie przytrzymanie – wyczyszczenie
        btnFilterDepositType = view.findViewById(R.id.btn_filter_deposit_type);
        if (btnFilterDepositType != null) {
            updateFilterButtonUi();
            btnFilterDepositType.setOnClickListener(v -> showTypeFilterDialog());
            btnFilterDepositType.setOnLongClickListener(v -> {
                if (depositFilterActive) {
                    depositFilterActive = false;
                    depositFilterType = "ALL";
                    loadData();
                    updateFilterButtonUi();
                }
                return true;
            });
        }

        btnExportDeposits = view.findViewById(R.id.btn_export_deposits);
        if (btnExportDeposits != null) {
            btnExportDeposits.setOnClickListener(v -> showExportDialog());
        }

        tvMonthlySummary = view.findViewById(R.id.deposit_monthly_summary);
        updateMonthlySummary();
    }


    private void loadData(){
        if(adapter==null) return;
        java.util.List<DepositItem> all = dbHelper.getAllDeposits();
        // Zastosuj filtr po typie, jeśli aktywny
        java.util.List<DepositItem> shown;
        if (depositFilterActive && depositFilterType != null && !"ALL".equals(depositFilterType)) {
            shown = new java.util.ArrayList<>();
            for (DepositItem d : all) {
                if (depositFilterType.equals(d.getCategory())) {
                    shown.add(d);
                }
            }
        } else {
            shown = all;
        }
        adapter.replaceData(shown);
        updateTotalAndEmpty(shown);
        updateMonthlySummary();
        if(pendingAddedId>0 && !shown.isEmpty() && shown.get(0).getId()==pendingAddedId){
            animateFirst();
            pendingAddedId=-1;
        } else if(pendingAddedId>0){ // jeśli kolejność inna spróbuj znaleźć pozycję
            int pos=-1;
            for(int i=0;i<shown.size();i++){ if(shown.get(i).getId()==pendingAddedId){ pos=i; break; } }
            final int targetPos = pos; // kopia final do lambdy
            if(targetPos>=0){
                recyclerView.post(() -> {
                    RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(targetPos);
                    if(vh!=null){
                        View v=vh.itemView;
                        v.setAlpha(0f);
                        v.setTranslationY(dp(24));
                        v.animate().alpha(1f).translationY(0f).setDuration(240).start();
                    }
                });
            }
            pendingAddedId=-1;
        }
    }

    private void updateTotalAndEmpty(java.util.List<DepositItem> list){
        double sum=0; for(DepositItem d:list){ if(!d.isReturned()) sum+=d.getValue(); }
        if(adapter!=null){ adapter.setTotalValue(sum); }
        if(emptyView!=null){ emptyView.setVisibility(list.isEmpty()? View.VISIBLE: View.GONE); }
    }
    private void animateFirst(){ recyclerView.post(() -> { RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(0); if(vh==null){ recyclerView.postDelayed(this::animateFirst,32); return;} View v=vh.itemView; v.setAlpha(0f); v.setTranslationY(dp(36)); v.animate().alpha(1f).translationY(0f).setDuration(260).start(); }); }
    private int dp(int d){ return (int)(getResources().getDisplayMetrics().density*d); }

    private void openAdd(){ AddDepositFragment f = new AddDepositFragment(); requireActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right).replace(R.id.fragment_container, f, AddDepositFragment.TAG).addToBackStack(AddDepositFragment.TAG).commit(); }
    private void openEdit(long id){ AddDepositFragment f = AddDepositFragment.newEditInstance(id); requireActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right).replace(R.id.fragment_container, f, AddDepositFragment.TAG).addToBackStack(AddDepositFragment.TAG).commit(); }
    private void confirmDelete(long id){ new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Usuń opakowanie")
            .setMessage("Czy na pewno usunąć?")
            .setPositiveButton("Usuń", (d,w)->{ if(dbHelper.deleteDeposit(id)){ Toast.makeText(requireContext(),"Usunięto",Toast.LENGTH_SHORT).show(); loadData(); } else Toast.makeText(requireContext(),"Błąd",Toast.LENGTH_SHORT).show(); })
            .setNegativeButton("Anuluj", null)
            .show(); }

    @Override public void onDestroyView(){ super.onDestroyView(); if(dbHelper!=null){ dbHelper.close(); dbHelper=null; } }

    private void updateSortButtonUi() {
        if (btnSortDeposit == null) return;
        btnSortDeposit.setText(R.string.deposit_sort);
        if (!depositSortActive) {
            btnSortDeposit.setIcon(null);
        } else {
            int iconRes = depositSortAscending ? R.drawable.ic_arrow_up_bold : R.drawable.ic_arrow_down_bold;
            btnSortDeposit.setIcon(androidx.appcompat.content.res.AppCompatResources.getDrawable(requireContext(), iconRes));
        }
    }

    /** Aktualizacja UI przycisku filtra typu. */
    private void updateFilterButtonUi() {
        if (btnFilterDepositType == null) return;
        if (!depositFilterActive || depositFilterType == null || "ALL".equals(depositFilterType)) {
            btnFilterDepositType.setText(R.string.deposit_type);
            btnFilterDepositType.setIcon(null);
        } else {
            btnFilterDepositType.setText(depositFilterType);
            // Dodaj prostą ikonę filtra, jeśli istnieje w zasobach, inaczej pozostaw null
            // btnFilterDepositType.setIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_filter));
        }
    }

    /** Wyświetla dialog z listą typów opakowań do wyboru. */
    private void showTypeFilterDialog() {
        java.util.List<DepositItem> all = dbHelper.getAllDeposits();
        java.util.LinkedHashSet<String> typesSet = new java.util.LinkedHashSet<>();
        for (DepositItem d : all) {
            if (d.getCategory() != null && !d.getCategory().isEmpty()) typesSet.add(d.getCategory());
        }
        if (typesSet.isEmpty()) {
            Toast.makeText(requireContext(), "Brak typów do filtrowania", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] types = typesSet.toArray(new String[0]);
        int checked = -1;
        if (depositFilterActive && depositFilterType != null) {
            for (int i = 0; i < types.length; i++) {
                if (types[i].equals(depositFilterType)) { checked = i; break; }
            }
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.deposit_type)
                .setSingleChoiceItems(types, checked, (dialog, which) -> {
                    depositFilterType = types[which];
                    depositFilterActive = true;
                })
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    loadData();
                    updateFilterButtonUi();
                })
                .setNegativeButton(R.string.back, (dialog, which) -> {})
                .show();
    }

    private void showExportDialog() {
        if (!isAdded()) return;
        String[] formats = {getString(R.string.export_format_csv), "PDF"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.export_choose_format_title)
                .setItems(formats, (d, which) -> {
                    if (which == 0) exportCsv(); else exportPdf();
                })
                .show();
    }

    private void exportCsv() {
        try {
            java.util.List<DepositItem> deposits = dbHelper.getAllDeposits();
            StringBuilder sb = new StringBuilder();
            sb.append("id,type,name,value,barcode,returned\n");
            for (DepositItem di : deposits) {
                sb.append(di.getId()).append(',')
                        .append(escapeCsv(di.getCategory())).append(',')
                        .append(escapeCsv(di.getName())).append(',')
                        .append(di.getValue()).append(',')
                        .append(escapeCsv(di.getBarcode())).append(',')
                        .append(di.isReturned() ? 1 : 0)
                        .append('\n');
            }
            String fileName = "deposits_export_" + System.currentTimeMillis() + ".csv";
            writeSharedFile(fileName, sb.toString(), "text/csv");
            saveToDownloads(fileName, sb.toString(), "text/csv");
            Toast.makeText(requireContext(), R.string.export_success, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void exportPdf() {
        try {
            java.util.List<DepositItem> deposits = dbHelper.getAllDeposits();
            android.graphics.pdf.PdfDocument doc = new android.graphics.pdf.PdfDocument();
            android.graphics.Paint paint = new android.graphics.Paint();
            android.graphics.Paint bold = new android.graphics.Paint();
            bold.setFakeBoldText(true);
            int pageWidth = 595; // A4 width (pt)
            int pageHeight = 842; // A4 height (pt)
            int margin = 24;
            int y = margin + 16;

            android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            android.graphics.pdf.PdfDocument.Page page = doc.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();

            // Tytuł
            paint.setTextSize(14);
            bold.setTextSize(14);
            canvas.drawText("Lista opakowań kaucyjnych", margin, y, bold);
            y += 24;
            paint.setTextSize(12);

            // Nagłówki
            canvas.drawText("Typ", margin, y, bold);
            canvas.drawText("Nazwa", margin + 140, y, bold);
            canvas.drawText("Kaucja", margin + 340, y, bold);
            canvas.drawText("Zwr.", margin + 420, y, bold);
            y += 18;

            for (DepositItem di : deposits) {
                if (y > pageHeight - margin) {
                    doc.finishPage(page); // zakończ bieżącą stronę przed utworzeniem nowej
                    pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, doc.getPages().size()+1).create();
                    page = doc.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = margin;
                }
                canvas.drawText(di.getCategory() != null ? di.getCategory() : "", margin, y, paint);
                canvas.drawText(di.getName() != null ? di.getName() : "", margin + 140, y, paint);
                canvas.drawText(String.format(java.util.Locale.getDefault(), "%.2f zł", di.getValue()), margin + 340, y, paint);
                canvas.drawText(di.isReturned() ? "TAK" : "NIE", margin + 420, y, paint);
                y += 16;
            }

            // Zakończ ostatnią stronę
            doc.finishPage(page);

            String fileName = "deposits_export_" + System.currentTimeMillis() + ".pdf";
            java.io.File cacheDir = requireContext().getCacheDir();
            java.io.File outFile = new java.io.File(cacheDir, fileName);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                doc.writeTo(fos);
            }
            doc.close();

            // Udostępnianie
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", outFile);
            android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            share.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(android.content.Intent.createChooser(share, getString(R.string.export_choose_format_title)));

            // Zapis do Pobrane – użyj bajtów
            saveToDownloadsBytes(fileName, readFileBytes(outFile), "application/pdf");
            Toast.makeText(requireContext(), R.string.export_success, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.export_failed, Toast.LENGTH_SHORT).show();
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
        return needsQuotes ? ('\"' + out + '\"') : out;
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
                Toast.makeText(requireContext(), R.string.export_saved_downloads, Toast.LENGTH_SHORT).show();
            } else {
                java.io.File downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                if (!downloads.exists()) downloads.mkdirs();
                java.io.File out = new java.io.File(downloads, fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                    fos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                requireContext().sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, android.net.Uri.fromFile(out)));
                Toast.makeText(requireContext(), R.string.export_saved_downloads, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.export_write_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToDownloadsBytes(String fileName, byte[] content, String mime) {
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
                    os.write(content);
                }
                values.clear();
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(item, values, null, null);
                Toast.makeText(requireContext(), R.string.export_saved_downloads, Toast.LENGTH_SHORT).show();
            } else {
                java.io.File downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                if (!downloads.exists()) downloads.mkdirs();
                java.io.File out = new java.io.File(downloads, fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                    fos.write(content);
                }
                requireContext().sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, android.net.Uri.fromFile(out)));
                Toast.makeText(requireContext(), R.string.export_saved_downloads, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.export_write_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] readFileBytes(java.io.File f) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.InputStream is = new java.io.FileInputStream(f)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
        }
        return baos.toByteArray();
    }

    private void updateMonthlySummary() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        int month01 = cal.get(java.util.Calendar.MONTH) + 1;
        int count = dbHelper.countReturnedInMonth(year, month01);
        if (tvMonthlySummary != null) {
            tvMonthlySummary.setText("Zwrócone w tym miesiącu: " + count);
        }
    }
}
