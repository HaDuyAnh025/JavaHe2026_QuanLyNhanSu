package view;

import javax.swing.*;

/**
 * Diem khoi dau (entry point) cua ung dung.
 * Luong man hinh: Main -> Login -> (Dashboard trong MainFrame) -> Dang xuat -> Login ...
 *
 * Chay class nay (chay ham main) de bat dau ung dung, thay vi chay
 * truc tiep Login.java hay MainFrame.java.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}