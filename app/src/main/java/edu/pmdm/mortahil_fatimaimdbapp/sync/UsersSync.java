package edu.pmdm.mortahil_fatimaimdbapp.sync;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import edu.pmdm.mortahil_fatimaimdbapp.database.DatabaseManager;
import edu.pmdm.mortahil_fatimaimdbapp.models.User;

public class UsersSync {

    private static final String TAG = "UsersSync";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore firestore;
    private final DatabaseManager databaseManager; // Para gestionar la sincronización local
    private final CollectionReference usersCollection;

    public UsersSync(Context context) {
        firestore = FirebaseFirestore.getInstance();
        usersCollection = firestore.collection(COLLECTION_USERS);
        databaseManager = new DatabaseManager(context); // Base de datos local
    }

    // Sincroniza los datos locales hacia Firebase
    public void sincronizarHaciaFirebase() {
        for (User user : databaseManager.obtenerTodosLosUsuarios()) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("user_id", user.getId());
            userData.put("name", user.getNombre());
            userData.put("email", user.getCorreo());

            Map<String, Object> activityLog = new HashMap<>();
            activityLog.put("login_time", user.getUltimoLogin());
            activityLog.put("logout_time", user.getUltimoLogout());

            // Agregar el log de actividad
            usersCollection.document(user.getId())
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Usuario sincronizado con Firebase: " + user.getId()))
                    .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar usuario con Firebase: " + user.getId(), e));

            usersCollection.document(user.getId())
                    .collection("activity_log")
                    .add(activityLog)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Log de actividad sincronizado: " + user.getId()))
                    .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar log de actividad: " + user.getId(), e));
        }
    }

    // Sincroniza los datos desde Firebase hacia la base de datos local
    public void sincronizarDesdeFirebase() {
        usersCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot snapshot = task.getResult();
                if (snapshot != null && !snapshot.isEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        snapshot.forEach(document -> {
                            Map<String, Object> userData = document.getData();
                            String userId = (String) userData.get("user_id");
                            String name = (String) userData.get("name");
                            String email = (String) userData.get("email");

                            // Extraer el log de actividad
                            usersCollection.document(userId).collection("activity_log")
                                    .get()
                                    .addOnSuccessListener(activitySnapshot -> {
                                        if (activitySnapshot != null && !activitySnapshot.isEmpty()) {
                                            activitySnapshot.forEach(activityDoc -> {
                                                String loginTime = (String) activityDoc.get("login_time");
                                                String logoutTime = (String) activityDoc.get("logout_time");

                                                // Guardar en la base de datos local
                                                User user = new User(userId, name, email, loginTime, logoutTime);
                                                databaseManager.guardarUsuario(user);
                                            });
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Error al obtener el log de actividad: " + userId, e));
                        });
                    }
                }
            } else {
                Log.e(TAG, "Error al sincronizar desde Firebase", task.getException());
            }
        });
    }

//Estoy creando este metodo que me permitira subir tanto el login y el logout de la actividad, sin subir datos nulos.

    public void registrarActividad(String userId, String loginTime, String logoutTime) {
        if (userId == null || userId.isEmpty()) return;

        Map<String, Object> activityLog = new HashMap<>();
        activityLog.put("login_time", loginTime);
        activityLog.put("logout_time", logoutTime);

        // Creamos un nuevo documento en la subcolección activity_log
        usersCollection.document(userId).collection("activity_log")
                .add(activityLog)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Registro de actividad añadido a Firebase para usuario: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Error al añadir registro de actividad para usuario: " + userId, e));
    }


    public void subirRegistroAFirebase(User user) {
        if (user == null) return;

        Map<String, Object> activityLog = new HashMap<>();
        activityLog.put("login_time", user.getUltimoLogin());
        activityLog.put("logout_time", user.getUltimoLogout());

        // Subir el registro completo a Firebase
        usersCollection.document(user.getId())
                .collection("activity_log")
                .add(activityLog)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Registro sincronizado con Firebase para usuario: " + user.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar registro con Firebase para usuario: " + user.getId(), e));
    }


    public void sincronizarRegistro(String userId, Map<String, Object> userData, Map<String, Object> activityLog) {
        // Actualizamos la información principal del usuario
        usersCollection.document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Usuario sincronizado con Firebase: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar usuario con Firebase: " + userId, e));

        // Agregamos el registro de actividad
        usersCollection.document(userId)
                .collection("activity_log")
                .add(activityLog)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Log de actividad sincronizado: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar log de actividad: " + userId, e));
    }



}
