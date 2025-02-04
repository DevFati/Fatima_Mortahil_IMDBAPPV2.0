package edu.pmdm.mortahil_fatimaimdbapp;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.util.Base64;

public class KeystoreManager {

    private static final String KEY_ALIAS = "UserKey"; // Alias único para la clave
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private KeyStore keyStore;

    public KeystoreManager() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generarClave(); // Generar clave si no existe
            }
        } catch (Exception e) {
            Log.e("KeystoreManager", "Error al inicializar KeyStore", e);
        }
    }

    private void generarClave() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        keyGenerator.generateKey();
    }

    private SecretKey obtenerClave() throws Exception {
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    public String cifrar(String textoPlano) throws Exception {
        if (textoPlano == null || textoPlano.isEmpty()) {
            Log.e("KeystoreManager", "Texto plano vacío o nulo");
            return "";
        }

        // Configurar el cifrador
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, obtenerClave());

        // Obtener el IV y los datos cifrados
        byte[] iv = cipher.getIV(); // Vector de inicialización
        byte[] datosCifrados = cipher.doFinal(textoPlano.getBytes("UTF-8")); // Codificar a UTF-8

        // Combinar IV y datos cifrados
        byte[] combinado = new byte[iv.length + datosCifrados.length];
        System.arraycopy(iv, 0, combinado, 0, iv.length);
        System.arraycopy(datosCifrados, 0, combinado, iv.length, datosCifrados.length);

        // Convertir a Base64 para almacenamiento
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(combinado);
        } else {
            throw new UnsupportedOperationException("La codificación en Base64 no es compatible con esta versión de Android.");
        }
    }

    public String descifrar(String textoCifrado) throws Exception {
        if (textoCifrado == null || textoCifrado.isEmpty()) {
            Log.e("KeystoreManager", "Texto cifrado vacío o nulo");
            return "";
        }

        byte[] combinado;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            combinado = Base64.getDecoder().decode(textoCifrado);
        } else {
            throw new UnsupportedOperationException("La decodificación en Base64 no es compatible con esta versión de Android.");
        }

        // Extraer IV y datos cifrados
        byte[] iv = new byte[12]; // Longitud estándar para GCM
        byte[] datosCifrados = new byte[combinado.length - 12];
        System.arraycopy(combinado, 0, iv, 0, 12);
        System.arraycopy(combinado, 12, datosCifrados, 0, datosCifrados.length);

        // Configurar descifrado
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, obtenerClave(), new GCMParameterSpec(128, iv));

        // Descifrar y convertir a String
        byte[] textoPlano = cipher.doFinal(datosCifrados);
        return new String(textoPlano, "UTF-8");
    }
}
