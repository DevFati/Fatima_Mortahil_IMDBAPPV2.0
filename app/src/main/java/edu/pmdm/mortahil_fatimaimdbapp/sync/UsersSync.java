package edu.pmdm.mortahil_fatimaimdbapp.sync;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import edu.pmdm.mortahil_fatimaimdbapp.KeystoreManager;
import edu.pmdm.mortahil_fatimaimdbapp.database.DatabaseManager;
import edu.pmdm.mortahil_fatimaimdbapp.models.User;

public class UsersSync {

    private static final String TAG = "UsersSync";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore firestore;
    private final DatabaseManager databaseManager; // Para gestionar la sincronización local
    private final CollectionReference usersCollection;
    private final KeystoreManager keystoreManager; // Gestor de cifrado

    public UsersSync(Context context) {
        firestore = FirebaseFirestore.getInstance();
        usersCollection = firestore.collection(COLLECTION_USERS);
        databaseManager = new DatabaseManager(context); // Base de datos local
        keystoreManager = new KeystoreManager(); // Inicializar el gestor de cifrado

    }

    // Sincroniza los datos locales hacia Firebase
    public void sincronizarHaciaFirebase() {
        for (User user : databaseManager.obtenerTodosLosUsuarios()) {
            try {


                Map<String, Object> userData = new HashMap<>();
                userData.put("user_id", user.getId());
                userData.put("name", user.getNombre() != null ? user.getNombre() : "");
                userData.put("email", user.getCorreo() != null ? user.getCorreo() : "");
                userData.put("address", user.getAddress() != null ? user.getAddress() : "");
                userData.put("phone", user.getPhone() != null ? user.getPhone() : "");
                userData.put("image", user.getImage() != null ? user.getImage() : "");


                Map<String, Object> activityLog = new HashMap<>();
                activityLog.put("login_time", user.getUltimoLogin() != null ? user.getUltimoLogin() : "");
                activityLog.put("logout_time", user.getUltimoLogout() != null ? user.getUltimoLogout() : "");

                // Subir usuario a Firebase
                usersCollection.document(user.getId())
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Usuario sincronizado con Firebase: " + user.getId()))
                        .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar usuario con Firebase: " + user.getId(), e));

                // Subir log de actividad
                usersCollection.document(user.getId())
                        .collection("activity_log")
                        .add(activityLog)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Log de actividad sincronizado: " + user.getId()))
                        .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar log de actividad: " + user.getId(), e));
            } catch (Exception e) {
                Log.e(TAG, "Error al cifrar datos del usuario: " + user.getId(), e);
            }
        }
    }


