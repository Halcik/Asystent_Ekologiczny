package com.example.asystent_ekologiczny;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ReturnPointsMapDialogFragment extends DialogFragment implements OnMapReadyCallback {

    public static final String TAG = "ReturnPointsMapDialog";
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    private MapView mapView;
    private GoogleMap googleMap;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_return_points_map, null, false);

        mapView = content.findViewById(R.id.mapView);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                .create();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Przykładowe punkty zwrotu (symulacja)
        LatLng p1 = new LatLng(51.939988, 15.5287752);
        LatLng p2   = new LatLng(51.946711, 15.518838);
        LatLng p3  = new LatLng(51.935497, 15.494233);

        googleMap.addMarker(new MarkerOptions()
                .position(p1)
                .title("Butelkomat - Podgórna, Zielona Góra"));

        googleMap.addMarker(new MarkerOptions()
                .position(p2)
                .title("Butelkomat - Elektron, Wiadomo Gdzie"));

        googleMap.addMarker(new MarkerOptions()
                .position(p3)
                .title("Butelkomat - Areszt Śledczy, Zielona Góra"));

        // Ustaw kamerę mniej więcej na środek Polski
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(p2, 11.8f));
    }

    // ---- lifecycle MapView ----

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
            if (mapViewBundle == null) {
                mapViewBundle = new Bundle();
                outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
            }
            mapView.onSaveInstanceState(mapViewBundle);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }
}
