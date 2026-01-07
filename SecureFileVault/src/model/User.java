package model;

public class User {

	private String firstName;
	private String lastName;
	private String email;
	private String password;
	private String role;

	// Runtime-only password
	private transient char[] sessionPassword;

	// ✅ FULL constructor (LOGIN)
	public User(String firstName, String lastName, String email, String password, String role) {

		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.password = password;
		this.role = role;
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

	// ===== SESSION PASSWORD =====
	public void setSessionPassword(char[] sessionPassword) {
		this.sessionPassword = sessionPassword;
	}

	public char[] getSessionPassword() {
		return sessionPassword;
	}
	
	public void setPassword(String password) {
		this.password=password;
	}
}
