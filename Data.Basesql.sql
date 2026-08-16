-- ============================================================
-- QLNHANSU - Script tao database (MySQL)
-- Thiet ke don gian: 4 bang, phan quyen bang 1 cot VaiTro (enum)
-- Mat khau KHONG luu plaintext - chi luu MatKhauHash (BCrypt)
-- ============================================================

-- CANH BAO: dong duoi day se XOA TOAN BO database qlnhansu (neu co)
-- truoc khi tao lai tu dau. Chi dung khi dang dev/test, du lieu that
-- da nhap se bi MAT SACH. Neu khong muon xoa, hay COMMENT dong nay lai.
DROP DATABASE IF EXISTS qlnhansu;

CREATE DATABASE IF NOT EXISTS qlnhansu
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE qlnhansu;

-- ------------------------------------------------------------
-- 1. PHONGBAN - danh muc phong ban
--    MaPB: toi da 5 chu cai in hoa (vd: IT, KD, MKT), nguoi dung tu nhap
--    khi tao moi (khong con AUTO_INCREMENT).
-- ------------------------------------------------------------
CREATE TABLE PHONGBAN (
    MaPB          VARCHAR(5)    PRIMARY KEY,
    TenPhongBan   VARCHAR(100)  NOT NULL,
    MoTa          VARCHAR(255)  NULL,
    CONSTRAINT UQ_PhongBan_Ten UNIQUE (TenPhongBan)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 2. CHUCVU - danh muc chuc vu
--    MaCV: toi da 5 chu cai in hoa (vd: NV, TP, GD), nguoi dung tu nhap.
-- ------------------------------------------------------------
CREATE TABLE CHUCVU (
    MaCV          VARCHAR(5)    PRIMARY KEY,
    TenChucVu     VARCHAR(100)  NOT NULL,
    MoTa          VARCHAR(255)  NULL,
    CONSTRAINT UQ_ChucVu_Ten UNIQUE (TenChucVu)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 3. NHANVIEN - ho so nhan vien
--    MaNV: sinh tu dong theo cu phap MaPB + so thu tu 3 chu so, dem
--    RIENG cho tung phong ban (vd phong IT: IT001, IT002...; phong
--    Kinh doanh: KD001, KD002...). Do NhanVienDAO.insert() tu tinh
--    truoc khi INSERT (khong con AUTO_INCREMENT).
-- ------------------------------------------------------------
CREATE TABLE NHANVIEN (
    MaNV              VARCHAR(10)       PRIMARY KEY,
    HoTen             VARCHAR(100)      NOT NULL,
    NgaySinh          DATE              NULL,
    GioiTinh          ENUM('Nam','Nữ','Khác') NOT NULL DEFAULT 'Khác',
    SoCCCD            VARCHAR(20)       NULL,
    SoDienThoai       VARCHAR(15)       NULL,
    Email             VARCHAR(100)      NULL,
    DiaChi            VARCHAR(255)      NULL,
    AvatarPath        VARCHAR(255)      NULL,

    MaPB              VARCHAR(5)        NULL,
    MaCV              VARCHAR(5)        NULL,
    NgayVaoLam        DATE              NULL,
    LoaiHopDong       VARCHAR(50)       NULL,
    MucLuongCoBan     DECIMAL(15,2)     NULL DEFAULT 0,

    TrangThai         ENUM('DangLamViec','NghiViec','TamNghi') NOT NULL DEFAULT 'DangLamViec',

    NgayTao           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    NgayCapNhat       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT UQ_NhanVien_CCCD  UNIQUE (SoCCCD),
    CONSTRAINT UQ_NhanVien_Email UNIQUE (Email),

    CONSTRAINT FK_NhanVien_PhongBan
        FOREIGN KEY (MaPB) REFERENCES PHONGBAN(MaPB)
        ON UPDATE CASCADE ON DELETE SET NULL,

    CONSTRAINT FK_NhanVien_ChucVu
        FOREIGN KEY (MaCV) REFERENCES CHUCVU(MaCV)
        ON UPDATE CASCADE ON DELETE SET NULL,

    INDEX IDX_NhanVien_HoTen (HoTen),
    INDEX IDX_NhanVien_TrangThai (TrangThai),
    INDEX IDX_NhanVien_GioiTinh (GioiTinh)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- 4. TAIKHOAN - tai khoan dang nhap + phan quyen don gian
--    QUAN TRONG: cot MatKhauHash CHI luu 1 gia tri duy nhat -
--    la chuoi hash BCrypt cua mat khau, KHONG luu mat khau goc.
--    Luong xu ly (o phia Java, dung thu vien jBCrypt):
--      - Tao tk : BCrypt.hashpw(matKhauGoc, BCrypt.gensalt())  -> luu vao cot nay
--      - Dang nhap: BCrypt.checkpw(matKhauNhap, MatKhauHash)   -> tra ve true/false
-- ------------------------------------------------------------
CREATE TABLE TAIKHOAN (
    MaTK              INT AUTO_INCREMENT PRIMARY KEY,
    TenDangNhap       VARCHAR(100)      NOT NULL,
    MatKhauHash       VARCHAR(255)      NOT NULL,   -- chuoi hash BCrypt, vd: $2b$12$...
    VaiTro            ENUM('Admin','NhanVien') NOT NULL DEFAULT 'NhanVien',

    MaNV              VARCHAR(10)       NULL,       -- co the NULL: tai khoan khong gan ho so NV (vd: admin ky thuat)
    TrangThai         ENUM('HoatDong','KhoaTaiKhoan') NOT NULL DEFAULT 'HoatDong',

    NgayTao           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LanDangNhapCuoi   DATETIME          NULL,

    CONSTRAINT UQ_TaiKhoan_TenDangNhap UNIQUE (TenDangNhap),

    CONSTRAINT FK_TaiKhoan_NhanVien
        FOREIGN KEY (MaNV) REFERENCES NHANVIEN(MaNV)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- DU LIEU MAU (tuy chon, xoa neu khong can)
-- ============================================================

-- PHONGBAN: IT, KD=Kinh doanh, NS=Nhan su, KT=Ke toan, MKT=Marketing, CSKH=Cham soc khach hang
INSERT INTO PHONGBAN (MaPB, TenPhongBan, MoTa) VALUES
    ('IT',   'IT',                      'Phong Cong nghe thong tin'),
    ('KD',   'Kinh doanh',               'Phong Kinh doanh'),
    ('NS',   'Nhan su',                  'Phong Nhan su'),
    ('KT',   'Ke toan',                  'Phong Ke toan - Tai chinh'),
    ('MKT',  'Marketing',                'Phong Marketing'),
    ('CSKH', 'Cham soc khach hang',      'Phong Cham soc khach hang');

-- CHUCVU: NV=Nhan vien, TN=Truong nhom, QL=Quan ly, TP=Truong phong, GD=Giam doc
INSERT INTO CHUCVU (MaCV, TenChucVu, MoTa) VALUES
    ('NV', 'Nhan vien',    'Nhan vien thuong'),
    ('TN', 'Truong nhom',  'Truong nhom / Team lead'),
    ('QL', 'Quan ly',      'Quan ly phong ban'),
    ('TP', 'Truong phong', 'Truong phong'),
    ('GD', 'Giam doc',     'Giam doc');

-- Tai khoan admin mac dinh de test dang nhap (SEED DATA THU CONG).
-- Luu y quan trong ve luong hoat dong:
--   - Day la du lieu MOI DUOC TAO SAN de test, KHONG phai nguoi dung
--     tu tay go chuoi hash nay.
--   - Trong thuc te, nguoi dung CHI go mat khau thuong (vd "123456") o
--     man hinh Dang ky/Doi mat khau. Chinh CODE JAVA (BCrypt.hashpw)
--     se tu dong bam no ra hash NGAY TRUOC KHI chay lenh INSERT that
--     su (qua PreparedStatement), nguoi dung khong bao gio thay hash.
--   - Rieng dong INSERT ben duoi la ngoai le: vi luc chay script nay
--     chua co man hinh Register hoat dong, nen minh phai TU TAY tinh
--     truoc hash cua chuoi "123456" (bang thu vien bcrypt) roi dan
--     thang vao day, chi de co san 1 tai khoan test dang nhap ngay.
-- MatKhauHash duoi day la hash BCrypt THAT cua chuoi "123456", tao bang
-- chinh thu vien org.mindrot.jbcrypt (cost factor 12, dang $2a$) - dung
-- thu vien nay vi code Java (TaiKhoanDAO) dung org.mindrot.jbcrypt de
-- kiem tra, va thu vien nay CHI doc duoc hash dang $2a$ (khong doc duoc
-- $2b$/$2y$ do cac thu vien bcrypt khac sinh ra).
-- Dang nhap bang mat khau "123456" se kiem tra dung.
INSERT INTO TAIKHOAN (TenDangNhap, MatKhauHash, VaiTro, MaNV, TrangThai) VALUES
    ('admin@hrms.com', '$2a$12$VbqHS2G.vQlSzoa12yCZquH.rdJh4Zt3C3XTppy9oVZ2VcPblOsO2', 'Admin', NULL, 'HoatDong');

-- ------------------------------------------------------------
-- 20 nhan vien mau de test Liet ke/Tim kiem/Loc/Dashboard.
-- Dat ten theo mau Ho + Dem + 1 chu cai (A-T) de de phan biet trong demo.
-- MaNV = MaPB + so thu tu 3 chu so, dem RIENG cho tung phong ban (dung
-- quy uoc nhu NhanVienDAO.insert() se tu sinh khi them nhan vien moi).
-- ------------------------------------------------------------
INSERT INTO NHANVIEN (MaNV, HoTen, NgaySinh, GioiTinh, SoCCCD, SoDienThoai, Email, DiaChi,
                       MaPB, MaCV, NgayVaoLam, LoaiHopDong, MucLuongCoBan, TrangThai) VALUES
    ('IT001',   'Nguyen Van A',  '1990-01-10', 'Nam', '079200000001', '0912340001', 'nv01@congty.vn', 'Quan 1, TP.HCM',        'IT',   'NV', '2022-03-01', 'Chính thức', 12000000, 'DangLamViec'),
    ('KD001',   'Tran Thi B',    '1991-02-15', 'Nữ',  '079200000002', '0912340002', 'nv02@congty.vn', 'Cau Giay, Ha Noi',      'KD',   'NV', '2022-04-10', 'Chính thức', 11000000, 'DangLamViec'),
    ('NS001',   'Le Van C',      '1989-03-20', 'Nam', '079200000003', '0912340003', 'nv03@congty.vn', 'Hai Chau, Da Nang',     'NS',   'TN', '2021-06-15', 'Chính thức', 15000000, 'DangLamViec'),
    ('KT001',   'Pham Thi D',    '1993-04-25', 'Nữ',  '079200000004', '0912340004', 'nv04@congty.vn', 'Quan 3, TP.HCM',        'KT',   'NV', '2022-07-01', 'Chính thức', 11500000, 'DangLamViec'),
    ('MKT001',  'Hoang Van E',   '1988-05-30', 'Nam', '079200000005', '0912340005', 'nv05@congty.vn', 'Dong Da, Ha Noi',       'MKT',  'NV', '2021-09-12', 'Chính thức', 12500000, 'DangLamViec'),
    ('CSKH001', 'Huynh Thi F',   '1994-06-05', 'Nữ',  '079200000006', '0912340006', 'nv06@congty.vn', 'Ninh Kieu, Can Tho',    'CSKH', 'NV', '2023-01-20', 'Thử việc',     8000000,  'DangLamViec'),
    ('IT002',   'Phan Van G',    '1987-07-10', 'Nam', '079200000007', '0912340007', 'nv07@congty.vn', 'Quan 7, TP.HCM',        'IT',   'TN', '2020-05-05', 'Chính thức', 16000000, 'DangLamViec'),
    ('KD002',   'Vu Thi H',      '1992-08-15', 'Nữ',  '079200000008', '0912340008', 'nv08@congty.vn', 'Thanh Xuan, Ha Noi',    'KD',   'QL', '2019-11-01', 'Chính thức', 20000000, 'DangLamViec'),
    ('NS002',   'Vo Van I',      '1990-09-20', 'Nam', '079200000009', '0912340009', 'nv09@congty.vn', 'Binh Thanh, TP.HCM',    'NS',   'NV', '2023-02-14', 'Chính thức', 11000000, 'DangLamViec'),
    ('KT002',   'Dang Thi J',    '1995-10-25', 'Nữ',  '079200000010', '0912340010', 'nv10@congty.vn', 'Son Tra, Da Nang',      'KT',   'NV', '2023-03-01', 'Thử việc',     8500000,  'DangLamViec'),
    ('MKT002',  'Bui Van K',     '1986-11-30', 'Nam', '079200000011', '0912340011', 'nv11@congty.vn', 'Quan 5, TP.HCM',        'MKT',  'TP', '2018-08-08', 'Chính thức', 22000000, 'DangLamViec'),
    ('CSKH002', 'Do Thi L',      '1993-12-05', 'Nữ',  '079200000012', '0912340012', 'nv12@congty.vn', 'Cau Giay, Ha Noi',      'CSKH', 'NV', '2022-10-10', 'Chính thức', 10500000, 'DangLamViec'),
    ('IT003',   'Ho Van M',      '1991-01-08', 'Nam', '079200000013', '0912340013', 'nv13@congty.vn', 'Hai Chau, Da Nang',     'IT',   'NV', '2023-04-01', 'Chính thức', 12000000, 'DangLamViec'),
    ('KD003',   'Ngo Thi N',     '1989-02-14', 'Nữ',  '079200000014', '0912340014', 'nv14@congty.vn', 'Quan 10, TP.HCM',       'KD',   'NV', '2021-12-01', 'Chính thức', 11000000, 'DangLamViec'),
    ('NS003',   'Duong Van O',   '1990-03-18', 'Khác','079200000015', '0912340015', 'nv15@congty.vn', 'Dong Da, Ha Noi',       'NS',   'NV', '2022-05-05', 'Chính thức', 10500000, 'DangLamViec'),
    ('KT003',   'Ly Thi P',      '1994-04-22', 'Nữ',  '079200000016', '0912340016', 'nv16@congty.vn', 'Ninh Kieu, Can Tho',    'KT',   'TN', '2020-09-09', 'Chính thức', 14000000, 'DangLamViec'),
    ('MKT003',  'Dinh Van Q',    '1987-05-26', 'Nam', '079200000017', '0912340017', 'nv17@congty.vn', 'Quan 1, TP.HCM',        'MKT',  'NV', '2023-05-15', 'Thời vụ',       7500000,  'DangLamViec'),
    ('CSKH003', 'Trinh Thi R',   '1992-06-30', 'Nữ',  '079200000018', '0912340018', 'nv18@congty.vn', 'Thanh Xuan, Ha Noi',    'CSKH', 'QL', '2019-07-07', 'Chính thức', 19000000, 'DangLamViec'),
    ('IT004',   'Mai Van S',     '1988-07-04', 'Nam', '079200000019', '0912340019', 'nv19@congty.vn', 'Binh Thanh, TP.HCM',    'IT',   'GD', '2017-01-01', 'Chính thức', 35000000, 'DangLamViec'),
    ('KD004',   'Dao Thi T',     '1995-08-09', 'Nữ',  '079200000020', '0912340020', 'nv20@congty.vn', 'Son Tra, Da Nang',      'KD',   'NV', '2023-06-01', 'Chính thức', 10000000, 'DangLamViec');
