package view.panels;

import database.dao.NhanVienDAO;
import database.dao.PhongBanDAO;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.Map;

public class DashboardPanel extends JPanel {

    private JPanel rootPanel;
    private JButton tảiBáoCáoButton;

    private JLabel lblTongSoNhanVienValue;
    private JLabel lblTongSoNhanVienDelta;
    private JLabel lblSoPhongBanValue;
    private JLabel lblSoPhongBanDelta;
    private JLabel lblNhanVienMoiValue;
    private JLabel lblNhanVienMoiDelta;

    private JPanel chartPanel;

    private final NhanVienDAO nhanVienDAO = new NhanVienDAO();
    private final PhongBanDAO phongBanDAO = new PhongBanDAO();

    public DashboardPanel() {
        setupActions();
        loadDashboardData();
    }

    private void setupActions() {
        tảiBáoCáoButton.addActionListener(e -> exportReport());
    }

    private void loadDashboardData() {
        try {
            int tongNV = nhanVienDAO.countAll();
            lblTongSoNhanVienValue.setText(String.valueOf(tongNV));
            lblTongSoNhanVienDelta.setText("Toàn bộ nhân viên đang quản lý");

            int soPhongBan = phongBanDAO.findAll().size();
            lblSoPhongBanValue.setText(String.valueOf(soPhongBan));
            lblSoPhongBanDelta.setText("Danh mục phòng ban hiện có");

            int nvMoi = nhanVienDAO.countNewThisMonth();
            lblNhanVienMoiValue.setText(String.valueOf(nvMoi));
            lblNhanVienMoiDelta.setText("Vào làm trong tháng này");

            loadPhongBanStats();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Không tải được số liệu tổng quan: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPhongBanStats() throws SQLException {
        Map<String, Integer> soLuongTheoPhongBan = nhanVienDAO.countByPhongBan();

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        for (Map.Entry<String, Integer> entry : soLuongTheoPhongBan.entrySet()) {
            JLabel row = new JLabel(entry.getKey() + " — " + entry.getValue() + " nhân viên");
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            listPanel.add(row);
        }

        chartPanel.removeAll();
        chartPanel.add(listPanel, BorderLayout.NORTH);
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    private void exportReport() {
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
