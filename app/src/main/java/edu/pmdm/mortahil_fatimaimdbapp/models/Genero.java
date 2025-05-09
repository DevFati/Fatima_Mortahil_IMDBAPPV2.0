package edu.pmdm.mortahil_fatimaimdbapp.models;

public class Genero {
    private int id;
    private String nombre;


    public Genero(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre; // Solo muestra el nombre en el Spinner
    }
}
