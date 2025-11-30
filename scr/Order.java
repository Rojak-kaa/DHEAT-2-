import java.sql.*;
import java.util.*;

public class Order {

    private Connection orderConn;   // order_list
    private Connection invtConn;    // inventory
    private Scanner sc = new Scanner(System.in);
    private Payment pay;
    //Payment payment = new Payment(orderConn);  // pass order database connection

    // Order fields
    protected String order_id;
    protected String i_id;
    protected String i_name;
    protected int i_quantity;
    protected double i_price;
    protected String i_remark;
    protected double item_total;
    protected String order_status;
    protected int choice;

    public Order() {
        initializeOrderDatabase();
        initializeInventoryDatabase();

        if (orderConn == null || invtConn == null) {
            System.out.println("Database connection failed. Exiting program.");
            System.exit(1);
        }

        // Create Payment object AFTER connections are established
        pay = new Payment(orderConn);
    }

    // =============================
    // INPUT HELPERS
    // =============================
    // Returns non-empty trimmed input; repeats until valid.
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

    // =============================
    // CONNECT TO order_list
    // =============================
    private void initializeOrderDatabase() {
        try {
            this.orderConn = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/order_list",
                "root",
                "UoW192411@"
            );
            orderConn.setAutoCommit(false);
            System.out.println("Order database connected!");
        } catch (SQLException e) {
            System.out.println("Order DB failed: " + e.getMessage());
            this.orderConn = null;
        }
    }

    private void initializePayment() {
        try {
            this.pay = new Payment(orderConn);
            // Test the connection by creating a simple statement
            if (orderConn != null && !orderConn.isClosed()) {
                System.out.println("Payment system initialized successfully.");
            }
        } catch (SQLException e) {
            System.out.println("Failed to initialize payment system: " + e.getMessage());
        }
    }
    
    // CONNECT TO inventory
    private void initializeInventoryDatabase() {
        try {
            this.invtConn = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/inventory",
                "root",
                "UoW192411@"
            );
            invtConn.setAutoCommit(false);
            System.out.println("Inventory database connected!");
        } catch (SQLException e) {
            System.out.println("Inventory DB failed: " + e.getMessage());
        }
    }

    public List<String> viewOrderByStatus(String status) {
        List<String> orders = new ArrayList<>();
        String sql = "SELECT order_id, i_name, i_quantity, i_remark, order_status FROM order_items WHERE order_status = ?";
        try (PreparedStatement ps = orderConn.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String info = "Order ID: " + rs.getString("order_id") +
                        " | Item: " + rs.getString("i_name") +
                        " | Qty: " + rs.getInt("i_quantity") +
                        " | Remark: " + rs.getString("i_remark") +
                        " | Status: " + rs.getString("order_status");
                orders.add(info);
            }
        } catch (SQLException e) {
            System.out.println("Failed to query orders by status: " + e.getMessage());
        }
        return orders;
    }

    // =====================================================
    // TAKE ORDER (create order + insert multiple items)
    // =====================================================
    public void takeOrder() {
        System.out.println("\n=========================== Take Order ===========================");
        DBConn.showMenu();

        // Generate order ID
        order_id = "O" + (System.currentTimeMillis()%1000);
        System.out.println("Order ID: " + order_id);

        // INSERT NEW ORDER HEADER
        createNewOrder(order_id);

        char more = 'y';

        while (more == 'y' || more == 'Y') {
            // Ask Item ID
            boolean valid = false;
            int stock = 0;
            String table = "";

            while (!valid) {
                i_id = getRequiredInput("Item ID: ").trim();

                String sqlCheck = """
                SELECT f_id AS id, f_name AS name, f_price AS price, f_quantity AS qty, 'food' AS tbl
                FROM food WHERE f_id = ?
                UNION
                SELECT b_id, b_name, b_price, b_quantity, 'beverage'
                FROM beverage WHERE b_id = ?
                UNION
                SELECT d_id, d_name, d_price, d_quantity, 'dessert'
                FROM dessert WHERE d_id = ?
                """;

                try (PreparedStatement ps = invtConn.prepareStatement(sqlCheck)) {
                    ps.setString(1, i_id);
                    ps.setString(2, i_id);
                    ps.setString(3, i_id);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        i_id = rs.getString("id");
                        i_name = rs.getString("name");
                        i_price = rs.getDouble("price");
                        stock = rs.getInt("qty");
                        table = rs.getString("tbl");

                          if (stock == 0) {
                            System.out.println("Sorry! '" + i_name + "' is OUT OF STOCK and cannot be added.");
                            valid = false; // don't proceed to quantity input
                            continue; // exit current item selection loop
                        }

                        System.out.println("Item Name: " + i_name);
                        System.out.println("Price: RM " + i_price);
                        System.out.println("Stock: " + stock);
                        valid = true;
                    } else {
                        System.out.println("Invalid input. The Item is not available.");
                        System.out.println("Please enter a valid item");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // Quantity
            do {
                
                i_quantity = getRequiredInt("Quantity: ");
                if (i_quantity > stock) {
                    System.out.println("Not enough stock!");
                    break;
                } else if (i_quantity <= 0) {
                    System.out.println("Invalid quantity. Please enter more than 0");
                }
            } while (i_quantity > stock || i_quantity <= 0);

            i_remark = getRequiredInput("Remark: ");

            // Calculate total
            item_total = i_quantity * i_price;

            // Update status
            order_status = "Pending";

            // INSERT INTO order_items
            addItemToOrder(order_id, i_id, i_name, i_quantity, i_price, item_total, i_remark, order_status);

            // UPDATE INVENTORY
            reduceStock(table, i_id, stock - i_quantity);
            try {
                invtConn.commit(); // commit inventory changes
            } catch (SQLException e) {
                System.out.println("Inventory commit failed: " + e.getMessage());
            }

            // Commit changes
            try {
                orderConn.commit();
                invtConn.commit();
            } catch (SQLException e) {
                try {
                    orderConn.rollback();
                    invtConn.rollback();
                } catch (SQLException ex) {
                    System.out.println("Rollback failed: " + ex.getMessage());
                }
            }

            // Ask to add more items (expect 'y' or 'n')
            String moreInput;
            while (true) {
                moreInput = getRequiredInput("\nAdd more items? (y/n): ").trim().toLowerCase();
                if (moreInput.equals("y") || moreInput.equals("n")) break;
                System.out.println("Please enter 'y' or 'n'.");
            }
            more = moreInput.charAt(0);
        }

        System.out.println("\n=================================== ORDER COMPLETE =========================================");
        System.out.println("Order ID: " + order_id);
        viewSingleOrder(order_id);

        // Confirmation menu
        handleConfirmationMenu();
    }

    // NEW METHOD: Handle confirmation menu separately
    private void handleConfirmationMenu() {
        do {
            System.out.println("\n---------- Order Option ----------");
            System.out.println("1. Edit order");
            System.out.println("2. Cancel order");
            System.out.println("3. Confirm order");
            System.out.println("4. Exit");

            choice = getRequiredInt("Enter num: ");

            switch (choice) {
                case 1 -> {
                    editOrder();
                    // After editing, show updated order and stay in confirmation menu
                    System.out.println("\n================================= ORDER DETAILS =======================================");
                    System.out.println("Order ID: " + order_id);
                    viewSingleOrder(order_id);
                    // Continue in confirmation menu (don't break)
                }
                case 2 -> {
                    System.out.println("\n1. Cancel order");
                    System.out.println("2. Cancel item");
                    System.out.println("3. Exit");
                    int cancelChoice = getRequiredInt("Enter num: ");
                    switch (cancelChoice) {
                        case 1 -> {
                            cancelOrder(order_id);
                            return; // Exit confirmation menu
                        }
                        case 2 -> {
                            deleteItem(order_id);
                            // After deleting item, show updated order
                            System.out.println("\n============================== ORDER COMPLETE =================================");
                            System.out.println("Order ID: " + order_id);
                            viewSingleOrder(order_id);
                            // Continue in confirmation menu
                        }
                        case 3 -> {
                            break;
                        }
                        default -> System.out.println("Invalid choice.");
                    }
                }
                case 3 -> {
                    pay.payBill(order_id);
                    return; // Exit confirmation menu
                }
                case 4 -> {
                    return; // Exit confirmation menu
                }
                default -> {
                    System.out.println("Invalid number.");
                    break;
                }
            }
        } while (true); // Keep showing confirmation menu until user exits or confirms
    }

    public boolean updateOrderStatus(String orderID, String newStatus, String currentStatus) {
        String sql = "UPDATE order_items SET order_status = ? WHERE order_id = ? AND order_status = ?";
        try (PreparedStatement ps = orderConn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, orderID);
            ps.setString(3, currentStatus);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                orderConn.commit();
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Failed to update order status: " + e.getMessage());
            try { orderConn.rollback(); } catch (SQLException ex) {}
        }
        return false;
    }

    public void cancelOrder(String oid) {
        System.out.println("\n======== Cancelling Order " + oid + " =============");

        // 1. Check if order exists
        String checkSql = "SELECT i_id, i_quantity FROM order_items WHERE order_id = ?";
        try (PreparedStatement ps = orderConn.prepareStatement(checkSql)) {
            ps.setString(1, oid);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println("Order not found. Cannot cancel.");
                return;
            }

            // 2. RESTORE STOCK FOR EACH ITEM
            do {
                String itemId = rs.getString("i_id");
                int qty = rs.getInt("i_quantity");
                restoreStock(itemId, qty);
            } while (rs.next());

        } catch (SQLException e) {
            System.out.println("Error checking order: " + e.getMessage());
            return;
        }

        // 3. DELETE ITEMS FROM order_items
        String deleteItems = "DELETE FROM order_items WHERE order_id = ?";
        try (PreparedStatement ps = orderConn.prepareStatement(deleteItems)) {
            ps.setString(1, oid);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Delete items failed: " + e.getMessage());
            return;
        }

        // 4. DELETE ORDER HEADER (orders table)
        String deleteOrder = "DELETE FROM orders WHERE order_id = ?";
        try (PreparedStatement ps = orderConn.prepareStatement(deleteOrder)) {
            ps.setString(1, oid);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Delete order failed: " + e.getMessage());
            return;
        }

        // COMMIT
        try {
            orderConn.commit();
            invtConn.commit();
        } catch (SQLException e) {
            System.out.println("Commit failed: " + e.getMessage());
        }

        System.out.println("Order " + oid + " has been fully CANCELLED.");
    }

    private void restoreStock(String itemId, int qty) {
        String sqlCheck = """
        SELECT f_id AS id, f_quantity AS qty, 'food' AS tbl FROM food WHERE f_id = ?
        UNION
        SELECT b_id, b_quantity, 'beverage' FROM beverage WHERE b_id = ?
        UNION
        SELECT d_id, d_quantity, 'dessert' FROM dessert WHERE d_id = ?
        """;

        try (PreparedStatement ps = invtConn.prepareStatement(sqlCheck)) {
            ps.setString(1, itemId);
            ps.setString(2, itemId);
            ps.setString(3, itemId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return;

            String table = rs.getString("tbl");
            int currentQty = rs.getInt("qty");
            int newQty = currentQty + qty;

            // Update
            reduceStock(table, itemId, newQty);

        } catch (SQLException e) {
            System.out.println("Failed to restore stock: " + e.getMessage());
        }
    }

    public void deleteItem(String orderId) {
        String itemId = getRequiredInput("Enter Item ID to remove from order: ").trim();

        // 1. Check if the item exists in this order
        String checkSql = "SELECT i_quantity FROM order_items WHERE order_id = ? AND i_id = ?";
        int qty = 0;
        try (PreparedStatement ps = orderConn.prepareStatement(checkSql)) {
            ps.setString(1, orderId);
            ps.setString(2, itemId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("Item not found in this order.");
                return;
            }
            qty = rs.getInt("i_quantity");
        } catch (SQLException e) {
            System.out.println("Error checking item: " + e.getMessage());
            return;
        }

        // 2. Restore stock for this item
        restoreStock(itemId, qty);

        // 3. Delete the item from order_items
        String deleteSql = "DELETE FROM order_items WHERE order_id = ? AND i_id = ?";
        try (PreparedStatement ps = orderConn.prepareStatement(deleteSql)) {
            ps.setString(1, orderId);
            ps.setString(2, itemId);
            if (ps.executeUpdate() > 0) {
                System.out.println("Item removed from order successfully.");
            }
            orderConn.commit();
            invtConn.commit();
        } catch (SQLException e) {
            System.out.println("Failed to delete item: " + e.getMessage());
            try { orderConn.rollback(); invtConn.rollback(); } catch (SQLException ex) {}
        }
    }

    public double getOrderTotal(String orderId) {
        double sum = 0;
        String sql = "SELECT SUM(item_total) AS total FROM order_items WHERE order_id = ?";
        try (PreparedStatement ps = orderConn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                sum = rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.out.println("Total calculation failed: " + e.getMessage());
        }
        return sum;
    }

    // Insert into orders table
    private void createNewOrder(String orderId) {
        String sql = "INSERT INTO orders(order_id, order_status, order_date) VALUES(?, 'Pending', NOW())";
       try (PreparedStatement ps = orderConn.prepareStatement(sql)) {

        ps.setString(1, orderId);

        ps.executeUpdate();

        orderConn.commit(); // <-- commit here ensures the order header is saved

        System.out.println();

    } catch (SQLException e) {

        System.out.println("Failed to create order: " + e.getMessage());

        try { orderConn.rollback(); } catch (SQLException ex) {}

    }

}

    // Insert item into order_items
    private void addItemToOrder(String orderId, String itemId, String name, int qty, double price, double total, String remark, String status) {
        String sql = """
            INSERT INTO order_items(order_id, i_id, i_name, i_quantity, i_price, item_total, i_remark, order_status)
            VALUES(?,?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = orderConn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, itemId);
            ps.setString(3, name);
            ps.setInt(4, qty);
            ps.setDouble(5, price);
            ps.setDouble(6, total);
            ps.setString(7, remark);
            ps.setString(8, status);

            int rows = ps.executeUpdate();
            orderConn.commit();
            System.out.println("Item added!");
        } catch (SQLException e) {
            System.out.println("Failed inserting item: " + e.getMessage());
            try { orderConn.rollback(); } catch (SQLException ex) {}

            }
 
        }

    public void updateStatus() {
        while (true) {
            // Show only ready orders
            List<String> readyOrders = this.viewOrderByStatus("READY");  
            if (readyOrders.isEmpty()) {
                System.out.println("No ready orders at the moment.");
            } else {
                System.out.println("Ready Orders:\n");

                for (String orderInfo : readyOrders) {
                    System.out.println(orderInfo);
                }
                System.out.println("------------------------------------------------------------------------------------------------");
            }

            System.out.println("1. Update Order Status to COMPLETED");
            System.out.println("2. Exit");

            String choice = getRequiredInput("Select: ").trim();

            if (choice.equals("2")) {
                return;
            }

            if (!choice.equals("1")) {
                System.out.println("Invalid choice.");
                continue;
            }

            // Ask for order ID
            String orderID = getRequiredInput("Enter Order ID to mark as COMPLETED (or x to cancel): ").trim();

            if (orderID.equalsIgnoreCase("x")) {
                continue;
            }

            // Update the order status
            boolean updated = this.updateOrderStatus(orderID, "COMPLETED", "READY");

            if (updated) {
                System.out.println("Order " + orderID + " COMPLETED.");
            } else {
                System.out.println("Order not found or already completed.");
                System.out.println("Please enter a valid Order ID.\n");
            }
        }
    }

    // Reduce stock in correct table
    private void reduceStock(String table, String id, int newQty) {
        String colQty = table.equals("food") ? "f_quantity"
                    : table.equals("beverage") ? "b_quantity"
                    : "d_quantity";

        String colId = table.equals("food") ? "f_id"
                   : table.equals("beverage") ? "b_id"
                   : "d_id";

        String sql = "UPDATE " + table + " SET " + colQty + "=? WHERE " + colId + "=?";

        try (PreparedStatement ps = invtConn.prepareStatement(sql)) {
            ps.setInt(1, newQty);
            ps.setString(2, id);
            int rows = ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Stock update failed: " + e.getMessage());
        }
    }

    // View a single order - FIXED FORMAT to match expected output
    private void viewSingleOrder(String oid) {
        String sql = "SELECT * FROM order_items WHERE order_id=?";
        try (PreparedStatement ps = orderConn.prepareStatement(sql)) {
            ps.setString(1, oid);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.println(
                    rs.getString("i_id").toUpperCase() +
                    " | Item: " + rs.getString("i_name") +
                    " | Qty: " + rs.getInt("i_quantity") +
                    " | Price: RM " + rs.getDouble("i_price") +
                    " | Total: RM " + rs.getDouble("item_total") +
                    " | Remark: " + rs.getString("i_remark") +
                    " | Status: " + rs.getString("order_status")
                );
            }
        } catch (SQLException e) {
            System.out.println("Failed loading order: " + e.getMessage());
        }
    }

    // --------------------------- // VIEW ORDER // --------------------------- 
    public void viewOrder() {
        System.out.println("====== View All Orders ======");
        String sql = "SELECT order_id, i_id, i_name, i_quantity, i_remark, order_status FROM order_items";
        try (Statement stmt = orderConn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println("Order ID: " + rs.getString("order_id") +
                        " | Item ID: " + rs.getString("i_id") +
                        " | Item Name: " + rs.getString("i_name") +
                        " | Quantity: " + rs.getInt("i_quantity") +
                        " | Remark: " + rs.getString("i_remark") +
                        " | Order Status: " + rs.getString("order_status"));
            }
        } catch (SQLException e) {
            System.out.println("Failed to query orders: " + e.getMessage());
        }
    }

    // --------------------------- // SEARCH ORDER (by ID) // --------------------------- 
    public boolean searchOrder(String oid) {
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        try (PreparedStatement pstmt = orderConn.prepareStatement(sql)) { 
            pstmt.setString(1, oid); 
            ResultSet rs = pstmt.executeQuery(); 
            
            if (rs.next()) { 
                System.out.println("FOUND ORDER:");
                System.out.println("Item ID: " + rs.getString("i_id")); 
                System.out.println("Quantity: " + rs.getInt("i_quantity")); 
                System.out.println("Remark: " + rs.getString("i_remark")); 
                System.out.println("Status: " + rs.getString("order_status"));
                System.out.println("------------------------");

                // load into object
                i_id = rs.getString("i_id"); 
                i_quantity = rs.getInt("i_quantity"); 
                i_remark = rs.getString("i_remark"); 
                order_status = rs.getString("order_status");

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

    // --------------------------- // EDIT ORDER // --------------------------- 
    public void editOrder() {
        if (this.order_id == null) {
            System.out.println("No active order to edit.");
            return;
        }

        String current_id = this.order_id;

        // 1. List all items in this order
        String sqlList = "SELECT i_id, i_name, i_quantity, i_remark FROM order_items WHERE order_id = ?";
        List<String> itemIds = new ArrayList<>();
        try (PreparedStatement ps = orderConn.prepareStatement(sqlList)) {
            ps.setString(1, current_id);
            ResultSet rs = ps.executeQuery();
            System.out.println("\n==================== Items in Order ======================");
            while (rs.next()) {
                String id = rs.getString("i_id");
                String name = rs.getString("i_name");
                int qty = rs.getInt("i_quantity");
                String remark = rs.getString("i_remark");
                System.out.println("Item ID: " + id + " | Name: " + name + " | Qty: " + qty + " | Remark: " + remark);
                itemIds.add(id);
            }
        } catch (SQLException e) {
            System.out.println("Failed to load items: " + e.getMessage());
            return;
        }

        if (itemIds.isEmpty()) {
            System.out.println("No items in this order to edit.");
            return;
        }

        // 2. Ask which item to edit
        String editItemId;
        while (true) {
            editItemId = getRequiredInput("Enter the Item ID you want to edit (or x to return): ").trim();
            if (editItemId.equalsIgnoreCase("x")) return;
            if (itemIds.contains(editItemId)) break;
            System.out.println("Item not found.");
            System.out.println("Please enter a valid item ID");
        }

        // 3. Choose edit option
        System.out.println("1. Edit Remark");
        System.out.println("2. Edit Quantity");
        System.out.println("3. Cancel");
        String choiceInput = getRequiredInput("Enter choice (or x to return): ").trim();
        if (choiceInput.equalsIgnoreCase("x")) return;

        int localChoice;
        try {
            localChoice = Integer.parseInt(choiceInput);
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice. Returning to confirmation menu.");
            return;
        }

        switch (localChoice) {
            case 1 -> {
                String newRemark = getRequiredInput("New Remark (or press Enter to skip): ");
                if (!newRemark.isEmpty()) {
                    String sql = "UPDATE order_items SET i_remark = ? WHERE order_id = ? AND i_id = ?";
                    try (PreparedStatement pstmt = orderConn.prepareStatement(sql)) {
                        pstmt.setString(1, newRemark);
                        pstmt.setString(2, current_id);
                        pstmt.setString(3, editItemId);
                        if (pstmt.executeUpdate() > 0) {
                            orderConn.commit();
                            System.out.println("Remark updated successfully.");
                        }
                    } catch (SQLException e) {
                        System.out.println("Update failed: " + e.getMessage());
                    }
                }
            }
            case 2 -> {
                // Quantity edit logic
                int currentStock = 0;
                String table = "";
                String sqlStock = """
                    SELECT f_quantity AS qty, 'food' AS tbl FROM food WHERE f_id = ?
                    UNION
                    SELECT b_quantity, 'beverage' FROM beverage WHERE b_id = ?
                    UNION
                    SELECT d_quantity, 'dessert' FROM dessert WHERE d_id = ?
                """;
                try (PreparedStatement ps = invtConn.prepareStatement(sqlStock)) {
                    ps.setString(1, editItemId);
                    ps.setString(2, editItemId);
                    ps.setString(3, editItemId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        currentStock = rs.getInt("qty");
                        table = rs.getString("tbl");
                    }
                } catch (SQLException e) {
                    System.out.println("Failed to get stock: " + e.getMessage());
                    return;
                }

                int newQty;
                while (true) {
                    String input = getRequiredInput("New Quantity (or press Enter to skip): ").trim();
                    if (input.equalsIgnoreCase("x")) return;
                    try {
                        newQty = Integer.parseInt(input);
                        if (newQty <= 0) System.out.println("Quantity must be more than 0.");
                        else if (newQty > currentStock) System.out.println("Not enough stock! Available: " + currentStock);
                        else break;
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number. Try again.");
                    }
                }

                String sqlUpdate = "UPDATE order_items SET i_quantity = ?, item_total = i_price * ? WHERE order_id = ? AND i_id = ?";
                try (PreparedStatement ps = orderConn.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, newQty);
                    ps.setInt(2, newQty);
                    ps.setString(3, current_id);
                    ps.setString(4, editItemId);
                    if (ps.executeUpdate() > 0) {
                        reduceStock(table, editItemId, currentStock - newQty);
                        orderConn.commit();
                        invtConn.commit();
                        System.out.println("Quantity updated successfully.");
                    }
                } catch (SQLException e) {
                    System.out.println("Update failed: " + e.getMessage());
                    try { orderConn.rollback(); invtConn.rollback(); } catch (SQLException ex) {}
                }
            }
            case 3 -> System.out.println("Edit cancelled.");
            default -> System.out.println("Invalid choice.");
        }
    }

    public void orderSystem() {
        do {
            System.out.println("\n====== Waiter/Cashier Menu ======");
            System.out.println("1. Take Order");
            System.out.println("2. View Orders");
            System.out.println("3. Update Order Status");
            System.out.println("4. Generate Sales Report");
            System.out.println("5. Exit");
            choice = getRequiredInt("Enter your choice: ");

            switch (choice) {
                case 1 -> takeOrder();
                case 2 -> viewOrder();
                case 3 -> {
                    System.out.println("\n-----------------------------------------Update Status------------------------------------------");
                    updateStatus();
                }
                case 4 -> {
                    int reportChoice;
                    do{
                    System.out.println("\n--------- Report ----------");
                    System.out.println("1. Monthly report");
                    System.out.println("2. Weekly report");
                    System.out.println("3. EXIT");
                    reportChoice = getRequiredInt("Enter number: ");

                    switch (reportChoice) {
                        case 1 -> {pay.monthlyReport();}
                        case 2 -> {pay.weeklyReport();break;}
                        case 3 -> {break;}
                        default -> System.out.println("Invalid choice.");
                    }
                }while(reportChoice != 3);
                
                }
                case 5 -> System.out.println("Exiting...");
                default -> System.out.println("Invalid choice.");
            }
        } while (choice != 5);
    }
}
