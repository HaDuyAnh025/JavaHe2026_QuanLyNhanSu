package database.dao;

import model.DanhMuc;

import java.sql.SQLException;
import java.util.List;

/** PhongBanDAO va ChucVuDAO cung implement interface nay de dung chung CategoryManagementPanel. */
public interface DanhMucDAO {

    List<? extends DanhMuc> findAll() throws SQLException;

    int insert(String ten, String moTa) throws SQLException;

    void update(int id, String ten, String moTa) throws SQLException;

    void delete(int id) throws SQLException;

    /** So nhan vien dang thuoc danh muc nay (de canh bao truoc khi xoa). */
    int countUsage(int id) throws SQLException;
}
