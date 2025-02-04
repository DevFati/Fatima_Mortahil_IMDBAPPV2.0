package edu.pmdm.mortahil_fatimaimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.Arrays;

import edu.pmdm.mortahil_fatimaimdbapp.databinding.ActivitySelectAddressBinding;

public class SelectAddressActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivitySelectAddressBinding binding;
    private GoogleMap mMap;
    private LatLng latLngSeleccionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar ViewBinding
        binding = ActivitySelectAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializar Places API con tu clave
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyAER7D-uvYpBOG3wZjz9z3AeGulqAci-OU");
        }

        // Abrir directamente el selector de dirección al iniciar la actividad
        abrirBuscadorDeLugares();


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }


        // Botón para buscar una nueva dirección
        binding.searchAddressButton.setOnClickListener(v -> abrirBuscadorDeLugares());

        // Botón para confirmar la dirección seleccionada
        binding.confirmAddressButton.setOnClickListener(v -> confirmarDireccion());
    }

    private void abrirBuscadorDeLugares() {

            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.FULLSCREEN, // Cambiado a FULLSCREEN
                    Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG)
            ).build(this);
            startActivityForResult(intent, 1);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK && data != null) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                latLngSeleccionada = place.getLatLng();
                binding.searchAddress.setText(place.getAddress());
                actualizarMapa(latLngSeleccionada, place.getAddress());
            } else if (resultCode == RESULT_CANCELED) {
                // No permitir salir sin seleccionar una dirección
                Toast.makeText(this, "Por favor selecciona una dirección", Toast.LENGTH_SHORT).show();
                abrirBuscadorDeLugares(); // Reabrir el buscador
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Toast.makeText(this, "Error al seleccionar dirección", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void actualizarMapa(LatLng latLng, String address) {
        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title(address));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    }

    private void confirmarDireccion() {
        if (latLngSeleccionada != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("direccion", binding.searchAddress.getText().toString());
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Por favor selecciona una dirección", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Si hay una ubicación seleccionada previamente, céntrala en el mapa
        if (latLngSeleccionada != null) {
            actualizarMapa(latLngSeleccionada, binding.searchAddress.getText().toString());
        }


    }
}
