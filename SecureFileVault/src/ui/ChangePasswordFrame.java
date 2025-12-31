package ui;

import dao.UserDAO;
import security.PasswordUtil;

import javax.swing.*;
import java.awt.*;

public class ChangePasswordFrame extends JFrame {

	private JPasswordField oldPwField;
	private JPasswordField newPwField;

	private final String adminEmail;
	private final UserDAO dao = new UserDAO();

	public ChangePasswordFrame(String adminEmail) {
		this.adminEmail = adminEmail;

		setTitle("Change Password");
		setSize(350, 220);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(10, 10));

		JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
		form.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		form.add(new JLabel("Old Password:"));
		oldPwField = new JPasswordField();
		form.add(oldPwField);

		form.add(new JLabel("New Password:"));
		newPwField = new JPasswordField();
		form.add(newPwField);

		add(form, BorderLayout.CENTER);

		JButton saveBtn = new JButton("Update");
		JButton cancelBtn = new JButton("Cancel");

		JPanel btnPanel = new JPanel();
		btnPanel.add(saveBtn);
		btnPanel.add(cancelBtn);
		add(btnPanel, BorderLayout.SOUTH);

		saveBtn.addActionListener(e -> updatePassword());
		cancelBtn.addActionListener(e -> dispose());

		setVisible(true);
	}

	private void updatePassword() {
		String oldPw = new String(oldPwField.getPassword()).trim();
		String newPw = new String(newPwField.getPassword()).trim();

		if (oldPw.isEmpty() || newPw.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please fill all fields", "Validation", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (newPw.length() < 6) {
			JOptionPane.showMessageDialog(this, "Password must be at least 6 characters", "Validation",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {
			String oldHash = dao.findByEmail(adminEmail).getPassword();

			if (!PasswordUtil.verifyPassword(oldPw, oldHash)) {
				JOptionPane.showMessageDialog(this, "Old password incorrect", "Security", JOptionPane.ERROR_MESSAGE);
				return;
			}

			String newHash = PasswordUtil.hashPassword(newPw);
			dao.updatePasswordByEmail(adminEmail, newHash);

			JOptionPane.showMessageDialog(this, "Password updated successfully");
			dispose();

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Operation failed", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
