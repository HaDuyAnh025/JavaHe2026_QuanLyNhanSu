# Giải thích chi tiết code & luồng xử lý — QLNhanSu

Tài liệu này giải thích **kiến trúc tổng thể** và **từng luồng xử lý chính** của dự án, dành cho người chưa đọc code bao giờ cũng nắm được. Đi kèm [`README.md`](README.md) (tổng quan + hướng dẫn chạy).

---

## 1. Kiến trúc tổng quan

Dự án theo mô hình phân lớp đơn giản, không dùng framework:

```
view (Swing UI)  →  database.dao (DAO)  →  database.DBConnection  →  MySQL
       ↑                                              ↑
     model (POJO)  ←───────────── ánh xạ (mapRow) ─────┘
```

- **`view`**: toàn bộ giao diện Swing. Chia làm `Login`/`MainFrame` (2 khung ngoài cùng) và `view.panels` (các "trang" nội dung nhúng vào `MainFrame`).
- **`database.dao`**: mỗi lớp DAO tương ứng 1 bảng, chỉ chứa JDBC thuần (`PreparedStatement`), không có logic UI.
- **`model`**: POJO thuần (getter/setter), không có logic, chỉ mang dữ liệu giữa các lớp.
- **`util`**: hàm tiện ích dùng chung (validate, format ngày, đổ bảng, ghi log).

Không có tầng "Service" trung gian — DAO được gọi thẳng từ panel UI (đơn giản hóa vì quy mô dự án nhỏ).

---

## 2. Cơ chế `.form` + `.java` (IntelliJ GUI Designer) — điều quan trọng nhất cần hiểu trước

Các màn hình `Login`, `MainFrame`, `DashboardPanel`, `ListEmployeesPanel`, `AddEmployeesPanel` được thiết kế bằng **IntelliJ GUI Designer** (kéo-thả), lưu trong file `.form` (XML mô tả layout dùng `GridLayoutManager`). Class `.java` tương ứng **không tự vẽ giao diện bằng code** — khi IntelliJ build, nó tự sinh một hàm `$$$setupUI$$$()` từ nội dung `.form` và "tiêm" vào bytecode `.class` (**chế độ "binary form generation"**).

Hệ quả quan trọng:
- Các field như `private JButton lọcButton;` trong `.java` **PHẢI trùng tên** với thuộc tính `binding="..."` trong `.form` tương ứng — IntelliJ tự gán giá trị cho field đó lúc khởi tạo, không cần code Java tự `new JButton()`.
- File `.java` chỉ chứa: khai báo field (khớp binding), constructor gọi các hàm `setupXxx()` để gắn sự kiện (`addActionListener`), và toàn bộ logic nghiệp vụ.
- **Không thể build/chạy dự án bằng `javac` dòng lệnh** — bắt buộc phải mở bằng IntelliJ vì bước sinh `$$$setupUI$$$()` chỉ chạy trong IDE.
- Mỗi `<grid>` trong `.form` có `row-count`/`column-count` khai báo — **phải khớp chính xác** số hàng/cột lớn nhất mà các component con dùng tới (0-indexed), nếu không sẽ lỗi runtime `IllegalArgumentException: wrong row: N`. Đây là lỗi hay gặp nhất khi chỉnh sửa `.form` bằng tay.

`AccountManagementPanel` và `CategoryManagementPanel` là 2 ngoại lệ: viết **thuần Java Swing** (không có `.form`), vì đây là 2 màn hoàn toàn mới không cần khớp thiết kế có sẵn, và có bảng với cột "Thao tác" chứa nút bấm động (khó dựng bằng GUI Designer tĩnh) nên tự code renderer/editor cho `JTable`.

---

## 3. Khởi động ứng dụng

```
Main.main() → new Login().setVisible(true)
```

`Login` chỉ validate **định dạng** trước (email đúng dạng, mật khẩu ≥ 6 ký tự) rồi mới gọi `TaiKhoanDAO.checkLogin(username, password)`:

