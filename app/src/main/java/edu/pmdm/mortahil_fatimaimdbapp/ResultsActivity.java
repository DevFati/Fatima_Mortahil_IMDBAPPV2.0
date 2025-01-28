package edu.pmdm.mortahil_fatimaimdbapp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.mortahil_fatimaimdbapp.adapters.MovieAdapter;
import edu.pmdm.mortahil_fatimaimdbapp.databinding.ActivityResultsBinding;
import edu.pmdm.mortahil_fatimaimdbapp.models.Movie;
import edu.pmdm.mortahil_fatimaimdbapp.sync.FavoritesSync;

public class ResultsActivity extends AppCompatActivity {
    private ActivityResultsBinding binding;
    private List<Movie> peliculas;
    private FavoritesSync favSync;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityResultsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recuperar las películas desde el Intent
        if (getIntent() != null && getIntent().hasExtra("peliculas")) {
            peliculas = getIntent().getParcelableArrayListExtra("peliculas");
        } else {
            peliculas = new ArrayList<>();
            Toast.makeText(this, "No se encontraron películas.", Toast.LENGTH_SHORT).show();
        }

        configurarRecyclerView();
    }

    private void configurarRecyclerView() {
        // Configurar GridLayoutManager para mostrar las películas en 2 columnas
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        binding.recyclerViewResultados.setLayoutManager(gridLayoutManager);
        String userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("USER_ID", null);

        favSync = new FavoritesSync(this, userId);

        // Configurar el adaptador
        MovieAdapter adapter = new MovieAdapter(peliculas, this,false, favSync );
        binding.recyclerViewResultados.setAdapter(adapter);
    }
}
