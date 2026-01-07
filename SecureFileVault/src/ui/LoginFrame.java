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

        // 定义输入框样式：灰色边框 + 左右内边距
        LineBorder lineBorder = new LineBorder(new Color(189, 195, 199), 1);
        EmptyBorder padding = new EmptyBorder(5, 8, 5, 8);
        CompoundBorder niceBorder = new CompoundBorder(lineBorder, padding);

        form.add(new JLabel("Email Address:"));
        emailField = new JTextField();
        emailField.setBorder(niceBorder); // 应用新边框
        form.add(emailField);

        form.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        passwordField.setBorder(niceBorder); // 应用新边框
        form.add(passwordField);

        add(form, BorderLayout.CENTER);

        // --- 3. Button Panel (Styled Buttons) ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        
        JButton loginBtn = new JButton("Login");
        JButton registerUserBtn = new JButton("Register User");
        JButton registerAdminBtn = new JButton("Register Admin");

        // 使用下方定义的方法来设置按钮颜色
        styleButton(loginBtn, new Color(46, 204, 113));      // 绿色
        styleButton(registerUserBtn, new Color(52, 152, 219)); // 蓝色
        styleButton(registerAdminBtn, new Color(220, 220, 220)); // 灰色

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
    } // <--- 构造函数结束

    // ✅ 这个方法必须在构造函数外面
    private void styleButton(JButton btn, Color bgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);        
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setOpaque(true);
        btn.setBorderPainted(false); // 让按钮看起来更扁平
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
            String rawPassword = new String(password);

            if (user == null || !PasswordUtil.verifyPassword(rawPassword, user.getPassword())) {
                auditDao.log("LOGIN_FAIL", email, email);
                JOptionPane.showMessageDialog(this, "Invalid credentials", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

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
} // <--- 类结束