package view.panels;

import database.dao.ChucVuDAO;
import database.dao.DanhMucDAO;
import database.dao.PhongBanDAO;
import model.DanhMuc;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.regex.Pattern;

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

    private static class CategoryTab extends JPanel {
        private static final Pattern MA_PATTERN = Pattern.compile("^[A-Z]{1,5}$");
        private static final int MAX_VISIBLE_ROWS = 8;

        private final DanhMucDAO dao;
        private final String nounLower;

        private final DefaultTableModel tableModel;
        private final JTable table;
        private final JScrollPane tableScroll;
        private List<? extends DanhMuc> currentRows;

        CategoryTab(DanhMucDAO dao, String nounLower) {
            this.dao = dao;
            this.nounLower = nounLower;
            setLayout(new BorderLayout(0, 8));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JButton addButton = new JButton("+ Thêm " + nounLower);
            addButton.addActionListener(e -> openEditDialog(null));
            JPanel headerBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            headerBar.add(addButton);
            add(headerBar, BorderLayout.NORTH);

            String[] columns = {"Mã", "Tên", "Mô tả", "Thao tác"};
            tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return col == 3;
                }
            };
            table = new JTable(tableModel);
            table.setRowHeight(32);
            table.getColumnModel().getColumn(3).setCellRenderer(new ActionCellRenderer());
            table.getColumnModel().getColumn(3).setCellEditor(new ActionCellEditor());
            table.getColumnModel().getColumn(3).setPreferredWidth(140);
            table.getColumnModel().getColumn(3).setMaxWidth(140);

            tableScroll = new JScrollPane(table);
            JPanel centerHolder = new JPanel(new BorderLayout());
            centerHolder.add(tableScroll, BorderLayout.NORTH);
            add(centerHolder, BorderLayout.CENTER);

            reload();
        }

        private void reload() {
            try {
                currentRows = dao.findAll();
                tableModel.setRowCount(0);
                for (DanhMuc dm : currentRows) {
                    tableModel.addRow(new Object[]{dm.getId(), dm.getTen(), dm.getMoTa() == null ? "" : dm.getMoTa(), null});
                }
                updateTableHeight();
            } catch (SQLException e) {
                showError("Không tải được danh sách: " + e.getMessage());
            }
        }

        private void updateTableHeight() {
            int rowCount = Math.max(tableModel.getRowCount(), 1);
            int visibleRows = Math.min(rowCount, MAX_VISIBLE_ROWS);
            int headerHeight = table.getTableHeader().getPreferredSize().height;
            int height = headerHeight + visibleRows * table.getRowHeight() + 2;
            tableScroll.setPreferredSize(new Dimension(10, height));
            revalidate();
        }

        private void openEditDialog(DanhMuc editing) {
            JTextField maField = new JTextField(editing == null ? "" : editing.getId());
            JTextField tenField = new JTextField(editing == null ? "" : editing.getTen());
            JTextField moTaField = new JTextField(editing == null || editing.getMoTa() == null ? "" : editing.getMoTa());

            maField.setEditable(editing == null);

            JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
            form.add(new JLabel("Mã " + nounLower + " (tối đa 5 chữ cái in hoa):"));
            form.add(maField);
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
                    String ma = maField.getText().trim().toUpperCase();
                    if (!MA_PATTERN.matcher(ma).matches()) {
                        JOptionPane.showMessageDialog(this, "Mã " + nounLower + " phải là chữ in hoa (A-Z), tối đa 5 ký tự.");
                        return;
                    }
                    dao.insert(ma, ten, moTa.isEmpty() ? null : moTa);
                } else {
                    dao.update(editing.getId(), ten, moTa.isEmpty() ? null : moTa);
                }
                reload();
            } catch (SQLIntegrityConstraintViolationException e) {
                String msg = editing == null
                        ? "Mã hoặc tên " + nounLower + " này đã tồn tại."
                        : "Tên " + nounLower + " này đã tồn tại.";
                JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException e) {
                showError("Không lưu được: " + e.getMessage());
            }
        }

        private void deleteEntry(DanhMuc selected) {
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
                    if (row >= 0 && currentRows != null && row < currentRows.size()) {
                        openEditDialog(currentRows.get(row));
                    }
                });
                deleteButton.addActionListener(e -> {
                    int row = editingRow;
                    fireEditingStopped();
                    if (row >= 0 && currentRows != null && row < currentRows.size()) {
                        deleteEntry(currentRows.get(row));
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
}
