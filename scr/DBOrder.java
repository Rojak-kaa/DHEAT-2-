import java.sql.*;


public class DBOrder{
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/order_list";
    private static final String USER = "root";
    private static final String PASSWORD = "UoW192411@";
    
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(URL , USER, PASSWORD);
    }
    
    public static void main(String[] args) {
        try {
            Connection conn = getConnection();
            System.out.println("Connection successful!");

            Statement stmt = conn.createStatement();
            ResultSet ol = stmt.executeQuery("SELECT * FROM order_list");

            while(ol.next()) {
                System.out.println(ol.getString("order_id"));
                System.out.println(ol.getString("i_id"));
                System.out.println(ol.getString("i_name"));
                System.out.println(ol.getInt("i_quantity"));
                System.out.println(ol.getString("i_remark"));
                System.out.println(ol.getDouble("total_price"));

            }


            conn.close();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
}