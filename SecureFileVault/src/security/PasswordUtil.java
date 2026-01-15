package security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
	final static int ROUND = 12;
    public static String hashPassword(String password) {
    	String hashed = BCrypt.hashpw(password, BCrypt.gensalt(ROUND));
    	return hashed;
    }

    public static boolean verifyPassword(String inputPassword, String storedHash) {
    		if (storedHash == null) return false;
    		try {
            return BCrypt.checkpw(inputPassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false; 
        }
    }
}