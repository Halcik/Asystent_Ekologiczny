package com.example.asystent_ekologiczny;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

/**
 * Fragment prezentujący przykładowe pozycje systemu kaucyjnego (dane statyczne).
 * Docelowo może być zastąpiony modułem obliczeń / listą edytowalną.
 */
public class DepositFragment extends Fragment {

    public static final String TAG = "DepositFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_deposit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayout list = view.findViewById(R.id.deposit_list);
        if (list != null) {
            addDeposit(list, "🍺 Butelki szklane 0,5L", "8 sztuk × 0,50 zł", "4,00 zł");
            addDeposit(list, "🥤 Puszki aluminiowe", "12 sztuk × 0,50 zł", "6,00 zł");
            addDeposit(list, "🍼 Butelki plastikowe 1,5L", "5 sztuk × 0,50 zł", "2,50 zł");
        }
    }

    /** Tworzy kartę z pozycją kaucji i dodaje do listy. */
    private void addDeposit(LinearLayout parent, String title, String subtitle, String value) {
        if (getContext() == null) return;
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (getResources().getDisplayMetrics().density * 8);
        card.setLayoutParams(params);
        card.setCardElevation(4f);
        card.setUseCompatPadding(true);
        card.setPreventCornerOverlap(true);
        card.setContentPadding(24,24,24,24);

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextSize(16);

        TextView tvSubtitle = new TextView(requireContext());
        tvSubtitle.setText(subtitle);
        tvSubtitle.setTextSize(13);

        TextView tvValue = new TextView(requireContext());
        tvValue.setText(value);
        tvValue.setTextSize(15);
        tvValue.setTypeface(Typeface.DEFAULT_BOLD);
        tvValue.setPadding(0,(int)(getResources().getDisplayMetrics().density*4),0,0);

        inner.addView(tvTitle);
        inner.addView(tvSubtitle);
        inner.addView(tvValue);

        card.addView(inner);
        parent.addView(card);
    }
}
