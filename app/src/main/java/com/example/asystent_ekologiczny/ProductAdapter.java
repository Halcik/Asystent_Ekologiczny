package com.example.asystent_ekologiczny;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable; // dodane
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter RecyclerView prezentujący listę / siatkę produktów.
 * Odpowiada za:
 *  - dynamiczne kolory kart w zależności od terminu ważności,
 *  - przełączanie listy/siatki (padding, layout manager w fragmencie),
 *  - sortowanie po cenie (wywoływane z zewnątrz),
 *  - tooltip z datą zakupu (long click),
 *  - płynne animacje przy dodaniu.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductVH> {

    private final List<Product> products;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final DecimalFormat priceFormat = new DecimalFormat("0.00");
    private final FragmentActivity activity;
    private boolean gridMode = false; // zachowane
    private static PopupWindow activeTooltip; // jeden aktywny popup globalnie

    public ProductAdapter(FragmentActivity activity, List<Product> products) {
        this.activity = activity;
        this.products = new ArrayList<>(products); // kopia danych
        dateFormat.setLenient(false);
        setHasStableIds(true); // stabilne ID dla bezpiecznych animacji
    }

    @NonNull
    @Override
    public ProductVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductVH holder, int position) {
        Product p = products.get(position);
        Context ctx = holder.itemView.getContext();
        Date today = stripTime(new Date());

        int colorStroke = R.color.card_border_success;
        int colorCard = R.color.card_background_ok;
        if (p.getExpirationDate() != null && !p.getExpirationDate().isEmpty()) {
            try {
                Date exp = dateFormat.parse(p.getExpirationDate());
                if (exp != null) {
                    long days = (exp.getTime() - today.getTime()) / (1000L*60*60*24);
                    if (days < 0) {
                        colorStroke = R.color.card_border_danger;
                        colorCard = R.color.card_background_danger;
                    } else if (days <= 3) {
                        colorStroke = R.color.card_border_warning;
                    }
                }
            } catch (ParseException ignored) {}
        }
        holder.card.setStrokeWidth(dpToPx(ctx, 2));
        holder.card.setStrokeColor(ContextCompat.getColor(ctx, colorStroke));
        holder.card.setCardBackgroundColor(ContextCompat.getColor(ctx, colorCard));
        // Mniejszy padding (kolejna redukcja): lista (12x8dp), siatka (8x6dp)
        int horizontal = gridMode ? 8 : 12; // dp
        int vertical = gridMode ? 6 : 8;    // dp
        holder.card.setContentPadding(dpToPx(ctx, horizontal), dpToPx(ctx, vertical), dpToPx(ctx, horizontal), dpToPx(ctx, vertical));

        holder.tvTitle.setText(p.getName());
        holder.tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        // Ustawienie ikony kategorii
        holder.ivCategory.setImageResource(resolveCategoryIcon(p.getCategory()));

        String formattedPrice = priceFormat.format(p.getPrice()).replace('.', ',') + " zł";
        String subtitle = "Cena: " + formattedPrice;
        if (p.getExpirationDate() != null && !p.getExpirationDate().isEmpty()) subtitle += "  •  Ważność: " + p.getExpirationDate();
        holder.tvSubtitle.setText(subtitle);

        String extra = (p.getCategory() == null ? "" : p.getCategory()) + (p.getStore()==null?"" : ("  •  " + p.getStore()));
        holder.tvExtra.setText(extra.trim());

        holder.card.setOnClickListener(v -> {
            ProductDetailsFragment fragment = ProductDetailsFragment.newInstance(p.getId());
            activity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, fragment, ProductDetailsFragment.TAG)
                    .addToBackStack(ProductDetailsFragment.TAG)
                    .commit();
        });
        holder.card.setOnLongClickListener(v -> {
            String purchase = p.getPurchaseDate();
            String msg = (purchase != null && !purchase.isEmpty()) ? ("Data zakupu: " + purchase) : "Brak daty zakupu";
            showPurchaseTooltip(holder.tvTitle, holder.card, msg);
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        });
    }

    private void showPurchaseTooltip(View anchorTitle, View cardView, String message) {
        if (activeTooltip != null) { activeTooltip.dismiss(); activeTooltip = null; }
        View content = LayoutInflater.from(anchorTitle.getContext()).inflate(R.layout.tooltip_purchase_date, null, false);
        TextView tv = content.findViewById(R.id.tv_tooltip_text);
        tv.setText(message);
        PopupWindow pw = new PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        pw.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        pw.setOutsideTouchable(true);

        int[] titleLoc = new int[2];
        anchorTitle.getLocationOnScreen(titleLoc); // lewy górny rożek tytułu
        int[] cardLoc = new int[2];
        cardView.getLocationOnScreen(cardLoc); // lewy górny rożek karty

        Rect displayFrame = new Rect();
        anchorTitle.getWindowVisibleDisplayFrame(displayFrame);
        content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int pwW = content.getMeasuredWidth();
        int pwH = content.getMeasuredHeight();
        int titleH = anchorTitle.getHeight();
        int cardW = cardView.getWidth();

        // Poziomo: środek karty
        int x = cardLoc[0] + (cardW / 2) - (pwW / 2);
        // Pionowo: wycentrowane względem środka nazwy
        int titleCenterY = titleLoc[1] + (titleH / 2);
        int y = titleCenterY - (pwH / 2);

        // Ograniczenia ekranu
        int minX = displayFrame.left + dpToPx(anchorTitle.getContext(),4);
        int maxX = displayFrame.right - pwW - dpToPx(anchorTitle.getContext(),4);
        if (x < minX) x = minX;
        if (x > maxX) x = maxX;
        int minY = displayFrame.top + dpToPx(anchorTitle.getContext(),4);
        int maxY = displayFrame.bottom - pwH - dpToPx(anchorTitle.getContext(),4);
        if (y < minY) y = minY;
        if (y > maxY) y = maxY;

        pw.showAtLocation(anchorTitle.getRootView(), Gravity.NO_GRAVITY, x, y);
        activeTooltip = pw;
        anchorTitle.postDelayed(() -> { if (activeTooltip == pw) { pw.dismiss(); activeTooltip = null; } }, 2500);
    }

    @Override
    public int getItemCount() { return products.size(); }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= products.size()) return RecyclerView.NO_ID;
        return products.get(position).getId();
    }

    /** Odświeża cały zestaw danych (bez diff - dla prostoty). */
    public void replaceData(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);
        notifyDataSetChanged(); // prosty refresh
    }

    /** Ustawia tryb siatki (zmienia padding kart). */
    public void setGridMode(boolean grid) {
        if (this.gridMode != grid) {
            this.gridMode = grid;
            notifyDataSetChanged(); // odśwież karty z nowym paddingiem
        }
    }

    /** Dodaje nowy produkt na górę listy jeśli nie istnieje (animacja wstawienia). */
    public void addProductAtTop(Product p) {
        if (p == null) return;
        for (Product existing : products) {
            if (existing.getId() == p.getId()) return; // duplikat
        }
        products.add(0, p);
        notifyItemInserted(0);
    }

    /** Sortuje według ceny w kierunku rosnącym / malejącym. */
    public void sortByPrice(boolean ascending) {
        java.util.Collections.sort(products, (a, b) -> {
            if (ascending) return Double.compare(a.getPrice(), b.getPrice());
            return Double.compare(b.getPrice(), a.getPrice());
        });
        notifyDataSetChanged();
    }

    static class ProductVH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvTitle;
        final TextView tvSubtitle;
        final TextView tvExtra;
        final ImageView ivCategory; // nowa referencja
        ProductVH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_product);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvExtra = itemView.findViewById(R.id.tvExtra);
            ivCategory = itemView.findViewById(R.id.ivCategory);
        }
    }

    /** Ikona kategorii na podstawie prostych dopasowań). */
    private int resolveCategoryIcon(String category) {
        if (category == null) return R.drawable.ic_category_other;
        String c = category.trim().toLowerCase(Locale.getDefault());
        if (c.contains("warzy")) return R.drawable.ic_category_vegetables; // warzywa
        if (c.contains("owoc") || c.contains("jabł") || c.contains("jabl") || c.contains("banan") || c.contains("grusz") || c.contains("trusk")) return R.drawable.ic_category_fruits; // owoce
        if (c.contains("nabia") || c.contains("mle")) return R.drawable.ic_category_dairy; // nabiał / mleko
        if (c.contains("napoj") || c.contains("picia") || c.contains("drink") || c.contains("sok")) return R.drawable.ic_category_drinks; // napoje
        if (c.contains("piec") || c.contains("chleb") || c.contains("bul") || c.contains("buł") || c.contains("bagiet")) return R.drawable.ic_category_bread; // pieczywo
        return R.drawable.ic_category_other;
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

    private int dpToPx(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }
}
