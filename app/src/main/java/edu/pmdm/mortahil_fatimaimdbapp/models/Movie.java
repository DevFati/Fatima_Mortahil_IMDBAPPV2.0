package edu.pmdm.mortahil_fatimaimdbapp.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Movie implements Parcelable {

    private String id;         // ID de la película
    private String titulo;     // Título de la película
    private String urlImagen;  // URL de la imagen
    private String descripcion; // Descripción
    private String fechaLanzamiento; // Fecha de lanzamiento
    private double calificacion; // Puntuación de la película
    private String api; // API de origen ("imdb" o "tmdb")

    public Movie(String id, String titulo, String urlImagen, String descripcion, String fechaLanzamiento, double calificacion, String api) {
        this.id = id;
        this.titulo = titulo;
        this.urlImagen = urlImagen;
        this.descripcion = descripcion;
        this.fechaLanzamiento = fechaLanzamiento;
        this.calificacion = calificacion;
        this.api = api;
    }


    public Movie( String titulo, String urlImagen, String fechaLanzamiento, double calificacion, String api) {
        this.titulo = titulo;
        this.urlImagen = urlImagen;
        this.fechaLanzamiento = fechaLanzamiento;
        this.calificacion = calificacion;
        this.api = api;
    }

    protected Movie(Parcel in) {
        id = in.readString();
        titulo = in.readString();
        urlImagen = in.readString();
        descripcion = in.readString();
        fechaLanzamiento = in.readString();
        calificacion = in.readDouble();
        api = in.readString();
    }

    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(titulo);
        dest.writeString(urlImagen);
        dest.writeString(descripcion);
        dest.writeString(fechaLanzamiento);
        dest.writeDouble(calificacion);
        dest.writeString(api);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFechaLanzamiento() {
        return fechaLanzamiento;
    }

    public void setFechaLanzamiento(String fechaLanzamiento) {
        this.fechaLanzamiento = fechaLanzamiento;
    }

    public double getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(double calificacion) {
        this.calificacion = calificacion;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }
}


