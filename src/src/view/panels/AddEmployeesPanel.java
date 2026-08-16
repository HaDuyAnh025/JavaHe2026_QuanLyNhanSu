package view.panels;

import javax.swing.*;

/**
 * Noi dung man hinh "Them nhan vien".
 * Cac field PHAI trung ten voi binding trong AddEmployeesPanel.form.
 */
public class AddEmployeesPanel extends JPanel {

    // ===== Sinh tu AddEmployeesPanel.form =====
    private JPanel rootPanel;

    private JTextField họVàTênTextField;
    private JTextField ngàySinhTextField;
    private JButton chọnNgàySinhButton;
    private JRadioButton namRadioButton;
    private JRadioButton nữRadioButton;
    private JRadioButton khácRadioButton;
    private JTextField sốCCCDTextField;
    private JTextField sốĐiệnThoạiTextField;
    private JTextField emailTextField;
    private JTextArea địaChỉTextArea;
    private JButton avatarUploadButton;

    private JTextField mãNhânViênTextField;
    private JComboBox phòngBanComboBox;
    private JComboBox chứcVụComboBox;
    private JTextField ngàyVàoLàmTextField;
    private JButton chọnNgàyVàoLàmButton;
    private JComboBox loạiHợpĐồngComboBox;
    private JTextField mứcLươngCơBảnTextField;

    private JButton hủyButton;
    private JButton lưuHồSơButton;

    /** Callback de MainFrame quay lai danh sach sau khi luu hoac huy. */
    private Runnable onSavedOrCancelled;

    public AddEmployeesPanel() {
        setupActions();
    }

    private void setupActions() {
        chọnNgàySinhButton.addActionListener(e -> pickDate(ngàySinhTextField));
        chọnNgàyVàoLàmButton.addActionListener(e -> pickDate(ngàyVàoLàmTextField));
        avatarUploadButton.addActionListener(e -> uploadAvatar());

        hủyButton.addActionListener(e -> {
            clearForm();
            if (onSavedOrCancelled != null) {
                onSavedOrCancelled.run();
            }
        });

        lưuHồSơButton.addActionListener(e -> {
            if (validateForm()) {
                saveEmployee();
                clearForm();
                if (onSavedOrCancelled != null) {
                    onSavedOrCancelled.run();
                }
            }
        });
    }

    /** TODO: mo JDatePicker hoac dialog chon ngay, gan ket qua vao textField tuong ung. */
    private void pickDate(JTextField targetField) {
    }

    /** TODO: mo JFileChooser de chon anh dai dien. */
    private void uploadAvatar() {
    }

    /** TODO: kiem tra du lieu bat buoc (ho ten, CCCD, email, so dien thoai...). */
    private boolean validateForm() {
        if (họVàTênTextField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui long nhap ho va ten.");
            return false;
        }
        return true;
    }

    /** TODO: goi EmployeeDAO.insert(...) voi du lieu tu form. */
    private void saveEmployee() {
        JOptionPane.showMessageDialog(this, "Da luu ho so nhan vien.");
    }

    private void clearForm() {
        họVàTênTextField.setText("");
        ngàySinhTextField.setText("");
        sốCCCDTextField.setText("");
        sốĐiệnThoạiTextField.setText("");
        emailTextField.setText("");
        địaChỉTextArea.setText("");
        mãNhânViênTextField.setText("");
        ngàyVàoLàmTextField.setText("");
        mứcLươngCơBảnTextField.setText("");
    }

    public void setOnSavedOrCancelled(Runnable callback) {
        this.onSavedOrCancelled = callback;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
