package database.dao;

import database.DBConnection;
import model.ChucVu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ChucVuDAO implements DanhMucDAO {

    @Override
    public List<ChucVu> findAll() throws SQLException {
        String sql = "SELECT MaCV, TenChucVu, MoTa FROM CHUCVU ORDER BY TenChucVu";
        List<ChucVu> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new ChucVu(rs.getInt("MaCV"), rs.getString("TenChucVu"), rs.getString("MoTa")));
            }
        }
        return result;
    }

    @Override
    public int insert(String ten, String moTa) throws SQLException {
        String sql = "INSERT INTO CHUCVU (TenChucVu, MoTa) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ten);
            ps.setString(2, moTa);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Khong lay duoc MaCV vua tao.");
    }

    @Override
    public void update(int maCV, String ten, String moTa) throws SQLException {
        String sql = "UPDATE CHUCVU SET TenChucVu = ?, MoTa = ? WHERE MaCV = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ten);
            ps.setString(2, moTa);
            ps.setInt(3, maCV);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int maCV) throws SQLException {
        String sql = "DELETE FROM CHUCVU WHERE MaCV = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maCV);
            ps.executeUpdate();
        }
    }

    @Override
    public int countUsage(int maCV) throws SQLException {
        String sql = "SELECT COUNT(*) FROM NHANVIEN WHERE MaCV = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maCV);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
