package util;

import model.NhanVien;

import javax.swing.table.DefaultTableModel;
import java.util.List;

public class TableModelUtil {

    private TableModelUtil() {
    }

    /** Do danh sach nhan vien vao bang theo dung 6 cot: Ma NV, Ho va ten, Phong ban, Chuc vu, Lien he, Trang thai. */
    public static void fillEmployeeTable(DefaultTableModel model, List<NhanVien> list) {
        model.setRowCount(0);
        for (NhanVien nv : list) {
            model.addRow(new Object[]{
                    nv.getMaNV(),
                    nv.getHoTen(),
                    nv.getTenPhongBan() == null ? "" : nv.getTenPhongBan(),
                    nv.getTenChucVu() == null ? "" : nv.getTenChucVu(),
                    nv.getSoDienThoai() == null ? "" : nv.getSoDienThoai(),
                    trangThaiToVietnamese(nv.getTrangThai())
            });
        }
    }

    public static String trangThaiToVietnamese(String trangThai) {
        if (trangThai == null) {
            return "";
        }
        switch (trangThai) {
            case "DangLamViec":
                return "Đang làm việc";
            case "NghiViec":
                return "Nghỉ việc";
            case "TamNghi":
                return "Tạm nghỉ";
            default:
                return trangThai;
        }
    }
}
