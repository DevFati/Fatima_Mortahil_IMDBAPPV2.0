package edu.pmdm.mortahil_fatimaimdbapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String nomBaseDatos = "favoritos_db"; // Nombre del archivo de la base de datos
    private static final int versionBD = 14; // Incrementar la versión al combinar tablas

    // Tabla `favoritos`
    public static final String tablaF = "favoritos"; // Nombre de la tabla de favoritos
    public static final String columna_id = "id"; // ID de la película
    public static final String columna_usuario = "usuario"; // ID del usuario
    public static final String columna_api = "api"; // API de la película (IMDB/TMDB)

    // Tabla `users`
    public static final String tablaUsers = "users"; // Nombre de la tabla de usuarios
    public static final String columna_user_id = "user_id"; // ID del usuario
    public static final String columna_nombre = "name"; // Nombre del usuario
    public static final String columna_mail = "email"; // Correo del usuario
    public static final String columna_login_time = "login_time"; // Hora de inicio de sesión
    public static final String columna_logout_time = "logout_time"; // Hora de cierre de sesión

    public static final String columna_address = "address"; // Dirección del usuario
    public static final String columna_phone = "phone"; // Teléfono del usuario
    public static final String columna_image = "image"; // Imagen del usuario

    // Script para crear la tabla `favoritos`
    private static final String CREAR_TABLAF =
            "CREATE TABLE " + tablaF + " (" +
                    columna_id + " TEXT NOT NULL, " +
                    columna_usuario + " TEXT NOT NULL, " +
                    columna_api + " TEXT, " +
                    "PRIMARY KEY (" + columna_id + ", " + columna_usuario + "));";

    // Script para crear la tabla `users`
    private static final String CREAR_TABLA_USERS =
            "CREATE TABLE " + tablaUsers + " (" +
                    columna_user_id + " TEXT PRIMARY KEY, " +
                    columna_nombre + " TEXT, " +
                    columna_mail + " TEXT, " +
                    columna_login_time + " TEXT, " +
                    columna_logout_time + " TEXT, " +
                    columna_address + " TEXT, " +
                    columna_phone + " TEXT, " +
                    columna_image + " TEXT);";

    public DatabaseHelper(Context context) {
        super(context, nomBaseDatos, null, versionBD);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREAR_TABLAF);
        db.execSQL(CREAR_TABLA_USERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Reemplazar ambas tablas al incrementar la versión de la base de datos
        db.execSQL("DROP TABLE IF EXISTS " + tablaF);
        db.execSQL("DROP TABLE IF EXISTS " + tablaUsers);
        onCreate(db);
    }
}
