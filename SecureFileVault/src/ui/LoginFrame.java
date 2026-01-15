package ui;

import dao.UserDAO;
import dao.AuditLogDAO;
import model.User;
import security.PasswordUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Arrays;

public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private int failedAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;
    private JButton loginBtn;

    private final UserDAO userDao = new UserDAO();
    private final AuditLogDAO auditDao = new AuditLogDAO();
    

    public LoginFrame() {
        setTitle("Secure Vault - Login");
        setSize(420, 320); 
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- 1. Header (Monospaced + Dark Blue) ---
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(44, 62, 80)); 
        JLabel headerLabel = new JLabel("SYSTEM AUTHENTICATION");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        headerPanel.add(headerLabel);
        add(headerPanel, BorderLayout.NORTH);

        // --- 2. Form Panel (Custom Inputs) ---
        JPanel form = new JPanel(new GridLayout(2, 2, 10, 20));
        form.setBorder(new EmptyBorder(30, 40, 20, 40));

        LineBorder lineBorder = new LineBorder(new Color(189, 195, 199), 1);
        EmptyBorder padding = new EmptyBorder(5, 8, 5, 8);
        CompoundBorder niceBorder = new CompoundBorder(lineBorder, padding);

        form.add(new JLabel("Email Address:"));
        emailField = new JTextField();
        emailField.setBorder(niceBorder); 
        form.add(emailField);

        form.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        passwordField.setBorder(niceBorder); 
        form.add(passwordField);

        add(form, BorderLayout.CENTER);

        // --- 3. Button Panel (Styled Buttons) ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        
        loginBtn = new JButton("Login");
        JButton registerUserBtn = new JButton("Register User");
        JButton registerAdminBtn = new JButton("Register Admin");

        styleButton(loginBtn, new Color(46, 204, 113));
        styleButton(registerUserBtn, new Color(52, 152, 219)); 
        styleButton(registerAdminBtn, new Color(220, 220, 220)); 

        if (userDao.adminExists()) {
            registerAdminBtn.setEnabled(false);
        }

        btnPanel.add(loginBtn);
        btnPanel.add(registerUserBtn);
        btnPanel.add(registerAdminBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Listeners
        loginBtn.addActionListener(e -> login());
        registerUserBtn.addActionListener(e -> new RegisterFrame("USER"));
        registerAdminBtn.addActionListener(e -> new RegisterFrame("ADMIN"));

        getRootPane().setDefaultButton(loginBtn);
        setVisible(true);
    } 
    private void styleButton(JButton btn, Color bgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);        
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setOpaque(true);
        btn.setBorderPainted(false); 
    }


    private void login() {

    	if (failedAttempts >= MAX_ATTEMPTS) {
            JOptionPane.showMessageDialog(this, 
                "System is temporarily locked due to too many failed attempts.\nPlease wait.", 
                "Login Locked", 
                JOptionPane.WARNING_MESSAGE);
            return; 
        }
    	
    String email = emailField.getText().trim().toLowerCase();
    char[] password = passwordField.getPassword();

    if (email.isEmpty() || password.length == 0) {
        JOptionPane.showMessageDialog(this, "Please fill all fields");
        return;
    }

    try {
        User user = userDao.findByEmail(email);
        String rawPassword = new String(password);

        if (user == null || !PasswordUtil.verifyPassword(rawPassword, user.getPassword())) {
            auditDao.log("LOGIN_FAIL", email, email);
            
            failedAttempts++;
            
            if (failedAttempts >= MAX_ATTEMPTS) {
                loginBtn.setEnabled(false);
                loginBtn.setText("Locked (10s)");
                
                JOptionPane.showMessageDialog(this, "Too many failed attempts. Locked for 10s.", "Locked", JOptionPane.WARNING_MESSAGE);
                
                int[] timeLeft = {10};
                Timer countdownTimer = new Timer(1000, null);
                countdownTimer.addActionListener(e -> {
                    timeLeft[0]--;
                    
                    if (timeLeft[0] <= 0) {
                        countdownTimer.stop();      
                        failedAttempts = 0;        
                        loginBtn.setEnabled(true);  
                        loginBtn.setText("Login");  
                    } else {
                        loginBtn.setText("Locked (" + timeLeft[0] + "s)");
                    }
                });
                
                countdownTimer.start();
                
            } else {
                int remaining = MAX_ATTEMPTS - failedAttempts;
                JOptionPane.showMessageDialog(this, "Invalid credentials. Attempts remaining: " + remaining, "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        failedAttempts = 0; 
        auditDao.log("LOGIN_SUCCESS", email, email);
        user.setSessionPassword(password);
        dispose();
        new FileVaultFrame(user);

    } catch (Exception ex) {
        ex.printStackTrace();
    } finally {
        Arrays.fill(password, '0');
    }
}
}