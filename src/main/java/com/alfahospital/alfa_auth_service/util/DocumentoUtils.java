package com.alfahospital.alfa_auth_service.util;

public final class DocumentoUtils {

    private DocumentoUtils() {}

    public static boolean esCedulaValida(String cedula) {
        if (cedula == null || !cedula.matches("^\\d{10}$")) return false;

        int provincia = Integer.parseInt(cedula.substring(0, 2));
        if ((provincia < 1 || provincia > 24) && provincia != 30) return false;

        int tercerDigito = Character.getNumericValue(cedula.charAt(2));
        if (tercerDigito > 5) return false;

        int[] digitos = cedula.chars().map(Character::getNumericValue).toArray();
        int digitoVerificador = digitos[9];
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int val = digitos[i];
            if (i % 2 == 0) {
                val *= 2;
                if (val > 9) val -= 9;
            }
            suma += val;
        }
        int calculado = (10 - (suma % 10)) % 10;
        return calculado == digitoVerificador;
    }

    public static boolean esPasaporteValido(String pasaporte) {
        return pasaporte != null && pasaporte.matches("^[a-zA-Z0-9]{6,15}$");
    }
}
