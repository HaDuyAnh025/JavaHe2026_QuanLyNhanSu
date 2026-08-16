package view.panels;

import database.dao.TaiKhoanDAO;
import model.Session;
import model.TaiKhoan;
import util.ValidationUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Man hinh "Phan quyen" / Quan ly tai khoan - chi danh cho Admin.
 * Cho phep tao tai khoan moi, khoa/mo khoa tai khoan, va doi vai tro (Admin/NhanVien).
 * Viet thuan Java Swing (khong dung GUI Designer) vi day la man hinh
 * moi hoan toan, khong can khop thiet ke .form co san.
 */
public class AccountManagementPanel extends JPanel {

    private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();

    private DefaultTableModel tableModel;
    private JTable accountTable;
    private List<TaiKhoan> currentRows;

    public AccountManagementPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        reloadData();
    }

    private JComponent buildHeader() {
        JLabel title = new JLabel("Quản lý tài khoản (Phân quyền)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitle = new JLabel("Tạo tài khoản, khóa/mở khóa, gán vai trò Admin hoặc Nhân viên.");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JComponent buildTable() {
        String[] columns = {"Mã TK", "Tên đăng nhập", "Vai trò", "Trạng thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        accountTable = new JTable(tableModel);
        accountTable.setRowHeight(28);
        return new JScrollPane(accountTable);
    }

    private JComponent buildActionBar() {
        JButton createButton = new JButton("+ Tạo tài khoản mới");
        createButton.addActionListener(e -> openCreateAccountDialog());

        JButton toggleLockButton = new JButton("Khóa / Mở khóa");
        toggleLockButton.addActionListener(e -> toggleLockSelected());

        JButton changeRoleButton = new JButton("Đổi vai trò");
        changeRoleButton.addActionListener(e -> changeRoleSelected());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.add(createButton);
        bar.add(toggleLockButton);
        bar.add(changeRoleButton);
        return bar;
    }

    private void reloadData() {
        try {
            currentRows = taiKhoanDAO.findAll();
            tableModel.setRowCount(0);
            for (TaiKhoan tk : currentRows) {
                tableModel.addRow(new Object[]{
                        tk.getMaTK(),
                        tk.getTenDangNhap(),
                        tk.getVaiTro(),
                        "HoatDong".equals(tk.getTrangThai()) ? "Hoạt động" : "Đã khóa"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Không tải được danh sách tài khoản: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private TaiKhoan getSelectedAccount() {
        int row = accountTable.getSelectedRow();
        if (row < 0 || currentRows == null || row >= currentRows.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một tài khoản trong bảng.");
            return null;
        }
        return currentRows.get(row);
    }

    private void toggleLockSelected() {
        TaiKhoan tk = getSelectedAccount();
        if (tk == null) {
            return;
        }
        if (isSelf(tk)) {
            JOptionPane.showMessageDialog(this, "Không thể tự khóa chính tài khoản đang đăng nhập.");
            return;
        }
        String newStatus = "HoatDong".equals(tk.getTrangThai()) ? "KhoaTaiKhoan" : "HoatDong";
        try {
            taiKhoanDAO.setTrangThai(tk.getMaTK(), newStatus);
            reloadData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Không cập nhật được trạng thái: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changeRoleSelected() {
        TaiKhoan tk = getSelectedAccount();
        if (tk == null) {
            return;
        }
        if (isSelf(tk)) {
            JOptionPane.showMessageDialog(this, "Không thể tự đổi vai trò của chính tài khoản đang đăng nhập.");
            return;
        }
        String[] options = {"Admin", "NhanVien"};
        String chosen = (String) JOptionPane.showInputDialog(this, "Chọn vai trò mới cho \"" + tk.getTenDangNhap() + "\":",
                "Đổi vai trò", JOptionPane.PLAIN_MESSAGE, null, options, tk.getVaiTro());
        if (chosen == null) {
            return;
        }
        try {
            taiKhoanDAO.setVaiTro(tk.getMaTK(), chosen);
            reloadData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Không cập nhật được vai trò: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isSelf(TaiKhoan tk) {
        TaiKhoan current = Session.getCurrentAccount();
        return current != null && current.getMaTK() == tk.getMaTK();
    }

    private void openCreateAccountDialog() {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"NhanVien", "Admin"});

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Tên đăng nhập:"));
        form.add(usernameField);
        form.add(new JLabel("Mật khẩu (tối thiểu 6 ký tự):"));
        form.add(passwordField);
        form.add(new JLabel("Xác nhận mật khẩu:"));
        form.add(confirmField);
        form.add(new JLabel("Vai trò:"));
        form.add(roleCombo);

        int result = JOptionPane.showConfirmDialog(this, form, "Tạo tài khoản mới",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập.");
            return;
        }
        if (!ValidationUtil.isValidEmail(username)) {
            JOptionPane.showMessageDialog(this, "Tên đăng nhập phải là email hợp lệ (vd: ten@congty.com) - " +
                    "vì màn Đăng nhập yêu cầu đúng định dạng này, đặt tên khác sẽ không đăng nhập được.");
            return;
        }
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp.");
            return;
        }

        try {
            if (taiKhoanDAO.existsUsername(username)) {
                JOptionPane.showMessageDialog(this, "Tên đăng nhập đã tồn tại.");
                return;
            }
            String vaiTro = (String) roleCombo.getSelectedItem();

            taiKhoanDAO.create(username, password, vaiTro);
            JOptionPane.showMessageDialog(this, "Đã tạo tài khoản thành công.");
            reloadData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Không tạo được tài khoản: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JPanel getRootPanel() {
        return this;
    }
}
