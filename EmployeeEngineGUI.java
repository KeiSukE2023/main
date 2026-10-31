import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

// 1. DOMAIN MODELS

class Employee {
    private String id;
    private String name;
    private String jobType;
    private double basicSalary;

    public Employee(String id, String name, String jobType, double basicSalary) {
        this.id = id;
        this.name = name;
        this.jobType = jobType;
        this.basicSalary = basicSalary;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getJobType() { return jobType; }
    public double getBasicSalary() { return basicSalary; }
}

class PayrollRecord {
    private String employeeId;
    private String employeeName;
    private double basicSalary;
    private double jobDeduction;
    private double letterDeduction;
    private double overtimePay;
    private double netPay;
    private boolean letterSubmitted;

    public PayrollRecord(String employeeId, String employeeName, double basicSalary, 
                         double jobDeduction, double letterDeduction, double overtimePay, 
                         double netPay, boolean letterSubmitted) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.basicSalary = basicSalary;
        this.jobDeduction = jobDeduction;
        this.letterDeduction = letterDeduction;
        this.overtimePay = overtimePay;
        this.netPay = netPay;
        this.letterSubmitted = letterSubmitted;
    }

    public Object[] toRowData() {
        return new Object[]{
            employeeId,
            employeeName,
            "P" + formatMoney(basicSalary),
            "-P" + formatMoney(jobDeduction),
            letterSubmitted ? "Submitted (Waived)" : "Missing (-5% Penalty)",
            "-P" + formatMoney(letterDeduction),
            "+P" + formatMoney(overtimePay),
            "P" + formatMoney(netPay)
        };
    }

    public String getFormattedSummary() {
        return String.format(
            "Employee ID: %s%nEmployee Name: %s%nBasic Salary: P%s%nJob Deduction: -P%s%nLetter Status: %s%nLetter Deduction: -P%s%nOvertime Pay: +P%s%nNet Pay: P%s",
            employeeId,
            employeeName,
            formatMoney(basicSalary),
            formatMoney(jobDeduction),
            letterSubmitted ? "Submitted (Waived)" : "Missing (-5% Penalty)",
            formatMoney(letterDeduction),
            formatMoney(overtimePay),
            formatMoney(netPay)
        );
    }

    private String formatMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toString();
    }
}


// 2. MAIN SWING GUI APPLICATION

public class EmployeeEngineGUI extends JFrame {

    // Central Data Structures (ArrayList & HashMap)
    private final ArrayList<Employee> employeeList = new ArrayList<>();
    private final ArrayList<PayrollRecord> payrollRecords = new ArrayList<>();
    private final HashMap<String, Double> jobDeductionRules = new HashMap<>();

    // UI Tables & Models
    private DefaultTableModel empTableModel;
    private DefaultTableModel payrollTableModel;
    private JTable empTable;
    private JTable payrollTable;

    // Form Inputs
    private JTextField txtId, txtName, txtSalary, txtOvertime;
    private JComboBox<String> comboJobType;
    private JComboBox<String> comboEmployeeSelect;
    private JCheckBox chkLetterSubmitted;

    public EmployeeEngineGUI() {
        setTitle("EmployeeEngine - Enterprise Employee & Payroll System (GUI)");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize Job Rules (HashMap)
        jobDeductionRules.put("Manager", 0.08);   // 8%
        jobDeductionRules.put("Developer", 0.05); // 5%
        jobDeductionRules.put("Staff", 0.03);     // 3%

        // Pre-populate sample data
        
        // Create Main Tabbed Pane (Matches Architecture Sections in Documentation)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Employee Management", createEmployeePanel());
        tabbedPane.add("Payroll Processing", createPayrollPanel());
        tabbedPane.add("Payroll Summary Records", createSummaryPanel());

        add(tabbedPane);
        refreshEmployeeDropdown();
        refreshEmployeeTable();
    }

    // --- TAB 1: EMPLOYEE MANAGEMENT ---
    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form inputs (West)
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Add / Remove Employee"));

        formPanel.add(new JLabel("Employee ID:"));
        txtId = new JTextField("EMP-003");
        formPanel.add(txtId);

        formPanel.add(new JLabel("Full Name:"));
        txtName = new JTextField();
        formPanel.add(txtName);

        formPanel.add(new JLabel("Job Position:"));
        comboJobType = new JComboBox<>(jobDeductionRules.keySet().toArray(new String[0]));
        formPanel.add(comboJobType);

        formPanel.add(new JLabel("Basic Salary (P):"));
        txtSalary = new JTextField();
        formPanel.add(txtSalary);

        JButton btnAdd = new JButton("Add Employee");
        JButton btnRemove = new JButton("Delete Selected");
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(btnAdd);
        btnPanel.add(btnRemove);

        JPanel westContainer = new JPanel(new BorderLayout());
        westContainer.add(formPanel, BorderLayout.CENTER);
        westContainer.add(btnPanel, BorderLayout.SOUTH);

