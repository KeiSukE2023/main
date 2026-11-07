import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.io.IOException;


class Employee{
    private String name;
    private String id;
    private String jobType;
    private double basicSalary;

    public Employee(String name,String id, String jobType,double basicSalary){
        this.name = name;
        this.id = id;
        this.jobType = jobType;
        this.basicSalary = basicSalary;


    }
    public String getName() {
        return name;
    }
    public String getId() {
        return id;
    }
    public String getJobType() {
        return jobType;
    }
    public double getBasicSalary() {
        return basicSalary;
    }

}
class PayrollRecord{
    private String employeeName;
    private String employeeId;
    private double basicSalary;
    private double jobDeduction;
    private double netPay;
    private boolean letterSubmitted;
    private double letterDeduction;
    private double overtimePay;

    public PayrollRecord(String employeeName, String employeeId, double basicSalary, double jobDeduction, double netPay, boolean letterSubmitted, double letterDeduction, double overtimePay) {
        this.employeeName = employeeName;
        this.employeeId = employeeId;
        this.basicSalary = basicSalary;
        this.jobDeduction = jobDeduction;
        this.netPay = netPay;
        this.letterSubmitted = letterSubmitted;
        this.letterDeduction = letterDeduction;
        this.overtimePay = overtimePay;
    }
    public Object[] toRowData(){
        return new Object[]{employeeId, employeeName, "P" + format(basicSalary), "-P" + format(jobDeduction),
                letterSubmitted ? "Submitted(Waived)" : "Missing(-5% Penalty)", "-P" + format(letterDeduction),
                "+P" + format(overtimePay), "P" + format(netPay)};

    }
    private String format(double value) {
        BigDecimal bd = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        return bd.toString();
    }
}


//MAIN GUI APPLICATION CLASS :3
public class EmployeeManagementGUI extends JFrame {
    // Central Data Structures (ArrayList & HashMap)
    private final ArrayList<Employee> employeeList = new ArrayList<>();
    private final ArrayList<PayrollRecord> payrollRecords = new ArrayList<>();
    private final HashMap<String, Double> jobDeductionRules = new HashMap<>();

// UI Tables & Models :3
    private DefaultTableModel empTableModel;
    private DefaultTableModel payrollTableModel;
    private JTable empTable;
    private JTable payrollTable;

// Form Inputs GUI components :3
    private JTextField txtId, txtName, txtSalary, txtOvertime;
    private JComboBox<String> comboJobType;
    private JComboBox<String> comboEmployeeSelect;
    private JCheckBox chkLetterSubmitted;
    private static final File PAYROLL_FILE = new File("Employee Generator Payslip.txt");
    private static final File EMPLOYEE_FILE = new File("EmployeeFILES.txt");

