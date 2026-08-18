package view.panels;

import database.dao.ChucVuDAO;
import database.dao.NhanVienDAO;
import database.dao.PhongBanDAO;
import model.ChucVu;
import model.NhanVien;
import model.PhongBan;
import util.ActivityLogger;
import util.DateUtil;
import util.ValidationUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public class AddEmployeesPanel extends JPanel {

    private JPanel rootPanel;

    private JLabel lblFormTitle;

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

    private static final String NO_SELECTION = "-- Không chọn --";
    private static final String AVATAR_DIR = "avatars";
    private static final int AVATAR_ICON_SIZE = 80;

    private final NhanVienDAO nhanVienDAO = new NhanVienDAO();
    private final PhongBanDAO phongBanDAO = new PhongBanDAO();
    private final ChucVuDAO chucVuDAO = new ChucVuDAO();

    private NhanVien editingEmployee;
    private String selectedAvatarPath;

    private Runnable onSavedOrCancelled;

    public AddEmployeesPanel() {
        mãNhânViênTextField.setEditable(false);
        setupActions();
        setupGenderRadios();
        resetToAddMode();
    }

    private void setupGenderRadios() {
        namRadioButton.addActionListener(e -> selectSingleGenderRadio(namRadioButton));
        nữRadioButton.addActionListener(e -> selectSingleGenderRadio(nữRadioButton));
        khácRadioButton.addActionListener(e -> selectSingleGenderRadio(khácRadioButton));
    }

    private void selectSingleGenderRadio(JRadioButton chosen) {
        namRadioButton.setSelected(chosen == namRadioButton);
        nữRadioButton.setSelected(chosen == nữRadioButton);
        khácRadioButton.setSelected(chosen == khácRadioButton);
    }

    private void populateCombos() {
        phòngBanComboBox.removeAllItems();
        phòngBanComboBox.addItem(NO_SELECTION);
        chứcVụComboBox.removeAllItems();
        chứcVụComboBox.addItem(NO_SELECTION);
        try {
            for (PhongBan pb : phongBanDAO.findAll()) {
                phòngBanComboBox.addItem(pb);
            }
            for (ChucVu cv : chucVuDAO.findAll()) {
                chứcVụComboBox.addItem(cv);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Không tải được danh sách phòng ban/chức vụ: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        loạiHợpĐồngComboBox.setModel(new DefaultComboBoxModel<>(
                new String[]{NO_SELECTION, "Thử việc", "Chính thức", "Thời vụ"}));
    }

    private void setupActions() {
        chọnNgàySinhButton.addActionListener(e -> pickDate(ngàySinhTextField));
        chọnNgàyVàoLàmButton.addActionListener(e -> pickDate(ngàyVàoLàmTextField));
        avatarUploadButton.addActionListener(e -> uploadAvatar());

        hủyButton.addActionListener(e -> {
            resetToAddMode();
            if (onSavedOrCancelled != null) {
                onSavedOrCancelled.run();
            }
        });

        lưuHồSơButton.addActionListener(e -> {
            if (validateForm() && saveEmployee()) {
                resetToAddMode();
                if (onSavedOrCancelled != null) {
                    onSavedOrCancelled.run();
                }
            }
        });
    }

    private void pickDate(JTextField targetField) {
        LocalDate initial = safeParse(targetField.getText());
        if (initial == null) {
            initial = LocalDate.now();
        }
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        spinner.setValue(java.sql.Date.valueOf(initial));

        int result = JOptionPane.showConfirmDialog(this, spinner, "Chọn ngày",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            java.util.Date selected = (java.util.Date) spinner.getValue();
            LocalDate localDate = selected.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            targetField.setText(DateUtil.format(localDate));
        }
    }

    private LocalDate safeParse(String ddMMyyyy) {
        try {
            return DateUtil.parse(ddMMyyyy);
        } catch (Exception e) {
            return null;
        }
    }

    private void uploadAvatar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Ảnh (JPEG, PNG)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File source = chooser.getSelectedFile();
        try {
            File avatarDir = new File(AVATAR_DIR);
            if (!avatarDir.exists() && !avatarDir.mkdirs()) {
                throw new IOException("Khong tao duoc thu muc " + avatarDir.getAbsolutePath());
            }
            String ext = getFileExtension(source.getName());
            String fileName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
            File dest = new File(avatarDir, fileName);
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            selectedAvatarPath = AVATAR_DIR + "/" + fileName;
            showAvatarPreview(selectedAvatarPath);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không sao chép được ảnh: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1);
    }

    private void showAvatarPreview(String relativePath) {
        if (relativePath == null) {
            avatarUploadButton.setIcon(null);
            avatarUploadButton.setText("Tải ảnh lên");
            return;
        }
        try {
            BufferedImage image = ImageIO.read(new File(relativePath));
            if (image == null) {
                throw new IOException("File anh khong hop le");
            }
            Image scaled = image.getScaledInstance(AVATAR_ICON_SIZE, AVATAR_ICON_SIZE, Image.SCALE_SMOOTH);
            avatarUploadButton.setIcon(new ImageIcon(scaled));
            avatarUploadButton.setText("");
        } catch (IOException e) {
            avatarUploadButton.setIcon(null);
            avatarUploadButton.setText("Tải ảnh lên");
        }
    }

    private boolean validateForm() {
        if (!ValidationUtil.isNotBlank(họVàTênTextField.getText())) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập họ và tên.");
            họVàTênTextField.requestFocus();
            return false;
        }
        LocalDate ngaySinh;
        try {
            ngaySinh = DateUtil.parse(ngàySinhTextField.getText());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ngày sinh không đúng định dạng dd/mm/yyyy.");
            ngàySinhTextField.requestFocus();
            return false;
        }
        if (ngaySinh != null && ngaySinh.isAfter(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "Ngày sinh không được ở tương lai.");
            ngàySinhTextField.requestFocus();
            return false;
        }
        String cccd = sốCCCDTextField.getText().trim();
        if (!cccd.isEmpty() && !ValidationUtil.isValidCCCD(cccd)) {
            JOptionPane.showMessageDialog(this, "Số CCCD/CMND không hợp lệ (9 hoặc 12 chữ số).");
            sốCCCDTextField.requestFocus();
            return false;
        }
        if (!ValidationUtil.isValidPhone(sốĐiệnThoạiTextField.getText())) {
            JOptionPane.showMessageDialog(this, "Số điện thoại không hợp lệ.");
            sốĐiệnThoạiTextField.requestFocus();
            return false;
        }
        if (!ValidationUtil.isValidEmail(emailTextField.getText())) {
            JOptionPane.showMessageDialog(this, "Email không hợp lệ.");
            emailTextField.requestFocus();
            return false;
        }
        try {
            DateUtil.parse(ngàyVàoLàmTextField.getText());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ngày vào làm không đúng định dạng dd/mm/yyyy.");
            ngàyVàoLàmTextField.requestFocus();
            return false;
        }
        String luong = mứcLươngCơBảnTextField.getText().trim();
        if (!luong.isEmpty()) {
            try {
                if (new BigDecimal(luong).signum() < 0) {
                    JOptionPane.showMessageDialog(this, "Lương cơ bản không được âm.");
                    mứcLươngCơBảnTextField.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Lương cơ bản phải là số.");
                mứcLươngCơBảnTextField.requestFocus();
                return false;
            }
        }
        return true;
    }

    private boolean saveEmployee() {
        NhanVien nv = buildEmployeeFromForm();
        try {
            if (editingEmployee == null) {
                String newId = nhanVienDAO.insert(nv);
                JOptionPane.showMessageDialog(this, "Đã thêm nhân viên mới (Mã NV: " + newId + ").");
            } else {
                nv.setMaNV(editingEmployee.getMaNV());
                nhanVienDAO.update(nv);
                ActivityLogger.logNhanVienAction("SỬA", nv);
                JOptionPane.showMessageDialog(this, "Đã cập nhật hồ sơ nhân viên.");
            }
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this, "Số CCCD hoặc Email đã tồn tại trong hệ thống.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Không thể lưu hồ sơ: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private NhanVien buildEmployeeFromForm() {
        NhanVien nv = new NhanVien();
        nv.setHoTen(họVàTênTextField.getText().trim());
        nv.setNgaySinh(safeParse(ngàySinhTextField.getText()));
        nv.setGioiTinh(namRadioButton.isSelected() ? "Nam" : nữRadioButton.isSelected() ? "Nữ" : "Khác");

        String cccd = sốCCCDTextField.getText().trim();
        nv.setSoCCCD(cccd.isEmpty() ? null : cccd);
        nv.setSoDienThoai(sốĐiệnThoạiTextField.getText().trim());
        nv.setEmail(emailTextField.getText().trim());

        String diaChi = địaChỉTextArea.getText().trim();
        nv.setDiaChi(diaChi.isEmpty() ? null : diaChi);
        nv.setAvatarPath(selectedAvatarPath);

        Object pbSel = phòngBanComboBox.getSelectedItem();
        nv.setMaPB(pbSel instanceof PhongBan ? ((PhongBan) pbSel).getMaPB() : null);
        Object cvSel = chứcVụComboBox.getSelectedItem();
        nv.setMaCV(cvSel instanceof ChucVu ? ((ChucVu) cvSel).getMaCV() : null);

        nv.setNgayVaoLam(safeParse(ngàyVàoLàmTextField.getText()));
        Object loaiHD = loạiHợpĐồngComboBox.getSelectedItem();
        nv.setLoaiHopDong(loaiHD == null || NO_SELECTION.equals(loaiHD) ? null : loaiHD.toString());

        String luong = mứcLươngCơBảnTextField.getText().trim();
        nv.setMucLuongCoBan(luong.isEmpty() ? BigDecimal.ZERO : new BigDecimal(luong));

        nv.setTrangThai(editingEmployee != null ? editingEmployee.getTrangThai() : "DangLamViec");
        return nv;
    }

    public void loadForEdit(NhanVien nv) {
        populateCombos();
        this.editingEmployee = nv;
        lblFormTitle.setText("Sửa thông tin nhân viên");
        lưuHồSơButton.setText("Cập nhật hồ sơ");
        mãNhânViênTextField.setText(nv.getMaNV());

        họVàTênTextField.setText(nv.getHoTen() == null ? "" : nv.getHoTen());
        ngàySinhTextField.setText(DateUtil.format(nv.getNgaySinh()));
        selectGender(nv.getGioiTinh());
        sốCCCDTextField.setText(nv.getSoCCCD() == null ? "" : nv.getSoCCCD());
        sốĐiệnThoạiTextField.setText(nv.getSoDienThoai() == null ? "" : nv.getSoDienThoai());
        emailTextField.setText(nv.getEmail() == null ? "" : nv.getEmail());
        địaChỉTextArea.setText(nv.getDiaChi() == null ? "" : nv.getDiaChi());
        selectedAvatarPath = nv.getAvatarPath();
        showAvatarPreview(selectedAvatarPath);

        selectPhongBanById(nv.getMaPB());
        selectChucVuById(nv.getMaCV());
        ngàyVàoLàmTextField.setText(DateUtil.format(nv.getNgayVaoLam()));
        selectLoaiHopDong(nv.getLoaiHopDong());
        mứcLươngCơBảnTextField.setText(nv.getMucLuongCoBan() == null ? "" : nv.getMucLuongCoBan().toPlainString());
    }

    public void resetToAddMode() {
        populateCombos();
        this.editingEmployee = null;
        lblFormTitle.setText("Thêm mới nhân viên");
        lưuHồSơButton.setText("Lưu hồ sơ");
        mãNhânViênTextField.setText("Tự động");
        clearForm();
    }

    private void clearForm() {
        họVàTênTextField.setText("");
        ngàySinhTextField.setText("");
        selectSingleGenderRadio(namRadioButton);
        sốCCCDTextField.setText("");
        sốĐiệnThoạiTextField.setText("");
        emailTextField.setText("");
        địaChỉTextArea.setText("");
        if (phòngBanComboBox.getItemCount() > 0) {
            phòngBanComboBox.setSelectedIndex(0);
        }
        if (chứcVụComboBox.getItemCount() > 0) {
            chứcVụComboBox.setSelectedIndex(0);
        }
        ngàyVàoLàmTextField.setText("");
        if (loạiHợpĐồngComboBox.getItemCount() > 0) {
            loạiHợpĐồngComboBox.setSelectedIndex(0);
        }
        mứcLươngCơBảnTextField.setText("");
        selectedAvatarPath = null;
        showAvatarPreview(null);
    }

    private void selectGender(String gioiTinh) {
        if ("Nam".equals(gioiTinh)) {
            selectSingleGenderRadio(namRadioButton);
        } else if ("Nữ".equals(gioiTinh)) {
            selectSingleGenderRadio(nữRadioButton);
        } else {
            selectSingleGenderRadio(khácRadioButton);
        }
    }

    private void selectPhongBanById(String maPB) {
        if (maPB != null) {
            for (int i = 0; i < phòngBanComboBox.getItemCount(); i++) {
                Object item = phòngBanComboBox.getItemAt(i);
                if (item instanceof PhongBan && maPB.equals(((PhongBan) item).getMaPB())) {
                    phòngBanComboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
        phòngBanComboBox.setSelectedIndex(0);
    }

    private void selectChucVuById(String maCV) {
        if (maCV != null) {
            for (int i = 0; i < chứcVụComboBox.getItemCount(); i++) {
                Object item = chứcVụComboBox.getItemAt(i);
                if (item instanceof ChucVu && maCV.equals(((ChucVu) item).getMaCV())) {
                    chứcVụComboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
        chứcVụComboBox.setSelectedIndex(0);
    }

    private void selectLoaiHopDong(String loaiHopDong) {
        if (loaiHopDong != null) {
            for (int i = 0; i < loạiHợpĐồngComboBox.getItemCount(); i++) {
                if (loaiHopDong.equals(loạiHợpĐồngComboBox.getItemAt(i))) {
                    loạiHợpĐồngComboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
        loạiHợpĐồngComboBox.setSelectedIndex(0);
    }

    public void setOnSavedOrCancelled(Runnable callback) {
        this.onSavedOrCancelled = callback;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
