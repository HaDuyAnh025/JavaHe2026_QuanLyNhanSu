package database.dao;

import database.DBConnection;
import model.NhanVien;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    /**
     * Them nhan vien moi. MaNV duoc TU SINH theo cu phap MaPhongBan + so thu tu
     * 3 chu so, dem RIENG cho tung phong ban (vd: IT001, IT002...; KD001...).
     * Neu chua chon phong ban thi dung tien to "NV". Tra ve MaNV vua sinh.
     */
    public String insert(NhanVien nv) throws SQLException {
        String maNV = generateNextMaNV(nv.getMaPB());
        nv.setMaNV(maNV);
        String sql = "INSERT INTO NHANVIEN (MaNV, HoTen, NgaySinh, GioiTinh, SoCCCD, SoDienThoai, Email, DiaChi, " +
                "AvatarPath, MaPB, MaCV, NgayVaoLam, LoaiHopDong, MucLuongCoBan, TrangThai) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maNV);
            bindEmployeeFields(ps, nv, 2);
            ps.executeUpdate();
        }
        return maNV;
    }

    /** Mo phong so thu tu tiep theo trong pham vi 1 phong ban (khong bao gio trung nhau du co xoa giua chung). */
    private String generateNextMaNV(String maPB) throws SQLException {
        String prefix = (maPB == null || maPB.trim().isEmpty()) ? "NV" : maPB.trim();
        String sql = "SELECT MaNV FROM NHANVIEN WHERE MaNV LIKE ?";
        int max = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String suffix = rs.getString(1).substring(prefix.length());
                    try {
                        max = Math.max(max, Integer.parseInt(suffix));
                    } catch (NumberFormatException ignored) {
                        // MaNV khac dinh dang mong doi (khong the xay ra voi du lieu do app tu sinh), bo qua
                    }
                }
            }
        }
        return prefix + String.format("%03d", max + 1);
    }

    public void update(NhanVien nv) throws SQLException {
        String sql = "UPDATE NHANVIEN SET HoTen=?, NgaySinh=?, GioiTinh=?, SoCCCD=?, SoDienThoai=?, Email=?, " +
                "DiaChi=?, AvatarPath=?, MaPB=?, MaCV=?, NgayVaoLam=?, LoaiHopDong=?, MucLuongCoBan=?, TrangThai=? " +
                "WHERE MaNV=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int lastIndex = bindEmployeeFields(ps, nv, 1);
            ps.setString(lastIndex, nv.getMaNV());
            ps.executeUpdate();
        }
    }

    public void delete(String maNV) throws SQLException {
        String sql = "DELETE FROM NHANVIEN WHERE MaNV=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maNV);
            ps.executeUpdate();
        }
    }

    public NhanVien findById(String maNV) throws SQLException {
        String sql = SELECT_BASE + "WHERE nv.MaNV = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maNV);
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
                "WHERE nv.HoTen LIKE ? OR nv.SoDienThoai LIKE ? OR nv.Email LIKE ? OR nv.MaNV LIKE ? " +
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
                "WHERE nv.HoTen LIKE ? OR nv.SoDienThoai LIKE ? OR nv.Email LIKE ? OR nv.MaNV LIKE ?";
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

    public List<NhanVien> advancedSearch(String maPB, String maCV, String trangThai, String gioiTinh,
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

    public int countAdvancedSearch(String maPB, String maCV, String trangThai, String gioiTinh,
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

    private void appendAdvancedFilters(StringBuilder sql, List<Object> params, String maPB, String maCV,
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

    /** Gan cac field cua nv vao PreparedStatement (KHONG gom MaNV) bat dau tu startIndex, tra ve index tiep theo. */
    private int bindEmployeeFields(PreparedStatement ps, NhanVien nv, int startIndex) throws SQLException {
        int i = startIndex;
        ps.setString(i++, nv.getHoTen());
        ps.setDate(i++, util.DateUtil.toSqlDate(nv.getNgaySinh()));
        ps.setString(i++, nv.getGioiTinh());
        ps.setString(i++, nv.getSoCCCD());
        ps.setString(i++, nv.getSoDienThoai());
        ps.setString(i++, nv.getEmail());
        ps.setString(i++, nv.getDiaChi());
        ps.setString(i++, nv.getAvatarPath());
        ps.setString(i++, nv.getMaPB());
        ps.setString(i++, nv.getMaCV());
        ps.setDate(i++, util.DateUtil.toSqlDate(nv.getNgayVaoLam()));
        ps.setString(i++, nv.getLoaiHopDong());
        ps.setBigDecimal(i++, nv.getMucLuongCoBan());
        ps.setString(i++, nv.getTrangThai());
        return i;
    }

    private NhanVien mapRow(ResultSet rs) throws SQLException {
        NhanVien nv = new NhanVien();
        nv.setMaNV(rs.getString("MaNV"));
        nv.setHoTen(rs.getString("HoTen"));
        nv.setNgaySinh(util.DateUtil.fromSqlDate(rs.getDate("NgaySinh")));
        nv.setGioiTinh(rs.getString("GioiTinh"));
        nv.setSoCCCD(rs.getString("SoCCCD"));
        nv.setSoDienThoai(rs.getString("SoDienThoai"));
        nv.setEmail(rs.getString("Email"));
        nv.setDiaChi(rs.getString("DiaChi"));
        nv.setAvatarPath(rs.getString("AvatarPath"));
        nv.setMaPB(rs.getString("MaPB"));
        nv.setTenPhongBan(rs.getString("TenPhongBan"));
        nv.setMaCV(rs.getString("MaCV"));
        nv.setTenChucVu(rs.getString("TenChucVu"));
        nv.setNgayVaoLam(util.DateUtil.fromSqlDate(rs.getDate("NgayVaoLam")));
        nv.setLoaiHopDong(rs.getString("LoaiHopDong"));
        nv.setMucLuongCoBan(rs.getBigDecimal("MucLuongCoBan"));
        nv.setTrangThai(rs.getString("TrangThai"));
        return nv;
    }
}
