package edu.pmdm.mortahil_fatimaimdbapp.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import androidx.core.util.Pair;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pmdm.mortahil_fatimaimdbapp.KeystoreManager;
import edu.pmdm.mortahil_fatimaimdbapp.models.User;
import edu.pmdm.mortahil_fatimaimdbapp.sync.UsersSync;

public class DatabaseManager {

    private SQLiteDatabase db; // Conexión a la base de datos
    private DatabaseHelper dbHelper; // Helper que gestiona la base de datos combinada
    private KeystoreManager keystoreManager; // Manejador de cifrado

    public DatabaseManager(Context context) {
        dbHelper = new DatabaseHelper(context); // Usamos el helper combinado
        db = dbHelper.getWritableDatabase(); // Abrimos la conexión a la base de datos
        keystoreManager = new KeystoreManager(); // Inicializamos el manejador de cifrado

    }

    public void close() {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    // ----------------------- Gestión de Favoritos -----------------------
    public boolean agregarFavorito(String movieId, String userId, String api) {
        // Comprobamos si el favorito ya existe
        Cursor cursor = db.query(
                DatabaseHelper.tablaF, // Tabla favoritos
                null,
                DatabaseHelper.columna_id + "=? AND " + DatabaseHelper.columna_usuario + "=?",
                new String[]{movieId, userId},
                null, null, null
        );

        boolean existe = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();

        if (existe) {
            return false; // Ya existe, no hacemos nada
        }

        // Si no existe, lo insertamos
        ContentValues valores = new ContentValues();
        valores.put(DatabaseHelper.columna_id, movieId);
        valores.put(DatabaseHelper.columna_usuario, userId);
        valores.put(DatabaseHelper.columna_api, api);

        long resultado = db.insert(DatabaseHelper.tablaF, null, valores);
        return resultado != -1; // Si es distinto de -1, la operación fue exitosa
    }

    public void eliminarFavorito(String movieId, String userId) {
        db.delete(
                DatabaseHelper.tablaF,
                DatabaseHelper.columna_id + "=? AND " + DatabaseHelper.columna_usuario + "=?",
                new String[]{movieId, userId}
        );
    }







    public List<Pair<String, String>> obtenerFavoritosIdsYSources(String userId) {
        List<Pair<String, String>> favoritos = new ArrayList<>();

        Cursor cursor = db.query(
                DatabaseHelper.tablaF,
                new String[]{DatabaseHelper.columna_id, DatabaseHelper.columna_api},
                DatabaseHelper.columna_usuario + "=?",
                new String[]{userId},
                null, null, null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String id = cursor.getString(cursor.getColumnIndex(DatabaseHelper.columna_id));
                @SuppressLint("Range") String api = cursor.getString(cursor.getColumnIndex(DatabaseHelper.columna_api));
                favoritos.add(new Pair<>(id, api));
            }
            cursor.close();
        }

        return favoritos;
    }

    public void limpiarFavoritosUsuario(String userId) {
        db.delete(
                DatabaseHelper.tablaF,
                DatabaseHelper.columna_usuario + "=?",
                new String[]{userId}
        );
    }

    // ----------------------- Gestión de Usuarios -----------------------
    public void guardarUsuario(User usuario) {
        try {
            ContentValues valores = new ContentValues();
            valores.put(DatabaseHelper.columna_user_id, usuario.getId());
            valores.put(DatabaseHelper.columna_nombre, usuario.getNombre());
            valores.put(DatabaseHelper.columna_mail, usuario.getCorreo());

            valores.put(DatabaseHelper.columna_address, usuario.getAddress());
            valores.put(DatabaseHelper.columna_phone, usuario.getPhone());
            valores.put(DatabaseHelper.columna_image, usuario.getImage());
            valores.put(DatabaseHelper.columna_login_time, usuario.getUltimoLogin());
            valores.put(DatabaseHelper.columna_logout_time, usuario.getUltimoLogout());



            db.insertWithOnConflict(
                    DatabaseHelper.tablaUsers,
                    null,
                    valores,
                    SQLiteDatabase.CONFLICT_REPLACE

            );
            Log.d("DatabaseManager", "Usuario guardado: " + usuario.getId() + ", imagen: " + usuario.getImage());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public User obtenerUsuarioPorId(String idUsuario) {
        Cursor cursor = db.query(
                DatabaseHelper.tablaUsers,
                null,
                DatabaseHelper.columna_user_id + "=?",
                new String[]{idUsuario},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            try {
                // Descifrar dirección y teléfono al leerlos


                User usuario = new User(
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_user_id)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_nombre)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_mail)),

                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_address)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_phone)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_image)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_login_time)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_logout_time))
                );

                cursor.close();
                return usuario;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public void actualizarUltimoLogout(String idUsuario, String ultimoLogout) {
        ContentValues valores = new ContentValues();
        valores.put(DatabaseHelper.columna_logout_time, ultimoLogout);

        db.update(
                DatabaseHelper.tablaUsers,
                valores,
                DatabaseHelper.columna_user_id + "=?",
                new String[]{idUsuario}
        );
    }

    public List<User> obtenerTodosLosUsuarios() {
        List<User> usuarios = new ArrayList<>();
        Cursor cursor = db.query(
                DatabaseHelper.tablaUsers, // Nombre de la tabla
                null, // Todas las columnas
                null, // No hay cláusula WHERE, queremos todos los registros
                null, // No hay valores para reemplazar en el WHERE
                null, // No agrupamos resultados
                null, // No filtramos por grupo
                null  // No hay orden específico
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {

                    User usuario = new User(
                            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_user_id)),
                            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_nombre)),
                            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_mail)),
                            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_phone)),
                            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_address)),
                            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_image)),
                            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_login_time)),
                            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.columna_logout_time))
                    );
                    usuarios.add(usuario);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        return usuarios;
    }


    public void actualizarDatosUsuario(String userId, String nombre, String telefono, String direccion, String imagen) {
        try {
            Log.d("DatabaseManager","entraaa" );

            ContentValues valores = new ContentValues();

            // Solo actualizamos los datos especificados
            if (nombre != null && !nombre.isEmpty()) {
                valores.put(DatabaseHelper.columna_nombre, nombre);
            }
            if (telefono != null && !telefono.isEmpty()) {
                valores.put(DatabaseHelper.columna_phone, telefono);
            }
            if (direccion != null && !direccion.isEmpty()) {
                valores.put(DatabaseHelper.columna_address, direccion);
            }
            if (imagen != null && !imagen.isEmpty()) {
                valores.put(DatabaseHelper.columna_image, imagen);
            }

            // Actualizamos la tabla users donde el ID coincida
            db.update(
                    DatabaseHelper.tablaUsers,
                    valores,
                    DatabaseHelper.columna_user_id + "=?",
                    new String[]{userId}
            );



            // Sincronizar con Firebase
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentReference userRef = firestore.collection("users").document(userId);

            Map<String, Object> datosActualizados = new HashMap<>();
            if (nombre != null) datosActualizados.put("name", nombre);
            if (telefono != null) datosActualizados.put("phone", telefono);
            if (direccion != null) datosActualizados.put("address", direccion);
            if (imagen != null) datosActualizados.put("image", imagen);


            userRef.set(datosActualizados, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("FirebaseUpdate", "Datos sincronizados con Firebase para el usuario: " + userId))
                    .addOnFailureListener(e -> Log.e("FirebaseUpdate", "Error al sincronizar con Firebase para el usuario: " + userId, e));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}