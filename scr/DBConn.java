import java.sql.*;

public class DBConn {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/inventory";
    private static final String USER = "root";
    private static final String PASSWORD = "UoW192411@";
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("Connection successful!");
            
            // Show all menu items
            showMenu(conn);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void showMenu() {
        try (Connection conn = getConnection()) {
            showMenu(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static void showMenu(Connection conn) throws SQLException {
        System.out.println("\n======= MENU =======");
        
        showCategory(conn, "SELECT * FROM food", "FOOD");
        showCategory(conn, "SELECT * FROM beverage", "DRINK"); 
        showCategory(conn, "SELECT * FROM dessert", "DESSERT");
        
        System.out.println("===================\n");
    }
    
    private static void showCategory(Connection conn, String query, String categoryName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            System.out.println("\n--- " + categoryName + " ---");
            
            while (rs.next()) {
                String id = rs.getString(1);       // First column (id)
                String name = rs.getString(2);     // Second column (name)  
                double price = rs.getDouble(4);    // Fourth column (price)
                
                System.out.println(id + " | " + name + " | Price: RM " + price);
            }
        }
    }
}