import java.sql.*;
import java.util.*;

public class Payment {

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

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    // ============================
    // Print Bill
    // ============================
    public double printBill(List<BillItem> items) {
        double total = 0;

        System.out.println("\n====== YOUR BILL ======");
        for (BillItem item : items) {
            System.out.println(item.name + "  x" + item.qty + "   RM " + item.subtotal);
            total += item.subtotal;
        }
        System.out.println("--------------------------");
        System.out.println("TOTAL: RM " + total);
        System.out.println("==========================");

        return total;
    }

    // ============================
    // Payment Process
    // ============================
    public void payBill(String orderId) {

        List<BillItem> items = getBillItems(orderId);
        double totalAmount = printBill(items);

        System.out.println("\nPayment Method:");
        System.out.println("1. Cash");
        System.out.println("2. QR / Debit Card");
        System.out.print("Enter number: ");
        int choice = sc.nextInt();

        if (choice == 1) {
            double cash;
            do {
                System.out.print("Enter cash amount: RM ");
                cash = sc.nextDouble();

                if (cash < totalAmount) {
                    System.out.println("Not enough! Need RM " + (totalAmount - cash));
                }
            } while (cash < totalAmount);

            double change = cash - totalAmount;
            System.out.println("Payment Successful!");
            if (change > 0)
                System.out.println("Change: RM " + change);
        } else {
            System.out.println("Processing...");
            System.out.println("Payment Successful!");
        }

        printReceipt(orderId, items);
    }

    // ============================
    // Print Receipt
    // ============================
    private void printReceipt(String orderId, List<BillItem> items) {
        System.out.println("\n=========== RECEIPT ===========");
        System.out.println("Order ID: " + orderId);
        System.out.println("Date: " + new java.util.Date());
        System.out.println("--------------------------------");

        double total = 0;
        for (BillItem item : items) {
            System.out.println(item.name + " x" + item.qty + "   RM " + item.subtotal);
            total += item.subtotal;
        }

        System.out.println("--------------------------------");
        System.out.println("TOTAL PAID: RM " + total);
        System.out.println("=========== THANK YOU ==========");
    }

    // ============================
    // WEEKLY REPORT
    // ============================
    public void weeklyReport() {
        String sql = """
            SELECT o.order_id, o.order_date, SUM(oi.item_total) AS total
            FROM orders o
            JOIN order_items oi ON o.order_id = oi.order_id
            WHERE o.order_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            GROUP BY o.order_id, o.order_date
            ORDER BY o.order_date;
        """;

        System.out.println("\n====== WEEKLY SALES REPORT ======");
        try (Statement stmt = orderConn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            double weeklyTotal = 0;
            while (rs.next()) {
                String orderId = rs.getString("order_id");
                Timestamp date = rs.getTimestamp("order_date");
                double total = rs.getDouble("total");
                weeklyTotal += total;

                System.out.printf("Order ID: %s | Date: %s | Total: RM %.2f%n", orderId, date, total);
            }
            System.out.println("--------------------------------");
            System.out.printf("TOTAL SALES THIS WEEK: RM %.2f%n", weeklyTotal);
        } catch (SQLException e) {
            System.out.println("Failed to generate weekly report: " + e.getMessage());
        }
    }

    // ============================
    // MONTHLY REPORT
    // ============================
    public void monthlyReport() {
        String sql = """
            SELECT o.order_id, o.order_date, SUM(oi.item_total) AS total
            FROM orders o
            JOIN order_items oi ON o.order_id = oi.order_id
            WHERE o.order_date >= DATE_SUB(NOW(), INTERVAL 1 MONTH)
            GROUP BY o.order_id, o.order_date
            ORDER BY o.order_date;
        """;

        System.out.println("\n====== MONTHLY SALES REPORT ======");
        try (Statement stmt = orderConn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            double monthlyTotal = 0;
            while (rs.next()) {
                String orderId = rs.getString("order_id");
                Timestamp date = rs.getTimestamp("order_date");
                double total = rs.getDouble("total");
                monthlyTotal += total;

                System.out.printf("Order ID: %s | Date: %s | Total: RM %.2f%n", orderId, date, total);
            }
            System.out.println("--------------------------------");
            System.out.printf("TOTAL SALES THIS MONTH: RM %.2f%n", monthlyTotal);
        } catch (SQLException e) {
            System.out.println("Failed to generate monthly report: " + e.getMessage());
        }
    }
}
