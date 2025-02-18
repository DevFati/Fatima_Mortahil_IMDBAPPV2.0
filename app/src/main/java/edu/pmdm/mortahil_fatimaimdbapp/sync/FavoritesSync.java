package edu.pmdm.mortahil_fatimaimdbapp.sync;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.core.util.Pair;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;
import java.util.List;

import edu.pmdm.mortahil_fatimaimdbapp.database.DatabaseManager;
import edu.pmdm.mortahil_fatimaimdbapp.models.Movie;
import edu.pmdm.mortahil_fatimaimdbapp.models.User;

public class FavoritesSync {

    private final FirebaseFirestore firestore;
    private final DatabaseManager favoritesManager;
    private final Context context;
    private final String userId;

    public FavoritesSync(Context context, String userId) {
        this.context = context;
        this.userId = userId;
        this.firestore = FirebaseFirestore.getInstance();
        this.favoritesManager = new DatabaseManager(context);
    }

    public FavoritesSync(Context context) {
        this.context = context;
        this.userId = "";
        this.firestore = FirebaseFirestore.getInstance();
        this.favoritesManager = new DatabaseManager(context);
    }

    /**
     * Sincronizamos los datos de favoritos desde Firestore a la base de datos local.
     */
    public void sincronizarDesdeFirestore() {
        firestore.collection("favorites").document(userId).collection("movies")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    favoritesManager.limpiarFavoritosUsuario(userId);

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Movie movie = document.toObject(Movie.class);
                        if (movie != null) {
                            favoritesManager.agregarFavorito(movie.getId(), userId, movie.getApi());
                        }
                    }

                    Toast.makeText(context, "Datos sincronizados desde Firestore", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error al sincronizar desde Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Sincroniza los datos de favoritos desde Firestore para todos los usuarios en la base de datos local.
     */
    public void cargarFavoritosDesdeFirestore() {
        // Obtener la lista de usuarios con favoritos desde Firestore
        firestore.collection("favorites")
                .get()
                .addOnSuccessListener(usuariosSnapshot -> {
                    if (!usuariosSnapshot.isEmpty()) {
                        Log.d("GestorFavoritos", "Se encontraron usuarios con favoritos: " + usuariosSnapshot.size());
                        for (QueryDocumentSnapshot usuarioDocumento : usuariosSnapshot) {
                            String idUsuario = usuarioDocumento.getId(); // Identificador único del usuario
                            Log.d("GestorFavoritos", "Cargando favoritos para el usuario: " + idUsuario);
                            firestore.collection("favorites").document(idUsuario).collection("movies")
                                    .get()
                                    .addOnSuccessListener(peliculasSnapshot -> {
                                        if (!peliculasSnapshot.isEmpty()) {
                                            Log.d("GestorFavoritos", "Películas encontradas para el usuario " + idUsuario + ": " + peliculasSnapshot.size());


                                            for (QueryDocumentSnapshot peliculaDocumento : peliculasSnapshot) {
                                                String idPelicula = peliculaDocumento.getId();
                                                String fuenteApi = peliculaDocumento.getString("api");


                                                // Guardar la película en la base de datos local
                                                favoritesManager.agregarFavorito(idPelicula, idUsuario, fuenteApi);
                                                Log.d("GestorFavoritos", "Pelicula agregada a favoritos: " + idPelicula);
                                            }
                                        } else {
                                            Log.d("GestorFavoritos", "No se encontraron películas para el usuario: " + idUsuario);
                                        }
                                    })
                                    .addOnFailureListener(error -> Log.e("GestorFavoritos", "Error al cargar películas para el usuario: " + idUsuario, error));
                        }
                    } else {
                        Log.w("GestorFavoritos", "No se encontraron usuarios con favoritos.");
                    }
                })
                .addOnFailureListener(error -> Log.e("GestorFavoritos", "Error al obtener la lista de usuarios de Firestore", error));
    }





    /**
     * Sincronizamos los datos de favoritos desde la base de datos local a Firestore lo hemos usado la primera vez para cargar la bbdd al firestore
     */
    public void sincronizarHaciaFirestore() {
        List<Pair<String, String>> favoritos = favoritesManager.obtenerFavoritosIdsYSources(userId);

        for (Pair<String, String> favorito : favoritos) {
            Movie movie = new Movie();
            movie.setId(favorito.first);
            movie.setApi(favorito.second);

            firestore.collection("favorites").document(userId).collection("movies")
                    .document(movie.getId())
                    .set(movie)
                    .addOnFailureListener(e -> Toast.makeText(
                            context,
                            "Error al sincronizar datos hacia Firestore: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show()
                    );
        }
    }

    /**
     * Elimina un favorito tanto de Firestore como de la base de datos local.
     */
    public void eliminarFavorito(Movie movie) {
        // Eliminar de Firestore
        firestore.collection("favorites")
                .document(userId)
                .collection("movies")
                .document(movie.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    System.out.println("eliminado de firestore");
                })
                .addOnFailureListener(e -> {
                    System.out.println("error al eliminar de firestore");

                });

        // Eliminar de la base de datos local
        favoritesManager.eliminarFavorito(movie.getId(), userId);
    }

    /**
     * Añade un favorito tanto a Firestore como a la base de datos local.
     */
    public void agregarFavorito(Movie movie,String user) {
        DocumentReference documentoUsuario = firestore.collection("favorites").document(user);
        documentoUsuario.set(Collections.singletonMap("existe",true), SetOptions.merge());
        // Agregar a Firestore
        firestore.collection("favorites")
                .document(userId)
                .collection("movies")
                .document(movie.getId())
                .set(movie)
                .addOnSuccessListener(aVoid -> {
                    System.out.println("añadido a firestore");

                })
                .addOnFailureListener(e -> {
                    System.out.println("error al añadir al firestore");
                });

        // Agregar a la base de datos local
        favoritesManager.agregarFavorito(movie.getId(), userId, movie.getApi());
    }
}

