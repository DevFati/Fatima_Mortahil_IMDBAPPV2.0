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
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import edu.pmdm.mortahil_fatimaimdbapp.databinding.ActivityLogInBinding;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth autenticacion;
    private GoogleSignInClient clienteGoogle;
    private ActivityLogInBinding enlaceVista;
    private CallbackManager mCallbackManager;


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
                                // Guardar datos en SharedPreferences
                                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                        .putString("nombre", usuario.getDisplayName())
                                        .putString("correo", usuario.getEmail())
                                        .putString("foto", usuario.getPhotoUrl() != null ? usuario.getPhotoUrl().toString() : "")
                                        .putString("USER_ID", usuario.getUid()) // Guarda el UID del usuario
                                        .apply();

                                // Redirigir a MainActivity
                                Log.d("LoginActivity", "USER_ID guardado: " + usuario.getUid());

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Error al autenticar con Firebase", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        autenticacion.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Solicitud a la API Graph para obtener la información del usuario
                GraphRequest request = GraphRequest.newMeRequest(token, (object, response) -> {
                    try {
                        // Verifica y obtiene los datos del usuario
                        String nombre = object.optString("name", "Usuario de Facebook");
                        String email = object.optString("email", "Correo no disponible");

                        // Obtén la URL correcta de la foto de perfil desde la respuesta JSON
                        String fotoPerfilUrl = object.getJSONObject("picture").getJSONObject("data").getString("url");

                        // Guarda los datos en SharedPreferences o úsalos según sea necesario
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                .putString("nombre", nombre)
                                .putString("correo", "Conectado con Facebook")
                                .putString("foto", fotoPerfilUrl)
                                .apply();

                        Log.d("FacebookLogin", "Nombre: " + nombre);
                        Log.d("FacebookLogin", "Correo: " + email);
                        Log.d("FacebookLogin", "Foto personalizada: " + fotoPerfilUrl);

                        // Redirige al MainActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Log.e("FacebookLogin", "Error al procesar la respuesta de Facebook", e);
                        Toast.makeText(LoginActivity.this, "Error al obtener datos de Facebook", Toast.LENGTH_SHORT).show();
                    }
                });

                // Configura los parámetros que deseas obtener
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,picture.width(500).height(500)");
                request.setParameters(parameters);
                request.executeAsync();
            } else {
                Log.e("FacebookLogin", "Error al autenticar con Firebase", task.getException());
                Toast.makeText(this, "Error al autenticar con Facebook", Toast.LENGTH_SHORT).show();
            }
        });
    }


}