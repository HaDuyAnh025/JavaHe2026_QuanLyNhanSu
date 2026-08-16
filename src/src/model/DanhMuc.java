package model;

/**
 * PhongBan va ChucVu co cung dang du lieu (id, ten, mo ta) nen dung chung
 * interface nay de CategoryManagementPanel co the hien thi/CRUD ca 2 bang
 * 1 UI duy nhat, khong phai viet trung 2 lan.
 */
public interface DanhMuc {
    String getId();

    String getTen();

    String getMoTa();
}
