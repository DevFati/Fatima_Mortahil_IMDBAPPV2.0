package edu.pmdm.mortahil_fatimaimdbapp.models;

public class User {

    private String id; // ID del usuario
    private String nombre; // Nombre del usuario
    private String correo; // Correo del usuario
    private String ultimoLogin; // Último tiempo de login
    private String ultimoLogout; // Último tiempo de logout

    public User(String id, String nombre, String correo, String ultimoLogin, String ultimoLogout) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.ultimoLogin = ultimoLogin;
        this.ultimoLogout = ultimoLogout;
    }

    // Getters y setters
    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getUltimoLogin() {
        return ultimoLogin;
    }

    public String getUltimoLogout() {
        return ultimoLogout;
    }

    public void setUltimoLogout(String ultimoLogout) {
        this.ultimoLogout = ultimoLogout;
    }
}
