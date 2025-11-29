import java.sql.*;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateFormatter;

public class Billing {

    private Connection orderConn;
    private Connection billingConn;
    private Scanner sc = new Scanner(System.in);

    private String billId;
    private String orderId;
    //private int quantity;
    //private double price;
    //private double totalPrice;
    private double grandTotal;
    private String paymentMethod;
    private String billingDate;

    public Billing(){
        initializeOrderConnection();
        initializeBillingConnection();

        if (orderConn == null || billingConn == null){
            System.out.println("Database connection failed.");
            System.exit(1);
        }
    }

    private void initializeOrderConnection(){
        try{
            this.conn = DrverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3306/order_list",
                    "root",
                    "UoW192411@"
            );
            System.out.println("Order database connected!");
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
            this.orderConn = null;
        }
    }

    private void initializeBillingConnection() {
        try {
            this.bilingConn = DriverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3306/billing",
                    "root",
                    "UoW192411@"
            );
            billingConn.setAutoCommit(false);
            System.out.println("Billing database connected!");
        } catch (SQLException e) {
            System.out.println("Billing database connection failed: " +e.getMessage());
            this.billingConn = null;
        }
    }

    //Generate billID
    private String generateBillId() {
        String billId = "B001";
        String sql = "SELECT bill_id FROM billing ORDER BY bill_id DESC LIMIT 1";

        try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String lastId = rs.getString("bill_id");
                int number = Integer.parseInt(lastId.substring(1))+1;
                billId = String.format("B%01d", number);
            }
        }catch (SQLException e) {
            System.out.println("Error generating Bill ID: " +e.getMessage());
        }
        return billId;
    }

    //Calculate bill
    public void calculateBill(String orderId){
        this.order_id = orderId;
        this.bill_id = generateBillId();

        LocalDate now = LocalDate.now();
        DateFormatter formatter = DateFormatter.ofPattern("yyyy-mm-dd");
        this.bill_date = now.format(formatter);

        String sql = "SELECT SUM(item_total) as total FROM order_items WHERE order_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                //this.price = rs.price("price");
                this.total_price = rs.getDouble("total");
                this.grand_total += total_price;

                displayBill();
                processPayment();
                saveBilling();

            }else{
                System.out.println("Order not found!");
            }
        }catch (SQLException e) {
            System.out.println("Error calculating bill: " + e.getMessage());
        }
    }


    //Display bill
    private void displayBill(){
        System.out.println("\n========================================");
        System.out.println("           DHEAT RESTAURANT             ");
        System.out.println("========================================");
        System.out.println("Bill ID: " + billId);
        System.out.println("Order ID: " + orderId);
        System.out.println("Date: " + billingDate);
        System.out.println("------------------------------------");

        displayOrderItems();

        System.out.println("------------------------------------");
        System.out.printf("GRAND TOTAL:       RM %.2f%n", grandTotal);
        System.out.println("------------------------------------");
        System.out.println("Payment Method: " + paymentMethod);
        System.out.println("\n    Thank you for dining with us!");
        System.out.println("         Please come again!");
        System.out.println("========================================\n");
    }
    //Display order items
    private void displayOrderItems() {
        String sql = "SELECT i_name, i_quantity, i_price, item_total FROM order_items WHERE order_id = ?";

        try (PreparedStatement pstmt = orderConn.preparedStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\nItem Name          Qty    Price    Total");
            System.out.println("----------------------------------------");

            while (rs.next()) {
                String name = rs.getString("i_name");
                int qty = rs.getInt("i_quantity");
                double price = rs.getDouble("i_price");
                double total = rs.getDouble("item_total");

                System.out.printf("%-18s %3d   %6.2f   %6.2f%n",
                        name, qty, price, total);
            }

        } catch (SQLException e) {
            System.out.println("Error displaying items: " +e.getMessage());
        }
    }
    //Process payment
    private void processPayment() {
        int choice;

        do {
            System.out.println("\n--- Payment Method ---");
            System.out.println("1. Cash");
            System.out.println("2. Card/QR");
            System.out.println("Select payment method: ");
            choice = sc.nextInt();

            switch (choice) {
                case 1 -> {
                    paymentMethod = "Cash";
                }
                case 2 -> {
                    paymentMethod = "Card";
                }
                default -> System.out.println("Invalid choice! Try again.");
            }
        }while (choice != 1 && choice != 2);
    }



    //Save billing to database
    public boolean saveBilling() {
        String sql = "INSERT INTO billing (bill_id, order_id, grand_total, payment_method, billing_date) " +
                     "VALUES ("?, ?, ?, ?, ?)";

        try(PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, billId);
            pstmt.setString(2, orderId);
//            pstmt.setDouble(3, price);
//            pstmt.setInt(4, quantity);
//            pstmt.setDouble(5, totalPrice);
            pstmt.setDouble(3, grandTotal);
            pstmt.setString(4, paymentMethod);
            pstmt.setString(5, billingDate);

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                billingConn.commit();
                System.out.println("\n Bill saved successfully!");
                System.out.println("Bill ID: " + bill_id);
                printReceipt();
                return true;
            }
        }catch (SQLException e) {
            System.out.println("Failed to save bill: " +e.getMessage());
            try {
                billingConn.rollback();
            } catch (SQLException ex) {
                System.out.println("Rollback failed: " +ex.getMessge());
            }
        }
        return false;
    }

    // //billing menu
    // public void billingSystem() {
    //     int choice;

    //     do{
    //         System.out.println("\n====== BILLING SYSTEM ======");
    //         System.out.println("1. Generate Bill");
    //         System.out.println("2. View All Bills");
    //         System.out.println("3. Search Bill");
    //         System.out.println("4. Exit");
    //         System.out.print("Enter choice: ");
    //         choice = sc.nextInt();

    //         switch (choice) {
    //             case 1 -> {
    //                 System.out.print("Enter Order ID: ");
    //                 String oid = sc.next();
    //                 calculateBill(oid);
    //             }
    //             case 2 -> viewAllBills();
    //             case 3 -> searchBill();
    //             case 4 -> System.out.println("Exiting billing system...");
    //             default -> System.out.println("Invalid choice!");
    //         }
    //     } while (choice != 4);
    // }

    //close connection
    public void closeConnections() {
        try {
            if (orderConn != null) orderConn.close();
            if (billingConn != null) billingConn.close();
            System.out.println("Connections closed.");
        } catch (SQLException e) {
            System.out.println("Error closing connections: " + e.getMessage());
        }
    }

    }