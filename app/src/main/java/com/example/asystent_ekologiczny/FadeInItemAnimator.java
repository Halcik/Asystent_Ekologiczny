package com.example.asystent_ekologiczny;

import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorListener;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

/**
 * FadeInItemAnimator – prosty animator do RecyclerView, który dodaje efekt płynnego
 * pojawiania się (fade-in) NOWO DODAWANYCH elementów listy.
 *
 * Użycie:
 *    recyclerView.setItemAnimator(new FadeInItemAnimator());
 *
 * Co animuje: TYLKO operacje dodania elementu (animateAdd). Pozostałe animacje
 * (usuwanie, zmiana, przenoszenie) pozostają takie jak w DefaultItemAnimator.
 *
 * Dlaczego własna klasa zamiast gotowych bibliotek: kod jest krótki, czytelny
 * i eliminuje nadmiarowe zależności. Łatwo można dopisać np. skalowanie czy
 * przesunięcie.
 */
public class FadeInItemAnimator extends DefaultItemAnimator {

    /**
     * Konstruktor – ustawiamy czas trwania animacji dodawania.
     * Możesz zmienić wartość (ms) dopasowując prędkość pojawiania.
     */
    public FadeInItemAnimator() {
        setAddDuration(200); // czas animacji dodania w milisekundach
    }

    /**
     * animateAdd – wywoływane, gdy RecyclerView dodaje nowy ViewHolder.
     * Kroki:
     *  1. Ustawiamy alpha = 0 (start – element niewidoczny).
     *  2. Zgłaszamy rozpoczęcie animacji do systemu (dispatchAddStarting).
     *  3. Animujemy przejście alpha -> 1.
     *  4. Na końcu zgłaszamy zakończenie animacji (dispatchAddFinished).
     */
    @Override
    public boolean animateAdd(final RecyclerView.ViewHolder holder) {
        holder.itemView.setAlpha(0f); // startowa przezroczystość
        dispatchAddStarting(holder); // powiadomienie RecyclerView o starcie animacji dodania

        ViewCompat.animate(holder.itemView)
                .alpha(1f) // docelowa pełna widoczność
                .setDuration(getAddDuration())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override public void onAnimationStart(View view) { /* brak dodatkowych działań */ }
                    @Override public void onAnimationEnd(View view) {
                        view.setAlpha(1f); // upewniamy się, że końcowa alpha = 1
                        dispatchAddFinished(holder); // informujemy, że animacja się zakończyła
                    }
                    @Override public void onAnimationCancel(View view) {
                        // Jeśli animacja zostanie przerwana – przywracamy pełną widoczność.
                        view.setAlpha(1f);
                    }
                }).start();
        return true; // informujemy, że obsłużyliśmy animację sami
    }

    /**
     * endAnimation – gdy system chce natychmiast zakończyć animację
     * (np. szybka zmiana danych), anulujemy bieżącą i przywracamy właściwy stan.
     */
    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        item.itemView.animate().cancel(); // zatrzymanie ewentualnej animacji
        item.itemView.setAlpha(1f);       // przywrócenie pełnej widoczności
        super.endAnimation(item);         // reszta logiki bazowej klasy
    }
}
