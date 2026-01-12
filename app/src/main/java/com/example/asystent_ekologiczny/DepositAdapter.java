package com.example.asystent_ekologiczny;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Adapter listy opakowań kaucyjnych. */
public class DepositAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface Listener {
        void onItemClick(DepositItem item);
        void onItemLongClick(DepositItem item);
        void onItemDelete(DepositItem item);
        void onItemReturnedToggle(DepositItem item, boolean returned);
    }
    private final List<DepositItem> data = new ArrayList<>();
    private Listener listener;

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;
    private double totalValue = 0.0;

    public void setListener(Listener l) { this.listener = l; }
    public void replaceData(List<DepositItem> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }
    public void addAtTop(DepositItem item) {
        data.add(0, item);
        notifyItemInserted(0);
    }
    public void setTotalValue(double v){ totalValue = v; notifyItemChanged(getItemCount()-1); }

    @Override public int getItemViewType(int position){ return position == data.size() ? TYPE_FOOTER : TYPE_ITEM; }

    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==TYPE_FOOTER){
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(24,32,24,32);
            tv.setTextSize(15);
            tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tv.setTextColor(parent.getResources().getColor(R.color.text_primary));
            return new FooterVH(tv);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deposit, parent, false);
        return new ItemVH(v);
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(getItemViewType(position)==TYPE_FOOTER){ ((FooterVH)holder).bind(totalValue); return; }
        DepositItem item = data.get(position);
        ItemVH ih = (ItemVH)holder;
        ih.title.setText(item.getName());
        String extraBarcode = item.getBarcode()!=null && !item.getBarcode().isEmpty()? " • " + item.getBarcode():"";
        ih.subtitle.setText(String.format(Locale.getDefault(), "%s • %.2f zł%s", item.getCategory(), item.getValue(), extraBarcode));
        ih.itemView.setOnClickListener(v -> { if (listener!=null) listener.onItemClick(item); });
        ih.itemView.setOnLongClickListener(v -> { if (listener!=null) listener.onItemLongClick(item); return true; });
        ih.btnDelete.setOnClickListener(v -> { if(listener!=null) listener.onItemDelete(item); });
        // Checkbox zwrócenia
        ih.cbReturned.setOnCheckedChangeListener(null);
        ih.cbReturned.setChecked(item.isReturned());
        ih.cbReturned.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onItemReturnedToggle(item, isChecked);
        });
        // Stylizacja: wyszarzaj zwrócone (jak zużyte w produktach)
        if (item.isReturned()) {
            ih.itemView.setAlpha(0.6f);
            ih.title.setTextColor(ih.itemView.getResources().getColor(R.color.text_secondary));
            ih.subtitle.setTextColor(ih.itemView.getResources().getColor(R.color.text_secondary));
        } else {
            ih.itemView.setAlpha(1f);
            ih.title.setTextColor(ih.itemView.getResources().getColor(R.color.text_primary));
            ih.subtitle.setTextColor(ih.itemView.getResources().getColor(R.color.text_secondary));
        }
        // Ramka karty – zielona dla zwróconych lub standardowa dla aktywnych
        if (ih.card != null) {
            if (item.isReturned()) {
                ih.card.setStrokeColor(ih.card.getResources().getColor(R.color.card_border_success));
            } else {
                ih.card.setStrokeColor(ih.card.getResources().getColor(R.color.card_border_success)); // zostaw ten sam lub ustaw inny dla aktywnych
            }
        }
    }

    @Override public int getItemCount() { return data.size() + 1; }

    static class ItemVH extends RecyclerView.ViewHolder {
        MaterialCardView card; TextView title; TextView subtitle; View btnDelete; CheckBox cbReturned;
        ItemVH(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            title = itemView.findViewById(R.id.tv_deposit_title);
            subtitle = itemView.findViewById(R.id.tv_deposit_subtitle);
            btnDelete = itemView.findViewById(R.id.btn_deposit_delete);
            cbReturned = itemView.findViewById(R.id.cb_returned);
        }
    }
    static class FooterVH extends RecyclerView.ViewHolder {
        private final TextView tv;
        FooterVH(@NonNull View itemView){ super(itemView); tv=(TextView)itemView; }
        void bind(double sum){ tv.setText(itemView.getResources().getString(R.string.deposit_footer_active_sum, sum)); }
    }

    /** Sortuje według kaucji w kierunku rosnącym / malejącym. */
    public void sortByDeposit(boolean ascending) {
        java.util.Collections.sort(data, (a, b) -> {
            if (ascending) return Double.compare(a.getValue(), b.getValue());
            return Double.compare(b.getValue(), a.getValue());
        });
        notifyDataSetChanged();
    }
}
