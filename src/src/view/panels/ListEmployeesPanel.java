package view.panels;

import database.dao.ChucVuDAO;
import database.dao.NhanVienDAO;
import database.dao.PhongBanDAO;
import model.ChucVu;
import model.NhanVien;
import model.PhongBan;
import util.ActivityLogger;
import util.TableModelUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ListEmployeesPanel extends JPanel {

    private JPanel rootPanel;

    private JLabel lblPageTitle;
    private JLabel lblSubtitle;
    private JButton thêmNhânViênButton;
    private JTextField tìmKiếmNhanhTextField;
    private JButton tìmKiếmButton;
    private JButton lọcButton;
    private JButton xuấtButton;

    private JPanel filterPanel;
    private JComboBox phòngBanComboBox;
    private JComboBox chứcVụComboBox;
    private JComboBox trạngTháiComboBox;
    private JRadioButton tấtCảRadioButton;
    private JRadioButton namRadioButton;
    private JRadioButton nữRadioButton;
    private JRadioButton khácRadioButton;
    private JButton ápDụngLọcButton;
    private JButton xóaLọcButton;

    private JTable employeeTable;

    private JLabel lblResultCount;
    private JButton trangTrướcButton;
    private JButton trangSauButton;

    private DefaultTableModel tableModel;

    private static final String TITLE_BROWSE = "Danh sách nhân viên";
    private static final String TITLE_SEARCH = "Kết quả tìm kiếm";
    private static final int PAGE_SIZE = 10;
    private static final int ACTION_COLUMN = 5;

    private static final int MODE_BROWSE = 0;
    private static final int MODE_KEYWORD_SEARCH = 1;
    private static final int MODE_ADVANCED_FILTER = 2;

    private final NhanVienDAO nhanVienDAO = new NhanVienDAO();
    private final PhongBanDAO phongBanDAO = new PhongBanDAO();
    private final ChucVuDAO chucVuDAO = new ChucVuDAO();

    private int viewMode = MODE_BROWSE;
    private String currentKeyword = "";
    private int currentPage = 1;
    private int totalCount = 0;
    private List<NhanVien> currentRows = Collections.emptyList();

    private Runnable onAddEmployeeRequested;
    private Consumer<NhanVien> onEditEmployeeRequested;

    public ListEmployeesPanel() {
        setupTable();
        setupActions();
        exitSearchMode();
    }

    private void setupTable() {
        String[] columns = {"Ma NV", "Ho va ten", "Phong ban", "Chuc vu", "Lien he", "Thao tac"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == ACTION_COLUMN;
            }
        };
        employeeTable.setModel(tableModel);
        employeeTable.getColumnModel().getColumn(ACTION_COLUMN).setCellRenderer(new ActionCellRenderer());
        employeeTable.getColumnModel().getColumn(ACTION_COLUMN).setCellEditor(new ActionCellEditor());
        employeeTable.getColumnModel().getColumn(ACTION_COLUMN).setPreferredWidth(140);
        employeeTable.getColumnModel().getColumn(ACTION_COLUMN).setMaxWidth(140);
        employeeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = employeeTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < currentRows.size()) {
                        handleEdit(currentRows.get(row));
                    }
                }
            }
        });
    }

    private void setupActions() {
        thêmNhânViênButton.addActionListener(e -> {
            if (onAddEmployeeRequested != null) {
                onAddEmployeeRequested.run();
            }
        });

        tìmKiếmNhanhTextField.addActionListener(e -> performQuickSearch());
        tìmKiếmButton.addActionListener(e -> performQuickSearch());

        lọcButton.addActionListener(e -> toggleFilterPanel());
        xuấtButton.addActionListener(e -> exportResults());

        ápDụngLọcButton.addActionListener(e -> applyAdvancedFilters());
        xóaLọcButton.addActionListener(e -> clearFilters());

        trangTrướcButton.addActionListener(e -> goToPreviousPage());
        trangSauButton.addActionListener(e -> goToNextPage());

        setupGenderFilterRadios();
    }

    private void setupGenderFilterRadios() {
        tấtCảRadioButton.addActionListener(e -> selectSingleGenderFilterRadio(tấtCảRadioButton));
        namRadioButton.addActionListener(e -> selectSingleGenderFilterRadio(namRadioButton));
        nữRadioButton.addActionListener(e -> selectSingleGenderFilterRadio(nữRadioButton));
        khácRadioButton.addActionListener(e -> selectSingleGenderFilterRadio(khácRadioButton));
    }

    private void selectSingleGenderFilterRadio(JRadioButton chosen) {
        tấtCảRadioButton.setSelected(chosen == tấtCảRadioButton);
        namRadioButton.setSelected(chosen == namRadioButton);
        nữRadioButton.setSelected(chosen == nữRadioButton);
        khácRadioButton.setSelected(chosen == khácRadioButton);
    }

    private void populateFilterCombos() {
        try {
            phòngBanComboBox.removeAllItems();
            phòngBanComboBox.addItem("Tất cả phòng ban");
            for (PhongBan pb : phongBanDAO.findAll()) {
                phòngBanComboBox.addItem(pb);
            }

            chứcVụComboBox.removeAllItems();
            chứcVụComboBox.addItem("Tất cả chức vụ");
            for (ChucVu cv : chucVuDAO.findAll()) {
                chứcVụComboBox.addItem(cv);
            }
        } catch (SQLException e) {
            showDbError(e);
        }
    }

    private void toggleFilterPanel() {
        boolean showing = !filterPanel.isVisible();
        filterPanel.setVisible(showing);
        lọcButton.setText(showing ? "Ẩn lọc" : "Lọc");
        rootPanel.revalidate();
        rootPanel.repaint();
    }

    private void searchByKeyword(String keyword) {
        this.currentKeyword = keyword;
        this.viewMode = MODE_KEYWORD_SEARCH;
        this.currentPage = 1;
        applySearchModeUI();
        refreshCurrentView();
    }

    public void exitSearchMode() {
        this.currentKeyword = "";
        this.viewMode = MODE_BROWSE;
        this.currentPage = 1;
        tìmKiếmNhanhTextField.setText("");
        applyBrowseModeUI();
        populateFilterCombos();
        refreshCurrentView();
    }

    private void performQuickSearch() {
        String keyword = tìmKiếmNhanhTextField.getText().trim();
        if (keyword.isEmpty()) {
            exitSearchMode();
        } else {
            searchByKeyword(keyword);
        }
    }

    public void reloadData() {
        exitSearchMode();
    }

    private void applyBrowseModeUI() {
        lblPageTitle.setText(TITLE_BROWSE);
        lblSubtitle.setVisible(true);
        thêmNhânViênButton.setVisible(true);
    }

    private void applySearchModeUI() {
        lblPageTitle.setText(TITLE_SEARCH);
        lblSubtitle.setVisible(false);
        thêmNhânViênButton.setVisible(false);
    }

    private void refreshCurrentView() {
        switch (viewMode) {
            case MODE_KEYWORD_SEARCH:
                loadSearchResults();
                break;
            case MODE_ADVANCED_FILTER:
                loadFilterResults();
                break;
            default:
                loadBrowsePage();
                break;
        }
    }

    private void loadBrowsePage() {
        try {
            currentRows = nhanVienDAO.findPage(currentPage, PAGE_SIZE);
            totalCount = nhanVienDAO.countAll();
            TableModelUtil.fillEmployeeTable(tableModel, currentRows);
            updateResultCountLabel("nhân viên");
        } catch (SQLException e) {
            showDbError(e);
        }
    }

    private void loadSearchResults() {
        try {
            currentRows = nhanVienDAO.search(currentKeyword, currentPage, PAGE_SIZE);
            totalCount = nhanVienDAO.countSearch(currentKeyword);
            TableModelUtil.fillEmployeeTable(tableModel, currentRows);
            updateResultCountLabel("kết quả");
        } catch (SQLException e) {
            showDbError(e);
        }
    }

    private void applyAdvancedFilters() {
        this.viewMode = MODE_ADVANCED_FILTER;
        this.currentPage = 1;
        applySearchModeUI();
        refreshCurrentView();
    }

    private void clearFilters() {
        if (phòngBanComboBox.getItemCount() > 0) {
            phòngBanComboBox.setSelectedIndex(0);
        }
        if (chứcVụComboBox.getItemCount() > 0) {
            chứcVụComboBox.setSelectedIndex(0);
        }
        if (trạngTháiComboBox.getItemCount() > 0) {
            trạngTháiComboBox.setSelectedIndex(0);
        }
        selectSingleGenderFilterRadio(tấtCảRadioButton);
        exitSearchMode();
    }

    private void loadFilterResults() {
        Object pbSel = phòngBanComboBox.getSelectedItem();
        String maPB = pbSel instanceof PhongBan ? ((PhongBan) pbSel).getMaPB() : null;

        Object cvSel = chứcVụComboBox.getSelectedItem();
        String maCV = cvSel instanceof ChucVu ? ((ChucVu) cvSel).getMaCV() : null;

        String trangThai = trangThaiDisplayToCode((String) trạngTháiComboBox.getSelectedItem());
        String gioiTinh = namRadioButton.isSelected() ? "Nam"
                : nữRadioButton.isSelected() ? "Nữ"
                : khácRadioButton.isSelected() ? "Khác"
                : null;

        try {
            currentRows = nhanVienDAO.advancedSearch(maPB, maCV, trangThai, gioiTinh, currentKeyword, currentPage, PAGE_SIZE);
            totalCount = nhanVienDAO.countAdvancedSearch(maPB, maCV, trangThai, gioiTinh, currentKeyword);
            TableModelUtil.fillEmployeeTable(tableModel, currentRows);
            updateResultCountLabel("kết quả");
        } catch (SQLException e) {
            showDbError(e);
        }
    }

    private String trangThaiDisplayToCode(String display) {
        if (display == null) {
            return null;
        }
        switch (display) {
            case "Đang làm việc":
                return "DangLamViec";
            case "Nghỉ việc":
                return "NghiViec";
            case "Tạm nghỉ":
                return "TamNghi";
            default:
                return null;
        }
    }

    private void updateResultCountLabel(String noun) {
        int from = totalCount == 0 ? 0 : (currentPage - 1) * PAGE_SIZE + 1;
        int to = Math.min(currentPage * PAGE_SIZE, totalCount);
        lblResultCount.setText("Hiển thị " + from + "-" + to + " của " + totalCount + " " + noun);
        trangTrướcButton.setEnabled(currentPage > 1);
        trangSauButton.setEnabled(currentPage < totalPages());
    }

    private int totalPages() {
        return totalCount == 0 ? 1 : (int) Math.ceil(totalCount / (double) PAGE_SIZE);
    }

    private void exportResults() {
    }

    private void goToPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            refreshCurrentView();
        }
    }

    private void goToNextPage() {
        if (currentPage < totalPages()) {
            currentPage++;
            refreshCurrentView();
        }
    }

    private void handleEdit(NhanVien nv) {
        if (onEditEmployeeRequested != null) {
            onEditEmployeeRequested.accept(nv);
        }
    }

    private void handleDelete(NhanVien nv) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa nhân viên \"" + nv.getHoTen() + "\"?",
                "Xóa nhân viên", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            nhanVienDAO.delete(nv.getMaNV());
            ActivityLogger.logNhanVienAction("XÓA", nv);
            refreshCurrentView();
        } catch (SQLException e) {
            showDbError(e);
        }
    }

    private void showDbError(SQLException e) {
        JOptionPane.showMessageDialog(this, "Lỗi truy vấn cơ sở dữ liệu: " + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public void setOnAddEmployeeRequested(Runnable callback) {
        this.onAddEmployeeRequested = callback;
    }

    public void setOnEditEmployeeRequested(Consumer<NhanVien> callback) {
        this.onEditEmployeeRequested = callback;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private static class ActionCellRenderer extends JPanel implements TableCellRenderer {
        ActionCellRenderer() {
            super(new FlowLayout(FlowLayout.CENTER, 4, 2));
            add(new JButton("Sửa"));
            add(new JButton("Xóa"));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            return this;
        }
    }

    private class ActionCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        private int editingRow = -1;

        ActionCellEditor() {
            JButton editButton = new JButton("Sửa");
            JButton deleteButton = new JButton("Xóa");
            editButton.addActionListener(e -> {
                int row = editingRow;
                fireEditingStopped();
                if (row >= 0 && row < currentRows.size()) {
                    handleEdit(currentRows.get(row));
                }
            });
            deleteButton.addActionListener(e -> {
                int row = editingRow;
                fireEditingStopped();
                if (row >= 0 && row < currentRows.size()) {
                    handleDelete(currentRows.get(row));
                }
            });
            panel.add(editButton);
            panel.add(deleteButton);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            editingRow = row;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }
}
