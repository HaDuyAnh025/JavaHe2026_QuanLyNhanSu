# Giải thích chi tiết — Tầng DAO (`database/dao/`)

Tài liệu giải thích **toàn bộ 5 file** trong tầng DAO của dự án, chú thích **từng dòng/khối code**. Các dòng `//` in nghiêng sau code là chú thích **mình thêm vào để giải thích** — file gốc không có comment.

Phần đánh dấu 🟡 **NGOÀI CHƯƠNG TRÌNH** là kỹ thuật không nằm trong danh sách đã học (if/else, switch, do-while, for, array, đọc/ghi file, exception, OOP, MySQL JDBC, package/abstract class, PreparedStatement, insert/liệt kê dữ liệu JDBC, form Swing, validate form).

---

## Mục lục

0. [Tổng quan tầng DAO](#0-tổng-quan-tầng-dao)
1. [`DanhMucDAO.java` — interface dùng chung](#1-danhmucdaojava--interface-dùng-chung)
2. [`PhongBanDAO.java`](#2-phongbandaojava)
3. [`ChucVuDAO.java`](#3-chucvudaojava)
4. [`NhanVienDAO.java` — DAO phức tạp nhất](#4-nhanviendaojava--dao-phức-tạp-nhất)
5. [`TaiKhoanDAO.java`](#5-taikhoandaojava)
6. [Tổng kết: trong chương trình vs ngoài chương trình](#6-tổng-kết-trong-chương-trình-vs-ngoài-chương-trình)

---

## 0. Tổng quan tầng DAO

```
database/
├── DBConnection.java     ← mở Connection tới MySQL (dùng chung cho cả 5 file DAO)
└── dao/
    ├── DanhMucDAO.java   ← interface, không có logic, chỉ khai báo "hợp đồng" (mục 1)
    ├── PhongBanDAO.java  ← implements DanhMucDAO — CRUD bảng PHONGBAN (mục 2)
    ├── ChucVuDAO.java    ← implements DanhMucDAO — CRUD bảng CHUCVU (mục 3)
    ├── NhanVienDAO.java  ← CRUD + tìm kiếm + lọc + phân trang bảng NHANVIEN (mục 4)
    └── TaiKhoanDAO.java  ← đăng nhập (BCrypt) + CRUD bảng TAIKHOAN (mục 5)
```

**Quy tắc chung của mọi file DAO trong dự án** (áp dụng xuyên suốt, sẽ không lặp lại ở từng file):

- Mỗi phương thức **tự mở 1 `Connection` riêng** bằng `DBConnection.getConnection()` và tự đóng bằng `try-with-resources` — không có phương thức nào giữ `Connection` dùng lại cho nhiều thao tác.
- Mọi câu SQL đều dùng `PreparedStatement` với dấu `?` — **không bao giờ nối chuỗi trực tiếp giá trị người dùng nhập vào SQL** (chống SQL Injection).
- Mọi phương thức đều khai báo `throws SQLException` — DAO **không tự bắt lỗi**, đẩy trách nhiệm `try/catch` lên tầng View (nơi mới biết cách hiển thị lỗi phù hợp cho người dùng).
- DAO chỉ làm việc với `Connection`/`PreparedStatement`/`ResultSet` (JDBC) và trả về **object Model** (`NhanVien`, `TaiKhoan`...) — không bao giờ `import javax.swing.*`.

---

## 1. `DanhMucDAO.java` — interface dùng chung

File: [`database/dao/DanhMucDAO.java`](src/src/database/dao/DanhMucDAO.java)

```java
package database.dao;

import model.DanhMuc;

import java.sql.SQLException;
import java.util.List;

public interface DanhMucDAO {
    // 🟡 NGOÀI CHƯƠNG TRÌNH — interface KHÔNG chứa code thực thi, chỉ khai báo "chữ ký" (tên + tham số +
    // kiểu trả về) của các phương thức mà bất kỳ class nào "implements" nó BẮT BUỘC phải viết đầy đủ.
    // Coi như 1 "bản hợp đồng": PhongBanDAO và ChucVuDAO đều CAM KẾT có đủ 5 hàm dưới đây.

    List<? extends DanhMuc> findAll() throws SQLException;
    // "List<? extends DanhMuc>": wildcard generic — nghĩa là hàm này có thể trả về List<PhongBan> HOẶC
    // List<ChucVu> (bất kỳ kiểu nào implements DanhMuc), miễn là nơi gọi chỉ ĐỌC danh sách (dùng for-each
    // lấy dm.getId()/getTen()...), KHÔNG được viết dao.findAll().add(...) vì Java không biết chắc bên
    // trong là List cụ thể kiểu gì để đảm bảo an toàn kiểu dữ liệu.

    void insert(String ma, String ten, String moTa) throws SQLException;
    void update(String id, String ten, String moTa) throws SQLException;
    void delete(String id) throws SQLException;
    int countUsage(String id) throws SQLException;
    // countUsage(id): đếm xem có bao nhiêu nhân viên đang gán mã Phòng ban/Chức vụ này — dùng để cảnh
    // báo trước khi xóa (xem CategoryManagementPanel.deleteEntry()).
}
```

**Vì sao cần interface này?** [`CategoryManagementPanel.java`](src/src/view/panels/CategoryManagementPanel.java) viết **1 class UI duy nhất** (`CategoryTab`) dùng chung cho cả 2 tab "Phòng ban" và "Chức vụ":

```java
tabs.addTab("Phòng ban", new CategoryTab(new PhongBanDAO(), "phòng ban"));
tabs.addTab("Chức vụ",  new CategoryTab(new ChucVuDAO(),  "chức vụ"));
```
`CategoryTab` chỉ biết làm việc với kiểu `DanhMucDAO` (không quan tâm bên trong là `PhongBanDAO` hay `ChucVuDAO`) — đây là **đa hình (polymorphism)**, tránh phải viết 2 class UI gần như giống hệt nhau.

---

## 2. `PhongBanDAO.java`

File: [`database/dao/PhongBanDAO.java`](src/src/database/dao/PhongBanDAO.java)

```java
public class PhongBanDAO implements DanhMucDAO {
    // Cam kết cài đủ 5 phương thức của DanhMucDAO (mục 1) — thiếu 1 hàm sẽ không biên dịch được.

    @Override
    public List<PhongBan> findAll() throws SQLException {
        // Kiểu trả về cụ thể ở đây là List<PhongBan> (không phải List<? extends DanhMuc>) — Java CHO
        // PHÉP override với kiểu trả về CỤ THỂ HƠN kiểu khai báo trong interface (covariant return type),
        // vì PhongBan implements DanhMuc nên List<PhongBan> vẫn "là một" List<? extends DanhMuc>.
        String sql = "SELECT MaPB, TenPhongBan, MoTa FROM PHONGBAN ORDER BY TenPhongBan";
        List<PhongBan> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new PhongBan(rs.getString("MaPB"), rs.getString("TenPhongBan"), rs.getString("MoTa")));
                // Gọi thẳng constructor 3 tham số của PhongBan (model) — khác NhanVienDAO có hàm
                // mapRow() riêng vì NhanVien có QUÁ NHIỀU field, còn PhongBan chỉ có 3 field nên
                // không cần tách riêng 1 hàm map cho gọn.
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
        // Giống ChucVuDAO.update(): phần SET bind TRƯỚC (vị trí 1, 2), điều kiện WHERE bind SAU CÙNG
        // (vị trí 3) — dù tham số maPB là tham số ĐẦU TIÊN của hàm Java, thứ tự bind PHẢI theo thứ tự
        // dấu "?" xuất hiện trong câu SQL, không theo thứ tự tham số hàm.
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
            // Nhờ ràng buộc "ON DELETE SET NULL" của cột NHANVIEN.MaPB (khai báo trong Data.Basesql.sql),
            // xóa 1 phòng ban đang có nhân viên KHÔNG bị lỗi khóa ngoại — MySQL tự gán NULL cho các
            // nhân viên liên quan.
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
                // COUNT(*) LUÔN trả về đúng 1 dòng (kể cả kết quả là 0), rs.getInt(1) lấy cột đầu tiên
                // theo SỐ THỨ TỰ (không có tên cột rõ ràng để lấy theo tên như "SELECT COUNT(*) AS SoLuong").
            }
        }
    }
}
```

`PhongBanDAO` và `ChucVuDAO` (mục 3) là **2 class song sinh** — cấu trúc giống hệt nhau, chỉ khác tên bảng (`PHONGBAN`/`CHUCVU`) và tên cột (`TenPhongBan`/`TenChucVu`). Đây chính là lý do nên có interface `DanhMucDAO` để tầng View không phải viết trùng lặp logic xử lý cho 2 class gần như giống hệt này.

---

## 3. `ChucVuDAO.java`

File: [`database/dao/ChucVuDAO.java`](src/src/database/dao/ChucVuDAO.java)

```java
public class ChucVuDAO implements DanhMucDAO {

    @Override
    public List<ChucVu> findAll() throws SQLException {
        String sql = "SELECT MaCV, TenChucVu, MoTa FROM CHUCVU ORDER BY TenChucVu";
        List<ChucVu> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new ChucVu(rs.getString("MaCV"), rs.getString("TenChucVu"), rs.getString("MoTa")));
            }
        }
        return result;
    }

    @Override
    public void insert(String maCV, String ten, String moTa) throws SQLException {
        String sql = "INSERT INTO CHUCVU (MaCV, TenChucVu, MoTa) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maCV);
            ps.setString(2, ten);
            ps.setString(3, moTa);
            // moTa có thể là null (người dùng không nhập Mô tả) -> setString(3, null) hợp lệ,
            // JDBC tự hiểu là ghi NULL vào cột MoTa.
            ps.executeUpdate();
        }
    }

    @Override
    public void update(String maCV, String ten, String moTa) throws SQLException {
        String sql = "UPDATE CHUCVU SET TenChucVu = ?, MoTa = ? WHERE MaCV = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ten);      // dấu ? thứ 1 -> TenChucVu = ?
            ps.setString(2, moTa);     // dấu ? thứ 2 -> MoTa = ?
            ps.setString(3, maCV);     // dấu ? thứ 3 -> WHERE MaCV = ?
            ps.executeUpdate();
            // MaCV KHÔNG nằm trong phần SET — không cho phép đổi mã chức vụ, chỉ dùng để xác định
            // ĐÚNG DÒNG nào cần sửa. Khớp với View: ô "Mã" bị khóa khi đang ở chế độ Sửa.
        }
    }

    @Override
    public void delete(String maCV) throws SQLException {
        String sql = "DELETE FROM CHUCVU WHERE MaCV = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maCV);
            ps.executeUpdate();
        }
    }

    @Override
    public int countUsage(String maCV) throws SQLException {
        String sql = "SELECT COUNT(*) FROM NHANVIEN WHERE MaCV = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maCV);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
```

---

## 4. `NhanVienDAO.java` — DAO phức tạp nhất

File: [`database/dao/NhanVienDAO.java`](src/src/database/dao/NhanVienDAO.java) — DAO lớn nhất dự án (18 phương thức) vì bảng `NHANVIEN` là bảng trung tâm, cần liệt kê/tìm kiếm/lọc/phân trang/thống kê.

### 4.1 Hằng số SQL dùng chung

```java
private static final String SELECT_BASE =
        "SELECT nv.MaNV, nv.HoTen, nv.NgaySinh, nv.GioiTinh, nv.SoCCCD, nv.SoDienThoai, nv.Email, " +
                "nv.DiaChi, nv.AvatarPath, nv.MaPB, pb.TenPhongBan, nv.MaCV, cv.TenChucVu, " +
                "nv.NgayVaoLam, nv.LoaiHopDong, nv.MucLuongCoBan, nv.TrangThai " +
                "FROM NHANVIEN nv " +
                "LEFT JOIN PHONGBAN pb ON nv.MaPB = pb.MaPB " +
                "LEFT JOIN CHUCVU cv ON nv.MaCV = cv.MaCV ";
// Khai báo "static final" — hằng số dùng CHUNG cho 5 phương thức đọc dữ liệu bên dưới (findById,
// findPage, search, advancedSearch...), tránh gõ lại toàn bộ câu SELECT dài này nhiều lần.
// "nv", "pb", "cv" là ALIAS (bí danh) đặt cho 3 bảng NHANVIEN/PHONGBAN/CHUCVU, giúp câu SQL ngắn gọn
// và tránh nhập nhằng khi 2 bảng cùng có cột trùng tên.
// LEFT JOIN (thay vì JOIN thường): vẫn trả về dòng nhân viên dù MaPB/MaCV là NULL (nhân viên chưa
// gán phòng ban/chức vụ) — nếu dùng JOIN thường, nhân viên đó sẽ BỊ LOẠI khỏi kết quả.
```

### 4.2 `insert()` + `generateNextMaNV()` — thêm mới, tự sinh mã

```java
public String insert(NhanVien nv) throws SQLException {
    String maNV = generateNextMaNV(nv.getMaPB());
    // Sinh mã TRƯỚC khi insert, vì MaNV là khóa chính — phải có sẵn giá trị mới ghép được câu INSERT.
    nv.setMaNV(maNV);
    String sql = "INSERT INTO NHANVIEN (MaNV, HoTen, NgaySinh, GioiTinh, SoCCCD, SoDienThoai, Email, DiaChi, " +
            "AvatarPath, MaPB, MaCV, NgayVaoLam, LoaiHopDong, MucLuongCoBan, TrangThai) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    // 15 cột -> 15 dấu "?".
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, maNV);
        bindEmployeeFields(ps, nv, 2);
        // bindEmployeeFields() bind 14 field còn lại của "nv", bắt đầu từ vị trí "?" số 2 (vì vị trí 1
        // vừa set thủ công là maNV) — hàm này được TÁI SỬ DỤNG ở cả insert() lẫn update() (mục 4.3).
        ps.executeUpdate();
    }
    return maNV;
    // Trả mã NV vừa sinh về View để hiện "Đã thêm nhân viên mới (Mã NV: IT004)".
}
```

```java
private String generateNextMaNV(String maPB) throws SQLException {
    String prefix = (maPB == null || maPB.trim().isEmpty()) ? "NV" : maPB.trim();
    // Chưa chọn phòng ban -> tiền tố mặc định "NV"; có chọn -> dùng chính mã phòng ban làm tiền tố.
    String sql = "SELECT MaNV FROM NHANVIEN WHERE MaNV LIKE ?";
    int max = 0;
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, prefix + "%");
        // VD prefix="IT" -> tìm mọi mã bắt đầu bằng "IT" (IT001, IT002...). Dấu "%" ở đây là ký tự
        // đại diện CỦA CÚ PHÁP LIKE (không phải input người dùng), nên KHÔNG cần escape.
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String suffix = rs.getString(1).substring(prefix.length());
                // getString(1): lấy cột đầu tiên theo SỐ THỨ TỰ (chỉ SELECT 1 cột MaNV nên không cần
                // lấy theo tên). substring(prefix.length()): cắt bỏ tiền tố, giữ lại phần số phía sau
                // — VD "IT003".substring(2) = "003".
                try {
                    max = Math.max(max, Integer.parseInt(suffix));
                    // Tìm số LỚN NHẤT trong tất cả mã hiện có cùng tiền tố — Integer.parseInt("003") = 3
                    // (tự bỏ số 0 ở đầu).
                } catch (NumberFormatException ignored) {
                    // Nếu "suffix" lỡ không phải toàn số (dữ liệu bất thường) thì BỎ QUA dòng đó,
                    // không làm crash cả vòng lặp đang duyệt.
                }
            }
        }
    }
    return prefix + String.format("%03d", max + 1);
    // String.format("%03d", n): định dạng số nguyên đủ 3 CHỮ SỐ, tự thêm số 0 ở đầu nếu thiếu.
    // VD max=3 -> "%03d" của 4 -> "004" -> kết quả cuối "IT004".
    // Cách này ĐẢM BẢO không trùng mã dù xóa nhân viên ở giữa dãy, vì luôn lấy MAX hiện có + 1,
    // chứ không đếm SỐ LƯỢNG nhân viên rồi +1 (đếm số lượng sẽ bị trùng nếu đã xóa bớt ở giữa).
}
```

### 4.3 `update()` — sửa hồ sơ

```java
public void update(NhanVien nv) throws SQLException {
    String sql = "UPDATE NHANVIEN SET HoTen=?, NgaySinh=?, GioiTinh=?, SoCCCD=?, SoDienThoai=?, Email=?, " +
            "DiaChi=?, AvatarPath=?, MaPB=?, MaCV=?, NgayVaoLam=?, LoaiHopDong=?, MucLuongCoBan=?, TrangThai=? " +
            "WHERE MaNV=?";
    // 14 dấu "?" cho phần SET + 1 dấu "?" cho WHERE = 15 dấu "?" tổng cộng, ĐÚNG THỨ TỰ với 14 field
    // (giống hệt insert()) rồi tới MaNV ở CUỐI CÙNG.
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        int lastIndex = bindEmployeeFields(ps, nv, 1);
        // bindEmployeeFields() bind 14 field bắt đầu từ vị trí 1 (khác insert() bắt đầu từ vị trí 2,
        // vì update() KHÔNG có MaNV ở đầu câu SQL). Hàm trả về "lastIndex" — vị trí "?" TIẾP THEO
        // (tức 15) để dùng ngay bên dưới, tránh phải đếm tay 14 field rồi cộng 1 dễ nhầm.
        ps.setString(lastIndex, nv.getMaNV());
        // Set MaNV vào dấu "?" CUỐI CÙNG — ứng với điều kiện WHERE MaNV=?.
        ps.executeUpdate();
    }
}
```

### 4.4 `delete()`, `findById()` — xóa & tìm theo mã

```java
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
            // Toán tử 3 ngôi: có dòng khớp -> map thành object NhanVien và trả về; không có -> trả null.
            // (Trong dự án, findById() hiện KHÔNG được nơi nào gọi tới — code viết sẵn nhưng chưa dùng,
            // dành cho tính năng có thể mở rộng sau này, VD trang "Xem chi tiết nhân viên".)
        }
    }
}
```

### 4.5 `findPage()` — liệt kê có phân trang

```java
public List<NhanVien> findPage(int page, int pageSize) throws SQLException {
    String sql = SELECT_BASE + "ORDER BY nv.MaNV LIMIT ? OFFSET ?";
    // 🟡 NGOÀI CHƯƠNG TRÌNH — LIMIT ? OFFSET ?: cú pháp phân trang của MySQL, chỉ lấy đúng "pageSize"
    // dòng, bỏ qua "offset" dòng đầu — không tải hết dữ liệu về rồi cắt bằng code Java.
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, pageSize);
        ps.setInt(2, Math.max(0, (page - 1) * pageSize));
        // Trang 1 -> offset 0; trang 2 -> offset = pageSize (bỏ 10 dòng đầu); v.v.
        // Math.max(0, ...) phòng khi "page" truyền vào là số âm/0 (lỗi logic ở tầng gọi).
        return mapAll(ps);
    }
}
```

### 4.6 Các hàm đếm (`countAll`, `countNewThisMonth`, `countByPhongBan`)

```java
public int countAll() throws SQLException {
    String sql = "SELECT COUNT(*) FROM NHANVIEN";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
    }
    // Dùng ở Dashboard ("Tổng số nhân viên") VÀ ở ListEmployeesPanel (tính tổng số trang khi duyệt
    // danh sách thường) — 1 hàm, nhiều nơi tái sử dụng.
}

public int countNewThisMonth() throws SQLException {
    String sql = "SELECT COUNT(*) FROM NHANVIEN " +
            "WHERE YEAR(NgayVaoLam) = YEAR(CURDATE()) AND MONTH(NgayVaoLam) = MONTH(CURDATE())";
    // YEAR()/MONTH()/CURDATE(): hàm xử lý ngày tháng CỦA MYSQL (không phải Java) — CURDATE() lấy
    // ngày hiện tại theo đồng hồ SERVER database, YEAR()/MONTH() tách năm/tháng ra để so sánh.
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
    }
}

public java.util.Map<String, Integer> countByPhongBan() throws SQLException {
    // 🟡 NGOÀI CHƯƠNG TRÌNH — trả về Map<String,Integer> (tên phòng ban -> số lượng) thay vì List,
    // vì kết quả là dữ liệu DẠNG CẶP (key-value), không phải 1 dãy tuần tự đơn thuần.
    String sql = "SELECT pb.TenPhongBan, COUNT(nv.MaNV) AS SoLuong " +
            "FROM PHONGBAN pb LEFT JOIN NHANVIEN nv ON nv.MaPB = pb.MaPB " +
            "GROUP BY pb.MaPB, pb.TenPhongBan ORDER BY pb.TenPhongBan";
    // GROUP BY: gộp các dòng NHANVIEN có CÙNG MaPB lại thành 1 nhóm, COUNT(nv.MaNV) đếm số nhân viên
    // trong mỗi nhóm. LEFT JOIN từ PHONGBAN (không phải từ NHANVIEN) để phòng ban KHÔNG có nhân viên
    // nào vẫn xuất hiện trong kết quả với SoLuong = 0 (nếu JOIN ngược lại/JOIN thường sẽ bị mất dòng đó).
    java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
    // LinkedHashMap (thay vì HashMap thường): GIỮ ĐÚNG THỨ TỰ các dòng được thêm vào — khớp với
    // "ORDER BY pb.TenPhongBan" trong SQL, để Dashboard hiện danh sách phòng ban theo đúng thứ tự A→Z.
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            result.put(rs.getString("TenPhongBan"), rs.getInt("SoLuong"));
            // Map.put(key, value): thêm 1 cặp tên phòng ban - số lượng vào Map.
        }
    }
    return result;
}
```

### 4.7 `search()` / `countSearch()` — tìm kiếm nhanh

```java
public List<NhanVien> search(String keyword, int page, int pageSize) throws SQLException {
    String sql = SELECT_BASE +
            "WHERE nv.HoTen LIKE ? OR nv.SoDienThoai LIKE ? OR nv.Email LIKE ? OR nv.MaNV LIKE ? " +
            "ORDER BY nv.MaNV LIMIT ? OFFSET ?";
    // 4 điều kiện LIKE nối bằng OR: khớp 1 TRONG 4 cột (Họ tên/SĐT/Email/Mã NV) là đủ để trả về dòng đó.
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        String like = "%" + keyword + "%";
        // "%" là ký tự đại diện của LIKE — khớp GẦN ĐÚNG (từ khóa xuất hiện ở bất kỳ vị trí nào trong
        // chuỗi), khác so sánh "=" phải khớp TUYỆT ĐỐI toàn bộ chuỗi.
        ps.setString(1, like);
        ps.setString(2, like);
        ps.setString(3, like);
        ps.setString(4, like);
        // Bind CÙNG 1 giá trị "like" vào cả 4 dấu "?" vì cả 4 cột cùng tìm theo 1 từ khóa.
        ps.setInt(5, pageSize);
        ps.setInt(6, Math.max(0, (page - 1) * pageSize));
        return mapAll(ps);
    }
}

public int countSearch(String keyword) throws SQLException {
    String sql = "SELECT COUNT(*) FROM NHANVIEN nv " +
            "WHERE nv.HoTen LIKE ? OR nv.SoDienThoai LIKE ? OR nv.Email LIKE ? OR nv.MaNV LIKE ?";
    // Câu SQL ĐẾM riêng, giữ NGUYÊN điều kiện WHERE giống hệt search() nhưng KHÔNG có LIMIT/OFFSET
    // (đếm TỔNG số kết quả khớp, không phải chỉ đếm số dòng trong 1 trang) -> dùng để tính tổng số trang.
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
```

### 4.8 `advancedSearch()` / `countAdvancedSearch()` / `appendAdvancedFilters()` — lọc nâng cao, SQL động

```java
public List<NhanVien> advancedSearch(String maPB, String maCV, String trangThai, String gioiTinh,
                                      String keyword, int page, int pageSize) throws SQLException {
    StringBuilder sql = new StringBuilder(SELECT_BASE);
    // 🟡 NGOÀI CHƯƠNG TRÌNH — StringBuilder cho phép GHÉP SQL CÓ ĐIỀU KIỆN: chỉ thêm mệnh đề WHERE
    // ứng với NHỮNG BỘ LỌC người dùng THẬT SỰ chọn (thay vì 1 câu SQL cố định với số lượng "?" biết trước).
    List<Object> params = new ArrayList<>();
    // Danh sách tham số sẽ bind vào PreparedStatement, PHẢI đúng thứ tự với các dấu "?" sẽ xuất hiện
    // trong sql — được build SONG SONG với sql ngay bên trong appendAdvancedFilters().

    appendAdvancedFilters(sql, params, maPB, maCV, trangThai, gioiTinh, keyword);
    sql.append("ORDER BY nv.MaNV LIMIT ? OFFSET ?");
    params.add(pageSize);
    params.add(Math.max(0, (page - 1) * pageSize));
    // 2 tham số phân trang được add() SAU CÙNG, khớp đúng thứ tự với 2 dấu "?" cuối trong câu SQL.

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql.toString())) {
        // sql.toString(): PreparedStatement cần kiểu String, không nhận trực tiếp StringBuilder.
        bindParams(ps, params);
        return mapAll(ps);
    }
}

public int countAdvancedSearch(String maPB, String maCV, String trangThai, String gioiTinh,
                                String keyword) throws SQLException {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM NHANVIEN nv ");
    // Câu đếm dùng lại CHÍNH XÁC cùng logic lọc (appendAdvancedFilters), chỉ khác phần SELECT ban đầu
    // (COUNT(*) thay vì SELECT_BASE) và KHÔNG có LIMIT/OFFSET ở cuối.
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
```

```java
private void appendAdvancedFilters(StringBuilder sql, List<Object> params, String maPB, String maCV,
                                    String trangThai, String gioiTinh, String keyword) {
    List<String> conditions = new ArrayList<>();
    // Danh sách các "mẩu điều kiện" SQL sẽ nối bằng AND — CHỈ chứa những điều kiện người dùng THẬT SỰ chọn.

    if (maPB != null) {
        conditions.add("nv.MaPB = ?");
        params.add(maPB);
        // Thêm mẩu điều kiện VÀ tham số tương ứng CÙNG LÚC, trong CÙNG 1 khối if -> giữ đúng thứ tự khớp nhau.
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
        // Dấu ngoặc () BAO QUANH 3 điều kiện OR — bắt buộc phải có, nếu không mệnh đề OR này sẽ áp dụng
        // SAI PHẠM VI khi kết hợp với các điều kiện AND khác (lỗi độ ưu tiên toán tử rất dễ mắc).
        String like = "%" + keyword.trim() + "%";
        params.add(like);
        params.add(like);
        params.add(like);
        // 1 điều kiện keyword nhưng có 3 dấu "?" bên trong -> phải add() params đúng 3 lần liên tiếp.
    }

    if (!conditions.isEmpty()) {
        sql.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        // String.join(" AND ", conditions): nối các mẩu điều kiện bằng " AND ". VD
        // ["nv.MaPB = ?", "nv.GioiTinh = ?"] -> "nv.MaPB = ? AND nv.GioiTinh = ?"
        // Không chọn lọc gì cả (conditions rỗng) -> KHÔNG thêm mệnh đề WHERE nào (lấy hết dữ liệu).
    }
}
```

### 4.9 `bindParams()` — bind danh sách tham số linh hoạt

```java
private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
    for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));
        // ps.setObject() nhận Object BẤT KỲ (String, Integer...) thay vì phải gọi đúng setString/setInt
        // theo từng kiểu cụ thể — cần thiết ở đây vì "params" trộn lẫn nhiều kiểu dữ liệu khác nhau
        // (String cho mã/trạng thái, Integer cho pageSize/offset).
        // "i + 1": tham số PreparedStatement đánh số TỪ 1, còn index của List bắt đầu TỪ 0.
    }
}
```

### 4.10 `mapAll()`, `bindEmployeeFields()`, `mapRow()` — 3 hàm "lõi" dùng lại khắp file

```java
private List<NhanVien> mapAll(PreparedStatement ps) throws SQLException {
    List<NhanVien> result = new ArrayList<>();
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            result.add(mapRow(rs));
        }
    }
    return result;
    // Hàm DÙNG CHUNG cho findPage/search/advancedSearch — cả 3 hàm đó đều đã tự chuẩn bị sẵn
    // PreparedStatement (bind tham số xong), chỉ còn thiếu bước "chạy + map kết quả" GIỐNG HỆT NHAU
    // -> tách riêng ra đây để KHÔNG PHẢI lặp lại đoạn "while (rs.next()) { result.add(mapRow(rs)); }"
    // ở cả 3 nơi.
}
```

```java
private int bindEmployeeFields(PreparedStatement ps, NhanVien nv, int startIndex) throws SQLException {
    int i = startIndex;
    // "startIndex" linh hoạt: insert() gọi với startIndex=2 (vì vị trí 1 là MaNV), update() gọi với
    // startIndex=1 (vì update() không có MaNV ở đầu câu SET) — CÙNG 1 hàm bind dùng được cho cả 2 nơi.
    ps.setString(i++, nv.getHoTen());
    // "i++": vừa DÙNG giá trị hiện tại của i để set tham số, vừa TĂNG i lên 1 SAU ĐÓ — cách viết gọn
    // để không phải viết "ps.setString(i, ...); i = i + 1;" nhắc lại 14 lần.
    ps.setDate(i++, util.DateUtil.toSqlDate(nv.getNgaySinh()));
    // nv.getNgaySinh() trả về java.time.LocalDate (kiểu hiện đại), nhưng PreparedStatement.setDate()
    // CHỈ nhận java.sql.Date (kiểu JDBC cũ) -> DateUtil.toSqlDate() làm cầu nối convert giữa 2 kiểu.
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
    // setBigDecimal(): PreparedStatement có hàm set RIÊNG cho kiểu BigDecimal (tiền tệ chính xác),
    // khác setDouble()/setFloat() dành cho số thực thông thường.
    ps.setString(i++, nv.getTrangThai());
    return i;
    // Trả về vị trí "?" TIẾP THEO (sau khi đã bind hết 14 field) — update() dùng giá trị này để biết
    // chính xác set MaNV vào vị trí nào cho điều kiện WHERE (xem mục 4.3), KHÔNG cần đếm tay.
}
```

```java
private NhanVien mapRow(ResultSet rs) throws SQLException {
    NhanVien nv = new NhanVien();
    nv.setMaNV(rs.getString("MaNV"));
    nv.setHoTen(rs.getString("HoTen"));
    nv.setNgaySinh(util.DateUtil.fromSqlDate(rs.getDate("NgaySinh")));
    // Chiều NGƯỢC LẠI với bindEmployeeFields(): rs.getDate() trả java.sql.Date (kiểu JDBC), convert
    // sang java.time.LocalDate (kiểu hiện đại) bằng DateUtil.fromSqlDate() để dùng trong toàn bộ View.
    nv.setGioiTinh(rs.getString("GioiTinh"));
    nv.setSoCCCD(rs.getString("SoCCCD"));
    nv.setSoDienThoai(rs.getString("SoDienThoai"));
    nv.setEmail(rs.getString("Email"));
    nv.setDiaChi(rs.getString("DiaChi"));
    nv.setAvatarPath(rs.getString("AvatarPath"));
    nv.setMaPB(rs.getString("MaPB"));
    nv.setTenPhongBan(rs.getString("TenPhongBan"));
    // "TenPhongBan" là cột LẤY ĐƯỢC NHỜ LEFT JOIN với bảng PHONGBAN trong SELECT_BASE (mục 4.1) —
    // KHÔNG có trong bảng NHANVIEN gốc, chỉ tồn tại trong KẾT QUẢ TRUY VẤN nhờ JOIN.
    nv.setMaCV(rs.getString("MaCV"));
    nv.setTenChucVu(rs.getString("TenChucVu"));
    nv.setNgayVaoLam(util.DateUtil.fromSqlDate(rs.getDate("NgayVaoLam")));
    nv.setLoaiHopDong(rs.getString("LoaiHopDong"));
    nv.setMucLuongCoBan(rs.getBigDecimal("MucLuongCoBan"));
    nv.setTrangThai(rs.getString("TrangThai"));
    return nv;
    // Hàm này được GỌI TỪ mapAll() (dùng cho danh sách nhiều dòng) VÀ trực tiếp từ findById()
    // (dùng cho đúng 1 dòng) — tránh viết lại logic "đổ ResultSet vào NhanVien" 2 lần.
}
```

---

## 5. `TaiKhoanDAO.java`

File: [`database/dao/TaiKhoanDAO.java`](src/src/database/dao/TaiKhoanDAO.java)

```java
private static final String SELECT_BASE =
        "SELECT tk.MaTK, tk.TenDangNhap, tk.MatKhauHash, tk.VaiTro, tk.MaNV, nv.HoTen, " +
                "tk.TrangThai, tk.NgayTao, tk.LanDangNhapCuoi " +
                "FROM TAIKHOAN tk LEFT JOIN NHANVIEN nv ON tk.MaNV = nv.MaNV ";
// LEFT JOIN với NHANVIEN để lấy kèm Họ tên nhân viên (nếu tài khoản có gắn hồ sơ NV) — vẫn trả về
// dòng dù MaNV là NULL (tài khoản admin kỹ thuật không gắn nhân viên nào).
```

### 5.1 `checkLogin()` — xác thực đăng nhập (đã giải thích chi tiết trong `LUONG_HOAT_DONG.md` mục 2.3, tóm tắt lại đây để đủ bộ)

```java
public TaiKhoan checkLogin(String tenDangNhap, String matKhauGoc) throws SQLException {
    String sql = SELECT_BASE + "WHERE tk.TenDangNhap = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, tenDangNhap);
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;                       // không có tài khoản nào khớp username
            }
            TaiKhoan tk = mapRow(rs);
            if (!"HoatDong".equals(tk.getTrangThai())) {
                return null;                       // tài khoản đang bị khóa
            }
            if (!BCrypt.checkpw(matKhauGoc, tk.getMatKhauHash())) {
                // 🟡 NGOÀI CHƯƠNG TRÌNH — so khớp mật khẩu gõ vào với hash đã lưu (băm 1 chiều,
                // không giải mã ngược được).
                return null;                       // sai mật khẩu
            }
            capNhatLanDangNhapCuoi(tk.getMaTK());   // ghi nhận thời điểm đăng nhập gần nhất
            return tk;
        }
    }
}

private void capNhatLanDangNhapCuoi(int maTK) throws SQLException {
    String sql = "UPDATE TAIKHOAN SET LanDangNhapCuoi = NOW() WHERE MaTK = ?";
    // NOW(): hàm MySQL lấy thời gian hiện tại của SERVER database.
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, maTK);
        ps.executeUpdate();
    }
}
```

### 5.2 `findAll()`, `existsUsername()` — liệt kê & kiểm tra trùng

```java
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
    // Dùng cho AccountManagementPanel.reloadData() — đổ toàn bộ tài khoản lên bảng, KHÔNG phân trang
    // (khác NhanVienDAO.findPage()) vì số lượng tài khoản thường ít hơn nhiều so với số nhân viên.
}

public boolean existsUsername(String tenDangNhap) throws SQLException {
    String sql = "SELECT 1 FROM TAIKHOAN WHERE TenDangNhap = ?";
    // "SELECT 1" thay vì "SELECT *": chỉ cần biết CÓ TỒN TẠI dòng khớp hay không, không cần lấy dữ
    // liệu cột nào cả -> "1" là 1 hằng số giả, tránh lãng phí tải dữ liệu không cần thiết.
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, tenDangNhap);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next();
            // rs.next() vừa DI CHUYỂN con trỏ vừa TRẢ VỀ true/false có dòng nào không — dùng trực
            // tiếp giá trị này làm kết quả hàm (true = đã tồn tại username, false = chưa có).
        }
    }
}
```

### 5.3 `create()` — tạo tài khoản, băm mật khẩu

```java
public void create(String tenDangNhap, String matKhauGoc, String vaiTro) throws SQLException {
    String hash = BCrypt.hashpw(matKhauGoc, BCrypt.gensalt(12));
    // 🟡 NGOÀI CHƯƠNG TRÌNH:
    //   BCrypt.gensalt(12): sinh chuỗi "salt" ngẫu nhiên, số 12 là "cost factor" (độ khó băm, càng
    //   cao càng an toàn nhưng càng chậm) -> mỗi lần gọi ra 1 salt KHÁC NHAU dù cùng 1 mật khẩu gốc.
    //   BCrypt.hashpw(mật khẩu gốc, salt): trộn + băm nhiều vòng, trả về chuỗi hash dạng "$2a$12$...".
    String sql = "INSERT INTO TAIKHOAN (TenDangNhap, MatKhauHash, VaiTro, MaNV, TrangThai) VALUES (?,?,?,NULL,'HoatDong')";
    // MaNV LUÔN là hằng số NULL cứng trong câu SQL (không có dấu "?" ở vị trí đó) — tài khoản tạo
    // qua màn "Quản lý tài khoản" KHÔNG BAO GIỜ tự động gắn với hồ sơ nhân viên nào.
    // TrangThai cũng là hằng số 'HoatDong' -> tài khoản mới LUÔN được tạo ở trạng thái hoạt động.
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, tenDangNhap);
        ps.setString(2, hash);       // lưu HASH, KHÔNG BAO GIỜ lưu matKhauGoc
        ps.setString(3, vaiTro);
        ps.executeUpdate();
    }
}
```

### 5.4 `setTrangThai()`, `setVaiTro()` — khóa/mở khóa, đổi vai trò

```java
public void setTrangThai(int maTK, String trangThai) throws SQLException {
    String sql = "UPDATE TAIKHOAN SET TrangThai = ? WHERE MaTK = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, trangThai);
        ps.setInt(2, maTK);
        // setInt() (không phải setString()) vì cột MaTK trong DB là kiểu int AUTO_INCREMENT,
        // tham số Java truyền vào cũng là "int maTK" — cần gọi ĐÚNG hàm set tương ứng với kiểu dữ liệu.
        ps.executeUpdate();
    }
    // Dùng chung cho CẢ 2 chiều khóa/mở khóa — AccountManagementPanel.toggleLock() tự tính sẵn
    // trangThai mới ("HoatDong" hoặc "KhoaTaiKhoan") rồi truyền vào, hàm này không tự đảo trạng thái.
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
```

### 5.5 `mapRow()` — map ResultSet sang model TaiKhoan

```java
private TaiKhoan mapRow(ResultSet rs) throws SQLException {
    TaiKhoan tk = new TaiKhoan();
    tk.setMaTK(rs.getInt("MaTK"));
    tk.setTenDangNhap(rs.getString("TenDangNhap"));
    tk.setMatKhauHash(rs.getString("MatKhauHash"));
    // Lấy CẢ hash mật khẩu vào object TaiKhoan trong bộ nhớ (cần thiết để checkLogin() so khớp bằng
    // BCrypt.checkpw() ở mục 5.1) — nhưng View KHÔNG BAO GIỜ hiện giá trị này lên giao diện.
    tk.setVaiTro(rs.getString("VaiTro"));
    tk.setMaNV(rs.getString("MaNV"));
    tk.setHoTenNhanVien(rs.getString("HoTen"));
    tk.setTrangThai(rs.getString("TrangThai"));

    Timestamp ngayTao = rs.getTimestamp("NgayTao");
    tk.setNgayTao(ngayTao == null ? null : ngayTao.toLocalDateTime());
    // 🟡 NGOÀI CHƯƠNG TRÌNH — java.sql.Timestamp (kiểu JDBC) convert sang java.time.LocalDateTime
    // (kiểu hiện đại) để dùng xuyên suốt View. Kiểm tra null trước vì cột này có thể NULL trong DB.

    Timestamp lanDangNhapCuoi = rs.getTimestamp("LanDangNhapCuoi");
    tk.setLanDangNhapCuoi(lanDangNhapCuoi == null ? null : lanDangNhapCuoi.toLocalDateTime());
    return tk;
}
```

---

## 6. Tổng kết: trong chương trình vs ngoài chương trình

### 6.1 Trong chương trình đã học (áp dụng trong tầng DAO)

| Kiến thức | Xuất hiện ở đâu trong tầng DAO |
|---|---|
| if/else | `generateNextMaNV` (kiểm tra `maPB` rỗng), `appendAdvancedFilters` (từng `if (x != null)`), `checkLogin` (kiểm tra trạng thái/mật khẩu) |
| for | `bindParams` (`for (int i = 0; i < params.size(); i++)`) |
| Array | Không dùng array trực tiếp trong tầng DAO (dùng `List`/`Map` thay thế — coi phần "ngoài chương trình") |
| Đọc/ghi file | Không có trong tầng DAO (đọc `db.properties` nằm ở `DBConnection`, xem `LUONG_HOAT_DONG.md` mục 10) |
| Exception | `throws SQLException` ở MỌI phương thức; `catch (NumberFormatException ignored)` trong `generateNextMaNV` |
| OOP | Đóng gói (field `private static final`), kế thừa `implements DanhMucDAO`, override `@Override` |
| MySQL JDBC | `Connection`, `DriverManager` (qua `DBConnection`) |
| Package | Toàn bộ 5 file nằm trong package `database.dao` |
| PreparedStatement | Mọi câu SQL trong cả 5 file |
| Insert MySQL JDBC (dynamic data) | `NhanVienDAO.insert`, `TaiKhoanDAO.create`, `PhongBanDAO.insert`, `ChucVuDAO.insert` |
| Liệt kê dữ liệu JDBC | `findAll`, `findPage`, `mapAll`, `mapRow` ở cả 4 file DAO cụ thể |

### 6.2 Ngoài chương trình đã học (áp dụng trong tầng DAO)

| Kỹ thuật | Xuất hiện ở đâu | Vì sao khác chương trình đã học |
|---|---|---|
| Interface `DanhMucDAO` + đa hình | `DanhMucDAO`, `PhongBanDAO`, `ChucVuDAO` | 1 đoạn code UI (`CategoryTab`) dùng chung cho 2 DAO khác nhau |
| Wildcard generic `List<? extends DanhMuc>` | `DanhMucDAO.findAll()` | Kiểu trả về linh hoạt cho nhiều class con |
| jBCrypt (`BCrypt.hashpw`/`checkpw`) | `TaiKhoanDAO` | Băm mật khẩu 1 chiều, thư viện ngoài |
| SQL động (`StringBuilder` + `List<Object>`) | `NhanVienDAO.advancedSearch`, `appendAdvancedFilters` | Lọc theo nhiều điều kiện tùy chọn |
| Phân trang `LIMIT/OFFSET` | `NhanVienDAO.findPage/search/advancedSearch` | Không tải hết dữ liệu 1 lần |
| `Map`/`LinkedHashMap` | `NhanVienDAO.countByPhongBan` | Gom nhóm thống kê dạng key–value |
| `java.time` (`LocalDate`/`LocalDateTime`) qua `DateUtil` | `NhanVienDAO.mapRow/bindEmployeeFields`, `TaiKhoanDAO.mapRow` | API ngày giờ hiện đại thay `java.sql.Date/Timestamp` thô |
| `BigDecimal` | `NhanVienDAO` (`setBigDecimal`, `getBigDecimal`) | Tránh sai số số thực khi lưu/đọc tiền lương |
| Hàm ngày tháng của MySQL (`NOW()`, `YEAR()`, `MONTH()`, `CURDATE()`) | `TaiKhoanDAO.capNhatLanDangNhapCuoi`, `NhanVienDAO.countNewThisMonth` | Tính toán ngày tháng ngay trên server DB thay vì kéo dữ liệu về so sánh bằng Java |
| `GROUP BY` (SQL gộp nhóm) | `NhanVienDAO.countByPhongBan` | Thống kê tổng hợp, khác câu SELECT liệt kê thông thường |
| Tách hàm dùng chung (`bindEmployeeFields`, `mapAll`, `mapRow`) để tái sử dụng giữa nhiều phương thức | `NhanVienDAO` | Kỹ thuật tổ chức code tránh lặp lại (DRY), có tính "thiết kế" hơn bài tập JDBC cơ bản |
