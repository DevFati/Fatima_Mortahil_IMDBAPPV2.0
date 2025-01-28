package edu.pmdm.mortahil_fatimaimdbapp.ui.top10;

import static android.content.Context.MODE_PRIVATE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.mortahil_fatimaimdbapp.adapters.MovieAdapter;
import edu.pmdm.mortahil_fatimaimdbapp.api.IMDBApiService;
import edu.pmdm.mortahil_fatimaimdbapp.databinding.FragmentHomeBinding;
import edu.pmdm.mortahil_fatimaimdbapp.models.Movie;
import edu.pmdm.mortahil_fatimaimdbapp.models.PopularMoviesResponse;
import edu.pmdm.mortahil_fatimaimdbapp.sync.FavoritesSync;

public class Top10Fragment extends Fragment {
    private FragmentHomeBinding binding;
    private MovieAdapter adapter;
    private List<Movie> peliculas;
    private FavoritesSync favSync;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // Configuración del RecyclerView con GridLayoutManager
        binding.recyclerViewPeliculas.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        peliculas = new ArrayList<>();
        String userId = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getString("USER_ID", null);

        favSync = new FavoritesSync(getContext(), userId);

        adapter = new MovieAdapter(peliculas, requireContext(), false,favSync);

        binding.recyclerViewPeliculas.setAdapter(adapter);

        // Cargar películas desde la API
        cargarPeliculas();

        return binding.getRoot();
    }
    private void cargarPeliculas() {
        // Usamos `top10peliculas` de `PopularMoviesResponse` para cargar las películas
        PopularMoviesResponse.top10peliculas(new IMDBApiService() {
            @Override
            public void cargarpeliculas(List<Movie> movies) {
                // Ejecutamos en el hilo principal para actualizar la UI
                requireActivity().runOnUiThread(() -> {
                    if (movies != null && !movies.isEmpty()) {
                        peliculas.clear(); // Limpiamos la lista actual
                        peliculas.addAll(movies); // Añadimos las nuevas películas
                        adapter.notifyDataSetChanged(); // Notificamos cambios al adaptador
                    } else {
                        Toast.makeText(requireContext(), "No se encontraron películas populares.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void descripcion(String descripcion) {
                // Este método no lo usamos aqui
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
