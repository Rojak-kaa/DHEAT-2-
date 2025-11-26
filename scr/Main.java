import java.util.*;

public class Main {
    
    

    public static void main(String[] args) {
        Order order = new Order();
        Scanner scan= new Scanner(System.in);

        int userChoice;

        // if (!order.connect()) {
        //     return;
        // }


       
        do { 
        System.out.println("---------------------------------------------");
        System.out.println("Welcome to DHEAT Restaurant Management System");
        System.out.println("---------------------------------------------");
        System.out.println("1.Waiter&Casher");
        System.out.println("2.Kitchen Staff");
        System.out.println("3. END");
        System.out.print("Select your role: ");
        userChoice = scan.nextInt();
        switch(userChoice)
        {
            case 1 ->
            {
                
                order.orderSystem();
            }
            case 2 ->
            {
                //KitchenStaff ks = new KitchenStaff();
                //ks.updateStatus();
            }
            case 3 ->
            {
                System.out.println("Exiting......");
                System.exit(0);
                break;

            }

            default -> {
                System.out.println("Invalid choice. Exiting program.");
                break;
            }
        }
            
        } while (userChoice != 3);


    }
    
}


