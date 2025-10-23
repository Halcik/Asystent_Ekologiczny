package com.example.asystent_ekologiczny;

import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorListener;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

/** Animator dodający łagodne pojawianie (fade-in) przy dodaniu produktu. */
public class FadeInItemAnimator extends DefaultItemAnimator {

    public FadeInItemAnimator() {
        setAddDuration(200); // czas animacji dodania
    }

    @Override
    public boolean animateAdd(final RecyclerView.ViewHolder holder) {
        holder.itemView.setAlpha(0f);
        dispatchAddStarting(holder);
        ViewCompat.animate(holder.itemView)
                .alpha(1f)
                .setDuration(getAddDuration())
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override public void onAnimationStart(View view) {}
                    @Override public void onAnimationEnd(View view) {
                        view.setAlpha(1f);
                        dispatchAddFinished(holder);
                    }
                    @Override public void onAnimationCancel(View view) {
                        view.setAlpha(1f);
                    }
                }).start();
        return true;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        item.itemView.animate().cancel();
        item.itemView.setAlpha(1f);
        super.endAnimation(item);
    }
}

