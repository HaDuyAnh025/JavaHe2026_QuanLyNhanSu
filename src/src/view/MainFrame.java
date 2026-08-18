package view;

import model.NhanVien;
import model.Session;
import model.TaiKhoan;
import view.panels.AccountManagementPanel;
import view.panels.AddEmployeesPanel;
import view.panels.CategoryManagementPanel;
import view.panels.DashboardPanel;
import view.panels.ListEmployeesPanel;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private JPanel rootPanel;
    private JPanel pnlSidebar;
    private JButton logoButton;
    private JButton avatarButton;

    private JButton tổngQuanButton;
    private JButton btnQuanLyNhanVien;
    private JButton phânQuyềnButton;
    private JButton càiĐặtButton;
    private JButton đăngXuấtButton;

    private JPanel contentPanel;

    public static final String CARD_DASHBOARD = "DASHBOARD";
    public static final String CARD_LIST_EMPLOYEES = "LIST_EMPLOYEES";
    public static final String CARD_ADD_EMPLOYEE = "ADD_EMPLOYEE";
    public static final String CARD_ACCOUNTS = "ACCOUNTS";
    public static final String CARD_CATEGORIES = "CATEGORIES";

    private CardLayout cardLayout;

    private DashboardPanel dashboardPanel;
    private ListEmployeesPanel listEmployeesPanel;
    private AddEmployeesPanel addEmployeesPanel;
    private AccountManagementPanel accountManagementPanel;
    private CategoryManagementPanel categoryManagementPanel;

    private JButton activeNavButton;

    private final TaiKhoan currentAccount = Session.getCurrentAccount();

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
        applyRolePermissions();

        showCard(CARD_DASHBOARD, tổngQuanButton);
    }

    private void applyRolePermissions() {
        boolean isAdmin = currentAccount != null && currentAccount.isAdmin();
        phânQuyềnButton.setVisible(isAdmin);
        càiĐặtButton.setVisible(isAdmin);
        avatarButton.setText(currentAccount == null ? "..." : currentAccount.getTenDangNhap());
    }

    private void setupContentCards() {
        cardLayout = new CardLayout();
        contentPanel.setLayout(cardLayout);

        dashboardPanel = new DashboardPanel();
        listEmployeesPanel = new ListEmployeesPanel();
        addEmployeesPanel = new AddEmployeesPanel();
        accountManagementPanel = new AccountManagementPanel();
        categoryManagementPanel = new CategoryManagementPanel();

        contentPanel.add(dashboardPanel.getRootPanel(), CARD_DASHBOARD);
        contentPanel.add(listEmployeesPanel.getRootPanel(), CARD_LIST_EMPLOYEES);
        contentPanel.add(addEmployeesPanel.getRootPanel(), CARD_ADD_EMPLOYEE);
        contentPanel.add(accountManagementPanel.getRootPanel(), CARD_ACCOUNTS);
        contentPanel.add(categoryManagementPanel.getRootPanel(), CARD_CATEGORIES);

        listEmployeesPanel.setOnAddEmployeeRequested(() -> {
            addEmployeesPanel.resetToAddMode();
            showCard(CARD_ADD_EMPLOYEE, btnQuanLyNhanVien);
        });
        listEmployeesPanel.setOnEditEmployeeRequested((NhanVien nv) -> {
            addEmployeesPanel.loadForEdit(nv);
            showCard(CARD_ADD_EMPLOYEE, btnQuanLyNhanVien);
        });
        addEmployeesPanel.setOnSavedOrCancelled(() -> {
            listEmployeesPanel.reloadData();
            showCard(CARD_LIST_EMPLOYEES, btnQuanLyNhanVien);
        });
    }

    private void setupNavigation() {
        tổngQuanButton.addActionListener(e -> showCard(CARD_DASHBOARD, tổngQuanButton));

        btnQuanLyNhanVien.addActionListener(e -> {
            listEmployeesPanel.exitSearchMode();
            showCard(CARD_LIST_EMPLOYEES, btnQuanLyNhanVien);
        });

        phânQuyềnButton.addActionListener(e -> showCard(CARD_ACCOUNTS, phânQuyềnButton));
        càiĐặtButton.addActionListener(e -> showCard(CARD_CATEGORIES, càiĐặtButton));

        đăngXuấtButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Ban co chac muon dang xuat?", "Dang xuat",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                Session.clear();
                dispose();
                new Login().setVisible(true);
            }
        });
    }

    private void setupTopBar() {
        logoButton.addActionListener(e -> showCard(CARD_DASHBOARD, tổngQuanButton));

        avatarButton.addActionListener(e -> {
            String info = currentAccount == null ? "Chưa đăng nhập"
                    : "Tài khoản: " + currentAccount.getTenDangNhap() + "\nVai trò: " + currentAccount.getVaiTro();
            JOptionPane.showMessageDialog(this, info, "Thông tin tài khoản", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void showCard(String cardName, JButton navButton) {
        cardLayout.show(contentPanel, cardName);
        setActiveNavButton(navButton);
    }

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