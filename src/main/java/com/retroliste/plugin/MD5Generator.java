package com.retroliste.plugin;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Generator {
    public static String createMD5Hash(String input) throws NoSuchAlgorithmException {
        // MessageDigest Instanz für MD5 erstellen
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // String in Bytes umwandeln und Hash berechnen
        byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));

        // Bytes in Hexadezimal-String umwandeln
        BigInteger bigInt = new BigInteger(1, messageDigest);
        String hashtext = bigInt.toString(16);

        // Führende Nullen hinzufügen, falls nötig
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }

        return hashtext;
    }
}