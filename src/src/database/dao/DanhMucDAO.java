package database.dao;

import model.DanhMuc;

import java.sql.SQLException;
import java.util.List;

/** PhongBanDAO va ChucVuDAO cung implement interface nay de dung chung CategoryManagementPanel. */
public interface DanhMucDAO {

    List<? extends DanhMuc> findAll() throws SQLException;

    void insert(String ma, String ten, String moTa) throws SQLException;

    void update(String id, String ten, String moTa) throws SQLException;

    void delete(String id) throws SQLException;

    /** So nhan vien dang thuoc danh muc nay (de canh bao truoc khi xoa). */
    int countUsage(String id) throws SQLException;
}
