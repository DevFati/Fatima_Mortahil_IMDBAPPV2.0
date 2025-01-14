package edu.pmdm.mortahil_fatimaimdbapp.api;


import java.util.List;

import edu.pmdm.mortahil_fatimaimdbapp.models.Genero;
import edu.pmdm.mortahil_fatimaimdbapp.models.Movie;

public interface TMDBApiService {

    /**
     * Con este metodo obtenemos los generos disponibles desde la api tmdb
     * @param generos lista con los generos sacados de la api
     */
    void obtenerGeneros(List<Genero> generos);

    /**
     * metodo para buscar peliculas segun genero y  año
     * @param movies
     */
    void buscarPeliculas(List<Movie> movies);


}
