package model;

public class User {

	private String firstName;
	private String lastName;
	private String email;
	private String password;
	private String role;

	// Vault metadata
	private byte[] vaultSalt;
	private byte[] vaultIv;
	private byte[] vaultKeyEnc;
	private int vaultIter;

	// Runtime-only password
	private transient char[] sessionPassword;

	// ✅ FULL constructor (LOGIN)
	public User(String firstName, String lastName, String email, String password, String role, byte[] vaultSalt,
			byte[] vaultIv, byte[] vaultKeyEnc, int vaultIter) {

		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.password = password;
		this.role = role;
		this.vaultSalt = vaultSalt;
		this.vaultIv = vaultIv;
		this.vaultKeyEnc = vaultKeyEnc;
		this.vaultIter = vaultIter;
	}

	// ✅ SIMPLE constructor (REGISTER / TABLE)
	public User(String firstName, String lastName, String email, String password, String role) {
		this(firstName, lastName, email, password, role, null, null, null, 120000);
	}

	// ===== GETTERS =====
	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	public String getRole() {
		return role;
	}

	public byte[] getVaultSalt() {
		return vaultSalt;
	}

	public byte[] getVaultIv() {
		return vaultIv;
	}

	public byte[] getVaultKeyEnc() {
		return vaultKeyEnc;
	}

	public int getVaultIter() {
		return vaultIter;
	}

	// ===== SETTERS for vault meta =====
	public void setVaultSalt(byte[] vaultSalt) {
		this.vaultSalt = vaultSalt;
	}

	public void setVaultIv(byte[] vaultIv) {
		this.vaultIv = vaultIv;
	}

	public void setVaultKeyEnc(byte[] vaultKeyEnc) {
		this.vaultKeyEnc = vaultKeyEnc;
	}

	public void setVaultIter(int vaultIter) {
		this.vaultIter = vaultIter;
	}

	// ===== SESSION PASSWORD =====
	public void setSessionPassword(char[] sessionPassword) {
		this.sessionPassword = sessionPassword;
	}

	public char[] getSessionPassword() {
		return sessionPassword;
	}
}