1. Query `TAIKHOAN` theo `TenDangNhap`.
2. Kiểm tra `TrangThai = 'HoatDong'` (chưa bị khóa).
3. `BCrypt.checkpw(matKhauNhap, matKhauHashTrongDB)` — so khớp mật khẩu.
4. Nếu tất cả đúng: cập nhật `LanDangNhapCuoi`, trả về `TaiKhoan`.

Nếu đăng nhập thành công: `Session.setCurrentAccount(taiKhoan)` (lưu tài khoản vào 1 biến `static` dùng chung toàn app) → mở `new MainFrame()` → đóng `Login`.

`Session` là **singleton kiểu holder tĩnh** (không phải Spring bean, chỉ là 1 class với field `static`), giúp bất kỳ panel nào cũng lấy được "ai đang đăng nhập" mà không cần truyền tham số qua nhiều lớp.

---

## 4. `MainFrame` — khung chính & điều hướng

`MainFrame` chứa: sidebar (nút Tổng quan/Quản lý/Phân quyền/Danh mục/Đăng xuất), top bar (logo, avatar = tên tài khoản), và 1 `contentPanel` dùng **`CardLayout`** để chuyển "trang" mà không mở `JFrame` mới.

```java
setupContentCards()   // tạo 5 panel (Dashboard, ListEmployees, AddEmployees, Account, Category)
                       // add hết vào contentPanel với CardLayout, đăng ký callback qua lại giữa các panel
setupNavigation()      // gắn sự kiện cho các nút sidebar → showCard(...)
setupTopBar()          // gắn sự kiện avatar (hiện thông tin tài khoản)
applyRolePermissions() // ẩn "Phân quyền"/"Danh mục" nếu KHÔNG phải Admin
showCard(CARD_DASHBOARD, ...) // trang mặc định khi mở app
```

**Phân quyền hiển thị**: `applyRolePermissions()` chỉ ẩn/hiện 2 nút sidebar theo `currentAccount.isAdmin()`. Các thao tác Sửa/Xóa nhân viên thì **cả 2 vai trò đều được phép** (không phân biệt trong `ListEmployeesPanel`).

**Giao tiếp giữa các panel** (vì mỗi panel không biết về nhau, chỉ `MainFrame` biết hết) dùng callback kiểu `Runnable`/`Consumer`:

```java
listEmployeesPanel.setOnAddEmployeeRequested(() -> { addEmployeesPanel.resetToAddMode(); showCard(ADD_EMPLOYEE, ...); });
listEmployeesPanel.setOnEditEmployeeRequested(nv -> { addEmployeesPanel.loadForEdit(nv); showCard(ADD_EMPLOYEE, ...); });
addEmployeesPanel.setOnSavedOrCancelled(() -> { listEmployeesPanel.reloadData(); showCard(LIST_EMPLOYEES, ...); });
```

`contentPanel` được bọc trong 1 `JScrollPane` (`contentScroll`) ở `.form`, để khi cửa sổ bị thu nhỏ, nội dung **cuộn được** thay vì bị cắt hoặc ép kích thước cửa sổ tối thiểu quá lớn.

---

## 5. `ListEmployeesPanel` — trang "Quản lý" (trang phức tạp nhất)

Trang này có **3 chế độ hiển thị**, chuyển đổi bằng biến `viewMode`:

| Mode | Khi nào | Nguồn dữ liệu |
|---|---|---|
| `MODE_BROWSE` | Mặc định, hoặc bấm "Xóa lọc" | `nhanVienDAO.findPage(page, size)` — toàn bộ, phân trang |
| `MODE_KEYWORD_SEARCH` | Gõ Enter / bấm "Tìm kiếm" ở ô tìm kiếm đầu trang | `nhanVienDAO.search(keyword, page, size)` |
| `MODE_ADVANCED_FILTER` | Bấm "Áp dụng" trong bảng lọc (mở bằng nút "Lọc") | `nhanVienDAO.advancedSearch(maPB, maCV, trangThai, gioiTinh, keyword, page, size)` |

Mọi hành động (đổi trang, đổi bộ lọc, thêm/sửa/xóa xong) đều gọi lại `refreshCurrentView()` — hàm này chỉ nhìn `viewMode` rồi gọi đúng hàm load tương ứng, tránh viết lặp code phân trang 3 lần.

