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

/**
 * Màn hình Đăng nhập.
 * Các field bên dưới PHẢI trùng tên với thuộc tính "binding" trong Login.form
 * để IntelliJ GUI Designer tự sinh phần layout ($$$setupUI$$$) khi build.
 */
public class Login extends JFrame {

    // ==== Các field khớp binding trong Login.form ====
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

    // trạng thái ẩn/hiện mật khẩu
    private boolean isPasswordVisible = false;
    private final char defaultEchoChar;

    // ---- Quy tắc kiểm tra ĐỊNH DẠNG (chưa nối DB, chưa check tài khoản có tồn tại hay không) ----
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

    // ================== STYLE (giao dien co ban, khong dung mau) ==================
    private void styleComponents() {
        // Nut an/hien mat khau: chu "Hien"/"An" thay vi icon, khong ve vien nut
        btnTogglePassword.setBorderPainted(false);
        btnTogglePassword.setContentAreaFilled(false);
        btnTogglePassword.setFocusPainted(false);
        btnTogglePassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTogglePassword.setText("Hiện");

        // Con tro tay khi ruot qua link de nguoi dung biet co the bam duoc
        lblForgotPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // ================== SỰ KIỆN ==================
    private void attachEvents() {

        // Toggle hiện/ẩn mật khẩu
        btnTogglePassword.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                txtPassword.setEchoChar((char) 0); // hiện chữ thật
                btnTogglePassword.setText("Ẩn");
            } else {
                txtPassword.setEchoChar(defaultEchoChar); // ẩn lại
                btnTogglePassword.setText("Hiện");
            }
        });

        // Nút Đăng nhập
        btnLogin.addActionListener(e -> handleLogin());

        // Cho phép Enter ở ô mật khẩu = bấm Đăng nhập
        txtPassword.addActionListener(e -> handleLogin());

        // Quên mật khẩu
        lblForgotPassword.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // TODO: mở form ForgotPassword hoặc gửi email reset
                JOptionPane.showMessageDialog(Login.this,
                        "Chức năng khôi phục mật khẩu đang được phát triển.",
                        "Quên mật khẩu",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Yêu cầu cấp tài khoản
        lblRegister.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // TODO: mở form Register / gửi yêu cầu cho admin
                JOptionPane.showMessageDialog(Login.this,
                        "Vui lòng liên hệ quản trị viên để được cấp tài khoản.",
                        "Yêu cầu cấp tài khoản",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    // ================== LOGIC ĐĂNG NHẬP ==================
    // Buoc 1: kiem tra dinh dang (email hop le + mat khau du do dai).
    // Buoc 2: kiem tra that voi CSDL qua TaiKhoanDAO (BCrypt.checkpw + trang thai tai khoan).
    private void handleLogin() {
        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        // ---- Validate ĐỊNH DẠNG (chưa check với CSDL) ----
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

        // ---- Kiem tra voi CSDL (BCrypt.checkpw + trang thai tai khoan) ----
        btnLogin.setEnabled(false);
        try {
            TaiKhoan taiKhoan = new TaiKhoanDAO().checkLogin(username, password);
            if (taiKhoan == null) {
                showError("Sai tên đăng nhập hoặc mật khẩu, hoặc tài khoản đã bị khóa.");
                return;
            }

            if (chkRemember.isSelected()) {
                // TODO: lưu thông tin đăng nhập (vd: Preferences API hoặc file cấu hình)
            }

            Session.setCurrentAccount(taiKhoan);
            new MainFrame().setVisible(true); // mo man hinh chinh (co Dashboard ben trong)
            this.dispose();                   // dong man hinh Login
        } catch (SQLException e) {
            showError("Không thể kết nối cơ sở dữ liệu: " + e.getMessage());
        } finally {
            btnLogin.setEnabled(true);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    // ================== CHẠY THỬ ==================
    // Ghi chú: entry point CHÍNH THỨC của app là Main.java (package view).
    // Ham main() nay giu lai chi de tien test rieng man hinh Login khi can.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}