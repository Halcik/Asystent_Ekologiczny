package com.example.asystent_ekologiczny;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ProductSpacingDecoration – dekoracja RecyclerView dodająca odstępy między
 * elementami listy (linear) lub siatki (grid).
 *
 * Użycie (lista):
 *   recyclerView.addItemDecoration(new ProductSpacingDecoration(spacingPx, false, 1));
 * Użycie (siatka 2 kolumny):
 *   recyclerView.addItemDecoration(new ProductSpacingDecoration(spacingPx, true, 2));
 *
 * Parametry:
 *  spacing   – żądany odstęp w px (bazowa jednostka).
 *  grid      – tryb siatki (true) lub listy (false).
 *  spanCount – liczba kolumn siatki (ignorowane dla listy).
 */
public class ProductSpacingDecoration extends RecyclerView.ItemDecoration {

    private final int spacing; // bazowy odstęp w pikselach
    private final boolean grid; // czy pracujemy w trybie siatki
    private final int spanCount; // liczba kolumn w siatce

    public ProductSpacingDecoration(int spacingPx, boolean gridMode, int spanCount) {
        this.spacing = spacingPx;
        this.grid = gridMode;
        this.spanCount = spanCount;
    }

    /**
     * getItemOffsets – system wywołuje dla KAŻDEGO widoku, aby dowiedzieć się
     * ile miejsca (Rect) ma zostać przeznaczone na marginesy wokół elementu.
     */
    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return; // brak pozycji => brak zmian

        if (!grid) { // TRYB LISTY (LinearLayoutManager)
            // Po bokach brak odstępu – można dodać jeśli potrzeba wizualnego marginesu.
            outRect.left = 0;
            outRect.right = 0;
            // Odstęp nad elementem (oprócz pierwszego).
            outRect.top = position == 0 ? 0 : spacing;

            // Dla ostatniego elementu dodaj niewielki dolny margines aby nie kleił się.
            boolean last = position == (parent.getAdapter() == null ? 0 : parent.getAdapter().getItemCount() - 1);
            outRect.bottom = last ? spacing / 2 : 0; // dolny mniejszy niż górny dla lekkości
        } else { // TRYB SIATKI (GridLayoutManager)
            // Obliczenie kolumny (0..spanCount-1).
            int column = position % spanCount;

            // Formuła równomiernego rozkładu:
            //  outRect.left i right wyliczane tak, aby suma marginesów między elementami
            //  była stała i wizualnie wyrównana.
            outRect.left = spacing - column * spacing / spanCount;
            outRect.right = (column + 1) * spacing / spanCount;

            // Pierwszy rząd bez górnego odstępu (można zmienić jeśli potrzeba).
            outRect.top = position < spanCount ? 0 : spacing;
            // Dolny lekko zmniejszony – zwykle wystarcza połowa.
            outRect.bottom = spacing / 2;
        }
    }
}
