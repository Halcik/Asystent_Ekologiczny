package com.example.asystent_ekologiczny;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Slide-in z dołu dla dodawanych elementów RecyclerView (bez fade-in).
 */
public class FadeInItemAnimator extends DefaultItemAnimator {

    public FadeInItemAnimator() { setAddDuration(220); }

    @Override
    public boolean animateAdd(final RecyclerView.ViewHolder holder) {
        final View item = holder.itemView;
        // Startowe przesunięcie w dół
        float dy = item.getResources().getDisplayMetrics().density * 24f;
        item.setTranslationY(dy);
        dispatchAddStarting(holder);

        item.animate()
                .translationY(0f)
                .setDuration(getAddDuration())
                .setListener(new android.animation.AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(@NonNull android.animation.Animator animation) {
                        item.setTranslationY(0f);
                        dispatchAddFinished(holder);
                    }
                    @Override public void onAnimationCancel(@NonNull android.animation.Animator animation) {
                        item.setTranslationY(0f);
                    }
                });
        return true;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        item.itemView.animate().cancel();
        item.itemView.setTranslationY(0f);
        super.endAnimation(item);
    }
}
