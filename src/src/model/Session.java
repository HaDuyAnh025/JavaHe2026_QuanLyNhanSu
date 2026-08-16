package model;

/**
 * Giu thong tin tai khoan dang dang nhap trong phien lam viec hien tai cua ung dung.
 */
public class Session {

    private static TaiKhoan currentAccount;

    private Session() {
    }

    public static TaiKhoan getCurrentAccount() {
        return currentAccount;
    }

    public static void setCurrentAccount(TaiKhoan account) {
        currentAccount = account;
    }

    public static void clear() {
        currentAccount = null;
    }
}
