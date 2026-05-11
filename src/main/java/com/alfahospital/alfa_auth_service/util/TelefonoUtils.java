package com.alfahospital.alfa_auth_service.util;

public final class TelefonoUtils {

    private TelefonoUtils() {
    }

    public static String normalizar(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }

        String numero = telefono
                .replace("whatsapp:", "")
                .replaceAll("[^\\d+]", "")
                .trim();

        if (numero.startsWith("0")) {
            numero = "+593" + numero.substring(1);
        } else if (numero.startsWith("593")) {
            numero = "+" + numero;
        }

        return numero;
    }
}
