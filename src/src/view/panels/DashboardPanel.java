package view.panels;

import javax.swing.*;
import java.awt.*;

/**
 * Noi dung man hinh "Tong quan" (Dashboard).
 * Duoc nhung vao contentPanel cua MainFrame qua CardLayout,
 * KHONG con la mot JFrame rieng.
 *
 * Cac field ben duoi PHAI trung ten voi thuoc tinh "binding"
 * trong DashboardPanel.form.
 */
public class DashboardPanel extends JPanel {

    // ===== Sinh tu DashboardPanel.form =====
    private JPanel rootPanel;
    private JButton tảiBáoCáoButton;

    private JLabel lblTongSoNhanVienValue;
    private JLabel lblTongSoNhanVienDelta;
    private JLabel lblSoPhongBanValue;
    private JLabel lblSoPhongBanDelta;
    private JLabel lblNhanVienMoiValue;
    private JLabel lblNhanVienMoiDelta;
    private JLabel lblChoXuLyValue;
    private JLabel lblChoXuLyDelta;

    private JPanel chartPanel;         // holder de nhung bieu do thong ke theo phong ban
    private JPanel activityListPanel;  // holder de nhung danh sach hoat dong gan day
    private JButton xemTấtCảButton;

    public DashboardPanel() {
        setupActions();
        loadDashboardData();
    }

    private void setupActions() {
        tảiBáoCáoButton.addActionListener(e -> exportReport());
        xemTấtCảButton.addActionListener(e -> showAllActivities());
    }

    /** TODO: goi DAO/service de lay so lieu that va do vao cac JLabel/chartPanel. */
    private void loadDashboardData() {
        // vi du:
        // int tongNV = employeeDAO.countAll();
        // lblTongSoNhanVienValue.setText(String.valueOf(tongNV));
    }

    private void exportReport() {
        // TODO: xuat bao cao (PDF/Excel...)
    }

    private void showAllActivities() {
        // TODO: mo dialog hoac chuyen sang man hinh danh sach hoat dong day du
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
