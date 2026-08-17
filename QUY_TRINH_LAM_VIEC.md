# Quy trình làm việc nhóm (2 người)

Dựa trên phân chia công việc ở [README.md — mục 4](README.md#4-phân-chia-công-việc-nhóm-2-người).

## Nguyên tắc

- **Thiết kế database phải xong trước tiên** — model Java và toàn bộ DAO đều ánh xạ từ schema, không thể làm ngược.
- **DAO (Data Access Object)** là lớp duy nhất được chạm vào SQL/JDBC (`PreparedStatement`, `Connection`, `ResultSet`). Panel chỉ gọi hàm như `nhanVienDAO.getAll()`, không viết SQL trực tiếp. Nhờ tách lớp này, 2 người có thể làm độc lập miễn là thống nhất trước tên hàm và kiểu trả về.
- Trước khi bắt tay code, cả 2 phải **chốt hợp đồng**: chữ ký các class `model/` và tên hàm chính trong `dao/` — vì đây là ranh giới giao tiếp giữa 2 phần việc.
- Trong lúc chờ DAO thật, Phúc **không nghỉ hoàn toàn** mà:
  1. Vẽ `.form` bằng GUI Designer trước (không cần DAO, chỉ cần biết field/hàm đã chốt).
  2. Nếu còn dư thời gian, code logic panel bằng **mockdata** (`List` hardcode tay) để test UI, đặt tên biến/kiểu dữ liệu giống hệt lúc dùng DAO thật — để lúc có DAO chỉ cần đổi nguồn dữ liệu, không phải viết lại.

## Timeline (6 ngày)

| Ngày | Hà Duy Anh (A47752) | Nguyễn Tiến Phúc (A48595) | Ghi chú |
|---|---|---|---|
| **1 (sáng)** | Thiết kế database: ERD, 4 bảng, khóa chính/ngoại, ràng buộc, viết `Data.Basesql.sql` | Review cùng thiết kế DB | Nền tảng cho mọi phần sau |
| **1 (chiều)** | Từ schema, khai báo `model/` + tên hàm DAO chính (chữ ký, chưa cần code thân hàm) | Review model/DAO có đủ field/hàm cho panel mình cần không | Chốt hợp đồng |
| **2** | Code `DBConnection` + impl toàn bộ `dao/` (CRUD, sinh Mã NV tự động) | Vẽ `.form` cho `MainFrame`, `DashboardPanel`, `AccountManagementPanel`, `CategoryManagementPanel` — thuần layout, đặt tên biến theo field đã chốt | Không chờ, chỉ chưa gắn logic thật |
| **2 (nếu dư thời gian)** | (tiếp tục `dao/`) | Code logic panel dùng mockdata để test UI hiển thị đúng chưa | Tùy dư thời gian |
| **2 (cuối ngày)** | Push `dao/`, báo Phúc | Nhận DAO thật | Điểm đồng bộ giữa 2 người |
| **3** | `Login.java/.form` + BCrypt, bắt đầu `ListEmployeesPanel` | Thay mockdata bằng DAO thật trong các panel đã code sẵn | Chỉ đổi nguồn dữ liệu, không viết lại từ đầu |
| **4** | Hoàn thiện `ListEmployeesPanel`, bắt đầu `AddEmployeesPanel` | Hoàn thiện `AccountManagementPanel` với dữ liệu thật (khóa/mở/đổi vai trò) | |
| **5** | Hoàn thiện `AddEmployeesPanel` (validate, avatar, `ActivityLogger`) | Hoàn thiện `CategoryManagementPanel` với dữ liệu thật | |
| **6 (sáng)** | Cắm `Login` → `MainFrame`, merge nhánh | Cắm các panel còn lại vào `MainFrame`, merge nhánh | Dễ conflict ở `MainFrame.java` |
| **6 (chiều)** | Fix bug phần mình | Test end-to-end toàn luồng (đăng nhập → CRUD NV → phân quyền → danh mục), ghi lại lỗi | Khớp phân công README ([dòng 109](README.md#L109)) |

## Git workflow

- Nhánh `main` luôn ở trạng thái chạy được.
- Mỗi người 1 nhánh feature (`feature/nhanvien`, `feature/admin-ui`), commit nhỏ, merge vào `main` mỗi ngày để tránh dồn conflict.
- File dễ conflict nhất: `MainFrame.java`/`.form` — vì cả 2 đều cần cắm panel vào đây, nên ưu tiên Phúc merge trước (người sở hữu file), Anh merge theo sau.
