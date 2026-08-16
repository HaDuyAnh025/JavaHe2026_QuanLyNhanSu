package model;

public class ChucVu implements DanhMuc {
    private String maCV;
    private String tenChucVu;
    private String moTa;

    public ChucVu() {
    }

    public ChucVu(String maCV, String tenChucVu, String moTa) {
        this.maCV = maCV;
        this.tenChucVu = tenChucVu;
        this.moTa = moTa;
    }

    public String getMaCV() {
        return maCV;
    }

    public void setMaCV(String maCV) {
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
    public String getId() {
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
