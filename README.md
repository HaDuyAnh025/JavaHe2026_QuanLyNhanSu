Final_1
└── src
    └── qlnhansu                      ← package gốc (đổi tên gói theo ý bạn, vd: hrms)
        ├── Main.java                  ← điểm khởi chạy (mở Dashboard/Login)
        │
        ├── model/                     ← các lớp dữ liệu (POJO) ánh xạ 1-1 với bảng DB
        │   ├── NhanVien.java
        │   ├── PhongBan.java
        │   ├── ChucVu.java
        │   └── TaiKhoan.java
        │
        ├── database/                  ← toàn bộ phần kết nối & truy vấn CSDL
        │   ├── DBConnection.java      ← singleton mở/đóng Connection (JDBC)
        │   ├── dao/                   ← Data Access Object — 1 lớp/1 bảng
        │   │   ├── NhanVienDAO.java   ← CRUD cho AddEmployees, ListEmployees, SearchEmployees
        │   │   ├── PhongBanDAO.java   ← đổ dữ liệu cho JComboBox "Phòng ban"
        │   │   └── ChucVuDAO.java     ← đổ dữ liệu cho JComboBox "Chức vụ"
        │   └── schema.sql             ← script tạo bảng, chạy 1 lần khi setup DB
        │
        ├── service/                   ← (tùy chọn) xử lý nghiệp vụ giữa UI và DAO
        │   └── NhanVienService.java   ← validate dữ liệu trước khi gọi DAO
        │
        ├── view/                      ← toàn bộ .form + .java UI hiện có của bạn
        │   ├── Dashboard.form / .java
        │   ├── AddEmployees.form / .java
        │   ├── ListEmployees.form / .java
        │   ├── SearchEmployees.form / .java
        │   └── Update_SearchEmp.form / .java
        │
        ├── util/                      ← tiện ích dùng chung
        │   ├── DateUtil.java          ← parse/format dd/mm/yyyy
        │   ├── ValidationUtil.java    ← validate email, SĐT, CCCD
        │   └── TableModelUtil.java    ← đổ List<NhanVien> vào DefaultTableModel cho JTable
        │
        └── resources/
            └── db.properties          ← url, username, password (KHÔNG hardcode trong code)