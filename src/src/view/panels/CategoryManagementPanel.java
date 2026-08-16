package view.panels;

import database.dao.ChucVuDAO;
import database.dao.DanhMucDAO;
import database.dao.PhongBanDAO;
import model.DanhMuc;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * Man hinh "Danh muc" (thay cho "Cai dat" cu): quan ly Phong ban va Chuc vu.
 * PhongBan/ChucVu cung dang du lieu (id, ten, mo ta) nen dung chung 1 tab
 * CRUD (xem CategoryTab) thay vi viet trung 2 lan.
 */
public class CategoryManagementPanel extends JPanel {

    public CategoryManagementPanel() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Phòng ban", new CategoryTab(new PhongBanDAO(), "phòng ban"));
        tabs.addTab("Chức vụ", new CategoryTab(new ChucVuDAO(), "chức vụ"));
        add(tabs, BorderLayout.CENTER);
    }

    public JPanel getRootPanel() {
        return this;
    }

    /** 1 tab CRUD dung chung cho Phong ban / Chuc vu. */
    private static class CategoryTab extends JPanel {
        private final DanhMucDAO dao;
        private final String nounLower;

        private final DefaultTableModel tableModel;
        private final JTable table;
        private List<? extends DanhMuc> currentRows;

        CategoryTab(DanhMucDAO dao, String nounLower) {
            this.dao = dao;
            this.nounLower = nounLower;
            setLayout(new BorderLayout(0, 8));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            String[] columns = {"Mã", "Tên", "Mô tả"};
            tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };
            table = new JTable(tableModel);
            table.setRowHeight(26);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JButton addButton = new JButton("+ Thêm " + nounLower);
            addButton.addActionListener(e -> openEditDialog(null));
            JButton editButton = new JButton("Sửa");
            editButton.addActionListener(e -> {
                DanhMuc selected = getSelected();
                if (selected != null) {
                    openEditDialog(selected);
                }
            });
            JButton deleteButton = new JButton("Xóa");
            deleteButton.addActionListener(e -> deleteSelected());

            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            bar.add(addButton);
            bar.add(editButton);
            bar.add(deleteButton);
            add(bar, BorderLayout.SOUTH);

            reload();
        }

        private void reload() {
            try {
                currentRows = dao.findAll();
                tableModel.setRowCount(0);
                for (DanhMuc dm : currentRows) {
                    tableModel.addRow(new Object[]{dm.getId(), dm.getTen(), dm.getMoTa() == null ? "" : dm.getMoTa()});
                }
            } catch (SQLException e) {
                showError("Không tải được danh sách: " + e.getMessage());
            }
        }

        private DanhMuc getSelected() {
            int row = table.getSelectedRow();
            if (row < 0 || currentRows == null || row >= currentRows.size()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một " + nounLower + " trong bảng.");
                return null;
            }
            return currentRows.get(row);
        }

        private void openEditDialog(DanhMuc editing) {
            JTextField tenField = new JTextField(editing == null ? "" : editing.getTen());
            JTextField moTaField = new JTextField(editing == null || editing.getMoTa() == null ? "" : editing.getMoTa());

            JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
            form.add(new JLabel("Tên " + nounLower + ":"));
            form.add(tenField);
            form.add(new JLabel("Mô tả:"));
            form.add(moTaField);

            String title = editing == null ? "Thêm " + nounLower : "Sửa " + nounLower;
            int result = JOptionPane.showConfirmDialog(this, form, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String ten = tenField.getText().trim();
            String moTa = moTaField.getText().trim();
            if (ten.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên " + nounLower + ".");
                return;
            }

            try {
                if (editing == null) {
                    dao.insert(ten, moTa.isEmpty() ? null : moTa);
                } else {
                    dao.update(editing.getId(), ten, moTa.isEmpty() ? null : moTa);
                }
                reload();
            } catch (SQLIntegrityConstraintViolationException e) {
                JOptionPane.showMessageDialog(this, "Tên " + nounLower + " này đã tồn tại.",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException e) {
                showError("Không lưu được: " + e.getMessage());
            }
        }

        private void deleteSelected() {
            DanhMuc selected = getSelected();
            if (selected == null) {
                return;
            }
            try {
                int usage = dao.countUsage(selected.getId());
                String msg = usage > 0
                        ? "Có " + usage + " nhân viên đang thuộc " + nounLower + " \"" + selected.getTen()
                        + "\". Xóa sẽ bỏ trống " + nounLower + " của họ. Vẫn xóa?"
                        : "Xóa " + nounLower + " \"" + selected.getTen() + "\"?";
                int confirm = JOptionPane.showConfirmDialog(this, msg, "Xóa " + nounLower,
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                dao.delete(selected.getId());
                reload();
            } catch (SQLException e) {
                showError("Không xóa được: " + e.getMessage());
            }
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
