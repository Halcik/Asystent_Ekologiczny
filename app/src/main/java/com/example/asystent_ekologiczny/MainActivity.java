package com.example.asystent_ekologiczny;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED = "selected_item";
    private BottomNavigationView bottomNavigationView;

    private ProductsFragment productsFragment;
    private DepositFragment depositFragment;
    private ReportsFragment reportsFragment;
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            showFragment(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            // Pierwsze uruchomienie – tworzymy fragmenty i dodajemy je (show/hide)
            productsFragment = new ProductsFragment();
            depositFragment = new DepositFragment();
            reportsFragment = new ReportsFragment();
            settingsFragment = new SettingsFragment();

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_container, productsFragment, ProductsFragment.TAG);
            ft.add(R.id.fragment_container, depositFragment, DepositFragment.TAG).hide(depositFragment);
            ft.add(R.id.fragment_container, reportsFragment, ReportsFragment.TAG).hide(reportsFragment);
            ft.add(R.id.fragment_container, settingsFragment, SettingsFragment.TAG).hide(settingsFragment);
            ft.commit();
        } else {
            // Odzyskujemy istniejące instancje po zmianie konfiguracji
            productsFragment = (ProductsFragment) getSupportFragmentManager().findFragmentByTag(ProductsFragment.TAG);
            depositFragment = (DepositFragment) getSupportFragmentManager().findFragmentByTag(DepositFragment.TAG);
            reportsFragment = (ReportsFragment) getSupportFragmentManager().findFragmentByTag(ReportsFragment.TAG);
            settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(SettingsFragment.TAG);
        }

        int selectedId = savedInstanceState == null ? R.id.navigation_products : savedInstanceState.getInt(KEY_SELECTED, R.id.navigation_products);
        bottomNavigationView.setSelectedItemId(selectedId);
        showFragment(selectedId);
    }

    private void showFragment(int itemId) {
        // Ukrywamy wszystko i pokazujemy właściwy fragment
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        if (productsFragment != null) ft.hide(productsFragment);
        if (depositFragment != null) ft.hide(depositFragment);
        if (reportsFragment != null) ft.hide(reportsFragment);
        if (settingsFragment != null) ft.hide(settingsFragment);

        if (itemId == R.id.navigation_deposit) {
            ft.show(depositFragment);
        } else if (itemId == R.id.navigation_reports) {
            ft.show(reportsFragment);
        } else if (itemId == R.id.navigation_settings) {
            ft.show(settingsFragment);
        } else { // navigation_products
            ft.show(productsFragment);
        }
        ft.commit();
        updateToolbarTitle(itemId);
    }

    private void updateToolbarTitle(int itemId) {
        if (getSupportActionBar() == null) return;
        if (itemId == R.id.navigation_deposit) {
            getSupportActionBar().setTitle("Kaucja");
        } else if (itemId == R.id.navigation_reports) {
            getSupportActionBar().setTitle("Raporty");
        } else if (itemId == R.id.navigation_settings) {
            getSupportActionBar().setTitle("Ustawienia");
        } else {
            getSupportActionBar().setTitle("Produkty");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNavigationView != null) {
            outState.putInt(KEY_SELECTED, bottomNavigationView.getSelectedItemId());
        }
    }
}