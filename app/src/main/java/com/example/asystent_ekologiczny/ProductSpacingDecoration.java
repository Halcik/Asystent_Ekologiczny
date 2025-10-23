package com.example.asystent_ekologiczny;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Dekoracja ustawiająca odstępy między elementami listy / siatki.
 * Dla listy: tylko odstęp nad (poza pierwszym) i mały odstęp pod ostatnim.
 * Dla siatki: symetryczne odstępy z każdej strony; kolumny rozdzielone równo.
 */
public class ProductSpacingDecoration extends RecyclerView.ItemDecoration {

    private final int spacing; // w px
    private final boolean grid;
    private final int spanCount;

    public ProductSpacingDecoration(int spacingPx, boolean gridMode, int spanCount) {
        this.spacing = spacingPx;
        this.grid = gridMode;
        this.spanCount = spanCount;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return;

        if (!grid) { // LISTA
            outRect.left = 0;
            outRect.right = 0;
            outRect.top = position == 0 ? 0 : spacing; // odstęp między elementami
            // ostatni element: trochę dołu aby nie kleił się do końca
            boolean last = position == (parent.getAdapter() == null ? 0 : parent.getAdapter().getItemCount() - 1);
            outRect.bottom = last ? spacing / 2 : 0; // mniejszy dolny
        } else { // SIATKA
            // równy rozkład poziomy na podstawie kolumny
            int column = position % spanCount; // kolumna 0..spanCount-1
            // Formuła na równomierne rozłożenie przestrzeni między kolumnami
            outRect.left = spacing - column * spacing / spanCount;
            outRect.right = (column + 1) * spacing / spanCount;
            // top tylko dla kolejnych rzędów (pierwszy rząd bez dodatkowego odstępu od góry)
            outRect.top = position < spanCount ? 0 : spacing;
            outRect.bottom = spacing / 2; // nieco mniejszy dolny
        }
    }
}
