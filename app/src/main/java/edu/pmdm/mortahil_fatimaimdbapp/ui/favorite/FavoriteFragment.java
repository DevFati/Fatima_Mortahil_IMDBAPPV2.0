package edu.pmdm.mortahil_fatimaimdbapp.ui.favorite;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import edu.pmdm.mortahil_fatimaimdbapp.adapters.MovieAdapter;
import edu.pmdm.mortahil_fatimaimdbapp.database.FavoritesManager;
import edu.pmdm.mortahil_fatimaimdbapp.databinding.FragmentGalleryBinding;
import edu.pmdm.mortahil_fatimaimdbapp.models.Movie;
import edu.pmdm.mortahil_fatimaimdbapp.models.MovieResponse;
import edu.pmdm.mortahil_fatimaimdbapp.models.MovieSearchResponse;

public class FavoriteFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private MovieAdapter adapter; // Adaptador para la lista de películas en el RecyclerView
    private List<Movie> peliculas; //Lista de peliculas favoritas
    private FavoritesManager gestorFavoritos; //
    private BluetoothAdapter bluetoothAdapter;

    private ActivityResultLauncher<String> solicitarPermisoBluetoothConnectLauncher;
    private ActivityResultLauncher<Intent> activarBluetoothLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        peliculas = new ArrayList<>(); //inicializamos la lista de peliculas favoritas
        gestorFavoritos = new FavoritesManager(requireContext());

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        configurarRecyclerView(); //configuramos el recyclerview
        configurarLaunchers();

        //configuramos nuestro boton de ocmpartir para verificar permisos y manejar el bluetoth antes de mostrar el dialogo
        binding.btnCompartir.setOnClickListener(view -> manejarBluetoothYPedirPermisos());

        cargarFavoritos(); //cargamos  la lista de peliculas favoritas en el recyclerview

        return binding.getRoot();
    }

    private void configurarRecyclerView() {
        adapter = new MovieAdapter(peliculas, getContext(), true);
        binding.recyclerViewFavoritos.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewFavoritos.setAdapter(adapter);
    }

    //si el bluetooth esta desativado le pedimos al usuario que lo active, si esta activado solicitamos
    //el de bluetooth connect
    private void configurarLaunchers() {
        activarBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                        solicitarPermisoBluetoothConnect();
                    } else {
                        Toast.makeText(requireContext(), "Bluetooth no activado.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        //solicitamos el permiso bluetooth connect

        solicitarPermisoBluetoothConnectLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(requireContext(), "Permiso de Bluetooth Connect concedido.", Toast.LENGTH_SHORT).show();
                        manejarBluetoothYPedirPermisos(); // Vuelve a intentar activar Bluetooth
                    } else {
                        Toast.makeText(requireContext(), "Permiso de Bluetooth Connect denegado o no disponible.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

    }

    private void manejarBluetoothYPedirPermisos() {
        // Verifica si el adaptador Bluetooth está disponible sino se lo indicamos al usuario
        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), "Bluetooth no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si estamos en Android 12 o superior verificamos el permiso BLUETOOTH_CONNECT porque en versiones anteriores no existe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                solicitarPermisoBluetoothConnectLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                return; // Salimos para esperar a que se conceda el permiso
            }
        }

        // Intenta activar Bluetooth si no está activado
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activarBluetoothLauncher.launch(enableBtIntent);
        } else {
            // Bluetooth ya está activado, mostrar el diálogo de compartir
            mostrarDialogoCompartir();
        }
    }



    private void solicitarPermisoBluetoothConnect() {
        // Verificamos si el permiso BLUETOOTH_CONNECT ya está concedido
        //MUCHO CUIDADO CON ESTO!! Si no se hace verificacion de este permiso antes, y se quiere pedir primero
        //activar bluetooth la aplicacion se rompe
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            solicitarPermisoBluetoothConnectLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            if(peliculas.size()>0){
                mostrarDialogoCompartir();
            }else{
                Toast.makeText(requireContext(), "Lista de favoritos vacía", Toast.LENGTH_SHORT).show();

            }

        }
    }


    //creamos un json con los detalles de las peliculas favoritas del usuario
    private void mostrarDialogoCompartir() {
        JsonArray jsonArray = new JsonArray();
        for (Movie pelicula : peliculas) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", pelicula.getId());
            jsonObject.addProperty("overview", "");
            jsonObject.addProperty("posterUrl", pelicula.getUrlImagen());
            jsonObject.addProperty("rating", pelicula.getCalificacion());
            jsonObject.addProperty("releaseDate", pelicula.getFechaLanzamiento());
            jsonObject.addProperty("title", pelicula.getTitulo());
            jsonArray.add(jsonObject);
        }

        String jsonFinal = new Gson().toJson(jsonArray); //usamos "gson" para convertir la peli a json

        new AlertDialog.Builder(requireContext())
                .setTitle("Películas Favoritas en JSON")
                .setMessage(jsonFinal)
                .setPositiveButton("Cerrar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    //Obtenemos el id del usuario desde sharedpreferences
    private void cargarFavoritos() {
        String userId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                .getString("USER_ID", null);
        List<Pair<String, String>> favoritos = gestorFavoritos.obtenerFavoritosIdsYSources(userId);
        peliculas.clear();

        new Thread(() -> { //cargamos los datos de la pelicula desde la api (dependiendo de la api que sea)
            List<Movie> nuevasPeliculas = new ArrayList<>();
            MovieResponse movieResponse = new MovieResponse();
            MovieSearchResponse movieSearchResponse= new MovieSearchResponse();
            for (Pair<String, String> favorito : favoritos) {
                String id = favorito.first;
                String source = favorito.second;
                Movie pelicula = null;
                if ("imdb".equals(source)) {
                    pelicula = movieResponse.obtenerDetallesPelicula(id);
                }
                Movie peliculaT=null;
                if ("tmdb".equals(source)) {
                    pelicula = movieSearchResponse.obtenerDetallesPelicula(id);
                }
                if (pelicula != null) {
                    nuevasPeliculas.add(pelicula);
                }
            }
            if (isAdded()) { //actualizamos el adaptador en el hilo principal cuando se carguen todos lso datos
                requireActivity().runOnUiThread(() -> {
                    peliculas.clear();
                    peliculas.addAll(nuevasPeliculas);
                    adapter.notifyDataSetChanged();
                });
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        // Cerramos la conexión a la base de datos cuando el fragmento se destruye
        super.onDestroyView();
        if (gestorFavoritos != null) {
            gestorFavoritos.close();
        }
        binding = null;
    }
}
