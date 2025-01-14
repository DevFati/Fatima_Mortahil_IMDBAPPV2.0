package edu.pmdm.mortahil_fatimaimdbapp;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import edu.pmdm.mortahil_fatimaimdbapp.databinding.ActivityLogInBinding;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth autenticacion;
    private GoogleSignInClient clienteGoogle;
    private ActivityLogInBinding enlaceVista;

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
    protected void onCreate(Bundle savedInstanceState) {
        enlaceVista=ActivityLogInBinding.inflate(getLayoutInflater());

        super.onCreate(savedInstanceState);
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
}