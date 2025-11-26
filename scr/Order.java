import java.sql.*;
import java.util.*;


public class Order {

    private Connection conn;
    private Scanner sc = new Scanner(System.in);;

    protected String order_id;
    protected String i_id;
    protected String i_name;
    protected int i_quantity;
    protected double i_price;
    protected String i_remark;
    protected int choice;
    protected int f_quantity;
    protected int b_quantity;
    protected int d_quantity;
    protected String orderType;


    protected char userChoice;



    public Order()
    {
        
        order_id = null;
        i_name = null;
        i_quantity = 0;

        initializeDatabase();
        
    }

private void initializeDatabase() {
        try {
            this.conn = DBConn.getConnection();
            System.out.println("Database connection successful!");
            
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

public String typeOfOrder()
    {
        System.out.println("\nDine-in or Take away?");
        System.out.println("1.Dine-in\n2.Take away");
        System.out.print("Enter number: ");
        userChoice=sc.next().charAt(0);
        sc.nextLine();
        switch (userChoice) {
            case '1':
                orderType = "Dine-in";
                break;
            case '2':
                orderType = "Take away";
                break;
            default:
                orderType = "Unknown";
                System.out.println("Invalid number.");
                break;
        }

        return orderType;

    }


    // public boolean connect() {
    //     try {
    //         this.conn = DBConn.getConnection();
    //         System.out.println("Connection successful!");
    //         return true;
    //     } catch (SQLException e) {
    //         System.out.println("Database connection failed: " + e.getMessage());
    //         return false;
    //     }
    // }


    public boolean isItemExist(String id) {
    String sql = "SELECT 1 FROM customer WHERE customerID = ?";

    try (Connection conn = DBConn.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, id);
        ResultSet rs = ps.executeQuery();

        return rs.next(); // true = exists

    } catch (Exception e) {
        e.printStackTrace();
    }
    return false;
}






    public void takeOrder() {

        DBConn dbc = new DBConn();
        
        //dbc.showMenu();
        
        
        // for(int i = 0; i<1000 ; i++)
        // {
        //     if(i<10)
        //     {
        //         order_id = "O"+"00"+(i+1);
        //     }
        //     else if(i>=10 && i<100)
        //         {
        //            order_id="O"+"0"+(i+1);
        //         }
        //     else if(i>=100 && i<1000 )
        //         {
        //             order_id = "O"+(i+1);
        //         }

         do{
        System.out.println("====== Order Details======");
        System.out.println("Order ID: "+order_id);
        System.out.print("Item ID: ");
        i_id = sc.next();







        System.out.print("Quantity: " );
        i_quantity = sc.nextInt();
        System.out.print("Remark: " );
        i_remark = sc.next();


        }while()



        }

        

       
        String sql="INSERT INTO order(order_id,i_id,i_quantity,i_remark)VALUES(?,?,?,?)";

        do{
        System.out.println("======== Confrim =======");
        System.out.println("1. Edit Order");
        System.out.println("2. Confirm Order");

        while (true) {
            try {
            choice = Integer.parseInt(sc.nextLine());
            break;
            } catch (NumberFormatException e) {
                System.out.print("Invalid input! Please enter a number: ");
            }

}

        switch(choice)
        {
            case 1 ->
            {
                editOrder();
                break;
            }
            case 2 ->
            {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1,order_id);
            pstmt.setString(2, i_id);
            pstmt.setInt(3, i_quantity);
            pstmt.setString(4,i_remark);
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Order added successfully!");
            }
            }catch (SQLException e) {
            System.out.println("Failed to add order: " + e.getMessage());
            }

        
            } 
        }
            
        }while(choice != 2);
        


         

    }

    public void viewOrder()
    {
        System.out.println("====== View Order ======");
        String sql = "SELECT (order_id,i_id, i_name,i_quantity,i_remark) FROM order";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                System.out.println("Order ID: " + rs.getString("order_id") +
                                 "| Item ID: " + rs.getString("i_id")+
                                 "| Item Name:"+ rs.getString("i_name")+
                                " | Quantity: "+rs.getInt("i_quantity")+
                                " | Price : "+ rs.getDouble("i_price")
                                );
            }
        } catch (SQLException e) {
            System.out.println("Failed to query students: " + e.getMessage());
        }
    }


    public void editOrder()
    {
        System.out.println("====== Edit Order ======");
        System.out.println("1. Edit Remark");
        System.out.println("2. Edit Quantity");
        System.out.println("3. Exit");
        System.out.print("Enter you choice: ");
        choice=sc.nextInt();

        switch(choice){
            case 1 ->
            {
                searchOrder();
                System.out.print("Enter new remark: ");
                i_remark = sc.nextLine();

                //Check do the user want to continue
                System.out.println("Do you want to continue?");
                System.out.println("1.yes \n 2.No");
                System.out.print("Enter you choice: ");
                choice= sc.nextInt();
                if(choice == 1)
                    {
                        editOrder();

                    }
                else if(choice == 2)
                    {
                        String sql ="UPDATE order SET i_remark = ? WHERE order_id = ?";

                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, i_remark);
                        pstmt.setString(2, order_id);
            
                        int rows = pstmt.executeUpdate();
                            if (rows > 0) {
                             System.out.println("Order updated successfully!");
                            } else {
                            System.out.println("Order not found");
                            }
                        } catch (SQLException e) {
                        System.out.println("Failed to update order: " + e.getMessage());
                        }
                        break;
                        
                    }
                else{
                    System.out.print("Invalide number");

                }

            }
            case 2 ->
            {
                searchOrder();
                do{
                System.out.print("Enter new quantity: ");
                i_quantity = sc.nextInt();
                }while(i_quantity < 0 && i_quantity>f_quantity && i_quantity>b_quantity && i_quantity>d_quantity );
                

                System.out.println("Do you want to continue?");
                System.out.println("1.yes \n 2.No");
                System.out.print("Enter you choice: ");
                while (true) {
                try {
                choice = Integer.parseInt(sc.nextLine());
                break;
                } catch (NumberFormatException e) {
                System.out.print("Invalid input! Please enter a number: ");
            }
                }       

                if(choice == 1)
                    {
                        editOrder();
                    }
                else if(choice == 2)
                    {
                        
                        String sql ="UPDATE order SET i_quantity = ? WHERE order_id = ?";

                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setInt(1, i_quantity);
                        pstmt.setString(2, order_id);
            
                        int rows = pstmt.executeUpdate();
                            if (rows > 0) {
                             System.out.println("Order updated successfully!");
                            } else {
                            System.out.println("Order not found");
                            }
                        } catch (SQLException e) {
                        System.out.println("Failed to update order: " + e.getMessage());
                        }
                        
                    }
                else{
                    System.out.print("Invalide number");
                }

            }

        }


        
    }

    public void searchOrder()
    {
        System.out.print("Enter Order ID :");
        order_id=sc.nextLine();

        String sql ="SELECT * FROM order WHERE order_id= ?";

         try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, order_id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("Found user: " +
                                 "ID: " + rs.getInt("studentId") +
                                 ", Name: " + rs.getString("studentName") );
            } else {
                System.out.println("Cannot found " + order_id );
            }
        } catch (SQLException e) {
            System.out.println("Invalid: " + e.getMessage());
        }


    }


    public void cancleOrder()
    {


    }



    public void orderSystem()
    {
        
        do{
        System.out.println("====== Waiter/ Casher =======");
        //System.out.println("What do you need to do?");
        System.out.println("1.Take Order");
        System.out.println("2.View Orders");
        System.out.println("3.Update Status");
        System.out.println("4.Exit");
        System.out.print("Enter you choice:");
        while (true) {
        try {
            choice = Integer.parseInt(sc.nextLine());
            break;
        } catch (NumberFormatException e) {
            System.out.print("Invalid input! Please enter a number: ");
        }
}

        switch(choice)
        {
            case 1 ->
            {
                takeOrder();
            }
            case 2 ->
            {
                viewOrder();
                System.out.println("You have selected Waiter&Casher");
                System.out.println("What do you need to do?");
                System.out.println("1.Take Order");
                System.out.println("2.View Orders");
                System.out.println("3.Update Status");
                while (true) {
    try {
        choice = Integer.parseInt(sc.nextLine());
        break;
    } catch (NumberFormatException e) {
        System.out.print("Invalid input! Please enter a number: ");
    }
}


            }

            case 3 ->
            {
                //ks.updateStatus();//
            }

            case 4 ->
            {
                System.out.println("Exiting......");
                return;
            }
            default->
            {
                System.out.println("Invalid number. Try again.");
                break;

            }
            
        }
        } while(choice!=4);

       

    }
