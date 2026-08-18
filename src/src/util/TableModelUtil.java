package util;

import model.NhanVien;

import javax.swing.table.DefaultTableModel;
import java.util.List;

public class TableModelUtil {

    private TableModelUtil() {
    }

    public static void fillEmployeeTable(DefaultTableModel model, List<NhanVien> list) {
        model.setRowCount(0);
        for (NhanVien nv : list) {
            model.addRow(new Object[]{
                    nv.getMaNV(),
                    nv.getHoTen(),
                    nv.getTenPhongBan() == null ? "" : nv.getTenPhongBan(),
                    nv.getTenChucVu() == null ? "" : nv.getTenChucVu(),
                    nv.getSoDienThoai() == null ? "" : nv.getSoDienThoai(),
                    null
            });
        }
    }
}
