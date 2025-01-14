package edu.pmdm.mortahil_fatimaimdbapp;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import edu.pmdm.mortahil_fatimaimdbapp.databinding.ActivityMovieDetailsBinding;
import edu.pmdm.mortahil_fatimaimdbapp.models.Movie;

import android.Manifest;
import android.widget.Toast;

public  class MovieDetailsActivity extends AppCompatActivity {
    private  ActivityMovieDetailsBinding binding;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_SMS = 2;
    private String detalleSms; // Detalles de la película que seran enviador por sms
    private ActivityResultLauncher<Intent> contacto; //Para manejar cuando el usuario seleccione un contacto




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMovieDetailsBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        // recuperamos el objeto Movie pasado desde la actividad anterior por "Intent" usando el metodo "getParcelableExtra"

        Movie movie = getIntent().getParcelableExtra("movie");



        if (movie != null) {
            // Asignar los valores a los elementos de la interfaz
            binding.textViewTitulo.setText(movie.getTitulo());
            binding.textViewDescripcion.setText(movie.getDescripcion());
            binding.textViewFecha.setText("Release Date: " + movie.getFechaLanzamiento());
            binding.textViewPuntuacion.setText("Rating: " + movie.getCalificacion());

            // Preparar los detalles de la película que enviaremos por sms
            detalleSms = "Esta película te gustará: " + movie.getTitulo() + "\nRating: " + movie.getCalificacion();


            // Cargar la imagen con Glide desde la url de la pelicula
            Glide.with(this)
                    .load(movie.getUrlImagen())
                    .into(binding.imageViewPoster);
        }




        //si el usuario selecciono correctamente un contacto, se obtiene la URI del contacto que se ha seleccionado
        contacto = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri contactUri = data.getData();
                            obtenerNumeroDeContacto(contactUri);
                        }
                    }
                }
        );

        //boton para enviar sms
        binding.buttonEnviarSMS.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                // Solicitar el primer permiso para leer contactos si no esta concedido
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACT);
            } else {
                //si ya esta concedido el permiso nos vamos directamente a mostrar al usuario un contacto a seleccionar
                seleccionarContacto();
            }
        });



    }

    //Se lanza un intent para seleccionar un contacto de nuestra lista de contactos de nuestro movil
    private void seleccionarContacto() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        contacto.launch(intent);
    }


//Abrimos un cursor para ver los datos del contacto que hemos seleccionado y guardamos el telefono de este
    private void obtenerNumeroDeContacto(Uri contactUri) {
        Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String tel = cursor.getString(numberIndex);
            cursor.close();

            // Comprueba permisos para enviar SMS y si no los tiene los solicita. Si los tiene llama al metodo enviarSms
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS);
            } else {
                enviarSMS(tel);
            }
        }
    }

    //Construimos un intent para enviar un SMS al numero que hemos seleccionado
    private void enviarSMS(String tel) {
        try {
            Intent smsIntent = new Intent(Intent.ACTION_VIEW);
            smsIntent.setData(Uri.parse("sms:" + tel));
            //asignamos nuestro mensaje "detalleSms" al cuerpo de nuestro mensaje
            smsIntent.putExtra("sms_body", detalleSms);
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Error al enviar el SMS.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    //Si se concede el permiso de contactos, llamamos a seleccionar contato
    //si se concede el permiso de sms, se muestra un toast indicamndo al usuariio que los permisos fueron concedidos y este puede enviar sms. 
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CONTACT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                seleccionarContacto();
            } else {
                Toast.makeText(this, "Permiso de contactos denegado.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de SMS concedido. Pulsa de nuevo el boton para enviar un sms", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso de SMS denegado.", Toast.LENGTH_SHORT).show();
            }
        }
    }


}






