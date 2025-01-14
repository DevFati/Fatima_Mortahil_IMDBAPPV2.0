package edu.pmdm.mortahil_fatimaimdbapp.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
//con esta clase vamos a obtener los detalles de una pelicula desde TMDB
public class MovieSearchResponse {
//este metodo nos mostrara los detalles de una pelicula dado su id
    public Movie obtenerDetallesPelicula(String movieId) {
        OkHttpClient client = new OkHttpClient();

        // Configurar la solicitud con la URL específica del ID de la película
        Request request = new Request.Builder()
                .url("https://api.themoviedb.org/3/movie/" + movieId + "?language=en-US") // URL con el ID de la película
                .get()
                .addHeader("accept", "application/json")
                .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI2YzdlM2QwOGRhODllOTg3ZTQ5NGMwZmE1Yjc2ODA3OCIsIm5iZiI6MTczNjY5NDY3NS4xMDcsInN1YiI6IjY3ODNkYjkzMTM2ZTE1N2NmMjdiMzdkOCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.MyfzKUEqxr5W1D_ZUxWM7B7dAZDpOCZNv8GHOdMbNWc")
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String jsonResponse = response.body().string();
                JSONObject jsonObject = new JSONObject(jsonResponse);

                // Extraer los datos de la película del JSON
                String titulo = jsonObject.optString("title", "Título no disponible");
                String urlImagen = "https://image.tmdb.org/t/p/w500" + jsonObject.optString("poster_path", "");
                String fechaLanzamiento = jsonObject.optString("release_date", "Fecha no disponible");
                double calificacion = jsonObject.optDouble("vote_average", 0.0);


                // Crear y devolver el objeto Movie con los datos obtenidos
                return new Movie(movieId,titulo, urlImagen, "", fechaLanzamiento, calificacion,"tmdb");
            } else {
                System.out.println("Error en la solicitud: " + response.code());
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return null; // Si ocurre un error, devolver null
    }
}

