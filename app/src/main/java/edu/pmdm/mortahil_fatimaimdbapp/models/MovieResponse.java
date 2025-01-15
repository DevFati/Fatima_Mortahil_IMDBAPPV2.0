package edu.pmdm.mortahil_fatimaimdbapp.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import edu.pmdm.mortahil_fatimaimdbapp.api.IMDBApiClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//He creado esta clase para poder obtener todos los datos de las peliculas agregadas a favoritos
//que procedan de la api "imdb"
public class MovieResponse {
    public Movie obtenerDetallesPelicula(String movieId) {
        OkHttpClient client = new OkHttpClient();
        String apiKey= IMDBApiClient.getApiKey();

        Request request = new Request.Builder()
                .url("https://imdb-com.p.rapidapi.com/title/get-overview?tconst=" + movieId) //añadimos el id de la peli especifica como parametro
                .get()
                .addHeader("x-rapidapi-key", apiKey)
                .addHeader("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String jsonResponse = response.body().string();
                JSONObject jsonObject = new JSONObject(jsonResponse); //Trabajamos con objetos json
                //para facilitar el acceso a los datos

                // accedemos al nodo principal data y despues a title para obtener todos los datos de la peli o serie
                JSONObject dataObject = jsonObject.getJSONObject("data");
                JSONObject titleObject = dataObject.getJSONObject("title");

                // Título
                String titulo = titleObject.getJSONObject("titleText").getString("text");

                // URL de la imagen
                String urlImagen = null;
                if (titleObject.optJSONObject("primaryImage") != null) {
                    urlImagen = titleObject.getJSONObject("primaryImage").optString("url", "");
                }


                // Fecha de lanzamiento
                String fechaLanzamiento = "Fecha no disponible";
                if (titleObject.optJSONObject("releaseDate") != null) {
                    JSONObject releaseDate = titleObject.getJSONObject("releaseDate");
                    //extraemos yeat, month y day
                    int year = releaseDate.optInt("year", -1);
                    int month = releaseDate.optInt("month", -1);
                    int day = releaseDate.optInt("day", -1);
                    if (year > 0 && month > 0 && day > 0) {
                        //le damos formato a los valores
                        fechaLanzamiento = year + "-" + month + "-" + day;
                    }
                }

                // Rating
                double calificacion = 0.0;
                if (titleObject.optJSONObject("ratingsSummary") != null) {
                    calificacion = titleObject.getJSONObject("ratingsSummary").optDouble("aggregateRating", 0.0); //0.0 sera la calificacion por defecto
                }

                //creamos un nuevo objeto movie que devolveremos con los datos obtenidos importante pasarle como valor api "imdb"
                return new Movie(movieId, titulo, urlImagen, null, fechaLanzamiento, calificacion,"imdb");
            }else if(response.code()==429){ //llamadas a la api terminadas
                Log.e("API", "Límite de solicitudes alcanzado. Cambiando API Key.");
                IMDBApiClient.switchApiKey(); // Cambia a la siguiente clave
                // Reintenta con la nueva clave
                return obtenerDetallesPelicula(movieId);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return null; // Si ocurre un error, devolver null
    }
}



