package edu.pmdm.mortahil_fatimaimdbapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import edu.pmdm.mortahil_fatimaimdbapp.databinding.ActivityEditUserBinding;

public class EditUserActivity extends AppCompatActivity {

    private ActivityEditUserBinding binding;
    private String imagenTemporal = null; // Imagen seleccionada pero aún no guardada
    private String imagenGuardada; // Imagen previa guardada en SharedPreferences
    private Uri imagenCamaraUri; // URI de la imagen capturada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuración del ViewBinding
        binding = ActivityEditUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Cargar datos desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        binding.editUserName.setText(prefs.getString("nombre", ""));
        binding.editUserEmail.setText(prefs.getString("correo", ""));
        binding.editUserPhone.setText(prefs.getString("telefono", ""));
        binding.editUserAddress.setText(prefs.getString("direccion", ""));
        imagenGuardada = prefs.getString("foto", ""); // Cargar la imagen guardada

        if (!imagenGuardada.isEmpty()) {
            Glide.with(this).load(imagenGuardada).into(binding.userImageView);
        } else {
            binding.userImageView.setImageResource(R.drawable.baseline_account_box_24); // Imagen por defecto
        }

        // Seleccionar imagen
        binding.selectImageButton.setOnClickListener(v -> mostrarDialogoSeleccionImagen());

        // Guardar cambios
        binding.saveUserButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("nombre", binding.editUserName.getText().toString());
            editor.putString("telefono", binding.editUserPhone.getText().toString());
            editor.putString("direccion", binding.editUserAddress.getText().toString());

            // Solo guardar la imagen si el usuario ha seleccionado una nueva
            if (imagenTemporal != null) {
                editor.putString("foto", imagenTemporal);
            }

            editor.apply();
            Toast.makeText(this, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show();
            finish();
        });
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
            if (!url.isEmpty()) {
                Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.baseline_account_box_24)
                        .error(R.drawable.baseline_account_box_24) // Si la URL es incorrecta, se usa imagen por defecto
                        .into(binding.userImageView);

                if (esUrlValida(url)) {
                    imagenTemporal = url; // Guardar solo si es válida
                } else {
                    imagenTemporal = ""; // Si es inválida, no se modifica la imagen guardada
                }
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
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
