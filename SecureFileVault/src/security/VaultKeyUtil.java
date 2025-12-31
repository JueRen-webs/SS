package security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

public class VaultKeyUtil {

	private static final int SALT_LEN = 16; // 128-bit
	private static final int GCM_IV_LEN = 12; // 96-bit recommended for GCM
	private static final int GCM_TAG_BITS = 128; // 16 bytes
	private static final int VAULT_KEY_LEN = 32; // 256-bit vault key

	private static final SecureRandom RNG = new SecureRandom();

	public static class WrappedVaultKey {
		public final byte[] salt;
		public final byte[] iv;
		public final byte[] encKey;
		public final int iterations;

		public WrappedVaultKey(byte[] salt, byte[] iv, byte[] encKey, int iterations) {
			this.salt = salt;
			this.iv = iv;
			this.encKey = encKey;
			this.iterations = iterations;
		}
	}

	// Generate random vaultKey, wrap it with password-derived KEK
	public static WrappedVaultKey generateAndWrapVaultKey(char[] password, int iterations) throws Exception {
		byte[] vaultKey = new byte[VAULT_KEY_LEN];
		RNG.nextBytes(vaultKey);

		byte[] salt = new byte[SALT_LEN];
		RNG.nextBytes(salt);

		SecretKey kek = deriveKeyFromPassword(password, salt, iterations);

		byte[] iv = new byte[GCM_IV_LEN];
		RNG.nextBytes(iv);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, kek, new GCMParameterSpec(GCM_TAG_BITS, iv));
		byte[] enc = cipher.doFinal(vaultKey);

		// wipe vaultKey copy in memory (best effort)
		Arrays.fill(vaultKey, (byte) 0);

		return new WrappedVaultKey(salt, iv, enc, iterations);
	}

	// Unwrap stored vaultKey using password-derived KEK
	public static SecretKey unwrapVaultKey(char[] password, byte[] salt, int iterations, byte[] iv, byte[] encKey)
			throws Exception {
		SecretKey kek = deriveKeyFromPassword(password, salt, iterations);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, kek, new GCMParameterSpec(GCM_TAG_BITS, iv));
		byte[] vaultKeyBytes = cipher.doFinal(encKey);

		return new SecretKeySpec(vaultKeyBytes, "AES"); // return vault key (AES-256)
	}

	private static SecretKey deriveKeyFromPassword(char[] password, byte[] salt, int iterations) throws Exception {
		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		byte[] keyBytes = skf.generateSecret(spec).getEncoded();
		return new SecretKeySpec(keyBytes, "AES");
	}
}
