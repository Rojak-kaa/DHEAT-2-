import java.sql.*;
import java.util.*;

public class Payment {

    private double change;
    private Connection orderConn;
    Scanner sc = new Scanner(System.in);

    public Payment(Connection conn) {
        this.orderConn = conn;
    }


    

    // ============================
    // Bill Item Inner Class
    // ============================
    class BillItem {
        String name;
        double price;
        int qty;
        double subtotal;

        BillItem(String name, double price, int qty) {
            this.name = name;
            this.price = price;
            this.qty = qty;
            this.subtotal = price * qty;
        }
    }

    // ============================
    // Get Bill Items
    // ============================
    public List<BillItem> getBillItems(String orderId) {
        List<BillItem> items = new ArrayList<>();

        // Null check for database connection
        if (orderConn == null) {
            System.out.println("Error: Database connection is not available.");
            return items;
        }

        try {
            String sql = "SELECT i_name, i_price, i_quantity FROM order_items WHERE order_id = ?";
            PreparedStatement ps = orderConn.prepareStatement(sql);
            ps.setString(1, orderId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                items.add(new BillItem(
                        rs.getString("i_name"),
                        rs.getDouble("i_price"),
                        rs.getInt("i_quantity")
                ));
            }

            // Close resources
            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.out.println("Error retrieving bill items: " + e.getMessage());
        }

        return items;
    }


        private String getRequiredInput(String message) {
        while (true) {
            System.out.print(message);
            String input = sc.nextLine();
            if (input == null || input.trim().isEmpty()) {
                System.out.println("Empty input, please try again.");
                continue;
            }
            return input; 
        }
    }