    // Sincroniza los datos desde Firebase hacia la base de datos local de un solo usuario
    public void sincronizarDesdeFirebase(String userId) {
        usersCollection.document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            // Leer datos desde Firebase
                            String nombre = documentSnapshot.getString("name");
                            String telefonoCifrado = documentSnapshot.getString("phone");
                            String direccionCifrada = documentSnapshot.getString("address");
                            String imagen = documentSnapshot.getString("image");

                            // Actualizar la base de datos local
                            databaseManager.actualizarDatosUsuario(
                                    userId,
                                    nombre,
                                    telefonoCifrado,
                                    direccionCifrada,
                                    imagen
                            );
                            Log.d("UsersSync", "Sincronización exitosa desde Firebase para el usuario: " + userId);
                        } catch (Exception e) {
                            Log.e("UsersSync", "Error al sincronizar datos desde Firebase para el usuario: " + userId, e);
                        }
                    } else {
                        Log.w("UsersSync", "El documento no existe en Firebase para el usuario: " + userId);
                    }
                })
                .addOnFailureListener(e -> Log.e("UsersSync", "Error al obtener datos de Firebase", e));
    }
    // Sincroniza todos los usuarios desde Firebase hacia la base de datos local
    public void sincronizarTodosDesdeFirebase() {
        usersCollection.get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                            try {
                                // Obtener los datos del usuario desde Firestore
                                String userId = documentSnapshot.getId();
                                String nombre = documentSnapshot.getString("name");
                                String correo = documentSnapshot.getString("email");
                                String direccion = documentSnapshot.getString("address");
                                String telefono = documentSnapshot.getString("phone");
                                String imagen = documentSnapshot.getString("image");

                                // Validar campos necesarios
                                if (userId != null ) {
                                    // Verificar si el usuario ya existe en la base local
                                    User usuarioExistente = databaseManager.obtenerUsuarioPorId(userId);

                                    if (usuarioExistente == null) {
                                        // Si no existe, lo crea
                                        User nuevoUsuario = new User(
                                                userId,
                                                nombre,
                                                correo,
                                                direccion != null ? direccion : "",
                                                telefono != null ? telefono : "",
                                                imagen != null ? imagen : "",
                                                "", // Último login (vacío inicialmente)
                                                ""  // Último logout (vacío inicialmente)
                                        );
                                        databaseManager.guardarUsuario(nuevoUsuario);
                                        Log.d("UsersSync", "Usuario creado: " + userId);
                                    } else {
                                        // Si existe, lo actualiza
                                        databaseManager.actualizarDatosUsuario(
                                                userId,
                                                nombre,
                                                telefono != null ? telefono : "",
                                                direccion != null ? direccion : "",
                                                imagen != null ? imagen : ""
                                        );
                                        Log.d("UsersSync", "Usuario actualizado: " + userId);
                                    }
                                } else {
                                    Log.w("UsersSync", "Datos incompletos para usuario: " + userId);
                                }
                            } catch (Exception e) {
                                Log.e("UsersSync", "Error al sincronizar usuario desde Firebase", e);
                            }
                        }
                        Log.d("UsersSync", "Sincronización completada para todos los usuarios.");
                    } else {
                        Log.w("UsersSync", "No hay usuarios en Firebase para sincronizar.");
                    }
                })
                .addOnFailureListener(e -> Log.e("UsersSync", "Error al obtener usuarios desde Firebase", e));
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


    public void subirRegistroAFirebase(String userId, Map<String, Object> activityLog) {
        if (userId == null || userId.isEmpty()) return;

        // Agregar nuevo registro de login en activity_log
        usersCollection.document(userId)
                .collection("activity_log")
                .add(activityLog)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Nuevo registro de login en Firestore para: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Error al subir registro de login para usuario: " + userId, e));
    }



    public void sincronizarRegistro(String userId, Map<String, Object> activityLog) {


        // Agregamos el registro de actividad
        usersCollection.document(userId)
                .collection("activity_log")
                .add(activityLog)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Log de actividad sincronizado: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar log de actividad: " + userId, e));
    }


    public void agregarUsuarioFirebase(User usuario){
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference documentReference=firestore.collection(COLLECTION_USERS).document(usuario.getId());
        Map<String, Object> mapaUsuario=new HashMap<>();

        mapaUsuario.put("user_id",usuario.getId());
        mapaUsuario.put("name",usuario.getNombre());
        mapaUsuario.put("email",usuario.getCorreo());
        mapaUsuario.put("address",usuario.getAddress());
        mapaUsuario.put("phone",usuario.getPhone());
        mapaUsuario.put("image",usuario.getImage());

        documentReference.set(mapaUsuario);

    }


    public void actualizarLogoutFirebase(String userId, String logoutTime) {
        if (userId == null || userId.isEmpty()) return;

        CollectionReference activityLogRef = usersCollection.document(userId).collection("activity_log");

        // Obtener el último login y actualizar su logout_time
        activityLogRef.orderBy("login_time", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot lastLoginDoc = querySnapshot.getDocuments().get(0);
                        lastLoginDoc.getReference().update("logout_time", logoutTime)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Logout actualizado en Firestore para: " + userId))
                                .addOnFailureListener(e -> Log.e(TAG, "Error al actualizar logout en Firestore para: " + userId, e));
                    } else {
                        Log.w(TAG, "No se encontró un login previo para actualizar el logout en Firestore");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al obtener último login para actualizar logout en Firestore", e));
    }







}