package view.panels;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Noi dung man hinh "Quan ly" (danh sach nhan vien).
 * Cac field PHAI trung ten voi binding trong ListEmployeesPanel.form.
 *
 * Man hinh nay co 2 trang thai hien thi tieu de/noi dung bang, chuyen doi
 * bang code (viec tim kiem nhanh duoc thuc hien tu o "Tim kiem nhanh" o
 * TOP BAR cua MainFrame):
 *
 *  - BROWSE (mac dinh): tieu de "Danh sach nhan vien" + phu de, chan trang
 *    hien "Hien thi X-Y cua Z nhan vien".
 *  - SEARCH RESULT (sau khi go tu khoa o "Tim kiem nhanh"): tieu de doi
 *    thanh "Ket qua tim kiem", an phu de + nut Them, chan trang hien
 *    "Hien thi X-Y cua ket qua".
 *
 * Nut "Loc" va "Xuat" o header LUON hien thi (ca 2 trang thai tren).
 * Man hinh "Tim kiem nang cao" truoc day da duoc GOP vao day: bam nut
 * "Loc" se an/hien bang loc (filterPanel) o BEN TRAI bang danh sach,
 * giong nhu ban thiet ke Figma anh 2.
 */
public class ListEmployeesPanel extends JPanel {

    // ===== Sinh tu ListEmployeesPanel.form =====
    private JPanel rootPanel;

    private JLabel lblPageTitle;
    private JLabel lblSubtitle;
    private JButton thêmNhânViênButton;
    private JButton lọcButton;
    private JButton xuấtButton;

    // ----- Bang loc nang cao (an/hien khi bam nut "Loc") -----
    private JPanel filterPanel;
    private JComboBox phòngBanComboBox;
    private JComboBox chứcVụComboBox;
    private JComboBox trạngTháiComboBox;
    private JRadioButton tấtCảRadioButton;
    private JRadioButton namRadioButton;
    private JRadioButton nữRadioButton;
    private JButton ápDụngLọcButton;

    private JTable employeeTable;

    private JLabel lblResultCount;
    private JButton trangTrướcButton;
    private JButton trangSauButton;

    private DefaultTableModel tableModel;

    private static final String TITLE_BROWSE = "Danh sách nhân viên";
    private static final String TITLE_SEARCH = "Kết quả tìm kiếm";

    private boolean inSearchMode = false;
    private String currentKeyword = "";

    /** Callback de MainFrame biet khi nao can chuyen sang man hinh "Them nhan vien". */
    private Runnable onAddEmployeeRequested;

    public ListEmployeesPanel() {
        setupTable();
        setupActions();
        exitSearchMode(); // trang thai mac dinh = browse
    }

