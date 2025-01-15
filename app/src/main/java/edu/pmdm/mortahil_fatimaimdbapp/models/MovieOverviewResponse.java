package edu.pmdm.mortahil_fatimaimdbapp.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import edu.pmdm.mortahil_fatimaimdbapp.api.IMDBApiClient;
import edu.pmdm.mortahil_fatimaimdbapp.api.IMDBApiService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//Con esta clase obtenemos al descripcion de la pelicula desde la API de IMDB
public class MovieOverviewResponse {

    public static void descripcionPelicula(String movieId, IMDBApiService callback) {
        //creamos un cliente HTTP para realizar la solicitud
        OkHttpClient client = new OkHttpClient();
        String apiKey=IMDBApiClient.getApiKey();
//copiamos y pegamos la solicitud http que nos lanza la api
        Request request = new Request.Builder()
                .url("https://imdb-com.p.rapidapi.com/title/get-overview?tconst=" + movieId) // ID de la película
                .get()
                .addHeader("x-rapidapi-key", apiKey)
                .addHeader("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                .build();
//ejecutamos nuestra solicitud en un hilo secundaro para no bloquear la interfaz de usuario
        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonResponse);

                    // Navegamos en el JSON para extraer la descripción
                    JSONObject dataObject = jsonObject.optJSONObject("data");
                    if (dataObject != null) {
                        JSONObject titleObject = dataObject.optJSONObject("title");
                        if (titleObject != null) {
                            JSONObject plotObject = titleObject.optJSONObject("plot");
                            if (plotObject != null) {
                                JSONObject plotTextObject = plotObject.optJSONObject("plotText");
                                if (plotTextObject != null) {
                                    //Sacamos la descripcion de "plainText" y le damos un valor por defecto por si no existe
                                    String descripcion = plotTextObject.optString("plainText", "Sin descripción");
                                    //Usamos el callback para devolcer la descripcion al llamador.
                                    callback.descripcion(descripcion);
                                    return;
                                }
                            }
                        }
                    }
                }else if(response.code()==429){ //llamadas a la api terminadas
                    Log.e("API", "Límite de solicitudes alcanzado. Cambiando API Key.");
                    IMDBApiClient.switchApiKey(); // Cambia a la siguiente clave
                    descripcionPelicula(movieId,callback); // Reintenta con la nueva clave
                    return;
                }
                // Si algo falla, llamamos al callback con una descripción por defecto
                callback.descripcion("Sin descripción");
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                callback.descripcion("Sin descripción");
            }
        }).start();
    }
}
