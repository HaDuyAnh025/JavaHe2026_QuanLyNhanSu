package model;

public class PhongBan implements DanhMuc {
    private String maPB;
    private String tenPhongBan;
    private String moTa;

    public PhongBan() {
    }

    public PhongBan(String maPB, String tenPhongBan, String moTa) {
        this.maPB = maPB;
        this.tenPhongBan = tenPhongBan;
        this.moTa = moTa;
    }

    public String getMaPB() {
        return maPB;
    }

    public void setMaPB(String maPB) {
        this.maPB = maPB;
    }

    public String getTenPhongBan() {
        return tenPhongBan;
    }

    public void setTenPhongBan(String tenPhongBan) {
        this.tenPhongBan = tenPhongBan;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    @Override
    public String getId() {
        return maPB;
    }

    @Override
    public String getTen() {
        return tenPhongBan;
    }

    @Override
    public String toString() {
        return tenPhongBan;
    }
}
