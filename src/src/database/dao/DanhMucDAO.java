package database.dao;

import model.DanhMuc;

import java.sql.SQLException;
import java.util.List;

public interface DanhMucDAO {

    List<? extends DanhMuc> findAll() throws SQLException;

    void insert(String ma, String ten, String moTa) throws SQLException;

    void update(String id, String ten, String moTa) throws SQLException;

    void delete(String id) throws SQLException;

    int countUsage(String id) throws SQLException;
}
