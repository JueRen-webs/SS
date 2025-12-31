package ui;

import dao.AuditLogDAO;
import dao.UserDAO;
import model.User;
import security.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.security.SecureRandom;

public class RegisterFrame extends JFrame {

	private JTextField firstNameField;
	private JTextField lastNameField;
	private JTextField emailField;
	private JPasswordField passwordField;

	private final UserDAO userDao = new UserDAO();
	private final AuditLogDAO auditDao = new AuditLogDAO();
	private final String role;

	public RegisterFrame(String role) {
		this.role = role;

		setTitle("Register " + role);
		setSize(400, 260);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout(10, 10));

		JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
		form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		form.add(new JLabel("First Name:"));
		firstNameField = new JTextField();
		form.add(firstNameField);

		form.add(new JLabel("Last Name:"));
		lastNameField = new JTextField();
		form.add(lastNameField);

		form.add(new JLabel("Email:"));
		emailField = new JTextField();
		form.add(emailField);

		form.add(new JLabel("Password:"));
		passwordField = new JPasswordField();
		form.add(passwordField);

		add(form, BorderLayout.CENTER);

		JButton registerBtn = new JButton("Register");
		add(registerBtn, BorderLayout.SOUTH);

		registerBtn.addActionListener(e -> register());

		setVisible(true);
	}

	private void register() {

		String first = firstNameField.getText().trim();
		String last = lastNameField.getText().trim();
		String email = emailField.getText().trim().toLowerCase();
		char[] password = passwordField.getPassword();

		if (first.isEmpty() || last.isEmpty() || email.isEmpty() || password.length == 0) {
			JOptionPane.showMessageDialog(this, "All fields are required");
			return;
		}

		try {
			String hashed = PasswordUtil.hashPassword(new String(password));

			// Vault metadata will be initialized on first login
			User user = new User(first, last, email, hashed, role, null, null, null, 0);

			userDao.insert(user);

			auditDao.log("USER_REGISTER", email, email);

			JOptionPane.showMessageDialog(this, role + " registered successfully");

			dispose();

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
}
