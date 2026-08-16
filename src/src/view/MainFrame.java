package view;

import view.panels.AddEmployeesPanel;
import view.panels.DashboardPanel;
import view.panels.ListEmployeesPanel;

import javax.swing.*;
import java.awt.*;

/**
 * MainFrame la khung chinh duy nhat cua ung dung.
 * Chua sidebar + top bar dung chung, phan noi dung (contentPanel) dung
 * CardLayout de chuyen doi giua cac man hinh (khong mo JFrame moi).
 *
 * QUAN TRONG: cac field ben duoi PHAI trung ten voi thuoc tinh "binding"
 * trong MainFrame.form de IntelliJ GUI Designer tu dong gan component
 * khi build (khong can goi ham khoi tao UI thu cong).
 *
 * GHI CHU: man hinh "Tim kiem nang cao" da duoc GOP vao trang "Quan ly"
 * (xem ListEmployeesPanel) duoi dang bang loc an/hien ben trai khi bam
 * nut "Loc". Vi vay MainFrame khong con card/nut rieng cho no nua.
 */
public class MainFrame extends JFrame {

    // ===== Sinh tu MainFrame.form (KHONG xoa/doi ten neu khong sua .form) =====
    private JPanel rootPanel;
    private JPanel pnlSidebar;
    private JButton logoButton;
    private JButton avatarButton;
    private JButton trợGiúpButton;
    private JButton thôngBáoButton;
    private JTextField tìmKiếmNhanhTextField;

    private JButton tổngQuanButton;          // Dashboard
    private JButton btnQuanLyNhanVien;       // "Quan ly" (danh sach + tim kiem nhanh + loc nang cao gop chung)
    private JButton phânQuyềnButton;         // Phan quyen (chua co man hinh rieng)
    private JButton càiĐặtButton;            // Cai dat (chua co man hinh rieng)
    private JButton đăngXuấtButton;

    private JPanel contentPanel;             // container CardLayout

    // ===== Cac card (ten dung de goi CardLayout.show) =====
    public static final String CARD_DASHBOARD = "DASHBOARD";
    public static final String CARD_LIST_EMPLOYEES = "LIST_EMPLOYEES";
    public static final String CARD_ADD_EMPLOYEE = "ADD_EMPLOYEE";

    private CardLayout cardLayout;

    private DashboardPanel dashboardPanel;
    private ListEmployeesPanel listEmployeesPanel;
    private AddEmployeesPanel addEmployeesPanel;

    // Nut nav dang active, dung de bat/tat style highlight
    private JButton activeNavButton;

    public MainFrame() {
        setTitle("HR Management");
        setContentPane(rootPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);

        setupContentCards();
        setupNavigation();
        setupTopBar();

        // Man hinh mac dinh khi mo app
        showCard(CARD_DASHBOARD, tổngQuanButton);
    }

    /** Khoi tao CardLayout va them tat ca cac panel noi dung vao contentPanel. */
    private void setupContentCards() {
        cardLayout = new CardLayout();
        contentPanel.setLayout(cardLayout);

        dashboardPanel = new DashboardPanel();
        listEmployeesPanel = new ListEmployeesPanel();
        addEmployeesPanel = new AddEmployeesPanel();

        contentPanel.add(dashboardPanel.getRootPanel(), CARD_DASHBOARD);
        contentPanel.add(listEmployeesPanel.getRootPanel(), CARD_LIST_EMPLOYEES);
        contentPanel.add(addEmployeesPanel.getRootPanel(), CARD_ADD_EMPLOYEE);

        // Cho phep cac panel con dieu huong nguoc lai qua MainFrame
        listEmployeesPanel.setOnAddEmployeeRequested(() -> showCard(CARD_ADD_EMPLOYEE, btnQuanLyNhanVien));
        addEmployeesPanel.setOnSavedOrCancelled(() -> {
            listEmployeesPanel.reloadData(); // ve lai trang thai danh sach binh thuong
            showCard(CARD_LIST_EMPLOYEES, btnQuanLyNhanVien);
        });
    }

    /** Gan su kien cho cac nut o sidebar de chuyen card. */
    private void setupNavigation() {
        tổngQuanButton.addActionListener(e -> showCard(CARD_DASHBOARD, tổngQuanButton));

        btnQuanLyNhanVien.addActionListener(e -> {
            // Bam thang vao "Quan ly" tu sidebar -> ve trang thai danh sach binh thuong
            tìmKiếmNhanhTextField.setText("");
            listEmployeesPanel.exitSearchMode();
            showCard(CARD_LIST_EMPLOYEES, btnQuanLyNhanVien);
        });

        phânQuyềnButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Man hinh Phan quyen dang duoc xay dung."));
        càiĐặtButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Man hinh Cai dat dang duoc xay dung."));

        đăngXuấtButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Ban co chac muon dang xuat?", "Dang xuat",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();                       // dong man hinh chinh
                new Login().setVisible(true);    // ve lai man hinh dang nhap
            }
        });
    }

    /**
     * Gan su kien cho thanh ngang tren (search nhanh, thong bao, tro giup, avatar).
     * "Tim kiem nhanh" gio tim NGAY trong trang "Quan ly" (khong con trang Tim kiem rieng):
     *  - Co tu khoa  -> chuyen sang card Quan ly, hien "Ket qua tim kiem" (nhu anh 5)
     *  - Xoa tu khoa -> ve lai trang thai danh sach binh thuong (nhu anh 4)
     */
    private void setupTopBar() {
        logoButton.addActionListener(e -> showCard(CARD_DASHBOARD, tổngQuanButton));

        tìmKiếmNhanhTextField.addActionListener(e -> performQuickSearch());

        thôngBáoButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Chua co thong bao moi."));
        trợGiúpButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Lien he quan tri vien de duoc ho tro."));
        avatarButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "TODO: menu tai khoan (doi mat khau, dang xuat...)"));
    }

    private void performQuickSearch() {
        String keyword = tìmKiếmNhanhTextField.getText().trim();
        showCard(CARD_LIST_EMPLOYEES, btnQuanLyNhanVien);
        if (keyword.isEmpty()) {
            listEmployeesPanel.exitSearchMode();
        } else {
            listEmployeesPanel.searchByKeyword(keyword);
        }
    }

    /** Chuyen sang card chi dinh va cap nhat trang thai highlight cua sidebar. */
    private void showCard(String cardName, JButton navButton) {
        cardLayout.show(contentPanel, cardName);
        setActiveNavButton(navButton);
    }

    /** Bat highlight cho nut dang chon, tat highlight nut truoc do. */
    private void setActiveNavButton(JButton button) {
        if (activeNavButton != null) {
            activeNavButton.setSelected(false);
        }
        button.setSelected(true);
        activeNavButton = button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}