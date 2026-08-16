package database.dao;

import database.DBConnection;
import model.PhongBan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                result.add(new PhongBan(rs.getString("MaPB"), rs.getString("TenPhongBan"), rs.getString("MoTa")));
            }
        }
        return result;
    }

    @Override
    public void insert(String maPB, String ten, String moTa) throws SQLException {
        String sql = "INSERT INTO PHONGBAN (MaPB, TenPhongBan, MoTa) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maPB);
            ps.setString(2, ten);
            ps.setString(3, moTa);
            ps.executeUpdate();
        }
    }

    @Override
    public void update(String maPB, String ten, String moTa) throws SQLException {
        String sql = "UPDATE PHONGBAN SET TenPhongBan = ?, MoTa = ? WHERE MaPB = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ten);
            ps.setString(2, moTa);
            ps.setString(3, maPB);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String maPB) throws SQLException {
        String sql = "DELETE FROM PHONGBAN WHERE MaPB = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maPB);
            ps.executeUpdate();
        }
    }

    @Override
    public int countUsage(String maPB) throws SQLException {
        String sql = "SELECT COUNT(*) FROM NHANVIEN WHERE MaPB = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maPB);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
