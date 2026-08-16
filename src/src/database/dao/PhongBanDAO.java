package database.dao;

import database.DBConnection;
import model.PhongBan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PhongBanDAO implements DanhMucDAO {

    @Override
    public List<PhongBan> findAll() throws SQLException {
        String sql = "SELECT MaPB, TenPhongBan, MoTa FROM PHONGBAN ORDER BY TenPhongBan";
        List<PhongBan> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new PhongBan(rs.getInt("MaPB"), rs.getString("TenPhongBan"), rs.getString("MoTa")));
            }
        }
        return result;
    }

    @Override
    public int insert(String ten, String moTa) throws SQLException {
        String sql = "INSERT INTO PHONGBAN (TenPhongBan, MoTa) VALUES (?, ?)";
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
        throw new SQLException("Khong lay duoc MaPB vua tao.");
    }

    @Override
    public void update(int maPB, String ten, String moTa) throws SQLException {
        String sql = "UPDATE PHONGBAN SET TenPhongBan = ?, MoTa = ? WHERE MaPB = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ten);
            ps.setString(2, moTa);
            ps.setInt(3, maPB);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int maPB) throws SQLException {
        String sql = "DELETE FROM PHONGBAN WHERE MaPB = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maPB);
            ps.executeUpdate();
        }
    }

    @Override
    public int countUsage(int maPB) throws SQLException {
        String sql = "SELECT COUNT(*) FROM NHANVIEN WHERE MaPB = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maPB);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
