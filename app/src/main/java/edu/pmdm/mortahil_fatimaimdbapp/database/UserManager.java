package edu.pmdm.mortahil_fatimaimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import edu.pmdm.mortahil_fatimaimdbapp.models.User;

public class UserManager {

    private SQLiteDatabase baseDatos;

    public UserManager(Context contexto) {
        UserDatabaseHelper helper = new UserDatabaseHelper(contexto);
        baseDatos = helper.getWritableDatabase(); // Abrimos la base de datos para escritura
    }

    public void cerrar() {
        baseDatos.close(); // Cerramos la conexión a la base de datos
    }

    // Guardamos o actualizamos el usuario en la base de datos
    public void guardarUsuario(User usuario) {
        ContentValues valores = new ContentValues();
        valores.put(UserDatabaseHelper.columna_id, usuario.getId());
        valores.put(UserDatabaseHelper.columna_nombre, usuario.getNombre());
        valores.put(UserDatabaseHelper.columna_mail, usuario.getCorreo());
        valores.put(UserDatabaseHelper.columna_LOGIN_TIME, usuario.getUltimoLogin());
        valores.put(UserDatabaseHelper.columna_LOGOUT_TIME, usuario.getUltimoLogout());

        // Guardamos el usuario, reemplazando si ya existe
        baseDatos.insertWithOnConflict(UserDatabaseHelper.tablaUsers, null, valores, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Obtenemos el usuario por su ID
    public User obtenerUsuarioPorId(String idUsuario) {
        Cursor cursor = baseDatos.query(
                UserDatabaseHelper.tablaUsers,
                null,
                UserDatabaseHelper.columna_id + " = ?",
                new String[]{idUsuario},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            User usuario = new User(
                    cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.columna_id)),
                    cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.columna_nombre)),
                    cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.columna_mail)),
                    cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.columna_LOGIN_TIME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.columna_LOGOUT_TIME))
            );
            cursor.close();
            return usuario;
        }
        return null;
    }

    // Actualizamos el último tiempo de logout del usuario
    public void actualizarUltimoLogout(String idUsuario, String ultimoLogout) {
        ContentValues valores = new ContentValues();
        valores.put(UserDatabaseHelper.columna_LOGOUT_TIME, ultimoLogout);

        baseDatos.update(UserDatabaseHelper.tablaUsers, valores, UserDatabaseHelper.columna_id + " = ?", new String[]{idUsuario});
    }
}

