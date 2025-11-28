import java.sql.*;
import java.util.*;

public class Order {

    private Connection orderConn;   // order_list
    private Connection invtConn;    // inventory
    private Scanner sc = new Scanner(System.in);

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
            addItemToOrder(order_id, i_id, i_name, i_quantity, i_price, item_total, i_remark,order_status);

            // UPDATE INVENTORY
            reduceStock(table, i_id, stock - i_quantity);

            System.out.print("\nAdd more items? (y/n): ");
            more = sc.next().charAt(0);
        }

        System.out.println("\n===== ORDER COMPLETE =====");
        System.out.println("Order ID: " + order_id);
        viewSingleOrder(order_id);

// ===== After all orders and stock updates =====
    try {
        orderConn.commit();   // Save orders & order_items
        invtConn.commit();    // Save inventory changes
        System.out.println("Changes saved to MySQL!");
    }  catch (SQLException e) {
    System.out.println("Commit failed: " + e.getMessage());
    try {
        orderConn.rollback();
        invtConn.rollback();
        System.out.println("Rolled back changes due to error.");
    } catch (SQLException ex) {
        System.out.println("Rollback failed: " + ex.getMessage());
    }
    }

          do{
        System.out.println("-------Comfimation--------");
        System.out.println("1.Edit order");
        System.out.println("2.Comfirm order");
        System.out.print("Enter num: ");
        choice=sc.nextInt();
        switch(choice)
        {
            case 1 ->{editOrder();}
            case 2 -> {break;}
            default->
            {
                System.out.println("Invalid number.");
                break;}
        }
        }while(choice != 1 && choice !=2);



    }

    // Insert into orders table
    private void createNewOrder(String orderId) {
        String sql = "INSERT INTO orders(order_id, order_status) VALUES(?, 'Pending')";
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
    //show the order that have "Done" status


    System.out.print("Enter Order ID to update: ");
    String oid = sc.next();

    // Check if order exists
    String checkSql = "SELECT * FROM order_items WHERE order_id = ?";
    try (PreparedStatement checkPs = orderConn.prepareStatement(checkSql)) {
        checkPs.setString(1, oid);
        ResultSet rs = checkPs.executeQuery();
        if (!rs.next()) {
            System.out.println("Order ID not found.");
            return;
        }
    } catch (SQLException e) {
        System.out.println("Search failed: " + e.getMessage());
        return;
    }

    System.out.println("\n--- Update Order Status ---");
    System.out.println("1. Mark as COMPLETE");
    System.out.println("2. Exit");
    System.out.print("Enter your choice: ");
    int ch = sc.nextInt();

    if (ch == 2) {
        System.out.println("Status update cancelled.");
        return;
    }

    if (ch != 1) {
        System.out.println("Invalid choice.");
        return;
    }

    // Update to COMPLETE
    String sql = "UPDATE order_items SET order_status = 'Complete' WHERE order_id = ?";

    try (PreparedStatement ps = orderConn.prepareStatement(sql)) {
        ps.setString(1, oid);

        if (ps.executeUpdate() > 0) {
            orderConn.commit();
            System.out.println("Order status updated to COMPLETE!");
        }

    } catch (SQLException e) {
        System.out.println("Status update failed: " + e.getMessage());
        try { orderConn.rollback(); } catch (SQLException ex) {}
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
        try (Statement stmt = orderConn.createStatement(); ResultSet rs = stmt.executeQuery(sql))
        { while (rs.next()) { 
            System.out.println("Order ID: " + rs.getString("order_id") + 
            " | Item ID: " + rs.getString("i_id") +
            " | Item Name: "+ rs.getString("i_name") + 
            " | Quantity: " + rs.getInt("i_quantity") + 
            " | Remark: " + rs.getString("i_remark")+
            " | Order Status: "+ rs.getString("order_status") ); 
            }
        } catch (SQLException e) { System.out.println("Failed to query orders: " + e.getMessage()); } }

        // --------------------------- // SEARCH ORDER (by ID) // --------------------------- 
        public boolean searchOrder(String searchOrderID) { 
            
            System.out.print("Order ID: "); 
            
            String sql = "SELECT * FROM orders WHERE order_id = ?";

            try (PreparedStatement pstmt = orderConn.prepareStatement(sql))
            { 
                pstmt.setString(1, order_id); 
                ResultSet rs = pstmt.executeQuery(); 
                if (rs.next()) { System.out.println("FOUND ORDER:"); 
                System.out.println("Item ID: " + rs.getString("i_id")); 
                System.out.println("Quantity: " + rs.getInt("i_quantity")); 
                System.out.println("Remark: " + rs.getString("i_remark")); 
                System.out.println("Status: "+rs.getString("order_status"));
                System.out.println("------------------------"); // Load current data 
                i_id = rs.getString("i_id"); 
                i_quantity = rs.getInt("i_quantity"); 
                i_remark = rs.getString("i_remark"); 
                order_status = rs.getString("order_status");
                return true; } 
                else { 
                    System.out.println("Order not found."); 
                    return false; 
                } } catch (SQLException e) { 
                    System.out.println("Search failed: " + e.getMessage()); 
                } return false; }

        // --------------------------- // EDIT ORDER // --------------------------- 
        public void editOrder() { 
            if (!searchOrder(order_id)) {
                System.out.println("Cannot edit. Order not found.");
                return;
            }

            System.out.println("====== Edit Order ======"); 
            System.out.println("1. Edit Remark"); 
            System.out.println("2. Edit Quantity"); 
            System.out.println("3. Confirm"); 
            System.out.print("Enter choice: "); 
            choice = sc.nextInt(); 
            sc.nextLine(); 
            switch (choice) { 
                case 1 -> { System.out.print("New Remark: "); 
                String newRemark = sc.nextLine(); 
                String sql = "UPDATE orders_items SET i_remark = ? WHERE order_id = ?"; 
                try (PreparedStatement pstmt = orderConn.prepareStatement(sql)) { 
                    pstmt.setString(1, newRemark); 
                    pstmt.setString(2, order_id); 

                    
                    if (pstmt.executeUpdate() > 0) {
                        System.out.println("Remark updated successfully."); 
                    }
                } catch (SQLException e) { System.out.println("Update failed: " + e.getMessage()); } } 
                case 2 -> { 
                    System.out.print("New Quantity: "); 
                    int newQty = sc.nextInt(); 

                    String sql = "UPDATE order_items SET i_quantity = ? WHERE order_id = ? AND i_id = ?";


                    try (PreparedStatement pstmt = orderConn.prepareStatement(sql)) 
                    { 
                        pstmt.setInt(1, newQty); 
                        pstmt.setString(2, order_id); 
                        pstmt.setString(3, i_id);

                        
                        if (pstmt.executeUpdate() > 0) System.out.println("Quantity updated successfully."); 
                    } catch (SQLException e) { System.out.println("Update failed: " + e.getMessage()); } } 
                case 3 -> {
                        System.out.println("Order confirmed.");
                        return; 
                } 
                default ->
                {
                    System.out.println("Invalid choice.");

                }
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
                case 3 -> editOrder(); 
                case 4 -> System.out.println("Exiting..."); 
                default -> System.out.println("Invalid choice."); 
            } 
        } while (choice != 4); }

}
