package security;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.Base64;

public class CryptoUtil {

	private static final String AES_ALGO = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH = 128; // bits
	private static final int IV_LENGTH = 12; // bytes (GCM standard)
	private static final int KEY_LENGTH = 256; // bits

	// ===== AES KEY GENERATION (vault master key) =====
	public static SecretKey generateKey() throws Exception {
		KeyGenerator gen = KeyGenerator.getInstance("AES");
		gen.init(KEY_LENGTH);
		return gen.generateKey();
	}

	// ===== PBKDF2 KEY DERIVATION (KEK) =====
	public static SecretKey deriveKey(char[] password, byte[] salt, int iterations) throws Exception {
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
		SecretKey tmp = factory.generateSecret(spec);
		return new SecretKeySpec(tmp.getEncoded(), "AES");
	}

	// Alias names used in vault design
	public static byte[] generateSalt() {
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		return salt;
	}

	public static SecretKey deriveKEK(char[] password, byte[] salt, int iterations) throws Exception {
		return deriveKey(password, salt, iterations);
	}

	// ===== IV =====
	public static byte[] generateIV() {
		byte[] iv = new byte[IV_LENGTH];
		new SecureRandom().nextBytes(iv);
		return iv;
	}

	// ===== ENCRYPT / DECRYPT BYTES =====
	public static byte[] encrypt(byte[] plain, SecretKey key, byte[] iv) throws Exception {
		Cipher cipher = Cipher.getInstance(AES_ALGO);
		cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
		return cipher.doFinal(plain);
	}

	public static byte[] decrypt(byte[] cipherText, SecretKey key, byte[] iv) throws Exception {
		Cipher cipher = Cipher.getInstance(AES_ALGO);
		cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
		return cipher.doFinal(cipherText);
	}

	// ===== WRAP / UNWRAP vault key bytes using KEK (AES-GCM) =====
	public static byte[] wrapKey(SecretKey vaultKey, SecretKey kek, byte[] iv) throws Exception {
		return encrypt(vaultKey.getEncoded(), kek, iv);
	}

	public static SecretKey unwrapKey(byte[] vaultKeyEnc, SecretKey kek, byte[] iv) throws Exception {
		byte[] raw = decrypt(vaultKeyEnc, kek, iv);
		return new SecretKeySpec(raw, "AES");
	}

	// ===== BASE64 HELPERS =====
	public static String b64(byte[] data) {
		return Base64.getEncoder().encodeToString(data);
	}

	public static byte[] fromB64(String s) {
		return Base64.getDecoder().decode(s);
	}
}
