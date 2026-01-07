package security;

import org.mindrot.jbcrypt.BCrypt;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean verifyPassword(String inputPassword, String storedHash) {
        if (storedHash == null) return false;

        if (storedHash.startsWith("$2a$")) {
            try {
                return BCrypt.checkpw(inputPassword, storedHash);
            } catch (IllegalArgumentException e) {
                return false; 
            }
        } 
        
        else {
            return verifyLegacySha256(inputPassword, storedHash);
        }
    }

    public static boolean isLegacyHash(String storedHash) {
        return storedHash != null && !storedHash.startsWith("$2a$");
    }

    private static boolean verifyLegacySha256(String inputPassword, String storedHash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(inputPassword.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equals(storedHash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Legacy hashing error", e);
        }
    }
}