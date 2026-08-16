package model;

public class ChucVu implements DanhMuc {
    private int maCV;
    private String tenChucVu;
    private String moTa;

    public ChucVu() {
    }

    public ChucVu(int maCV, String tenChucVu, String moTa) {
        this.maCV = maCV;
        this.tenChucVu = tenChucVu;
        this.moTa = moTa;
    }

    public int getMaCV() {
        return maCV;
    }

    public void setMaCV(int maCV) {
        this.maCV = maCV;
    }

    public String getTenChucVu() {
        return tenChucVu;
    }

    public void setTenChucVu(String tenChucVu) {
        this.tenChucVu = tenChucVu;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    @Override
    public int getId() {
        return maCV;
    }

    @Override
    public String getTen() {
        return tenChucVu;
    }

    @Override
    public String toString() {
        return tenChucVu;
    }
}
