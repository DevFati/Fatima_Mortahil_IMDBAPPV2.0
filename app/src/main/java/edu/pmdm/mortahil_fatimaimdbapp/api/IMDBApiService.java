package edu.pmdm.mortahil_fatimaimdbapp.api;
import java.util.List;
import edu.pmdm.mortahil_fatimaimdbapp.models.Movie;


public interface IMDBApiService {
    /**
     * Método para obtener la lista de películas populares (las top 10)
     @param movies Lista de películas populares obtenidas de la API.
      *Lo hacemos de esta forma para recibir los resultados desde un callback
     * para ejecutarlo de manera asíncrona.
     */
    void cargarpeliculas(List<Movie> movies);

    /**
     * Método para obtener los detalles de una película específica de la api imdb
     *
     * @param descripcion la descripcion de la pelicula
     * usamos esta forma por la misma razon que la de arriba, ya que al trabajar con peticiones de red,
     * puede tardar un tiempo en cargarse todos los datos. Asi que nos permite no cargar nada hasta tener todos los datos
     *
     */
    void descripcion(String descripcion);
}

