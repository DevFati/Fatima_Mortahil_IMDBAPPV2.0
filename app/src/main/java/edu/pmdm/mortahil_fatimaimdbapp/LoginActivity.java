package edu.pmdm.mortahil_fatimaimdbapp;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import edu.pmdm.mortahil_fatimaimdbapp.database.DatabaseManager;
import edu.pmdm.mortahil_fatimaimdbapp.databinding.ActivityLogInBinding;
import edu.pmdm.mortahil_fatimaimdbapp.models.User;
import edu.pmdm.mortahil_fatimaimdbapp.sync.FavoritesSync;
import edu.pmdm.mortahil_fatimaimdbapp.sync.UsersSync;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth autenticacion;
    private GoogleSignInClient clienteGoogle;
    private ActivityLogInBinding enlaceVista;
    private CallbackManager mCallbackManager;
    private UsersSync usersSync;
    private FavoritesSync favSync;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        enlaceVista = ActivityLogInBinding.inflate(getLayoutInflater());

        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        EdgeToEdge.enable(this);
        setContentView(enlaceVista.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        enlaceVista.imageView2.setImageResource(R.drawable.logo);
        favSync = new FavoritesSync(this);
//favSync.sincronizarHaciaFirestore();
        favSync.cargarFavoritosDesdeFirestore();
        usersSync = new UsersSync(this);
        //usersSync.sincronizarHaciaFirebase();
        usersSync.sincronizarTodosDesdeFirebase();

        // Verificar si el usuario ya inició sesión porque si lo esta ya, pasa directamente a la pantalla Main
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // Si el usuario ya está autenticado, redirige a MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return; // Salimos de onCreate para que no cargue más cosas de esta actividad
        }

        // Configurar FirebaseAuth
        autenticacion = FirebaseAuth.getInstance();

        // Configurar GoogleSignInClient
        GoogleSignInOptions opciones = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id)) // el ID esta en strings.xml
                .requestEmail()
                .build();
        clienteGoogle = GoogleSignIn.getClient(this, opciones);

        // Configurar el botón de inicio de sesión
        enlaceVista.signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentInicioSesion = clienteGoogle.getSignInIntent();
                lanzadorResultadoActividad.launch(intentInicioSesion);
            }
        });


        // Inicializar el SDK de Facebook

        AppEventsLogger.activateApp(getApplication());
        mCallbackManager = CallbackManager.Factory.create();

        LoginButton loginButton = enlaceVista.loginButtonFacebook;
        loginButton.setReadPermissions("email", "public_profile");

        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(LoginActivity.this, "Error al iniciar sesión con Facebook: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT);
                Log.d("KeyHash:", keyHash);
                System.out.println("KeyHash: " + keyHash);
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        //Botón de registro
        enlaceVista.registerButton.setOnClickListener(v -> registroMail());

        // Botón de Inicio de Sesión
        enlaceVista.loginButton.setOnClickListener(v -> loginMail());

    }

    //Lo que hago aqui es el registro por mail, despues de que el usuario meta los datso correctamente, le logueamos directamente
    //he toamdo dicha decision porque es lo que suele ocurrir en las apps hoy en dia. Despues de un registro exitoso, te logueas automaticamnete.

    private void registroMail() {
        String email = enlaceVista.emailEditText.getText().toString().trim();
        String password = enlaceVista.passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        autenticacion.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser usuario = autenticacion.getCurrentUser();
                        if (usuario != null) {
                            // Guardar datos en SharedPreferences
                            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                    .putString("nombre", "")
                                    .putString("correo", usuario.getEmail() != null ? usuario.getEmail() : email)
                                    .putString("foto", "")
                                    .putString("USER_ID", usuario.getUid())
                                    .apply();

                            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Error en el registro: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    //Metodo que permite al usuario loguearse con un mail y contraseña.
    private void loginMail() {
        String email = enlaceVista.emailEditText.getText().toString().trim();
        String password = enlaceVista.passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        autenticacion.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser usuario = autenticacion.getCurrentUser();
                        if (usuario != null) {

                            String userId = usuario.getUid();
                            DatabaseManager databaseManager = new DatabaseManager(LoginActivity.this);

                            User usuarioLocal = databaseManager.obtenerUsuarioPorId(userId);
                            String nombre;
                            String fotoUrl;

                            if (usuarioLocal != null) {
                                // Si el usuario ya existe en la base de datos local, usamos esos datos
                                nombre = usuarioLocal.getNombre();
                                fotoUrl = usuarioLocal.getImage();
                            } else {
                                // Si no existe, usamos los datos de Google y los guardamos en la base de datos local
                                nombre = "";
                                fotoUrl = "";
                                User usuarioNuevo=new User(userId, nombre, usuario.getEmail(), fotoUrl);
                                databaseManager.guardarUsuario(usuarioNuevo);
                                usersSync.agregarUsuarioFirebase(usuarioNuevo);

                            }




                            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                    .putString("nombre", nombre)
                                    .putString("correo", usuario.getEmail())
                                    .putString("foto", fotoUrl)
                                    .putString("USER_ID", usuario.getUid())
                                    .apply();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Error al iniciar sesión: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //Lo que hago aqui es crear un lanzador que abre la ventanita de Google y permite al usuario elegir la cuenta
    // si todo sale OK se recogen los datos de la cuenta de google
    private final ActivityResultLauncher<Intent> lanzadorResultadoActividad =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult resultado) {
                            if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                                Task<GoogleSignInAccount> tarea = GoogleSignIn.getSignedInAccountFromIntent(resultado.getData());
                                try {
                                    GoogleSignInAccount cuenta = tarea.getResult(ApiException.class);
                                    if (cuenta != null) {
                                        //Usamos los datos de la cuenta para autenticar al usuario con firebase
                                        autenticarConGoogle(cuenta.getIdToken());
                                    }
                                } catch (ApiException e) {
                                    Toast.makeText(LoginActivity.this, "Error al iniciar sesión: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Pasar el resultado al CallbackManager de Facebook
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    // Usamos el token de google para autenticar al usuario con Firebase, si todo esta bien guardamos su nombre, correo y foto con sharedpreferences
    //y luego nos lleva a la pantalla principal
    private void autenticarConGoogle(String tokenId) {
        AuthCredential credencial = com.google.firebase.auth.GoogleAuthProvider.getCredential(tokenId, null);
        autenticacion.signInWithCredential(credencial)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> tarea) {
                        if (tarea.isSuccessful()) {
                            FirebaseUser usuario = autenticacion.getCurrentUser();
                            if (usuario != null) {
                                String userId = usuario.getUid();
                                DatabaseManager databaseManager = new DatabaseManager(LoginActivity.this);

                                User usuarioLocal = databaseManager.obtenerUsuarioPorId(userId);
                                String nombre;
                                String fotoUrl;

                                if (usuarioLocal != null) {
                                    // Si el usuario ya existe en la base de datos local, usamos esos datos
                                    nombre = usuarioLocal.getNombre();
                                    fotoUrl = usuarioLocal.getImage();
                                } else {
                                    // Si no existe, usamos los datos de Google y los guardamos en la base de datos local
                                    nombre = usuario.getDisplayName();
                                    fotoUrl = (usuario.getPhotoUrl() != null) ? usuario.getPhotoUrl().toString() : "";
                                     User usuarioNuevo=new User(userId, nombre, usuario.getEmail(), fotoUrl);
                                    databaseManager.guardarUsuario(usuarioNuevo);
                                    usersSync.agregarUsuarioFirebase(usuarioNuevo);

                                }

                                // Guardar en SharedPreferences
                                System.out.println("fotoooUrl" +fotoUrl);
                                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                        .putString("nombre", nombre)
                                        .putString("correo", usuario.getEmail())
                                        .putString("foto", fotoUrl)
                                        .putString("USER_ID", userId)
                                        .apply();

                                databaseManager.close();

                                // Redirigir a MainActivity
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Error al autenticar con Firebase", Toast.LENGTH_SHORT).show();
                        }
                    }});
                }




    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        autenticacion.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Solicitud a la API Graph para obtener la información del usuario
                GraphRequest request = GraphRequest.newMeRequest(token, (object, response) -> {
                    FirebaseUser usuario = autenticacion.getCurrentUser();
                    DatabaseManager databaseManager = new DatabaseManager(LoginActivity.this);

                    try {
                        String userId = usuario.getUid();
                        User usuarioLocal = databaseManager.obtenerUsuarioPorId(userId);
                        String nombre;
                        String fotoPerfilUrl;

                        if (usuarioLocal != null) {
                            // Si el usuario ya existe en la base de datos local, usamos esos datos
                            nombre = usuarioLocal.getNombre();
                            fotoPerfilUrl = usuarioLocal.getImage();
                        } else {
                            // Si no existe, usamos los datos de Facebook y los guardamos en la base de datos local
                            nombre = object.optString("name", "Usuario de Facebook");
                            fotoPerfilUrl = object.getJSONObject("picture").getJSONObject("data").getString("url");
                            User usuarioNuevo=new User(userId, nombre, usuario.getEmail(), fotoPerfilUrl);
                            databaseManager.guardarUsuario(usuarioNuevo);
                            usersSync.agregarUsuarioFirebase(usuarioNuevo);
                        }

                        // Guardar en SharedPreferences
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                .putString("nombre", nombre)
                                .putString("correo", usuario.getEmail())
                                .putString("foto", fotoPerfilUrl)
                                .putString("USER_ID", userId)
                                .apply();

                        databaseManager.close();

                        // Redirigir a MainActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Log.e("FacebookLogin", "Error al procesar la respuesta de Facebook", e);
                        Toast.makeText(LoginActivity.this, "Error al obtener datos de Facebook", Toast.LENGTH_SHORT).show();
                    }
                });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,picture.width(500).height(500)");
                request.setParameters(parameters);
                request.executeAsync();
            } else {
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    Toast.makeText(this, "Este correo está registrado con otro proveedor", Toast.LENGTH_SHORT).show();
                    if (AccessToken.getCurrentAccessToken() != null) {
                        LoginManager.getInstance().logOut();
                    }
                } else {
                    Toast.makeText(this, "Error al autenticar con Facebook", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


}