    private void setupTable() {
        String[] columns = {"Ma NV", "Ho va ten", "Phong ban", "Chuc vu", "Lien he", "Trang thai"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        employeeTable.setModel(tableModel);
    }

    private void setupActions() {
        thêmNhânViênButton.addActionListener(e -> {
            if (onAddEmployeeRequested != null) {
                onAddEmployeeRequested.run();
            }
        });

        // Bam "Loc" chi de AN/HIEN bang loc ben trai, khong doi trang thai tim kiem
        lọcButton.addActionListener(e -> toggleFilterPanel());
        xuấtButton.addActionListener(e -> exportResults());

        ápDụngLọcButton.addActionListener(e -> applyAdvancedFilters());

        trangTrướcButton.addActionListener(e -> goToPreviousPage());
        trangSauButton.addActionListener(e -> goToNextPage());
    }

    /** Dong/mo bang loc nang cao ben trai bang danh sach. */
    private void toggleFilterPanel() {
        boolean showing = !filterPanel.isVisible();
        filterPanel.setVisible(showing);
        lọcButton.setText(showing ? "Ẩn lọc" : "Lọc");
        rootPanel.revalidate();
        rootPanel.repaint();
    }

    /**
     * Duoc MainFrame goi khi nguoi dung go va Enter o o "Tim kiem nhanh" tren top bar.
     * Chuyen man hinh sang trang thai "Ket qua tim kiem" (nhu anh 5).
     */
    public void searchByKeyword(String keyword) {
        this.currentKeyword = keyword;
        this.inSearchMode = true;
        applySearchModeUI();
        loadSearchResults(keyword);
    }

    /** Ve lai trang thai danh sach binh thuong (nhu anh 4) - bo tim kiem. */
    public void exitSearchMode() {
        this.currentKeyword = "";
        this.inSearchMode = false;
        applyBrowseModeUI();
        reloadData();
    }

    private void applyBrowseModeUI() {
        lblPageTitle.setText(TITLE_BROWSE);
        lblSubtitle.setVisible(true);
        thêmNhânViênButton.setVisible(true);
        // Nut "Loc" va "Xuat" luon hien thi, khong phu thuoc trang thai
    }

    private void applySearchModeUI() {
        lblPageTitle.setText(TITLE_SEARCH);
        lblSubtitle.setVisible(false);
        thêmNhânViênButton.setVisible(false);
        // Nut "Loc" va "Xuat" luon hien thi, khong phu thuoc trang thai
    }

    /** TODO: goi EmployeeDAO de lay danh sach nhan vien (co phan trang) va do vao bang. */
    public void reloadData() {
        tableModel.setRowCount(0);
        // vi du:
        // List<Employee> list = employeeDAO.findPage(currentPage, pageSize);
        // for (Employee emp : list) {
        //     tableModel.addRow(new Object[]{emp.getMaNV(), emp.getHoTen(), emp.getPhongBan(),
        //             emp.getChucVu(), emp.getLienHe(), emp.getTrangThai()});
        // }
        // int total = employeeDAO.countAll();
        lblResultCount.setText("Hiển thị 1-10 của 150 nhân viên"); // TODO: thay bang so lieu that
    }

    /** TODO: goi EmployeeDAO.search(keyword) va do ket qua vao bang. */
    private void loadSearchResults(String keyword) {
        tableModel.setRowCount(0);
        // vi du:
        // List<Employee> results = employeeDAO.search(keyword);
        // for (Employee emp : results) { tableModel.addRow(...); }
        // int total = results.size();
        lblResultCount.setText("Hiển thị 1-3 của kết quả"); // TODO: thay bang so lieu that
    }

    /**
     * TODO: doc cac dieu kien loc (phòngBanComboBox, chứcVụComboBox, trạngTháiComboBox,
     * gioi tinh tấtCả/nam/nữ) roi goi EmployeeDAO.advancedSearch(...) va do ket qua vao bang.
     * Duoc goi khi nguoi dung bam nut "Ap dung bo loc" trong filterPanel.
     */
    private void applyAdvancedFilters() {
        tableModel.setRowCount(0);
        // vi du:
        // String phongBan = (String) phòngBanComboBox.getSelectedItem();
        // String chucVu = (String) chứcVụComboBox.getSelectedItem();
        // String trangThai = (String) trạngTháiComboBox.getSelectedItem();
        // String gioiTinh = namRadioButton.isSelected() ? "Nam" : nữRadioButton.isSelected() ? "Nu" : null;
        // List<Employee> results = employeeDAO.advancedSearch(phongBan, chucVu, trangThai, gioiTinh, currentKeyword);
        // for (Employee emp : results) { tableModel.addRow(...); }

        this.inSearchMode = true;
        applySearchModeUI();
        lblResultCount.setText("Hiển thị 1-3 của kết quả"); // TODO: thay bang so lieu that
    }

    private void exportResults() {
        // TODO: xuat ket qua ra Excel/PDF
    }

    private void goToPreviousPage() {
        if (inSearchMode) {
            // TODO: giam trang cua ket qua tim kiem roi loadSearchResults(currentKeyword)
        } else {
            // TODO: giam trang danh sach roi reloadData()
        }
    }

    private void goToNextPage() {
        if (inSearchMode) {
            // TODO: tang trang cua ket qua tim kiem roi loadSearchResults(currentKeyword)
        } else {
            // TODO: tang trang danh sach roi reloadData()
        }
    }

    public void setOnAddEmployeeRequested(Runnable callback) {
        this.onAddEmployeeRequested = callback;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}