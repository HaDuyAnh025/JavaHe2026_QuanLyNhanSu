package view;

import database.dao.TaiKhoanDAO;
import model.Session;
import model.TaiKhoan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Login extends JFrame {

    private JPanel rootPanel;
    private JPanel cardPanel;
    private JTextField txtUsername;
    private JPanel passwordPanel;
    private JPasswordField txtPassword;
    private JButton btnTogglePassword;
    private JPanel optionsPanel;
    private JCheckBox chkRemember;
    private JLabel lblForgotPassword;
    private JButton btnLogin;
    private JPanel registerPanel;
    private JLabel lblNoAccount;
    private JLabel lblRegister;

    private boolean isPasswordVisible = false;
    private final char defaultEchoChar;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    public Login() {
        setTitle("Đăng nhập hệ thống");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(rootPanel);
        setSize(520, 640);
        setMinimumSize(new Dimension(420, 560));
        setLocationRelativeTo(null);
        setResizable(false);

        defaultEchoChar = txtPassword.getEchoChar();

        styleComponents();
        attachEvents();
    }

    private void styleComponents() {
        btnTogglePassword.setBorderPainted(false);
        btnTogglePassword.setContentAreaFilled(false);
        btnTogglePassword.setFocusPainted(false);
        btnTogglePassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTogglePassword.setText("Hiện");

        lblForgotPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void attachEvents() {

        btnTogglePassword.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                txtPassword.setEchoChar((char) 0);
                btnTogglePassword.setText("Ẩn");
            } else {
                txtPassword.setEchoChar(defaultEchoChar);
                btnTogglePassword.setText("Hiện");
            }
        });

        btnLogin.addActionListener(e -> handleLogin());

        txtPassword.addActionListener(e -> handleLogin());

        lblForgotPassword.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(Login.this,
                        "Chức năng khôi phục mật khẩu đang được phát triển.",
                        "Quên mật khẩu",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        lblRegister.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(Login.this,
                        "Vui lòng liên hệ quản trị viên để được cấp tài khoản.",
                        "Yêu cầu cấp tài khoản",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private void handleLogin() {
        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập / email.");
            txtUsername.requestFocus();
            return;
        }
        if (!EMAIL_PATTERN.matcher(username).matches()) {
            showError("Email không đúng định dạng (vd: ten@domain.com).");
            txtUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("Vui lòng nhập mật khẩu.");
            txtPassword.requestFocus();
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            showError("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự.");
            txtPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        try {
            TaiKhoan taiKhoan = new TaiKhoanDAO().checkLogin(username, password);
            if (taiKhoan == null) {
                showError("Sai tên đăng nhập hoặc mật khẩu, hoặc tài khoản đã bị khóa.");
                return;
            }

            if (chkRemember.isSelected()) {
            }

            Session.setCurrentAccount(taiKhoan);
            new MainFrame().setVisible(true);
            this.dispose();
        } catch (SQLException e) {
            showError("Không thể kết nối cơ sở dữ liệu: " + e.getMessage());
        } finally {
            btnLogin.setEnabled(true);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}