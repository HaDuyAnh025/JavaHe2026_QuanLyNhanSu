package view.panels;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Noi dung man hinh "Tim kiem nang cao".
 * Cac field PHAI trung ten voi binding trong UpdateSearchEmpPanel.form.
 */
public class UpdateSearchEmpPanel extends JPanel {

    // ===== Sinh tu UpdateSearchEmpPanel.form =====
    private JPanel rootPanel;

    private JComboBox phòngBanComboBox;
    private JComboBox chứcVụComboBox;
    private JComboBox trạngTháiComboBox;
    private JRadioButton tấtCảRadioButton;
    private JRadioButton namRadioButton;
    private JRadioButton nữRadioButton;

    private JTextField tìmTheoTênMãTextField;
    private JButton tìmKiếmButton1;
    private JButton lọcButton;
    private JButton xuấtButton;
    private JTable resultTable;
    private JButton trangTrướcButton;
    private JButton trangSauButton;

    private DefaultTableModel tableModel;

    public UpdateSearchEmpPanel() {
        setupTable();
        setupActions();
    }

    private void setupTable() {
        String[] columns = {"Ma NV", "Ho va ten", "Phong ban", "Chuc vu", "Lien he", "Trang thai"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable.setModel(tableModel);
    }

    private void setupActions() {
        lọcButton.addActionListener(e -> applyAdvancedFilters());
        tìmKiếmButton1.addActionListener(e -> applyAdvancedFilters());
        xuấtButton.addActionListener(e -> exportResults());
        trangTrướcButton.addActionListener(e -> goToPreviousPage());
        trangSauButton.addActionListener(e -> goToNextPage());
    }

    /**
     * TODO: doc cac dieu kien loc (phòngBanComboBox, chứcVụComboBox, trạngTháiComboBox,
     * gioi tinh tấtCả/nam/nữ, tìmTheoTênMãTextField) roi goi EmployeeDAO.advancedSearch(...)
     * va do ket qua vao tableModel.
     */
    private void applyAdvancedFilters() {
        tableModel.setRowCount(0);
    }

    private void exportResults() {
        // TODO: xuat ket qua ra Excel/PDF
    }

    private void goToPreviousPage() {
        // TODO
    }

    private void goToNextPage() {
        // TODO
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
