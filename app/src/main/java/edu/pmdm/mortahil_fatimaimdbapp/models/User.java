package edu.pmdm.mortahil_fatimaimdbapp.models;

public class User {

    private String id; // ID del usuario
    private String nombre; // Nombre del usuario
    private String correo; // Correo del usuario
    private String address; // Dirección del usuario
    private String phone; // Teléfono del usuario
    private String image; // Imagen del usuario
    private String ultimoLogin; // Último tiempo de login
    private String ultimoLogout; // Último tiempo de logout

    // Constructor completo
    public User(String id, String nombre, String correo, String address, String phone, String image, String ultimoLogin, String ultimoLogout) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.address = address;
        this.phone = phone;
        this.image = image;
        this.ultimoLogin = ultimoLogin;
        this.ultimoLogout = ultimoLogout;
    }

    // Constructor sin datos sensibles (opcional)
    public User(String id, String nombre, String correo, String ultimoLogin, String ultimoLogout) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.ultimoLogin = ultimoLogin;
        this.ultimoLogout = ultimoLogout;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getImage() {
        return image;
    }

    public String getUltimoLogin() {
        return ultimoLogin;
    }

    public String getUltimoLogout() {
        return ultimoLogout;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setUltimoLogin(String ultimoLogin) {
        this.ultimoLogin = ultimoLogin;
    }

    public void setUltimoLogout(String ultimoLogout) {
        this.ultimoLogout = ultimoLogout;
    }
}