    public EmployeeManagementGUI(){
         setTitle("Employee Management System");
         setSize(900, 500);
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         setLocationRelativeTo(null);
         setLayout(new BorderLayout(15, 15));

         //Rules for job deduction :3
         jobDeductionRules.put("Manager", 0.08); //8% deduction for Manager
         jobDeductionRules.put("Developer", 0.05); //5% deduction for Developer
         jobDeductionRules.put("Staff", 0.03); //3% deduction for Staff


         // Create Main Tabbed Pane (Matches Architecture Sections in Documentation)
         JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Employee Management", createEmployeePanel());
        tabbedPane.add("Payroll Processing", createPayrollPanel());
        tabbedPane.add("Payroll Summary Records", createSummaryPanel());

        add(tabbedPane);
        loadEmployees();
        loadPayrollData();
        refreshEmployeeDropdown();
        refreshEmployeeTable();
    }
    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel inputPanel = new JPanel(new GridLayout(7, 1, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add / Remove Employee"));

        inputPanel.add(new JLabel("Employee ID:"));
        txtId = new JTextField();
        inputPanel.add(txtId);

       inputPanel.add(new JLabel("Full Name:"));
       txtName = new JTextField();
       inputPanel.add(txtName);

       inputPanel.add(new JLabel("Job Position:"));
       comboJobType = new JComboBox<>(new String[]{"Manager", "Developer", "Staff"});
       inputPanel.add(comboJobType);

       inputPanel.add(new JLabel("Basic Salary (P):"));
        txtSalary = new JTextField();
        inputPanel.add(txtSalary);

        JButton btnAdd = new JButton("Add Employee");
        JButton btnRemove = new JButton("Delete Employee");
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(btnAdd);
        btnPanel.add(btnRemove);

         JPanel westContainer = new JPanel(new BorderLayout());
         westContainer.add(inputPanel, BorderLayout.CENTER);
        westContainer.add(btnPanel, BorderLayout.SOUTH);

        empTableModel = new DefaultTableModel(new String[]{"ID", "Name", "Job Position", "Basic Salary (P)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table cells non-editable
            }
        };
        empTable = new JTable(empTableModel);
        JScrollPane scrollPane = new JScrollPane(empTable);
         scrollPane.setBorder(BorderFactory.createTitledBorder("Employee Master List"));

        panel.add(westContainer, BorderLayout.WEST);
        panel.add(scrollPane, BorderLayout.CENTER);

        btnAdd.addActionListener(e -> addEmployee());
        btnRemove.addActionListener(e -> removeEmployee());

        return panel;

    }
     private JPanel createPayrollPanel(){
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
     private JPanel createSummaryPanel(){
    JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
         payrollTableModel = new DefaultTableModel(new String[]{"ID", "Name", "Basic Salary", "Job Deduction", "Letter Status", "Letter Penalty", "Overtime", "Net Pay"}, 
         0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table cells non-editable
            }
         };
            payrollTable = new JTable(payrollTableModel);
        JScrollPane scrollPane = new JScrollPane(payrollTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("ArrayList PayrollRecord Archive"));

        panel.add(scrollPane, BorderLayout.CENTER);
        JButton btnExport = new JButton("Export Payroll Summary");
        btnExport.addActionListener(e -> SavePayrollToTextFileGUI());
        panel.add(btnExport, BorderLayout.SOUTH);
        return panel;
        
     }
    private void loadEmployees() {
        if (!EMPLOYEE_FILE.isFile()) {
            return;
        }
        try {
            for (String line : Files.readAllLines(EMPLOYEE_FILE.toPath(), StandardCharsets.UTF_8)) {
                String[] data = line.split("\\s*\\|\\s*", -1);
                if (data.length != 4) {
                    throw new IOException("Invalid employee record: " + line);
                }
                employeeList.add(new Employee(data[1], data[0], data[2], Double.parseDouble(data[3])));
            }
        } catch (IOException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error reading employee file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPayrollData() {
        if (!PAYROLL_FILE.isFile()) {
            return;
        }
        try {
            for (String line : Files.readAllLines(PAYROLL_FILE.toPath(), StandardCharsets.UTF_8)) {
                String[] data = line.split("\\s*\\|\\s*", -1);
                if (data.length != 8) {
                    throw new IOException("Invalid payroll record: " + line);
                }
                boolean letterSubmitted = data[4].contains("(Waived)");
                PayrollRecord record = new PayrollRecord(data[1], data[0], parseAmount(data[2]),
                        parseAmount(data[3]), parseAmount(data[7]), letterSubmitted,
                        parseAmount(data[5]), parseAmount(data[6]));
                payrollRecords.add(record);
                payrollTableModel.addRow(record.toRowData());
            }
        } catch (IOException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error reading payroll file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double parseAmount(String amount) {
        return Double.parseDouble(amount.trim().replace("P", "").replace("+", ""));
    }

    private void saveEmployees() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Employee employee : employeeList) {
            lines.add(String.format("%s | %s | %s | %.2f", employee.getId(), employee.getName(),
                    employee.getJobType(), employee.getBasicSalary()));
        }
        writeAtomically(EMPLOYEE_FILE, lines);
    }

    private void writeAtomically(File file, List<String> lines) throws IOException {
        Path target = file.toPath().toAbsolutePath();
        Path temporaryFile = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

     private void SavePayrollToTextFileGUI() {
        if (payrollRecords.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No payroll records available to export.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;

        }
        try {
            List<String> lines = new ArrayList<>();
            for (PayrollRecord payrollRecord : payrollRecords) {
                Object[] rowData = payrollRecord.toRowData();
                lines.add(String.format("%s | %s | %s | %s | %s | %s | %s | %s",
                        rowData[0], rowData[1], rowData[2], rowData[3],
                        rowData[4], rowData[5], rowData[6], rowData[7]));
            }
            writeAtomically(PAYROLL_FILE, lines);
            JOptionPane.showMessageDialog(this, "Payroll summary exported successfully to "
                    + PAYROLL_FILE.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error exporting payroll summary: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

        }

     }

     private void addEmployee() {
        String id = txtId.getText().trim();
        String name = txtName.getText().trim();
        String jobType = (String) comboJobType.getSelectedItem();
        String salaryStr = txtSalary.getText().trim();

        if (id.isEmpty() || name.isEmpty() || salaryStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double basicSalary;
        try {
            basicSalary = Double.parseDouble(salaryStr);
            if (basicSalary < 0) {
                throw new NumberFormatException("Negative salary");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive number for salary.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check for duplicate ID
        for (Employee emp : employeeList) {
            if (emp.getId().equals(id)) {
                JOptionPane.showMessageDialog(this, "Employee ID already exists. Please use a unique ID.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Employee newEmployee = new Employee(name, id, jobType, basicSalary);
        employeeList.add(newEmployee);
        try {
            saveEmployees();
        } catch (IOException ex) {
            employeeList.remove(newEmployee);
            JOptionPane.showMessageDialog(this, "Error saving employee file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshEmployeeDropdown();
        refreshEmployeeTable();
        JOptionPane.showMessageDialog(this, "Employee added successfully.");

    }

 private void removeEmployee() {
     int selectedRow = empTable.getSelectedRow();
      if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select an employee row to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
      }
       String targetId = empTableModel.getValueAt(selectedRow, 0).toString();
        int employeeIndex = -1;
        for (int i = 0; i < employeeList.size(); i++) {
            if (employeeList.get(i).getId().equals(targetId)) {
                employeeIndex = i;
                break;
            }
        }
        if (employeeIndex < 0) {
            return;
        }
        Employee removedEmployee = employeeList.remove(employeeIndex);
        try {
            saveEmployees();
        } catch (IOException ex) {
            employeeList.add(employeeIndex, removedEmployee);
            JOptionPane.showMessageDialog(this, "Error saving employee file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
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
        if (selectedSelection == null) 
            return;

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
            
// Payroll Calculation Logic
            double basic = target.getBasicSalary();double jobPercentage = jobDeductionRules.get(target.getJobType());
            double jobDeduction = basic * jobPercentage;

            double letterDeduction = 0.0;// Initialize letter deduction
             if (!letterSubmitted) {
                letterDeduction = basic * 0.05; // 5% penalty

             }
             //Overtime Pay Calculation: 1.25x the hourly rate for each hour worked beyond 8 hours/day (assuming 28 working days in a month)
            double hourlyRate = (basic / 28.0) / 8.0;
            double overtimePay = otHours * (hourlyRate * 1.25);

            // Net Pay Calculation
             double totalDeductions = jobDeduction + letterDeduction;
            double netPay = (basic + overtimePay) - totalDeductions;

             PayrollRecord record = new PayrollRecord(target.getName(), target.getId(), basic, jobDeduction, netPay, letterSubmitted, letterDeduction, overtimePay);
             payrollRecords.add(record);
            payrollTableModel.addRow(record.toRowData());

            JOptionPane.showMessageDialog(this,String.format("Payroll computed for %s (ID: %s)\nNet Pay: P%.2f", target.getName(), target.getId(), netPay), "Payroll Computed", JOptionPane.INFORMATION_MESSAGE);
            JOptionPane.showMessageDialog(this, "Payslip generated.", "Payslip Generated", JOptionPane.INFORMATION_MESSAGE);


         }catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for overtime hours.", "Error", JOptionPane.ERROR_MESSAGE);

        }

  }

  private void refreshEmployeeDropdown() {
        comboEmployeeSelect.removeAllItems();
        for (Employee emp : employeeList) {
            comboEmployeeSelect.addItem(emp.getId() + " - " + emp.getName());
        }
    }

    private void refreshEmployeeTable() {
        empTableModel.setRowCount(0); // Clear existing rows
        for (Employee emp : employeeList) {
            empTableModel.addRow(new Object[]{emp.getId(), emp.getName(), emp.getJobType(), String.format("P%.2f", emp.getBasicSalary())});
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EmployeeManagementGUI gui = new EmployeeManagementGUI();
            gui.setVisible(true);
        });
    }

}