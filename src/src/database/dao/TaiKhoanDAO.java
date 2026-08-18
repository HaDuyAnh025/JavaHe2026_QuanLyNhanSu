package database.dao;

import database.DBConnection;
import model.TaiKhoan;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TaiKhoanDAO {

    private static final String SELECT_BASE =
            "SELECT tk.MaTK, tk.TenDangNhap, tk.MatKhauHash, tk.VaiTro, tk.MaNV, nv.HoTen, " +
                    "tk.TrangThai, tk.NgayTao, tk.LanDangNhapCuoi " +
                    "FROM TAIKHOAN tk LEFT JOIN NHANVIEN nv ON tk.MaNV = nv.MaNV ";

    public TaiKhoan checkLogin(String tenDangNhap, String matKhauGoc) throws SQLException {
        String sql = SELECT_BASE + "WHERE tk.TenDangNhap = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                TaiKhoan tk = mapRow(rs);
                if (!"HoatDong".equals(tk.getTrangThai())) {
                    return null;
                }
                if (!BCrypt.checkpw(matKhauGoc, tk.getMatKhauHash())) {
                    return null;
                }
                capNhatLanDangNhapCuoi(tk.getMaTK());
                return tk;
            }
        }
    }

    private void capNhatLanDangNhapCuoi(int maTK) throws SQLException {
        String sql = "UPDATE TAIKHOAN SET LanDangNhapCuoi = NOW() WHERE MaTK = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maTK);
            ps.executeUpdate();
        }
    }

    public List<TaiKhoan> findAll() throws SQLException {
        String sql = SELECT_BASE + "ORDER BY tk.MaTK";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<TaiKhoan> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        }
    }

    public boolean existsUsername(String tenDangNhap) throws SQLException {
        String sql = "SELECT 1 FROM TAIKHOAN WHERE TenDangNhap = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void create(String tenDangNhap, String matKhauGoc, String vaiTro) throws SQLException {
        String hash = BCrypt.hashpw(matKhauGoc, BCrypt.gensalt(12));
        String sql = "INSERT INTO TAIKHOAN (TenDangNhap, MatKhauHash, VaiTro, MaNV, TrangThai) VALUES (?,?,?,NULL,'HoatDong')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            ps.setString(2, hash);
            ps.setString(3, vaiTro);
            ps.executeUpdate();
        }
    }

    public void setTrangThai(int maTK, String trangThai) throws SQLException {
        String sql = "UPDATE TAIKHOAN SET TrangThai = ? WHERE MaTK = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trangThai);
            ps.setInt(2, maTK);
            ps.executeUpdate();
        }
    }

    public void setVaiTro(int maTK, String vaiTro) throws SQLException {
        String sql = "UPDATE TAIKHOAN SET VaiTro = ? WHERE MaTK = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vaiTro);
            ps.setInt(2, maTK);
            ps.executeUpdate();
        }
    }

    private TaiKhoan mapRow(ResultSet rs) throws SQLException {
        TaiKhoan tk = new TaiKhoan();
        tk.setMaTK(rs.getInt("MaTK"));
        tk.setTenDangNhap(rs.getString("TenDangNhap"));
        tk.setMatKhauHash(rs.getString("MatKhauHash"));
        tk.setVaiTro(rs.getString("VaiTro"));
        tk.setMaNV(rs.getString("MaNV"));
        tk.setHoTenNhanVien(rs.getString("HoTen"));
        tk.setTrangThai(rs.getString("TrangThai"));
        Timestamp ngayTao = rs.getTimestamp("NgayTao");
        tk.setNgayTao(ngayTao == null ? null : ngayTao.toLocalDateTime());
        Timestamp lanDangNhapCuoi = rs.getTimestamp("LanDangNhapCuoi");
        tk.setLanDangNhapCuoi(lanDangNhapCuoi == null ? null : lanDangNhapCuoi.toLocalDateTime());
        return tk;
    }
}
