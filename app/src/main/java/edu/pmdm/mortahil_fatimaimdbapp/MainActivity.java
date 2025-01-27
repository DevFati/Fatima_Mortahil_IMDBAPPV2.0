package edu.pmdm.mortahil_fatimaimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import edu.pmdm.mortahil_fatimaimdbapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private GoogleSignInClient googleSignInClient;
    private FirebaseUser user;
    private String idProv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());




        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        googleSignInClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build());

        // Configurar el Listener del botón Logout
        View headerView = navigationView.getHeaderView(0); // Obtenemos la vista del encabezado
        Button buttonLogout = headerView.findViewById(R.id.buttonLogout);
        buttonLogout.setOnClickListener(v -> cerrarSesion());


        // Obtener los datos del usuario desde SharedPreferences y ponemos datos por defecto en caso de que el usuario no tenga alguno de ellos
        String nombre = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("nombre", "Usuario");
        String correo = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("correo", "correo@ejemplo.com");
        String fotoUrl = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("foto", null);

        // Actualizar el encabezado del NavigationView
        TextView navNombre = headerView.findViewById(R.id.textViewNombre);
        TextView navCorreo = headerView.findViewById(R.id.textViewCorreo);
        ImageView navFoto = headerView.findViewById(R.id.imageViewImagen);

        navNombre.setText(nombre);
        navCorreo.setText(correo);

        //Si hay una url de foto la cargamos pero sino la hay, ponemos una por defecto
        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            Glide.with(this).load(fotoUrl).into(navFoto);
        } else {
            navFoto.setImageResource(R.drawable.baseline_account_box_24); // Imagen por defecto si no hay foto
        }
        user=FirebaseAuth.getInstance().getCurrentUser();
        idProv=user.getProviderData().get(1).getProviderId();
        System.out.println("provedorrrrr : "+idProv);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Maneja los clics en los elementos del menú
        if (item.getItemId() == R.id.buttonLogout) {
            cerrarSesion();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void cerrarSesion() {
        // Cerrar sesión de Firebase
        FirebaseAuth.getInstance().signOut();

        // Cerrar sesión de Google
        if (idProv.equals("google.com")) {
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                // Cerrar sesión de Facebook
                if (AccessToken.getCurrentAccessToken() != null) {
                    LoginManager.getInstance().logOut();
                }

                // Redirigir al usuario a la pantalla de inicio de sesión
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        } else if(idProv.equals("facebook.com")) {
            // Si no hay sesión de Google, cerrar sesión de Facebook directamente
            if (AccessToken.getCurrentAccessToken() != null) {
                LoginManager.getInstance().logOut();
                System.out.println("entraaaaa ifffff ");

            }

            // Redirigir al usuario a la pantalla de inicio de sesión
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }


}