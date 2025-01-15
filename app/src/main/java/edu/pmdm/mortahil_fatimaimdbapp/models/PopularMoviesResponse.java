package edu.pmdm.mortahil_fatimaimdbapp.models;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.pmdm.mortahil_fatimaimdbapp.api.IMDBApiClient;
import edu.pmdm.mortahil_fatimaimdbapp.api.IMDBApiService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//Obtenemos las 10 peliculas mas populares (peliculas y/o series)
public class PopularMoviesResponse  {

    public static void top10peliculas(IMDBApiService callback){

        // lista que almacenara las peliculas obtenidas desde nuestra API
        List<Movie> movies = new ArrayList<>();
        //Creamos un cliente HTTP con el que realizaremos las solicitudes
        OkHttpClient client = new OkHttpClient();
        String apiKey= IMDBApiClient.getApiKey();


        Request request = new Request.Builder()
                .url("https://imdb-com.p.rapidapi.com/title/get-top-meter?topMeterTitlesType=ALL")
                .get()
                .addHeader("x-rapidapi-key", apiKey)
                .addHeader("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                .build();
        new Thread(() -> {
        try {
            //Ejecutamos la solicitud que nos devolvera un objeto response con los datos de la respuesta
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String jsonResponse = response.body().string();
                //Convertimos el string en un objeto JSOn
                JSONObject jsonObject = new JSONObject(jsonResponse);
                // Obtenemos el nodo principal y despues accedemos a "topMeterTitles" y a edges que es un array que contiene los datos
                //individuales de las pelis y series
                JSONObject dataObject = jsonObject.getJSONObject("data");
                JSONObject topMeterTitles = dataObject.getJSONObject("topMeterTitles");
                JSONArray edgesArray = topMeterTitles.getJSONArray("edges");

                //Iteramos pero solo los 10 primeros (porque nuestra lista solo es de los top 10)
                for (int i = 0; i < edgesArray.length() && i < 10; i++) {
                    //dentro de cada elemento accedemos al nodo "node" donde se encuentran todos los datos de la pelicula
                    JSONObject node = edgesArray.getJSONObject(i).getJSONObject("node");
                    //Obtenemos el titulo, la url de imagen y el ranking de las pelis y series
                    String titulo = "Sin título";
                    if (node.optJSONObject("titleText") != null) {
                        titulo = node.getJSONObject("titleText").optString("text", "Sin título");
                    }

                    String urlImagen = "";
                    if (node.optJSONObject("primaryImage") != null) {
                        urlImagen = node.getJSONObject("primaryImage").optString("url", "");
                    }

                    String ranking = "N/A";
                    if (node.optJSONObject("meterRanking") != null) {
                        ranking = String.valueOf(node.getJSONObject("meterRanking").optInt("currentRank", -1));
                    }

                    //extraemos el año, mes y el dia.
                    String fechaLanzamiento = "Fecha no disponible";
                    if (node.has("releaseDate")) {
                        JSONObject releaseDate = node.getJSONObject("releaseDate");
                        int year = releaseDate.optInt("year", -1);
                        int month = releaseDate.optInt("month", -1);
                        int day = releaseDate.optInt("day", -1);
                        if (year > 0 && month > 0 && day > 0) {
                            fechaLanzamiento = year + "-" + month + "-" + day;
                        }
                    }

                    //Creamos un nuevo objeto "Movie" con los datos sacados y lo añadimos a la lista de "movies".
                    String movieId = node.optString("id", "");
                    movies.add(new Movie(movieId, titulo, urlImagen, " ", fechaLanzamiento, Double.parseDouble(ranking),"imdb"));
                }
            }else if(response.code()==429){ //llamadas a la api terminadas
                Log.e("API", "Límite de solicitudes alcanzado. Cambiando API Key.");
                IMDBApiClient.switchApiKey(); // Cambia a la siguiente clave
                // Reintenta con la nueva clave
                 top10peliculas(callback);
                 return;
            }
            //usamos el metodo de "cargarpeliculas" de la interfaz "IMDBApiService" para enviar los datos al callback
            callback.cargarpeliculas(movies);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            callback.cargarpeliculas(new ArrayList<>());

        }
        }).start();
    }
}
