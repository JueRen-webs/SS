package ui;

import dao.UserDAO;
import dao.AuditLogDAO;
import model.User;
import security.PasswordUtil;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

	private JTextField emailField;
	private JPasswordField passwordField;

	private final UserDAO userDao = new UserDAO();
	private final AuditLogDAO auditDao = new AuditLogDAO();

	public LoginFrame() {

		setTitle("User Login");
		setSize(360, 220);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout(10, 10));

		// ===== FORM =====
		JPanel form = new JPanel(new GridLayout(2, 2, 5, 5));
		form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		form.add(new JLabel("Email:"));
		emailField = new JTextField();
		form.add(emailField);

		form.add(new JLabel("Password:"));
		passwordField = new JPasswordField();
		form.add(passwordField);

		add(form, BorderLayout.CENTER);

		// ===== BUTTONS =====
		JButton loginBtn = new JButton("Login");
		JButton registerUserBtn = new JButton("Register User");
		JButton registerAdminBtn = new JButton("Register Admin");

		// ✅ FIXED HERE
		if (userDao.adminExists()) {
			registerAdminBtn.setEnabled(false);
			registerAdminBtn.setToolTipText("Admin already registered");
		}

		JPanel btnPanel = new JPanel();
		btnPanel.add(loginBtn);
		btnPanel.add(registerUserBtn);
		btnPanel.add(registerAdminBtn);
		add(btnPanel, BorderLayout.SOUTH);

		loginBtn.addActionListener(e -> login());
		registerUserBtn.addActionListener(e -> new RegisterFrame("USER"));
		registerAdminBtn.addActionListener(e -> new RegisterFrame("ADMIN"));

		setVisible(true);
	}

	private void login() {

		String email = emailField.getText().trim().toLowerCase();
		char[] password = passwordField.getPassword();

		if (email.isEmpty() || password.length == 0) {
			JOptionPane.showMessageDialog(this, "Please fill all fields");
			return;
		}

		try {
			User user = userDao.findByEmail(email);

			// ❌ LOGIN FAILED
			if (user == null || !PasswordUtil.verifyPassword(new String(password), user.getPassword())) {

				auditDao.log("LOGIN_FAIL", email, email);

				JOptionPane.showMessageDialog(this, "Invalid email or password", "Login Failed",
						JOptionPane.ERROR_MESSAGE);

				passwordField.setText("");
				return;
			}

			// ✅ LOGIN SUCCESS
			auditDao.log("LOGIN_SUCCESS", email, email);

			// store session password (for vault unlock)
			user.setSessionPassword(password);

			JOptionPane.showMessageDialog(this, "Welcome " + user.getFirstName());

			dispose();
			new FileVaultFrame(user);

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Login failed. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