    // Returns a parsed integer; repeats until valid non-empty integer entered.
    private int getRequiredInt(String message) {
        while (true) {
            String input = getRequiredInput(message);
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a valid integer.");
            }
        }
    }



    // ============================
    // Print Bill
    // ============================
    public double printBill(List<BillItem> items) {
        double total = 0;

        if (items == null || items.isEmpty()) {
            System.out.println("No items found in the bill.");
            return 0.0;
        }

        System.out.println("\n======================== YOUR BILL ==============================");
        for (BillItem item : items) {
            System.out.printf("%-20s x%-3d   RM %.2f%n", item.name, item.qty, item.subtotal);
            total += item.subtotal;
        }
        System.out.println("-------------------------------------------------------------------");
        System.out.printf("TOTAL: RM %.2f%n", total);
        System.out.println("===================================================================");

        return total;
    }

    // ============================
    // Payment Process
    // ============================
    public void payBill(String orderId) {
        // Null check for database connection
        if (orderConn == null) {
            System.out.println("Error: Database connection is not available. Cannot process payment.");
            return;
        }

        List<BillItem> items = getBillItems(orderId);
        
        if (items.isEmpty()) {
            System.out.println("No items found for order " + orderId + ". Cannot process payment.");
            return;
        }
        
        double totalAmount = printBill(items);

        int choice;
        do{
        System.out.println("\nPayment Method:");
        System.out.println("1. Cash");
        System.out.println("2. QR / Debit Card");
        //System.out.print("Enter number: ");
        choice = getRequiredInt("Enter number: ");
        
        // try {
            
        //     choice = sc.nextInt();
        // } catch (InputMismatchException e) {
        //     System.out.println("Invalid input. Please enter 1 or 2.");
        //     sc.nextLine(); // clear the invalid input
        //     return;
        // }


        if (choice == 1) {
            processCashPayment(totalAmount);
        } else if (choice == 2) {
            processCardPayment();
        } else {
            System.out.println("Invalid payment method.");
            System.out.println("Please enter a valid payment method.");
            //return;
        }

        }while(choice!= 1 && choice!=2);

        // Update order status to completed after successful payment
        //updateOrderStatus(orderId, "COMPLETED");
        printReceipt(orderId, items);
    }

    // ============================
    // Cash Payment Processing
    // ============================
    private void processCashPayment(double totalAmount) {
        double cash;
        do {
            System.out.print("Enter cash amount: RM ");
            try {
                cash = sc.nextDouble();
            } catch (InputMismatchException e) {
                System.out.println("Invalid amount. Please enter a valid number.");
                sc.nextLine(); // clear invalid input
                cash = -1;
                continue;
            }

            if (cash < totalAmount) {
                System.out.printf("Not enough! Need RM %.2f more%n", (totalAmount - cash));
            }
        } while (cash < totalAmount);

        change = cash - totalAmount;
        System.out.println("Payment Successful!");
        if (change > 0) {
            System.out.printf("Change: RM %.2f%n", change);
        }
    }

    // ============================
    // Card Payment Processing
    // ============================
    private void processCardPayment() {
        System.out.println("Processing...");
        // Simulate processing delay
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Payment Successful!");
    }

    // ============================
    // Update Order Status
    // ============================
    private void updateOrderStatus(String orderId, String status) {
        if (orderConn == null) return;

        String sql = "UPDATE order_items SET order_status = ? WHERE order_id = ?";
        try (PreparedStatement ps = orderConn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, orderId);
            int rowsUpdated = ps.executeUpdate();
            
            if (rowsUpdated > 0) {
                orderConn.commit();
                System.out.println("Order status updated to: " + status);
            }
        } catch (SQLException e) {
            System.out.println("Failed to update order status: " + e.getMessage());
            try {
                orderConn.rollback();
            } catch (SQLException ex) {
                System.out.println("Rollback failed: " + ex.getMessage());
            }
        }
    }

    // ============================
    // Print Receipt
    // ============================
    private void printReceipt(String orderId, List<BillItem> items) {
        System.out.println("\n===================== RECEIPT =========================");
        System.out.println("Order ID: " + orderId);
        System.out.println("Date: " + new java.util.Date());
        System.out.println("---------------------------------------------------------");

        double total = 0;
        for (BillItem item : items) {
            System.out.printf("%-20s x%-3d   RM %.2f%n", item.name, item.qty, item.subtotal);
            total += item.subtotal;
        }

        System.out.println("---------------------------------------------------------");
        System.out.println("Change : RM"+change);
        System.out.printf("TOTAL PAID: RM %.2f%n", total);
        System.out.println("======================= THANK YOU =======================");
    }

    // ============================
    // WEEKLY REPORT
    // ============================
    public void weeklyReport() {
        // Null check for database connection
        if (orderConn == null) {
            System.out.println("Error: Database connection is not available. Cannot generate report.");
            return;
        }

        String sql = """
            SELECT o.order_id, o.order_date, SUM(oi.item_total) AS total
            FROM orders o
            JOIN order_items oi ON o.order_id = oi.order_id
            WHERE o.order_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            GROUP BY o.order_id, o.order_date
            ORDER BY o.order_date;
        """;

        System.out.println("\n======================== WEEKLY SALES REPORT =============================");
        try (Statement stmt = orderConn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sql)) {
            
            double weeklyTotal = 0;
            int orderCount = 0;
            
            while (rs.next()) {
                String orderId = rs.getString("order_id");
                Timestamp date = rs.getTimestamp("order_date");
                double total = rs.getDouble("total");
                weeklyTotal += total;
                orderCount++;

                System.out.printf("Order ID: %s | Date: %s | Total: RM %.2f%n", 
                                 orderId, date, total);
            }
            
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("TOTAL ORDERS: %d%n", orderCount);
            System.out.printf("TOTAL SALES THIS WEEK: RM %.2f%n", weeklyTotal);
            
        } catch (SQLException e) {
            System.out.println("Failed to generate weekly report: " + e.getMessage());
        }
    }

    // ============================
    // MONTHLY REPORT
    // ============================
    public void monthlyReport() {
        // Null check for database connection
        if (orderConn == null) {
            System.out.println("Error: Database connection is not available. Cannot generate report.");
            return;
        }

        String sql = """
            SELECT o.order_id, o.order_date, SUM(oi.item_total) AS total
            FROM orders o
            JOIN order_items oi ON o.order_id = oi.order_id
            WHERE o.order_date >= DATE_SUB(NOW(), INTERVAL 1 MONTH)
            GROUP BY o.order_id, o.order_date
            ORDER BY o.order_date;
        """;

        System.out.println("\n============ MONTHLY SALES REPORT ==============");
        try (Statement stmt = orderConn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sql)) {
            
            double monthlyTotal = 0;
            int orderCount = 0;
            
            while (rs.next()) {
                String orderId = rs.getString("order_id");
                Timestamp date = rs.getTimestamp("order_date");
                double total = rs.getDouble("total");
                monthlyTotal += total;
                orderCount++;

                System.out.printf("Order ID: %s | Date: %s | Total: RM %.2f%n", 
                                 orderId, date, total);
            }
            
            System.out.println("-------------------------------------------------------------------------------");
            System.out.printf("TOTAL ORDERS: %d%n", orderCount);
            System.out.printf("TOTAL SALES THIS MONTH: RM %.2f%n", monthlyTotal);
            
        } catch (SQLException e) {
            System.out.println("Failed to generate monthly report: " + e.getMessage());
        }
    }

    // ============================
    // Connection Check Method
    // ============================
    public boolean isConnectionValid() {
        if (orderConn == null) {
            return false;
        }
        
        try {
            return orderConn.isValid(2); // 2 second timeout
        } catch (SQLException e) {
            return false;
        }
    }
}