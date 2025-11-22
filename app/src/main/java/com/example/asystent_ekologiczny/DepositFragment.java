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
        });
        fabAdd.setOnClickListener(v -> openAdd());
        getParentFragmentManager().setFragmentResultListener("deposit_added", getViewLifecycleOwner(), (k,b)->{ long id=b.getLong("deposit_added_id",-1); if(id>0){ pendingAddedId = id; loadData(); }});
        getParentFragmentManager().setFragmentResultListener("deposit_updated", getViewLifecycleOwner(), (k,b)->{ long id=b.getLong("deposit_updated_id",-1); if(id>0){ loadData(); }});
        loadData(); // na końcu po zarejestrowaniu listenerów
    }

    private void loadData(){
        if(adapter==null) return;
        java.util.List<DepositItem> all = dbHelper.getAllDeposits();
        adapter.replaceData(all);
        updateTotalAndEmpty(all);
        if(pendingAddedId>0 && !all.isEmpty() && all.get(0).getId()==pendingAddedId){
            animateFirst();
            pendingAddedId=-1;
        } else if(pendingAddedId>0){ // jeśli kolejność inna spróbuj znaleźć pozycję
            int pos=-1;
            for(int i=0;i<all.size();i++){ if(all.get(i).getId()==pendingAddedId){ pos=i; break; } }
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

    private void updateTotalAndEmpty(java.util.List<DepositItem> list){ double sum=0; for(DepositItem d:list){ sum+=d.getValue(); } if(adapter!=null){ adapter.setTotalValue(sum); } if(emptyView!=null){ emptyView.setVisibility(list.isEmpty()? View.VISIBLE: View.GONE); }}
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
}


