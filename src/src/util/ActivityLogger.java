package util;

import model.NhanVien;
import model.Session;
import model.TaiKhoan;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ghi nhat ky (append) khi nhan vien bi SUA hoac XOA o man "Quan ly", de
 * biet ai (tai khoan nao) da lam gi, voi nhan vien nao, luc nao.
 * File duoc tao tai logs/nhanvien_activity.txt (canh thu muc chay app).
 */
public class ActivityLogger {

    private static final String LOG_FILE = "logs/nhanvien_activity.txt";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private ActivityLogger() {
    }

    public static void logNhanVienAction(String hanhDong, NhanVien nv) {
        TaiKhoan current = Session.getCurrentAccount();
        String nguoiThucHien = current == null ? "Không xác định"
                : current.getTenDangNhap() + " (" + current.getVaiTro() + ")";
        log("Tài khoản " + nguoiThucHien + " đã " + hanhDong + " nhân viên \""
                + nv.getHoTen() + "\" (Mã NV: " + nv.getMaNV() + ")");
    }

    private static void log(String message) {
        File file = new File(LOG_FILE);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        String line = "[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "] " + message;
        try (FileWriter fw = new FileWriter(file, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(line);
        } catch (IOException e) {
            // Ghi log la tinh nang phu, khong lam gian doan thao tac chinh
            // (xoa/sua nhan vien van thanh cong du khong ghi duoc log).
            System.err.println("Khong ghi duoc nhat ky: " + e.getMessage());
        }
    }
}
