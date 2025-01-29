package edu.pmdm.mortahil_fatimaimdbapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class UserDatabaseHelper extends SQLiteOpenHelper {

    private static final String nombreBaseDatos = "users_db";
    private static final int versionBD = 1;

    public static final String tablaUsers = "users";
    public static final String columna_id = "user_id";
    public static final String columna_nombre = "name";
    public static final String columna_mail = "email";
    public static final String columna_LOGIN_TIME = "login_time";
    public static final String columna_LOGOUT_TIME = "logout_time";

    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + tablaUsers + " (" +
                    columna_id + " TEXT PRIMARY KEY, " +
                    columna_nombre + " TEXT, " +
                    columna_mail + " TEXT, " +
                    columna_LOGIN_TIME + " TEXT, " +
                    columna_LOGOUT_TIME + " TEXT);";

    public UserDatabaseHelper(Context context) {
        super(context, nombreBaseDatos, null, versionBD);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + tablaUsers);
            onCreate(db);
            Log.d("UserDatabaseHelper", "Base de datos actualizada de versión " + oldVersion + " a " + newVersion);
        } catch (Exception e) {
            Log.e("UserDatabaseHelper", "Error al actualizar la base de datos: " + e.getMessage(), e);
        }
    }
}
