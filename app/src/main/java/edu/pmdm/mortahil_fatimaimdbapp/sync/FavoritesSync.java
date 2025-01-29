package edu.pmdm.mortahil_fatimaimdbapp.sync;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

import edu.pmdm.mortahil_fatimaimdbapp.database.FavoritesManager;
import edu.pmdm.mortahil_fatimaimdbapp.models.Movie;

public class FavoritesSync {

    private final FirebaseFirestore firestore;
    private final FavoritesManager favoritesManager;
    private final Context context;
    private final String userId;

    public FavoritesSync(Context context, String userId) {
        this.context = context;
        this.userId = userId;
        this.firestore = FirebaseFirestore.getInstance();
        this.favoritesManager = new FavoritesManager(context);
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
                            favoritesManager.agregar(movie.getId(), userId, movie.getApi());
                        }
                    }

                    Toast.makeText(context, "Datos sincronizados desde Firestore", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error al sincronizar desde Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
        favoritesManager.eliminarPorUsuario(movie.getId(), userId);
    }

    /**
     * Añade un favorito tanto a Firestore como a la base de datos local.
     */
    public void agregarFavorito(Movie movie) {
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
        favoritesManager.agregar(movie.getId(), userId, movie.getApi());
    }
}