**Cột "Thao tác"** (Sửa/Xóa ngay trên từng dòng) được cài đặt bằng cách gắn `TableCellRenderer`/`TableCellEditor` tùy chỉnh vào cột cuối của `JTable` (không dùng nút bấm tĩnh nằm ngoài bảng):

- `ActionCellRenderer`: chỉ vẽ 2 nút (không xử lý sự kiện) khi ô **không** đang được chỉnh sửa.
- `ActionCellEditor`: khi người dùng bấm vào ô, Swing coi ô đó đang "edit" và hiển thị `panel` chứa 2 nút thật (có `addActionListener`). Bấm nút nào thì `fireEditingStopped()` (thoát chế độ edit) rồi tra `currentRows.get(editingRow)` để biết đang thao tác với `NhanVien` nào.
- `currentRows` là `List<NhanVien>` song song với các dòng đang hiển thị trong bảng — giữ để tránh phải query lại DB mỗi khi bấm Sửa/Xóa.

**Bảng lọc nâng cao** (`filterPanel`, ẩn mặc định) có: combobox Phòng ban/Chức vụ (nạp từ DAO, item là chính đối tượng `PhongBan`/`ChucVu` — `toString()` trả về tên để combobox hiển thị đẹp), combobox Trạng thái, và 4 radio Giới tính (Tất cả/Nam/Nữ/Khác). Radio được gán **thêm 1 lớp bảo hiểm** bằng code (`selectSingleGenderFilterRadio`) ngoài `ButtonGroup` khai trong `.form`, để chắc chắn chỉ chọn được 1.

**Xóa lọc**: đưa toàn bộ combobox/radio về mặc định rồi gọi `exitSearchMode()` (quay về `MODE_BROWSE`).

---

## 6. `AddEmployeesPanel` — form Thêm/Sửa dùng chung

1 form Swing dùng cho **cả 2 mục đích**, phân biệt bằng field `editingEmployee` (`null` = đang Thêm mới):

- `resetToAddMode()`: xóa trắng form, đổi tiêu đề "Thêm mới nhân viên", nạp lại combobox Phòng ban/Chức vụ (để đồng bộ nếu vừa có thay đổi ở trang Danh mục).
- `loadForEdit(nv)`: đổ dữ liệu `NhanVien` có sẵn vào từng ô, đổi tiêu đề "Sửa thông tin nhân viên", khóa ô Mã NV.

**Validate** (`validateForm()`) chạy trước khi lưu: họ tên bắt buộc, ngày sinh đúng `dd/MM/yyyy` và không ở tương lai, CCCD (nếu có nhập) đúng 9 hoặc 12 số, SĐT/email bắt buộc và đúng định dạng, lương không âm.

**Chọn ngày sinh/ngày vào làm**: không dùng thư viện date-picker ngoài — tự dựng dialog nhỏ bằng `JSpinner(SpinnerDateModel)` (`pickDate()`), đủ dùng mà không phải thêm dependency.

**Ảnh đại diện** (`uploadAvatar()`): mở `JFileChooser`, copy file ảnh gốc vào thư mục `avatars/` với tên ngẫu nhiên (`UUID`), rồi lưu **đường dẫn tương đối** (`avatars/xxx.jpg`) vào DB — không lưu đường dẫn tuyệt đối trên máy người dùng vì sẽ hỏng nếu đổi máy/đổi thư mục.

**Sinh Mã NV**: form này **không tự sinh mã** — nó chỉ hiển thị chữ "Tự động" và để trống, việc sinh mã thật sự nằm ở `NhanVienDAO.insert()` (xem mục 8).

**Giới tính**: giống `ListEmployeesPanel`, có thêm lớp bảo hiểm `selectSingleGenderRadio()` để đảm bảo chỉ chọn được 1 trong Nam/Nữ/Khác.

---

## 7. Tầng DAO — quy tắc chung

Tất cả DAO theo cùng 1 khuôn:

```java
try (Connection conn = DBConnection.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setXxx(...);
    ...
}
```

