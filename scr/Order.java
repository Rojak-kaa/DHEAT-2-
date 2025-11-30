import java.sql.*;
import java.util.*;

public class Order {

    private Connection orderConn;   // order_list
    private Connection invtConn;    // inventory
    private Scanner sc = new Scanner(System.in);
   // private Payment pay ;
   private Payment pay;


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

        pay = new Payment(orderConn);
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
        this.orderConn = null; // explicit
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


        System.out.println("\n===== Take Order =====");

        DBConn.showMenu();

        // Generate order ID
        order_id = "O" + System.currentTimeMillis();
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
                System.out.print("Item ID: ");
                i_id = sc.next();

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

                        System.out.println("Item Name: " + i_name);
                        System.out.println("Price: RM " + i_price);
                        System.out.println("Stock: " + stock);

                        valid = true;
                       

                    } else {
                        System.out.println("Item not found! Try again.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // Quantity
            do {
                System.out.print("Quantity: ");
                i_quantity = sc.nextInt();
                if (i_quantity > stock ) {
                    System.out.println("Not enough stock!");
                }
                else if(i_quantity<=0)
                    {
                        System.out.println("Invalid quantity. Please enter more than 0");
                    }
            } while (i_quantity > stock || i_quantity <=0);

            sc.nextLine();
            System.out.print("Remark: ");
            i_remark = sc.nextLine();

            // Calculate total
            item_total = i_quantity * i_price;

            //update status
            order_status = "Pending";

            // INSERT INTO order_items
           addItemToOrder(order_id, i_id, i_name, i_quantity, i_price, item_total, i_remark, order_status);

            // UPDATE INVENTORY
            reduceStock(table, i_id, stock - i_quantity);

            //  IMPORTANT: commit HERE so locks are released 
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

    System.out.print("\nAdd more items? (y/n): ");
    more = sc.next().charAt(0);

        }

        System.out.println("\n===== ORDER COMPLETE =====");
        System.out.println("Order ID: " + order_id);
        viewSingleOrder(order_id);

// // ===== After all orders and stock updates =====
//     try {
//         orderConn.commit();   // Save orders & order_items
//         invtConn.commit();    // Save inventory changes
//         System.out.println("Changes saved to MySQL!");
//     }  catch (SQLException e) {
//     System.out.println("Commit failed: " + e.getMessage());
//     try {
//         orderConn.rollback();
//         invtConn.rollback();
//         System.out.println("Rolled back changes due to error.");
//     } catch (SQLException ex) {
//         System.out.println("Rollback failed: " + ex.getMessage());
//     }
//     }

          do{
        System.out.println("-------Comfimation--------");
        System.out.println("1.Edit order");
        System.out.println("2.Cancle order");
        System.out.println("3.Comfirm order");
        System.out.println("4.Exit");
        System.out.print("Enter num: ");
        choice=sc.nextInt();
        sc.nextLine();
        switch(choice)
        {
            case 1 ->{editOrder();}
            case 2 -> {
                System.out.println("1.Cancle order");
                System.out.println("2.Cancle item");
                System.out.println("3.Exit");
                System.out.print("Enter num:");
                choice = sc.nextInt();
                sc.nextLine();
                switch(choice)
                {
                    case 1 ->{cancelOrder(order_id);break;}
                    case 2 ->{deleteItem(order_id);break;}
                    case 3 ->{continue;}
                }
                return;
            }
            case 3 ->{
                Payment payment = new Payment(orderConn);  // pass order database connection
                payment.payBill(order_id);
                return;}
            case 4 ->{
                break;
            }
                
            default->
            {
                System.out.println("Invalid number.");
                break;}
        }
        }while(choice != 1 && choice !=2);



    }


