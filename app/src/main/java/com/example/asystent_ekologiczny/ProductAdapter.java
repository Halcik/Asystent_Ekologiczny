package com.example.asystent_ekologiczny;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductVH> {

    private final List<Product> products;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final DecimalFormat priceFormat = new DecimalFormat("0.00");
    private final FragmentActivity activity;
    private boolean gridMode = false; // nowy stan widoku

    public ProductAdapter(FragmentActivity activity, List<Product> products) {
        this.activity = activity;
        this.products = new ArrayList<>(products); // kopia danych
        dateFormat.setLenient(false);
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
    }

    @Override
    public int getItemCount() { return products.size(); }

    public void replaceData(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);
        notifyDataSetChanged(); // prosty refresh
    }

    public void setGridMode(boolean grid) {
        if (this.gridMode != grid) {
            this.gridMode = grid;
            notifyDataSetChanged(); // odśwież karty z nowym paddingiem
        }
    }

    static class ProductVH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvTitle;
        final TextView tvSubtitle;
        final TextView tvExtra;
        ProductVH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_product);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvExtra = itemView.findViewById(R.id.tvExtra);
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

    private int dpToPx(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }
}
