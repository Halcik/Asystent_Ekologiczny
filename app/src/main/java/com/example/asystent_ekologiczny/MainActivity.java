package com.example.asystent_ekologiczny;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
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

    private int pendingNav = -1; // docelowa nawigacja po zamknięciu AddProductFragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingsFragment.applySavedTheme(this); // zastosuj zapisany motyw
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (isAddProductVisible()) {
                // Jeśli edytujemy produkt i kliknięto Produkty – zostaw formularz
                if (id == R.id.navigation_products) {
                    return true; // nic nie zmieniamy
                }
                // Inna zakładka – zamknij formularz i zapamiętaj dokąd przejść
                pendingNav = id;
                getSupportFragmentManager().popBackStack();
                return true;
            }
            showFragment(id);
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

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (!isAddProductVisible() && pendingNav != -1) {
                int dest = pendingNav;
                pendingNav = -1;
                showFragment(dest);
                // Ustaw zaznaczenie na docelowym elemencie (mogło się zmienić wcześniej)
                bottomNavigationView.setSelectedItemId(dest == 0 ? R.id.navigation_products : dest);
            }
        });
        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) {
                bottomNavigationView.setVisibility(View.GONE); // klawiatura otwarta
            } else {
                bottomNavigationView.setVisibility(View.VISIBLE); // klawiatura zamknięta
            }
        });
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
            if (depositFragment != null) ft.show(depositFragment);
        } else if (itemId == R.id.navigation_reports) {
            if (reportsFragment != null) ft.show(reportsFragment);
        } else if (itemId == R.id.navigation_settings) {
            if (settingsFragment != null) ft.show(settingsFragment);
        } else { // navigation_products
            if (productsFragment != null) ft.show(productsFragment);
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

    private boolean isAddProductVisible() { // rozszerzone: sprawdza również szczegóły produktu
        Fragment fAdd = getSupportFragmentManager().findFragmentByTag(AddProductFragment.TAG);
        if (fAdd != null && fAdd.isVisible()) return true;
        Fragment fDetails = getSupportFragmentManager().findFragmentByTag(ProductDetailsFragment.TAG);
        return fDetails != null && fDetails.isVisible();
    }
}