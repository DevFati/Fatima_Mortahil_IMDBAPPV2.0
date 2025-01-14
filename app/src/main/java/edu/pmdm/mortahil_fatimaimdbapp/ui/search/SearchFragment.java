package edu.pmdm.mortahil_fatimaimdbapp.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.mortahil_fatimaimdbapp.ResultsActivity;
import edu.pmdm.mortahil_fatimaimdbapp.api.TMDBApiService;
import edu.pmdm.mortahil_fatimaimdbapp.databinding.FragmentSlideshowBinding;
import edu.pmdm.mortahil_fatimaimdbapp.models.Genero;
import edu.pmdm.mortahil_fatimaimdbapp.models.Movie;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class SearchFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private TMDBApiService apiService;
    private final OkHttpClient client = new OkHttpClient();
    private String anio;
    private String generoId;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        configurarApiService();
        cargarGeneros(); //cargamos los generos desde la API para que se vean en el spinner

        //configuramos el boton buscar
        binding.btnBuscar.setOnClickListener(view -> {
            //el campo del año no puede estar vacio
            if (!binding.editTextAnio.getText().toString().isEmpty() && binding.spinnerGeneros.getSelectedItem() != null) {
                            // Obtenemos el año ingresado y el género seleccionado
                 anio = binding.editTextAnio.getText().toString();
                Genero generoSeleccionado = (Genero) binding.spinnerGeneros.getSelectedItem();
                 generoId = generoSeleccionado.getId()+"";

                buscarPeliculas(anio, generoId); //llamamos al metodo para buscar las peliculas y series que coincidan
            } else {
                Toast.makeText(requireContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    private void buscarPeliculas(String anio, String generoId) {
        new Thread(() -> {
            //creamos una lista donde guardamos las peliculas obtenidas
            List<Movie> peliculas = new ArrayList<>();
            apiService.buscarPeliculas(peliculas); // Llama al método de la API con la lista

            if (!peliculas.isEmpty()) {
                //si encontramos peliculas, abrimos la actividad para ver los resultados
                Intent intent = new Intent(requireContext(), ResultsActivity.class);
                intent.putParcelableArrayListExtra("peliculas", new ArrayList<>(peliculas));
                requireActivity().runOnUiThread(() -> startActivity(intent));
            } else {
                //si no se encontraron peliculas, mostramos un mensaje de error.
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "No se encontraron películas para la búsqueda.", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void configurarApiService() {
        //implementamos los metodos que se han definiso en la interfaz de TMDBApiService
        apiService = new TMDBApiService() {

            @Override
            public void obtenerGeneros(List<Genero> generos) {
                //configuramos la solicitud para obtener los generos
                Request request = new Request.Builder()
                        .url("https://api.themoviedb.org/3/genre/movie/list?language=en")
                        .get()
                        .addHeader("accept", "application/json")
                        .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI2YzdlM2QwOGRhODllOTg3ZTQ5NGMwZmE1Yjc2ODA3OCIsIm5iZiI6MTczNjY5NDY3NS4xMDcsInN1YiI6IjY3ODNkYjkzMTM2ZTE1N2NmMjdiMzdkOCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.MyfzKUEqxr5W1D_ZUxWM7B7dAZDpOCZNv8GHOdMbNWc")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray genresArray = jsonObject.getJSONArray("genres");
                        //iteramos por cada genero en el array
                        for (int i = 0; i < genresArray.length(); i++) {
                            JSONObject genre = genresArray.getJSONObject(i);
                            int id = genre.getInt("id"); //id del genero
                            String nombre = genre.getString("name"); //nombre del genero
                            generos.add(new Genero(id, nombre)); //vamso agregando los generos a la lista
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void buscarPeliculas(List<Movie> peliculas) {
                //configuramos la solicitud para buscar las peliculas
                Request request = new Request.Builder()
                        .url("https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&page=1&primary_release_year="+anio+"&sort_by=popularity.desc&with_genres="+generoId)
                        .addHeader("accept", "application/json")
                        .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI2YzdlM2QwOGRhODllOTg3ZTQ5NGMwZmE1Yjc2ODA3OCIsIm5iZiI6MTczNjY5NDY3NS4xMDcsInN1YiI6IjY3ODNkYjkzMTM2ZTE1N2NmMjdiMzdkOCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.MyfzKUEqxr5W1D_ZUxWM7B7dAZDpOCZNv8GHOdMbNWc")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray resultsArray = jsonObject.getJSONArray("results");
                        //vamos iterando por cada pelicula
                        for (int i = 0; i < resultsArray.length(); i++) {
                            JSONObject movieObject = resultsArray.getJSONObject(i);
                            String id = movieObject.optString("id");
                            String titulo = movieObject.optString("title", "Sin título");
                            String url = movieObject.optString("poster_path", "");
                            String fecha = movieObject.optString("release_date", "Fecha no disponible");
                            double votacion = movieObject.optDouble("vote_average", 0.0);
                            // Creamos un objeto Movie con los datos obtenidos y lo agregamos a la lista

                            peliculas.add(new Movie(id, titulo, "https://image.tmdb.org/t/p/w500" + url, "", fecha, votacion, "tmdb"));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private void cargarGeneros() {
        new Thread(() -> {
            //creamos una lista para ir guardando los generos
            List<Genero> generos = new ArrayList<>();
            apiService.obtenerGeneros(generos); //llaamos al metodo para obtener los generos

            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<Genero> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, generos);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinnerGeneros.setAdapter(adapter);
            });
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