        // Table (Center)
        empTableModel = new DefaultTableModel(new String[]{"ID", "Name", "Job Position", "Basic Salary (P)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        empTable = new JTable(empTableModel);
        JScrollPane scrollPane = new JScrollPane(empTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("<Employee> Master List"));

        panel.add(westContainer, BorderLayout.WEST);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Actions
        btnAdd.addActionListener(e -> addEmployee());
        btnRemove.addActionListener(e -> removeEmployee());

        return panel;
    }

    //  TAB 2: PAYROLL PROCESSING 
    private JPanel createPayrollPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Compute Attendance, Letters & Payroll Formula"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Select Employee:"), gbc);
        comboEmployeeSelect = new JComboBox<>();
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(comboEmployeeSelect, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Overtime Hours Worked:"), gbc);
        txtOvertime = new JTextField("0");
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(txtOvertime, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Excuse Letter Status:"), gbc);
        chkLetterSubmitted = new JCheckBox("Excuse Letter Submitted (Waives 5% penalty)");
        chkLetterSubmitted.setSelected(true);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(chkLetterSubmitted, gbc);

        JButton btnCompute = new JButton("Compute & Generate Payslip");
        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(btnCompute, gbc);

        btnCompute.addActionListener(e -> computePayroll());

        return panel;
    }

    // --- TAB 3: PAYROLL SUMMARIES ---
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        payrollTableModel = new DefaultTableModel(new String[]{
            "ID", "Name", "Basic Salary", "Job Deduction", "Letter Status", "Letter Penalty", "Overtime", "Net Pay"
        }, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        payrollTable = new JTable(payrollTableModel);
        JScrollPane scrollPane = new JScrollPane(payrollTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("ArrayList<PayrollRecord> Archive"));

        panel.add(scrollPane, BorderLayout.CENTER);

        JButton btnExport = new JButton("Export Payroll Summary");
        btnExport.addActionListener(e -> exportPayrollToTextFileGUI());
        panel.add(btnExport, BorderLayout.SOUTH);
        return panel;
    }

    private void exportPayrollToTextFileGUI() {
        if (payrollRecords.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No payroll records available to export.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter("output.txt"))) {
            writer.println("==================================================");
            writer.println("           EMPENGINE PAYROLL MASTER REPORT        ");
            writer.println("==================================================");
            writer.println();

            for (PayrollRecord record : payrollRecords) {
                writer.println(record.getFormattedSummary());
                writer.println();
            }

            JOptionPane.showMessageDialog(this, "Payroll report exported successfully to output.txt!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error exporting file: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- LOGIC METHODS ---
    private void addEmployee() {
        try {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();
            String jobType = (String) comboJobType.getSelectedItem();
            double salary = Double.parseDouble(txtSalary.getText().trim());

            if (id.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all employee fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            employeeList.add(new Employee(id, name, jobType, salary));
            refreshEmployeeTable();
            refreshEmployeeDropdown();
            JOptionPane.showMessageDialog(this, "Employee successfully added to the system.");
            
            txtName.setText("");
            txtSalary.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Salary must be a valid numeric value.", "Format Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeEmployee() {
        int selectedRow = empTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee row to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String targetId = empTableModel.getValueAt(selectedRow, 0).toString();
        employeeList.removeIf(e -> e.getId().equalsIgnoreCase(targetId));
        refreshEmployeeTable();
        refreshEmployeeDropdown();
        JOptionPane.showMessageDialog(this, "Employee removed successfully.");
    }

    private void computePayroll() {
        if (employeeList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No employees available to compute payroll.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selectedSelection = (String) comboEmployeeSelect.getSelectedItem();
        if (selectedSelection == null) return;

        String empId = selectedSelection.split(" - ")[0];
        Employee target = null;
        for (Employee e : employeeList) {
            if (e.getId().equalsIgnoreCase(empId)) {
                target = e;
                break;
            }
        }

        if (target == null) return;

        try {
            double otHours = Double.parseDouble(txtOvertime.getText().trim());
            boolean letterSubmitted = chkLetterSubmitted.isSelected();

            double basic = target.getBasicSalary();
            
            // 1. HashMap Job Deduction Percentage Formula
            double jobPercentage = jobDeductionRules.get(target.getJobType());
            double jobDeduction = basic * jobPercentage;

            // 2. Letter Submission Rule Check (-5% penalty if not submitted)
            double letterDeduction = 0.0;
            if (!letterSubmitted) {
                letterDeduction = basic * 0.05;
            }

            // 3. Overtime Pay Calculation (Assuming 22 working days, 8 hrs/day)
            double hourlyRate = (basic / 22.0) / 8.0;
            double overtimePay = otHours * (hourlyRate * 1.25);

            // 4. Net Pay Calculation
            double totalDeductions = jobDeduction + letterDeduction;
            double netPay = (basic + overtimePay) - totalDeductions;

            // Save record to ArrayList
            PayrollRecord record = new PayrollRecord(
                target.getId(), target.getName(), basic, jobDeduction, 
                letterDeduction, overtimePay, netPay, letterSubmitted
            );
            payrollRecords.add(record);
            payrollTableModel.addRow(record.toRowData());

            // Display Interactive Breakdown Dialog
            JOptionPane.showMessageDialog(this, 
                String.format("Payroll Computed Successfully!\n\nEmployee: %s\nBasic Salary: P%.2f\nJob Deduction (%s): -P%.2f\nLetter Penalty: -P%.2f\nOvertime Pay: +P%.2f\n----------------------------------\nNET PAY: P%.2f",
                target.getName(), basic, target.getJobType(), jobDeduction, letterDeduction, overtimePay, netPay),
                "Payslip Generated", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Overtime hours must be a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshEmployeeTable() {
        empTableModel.setRowCount(0);
        for (Employee e : employeeList) {
            empTableModel.addRow(new Object[]{e.getId(), e.getName(), e.getJobType(), "P" + e.getBasicSalary()});
        }
    }

    private void refreshEmployeeDropdown() {
        comboEmployeeSelect.removeAllItems();
        for (Employee e : employeeList) {
            comboEmployeeSelect.addItem(e.getId() + " - " + e.getName() + " (" + e.getJobType() + ")");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EmployeeEngineGUI().setVisible(true));
    }
}