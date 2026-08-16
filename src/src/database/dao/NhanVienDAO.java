package database.dao;

import database.DBConnection;
import model.NhanVien;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class NhanVienDAO {

    private static final String SELECT_BASE =
            "SELECT nv.MaNV, nv.HoTen, nv.NgaySinh, nv.GioiTinh, nv.SoCCCD, nv.SoDienThoai, nv.Email, " +
                    "nv.DiaChi, nv.AvatarPath, nv.MaPB, pb.TenPhongBan, nv.MaCV, cv.TenChucVu, " +
                    "nv.NgayVaoLam, nv.LoaiHopDong, nv.MucLuongCoBan, nv.TrangThai " +
                    "FROM NHANVIEN nv " +
                    "LEFT JOIN PHONGBAN pb ON nv.MaPB = pb.MaPB " +
                    "LEFT JOIN CHUCVU cv ON nv.MaCV = cv.MaCV ";

    public int insert(NhanVien nv) throws SQLException {
        String sql = "INSERT INTO NHANVIEN (HoTen, NgaySinh, GioiTinh, SoCCCD, SoDienThoai, Email, DiaChi, " +
                "AvatarPath, MaPB, MaCV, NgayVaoLam, LoaiHopDong, MucLuongCoBan, TrangThai) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEmployeeFields(ps, nv);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Khong lay duoc MaNV vua tao.");
    }

    public void update(NhanVien nv) throws SQLException {
        String sql = "UPDATE NHANVIEN SET HoTen=?, NgaySinh=?, GioiTinh=?, SoCCCD=?, SoDienThoai=?, Email=?, " +
                "DiaChi=?, AvatarPath=?, MaPB=?, MaCV=?, NgayVaoLam=?, LoaiHopDong=?, MucLuongCoBan=?, TrangThai=? " +
                "WHERE MaNV=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int lastIndex = bindEmployeeFields(ps, nv);
            ps.setInt(lastIndex, nv.getMaNV());
            ps.executeUpdate();
        }
    }

    public void delete(int maNV) throws SQLException {
        String sql = "DELETE FROM NHANVIEN WHERE MaNV=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maNV);
            ps.executeUpdate();
        }
    }

    public NhanVien findById(int maNV) throws SQLException {
        String sql = SELECT_BASE + "WHERE nv.MaNV = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maNV);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<NhanVien> findPage(int page, int pageSize) throws SQLException {
        String sql = SELECT_BASE + "ORDER BY nv.MaNV LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, Math.max(0, (page - 1) * pageSize));
            return mapAll(ps);
        }
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM NHANVIEN";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** So nhan vien co NgayVaoLam trong thang hien tai (dung cho the "Nhan vien moi" o Dashboard). */
    public int countNewThisMonth() throws SQLException {
        String sql = "SELECT COUNT(*) FROM NHANVIEN " +
                "WHERE YEAR(NgayVaoLam) = YEAR(CURDATE()) AND MONTH(NgayVaoLam) = MONTH(CURDATE())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** So luong nhan vien theo tung phong ban (ke ca phong ban 0 nguoi), dung cho thong ke don gian o Dashboard. */
    public java.util.Map<String, Integer> countByPhongBan() throws SQLException {
        String sql = "SELECT pb.TenPhongBan, COUNT(nv.MaNV) AS SoLuong " +
                "FROM PHONGBAN pb LEFT JOIN NHANVIEN nv ON nv.MaPB = pb.MaPB " +
                "GROUP BY pb.MaPB, pb.TenPhongBan ORDER BY pb.TenPhongBan";
        java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("TenPhongBan"), rs.getInt("SoLuong"));
            }
        }
        return result;
    }

    public List<NhanVien> search(String keyword, int page, int pageSize) throws SQLException {
        String sql = SELECT_BASE +
                "WHERE nv.HoTen LIKE ? OR nv.SoDienThoai LIKE ? OR nv.Email LIKE ? OR CAST(nv.MaNV AS CHAR) LIKE ? " +
                "ORDER BY nv.MaNV LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setInt(5, pageSize);
            ps.setInt(6, Math.max(0, (page - 1) * pageSize));
            return mapAll(ps);
        }
    }

    public int countSearch(String keyword) throws SQLException {
        String sql = "SELECT COUNT(*) FROM NHANVIEN nv " +
                "WHERE nv.HoTen LIKE ? OR nv.SoDienThoai LIKE ? OR nv.Email LIKE ? OR CAST(nv.MaNV AS CHAR) LIKE ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<NhanVien> advancedSearch(Integer maPB, Integer maCV, String trangThai, String gioiTinh,
                                          String keyword, int page, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE);
        List<Object> params = new ArrayList<>();
        appendAdvancedFilters(sql, params, maPB, maCV, trangThai, gioiTinh, keyword);
        sql.append("ORDER BY nv.MaNV LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(Math.max(0, (page - 1) * pageSize));

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            return mapAll(ps);
        }
    }

    public int countAdvancedSearch(Integer maPB, Integer maCV, String trangThai, String gioiTinh,
                                    String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM NHANVIEN nv ");
        List<Object> params = new ArrayList<>();
        appendAdvancedFilters(sql, params, maPB, maCV, trangThai, gioiTinh, keyword);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void appendAdvancedFilters(StringBuilder sql, List<Object> params, Integer maPB, Integer maCV,
                                        String trangThai, String gioiTinh, String keyword) {
        List<String> conditions = new ArrayList<>();
        if (maPB != null) {
            conditions.add("nv.MaPB = ?");
            params.add(maPB);
        }
        if (maCV != null) {
            conditions.add("nv.MaCV = ?");
            params.add(maCV);
        }
        if (trangThai != null) {
            conditions.add("nv.TrangThai = ?");
            params.add(trangThai);
        }
        if (gioiTinh != null) {
            conditions.add("nv.GioiTinh = ?");
            params.add(gioiTinh);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            conditions.add("(nv.HoTen LIKE ? OR nv.SoDienThoai LIKE ? OR nv.Email LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private List<NhanVien> mapAll(PreparedStatement ps) throws SQLException {
        List<NhanVien> result = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    /** Gan cac field cua nv vao PreparedStatement theo dung thu tu cot trong INSERT/UPDATE, tra ve index tiep theo. */
    private int bindEmployeeFields(PreparedStatement ps, NhanVien nv) throws SQLException {
        int i = 1;
        ps.setString(i++, nv.getHoTen());
        ps.setDate(i++, util.DateUtil.toSqlDate(nv.getNgaySinh()));
        ps.setString(i++, nv.getGioiTinh());
        ps.setString(i++, nv.getSoCCCD());
        ps.setString(i++, nv.getSoDienThoai());
        ps.setString(i++, nv.getEmail());
        ps.setString(i++, nv.getDiaChi());
        ps.setString(i++, nv.getAvatarPath());
        setNullableInt(ps, i++, nv.getMaPB());
        setNullableInt(ps, i++, nv.getMaCV());
        ps.setDate(i++, util.DateUtil.toSqlDate(nv.getNgayVaoLam()));
        ps.setString(i++, nv.getLoaiHopDong());
        ps.setBigDecimal(i++, nv.getMucLuongCoBan());
        ps.setString(i++, nv.getTrangThai());
        return i;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private NhanVien mapRow(ResultSet rs) throws SQLException {
        NhanVien nv = new NhanVien();
        nv.setMaNV(rs.getInt("MaNV"));
        nv.setHoTen(rs.getString("HoTen"));
        nv.setNgaySinh(util.DateUtil.fromSqlDate(rs.getDate("NgaySinh")));
        nv.setGioiTinh(rs.getString("GioiTinh"));
        nv.setSoCCCD(rs.getString("SoCCCD"));
        nv.setSoDienThoai(rs.getString("SoDienThoai"));
        nv.setEmail(rs.getString("Email"));
        nv.setDiaChi(rs.getString("DiaChi"));
        nv.setAvatarPath(rs.getString("AvatarPath"));
        int maPB = rs.getInt("MaPB");
        nv.setMaPB(rs.wasNull() ? null : maPB);
        nv.setTenPhongBan(rs.getString("TenPhongBan"));
        int maCV = rs.getInt("MaCV");
        nv.setMaCV(rs.wasNull() ? null : maCV);
        nv.setTenChucVu(rs.getString("TenChucVu"));
        nv.setNgayVaoLam(util.DateUtil.fromSqlDate(rs.getDate("NgayVaoLam")));
        nv.setLoaiHopDong(rs.getString("LoaiHopDong"));
        nv.setMucLuongCoBan(rs.getBigDecimal("MucLuongCoBan"));
        nv.setTrangThai(rs.getString("TrangThai"));
        return nv;
    }
}