public boolean updateOrderStatus(String orderID, String newStatus, String currentStatus) {
    String sql = "UPDATE order_items SET order_status = ? WHERE order_id = ? AND order_status = ?";
    try (PreparedStatement ps = orderConn.prepareStatement(sql)) {
        ps.setString(1, newStatus);       // e.g., "COMPLETED"
        ps.setString(2, orderID);         // e.g., "O001"
        ps.setString(3, currentStatus);   // e.g., "READY" or "Pending"
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

    System.out.println("\n=== Cancelling Order " + oid + " ===");

    // 1. Check if order exists
    String checkSql = "SELECT i_id, i_quantity FROM order_items WHERE order_id = ?";
    try (PreparedStatement ps = orderConn.prepareStatement(checkSql)) {
        ps.setString(1, oid);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            System.out.println("Order not found. Cannot cancel.");
            return;
        }

        // ───────────────────────────────
        // 2. RESTORE STOCK FOR EACH ITEM
        // ───────────────────────────────
        do {
            String itemId = rs.getString("i_id");
            int qty = rs.getInt("i_quantity");

            restoreStock(itemId, qty);

        } while (rs.next());

    } catch (SQLException e) {
        System.out.println("Error checking order: " + e.getMessage());
        return;
    }

    // ───────────────────────────────
    // 3. DELETE ITEMS FROM order_items
    // ───────────────────────────────
    String deleteItems = "DELETE FROM order_items WHERE order_id = ?";
    try (PreparedStatement ps = orderConn.prepareStatement(deleteItems)) {
        ps.setString(1, oid);
        ps.executeUpdate();
    } catch (SQLException e) {
        System.out.println("Delete items failed: " + e.getMessage());
        return;
    }

    // ───────────────────────────────
    // 4. DELETE ORDER HEADER (orders table)
    // ───────────────────────────────
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
    System.out.print("Enter Item ID to remove from order: ");
    String itemId = sc.next();

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
        qty = rs.getInt("i_quantity"); // Save quantity to restore stock
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
            int row = ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to create order: " + e.getMessage());
        }

    }

    // Insert item into order_items
    private void addItemToOrder(String orderId, String itemId, String name, int qty, double price, double total, String remark, String status) {
        String sql = """
            INSERT INTO order_items(order_id, i_id, i_name, i_quantity, i_price, item_total, i_remark,order_status)
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
            ps.setString(8,status);

            int rows = ps.executeUpdate();
            System.out.println("Item added!");
        } catch (SQLException e) {
            System.out.println("Failed inserting item: " + e.getMessage());
        }


    }


 public void updateStatus() {

        while (true) {

            // Show only pending orders
            List<String> readyOrders = this.viewOrderByStatus("READY");  
            if (readyOrders.isEmpty()) {
                System.out.println("No ready orders at the moment.");
            } else {
                System.out.println("Ready Orders:");
                System.out.println("------------------------------------------");
                for (String orderInfo : readyOrders) {
                    System.out.println(orderInfo);
                }
                System.out.println("------------------------------------------");
            }

            System.out.println("1. Update Order Status to COMPLETED");
            System.out.println("2. Exit");
            System.out.print("Select: ");

            String choice = sc.nextLine();

            if (choice.equals("2")) {
                return;   // exit to main menu
            }

            if (!choice.equals("1")) {
                System.out.println("Invalid choice.");
                continue;
            }

            // Ask for order ID
            System.out.print("Enter Order ID to mark as READY (or x to cancel): ");
            String orderID = sc.nextLine();

            if (orderID.equalsIgnoreCase("x")) {
                continue;
            }

            // Update the order status using Order class
            boolean updated = this.updateOrderStatus(orderID, "COMPLETED","READY");

            if (updated) {
                System.out.println("Order " + orderID + "COMPLETED.");
            } else {
                System.out.println("Order not found or already completed.");
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

    // View a single order
    private void viewSingleOrder(String oid) {
        String sql = "SELECT * FROM order_items WHERE order_id=?";

        try (PreparedStatement ps = orderConn.prepareStatement(sql)) {
            ps.setString(1, oid);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.println(
                    rs.getString("i_name") +
                    " | Qty: " + rs.getInt("i_quantity") +
                    " | Price: RM " + rs.getDouble("i_price") +
                    " | Total: RM " + rs.getDouble("item_total")+
                    " | Status: "+rs.getString("order_status")
                );
            }

        } catch (SQLException e) {
            System.out.println("Failed loading order: " + e.getMessage());
        }
    }

    // --------------------------- // VIEW ORDER // --------------------------- 
    public void viewOrder() {
        System.out.println("====== View All Orders ======");
        String sql = "SELECT order_id,i_id,i_name,i_quantity,i_remark,order_status FROM order_items";
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
        System.out.println("\n====== Items in Order ======");
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
        System.out.print("Enter the Item ID you want to edit: ");
        editItemId = sc.next();
        if (itemIds.contains(editItemId)) break;
        System.out.println("Invalid Item ID. Try again.");
    }

    // 3. Choose what to edit
    System.out.println("1. Edit Remark");
    System.out.println("2. Edit Quantity");
    System.out.println("3. Cancel");
    System.out.print("Enter choice: ");
    int choice = sc.nextInt();
    sc.nextLine();

    switch (choice) {
        case 1 -> {
            System.out.print("New Remark: ");
            String newRemark = sc.nextLine();
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

        case 2 -> {
            // Get current stock
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

            // Ask new quantity
            int newQty;
            do {
                System.out.print("New Quantity: ");
                newQty = sc.nextInt();
                if (newQty <= 0) System.out.println("Quantity must be more than 0.");
                else if (newQty > currentStock) System.out.println("Not enough stock! Available: " + currentStock);
            } while (newQty <= 0 || newQty > currentStock);

            // Update order_items and inventory
            String sqlUpdate = "UPDATE order_items SET i_quantity = ?, item_total = i_price * ? WHERE order_id = ? AND i_id = ?";
            try (PreparedStatement ps = orderConn.prepareStatement(sqlUpdate)) {
                ps.setInt(1, newQty);
                ps.setInt(2, newQty);
                ps.setString(3, current_id);
                ps.setString(4, editItemId);
                if (ps.executeUpdate() > 0) {
                    reduceStock(table, editItemId, currentStock - newQty); // update inventory
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
            System.out.println("3. Update Status"); 
            System.out.println("4. Exit"); 
            System.out.print("Enter your choice: "); 
            choice = sc.nextInt(); 
            
            switch (choice) { 
                case 1 -> takeOrder(); 
                case 2 -> viewOrder(); 
                case 3 -> updateStatus(); 
                case 4 -> System.out.println("Exiting..."); 
                default -> System.out.println("Invalid choice."); 
            } 
        } while (choice != 4); }

}