- Dùng `try-with-resources` → tự đóng `Connection`/`PreparedStatement`/`ResultSet`, không rò kết nối.
- Luôn dùng `PreparedStatement` (không nối chuỗi SQL trực tiếp) → tránh SQL injection.
- Mỗi DAO có hàm `mapRow(ResultSet)` riêng để chuyển 1 dòng kết quả thành 1 object model.

### `NhanVienDAO` — điểm đặc biệt: tự sinh Mã NV

```java
public String insert(NhanVien nv) {
    String maNV = generateNextMaNV(nv.getMaPB());  // vd "IT004"
    nv.setMaNV(maNV);
    // INSERT với maNV đã tính, không phải AUTO_INCREMENT
}
```

`generateNextMaNV(maPB)`:
1. Query tất cả `MaNV` trong bảng `NHANVIEN` có tiền tố = mã phòng ban (`LIKE 'IT%'`).
2. Với mỗi kết quả, cắt bỏ tiền tố, parse phần còn lại thành số (`Integer.parseInt`), lấy giá trị **lớn nhất**.
3. Trả về `prefix + String.format("%03d", max + 1)`.

Cách này (lấy **MAX** thay vì **COUNT**) đảm bảo không bao giờ trùng mã dù nhân viên ở giữa đã bị xóa — ví dụ phòng IT có `IT001, IT002, IT003`, xóa `IT002`, thêm nhân viên mới vẫn ra `IT004` (không phải `IT003` bị trùng nếu dùng COUNT).

Nếu chưa chọn phòng ban khi thêm, dùng tiền tố mặc định `"NV"`.

### `advancedSearch()` / `countAdvancedSearch()`

Dùng chung 1 hàm `appendAdvancedFilters()` để build câu `WHERE` động — chỉ thêm điều kiện nào có giá trị (`maPB != null`, `keyword` không rỗng, ...), tránh viết N câu SQL riêng cho từng tổ hợp bộ lọc.

---

## 8. `CategoryManagementPanel` — CRUD dùng chung cho Phòng ban & Chức vụ

`PhongBan` và `ChucVu` có cấu trúc dữ liệu giống hệt nhau (mã, tên, mô tả) nên cùng implement interface `DanhMuc { getId(), getTen(), getMoTa() }`. Nhờ vậy, `CategoryManagementPanel` chỉ cần viết **1 lớp `CategoryTab`** dùng chung cho cả 2 tab ("Phòng ban" / "Chức vụ"), nhận vào 1 `DanhMucDAO` (interface mà cả `PhongBanDAO` và `ChucVuDAO` cùng implement) — không phải viết trùng logic CRUD 2 lần.

- **Mã** do người dùng tự nhập khi Thêm (không tự sinh) — validate bằng regex `^[A-Z]{1,5}$` (tối đa 5 chữ in hoa), tự động viết hoa chuỗi nhập vào trước khi kiểm tra.
- Khi Sửa, ô Mã hiển thị nhưng khóa không cho đổi (đổi mã sẽ phá vỡ liên kết với nhân viên đang tham chiếu).
- Cột "Thao tác" (Sửa/Xóa từng dòng) dùng renderer/editor tùy chỉnh, cùng kỹ thuật như mục 5.
- `deleteEntry()` gọi `dao.countUsage(id)` trước — nếu có nhân viên đang thuộc danh mục này thì cảnh báo rõ số lượng trước khi xóa thật (xóa xong, `NHANVIEN.MaPB`/`MaCV` của các nhân viên đó tự chuyển `NULL` nhờ `ON DELETE SET NULL` khai trong schema).
- Bảng chỉ cao vừa đủ số dòng hiện có (tối đa 8 dòng, `updateTableHeight()` tính lại mỗi lần `reload()`), tránh để lại khoảng trắng lớn khi danh mục còn ít dữ liệu.

---

## 9. `AccountManagementPanel` — Phân quyền tài khoản

Tương tự `CategoryManagementPanel` (viết thuần Java, có cột "Thao tác" với renderer/editor tùy chỉnh), nhưng thao tác trên `TAIKHOAN`:

- **Tạo tài khoản**: validate username phải đúng định dạng email (vì màn Đăng nhập dùng chung field này để check định dạng), mật khẩu ≥ 6 ký tự và khớp xác nhận, kiểm tra trùng username trước khi `TaiKhoanDAO.create()` (hàm này tự băm mật khẩu bằng `BCrypt.hashpw(pass, BCrypt.gensalt(12))` trước khi lưu).
- **Khóa/Mở khóa**, **Đổi vai trò**: thao tác trực tiếp trên từng dòng. Cả 2 đều gọi `isSelf(tk)` trước — nếu dòng đó chính là tài khoản đang đăng nhập thì chặn lại (không cho tự khóa/tự hạ quyền chính mình, tránh khóa nhầm mất quyền truy cập).

---

## 10. `DashboardPanel` — Tổng quan

`loadDashboardData()` gọi 3 hàm DAO lấy số thật (không còn số giả cứng như bản đầu):
- `nhanVienDAO.countAll()` — tổng số nhân viên.
- `phongBanDAO.findAll().size()` — số phòng ban.
- `nhanVienDAO.countNewThisMonth()` — nhân viên có `NgayVaoLam` trong tháng hiện tại.

`loadPhongBanStats()` gọi `nhanVienDAO.countByPhongBan()` (1 câu `GROUP BY` join `PHONGBAN` ↔ `NHANVIEN`, dùng `LEFT JOIN` để phòng ban 0 người vẫn hiện), rồi tự dựng 1 `JPanel` chứa danh sách `"Tên phòng ban — N nhân viên"` (dạng chữ, **không dùng biểu đồ** — theo đúng yêu cầu ban đầu để tránh phụ thuộc thư viện vẽ chart ngoài).

---

## 11. Bảo mật mật khẩu (BCrypt)

- **Không bao giờ** lưu mật khẩu gốc — cột `TAIKHOAN.MatKhauHash` chỉ lưu chuỗi hash BCrypt (`$2a$12$...`).
- Tạo tài khoản: `BCrypt.hashpw(matKhauGoc, BCrypt.gensalt(12))`.
- Đăng nhập: `BCrypt.checkpw(matKhauNhap, hashTrongDB)` — hàm này tự tách salt từ trong hash để so khớp, không cần lưu salt riêng.
- **Lưu ý kỹ thuật**: thư viện `org.mindrot.jbcrypt` (bản cổ điển) chỉ đọc được hash dạng tiền tố `$2a$`, **không đọc được** `$2b$`/`$2y$` do các thư viện bcrypt khác (vd Node.js, PHP) sinh ra — nếu import dữ liệu từ nguồn khác cần đảm bảo hash đúng định dạng `$2a$`.

---

## 12. File cấu hình & dữ liệu runtime (không commit lên Git)

| Đường dẫn | Nội dung | Vì sao gitignore |
|---|---|---|
| `src/src/database/db.properties` | URL/user/pass MySQL thật của từng máy | Có thể chứa mật khẩu thật, mỗi máy 1 cấu hình khác nhau |
| `avatars/` | Ảnh đại diện nhân viên (do người dùng upload lúc chạy app) | File nhị phân runtime, không phải source code |
| `logs/nhanvien_activity.txt` | Nhật ký sửa/xóa nhân viên | Sinh ra lúc chạy app, không phải source code |
| `out/` | Build output của IntelliJ | Sinh lại được từ source, không cần lưu |

`db.properties.example` (có commit) là file mẫu để người khác clone code về biết cần tạo `db.properties` thật với nội dung gì.

**Lưu ý cho môi trường nhiều người**: mỗi người tự chạy `Data.Basesql.sql` trên MySQL **của máy mình** → mỗi người có 1 database độc lập, không chia sẻ dữ liệu/ảnh giữa các máy. Chỉ khi nhiều người cùng trỏ vào **1 MySQL server chung** thì mới cần tính đến việc lưu ảnh ở nơi dùng chung được (vd BLOB trong DB) thay vì thư mục `avatars/` cục bộ.
