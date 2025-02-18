package edu.pmdm.mortahil_fatimaimdbapp.utils;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import edu.pmdm.mortahil_fatimaimdbapp.database.DatabaseHelper;
import edu.pmdm.mortahil_fatimaimdbapp.database.DatabaseManager;
import edu.pmdm.mortahil_fatimaimdbapp.models.User;
import edu.pmdm.mortahil_fatimaimdbapp.sync.UsersSync;

public class AppLifecycleManager implements LifecycleObserver {

    private static final String PREF_NAME = "UserPrefs";
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    private static final int LOGOUT_DELAY = 3000; // 3 segundos de espera para logout
    private static final String TAG = "AppLifecycleManager";

    private boolean isInBackground = false;
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;

    private final Context context;
    private final DatabaseHelper databaseHelper;
    private final Handler logoutHandler = new Handler();
    private UsersSync usersSync;
    private DatabaseManager databaseManager;



    private final Runnable logoutRunnable = () -> {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            registrarLogout(user);
            actualizarEstadoSesion(false);
            Log.d(TAG, "Logout registrado por inactividad.");
        }
    };

    public AppLifecycleManager(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
        this.usersSync = new UsersSync(context); // Inicialización de UsersSync
        this.databaseManager=new DatabaseManager(context);

    }

    @OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
    public void onAppForeground() {
        isInBackground = false;
        logoutHandler.removeCallbacks(logoutRunnable); // Cancelar cualquier logout programado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            registrarLogin(user);
            actualizarEstadoSesion(true);
            Log.d(TAG, "Usuario activo en primer plano.");
        }
    }

    @OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_STOP)
    public void onAppBackground() {
        if (!isActivityChangingConfigurations) {
            isInBackground = true;
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                registrarLogout(user);
                actualizarEstadoSesion(false);
                Log.d(TAG, "Logout registrado al mover la app a segundo plano.");
            }
        }
    }

    public void onPause() {
        // Si la actividad se pausa (por ejemplo, al cambiar de actividad o cerrar temporalmente), programamos un logout.
        logoutHandler.postDelayed(logoutRunnable, LOGOUT_DELAY);
        Log.d(TAG, "La aplicación está en pausa. Programando logout.");
    }

    public void onResume() {
        // Cancelamos el logout cuando se vuelve a la actividad principal.
        logoutHandler.removeCallbacks(logoutRunnable);
        Log.d(TAG, "La aplicación ha vuelto a estar activa.");
    }

    public void onTrimMemory(int level) {
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                registrarLogout(user);
                actualizarEstadoSesion(false);
                Log.d(TAG, "Logout registrado al minimizar la aplicación.");
            }
        }
    }

    public void checkForPendingLogout() {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean wasLoggedIn = preferences.getBoolean(PREF_IS_LOGGED_IN, false);

        if (wasLoggedIn) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            registrarLogout(currentUser);
            actualizarEstadoSesion(false);
            Log.d(TAG, "Logout pendiente registrado al reiniciar la app.");
        }
    }

    private void registrarLogin(FirebaseUser user) {
        if (user == null) return;

        String fechaActual = obtenerFechaActual();
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        try {
            // Guardar en SQLite
            db.execSQL("UPDATE users SET login_time = ? WHERE user_id = ?", new Object[]{fechaActual, user.getUid()});
            Log.d(TAG, "Login registrado en SQLite: " + fechaActual);

            // Guardar en Firestore con logout_time como null
            Map<String, Object> activityLog = new HashMap<>();
            activityLog.put("login_time", fechaActual);
            activityLog.put("logout_time", null); // Se deja como null al iniciar sesión

            usersSync.subirRegistroAFirebase(user.getUid(), activityLog);
            Log.d(TAG, "Login registrado en Firestore con logout_time=null");
        } catch (Exception e) {
            Log.e(TAG, "Error al registrar login: " + e.getMessage(), e);
        }
    }

    public void registrarLogout(FirebaseUser user) {
        if (user == null) return;

        String fechaActual = obtenerFechaActual();
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        try {
            // Actualizar logout_time en SQLite
            db.execSQL("UPDATE users SET logout_time = ? WHERE user_id = ?", new Object[]{fechaActual, user.getUid()});
            Log.d(TAG, "Logout registrado en SQLite: " + fechaActual);

            // Buscar el último login en Firestore y actualizarlo con el logout_time
            usersSync.actualizarLogoutFirebase(user.getUid(), fechaActual);
            Log.d(TAG, "Logout actualizado en Firestore");
        } catch (Exception e) {
            Log.e(TAG, "Error al registrar logout: " + e.getMessage(), e);
        }
    }



    private void actualizarEstadoSesion(boolean isLoggedIn) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }

    private String obtenerFechaActual() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }


    private void sincronizarRegistroConFirebase(String userId) {
        try {
            // Consultamos el registro completo del usuario
            User usuario=databaseManager.obtenerUsuarioPorId(userId);
                Map<String, Object> activityLog = new HashMap<>();
                activityLog.put("login_time", usuario.getUltimoLogin());
                activityLog.put("logout_time", usuario.getUltimoLogout());

                // Enviamos el registro a Firebase
                UsersSync usersSync = new UsersSync(context);
                usersSync.sincronizarRegistro(userId, activityLog);


        } catch (Exception e) {
            Log.e(TAG, "Error al sincronizar el registro con Firebase: " + e.getMessage(), e);
        }
    }

}
