import java.sql.*;
import java.util.*;

public class Order {

    private Connection orderConn;//order_list
    private Connection invtConn; //inventory database
    private Scanner sc = new Scanner(System.in);
    //DBOrder Order = new DBOrder();

    protected String order_id;
    protected String i_id;
    protected String i_name;
    protected int i_quantity;
    protected double i_price;
    protected String i_remark;
    protected String order_status;
    protected String orderType;
    protected char choice;
    protected double total_price;

    public Order() {
        initializeOrderDatabase();
        initializeInventoryDatabase();
    }

    private void initializeOrderDatabase() {
        try {
            this.orderConn = DBOrder.getConnection();
            System.out.println("Database connection successful!");
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    private void initializeInventoryDatabase() {
        try {
            this.invtConn = DriverManager.getConnection(
            "jdbc:mysql://127.0.0.1:3306/inventory", "root", "UoW192411@"
            );
            System.out.println("Inventory database connection successful!");
        } catch (SQLException e) {
            System.out.println("Inventory DB connection failed: " + e.getMessage());
        }
}



    // ---------------------------
    //  Check if Order Exists
    // ---------------------------
    public boolean isOrderExist(String id) {
        String sql = "SELECT 1 FROM orders WHERE order_id = ?";

        try (PreparedStatement ps = orderConn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ---------------------------
    //  EDIT ORDER
    // ---------------------------
    public void editOrder() {

        if (!searchOrder())
            return;

        System.out.println("====== Edit Order ======");
        System.out.println("1. Edit Remark");
        System.out.println("2. Edit Quantity");
        System.out.println("3. Confirm");
        System.out.print("Enter choice: ");

        choice = sc.next().charAt(0);
        sc.nextLine();

        switch (choice) {
            case '1' -> {
                System.out.print("New Remark: ");
                String newRemark = sc.nextLine();

                String sql = "UPDATE cus_order SET i_remark = ? WHERE order_id = ?";

                try (PreparedStatement pstmt = orderConn.prepareStatement(sql)) {

                    pstmt.setString(1, newRemark);
                    pstmt.setString(2, order_id);

                    if (pstmt.executeUpdate() > 0)
                        System.out.println("Remark updated successfully.");

                } catch (SQLException e) {
                    System.out.println("Update failed: " + e.getMessage());
                }
            }

            case '2' -> {
                System.out.print("New Quantity: ");
                int newQty = sc.nextInt();

                String sql = "UPDATE cus_order SET i_quantity = ? WHERE order_id = ?";

                try (PreparedStatement pstmt = orderConn.prepareStatement(sql)) {

                    pstmt.setInt(1, newQty);
                    pstmt.setString(2, order_id);

                    if (pstmt.executeUpdate() > 0)
                        System.out.println("Quantity updated successfully.");

                } catch (SQLException e) {
                    System.out.println("Update failed: " + e.getMessage());
                }
            }

            case '3' -> {
                return;
            }
        }
    }


    class CartItem {
    String itemId;
    String itemName;
    int qty;
    String remark;
    int stock;
    String tableName;

    public CartItem(String itemId, String itemName, int qty, String remark, int stock, String tableName) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.qty = qty;
        this.remark = remark;
        this.stock = stock;
        this.tableName = tableName;
    }
}


    // ---------------------------
    //  TAKE ORDER
    // ---------------------------
public void takeOrder() {
    System.out.println("====== Take Order ======");
    DBConn.showMenu();

    // Generate unique order ID
    order_id = "O" + System.currentTimeMillis();
    System.out.println("Order ID: " + order_id);

    if (invtConn == null) initializeInventoryDatabase();

    List<String> tempReceipt = new ArrayList<>();

    char choice = 'y';

    while (choice == 'y' || choice == 'Y') {

        boolean validItem = false;
        int availableStock = 0;
        String tableName = "";   // IMPORTANT: store which table the item belongs to

        // =============================
        // CHECK ITEM VALIDITY
        // =============================
        while (!validItem) {
            System.out.print("Item ID: ");
            i_id = sc.next();

            String checkSql = """
            SELECT f_id AS item_id, f_name AS item_name, f_quantity AS stock, 'food' AS tbl 
            FROM food WHERE f_id = ?
            UNION
            SELECT b_id AS item_id, b_name AS item_name, b_quantity AS stock, 'beverage' AS tbl 
            FROM beverage WHERE b_id = ?
            UNION
            SELECT d_id AS item_id, d_name AS item_name, d_quantity AS stock, 'dessert' AS tbl 
            FROM dessert WHERE d_id = ?
            """;

            try (PreparedStatement ps = invtConn.prepareStatement(checkSql)) {
                ps.setString(1, i_id);
                ps.setString(2, i_id);
                ps.setString(3, i_id);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    i_id = rs.getString("item_id");
                    i_name = rs.getString("item_name");
                    availableStock = rs.getInt("stock");
                    tableName = rs.getString("tbl");       // STORE TABLE NAME

                    System.out.println("Item Name: " + i_name);
                    System.out.println("Available Stock: " + availableStock);
                    validItem = true;
                } else {
                    System.out.println("Item not found! Try again.");
                }

            } catch (SQLException e) {
                System.out.println("Error checking item: " + e.getMessage());
            }
        }

        // =============================
        // ASK QUANTITY
        // =============================
        boolean validQty = false;
        while (!validQty) {
            System.out.print("Quantity: ");
            i_quantity = sc.nextInt();

            if (i_quantity <= availableStock) {
                validQty = true;
            } else {
                System.out.println("Not enough stock. Only " + availableStock + " left.");
            }
        }

        sc.nextLine();
        System.out.print("Remark: ");
        i_remark = sc.nextLine();

        order_status = "Pending";

        // =============================
        // INSERT INTO ORDER LIST
        // =============================
        String insertSql = """
            INSERT INTO cus_order(order_id, i_id, i_name, i_quantity, i_remark, order_status)
            VALUES(?,?,?,?,?,?)
        """;

        try (PreparedStatement ps = orderConn.prepareStatement(insertSql)) {
            ps.setString(1, order_id);
            ps.setString(2, i_id);
            ps.setString(3, i_name);
            ps.setInt(4, i_quantity);
            ps.setString(5, i_remark);
            ps.setString(6, order_status);

            ps.executeUpdate();
            System.out.println("Item added!");
        } catch (SQLException e) {
            System.out.println("Insert failed: " + e.getMessage());
        }

        // =============================
        // UPDATE INVENTORY STOCK
        // =============================
        String updateSql = "UPDATE " + tableName + " SET " +
                (tableName.equals("food") ? "f_quantity" :
                 tableName.equals("beverage") ? "b_quantity" : "d_quantity") +
                " = ? WHERE " +
                (tableName.equals("food") ? "f_id" :
                 tableName.equals("beverage") ? "b_id" : "d_id") +
                " = ?";

        try (PreparedStatement ps = invtConn.prepareStatement(updateSql)) {
            ps.setInt(1, availableStock - i_quantity);
            ps.setString(2, i_id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to update stock: " + e.getMessage());
        }

        // =============================
        // TEMP RECEIPT
        // =============================
        tempReceipt.add("Item: " + i_name + " | Qty: " + i_quantity);

        System.out.println("\n--- Current Order ---");
        for (String line : tempReceipt) System.out.println(line);

        // Continue?
        System.out.print("\nAdd more items? (y/n): ");
        choice = sc.next().charAt(0);
    }

    // =============================
    // FINAL RECEIPT
    // =============================
    System.out.println("\n===== ORDER COMPLETE =====");
    System.out.println("Order ID: " + order_id);
    for (String line : tempReceipt) System.out.println(line);
    System.out.println("Status: Pending");
}



    // ---------------------------
    //  VIEW ORDER
    // ---------------------------
    public void viewOrder() {

        System.out.println("====== View All Orders ======");

        String sql = "SELECT order_id,i_id,i_name,i_quantity,i_remark FROM cus_order";

        try (Statement stmt = orderConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.println("Order ID: " + rs.getString("order_id")
                        + " | Item ID: " + rs.getString("i_id")
                        + " | Quantity: " + rs.getInt("i_quantity")
                        + " | Remark: " + rs.getString("i_remark")
                );
            }

        } catch (SQLException e) {
            System.out.println("Failed to query orders: " + e.getMessage());
        }
    }

    // ---------------------------
    //  SEARCH ORDER (by ID)
    // ---------------------------
    public boolean searchOrder() {

        System.out.print("Enter Order ID: ");
        order_id = sc.nextLine();

        String sql = "SELECT * FROM cus_order WHERE order_id = ?";

        try (PreparedStatement pstmt = orderConn.prepareStatement(sql)) {

            pstmt.setString(1, order_id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                System.out.println("FOUND ORDER:");
                System.out.println("Item ID: " + rs.getString("i_id"));
                System.out.println("Quantity: " + rs.getInt("i_quantity"));
                System.out.println("Remark: " + rs.getString("i_remark"));
                System.out.println("------------------------");

                // Load current data
                i_id = rs.getString("i_id");
                i_quantity = rs.getInt("i_quantity");
                i_remark = rs.getString("i_remark");

                return true;
            } else {
                System.out.println("Order not found.");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("Search failed: " + e.getMessage());
        }

        return false;
    }


    // ---------------------------
    //  MENU
    // ---------------------------
    public void orderSystem() {

        do {
            System.out.println("====== Waiter/Cashier Menu ======");
            System.out.println("1. Take Order");
            System.out.println("2. View Orders");
            System.out.println("3. Edit Order");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");

            choice = sc.next().charAt(0);

            switch (choice) {
                case '1' -> takeOrder();
                case '2' -> viewOrder();
                case '3' -> editOrder();
                case '4' -> System.out.println("Exiting...");
                default -> System.out.println("Invalid choice.");
            }

        } while (choice != '4');
    }
}
