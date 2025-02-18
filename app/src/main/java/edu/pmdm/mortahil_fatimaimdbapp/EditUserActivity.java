package edu.pmdm.mortahil_fatimaimdbapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.libraries.places.api.Places;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.pmdm.mortahil_fatimaimdbapp.database.DatabaseManager;
import edu.pmdm.mortahil_fatimaimdbapp.databinding.ActivityEditUserBinding;
import edu.pmdm.mortahil_fatimaimdbapp.models.User;
import edu.pmdm.mortahil_fatimaimdbapp.sync.UsersSync;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EditUserActivity extends AppCompatActivity {

    private ActivityEditUserBinding binding;
    private String imagenTemporal = null; // Imagen seleccionada pero aún no guardada
    private String imagenGuardada; // Imagen previa guardada en SharedPreferences
    private Uri imagenCamaraUri; // URI de la imagen capturada
    private String direccionTemporal = ""; // Dirección seleccionada
    private static final int SELECT_ADDRESS_REQUEST = 1; // Declaración de la constante para manejar la dirección
    private UsersSync usersSync;
    private KeystoreManager keystoreManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usersSync=new UsersSync(this);
        keystoreManager=new KeystoreManager();
        // Configuración del ViewBinding
        binding = ActivityEditUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Obtener el ID del usuario desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "");
        // Sincronizar Firestore con la base de datos local
        DatabaseManager databaseManager = new DatabaseManager(this);
      //  usersSync.sincronizarDesdeFirebase(userId);

        // Inicializar Places API con la API Key
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyAER7D-uvYpBOG3wZjz9z3AeGulqAci-OU");
        }
        // Configurar el CountryCodePicker para el prefijo
        binding.countryCodePicker.registerCarrierNumberEditText(binding.editUserPhone);
        // Esperar un breve tiempo para completar la sincronización
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Mostrar datos desde la base de datos local
            cargarDatosDesdeBaseDeDatosLocal(userId);
        }, 1000); // Espera de 1 segundo (ajustar según sea necesario)

        // Seleccionar imagen
        binding.selectImageButton.setOnClickListener(v -> mostrarDialogoSeleccionImagen());
        binding.selectAddressButton.setOnClickListener(v -> verificarPermisoUbicacionYAbrirSelector());

        // Guardar cambios
        binding.saveUserButton.setOnClickListener(v -> {
            try {
                guardarDatosEnLocalYNube();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void cargarDatosDesdeBaseDeDatosLocal(String userId) {
        DatabaseManager databaseManager = new DatabaseManager(this);
        try {
            User usuario = databaseManager.obtenerUsuarioPorId(userId);
            if (usuario != null) {
                // Asignar datos descifrados a los campos de la UI
                String direccionDescifrada = keystoreManager.descifrar(usuario.getAddress());
                String telefonoDescifrado = keystoreManager.descifrar(usuario.getPhone());
                imagenGuardada=usuario.getImage();

                binding.editUserName.setText(usuario.getNombre());
                binding.editUserEmail.setText(usuario.getCorreo());

                binding.editUserAddress.setText(direccionDescifrada != null ? direccionDescifrada : "");

                binding.countryCodePicker.setFullNumber(telefonoDescifrado != null ? telefonoDescifrado : "");

                // Cargar imagen si está disponible
                if (usuario.getImage() != null && !usuario.getImage().isEmpty()) {
                    Glide.with(this).load(usuario.getImage()).into(binding.userImageView);
                } else {
                    binding.userImageView.setImageResource(R.drawable.baseline_account_box_24); // Imagen por defecto
                }
            } else {
                Toast.makeText(this, "Usuario no encontrado en la base de datos local", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("EditUserActivity", "Error al cargar datos del usuario", e);
            Toast.makeText(this, "Error al cargar los datos del usuario", Toast.LENGTH_SHORT).show();
        } finally {
            databaseManager.close();
        }
    }



    private void guardarDatosEnLocalYNube() throws Exception {
        String telCompleto = binding.countryCodePicker.getFullNumberWithPlus();
        String nombre = binding.editUserName.getText().toString();
        String direccion = direccionTemporal != null && !direccionTemporal.isEmpty() ? direccionTemporal : binding.editUserAddress.getText().toString();
        String imagen;

        if(imagenTemporal!=null && !imagenTemporal.equalsIgnoreCase("")){
            imagen=imagenTemporal;
        }else{
            imagen=imagenGuardada;
        }
        telCompleto= keystoreManager.cifrar(telCompleto);
        direccion=keystoreManager.cifrar(direccion);
        // Validar el número de teléfono
        if(!binding.editUserPhone.getText().toString().equals("")){
            if (!binding.countryCodePicker.isValidFullNumber()) {
                Toast.makeText(this, "Número de teléfono inválido", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // Guardar en SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "");

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("nombre", nombre);
        editor.putString("telefono", telCompleto);
        editor.putString("direccion", direccion);
        editor.putString("foto", imagen);
        editor.apply();
        // Guardar los datos en la base de datos local
        DatabaseManager databaseManager = new DatabaseManager(this);
        try {
            databaseManager.actualizarDatosUsuario(
                    userId,
                    nombre,
                    telCompleto,
                    direccion,
                    imagen
            );
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show();
        } finally {
            databaseManager.close();
        }

        // Finalizar la actividad
        finish();
    }




    private final ActivityResultLauncher<Intent> seleccionarDireccionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    direccionTemporal = result.getData().getStringExtra("direccion");
                    binding.editUserAddress.setText(direccionTemporal); // Mostrar temporalmente en el EditText
                }
            });

    private void verificarPermisoUbicacionYAbrirSelector() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Si el permiso ya está otorgado, abrir la actividad
            abrirSelectorDireccion();
        } else {
            // Si no tiene permiso, solicitarlo
            solicitarPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private final ActivityResultLauncher<String> solicitarPermisoUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Si el usuario aceptó, abrir la actividad de dirección
                    abrirSelectorDireccion();
                } else {
                    // Si el usuario denegó el permiso, mostrar un mensaje
                    Toast.makeText(this, "Permiso de ubicación requerido para seleccionar una dirección", Toast.LENGTH_LONG).show();
                }
            });






    private void abrirSelectorDireccion() {
        Intent intent = new Intent(this, SelectAddressActivity.class);
        intent.putExtra("direccion_actual", binding.editUserAddress.getText().toString()); // Pasar dirección actual
        seleccionarDireccionLauncher.launch(intent);
    }



    private void mostrarDialogoSeleccionImagen() {
        String[] opciones = {"Tomar foto", "Seleccionar de galería", "Ingresar URL"};
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar imagen desde:")
                .setItems(opciones, (dialog, opcion) -> {
                    switch (opcion) {
                        case 0:
                            tomarFoto();
                            break;
                        case 1:
                            seleccionarImagenGaleria();
                            break;
                        case 2:
                            ingresarUrlImagen();
                            break;
                    }
                })
                .show();
    }

    private void seleccionarImagenGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        seleccionarImagenGaleriaLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> seleccionarImagenGaleriaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(binding.userImageView);
                    imagenTemporal = selectedImageUri.toString(); // Guardar solo temporalmente
                }
            });

    private void tomarFoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permiso de cámara
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        } else {
            abrirCamara();
        }
    }

    // Método para abrir la cámara y guardar la imagen en la galería
    private void abrirCamara() {
        ContentValues valores = new ContentValues();
        valores.put(MediaStore.Images.Media.TITLE, "Nueva Imagen");
        valores.put(MediaStore.Images.Media.DESCRIPTION, "Imagen capturada por la cámara");
        imagenCamaraUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imagenCamaraUri);
        tomarFotoLauncher.launch(intent);
    }

    // Lanzador de actividad para manejar la foto tomada
    private final ActivityResultLauncher<Intent> tomarFotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Glide.with(this).load(imagenCamaraUri).into(binding.userImageView);
                    imagenTemporal = imagenCamaraUri.toString(); // Guardar solo temporalmente
                } else {
                    Toast.makeText(this, "No se capturó la imagen", Toast.LENGTH_SHORT).show();
                }
            });

    private void ingresarUrlImagen() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Introducir URL de imagen");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Cargar", (dialog, which) -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty() && esUrlValida(url)) {
                verificarYMostrarImagen(url);

            }else{
                Toast.makeText(this, "URL no es válida.", Toast.LENGTH_SHORT).show();

            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void verificarYMostrarImagen(String url) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean esValida = esImagenValida(url);

            handler.post(() -> {
                if (esValida) {
                    Glide.with(binding.userImageView.getContext())
                            .load(url)
                            .placeholder(R.drawable.baseline_account_box_24)
                            .error(R.drawable.baseline_account_box_24)
                            .into(binding.userImageView);

                    imagenTemporal = url;
                } else {
                    imagenTemporal = "";
                    Toast.makeText(binding.userImageView.getContext(), "URL no válida o no es una imagen.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private boolean esImagenValida(String url) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).head().build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) return false;

            String contentType = response.header("Content-Type");
            return contentType != null && contentType.startsWith("image/");
        } catch (Exception e) {
            return false;
        }
    }





    private boolean esUrlValida(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                Toast.makeText(this, "Debes otorgar permiso de cámara para tomar fotos", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
