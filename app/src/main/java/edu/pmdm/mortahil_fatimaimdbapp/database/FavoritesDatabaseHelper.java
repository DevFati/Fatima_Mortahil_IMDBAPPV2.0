package edu.pmdm.mortahil_fatimaimdbapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FavoritesDatabaseHelper extends SQLiteOpenHelper {

    private static final String nomBaseDatos = "favoritos_db"; //Nombre de nuestro archivo de la base de datos
    private static final int versionBD = 10;

    public static final String tablaF = "favoritos"; //Nombre de la tabla de favoritos
    public static final String columna_id = "id"; // ID de la película
    public static final String columna_usuario = "usuario"; // ID del usuario
    public static final String columna_api = "api"; // Aqui indico de donde viene la película, para poder
    //cargar sus datos mas adelante ya que si viene de imb cargare sus datos de una forma,
    //y si viene de tmb usare otra


    //  estructura de la tabla
    private static final String CREAR_TABLAF =
            "CREATE TABLE " + tablaF + " (" +
                    columna_id + " TEXT NOT NULL, " +
                    columna_usuario + " TEXT NOT NULL, " +
                    columna_api + " TEXT, " +
                    "PRIMARY KEY (" + columna_id + ", " + columna_usuario + "));";


    public FavoritesDatabaseHelper(Context context) {
        super(context, nomBaseDatos, null, versionBD);
    }

    // metodo llama
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREAR_TABLAF);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //cambie la clave primary key a clave compuesta para que dos usuarios puedan teenr en favoritos la misma peli
        //y no haya conflicto en esto
        if (oldVersion < 10) {
            db.execSQL("DROP TABLE IF EXISTS " + tablaF + ";");
            onCreate(db); // Llamar a onCreate para recrear la tabla con la nueva estructura
        }
    }
}


