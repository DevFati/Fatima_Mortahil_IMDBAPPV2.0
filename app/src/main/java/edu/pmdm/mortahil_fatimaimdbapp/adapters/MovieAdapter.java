package edu.pmdm.mortahil_fatimaimdbapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import edu.pmdm.mortahil_fatimaimdbapp.MovieDetailsActivity;
import edu.pmdm.mortahil_fatimaimdbapp.api.IMDBApiService;
import edu.pmdm.mortahil_fatimaimdbapp.database.DatabaseManager;
import edu.pmdm.mortahil_fatimaimdbapp.databinding.ItemMovieBinding;
import edu.pmdm.mortahil_fatimaimdbapp.models.Movie;
import edu.pmdm.mortahil_fatimaimdbapp.models.MovieOverviewResponse;
import edu.pmdm.mortahil_fatimaimdbapp.models.MovieSearchResponse;
import edu.pmdm.mortahil_fatimaimdbapp.sync.FavoritesSync;


public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private List<Movie> peliculas; //lista de peliculas
    private Context context; //contexto
    private boolean estamosEnFavoritos; //usamos este boolean para indicar cuando el adaptador se use en
    //el fragment de favoritos
    private FavoritesSync favSync;


    public MovieAdapter(List<Movie> peliculas, Context context, boolean estamosEnFavoritos, FavoritesSync favSync) {
        this.peliculas = peliculas;
        this.context = context;
        this.estamosEnFavoritos = estamosEnFavoritos;
        this.favSync = favSync; // Inicializamos aquí


    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMovieBinding binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MovieViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        //Obtenemos la pelicula en la posicion actual y llamamos al metodo "bind" del ViewHolder
        Movie movie = peliculas.get(position);
        holder.bind(movie, position);
    }

    @Override
    public int getItemCount() {

        return peliculas.size();
    }

    class MovieViewHolder extends RecyclerView.ViewHolder {
        private final ItemMovieBinding binding;

        public MovieViewHolder(@NonNull ItemMovieBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Movie movie, int position) {
            // Mostrar la imagen de la película usando Glide
            Glide.with(binding.imageViewMovie.getContext())
                    .load(movie.getUrlImagen())
                    .into(binding.imageViewMovie);

            // Configuración del onClickListener
            binding.getRoot().setOnClickListener(v -> {
                new Thread(() -> {
                    // Obtener descripción dependiendo de la API de cada pelicula
                    if ("imdb".equals(movie.getApi())) {
                        // Recuperar descripción para IMDb
                        MovieOverviewResponse.descripcionPelicula(movie.getId(), new IMDBApiService() {
                            @Override
                            public void cargarpeliculas(List<Movie> movies) {
                                // No lo usamos aqui
                            }

                            @Override
                            public void descripcion(String descripcion) {
                                // Actualizar la descripción en el objeto Movie
                                movie.setDescripcion(descripcion);

                                // vamos a la actividad de detalles que esta en el hilo principal
                                ((AppCompatActivity) context).runOnUiThread(() -> {
                                    Intent intent = new Intent(context, MovieDetailsActivity.class);
                                    intent.putExtra("movie", movie);
                                    context.startActivity(intent);
                                });
                            }
                        });
                    } else if ("tmdb".equals(movie.getApi())) {
                        // Manejo de descripción para TMDb
                        MovieSearchResponse movieSearchResponse = new MovieSearchResponse();
                        Movie updatedMovie = movieSearchResponse.obtenerDetallesPelicula(movie.getId());
                        if (updatedMovie != null) {
                            movie.setDescripcion(updatedMovie.getDescripcion());
                        }

                        // vamos a la actividad de detalles
                        ((AppCompatActivity) context).runOnUiThread(() -> {
                            Intent intent = new Intent(context, MovieDetailsActivity.class);
                            intent.putExtra("movie", movie);
                            context.startActivity(intent);
                        });
                    }



                }).start(); //ejecutamos la lógica en un hilo separado para no bloquear la interfaz haciendolo directamente
                //en el hilo principal
            });

            // Configuración del onLongClickListener para eliminar al pulsar longclick en favoritos
            if (estamosEnFavoritos) {
                binding.getRoot().setOnLongClickListener(v -> {
                    String userId = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            .getString("USER_ID", null);


                    DatabaseManager gestorFavoritos = new DatabaseManager(context);
                    gestorFavoritos.eliminarFavorito(movie.getId(), userId);

                    // Eliminar de la lista y notificar al adaptador
                    peliculas.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, peliculas.size());
                    favSync.eliminarFavorito(movie);
                    Toast.makeText(context, "Eliminado de favoritos: " + movie.getTitulo(), Toast.LENGTH_SHORT).show();

                    return true;
                });
            } else {
                // Configuración del onLongClickListener para agregar a favoritos al pulsar longclick en cualquier otra actividad o fragment que no sea el de favoritos
                binding.getRoot().setOnLongClickListener(v -> {
                    String userId = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            .getString("USER_ID", null);

                    DatabaseManager gestorFavoritos = new DatabaseManager(context);
                    boolean agregado = gestorFavoritos.agregarFavorito(movie.getId(), userId, movie.getApi());
                    if (agregado) {
                        favSync.agregarFavorito(movie,userId);
                        Toast.makeText(context, "Agregada a favoritos: " + movie.getTitulo(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Ya está en tus favoritos.", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
            }

        }
    }
}