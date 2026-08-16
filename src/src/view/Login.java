package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

// TODO: đổi import DAO cho khớp package thật của bạn, ví dụ:
// import qlnhansu.database.dao.TaiKhoanDAO;
// import qlnhansu.model.TaiKhoan;

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

    // ================== STYLE (theo Figma) ==================
    private void styleComponents() {
        // Nền tổng thể
        rootPanel.setBackground(Color.WHITE);
        cardPanel.setBackground(Color.WHITE);

        // Nút Đăng nhập: nền xanh, chữ trắng, bo góc
        btnLogin.setBackground(new Color(0x2F, 0x5B, 0xE8)); // xanh dương giống hình
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setOpaque(true);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Nút ẩn/hiện mật khẩu: giống icon, không viền
        btnTogglePassword.setBorderPainted(false);
        btnTogglePassword.setContentAreaFilled(false);
        btnTogglePassword.setFocusPainted(false);
        btnTogglePassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTogglePassword.setText("👁");

        // Link "Quên mật khẩu?" và "Yêu cầu cấp tài khoản"
        Color linkColor = new Color(0x2F, 0x5B, 0xE8);
        lblForgotPassword.setForeground(linkColor);
        lblForgotPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));

        lblRegister.setForeground(linkColor);
        lblRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblRegister.setFont(lblRegister.getFont().deriveFont(Font.BOLD));

        lblNoAccount.setForeground(new Color(0x66, 0x66, 0x66));
    }

    // ================== SỰ KIỆN ==================
    private void attachEvents() {

        // Toggle hiện/ẩn mật khẩu
        btnTogglePassword.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                txtPassword.setEchoChar((char) 0); // hiện chữ thật
                btnTogglePassword.setText("🙈");
            } else {
                txtPassword.setEchoChar(defaultEchoChar); // ẩn lại
                btnTogglePassword.setText("👁");
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
    // LƯU Ý: CHƯA nối CSDL, nên bước này CHỈ kiểm tra ĐÚNG ĐỊNH DẠNG
    // (email hợp lệ + mật khẩu đủ độ dài), KHÔNG kiểm tra tài khoản
    // có thật sự tồn tại hay mật khẩu có khớp hay không.
    // Nhập đúng cú pháp -> coi như đăng nhập thành công -> vào Dashboard.
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

        // ---- TODO: sau này nối DAO thật, thay đoạn dưới bằng: ----
        // TaiKhoanDAO dao = new TaiKhoanDAO();
        // TaiKhoan tk = dao.checkLogin(username, password);
        // if (tk != null) { ... mo Dashboard ... } else { showError(...); }

        // ---- Đúng định dạng là cho vào thẳng Dashboard (demo, chưa check DB) ----
        if (chkRemember.isSelected()) {
            // TODO: lưu thông tin đăng nhập (vd: Preferences API hoặc file cấu hình)
        }
        btnLogin.setEnabled(false);
        new MainFrame().setVisible(true); // mo man hinh chinh (co Dashboard ben trong)
        this.dispose();                   // dong man hinh Login
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