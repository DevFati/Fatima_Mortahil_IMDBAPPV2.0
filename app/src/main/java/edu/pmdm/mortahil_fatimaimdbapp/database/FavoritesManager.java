package edu.pmdm.mortahil_fatimaimdbapp.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;
//aqui gestionamos la base de datos de favoritos
public class FavoritesManager {
    //aqui tenemos la instancia del helper que gestionara la creacion y actualizacion de nuestra bbdd.
    private FavoritesDatabaseHelper baseDatos;
    private SQLiteDatabase db; //nuestro acceso a la bbdd

    public FavoritesManager(Context context) {
        baseDatos = new FavoritesDatabaseHelper(context);
        db = baseDatos.getWritableDatabase(); // Abrimos la conexión al crear la instancia para usarla mas adelante

    }

    // Método para cerrar la conexión cuando ya no la usemos
    public void close() {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    //Método para agregar las peliculas a favorito
    public boolean agregar(String movieId, String userId, String api) {
        SQLiteDatabase db = baseDatos.getWritableDatabase();

        // Aqui lo que hacemos es buscar en la base de datis si la pelicula ya esta guardada para ese usuario.
        Cursor cursor = db.query(FavoritesDatabaseHelper.tablaF,
                null,
                FavoritesDatabaseHelper.columna_id + "=? AND " + FavoritesDatabaseHelper.columna_usuario + "=?",
                new String[]{movieId, userId},
                null, null, null);

        //Si el cursor lanza datos significa que la pelicula ya esta en favoritos
        boolean existe = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }

        if (existe) {
            db.close();
            return false; // Ya existe en la base de datos para este usuario no seguimos y le indicamos al usuario que ya esta en favoritos
        }

        // Si no existe, agregamos la película
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.columna_id, movieId);
        values.put(FavoritesDatabaseHelper.columna_usuario, userId);
        values.put(FavoritesDatabaseHelper.columna_api, api);

        //Lo guardamos en la base de datos
        long resultado = db.insert(FavoritesDatabaseHelper.tablaF, null, values);


        return resultado != -1; // si el resultado es distinto de -1, significa que se guardo bien
    }

//Busco la pelicula de la base de datos para el id del usuario y la borramos solo para ese usuario
    public void eliminarPorUsuario(String movieId, String userId) {
        SQLiteDatabase db = baseDatos.getWritableDatabase();
        try {
            db.delete(FavoritesDatabaseHelper.tablaF,
                    FavoritesDatabaseHelper.columna_id + "=? AND " + FavoritesDatabaseHelper.columna_usuario + "=?",
                    new String[]{movieId, userId});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Con este metodo lo que hacemos es buscar en la base de datos todas las peliculas favoritas de un usuario especifico
    public List<Pair<String, String>> obtenerFavoritosIdsYSources(String userId) {
        List<Pair<String, String>> favoritos = new ArrayList<>();
        //Lo que hago es crear una lista vacia donde gursare los pares (por eso usamos list<pair>>) de los id de las peliculas
        //y de que api son
        SQLiteDatabase db = baseDatos.getReadableDatabase(); //con .getReadable... abrimos la base de datos en modo lectura ya que solo leeremos


        try (Cursor cursor = db.query(FavoritesDatabaseHelper.tablaF, //selecionamos nuestra tabla donde estan los datos
                new String[]{FavoritesDatabaseHelper.columna_id, FavoritesDatabaseHelper.columna_api}, //y seleccionamos las columnas que queremos leer
                FavoritesDatabaseHelper.columna_usuario + "=?", //e indicamos que solo queremos ver las peliculas del usuario actual
                new String[]{userId}, //remplazamos '?' por el id usuario que nos pasen por parametro al metodo
                null, null, null)) { //esto es por si queremos usar groupby, orden, etc.. no lo usamos

            //Vemos que el cursor no este vacio 
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    //Vamos extrayendo los valores de las columnas que hemos seleccionado, el id y la api usada en cada peli 
                    @SuppressLint("Range")
                    String id = cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.columna_id));
                    @SuppressLint("Range")
                    String api = cursor.getString(cursor.getColumnIndex(FavoritesDatabaseHelper.columna_api));

                    favoritos.add(new Pair<>(id, api));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return favoritos; //retornamos la lista de pares con las pelis favoritas de cada usuario
    }
}