# Giải thích chi tiết các luồng hoạt động — QLNhanSu

Tài liệu giải thích **cấu trúc thư mục** và **toàn bộ luồng chạy** của ứng dụng, kèm code thật của dự án với **chú thích từng dòng/đoạn** (các dòng `//` in nghiêng sau code là chú thích **mình thêm vào để giải thích**, không phải comment có sẵn trong source — bạn có thể đối chiếu lại file gốc để thấy code không có comment).

Những kỹ thuật/khái niệm **không nằm trong chương trình đã học** (if/else, switch, do-while, for, array, đọc/ghi file, exception, OOP cơ bản, MySQL JDBC, package/abstract class, PreparedStatement, insert/list dữ liệu JDBC, form Swing, validate form JDBC-Swing) được đánh dấu:

> 🟡 **NGOÀI CHƯƠNG TRÌNH** — kèm giải thích ngắn gọn nó là gì.

---

## Mục lục

0. [Tổng quan cấu trúc thư mục](#0-tổng-quan-cấu-trúc-thư-mục)
1. [Khởi động ứng dụng](#1-khởi-động-ứng-dụng)
2. [Luồng đăng nhập](#2-luồng-đăng-nhập)
3. [Luồng vào màn hình chính (MainFrame) & điều hướng](#3-luồng-vào-màn-hình-chính-mainframe--điều-hướng)
4. [Luồng đăng xuất](#4-luồng-đăng-xuất)
5. [Luồng Dashboard (Tổng quan)](#5-luồng-dashboard-tổng-quan)
6. [Luồng Quản lý nhân viên](#6-luồng-quản-lý-nhân-viên)
7. [Luồng Danh mục (Phòng ban / Chức vụ)](#7-luồng-danh-mục-phòng-ban--chức-vụ)
8. [Luồng Quản lý tài khoản (Phân quyền)](#8-luồng-quản-lý-tài-khoản-phân-quyền)
9. [Luồng ghi log hoạt động (Audit log)](#9-luồng-ghi-log-hoạt-động-audit-log)
10. [Luồng nền: kết nối DB cho mọi thao tác](#10-luồng-nền-kết-nối-db-cho-mọi-thao-tác)
11. [Tổng hợp: trong chương trình vs ngoài chương trình](#11-tổng-hợp-trong-chương-trình-vs-ngoài-chương-trình)

---

## 0. Tổng quan cấu trúc thư mục

```
NhanSu/                                  ← thư mục gốc (repo git)
├── Data.Basesql.sql                     ← script SQL tạo database + dữ liệu mẫu, chạy 1 lần trong MySQL
├── README.md                            ← tài liệu mô tả dự án
├── .gitignore                           ← danh sách file/thư mục không đưa lên git
├── .idea/                               ← cấu hình riêng của IntelliJ, không phải code
├── avatars/                             ← ảnh đại diện nhân viên, SINH RA lúc chạy app (gitignore)
├── logs/                                ← file log hoạt động, SINH RA lúc chạy app (gitignore)
├── out/                                 ← build output (.class) của IntelliJ, tự sinh (gitignore)
└── src/                                 ← MODULE ROOT của IntelliJ (chứa file .iml)
    ├── QLNhanSu.iml                     ← khai báo module + đường dẫn tới các thư viện ngoài (.jar)
    └── src/                             ← SOURCE ROOT thật sự — package Java bắt đầu ngay từ đây
        ├── database/                    ← tầng kết nối & truy xuất dữ liệu (JDBC)
        │   ├── DBConnection.java        ← đọc db.properties, mở Connection tới MySQL
        │   ├── db.properties            ← url/username/password kết nối MySQL (không commit)
        │   └── dao/                     ← 1 class DAO cho mỗi bảng, chứa toàn bộ câu SQL
        │       ├── DanhMucDAO.java      ← interface CRUD dùng chung cho Phòng ban & Chức vụ
        │       ├── PhongBanDAO.java
        │       ├── ChucVuDAO.java
        │       ├── NhanVienDAO.java     ← CRUD + tìm kiếm + lọc + phân trang + sinh Mã NV
        │       └── TaiKhoanDAO.java     ← đăng nhập (BCrypt), tạo/khóa/đổi vai trò tài khoản
        │
        ├── model/                       ← POJO ánh xạ 1-1 với các bảng trong DB
        │   ├── DanhMuc.java             ← interface {getId, getTen, getMoTa}
        │   ├── PhongBan.java / ChucVu.java  ← cùng implement DanhMuc
        │   ├── NhanVien.java
        │   ├── TaiKhoan.java
        │   └── Session.java             ← KHÔNG map bảng nào — giữ tài khoản đang đăng nhập
        │
        ├── util/                        ← hàm tiện ích dùng chung, không giữ trạng thái
        │   ├── ValidationUtil.java      ← validate email, SĐT, CCCD bằng regex
        │   ├── DateUtil.java            ← chuyển đổi chuỗi "dd/MM/yyyy" ⇄ LocalDate
        │   ├── TableModelUtil.java      ← đổ List<NhanVien> vào JTable
        │   └── ActivityLogger.java      ← ghi log sửa/xóa nhân viên ra file text
        │
        └── view/                        ← toàn bộ giao diện Swing
            ├── Main.java                ← entry point, mở màn Login đầu tiên
            ├── Login.java / .form       ← màn đăng nhập
            ├── MainFrame.java / .form   ← khung chính: sidebar + top bar + vùng nội dung (CardLayout)
            └── panels/                  ← các "trang" hiển thị BÊN TRONG MainFrame (không phải JFrame riêng)
                ├── DashboardPanel.java / .form
                ├── ListEmployeesPanel.java / .form
                ├── AddEmployeesPanel.java / .form
                ├── AccountManagementPanel.java   (thuần Java, không .form)
                └── CategoryManagementPanel.java  (thuần Java, không .form)
```

**Quy tắc phụ thuộc giữa các package** (chỉ đi 1 chiều, trừ `model`):

```
view / view.panels  →  database.dao  →  database (DBConnection)  →  MySQL
        ↑                                                              │
        └───────────────────────── model ─────────────────────────────┘
```

- `view` gọi `dao`, `dao` gọi `DBConnection` — **không** có chiều ngược lại.
- `model` là "phương tiện vận chuyển" dữ liệu: `dao` tạo object `model` từ `ResultSet` rồi trả lên `view`; `view` tạo object `model` từ form rồi đưa xuống `dao` để lưu.
- `util` được cả `view` lẫn `dao` dùng chung, nhưng bản thân `util` không phụ thuộc ngược lại `view`/`dao`.
- File `.form` đi kèm 1 số class trong `view/` là XML do **IntelliJ GUI Designer** sinh ra khi kéo-thả thiết kế giao diện; IntelliJ tự generate code khởi tạo component từ đây lúc build. 2 panel không có `.form` (`AccountManagementPanel`, `CategoryManagementPanel`) được dựng hoàn toàn bằng code tay.

---

## 1. Khởi động ứng dụng

File: [`view/Main.java`](src/src/view/Main.java)

```java
package view;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // JVM gọi hàm main() này đầu tiên khi bạn Run project — điểm vào (entry point) của toàn bộ ứng dụng

        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
        // SwingUtilities.invokeLater(...):
        //   🟡 NGOÀI CHƯƠNG TRÌNH — đẩy đoạn code bên trong vào hàng đợi của Event Dispatch Thread (EDT),
        //   thread riêng mà Swing dùng để vẽ và xử lý sự kiện UI. Đây là cách khởi động Swing "đúng chuẩn",
        //   tránh việc tạo/động vào component UI từ thread chính (main thread) gây lỗi khó debug.
        //
        // () -> new Login().setVisible(true)  — lambda expression (Java 8+), tương đương viết:
        //   new Runnable() {
        //       public void run() {
        //           new Login().setVisible(true);
        //       }
        //   }
        //   Bên trong lambda:
        //     new Login()        -> gọi constructor của Login, dựng toàn bộ giao diện + gắn sự kiện
        //     .setVisible(true)  -> hiển thị cửa sổ lên màn hình (JFrame mặc định ẩn khi mới tạo)
    }
}
```

---

## 2. Luồng đăng nhập

File: [`view/Login.java`](src/src/view/Login.java), [`database/dao/TaiKhoanDAO.java`](src/src/database/dao/TaiKhoanDAO.java), [`model/Session.java`](src/src/model/Session.java)

### 2.1 Bắt sự kiện bấm nút Đăng nhập

```java
btnLogin.addActionListener(e -> handleLogin());
// Gắn "người nghe sự kiện" (ActionListener) vào nút Đăng nhập.
// Khi người dùng click chuột vào nút, Swing tự gọi handleLogin() — lập trình hướng sự kiện (event-driven).

txtPassword.addActionListener(e -> handleLogin());
// Gắn thêm sự kiện cho ô mật khẩu: bấm Enter trong khi con trỏ đang ở ô này CŨNG gọi handleLogin(),
// giúp người dùng không bắt buộc phải dùng chuột.
```

### 2.2 `handleLogin()` — toàn bộ logic đăng nhập, chạy tuần tự

```java
private void handleLogin() {
    String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
    // Đọc text từ ô username. Toán tử 3 ngôi (?:) tránh NullPointerException nếu getText() trả null.
    // .trim() cắt khoảng trắng thừa 2 đầu (người dùng lỡ gõ dấu cách trước/sau).

    String password = new String(txtPassword.getPassword());
    // JPasswordField trả về char[] (mảng ký tự) chứ KHÔNG trả String — lý do bảo mật (String tồn tại lâu
    // trong bộ nhớ, char[] có thể xóa thủ công). Ở đây convert sang String cho tiện dùng tiếp.

    if (username.isEmpty()) {
        showError("Vui lòng nhập tên đăng nhập / email.");
        txtUsername.requestFocus();     // đưa con trỏ về lại ô username để người dùng gõ tiếp luôn
        return;                          // dừng hàm ngay, không chạy các bước validate/đăng nhập bên dưới
    }

    if (!EMAIL_PATTERN.matcher(username).matches()) {
        // 🟡 NGOÀI CHƯƠNG TRÌNH — EMAIL_PATTERN là java.util.regex.Pattern đã compile sẵn (biểu thức
        // chính quy). matcher(username).matches() kiểm tra TOÀN BỘ chuỗi username có khớp định dạng
        // email (ten@domain.com) hay không — mạnh hơn nhiều so với kiểm tra username.contains("@").
        showError("Email không đúng định dạng (vd: ten@domain.com).");
        txtUsername.requestFocus();
        return;
    }

    if (password.isEmpty()) {
        showError("Vui lòng nhập mật khẩu.");
        txtPassword.requestFocus();
        return;
    }

    if (password.length() < MIN_PASSWORD_LENGTH) {
        // MIN_PASSWORD_LENGTH = 6 (hằng số static final khai báo ở đầu class)
        showError("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự.");
        txtPassword.requestFocus();
        return;
    }

    // ---- Tới đây, dữ liệu nhập vào đã qua hết validate phía client (View) ----

    btnLogin.setEnabled(false);
    // Khóa nút Đăng nhập ngay khi bắt đầu gọi DB, tránh người dùng bấm liên tục gây gọi DB nhiều lần
    // trong lúc đang chờ phản hồi.

    try {
        TaiKhoan taiKhoan = new TaiKhoanDAO().checkLogin(username, password);
        // new TaiKhoanDAO()  -> tạo 1 instance DAO mới (DAO này không giữ trạng thái, tạo mới mỗi lần dùng)
        // .checkLogin(...)   -> gọi xuống tầng DAO để kiểm tra trong MySQL (xem mục 2.3 bên dưới)
        // Vì checkLogin() khai báo "throws SQLException" nên bắt buộc phải bọc trong try/catch ở đây.

        if (taiKhoan == null) {
            // DAO trả về null trong 3 trường hợp: sai username, sai password, hoặc tài khoản bị khóa
            // (View không phân biệt được lý do cụ thể — đây là chủ ý để tránh lộ thông tin cho kẻ dò mật khẩu)
            showError("Sai tên đăng nhập hoặc mật khẩu, hoặc tài khoản đã bị khóa.");
            return;
            // return ở đây NẰM TRONG try — khối finally bên dưới vẫn chạy để mở lại nút Đăng nhập
        }

        if (chkRemember.isSelected()) {
        }
        // Checkbox "Ghi nhớ đăng nhập" — khối rỗng, TÍNH NĂNG CHƯA ĐƯỢC CÀI ĐẶT (TODO còn bỏ ngỏ)

        Session.setCurrentAccount(taiKhoan);
        // Lưu tài khoản vừa đăng nhập vào biến static của Session — xem mục 2.4

        new MainFrame().setVisible(true);
        // Mở màn hình chính. Constructor MainFrame() đọc lại Session.getCurrentAccount() ngay bên trong
        // để biết đang set phân quyền cho ai (xem mục 3).

        this.dispose();
        // Đóng (giải phóng tài nguyên) cửa sổ Login hiện tại — không dùng setVisible(false) vì
        // cửa sổ Login sẽ không cần dùng lại nữa cho tới khi đăng xuất (lúc đó tạo Login mới).

    } catch (SQLException e) {
        // Bắt lỗi khi không kết nối được MySQL (VD service MySQL chưa bật, sai db.properties...)
        showError("Không thể kết nối cơ sở dữ liệu: " + e.getMessage());
    } finally {
        btnLogin.setEnabled(true);
        // finally LUÔN chạy dù try thành công, return sớm, hay catch bắt lỗi — đảm bảo nút Đăng nhập
        // không bao giờ bị khóa vĩnh viễn.
    }
}
```

### 2.3 `TaiKhoanDAO.checkLogin()` — tầng DAO, chạm thẳng vào MySQL

```java
public TaiKhoan checkLogin(String tenDangNhap, String matKhauGoc) throws SQLException {
    String sql = SELECT_BASE + "WHERE tk.TenDangNhap = ?";
    // SELECT_BASE là hằng số static ở đầu class, chứa sẵn phần
    // "SELECT ... FROM TAIKHOAN tk LEFT JOIN NHANVIEN nv ON tk.MaNV = nv.MaNV"
    // -> LEFT JOIN để lấy kèm HoTen nhân viên (nếu tài khoản có gắn hồ sơ NV), vẫn trả về dòng
    //    kể cả khi MaNV là NULL (tài khoản kỹ thuật/admin không gắn nhân viên nào).
    // Dấu "?" là tham số giữ chỗ (placeholder) của PreparedStatement — sẽ bind giá trị thật ở dưới.

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        // try-with-resources: cả conn và ps sẽ TỰ ĐỘNG được close() khi ra khỏi khối try,
        // dù có exception hay không — không cần viết finally { conn.close(); } thủ công.

        ps.setString(1, tenDangNhap);
        // Gán giá trị thật cho dấu "?" thứ 1. PreparedStatement tự escape ký tự đặc biệt,
        // CHỐNG SQL INJECTION — khác hẳn việc nối chuỗi "WHERE TenDangNhap = '" + tenDangNhap + "'".

        try (ResultSet rs = ps.executeQuery()) {
            // executeQuery() dùng cho câu SELECT, trả về ResultSet — con trỏ đang đứng TRƯỚC dòng đầu tiên.

            if (!rs.next()) {
                // rs.next() vừa "nhảy" con trỏ tới dòng tiếp theo, vừa trả về true/false có dòng đó không.
                // Không có dòng nào khớp TenDangNhap -> tài khoản không tồn tại.
                return null;
            }

            TaiKhoan tk = mapRow(rs);
            // Chuyển dòng dữ liệu thô (ResultSet) hiện tại thành 1 object TaiKhoan (Model) — xem mapRow() bên dưới.

            if (!"HoatDong".equals(tk.getTrangThai())) {
                // So sánh chuỗi bằng .equals() (không dùng ==) — tránh lỗi NullPointerException nếu
                // gọi ngược tk.getTrangThai().equals("HoatDong") mà getTrangThai() null.
                return null;   // tài khoản đang bị khóa ("KhoaTaiKhoan")
            }

            if (!BCrypt.checkpw(matKhauGoc, tk.getMatKhauHash())) {
                // 🟡 NGOÀI CHƯƠNG TRÌNH — BCrypt.checkpw(mật khẩu người dùng gõ, hash đã lưu trong DB):
                // tự tách "salt" ra khỏi chuỗi hash, băm lại mật khẩu vừa gõ với salt đó, rồi so sánh
                // 2 chuỗi hash. KHÔNG BAO GIỜ giải mã ngược hash về mật khẩu gốc được (băm 1 chiều).
                return null;   // sai mật khẩu
            }

            capNhatLanDangNhapCuoi(tk.getMaTK());
            // Ghi nhận thời điểm đăng nhập thành công gần nhất (xem hàm bên dưới) — chạy 1 UPDATE riêng.

            return tk;
            // Trả object TaiKhoan hợp lệ ngược lên View.
        }
    }
    // Không có catch ở đây: SQLException được khai báo "throws" ở đầu hàm, đẩy trách nhiệm xử lý
    // ngược lên nơi gọi hàm này (Login.handleLogin() ở mục 2.2 là nơi có try/catch thật sự).
}
```

```java
private void capNhatLanDangNhapCuoi(int maTK) throws SQLException {
    String sql = "UPDATE TAIKHOAN SET LanDangNhapCuoi = NOW() WHERE MaTK = ?";
    // NOW() là hàm của MySQL, lấy thời gian hiện tại của SERVER database (không phải máy chạy app).
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, maTK);
        ps.executeUpdate();
        // executeUpdate() dùng cho INSERT/UPDATE/DELETE, trả về SỐ DÒNG bị ảnh hưởng (ở đây không dùng tới).
    }
}
```

```java
private TaiKhoan mapRow(ResultSet rs) throws SQLException {
    TaiKhoan tk = new TaiKhoan();
    tk.setMaTK(rs.getInt("MaTK"));
    // rs.getInt("TênCột") lấy giá trị theo TÊN CỘT trong câu SELECT (thay vì theo số thứ tự cột,
    // dễ đọc và không lo sai khi câu SELECT đổi thứ tự cột).
    tk.setTenDangNhap(rs.getString("TenDangNhap"));
    tk.setMatKhauHash(rs.getString("MatKhauHash"));
    tk.setVaiTro(rs.getString("VaiTro"));
    tk.setMaNV(rs.getString("MaNV"));
    tk.setHoTenNhanVien(rs.getString("HoTen"));      // cột lấy được nhờ LEFT JOIN với NHANVIEN
    tk.setTrangThai(rs.getString("TrangThai"));

    Timestamp ngayTao = rs.getTimestamp("NgayTao");
    tk.setNgayTao(ngayTao == null ? null : ngayTao.toLocalDateTime());
    // 🟡 NGOÀI CHƯƠNG TRÌNH — java.sql.Timestamp (kiểu JDBC trả về cho cột datetime) được convert
    // sang java.time.LocalDateTime (API ngày giờ hiện đại của Java 8+) để dùng trong toàn bộ ứng dụng,
    // thay vì giữ nguyên kiểu java.sql.Timestamp/java.util.Date cũ.

    Timestamp lanDangNhapCuoi = rs.getTimestamp("LanDangNhapCuoi");
    tk.setLanDangNhapCuoi(lanDangNhapCuoi == null ? null : lanDangNhapCuoi.toLocalDateTime());
    return tk;
}
```

### 2.4 `Session.java` — nơi lưu "ai đang đăng nhập"

```java
public class Session {

    private static TaiKhoan currentAccount;
    // 🟡 NGOÀI CHƯƠNG TRÌNH — biến "static" nghĩa là CHỈ CÓ 1 bản duy nhất, dùng chung cho toàn bộ
    // ứng dụng (không gắn với 1 object cụ thể nào). Đây là cách "giả lập session" trong app desktop:
    // chỉ có 1 người dùng đăng nhập tại 1 thời điểm trên máy đang chạy app.

    private Session() {
    }
    // Constructor private -> không ai gọi được "new Session()" từ bên ngoài class.
    // Class này chỉ dùng qua các phương thức static, không cần (và không nên) tạo instance.

    public static TaiKhoan getCurrentAccount() {
        return currentAccount;
    }

    public static void setCurrentAccount(TaiKhoan account) {
        currentAccount = account;
    }

    public static void clear() {
        currentAccount = null;
        // Gọi khi đăng xuất — "quên" tài khoản đang đăng nhập.
    }
}
```

---

## 3. Luồng vào màn hình chính (MainFrame) & điều hướng

File: [`view/MainFrame.java`](src/src/view/MainFrame.java)

### 3.1 Constructor — thứ tự khởi tạo

```java
private final TaiKhoan currentAccount = Session.getCurrentAccount();
// Field này được gán NGAY KHI object MainFrame được tạo (trước cả khi chạy code trong constructor bên dưới),
// vì Java khởi tạo field theo thứ tự khai báo, trước phần thân constructor.
// Đọc lại Session — chính là tài khoản đã setCurrentAccount() ở bước 2.2 khi đăng nhập thành công.

public MainFrame() {
    setTitle("HR Management");
    setContentPane(rootPanel);          // rootPanel: JPanel gốc được IntelliJ GUI Designer sinh từ file MainFrame.form
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(1280, 800);
    setMinimumSize(new Dimension(1000, 650));
    setLocationRelativeTo(null);        // null -> JFrame tự canh giữa màn hình

    setupContentCards();   // 1. dựng 5 panel con + nối callback giữa chúng (mục 3.2)
    setupNavigation();      // 2. gắn sự kiện click cho các nút sidebar (mục 3.4)
    setupTopBar();           // 3. gắn sự kiện cho logo + avatar
    applyRolePermissions();   // 4. ẩn/hiện menu theo vai trò (mục 3.3)

    showCard(CARD_DASHBOARD, tổngQuanButton);
    // Sau khi mọi thứ đã dựng xong, chủ động hiện card Dashboard làm màn hình mặc định khi mới vào.
}
```

### 3.2 `setupContentCards()` — dựng các "trang" và nối chúng với nhau

```java
private void setupContentCards() {
    cardLayout = new CardLayout();
    contentPanel.setLayout(cardLayout);
    // 🟡 NGOÀI CHƯƠNG TRÌNH — CardLayout: layout đặc biệt cho phép NHIỀU JPanel "xếp chồng" lên nhau
    // trong contentPanel, nhưng chỉ hiện đúng 1 cái tại 1 thời điểm — giống hiệu ứng chuyển tab/trang.

    dashboardPanel = new DashboardPanel();
    listEmployeesPanel = new ListEmployeesPanel();
    addEmployeesPanel = new AddEmployeesPanel();
    accountManagementPanel = new AccountManagementPanel();
    categoryManagementPanel = new CategoryManagementPanel();
    // Tạo cả 5 panel NGAY TỪ ĐẦU (không đợi người dùng bấm mới tạo) — nghĩa là DashboardPanel/...
    // load dữ liệu từ DB ngay khi vừa đăng nhập xong, dù người dùng chưa bấm vào mục đó.

    contentPanel.add(dashboardPanel.getRootPanel(), CARD_DASHBOARD);
    contentPanel.add(listEmployeesPanel.getRootPanel(), CARD_LIST_EMPLOYEES);
    contentPanel.add(addEmployeesPanel.getRootPanel(), CARD_ADD_EMPLOYEE);
    contentPanel.add(accountManagementPanel.getRootPanel(), CARD_ACCOUNTS);
    contentPanel.add(categoryManagementPanel.getRootPanel(), CARD_CATEGORIES);
    // Đăng ký từng panel vào CardLayout kèm 1 "tên thẻ" (String hằng số, VD "LIST_EMPLOYEES") —
    // về sau gọi cardLayout.show(contentPanel, tênThẻ) để chuyển qua panel tương ứng.

    listEmployeesPanel.setOnAddEmployeeRequested(() -> {
        addEmployeesPanel.resetToAddMode();
        showCard(CARD_ADD_EMPLOYEE, btnQuanLyNhanVien);
    });
    // 🟡 NGOÀI CHƯƠNG TRÌNH — setOnAddEmployeeRequested nhận vào 1 Runnable (lambda không tham số,
    // không trả về gì). ListEmployeesPanel KHÔNG "import AddEmployeesPanel", nó chỉ biết gọi
    // "callback.run()" khi người dùng bấm nút Thêm — chính MainFrame (lớp cha, biết cả 2 panel)
    // mới quyết định bấm xong thì làm gì (ở đây: reset form rồi chuyển card).

    listEmployeesPanel.setOnEditEmployeeRequested((NhanVien nv) -> {
        addEmployeesPanel.loadForEdit(nv);
        showCard(CARD_ADD_EMPLOYEE, btnQuanLyNhanVien);
    });
    // setOnEditEmployeeRequested nhận Consumer<NhanVien> — lambda có 1 tham số đầu vào (NhanVien nv),
    // không trả về gì. "nv" chính là dòng nhân viên người dùng vừa bấm nút Sửa.

    addEmployeesPanel.setOnSavedOrCancelled(() -> {
        listEmployeesPanel.reloadData();
        showCard(CARD_LIST_EMPLOYEES, btnQuanLyNhanVien);
    });
    // Chiều ngược lại: khi AddEmployeesPanel lưu xong (hoặc bấm Hủy), nó gọi callback này —
    // MainFrame cho ListEmployeesPanel load lại dữ liệu mới rồi chuyển về card danh sách.
}
```

### 3.3 `applyRolePermissions()` — phân quyền hiển thị theo vai trò

```java
private void applyRolePermissions() {
    boolean isAdmin = currentAccount != null && currentAccount.isAdmin();
    // isAdmin() là hàm trong model TaiKhoan, thường chỉ là: return "Admin".equals(vaiTro);
    // Kiểm tra currentAccount != null trước để tránh NullPointerException (phòng trường hợp lạ:
    // MainFrame bị mở khi chưa có ai đăng nhập).

    phânQuyềnButton.setVisible(isAdmin);
    càiĐặtButton.setVisible(isAdmin);
    // setVisible(false) làm nút BIẾN MẤT khỏi giao diện (khác setEnabled(false) chỉ làm mờ/khóa nút
    // nhưng vẫn thấy). NhanVien thường sẽ không thấy 2 mục "Phân quyền" / "Danh mục" trên sidebar.

    avatarButton.setText(currentAccount == null ? "..." : currentAccount.getTenDangNhap());
    // Hiện tên đăng nhập lên nút avatar góc trên — hoặc "..." nếu (hiếm khi) không có ai đăng nhập.
}
```

### 3.4 `showCard()` — cơ chế chuyển "trang"

```java
private void showCard(String cardName, JButton navButton) {
    cardLayout.show(contentPanel, cardName);
    // Ra lệnh cho CardLayout: trong số các panel đã add() ở mục 3.2, hiện đúng panel có tên = cardName,
    // ẩn hết các panel còn lại — KHÔNG tạo cửa sổ mới, không mất trạng thái các panel khác.
    setActiveNavButton(navButton);
}

private void setActiveNavButton(JButton button) {
    if (activeNavButton != null) {
        activeNavButton.setSelected(false);
        // Bỏ trạng thái "đang chọn" của nút sidebar TRƯỚC ĐÓ (nếu có) — về mặt UI thường đổi màu nền nút.
    }
    button.setSelected(true);       // Bôi đậm nút vừa bấm
    activeNavButton = button;       // Ghi nhớ lại để lần sau còn biết "bỏ chọn" nút nào
}
```

---

## 4. Luồng đăng xuất

```java
đăngXuấtButton.addActionListener(e -> {
    int confirm = JOptionPane.showConfirmDialog(this,
            "Ban co chac muon dang xuat?", "Dang xuat",
            JOptionPane.YES_NO_OPTION);
    // Hộp thoại xác nhận Yes/No, trả về hằng số int (JOptionPane.YES_OPTION / NO_OPTION / CLOSED_OPTION).

    if (confirm == JOptionPane.YES_OPTION) {
        Session.clear();          // Xóa tài khoản đang lưu trong biến static (mục 2.4)
        dispose();                 // Đóng cửa sổ MainFrame hiện tại, giải phóng tài nguyên
        new Login().setVisible(true);
        // Tạo MỚI 1 cửa sổ Login (không tái sử dụng cửa sổ Login cũ đã dispose() từ lúc đăng nhập)
    }
});
```

---

## 5. Luồng Dashboard (Tổng quan)

File: [`view/panels/DashboardPanel.java`](src/src/view/panels/DashboardPanel.java)

```java
public DashboardPanel() {
    setupActions();
    loadDashboardData();
    // Gọi load dữ liệu NGAY trong constructor — khác với nhiều app khác chỉ load khi người dùng
    // thật sự click vào tab đó. Ở đây vì tất cả panel được tạo sẵn cùng lúc trong MainFrame (mục 3.2).
}
```

```java
private void loadDashboardData() {
    try {
        int tongNV = nhanVienDAO.countAll();
        lblTongSoNhanVienValue.setText(String.valueOf(tongNV));
        // String.valueOf(int) chuyển số thành chuỗi để gán cho JLabel (JLabel chỉ nhận String, không nhận int).
        lblTongSoNhanVienDelta.setText("Toàn bộ nhân viên đang quản lý");

        int soPhongBan = phongBanDAO.findAll().size();
        // Gọi findAll() lấy về CẢ DANH SÁCH rồi mới .size() đếm số phần tử — không tối ưu bằng 1 câu
        // COUNT(*) riêng, nhưng bảng phòng ban thường rất ít dòng nên không đáng lo hiệu năng.
        lblSoPhongBanValue.setText(String.valueOf(soPhongBan));
        lblSoPhongBanDelta.setText("Danh mục phòng ban hiện có");

        int nvMoi = nhanVienDAO.countNewThisMonth();
        lblNhanVienMoiValue.setText(String.valueOf(nvMoi));
        lblNhanVienMoiDelta.setText("Vào làm trong tháng này");

        loadPhongBanStats();
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Không tải được số liệu tổng quan: " + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        // Nếu BẤT KỲ lời gọi DAO nào ở trên ném SQLException, toàn bộ phần còn lại của try bị bỏ qua
        // (VD lỗi ngay từ countAll() thì soPhongBan/nvMoi cũng không kịp load) — chỉ hiện 1 thông báo lỗi.
    }
}
```

```java
private void loadPhongBanStats() throws SQLException {
    Map<String, Integer> soLuongTheoPhongBan = nhanVienDAO.countByPhongBan();
    // 🟡 NGOÀI CHƯƠNG TRÌNH — Map<String,Integer>: cấu trúc key–value, key = tên phòng ban,
    // value = số nhân viên. Khác Array/List chỉ lưu 1 dãy giá trị tuần tự không có "tên" đi kèm.

    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
    // BoxLayout.Y_AXIS: xếp các component con theo chiều DỌC, cái này dưới cái kia.

    for (Map.Entry<String, Integer> entry : soLuongTheoPhongBan.entrySet()) {
        // entrySet() trả về tập hợp các cặp (key, value) để duyệt bằng for-each — cách chuẩn để
        // vừa lấy key vừa lấy value cùng lúc khi lặp qua 1 Map (khác lặp List chỉ có value).
        JLabel row = new JLabel(entry.getKey() + " — " + entry.getValue() + " nhân viên");
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));   // khoảng đệm trên/dưới mỗi dòng
        listPanel.add(row);
    }

    chartPanel.removeAll();
    // Xóa hết component cũ trong chartPanel trước khi add mới — tránh bị lặp/chồng dữ liệu nếu
    // loadDashboardData() được gọi lại nhiều lần (VD sau khi thêm nhân viên).
    chartPanel.add(listPanel, BorderLayout.NORTH);
    chartPanel.revalidate();   // báo cho Swing tính lại layout vì nội dung con vừa thay đổi
    chartPanel.repaint();       // vẽ lại phần giao diện đó lên màn hình
}
```

```java
private void exportReport() {
}
// Nút "Tải báo cáo" gọi hàm này — THÂN HÀM RỖNG, tính năng chưa được cài đặt (TODO).
```

---

## 6. Luồng Quản lý nhân viên

File: [`view/panels/ListEmployeesPanel.java`](src/src/view/panels/ListEmployeesPanel.java), [`view/panels/AddEmployeesPanel.java`](src/src/view/panels/AddEmployeesPanel.java)

### 6.1 Điều phối chế độ xem bằng `switch`

```java
private void refreshCurrentView() {
    switch (viewMode) {
        // viewMode là 1 hằng số int: MODE_BROWSE(0) / MODE_KEYWORD_SEARCH(1) / MODE_ADVANCED_FILTER(2)
        case MODE_KEYWORD_SEARCH:
            loadSearchResults();
            break;                    // bắt buộc có break, thiếu sẽ "rơi" xuống case tiếp theo (fall-through)
        case MODE_ADVANCED_FILTER:
            loadFilterResults();
            break;
        default:
            loadBrowsePage();
            break;
    }
    // Mọi thao tác đổi trang / bấm nút tìm kiếm / lọc đều CHỈ đổi viewMode + currentPage rồi gọi lại
    // hàm này — tránh viết trùng lặp logic "load rồi hiện lên bảng" ở nhiều chỗ.
}
```

### 6.2 `loadBrowsePage()` — liệt kê + phân trang

```java
private void loadBrowsePage() {
    try {
        currentRows = nhanVienDAO.findPage(currentPage, PAGE_SIZE);
        // PAGE_SIZE = 10 (hằng số). findPage() trả về đúng 1 "trang" dữ liệu (tối đa 10 dòng).
        totalCount = nhanVienDAO.countAll();
        // Gọi THÊM 1 câu query riêng chỉ để đếm TỔNG số bản ghi (không phụ thuộc trang hiện tại) —
        // cần thiết để tính tổng số trang và bật/tắt nút Trang sau.
        TableModelUtil.fillEmployeeTable(tableModel, currentRows);
        updateResultCountLabel("nhân viên");
    } catch (SQLException e) {
        showDbError(e);
    }
}
```

```java
public List<NhanVien> findPage(int page, int pageSize) throws SQLException {
    String sql = SELECT_BASE + "ORDER BY nv.MaNV LIMIT ? OFFSET ?";
    // 🟡 NGOÀI CHƯƠNG TRÌNH — LIMIT ? OFFSET ?: chỉ lấy "pageSize" dòng, bỏ qua "offset" dòng đầu.
    // Đây là cú pháp phân trang của MySQL, KHÔNG lấy hết dữ liệu về rồi cắt bằng code Java
    // (cách đó sẽ rất chậm khi bảng có hàng chục nghìn dòng).
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, pageSize);
        ps.setInt(2, Math.max(0, (page - 1) * pageSize));
        // Trang 1 -> offset 0; trang 2 -> offset = pageSize (bỏ qua 10 dòng đầu); v.v.
        // Math.max(0, ...) đề phòng page truyền vào là số âm/0 (lỗi logic ở tầng gọi) thì offset vẫn không âm.
        return mapAll(ps);
        // mapAll(): chạy executeQuery() rồi duyệt ResultSet, gọi mapRow() cho từng dòng, gom vào List<NhanVien>.
    }
}
```

### 6.3 Tìm kiếm nhanh

```java
private void performQuickSearch() {
    String keyword = tìmKiếmNhanhTextField.getText().trim();
    if (keyword.isEmpty()) {
        exitSearchMode();          // gõ rồi xóa hết chữ -> tự động quay lại chế độ duyệt thường
    } else {
        searchByKeyword(keyword);
    }
}

private void searchByKeyword(String keyword) {
    this.currentKeyword = keyword;         // lưu lại từ khóa để dùng cho lần load lại / đổi trang sau này
    this.viewMode = MODE_KEYWORD_SEARCH;
    this.currentPage = 1;                   // luôn quay về trang 1 khi bắt đầu 1 lượt tìm kiếm mới
    applySearchModeUI();                     // đổi tiêu đề "Kết quả tìm kiếm", ẩn nút Thêm nhân viên
    refreshCurrentView();                    // -> gọi loadSearchResults() (do viewMode vừa đổi)
}
```

```java
public List<NhanVien> search(String keyword, int page, int pageSize) throws SQLException {
    String sql = SELECT_BASE +
            "WHERE nv.HoTen LIKE ? OR nv.SoDienThoai LIKE ? OR nv.Email LIKE ? OR nv.MaNV LIKE ? " +
            "ORDER BY nv.MaNV LIMIT ? OFFSET ?";
    // 4 điều kiện LIKE nối bằng OR: khớp 1 trong 4 cột (Họ tên/SĐT/Email/Mã NV) là đủ.
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        String like = "%" + keyword + "%";
        // "%" là ký tự đại diện của LIKE trong SQL — khớp bất kỳ chuỗi nào đứng trước/sau "keyword",
        // tức tìm GẦN ĐÚNG (chứa từ khóa ở bất kỳ vị trí nào), không cần khớp tuyệt đối.
        ps.setString(1, like);
        ps.setString(2, like);
        ps.setString(3, like);
        ps.setString(4, like);
        // Bind CÙNG 1 giá trị "like" vào cả 4 dấu "?" vì cả 4 cột đều tìm theo cùng 1 từ khóa.
        ps.setInt(5, pageSize);
        ps.setInt(6, Math.max(0, (page - 1) * pageSize));
        return mapAll(ps);
    }
}
```

### 6.4 Lọc nâng cao — xây SQL động

```java
private void loadFilterResults() {
    Object pbSel = phòngBanComboBox.getSelectedItem();
    String maPB = pbSel instanceof PhongBan ? ((PhongBan) pbSel).getMaPB() : null;
    // getSelectedItem() trả về kiểu Object (JComboBox không rành mạch kiểu dữ liệu bên trong).
    // "pbSel instanceof PhongBan": kiểm tra xem item đang chọn có PHẢI là 1 object PhongBan thật không
    // (item đầu combobox là chuỗi "Tất cả phòng ban" -> instanceof trả false -> maPB = null nghĩa là
    // "không lọc theo phòng ban").

    Object cvSel = chứcVụComboBox.getSelectedItem();
    String maCV = cvSel instanceof ChucVu ? ((ChucVu) cvSel).getMaCV() : null;

    String trangThai = trangThaiDisplayToCode((String) trạngTháiComboBox.getSelectedItem());
    // Combobox hiện chữ tiếng Việt có dấu ("Đang làm việc") nhưng DB lưu mã không dấu ("DangLamViec")
    // -> cần hàm chuyển đổi riêng (xem bên dưới).

    String gioiTinh = namRadioButton.isSelected() ? "Nam"
            : nữRadioButton.isSelected() ? "Nữ"
            : khácRadioButton.isSelected() ? "Khác"
            : null;
    // Toán tử 3 ngôi lồng nhau: kiểm tra lần lượt từng radio, radio nào đang chọn thì lấy giá trị đó;
    // nếu là radio "Tất cả" (không khớp cái nào ở trên) thì gioiTinh = null (không lọc giới tính).

    try {
        currentRows = nhanVienDAO.advancedSearch(maPB, maCV, trangThai, gioiTinh, currentKeyword, currentPage, PAGE_SIZE);
        totalCount = nhanVienDAO.countAdvancedSearch(maPB, maCV, trangThai, gioiTinh, currentKeyword);
        TableModelUtil.fillEmployeeTable(tableModel, currentRows);
        updateResultCountLabel("kết quả");
    } catch (SQLException e) {
        showDbError(e);
    }
}
```

```java
private String trangThaiDisplayToCode(String display) {
    if (display == null) {
        return null;
    }
    switch (display) {
        case "Đang làm việc":
            return "DangLamViec";
        case "Nghỉ việc":
            return "NghiViec";
        case "Tạm nghỉ":
            return "TamNghi";
        default:
            return null;
        // default rơi vào khi combobox đang chọn "Tất cả trạng thái" (không khớp 3 case trên)
        // -> trả null nghĩa là không lọc theo trạng thái.
    }
}
```

```java
public List<NhanVien> advancedSearch(String maPB, String maCV, String trangThai, String gioiTinh,
                                      String keyword, int page, int pageSize) throws SQLException {
    StringBuilder sql = new StringBuilder(SELECT_BASE);
    // 🟡 NGOÀI CHƯƠNG TRÌNH — StringBuilder: nối chuỗi SQL hiệu quả hơn dùng "+" nhiều lần trong vòng lặp,
    // và quan trọng hơn: cho phép GHÉP CÓ ĐIỀU KIỆN — chỉ thêm phần "WHERE ..." nếu người dùng có chọn lọc.
    List<Object> params = new ArrayList<>();
    // Danh sách tham số sẽ bind vào PreparedStatement — PHẢI đúng thứ tự với các dấu "?" sẽ xuất hiện
    // trong sql, nên được build song song với sql bên trong appendAdvancedFilters().

    appendAdvancedFilters(sql, params, maPB, maCV, trangThai, gioiTinh, keyword);
    sql.append("ORDER BY nv.MaNV LIMIT ? OFFSET ?");
    params.add(pageSize);
    params.add(Math.max(0, (page - 1) * pageSize));
    // Thêm 2 tham số phân trang vào SAU CÙNG, đúng thứ tự với 2 dấu "?" cuối trong câu SQL.

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql.toString())) {
        bindParams(ps, params);
        return mapAll(ps);
    }
}
```

```java
private void appendAdvancedFilters(StringBuilder sql, List<Object> params, String maPB, String maCV,
                                    String trangThai, String gioiTinh, String keyword) {
    List<String> conditions = new ArrayList<>();
    // Danh sách các mẩu điều kiện SQL sẽ nối bằng "AND" — CHỈ chứa những điều kiện người dùng thật sự chọn.

    if (maPB != null) {
        conditions.add("nv.MaPB = ?");
        params.add(maPB);
        // Thêm mẩu điều kiện VÀ tham số tương ứng CÙNG LÚC -> giữ đúng thứ tự khớp nhau.
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
        // 1 điều kiện keyword nhưng có tới 3 dấu "?" bên trong -> phải add() params đúng 3 lần.
    }

    if (!conditions.isEmpty()) {
        sql.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        // String.join(" AND ", conditions): nối các mẩu điều kiện lại bằng " AND ", VD
        // ["nv.MaPB = ?", "nv.GioiTinh = ?"] -> "nv.MaPB = ? AND nv.GioiTinh = ?"
        // Nếu không chọn lọc gì cả (conditions rỗng) -> KHÔNG thêm mệnh đề WHERE nào (lấy hết dữ liệu).
    }
}
```

```java
private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
    for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));
        // ps.setObject() nhận Object bất kỳ (String, Integer...) thay vì phải gọi đúng setString/setInt
        // theo từng kiểu — cần thiết ở đây vì danh sách params trộn lẫn nhiều kiểu dữ liệu khác nhau.
        // "i + 1" vì tham số PreparedStatement đánh số bắt đầu từ 1, còn index mảng/List bắt đầu từ 0.
    }
}
```

### 6.5 Thêm nhân viên — validate rồi lưu

```java
lưuHồSơButton.addActionListener(e -> {
    if (validateForm() && saveEmployee()) {
        // Toán tử && có "short-circuit": nếu validateForm() trả false, saveEmployee() KHÔNG được gọi
        // (không lãng phí 1 lượt gọi DB khi dữ liệu chưa hợp lệ).
        resetToAddMode();
        if (onSavedOrCancelled != null) {
            onSavedOrCancelled.run();
            // Báo cho MainFrame biết "đã lưu xong" -> MainFrame cho danh sách reload + quay về card danh sách.
        }
    }
});
```

```java
private boolean validateForm() {
    if (!ValidationUtil.isNotBlank(họVàTênTextField.getText())) {
        JOptionPane.showMessageDialog(this, "Vui lòng nhập họ và tên.");
        họVàTênTextField.requestFocus();
        return false;
        // Dừng NGAY ở lỗi đầu tiên gặp phải — không kiểm tra tiếp các ô còn lại trong cùng 1 lần bấm Lưu.
    }

    LocalDate ngaySinh;
    try {
        ngaySinh = DateUtil.parse(ngàySinhTextField.getText());
        // DateUtil.parse() ném DateTimeParseException (con của Exception) nếu chuỗi không đúng "dd/MM/yyyy"
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Ngày sinh không đúng định dạng dd/mm/yyyy.");
        ngàySinhTextField.requestFocus();
        return false;
    }
    if (ngaySinh != null && ngaySinh.isAfter(LocalDate.now())) {
        // LocalDate.now(): ngày hiện tại của máy đang chạy app. isAfter(): so sánh 2 mốc ngày.
        JOptionPane.showMessageDialog(this, "Ngày sinh không được ở tương lai.");
        ngàySinhTextField.requestFocus();
        return false;
    }

    String cccd = sốCCCDTextField.getText().trim();
    if (!cccd.isEmpty() && !ValidationUtil.isValidCCCD(cccd)) {
        // CCCD là trường TÙY CHỌN: chỉ validate định dạng KHI người dùng có nhập (cccd.isEmpty() == false).
        JOptionPane.showMessageDialog(this, "Số CCCD/CMND không hợp lệ (9 hoặc 12 chữ số).");
        sốCCCDTextField.requestFocus();
        return false;
    }

    if (!ValidationUtil.isValidPhone(sốĐiệnThoạiTextField.getText())) {
        // Khác CCCD, số điện thoại là BẮT BUỘC (isValidPhone() tự trả false nếu chuỗi rỗng).
        JOptionPane.showMessageDialog(this, "Số điện thoại không hợp lệ.");
        sốĐiệnThoạiTextField.requestFocus();
        return false;
    }

    if (!ValidationUtil.isValidEmail(emailTextField.getText())) {
        JOptionPane.showMessageDialog(this, "Email không hợp lệ.");
        emailTextField.requestFocus();
        return false;
    }

    try {
        DateUtil.parse(ngàyVàoLàmTextField.getText());
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Ngày vào làm không đúng định dạng dd/mm/yyyy.");
        ngàyVàoLàmTextField.requestFocus();
        return false;
    }

    String luong = mứcLươngCơBảnTextField.getText().trim();
    if (!luong.isEmpty()) {
        try {
            if (new BigDecimal(luong).signum() < 0) {
                // 🟡 NGOÀI CHƯƠNG TRÌNH — BigDecimal thay vì double: tránh sai số làm tròn khi tính
                // tiền (double lưu số thực dấu phẩy động, có thể lệch VD 0.1 + 0.2 != 0.3).
                // signum() trả -1/0/1 tùy số âm/bằng 0/dương — signum() < 0 nghĩa là số âm.
                JOptionPane.showMessageDialog(this, "Lương cơ bản không được âm.");
                mứcLươngCơBảnTextField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            // new BigDecimal("abc") ném NumberFormatException nếu chuỗi không phải số hợp lệ.
            JOptionPane.showMessageDialog(this, "Lương cơ bản phải là số.");
            mứcLươngCơBảnTextField.requestFocus();
            return false;
        }
    }
    // Lương KHÔNG bắt buộc nhập -> nếu rỗng thì bỏ qua toàn bộ khối kiểm tra ở trên, coi là hợp lệ.

    return true;
    // Chỉ tới được đây khi TẤT CẢ các bước kiểm tra bên trên đều đã "return false" thất bại — nghĩa là form hợp lệ.
}
```

```java
private boolean saveEmployee() {
    NhanVien nv = buildEmployeeFromForm();
    // Gom toàn bộ giá trị đang hiện trên form thành 1 object NhanVien (Model) — xem hàm bên dưới.
    try {
        if (editingEmployee == null) {
            // editingEmployee được set null trong resetToAddMode() (mục 6.6), set khác null trong
            // loadForEdit() (mục 6.7) -> biến này chính là "cờ" phân biệt đang THÊM hay đang SỬA.
            String newId = nhanVienDAO.insert(nv);
            JOptionPane.showMessageDialog(this, "Đã thêm nhân viên mới (Mã NV: " + newId + ").");
        } else {
            nv.setMaNV(editingEmployee.getMaNV());
            // Object nv mới build từ buildEmployeeFromForm() KHÔNG có Mã NV (ô mã NV bị khóa không cho sửa)
            // -> phải gán lại đúng mã NV cũ trước khi UPDATE, nếu không WHERE MaNV=? sẽ khớp với null.
            nhanVienDAO.update(nv);
            ActivityLogger.logNhanVienAction("SỬA", nv);
            JOptionPane.showMessageDialog(this, "Đã cập nhật hồ sơ nhân viên.");
        }
        return true;
    } catch (SQLIntegrityConstraintViolationException e) {
        // 🟡 NGOÀI CHƯƠNG TRÌNH — SQLIntegrityConstraintViolationException LÀ CON của SQLException,
        // ném ra riêng khi vi phạm ràng buộc UNIQUE/FOREIGN KEY... trong DB (ở đây là CCCD/Email trùng).
        // Bắt riêng case này TRƯỚC catch (SQLException e) chung để hiện thông báo CHÍNH XÁC nguyên nhân,
        // thay vì hiện thông báo lỗi SQL kỹ thuật khó hiểu với người dùng cuối.
        JOptionPane.showMessageDialog(this, "Số CCCD hoặc Email đã tồn tại trong hệ thống.",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        return false;
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Không thể lưu hồ sơ: " + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        return false;
    }
}
```

### 6.6 `NhanVienDAO.insert()` — tự sinh mã nhân viên

```java
public String insert(NhanVien nv) throws SQLException {
    String maNV = generateNextMaNV(nv.getMaPB());
    // Sinh mã TRƯỚC khi insert — vì mã NV là khóa chính, phải có sẵn giá trị mới ghép được câu INSERT.
    nv.setMaNV(maNV);
    String sql = "INSERT INTO NHANVIEN (MaNV, HoTen, NgaySinh, GioiTinh, SoCCCD, SoDienThoai, Email, DiaChi, " +
            "AvatarPath, MaPB, MaCV, NgayVaoLam, LoaiHopDong, MucLuongCoBan, TrangThai) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    // 15 cột -> 15 dấu "?" -> phải bind ĐỦ và ĐÚNG THỨ TỰ 15 giá trị bên dưới.
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, maNV);
        bindEmployeeFields(ps, nv, 2);
        // bindEmployeeFields() bind 14 field còn lại, bắt đầu từ vị trí "?" số 2 (vì vị trí 1 vừa
        // set thủ công là maNV — hàm này dùng chung cho cả insert() lẫn update()).
        ps.executeUpdate();
    }
    return maNV;
    // Trả mã NV vừa sinh về cho View, để hiện lên thông báo "Đã thêm nhân viên mới (Mã NV: IT004)".
}
```

```java
private String generateNextMaNV(String maPB) throws SQLException {
    String prefix = (maPB == null || maPB.trim().isEmpty()) ? "NV" : maPB.trim();
    // Chưa chọn phòng ban -> dùng tiền tố mặc định "NV"; có chọn -> dùng chính mã phòng ban làm tiền tố.
    String sql = "SELECT MaNV FROM NHANVIEN WHERE MaNV LIKE ?";
    int max = 0;
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, prefix + "%");
        // VD prefix = "IT" -> tìm mọi mã bắt đầu bằng "IT" (IT001, IT002...) — dấu % không escape vì
        // đây là control character của LIKE, KHÔNG phải input người dùng gõ trực tiếp.
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String suffix = rs.getString(1).substring(prefix.length());
                // getString(1): lấy cột đầu tiên trong SELECT theo SỐ THỨ TỰ (khác cách lấy theo tên cột
                // ở mapRow() — cả 2 cách đều hợp lệ trong JDBC).
                // substring(prefix.length()): cắt bỏ phần tiền tố, chỉ giữ lại phần số phía sau.
                // VD "IT003".substring(2) = "003".
                try {
                    max = Math.max(max, Integer.parseInt(suffix));
                    // Tìm số lớn nhất trong tất cả các mã đã có cùng tiền tố.
                } catch (NumberFormatException ignored) {
                    // Nếu phần "suffix" lỡ không phải toàn số (dữ liệu bất thường) thì bỏ qua dòng đó,
                    // KHÔNG làm crash cả vòng lặp — biến "ignored" không dùng tới, chỉ đặt tên cho rõ ý.
                }
            }
        }
    }
    return prefix + String.format("%03d", max + 1);
    // String.format("%03d", n): định dạng số nguyên thành chuỗi ĐỦ 3 CHỮ SỐ, thêm số 0 ở đầu nếu thiếu.
    // VD max=3 -> "%03d" của 4 -> "004" -> kết quả "IT004".
}
```

### 6.7 Sửa & xóa nhân viên

```java
private void handleEdit(NhanVien nv) {
    if (onEditEmployeeRequested != null) {
        onEditEmployeeRequested.accept(nv);
        // Consumer<NhanVien>.accept(nv): "gọi" lambda đã được MainFrame gán vào (mục 3.2) với tham số nv.
    }
}
```

```java
public void loadForEdit(NhanVien nv) {
    populateCombos();                              // nạp lại danh sách Phòng ban/Chức vụ mới nhất
    this.editingEmployee = nv;                      // đánh dấu đang ở chế độ SỬA (dùng ở saveEmployee(), mục 6.5)
    lblFormTitle.setText("Sửa thông tin nhân viên");
    lưuHồSơButton.setText("Cập nhật hồ sơ");
    mãNhânViênTextField.setText(nv.getMaNV());

    họVàTênTextField.setText(nv.getHoTen() == null ? "" : nv.getHoTen());
    ngàySinhTextField.setText(DateUtil.format(nv.getNgaySinh()));
    selectGender(nv.getGioiTinh());
    sốCCCDTextField.setText(nv.getSoCCCD() == null ? "" : nv.getSoCCCD());
    sốĐiệnThoạiTextField.setText(nv.getSoDienThoai() == null ? "" : nv.getSoDienThoai());
    emailTextField.setText(nv.getEmail() == null ? "" : nv.getEmail());
    địaChỉTextArea.setText(nv.getDiaChi() == null ? "" : nv.getDiaChi());
    selectedAvatarPath = nv.getAvatarPath();
    showAvatarPreview(selectedAvatarPath);
    // Từng dòng ở trên đọc 1 field của object Model "nv" rồi đổ ngược lên đúng ô input tương ứng —
    // ngược chiều hoàn toàn với buildEmployeeFromForm() (đọc form -> tạo Model) ở mục 6.5.

    selectPhongBanById(nv.getMaPB());
    selectChucVuById(nv.getMaCV());
    ngàyVàoLàmTextField.setText(DateUtil.format(nv.getNgayVaoLam()));
    selectLoaiHopDong(nv.getLoaiHopDong());
    mứcLươngCơBảnTextField.setText(nv.getMucLuongCoBan() == null ? "" : nv.getMucLuongCoBan().toPlainString());
    // toPlainString(): hiện BigDecimal dạng số thường (VD "5000000"), không dùng ký hiệu khoa học.
}
```

```java
private void selectPhongBanById(String maPB) {
    if (maPB != null) {
        for (int i = 0; i < phòngBanComboBox.getItemCount(); i++) {
            Object item = phòngBanComboBox.getItemAt(i);
            if (item instanceof PhongBan && maPB.equals(((PhongBan) item).getMaPB())) {
                // Combobox chứa OBJECT PhongBan (không phải String) -> muốn "chọn đúng dòng theo mã"
                // phải duyệt từng item, ép kiểu về PhongBan rồi so sánh getMaPB() với mã đang cần tìm.
                phòngBanComboBox.setSelectedIndex(i);
                return;      // tìm thấy thì dừng luôn, không cần duyệt tiếp
            }
        }
    }
    phòngBanComboBox.setSelectedIndex(0);
    // Không tìm thấy (hoặc maPB null, VD nhân viên chưa gán phòng ban) -> chọn item đầu ("-- Không chọn --")
}
```

```java
private void handleDelete(NhanVien nv) {
    int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc muốn xóa nhân viên \"" + nv.getHoTen() + "\"?",
            "Xóa nhân viên", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (confirm != JOptionPane.YES_OPTION) {
        return;                 // người dùng bấm No / đóng hộp thoại -> hủy thao tác, không làm gì thêm
    }
    try {
        nhanVienDAO.delete(nv.getMaNV());
        ActivityLogger.logNhanVienAction("XÓA", nv);
        refreshCurrentView();     // load lại đúng chế độ xem hiện tại (duyệt/tìm kiếm/lọc) để dòng vừa xóa biến mất
    } catch (SQLException e) {
        showDbError(e);
    }
}
```

### 6.8 Nút Sửa/Xóa nhúng ngay trong ô bảng

```java
private class ActionCellEditor extends AbstractCellEditor implements TableCellEditor {
    // 🟡 NGOÀI CHƯƠNG TRÌNH — AbstractCellEditor là abstract class CÓ SẴN của thư viện Swing
    // (javax.swing.AbstractCellEditor), lo sẵn phần quản lý sự kiện "đang chỉnh sửa ô nào".
    // Kế thừa nó + implements TableCellEditor để tự định nghĩa GIAO DIỆN hiện trong ô khi "chỉnh sửa".
    // Đây là INNER CLASS (không static) -> có thể truy cập trực tiếp field "currentRows" của
    // ListEmployeesPanel (lớp bao ngoài) mà không cần truyền tham số vào.

    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
    private int editingRow = -1;
    // Biến nhớ TẠM đang thao tác trên dòng thứ mấy của bảng — vì cả bảng chỉ dùng CHUNG 1 panel nút
    // (panel này được tái sử dụng cho mọi dòng, không tạo mới cho từng dòng).

    ActionCellEditor() {
        JButton editButton = new JButton("Sửa");
        JButton deleteButton = new JButton("Xóa");
        editButton.addActionListener(e -> {
            int row = editingRow;
            fireEditingStopped();
            // Báo cho JTable biết "đã chỉnh sửa xong ô này" — bắt buộc gọi TRƯỚC khi làm hành động khác,
            // nếu không JTable có thể giữ ô ở trạng thái "đang edit" gây lỗi hiển thị.
            if (row >= 0 && row < currentRows.size()) {
                handleEdit(currentRows.get(row));
                // currentRows.get(row): lấy đúng object NhanVien tương ứng với dòng bảng vừa bấm nút.
            }
        });
        deleteButton.addActionListener(e -> {
            int row = editingRow;
            fireEditingStopped();
            if (row >= 0 && row < currentRows.size()) {
                handleDelete(currentRows.get(row));
            }
        });
        panel.add(editButton);
        panel.add(deleteButton);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        editingRow = row;
        // Swing gọi hàm này MỖI LẦN người dùng click vào ô cột "Thao tác" của 1 dòng -> ghi nhớ lại
        // dòng đó vào editingRow để 2 ActionListener ở trên biết đang thao tác trên dòng nào.
        return panel;
        // Trả về panel chứa 2 nút để Swing "chèn" panel này đè lên đúng vị trí ô đang bấm.
    }

    @Override
    public Object getCellEditorValue() {
        return null;
        // Cột này không cần LƯU GIÁ TRỊ gì cả (khác các cột text bình thường), chỉ dùng để hiện nút bấm.
    }
}
```

### 6.9 Upload ảnh đại diện

```java
private void uploadAvatar() {
    JFileChooser chooser = new JFileChooser();
    // 🟡 NGOÀI CHƯƠNG TRÌNH — JFileChooser: hộp thoại "chọn file" tích hợp sẵn của hệ điều hành,
    // Swing cung cấp component gọi ra hộp thoại đó.
    chooser.setFileFilter(new FileNameExtensionFilter("Ảnh (JPEG, PNG)", "jpg", "jpeg", "png"));
    // Giới hạn người dùng chỉ thấy/chọn được các file có đuôi jpg/jpeg/png trong hộp thoại.
    if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
        return;      // người dùng bấm Cancel hoặc đóng hộp thoại -> dừng, không làm gì thêm
    }
    File source = chooser.getSelectedFile();
    try {
        File avatarDir = new File(AVATAR_DIR);
        if (!avatarDir.exists() && !avatarDir.mkdirs()) {
            // Nếu thư mục "avatars/" chưa tồn tại VÀ mkdirs() (tạo thư mục) thất bại -> ném lỗi thủ công.
            throw new IOException("Khong tao duoc thu muc " + avatarDir.getAbsolutePath());
        }
        String ext = getFileExtension(source.getName());
        String fileName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        // 🟡 NGOÀI CHƯƠNG TRÌNH — UUID.randomUUID(): sinh 1 chuỗi định danh gần như không thể trùng
        // (Universally Unique Identifier) làm tên file mới, tránh việc 2 người cùng upload file tên
        // "anh.jpg" thì file người sau sẽ GHI ĐÈ file người trước.
        File dest = new File(avatarDir, fileName);
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        // 🟡 NGOÀI CHƯƠNG TRÌNH — java.nio.file.Files.copy(): API copy file hiện đại (NIO.2, Java 7+),
        // khác cách đọc/ghi byte thủ công bằng FileInputStream/FileOutputStream đã học.

        selectedAvatarPath = AVATAR_DIR + "/" + fileName;
        // CHỈ lưu đường dẫn TƯƠNG ĐỐI (VD "avatars/xxxx.jpg"), không lưu đường dẫn tuyệt đối của máy
        // -> nếu đem cả thư mục project sang máy khác, ảnh vẫn hiện đúng.
        showAvatarPreview(selectedAvatarPath);
    } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Không sao chép được ảnh: " + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
```

---

## 7. Luồng Danh mục (Phòng ban / Chức vụ)

File: [`view/panels/CategoryManagementPanel.java`](src/src/view/panels/CategoryManagementPanel.java)

### 7.1 1 class dùng chung cho 2 tab nhờ interface

```java
public CategoryManagementPanel() {
    setLayout(new BorderLayout());
    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Phòng ban", new CategoryTab(new PhongBanDAO(), "phòng ban"));
    tabs.addTab("Chức vụ", new CategoryTab(new ChucVuDAO(), "chức vụ"));
    // 🟡 NGOÀI CHƯƠNG TRÌNH — CategoryTab nhận vào tham số kiểu DanhMucDAO (interface), truyền vào
    // PhongBanDAO hoặc ChucVuDAO tùy tab -> đây là ĐA HÌNH (polymorphism): CÙNG 1 đoạn code CategoryTab
    // hoạt động đúng với 2 loại dữ liệu khác nhau, không cần viết 2 class riêng cho 2 tab.
    add(tabs, BorderLayout.CENTER);
}
```

```java
public interface DanhMucDAO {
    List<? extends DanhMuc> findAll() throws SQLException;
    // 🟡 NGOÀI CHƯƠNG TRÌNH — "List<? extends DanhMuc>": wildcard generic, nghĩa là hàm này có thể trả về
    // List<PhongBan> HOẶC List<ChucVu> (miễn là PhongBan/ChucVu implement DanhMuc) — nơi gọi hàm chỉ
    // được PHÉP ĐỌC (không được add phần tử vào list trả về, vì Java không biết chắc kiểu cụ thể bên trong).
    void insert(String ma, String ten, String moTa) throws SQLException;
    void update(String id, String ten, String moTa) throws SQLException;
    void delete(String id) throws SQLException;
    int countUsage(String id) throws SQLException;
}
```

### 7.2 `reload()` — nạp dữ liệu qua interface

```java
private void reload() {
    try {
        currentRows = dao.findAll();
        // "dao" ở đây có kiểu khai báo là DanhMucDAO (interface), nhưng lúc chạy thật (runtime) sẽ
        // gọi ĐÚNG PhongBanDAO.findAll() hoặc ChucVuDAO.findAll() tùy vào object nào được truyền vào
        // constructor CategoryTab — đây gọi là "dynamic dispatch" (đa hình lúc chạy).
        tableModel.setRowCount(0);
        for (DanhMuc dm : currentRows) {
            // Kiểu vòng lặp là DanhMuc (interface) -> vòng lặp CHẠY ĐƯỢC dù currentRows thực chất là
            // List<PhongBan> hay List<ChucVu>, vì cả 2 đều implement DanhMuc.
            tableModel.addRow(new Object[]{dm.getId(), dm.getTen(), dm.getMoTa() == null ? "" : dm.getMoTa(), null});
            // dm.getId()/getTen()/getMoTa() gọi qua interface -> PhongBan trả về MaPB/TenPhongBan/MoTa,
            // ChucVu trả về MaCV/TenChucVu/MoTa — code ở đây KHÔNG CẦN BIẾT đang xử lý loại nào.
        }
        updateTableHeight();
    } catch (SQLException e) {
        showError("Không tải được danh sách: " + e.getMessage());
    }
}
```

### 7.3 Thêm/Sửa danh mục

```java
private void openEditDialog(DanhMuc editing) {
    JTextField maField = new JTextField(editing == null ? "" : editing.getId());
    JTextField tenField = new JTextField(editing == null ? "" : editing.getTen());
    JTextField moTaField = new JTextField(editing == null || editing.getMoTa() == null ? "" : editing.getMoTa());
    // editing == null -> đang THÊM MỚI (form trắng); editing != null -> đang SỬA (đổ sẵn dữ liệu cũ).

    maField.setEditable(editing == null);
    // Chỉ cho sửa "Mã" khi đang THÊM MỚI — khi SỬA thì khóa ô Mã lại (Mã là khóa chính, không nên đổi).

    JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
    // GridLayout(0, 1, ...): 0 hàng (tự động theo số component), 1 cột -> các component xếp CHỒNG DỌC.
    form.add(new JLabel("Mã " + nounLower + " (tối đa 5 chữ cái in hoa):"));
    form.add(maField);
    form.add(new JLabel("Tên " + nounLower + ":"));
    form.add(tenField);
    form.add(new JLabel("Mô tả:"));
    form.add(moTaField);

    String title = editing == null ? "Thêm " + nounLower : "Sửa " + nounLower;
    int result = JOptionPane.showConfirmDialog(this, form, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    // Nhét cả 1 JPanel (form) làm "nội dung" của hộp thoại JOptionPane — cách tạo form nhỏ gọn
    // mà không cần tự tạo hẳn 1 class JDialog riêng.
    if (result != JOptionPane.OK_OPTION) {
        return;      // bấm Cancel hoặc đóng hộp thoại -> hủy thao tác
    }

    String ten = tenField.getText().trim();
    String moTa = moTaField.getText().trim();
    if (ten.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Vui lòng nhập tên " + nounLower + ".");
        return;
    }

    try {
        if (editing == null) {
            String ma = maField.getText().trim().toUpperCase();
            // .toUpperCase() tự động chuyển chữ thường thành in hoa -> người dùng gõ "it" vẫn thành "IT".
            if (!MA_PATTERN.matcher(ma).matches()) {
                // MA_PATTERN = Pattern.compile("^[A-Z]{1,5}$") — chỉ chấp nhận 1-5 chữ CÁI IN HOA, không số/ký tự đặc biệt.
                JOptionPane.showMessageDialog(this, "Mã " + nounLower + " phải là chữ in hoa (A-Z), tối đa 5 ký tự.");
                return;
            }
            dao.insert(ma, ten, moTa.isEmpty() ? null : moTa);
            // moTa.isEmpty() ? null : moTa -> nếu người dùng không nhập mô tả, lưu NULL vào DB
            // thay vì lưu chuỗi rỗng "" (phân biệt rõ "chưa có mô tả" và "mô tả rỗng").
        } else {
            dao.update(editing.getId(), ten, moTa.isEmpty() ? null : moTa);
        }
        reload();
    } catch (SQLIntegrityConstraintViolationException e) {
        String msg = editing == null
                ? "Mã hoặc tên " + nounLower + " này đã tồn tại."
                : "Tên " + nounLower + " này đã tồn tại.";
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    } catch (SQLException e) {
        showError("Không lưu được: " + e.getMessage());
    }
}
```

### 7.4 Xóa danh mục — cảnh báo nếu đang được sử dụng

```java
private void deleteEntry(DanhMuc selected) {
    try {
        int usage = dao.countUsage(selected.getId());
        // Đếm xem còn bao nhiêu nhân viên đang gán MaPB/MaCV = mã này -> gọi ChucVuDAO.countUsage()
        // hoặc PhongBanDAO.countUsage() tùy đang ở tab nào (đa hình).
        String msg = usage > 0
                ? "Có " + usage + " nhân viên đang thuộc " + nounLower + " \"" + selected.getTen()
                + "\". Xóa sẽ bỏ trống " + nounLower + " của họ. Vẫn xóa?"
                : "Xóa " + nounLower + " \"" + selected.getTen() + "\"?";
        int confirm = JOptionPane.showConfirmDialog(this, msg, "Xóa " + nounLower,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        dao.delete(selected.getId());
        // Nhờ ràng buộc "ON DELETE SET NULL" khai báo sẵn trong DB, xóa dòng PHONGBAN/CHUCVU này
        // sẽ tự động khiến các nhân viên liên quan có MaPB/MaCV = NULL, KHÔNG bị lỗi khóa ngoại,
        // và KHÔNG bị xóa dây chuyền.
        reload();
    } catch (SQLException e) {
        showError("Không xóa được: " + e.getMessage());
    }
}
```

---

## 8. Luồng Quản lý tài khoản (Phân quyền)

File: [`view/panels/AccountManagementPanel.java`](src/src/view/panels/AccountManagementPanel.java)

### 8.1 Tạo tài khoản mới

```java
private void openCreateAccountDialog() {
    JTextField usernameField = new JTextField();
    JPasswordField passwordField = new JPasswordField();
    JPasswordField confirmField = new JPasswordField();
    // JPasswordField: giống JTextField nhưng HIỂN THỊ dấu chấm/sao thay vì ký tự thật khi gõ.
    JComboBox<String> roleCombo = new JComboBox<>(new String[]{"NhanVien", "Admin"});

    JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
    form.add(new JLabel("Tên đăng nhập:"));
    form.add(usernameField);
    form.add(new JLabel("Mật khẩu (tối thiểu 6 ký tự):"));
    form.add(passwordField);
    form.add(new JLabel("Xác nhận mật khẩu:"));
    form.add(confirmField);
    form.add(new JLabel("Vai trò:"));
    form.add(roleCombo);

    int result = JOptionPane.showConfirmDialog(this, form, "Tạo tài khoản mới",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
        return;
    }

    String username = usernameField.getText().trim();
    String password = new String(passwordField.getPassword());
    String confirm = new String(confirmField.getPassword());

    if (username.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập.");
        return;
    }
    if (!ValidationUtil.isValidEmail(username)) {
        JOptionPane.showMessageDialog(this, "Tên đăng nhập phải là email hợp lệ (vd: ten@congty.com) - " +
                "vì màn Đăng nhập yêu cầu đúng định dạng này, đặt tên khác sẽ không đăng nhập được.");
        // Ràng buộc này TỒN TẠI vì Login.java (mục 2.2) bắt buộc username phải khớp EMAIL_PATTERN —
        // nếu tạo tài khoản username không phải email, người đó sẽ KHÔNG BAO GIỜ đăng nhập được.
        return;
    }
    if (password.length() < 6) {
        JOptionPane.showMessageDialog(this, "Mật khẩu phải có ít nhất 6 ký tự.");
        return;
    }
    if (!password.equals(confirm)) {
        JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp.");
        return;
    }

    try {
        if (taiKhoanDAO.existsUsername(username)) {
            JOptionPane.showMessageDialog(this, "Tên đăng nhập đã tồn tại.");
            return;
            // Kiểm tra trùng THỦ CÔNG trước, dù DB cũng có UNIQUE constraint chặn -> mục đích là hiện
            // thông báo RÕ RÀNG ngay, thay vì đợi tới lúc insert() ném SQLIntegrityConstraintViolationException.
        }
        String vaiTro = (String) roleCombo.getSelectedItem();

        taiKhoanDAO.create(username, password, vaiTro);
        JOptionPane.showMessageDialog(this, "Đã tạo tài khoản thành công.");
        reloadData();
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Không tạo được tài khoản: " + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
```

```java
public void create(String tenDangNhap, String matKhauGoc, String vaiTro) throws SQLException {
    String hash = BCrypt.hashpw(matKhauGoc, BCrypt.gensalt(12));
    // 🟡 NGOÀI CHƯƠNG TRÌNH:
    //   BCrypt.gensalt(12): sinh 1 chuỗi "salt" ngẫu nhiên, số 12 là "cost factor" (độ khó tính toán,
    //   càng cao càng an toàn nhưng càng chậm) — làm mỗi lần băm ra 1 kết quả KHÁC NHAU dù cùng 1 mật khẩu.
    //   BCrypt.hashpw(mật khẩu gốc, salt): trộn mật khẩu với salt rồi băm nhiều vòng, trả về 1 chuỗi
    //   dạng "$2a$12$..." — ĐÂY MỚI LÀ THỨ được lưu vào cột MatKhauHash, KHÔNG BAO GIỜ lưu matKhauGoc.
    String sql = "INSERT INTO TAIKHOAN (TenDangNhap, MatKhauHash, VaiTro, MaNV, TrangThai) VALUES (?,?,?,NULL,'HoatDong')";
    // MaNV luôn NULL khi tạo qua màn này -> tài khoản mới CHƯA gắn với hồ sơ nhân viên nào.
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, tenDangNhap);
        ps.setString(2, hash);
        ps.setString(3, vaiTro);
        ps.executeUpdate();
    }
}
```

### 8.2 Khóa/Mở khóa & Đổi vai trò — chặn tự thao tác lên chính mình

```java
private void toggleLock(TaiKhoan tk) {
    if (isSelf(tk)) {
        JOptionPane.showMessageDialog(this, "Không thể tự khóa chính tài khoản đang đăng nhập.");
        return;
        // Chặn để tránh tình huống Admin tự khóa mình rồi KHÔNG CÒN AI đăng nhập được để mở khóa lại.
    }
    String newStatus = "HoatDong".equals(tk.getTrangThai()) ? "KhoaTaiKhoan" : "HoatDong";
    // Đảo trạng thái: đang HoatDong -> chuyển KhoaTaiKhoan, và ngược lại.
    try {
        taiKhoanDAO.setTrangThai(tk.getMaTK(), newStatus);
        reloadData();
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Không cập nhật được trạng thái: " + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
```

```java
private boolean isSelf(TaiKhoan tk) {
    TaiKhoan current = Session.getCurrentAccount();
    return current != null && current.getMaTK() == tk.getMaTK();
    // So sánh 2 số int bằng "==" (đúng, vì MaTK là kiểu int nguyên thủy, không phải object) —
    // current chính là tài khoản Admin ĐANG ĐĂNG NHẬP (lấy từ Session), tk là dòng trong bảng đang thao tác.
}
```

```java
private void changeRole(TaiKhoan tk) {
    if (isSelf(tk)) {
        JOptionPane.showMessageDialog(this, "Không thể tự đổi vai trò của chính tài khoản đang đăng nhập.");
        return;
    }
    String[] options = {"Admin", "NhanVien"};
    String chosen = (String) JOptionPane.showInputDialog(this, "Chọn vai trò mới cho \"" + tk.getTenDangNhap() + "\":",
            "Đổi vai trò", JOptionPane.PLAIN_MESSAGE, null, options, tk.getVaiTro());
    // showInputDialog với mảng options: hiện dạng DROPDOWN cho người dùng chọn 1 trong các giá trị có sẵn
    // (khác showInputDialog thường chỉ có 1 ô nhập tự do) — "tk.getVaiTro()" là giá trị được chọn sẵn ban đầu.
    if (chosen == null) {
        return;      // bấm Cancel -> chosen = null -> hủy thao tác
    }
    try {
        taiKhoanDAO.setVaiTro(tk.getMaTK(), chosen);
        reloadData();
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Không cập nhật được vai trò: " + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
```

---

## 9. Luồng ghi log hoạt động (Audit log)

File: [`util/ActivityLogger.java`](src/src/util/ActivityLogger.java)

```java
public static void logNhanVienAction(String hanhDong, NhanVien nv) {
    TaiKhoan current = Session.getCurrentAccount();
    // 🟡 NGOÀI CHƯƠNG TRÌNH — đọc lại "ai đang đăng nhập" từ Session để biết AI vừa thực hiện hành động
    // này, mà không cần View phải TRUYỀN tham số người dùng vào (ActivityLogger tự lấy từ Session).
    String nguoiThucHien = current == null ? "Không xác định"
            : current.getTenDangNhap() + " (" + current.getVaiTro() + ")";
    log("Tài khoản " + nguoiThucHien + " đã " + hanhDong + " nhân viên \""
            + nv.getHoTen() + "\" (Mã NV: " + nv.getMaNV() + ")");
    // hanhDong nhận vào là chuỗi "SỬA" hoặc "XÓA" (do nơi gọi truyền vào, xem mục 6.5/6.7).
}
```

```java
private static void log(String message) {
    File file = new File(LOG_FILE);
    // LOG_FILE = "logs/nhanvien_activity.txt" (đường dẫn TƯƠNG ĐỐI, tính từ thư mục chạy chương trình).
    File dir = file.getParentFile();
    if (dir != null && !dir.exists()) {
        dir.mkdirs();
        // Tự tạo thư mục "logs/" nếu chưa có — tránh lỗi FileNotFoundException khi mở file để ghi
        // mà thư mục cha chưa tồn tại (đây là lỗi RẤT PHỔ BIẾN khi mới học ghi file).
    }
    String line = "[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "] " + message;
    // TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss") — định dạng lại thời gian
    // hiện tại cho dễ đọc, thay vì để nguyên dạng mặc định của LocalDateTime.toString().

    try (FileWriter fw = new FileWriter(file, true);
         // Tham số thứ 2 = true nghĩa là APPEND (ghi nối tiếp vào cuối file) — nếu để mặc định (false)
         // hoặc bỏ tham số này, mỗi lần ghi sẽ XÓA TRẮNG nội dung file cũ rồi ghi đè.
         PrintWriter pw = new PrintWriter(fw)) {
        // Bọc FileWriter bằng PrintWriter để dùng được println() ghi cả dòng kèm xuống dòng tự động,
        // thay vì phải tự nối "\n" thủ công với FileWriter.write().
        pw.println(line);
    } catch (IOException e) {
        System.err.println("Khong ghi duoc nhat ky: " + e.getMessage());
        // Chỉ in lỗi ra console (System.err), KHÔNG ném exception lên trên — vì việc ghi log thất bại
        // (VD ổ đĩa đầy, không có quyền ghi) KHÔNG NÊN làm hỏng luồng nghiệp vụ chính (sửa/xóa nhân viên
        // vẫn tính là THÀNH CÔNG dù log lỗi).
    }
}
```

---

## 10. Luồng nền: kết nối DB cho mọi thao tác

File: [`database/DBConnection.java`](src/src/database/DBConnection.java)

```java
public class DBConnection {

    private static final Properties CONFIG = loadConfig();
    // "static final" -> loadConfig() CHỈ CHẠY ĐÚNG 1 LẦN, ngay khi class DBConnection được JVM nạp
    // lần đầu tiên (thường là lúc gọi DBConnection.getConnection() lần đầu) — không đọc lại file mỗi lần
    // kết nối, tránh lãng phí thao tác đọc file lặp lại.

    private DBConnection() {
    }
    // Constructor private -> không "new DBConnection()" được, class này chỉ dùng qua các hàm static.

    private static Properties loadConfig() {
        Properties props = new Properties();
        // 🟡 NGOÀI CHƯƠNG TRÌNH — java.util.Properties: cấu trúc key-value chuyên dùng để đọc file
        // cấu hình dạng "key=value" (file .properties), khác Map thông thường ở chỗ có sẵn hàm load()
        // đọc thẳng từ file/stream.
        try (InputStream in = DBConnection.class.getResourceAsStream("db.properties")) {
            // getResourceAsStream("db.properties"): tìm file này TRONG CLASSPATH, cùng package với
            // class DBConnection (tức thư mục database/) — khác hẳn việc mở file bằng đường dẫn tuyệt
            // đối trên ổ đĩa (new File("C:/...")), nên chạy được trên MỌI MÁY mà không cần sửa đường dẫn.
            if (in == null) {
                throw new IllegalStateException("Khong tim thay database/db.properties trong classpath.");
                // Không tìm thấy file -> ném lỗi RÕ RÀNG ngay lập tức, thay vì để lỗi NullPointerException
                // khó hiểu xảy ra ở bước sau khi cố gọi props.load(null).
            }
            props.load(in);
            // Đọc toàn bộ nội dung file dạng "key=value" (VD "db.url=jdbc:mysql://localhost:3306/qlnhansu")
            // và nạp hết vào object props.
        } catch (IOException e) {
            throw new IllegalStateException("Khong doc duoc database/db.properties.", e);
            // "throw new ... (message, e)": bọc exception gốc (e) vào bên trong exception mới ném ra —
            // giữ lại được nguyên nhân lỗi gốc (stack trace) để debug, thay vì "nuốt mất" exception cũ.
        }
        return props;
    }

    public static Connection getConnection() throws SQLException {
        String url = CONFIG.getProperty("db.url");
        String username = CONFIG.getProperty("db.username");
        String password = CONFIG.getProperty("db.password");
        return DriverManager.getConnection(url, username, password);
        // DriverManager.getConnection(): mở 1 KẾT NỐI MỚI tới MySQL mỗi lần hàm này được gọi.
        // Mỗi phương thức trong các DAO (insert/update/delete/find...) đều tự gọi getConnection() rồi
        // tự đóng lại ngay khi xong việc (try-with-resources) — không có "connection pool" dùng chung.
    }
}
```

---

## 11. Tổng hợp: trong chương trình vs ngoài chương trình

### 11.1 Phần NẰM TRONG chương trình đã học

Đối chiếu với danh sách: if/else, switch, do-while, for, array, ghi/đọc file, exception, OOP, MySQL JDBC, package/abstract class, PreparedStatement, insert MySQL JDBC (dynamic data), form Swing, validate form insert JDBC-Swing, liệt kê dữ liệu JDBC.

| Kiến thức đã học | Xuất hiện cụ thể ở đâu trong dự án |
|---|---|
| **if/else** | Khắp nơi — điển hình: `Login.handleLogin()` (validate từng bước), `AddEmployeesPanel.validateForm()`, `NhanVienDAO.appendAdvancedFilters()` (kiểm tra `if (maPB != null)`...) |
| **switch** | `ListEmployeesPanel.refreshCurrentView()` (rẽ theo `viewMode`), `ListEmployeesPanel.trangThaiDisplayToCode()` (map chuỗi hiển thị → mã DB) |
| **do-while** | ❌ Dự án **không dùng** vòng lặp `do-while` ở đâu cả — mọi vòng lặp đọc `ResultSet` đều dùng `while (rs.next())` (while thường, không phải do-while) |
| **for** | `NhanVienDAO.bindParams()` (for theo index), `CategoryManagementPanel.selectPhongBanById()`-kiểu (for duyệt combobox), for-each (`for (NhanVien nv : list)`) trong `TableModelUtil`, `CategoryTab.reload()`, `DashboardPanel.loadPhongBanStats()` |
| **Array** | `String[] columns` (khai báo cột `DefaultTableModel`), `Object[]` (1 dòng dữ liệu add vào bảng), `String[] options` (danh sách lựa chọn trong `JOptionPane.showInputDialog`), `String[]{"Admin","NhanVien"}` |
| **Ghi file / đọc file** | Ghi: `ActivityLogger.log()` (`FileWriter` + `PrintWriter`, append vào `logs/nhanvien_activity.txt`). Đọc: `DBConnection.loadConfig()` (đọc `db.properties` qua `InputStream`) |
| **Exception** | `try/catch/finally` xuyên suốt mọi DAO (`SQLException`), `Login.handleLogin()`, `AddEmployeesPanel.uploadAvatar()` (`IOException`), `validateForm()` (`NumberFormatException`, `DateTimeParseException` qua `Exception` chung) |
| **OOP** (đóng gói, kế thừa, đa hình cơ bản) | Đóng gói: mọi model (`NhanVien`, `TaiKhoan`...) có field `private` + getter/setter. Kế thừa: `Login extends JFrame`, `ListEmployeesPanel extends JPanel`. Đa hình cơ bản (override): `PhongBan`/`ChucVu` override `toString()`, `getId()/getTen()/getMoTa()` |
| **MySQL JDBC** | `DBConnection` (`DriverManager.getConnection`), toàn bộ `database/dao/` |
| **Package** | 5 package rõ ràng: `database`, `database.dao`, `model`, `util`, `view`, `view.panels` |
| **Abstract class** | Khái niệm có xuất hiện, nhưng dự án **không tự viết** abstract class riêng — chỉ **kế thừa** abstract class *có sẵn của thư viện Swing* (`AbstractCellEditor`) → xem thêm ghi chú ở mục 11.2 vì cách dùng cụ thể này nâng cao hơn bài học gốc |
| **PreparedStatement trong JDBC** | Mọi câu SQL trong `database/dao/*.java` đều dùng `PreparedStatement` với `?` + `setString/setInt/...` |
| **Insert MySQL JDBC (dynamic data)** | `NhanVienDAO.insert()`, `TaiKhoanDAO.create()`, `PhongBanDAO.insert()`, `ChucVuDAO.insert()` — đều nhận dữ liệu động từ form rồi bind vào `INSERT` |
| **Form giao diện Java Swing** | Toàn bộ `view/` — `Login.form`, `MainFrame.form`, `DashboardPanel.form`, `ListEmployeesPanel.form`, `AddEmployeesPanel.form` (dựng qua IntelliJ GUI Designer); `AccountManagementPanel`, `CategoryManagementPanel` dựng tay bằng code |
| **Validate form insert với JDBC-Swing** | `Login.handleLogin()`, `AddEmployeesPanel.validateForm()`, `AccountManagementPanel.openCreateAccountDialog()`, `CategoryManagementPanel.openEditDialog()` — validate ở View trước khi gọi DAO insert/update |
| **Liệt kê dữ liệu với JDBC** | `NhanVienDAO.findAll/findPage`, `PhongBanDAO.findAll`, `ChucVuDAO.findAll`, `TaiKhoanDAO.findAll`, đổ lên `JTable` qua `TableModelUtil`/`DefaultTableModel.addRow` |

### 11.2 Phần NẰM NGOÀI chương trình đã học

| Kỹ thuật | Xuất hiện ở đâu | Vì sao khác chương trình đã học |
|---|---|---|
| `SwingUtilities.invokeLater` / EDT | `Main.java` | Cách khởi động Swing đúng chuẩn thread |
| Regex (`Pattern`/`Matcher`) | `ValidationUtil`, `Login`, `CategoryManagementPanel` | Validate mạnh hơn if/else kiểm tra thủ công |
| jBCrypt (băm mật khẩu) | `TaiKhoanDAO` | Thư viện ngoài, bảo mật mật khẩu 1 chiều |
| `Session` static holder | `Session.java` | Lưu trạng thái đăng nhập toàn cục |
| Interface đa hình (`DanhMuc`, `DanhMucDAO`) | `model/`, `database/dao/`, `CategoryManagementPanel` | 1 đoạn code dùng chung cho 2 loại dữ liệu khác nhau |
| `CardLayout` | `MainFrame` | Nhiều "màn hình" trong 1 JFrame |
| Lambda + `Runnable`/`Consumer<T>` callback | `MainFrame`, các panel | Giao tiếp giữa các panel không phụ thuộc trực tiếp |
| Custom `TableCellRenderer`/`TableCellEditor`, `AbstractCellEditor` | 3 panel có bảng | Nhúng nút bấm vào ô `JTable`, kế thừa abstract class có sẵn của thư viện |
| SQL động (`StringBuilder` + `List<Object>` params) | `NhanVienDAO.advancedSearch` | Lọc theo nhiều điều kiện tùy chọn |
| Phân trang `LIMIT/OFFSET` | `NhanVienDAO`, `ListEmployeesPanel` | Không load hết dữ liệu 1 lần |
| Bắt exception theo phân cấp cụ thể (`SQLIntegrityConstraintViolationException`) | `AddEmployeesPanel`, `CategoryManagementPanel` | Phân biệt lỗi trùng khóa với lỗi SQL khác |
| `Map`/`LinkedHashMap` | `NhanVienDAO.countByPhongBan`, `DashboardPanel` | Gom nhóm thống kê key–value |
| `java.time` (`LocalDate`, `LocalDateTime`, `DateTimeFormatter`) | `DateUtil`, `ActivityLogger` | API ngày giờ hiện đại thay `Date`/`Calendar` |
| `JSpinner`/`SpinnerDateModel`, `JFileChooser` | `AddEmployeesPanel` | Component Swing nâng cao (chọn ngày, chọn file) |
| `ImageIO`, `BufferedImage`, `Files.copy`, `UUID` | `AddEmployeesPanel` (upload avatar) | Xử lý ảnh + copy file kiểu NIO.2 hiện đại |
| `Properties` + `getResourceAsStream` | `DBConnection` | Tách cấu hình DB khỏi code nguồn |
| Audit log gắn `Session` | `ActivityLogger` | Log nghiệp vụ (ai làm gì), không phải ghi file thường |
| `BigDecimal` cho tiền tệ | `AddEmployeesPanel`, `NhanVienDAO` | Tránh sai số số thực khi tính lương |
