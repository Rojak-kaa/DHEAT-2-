import java.util.*;

public class Main {
    
    

    public static void main(String[] args) {
        Order order = new Order();
        Billing billing = new Billing();
        Scanner sc= new Scanner(System.in);
        KitchenStaff ks = new KitchenStaff(order);
        
        

        int userChoice;

        // if (!order.connect()) {
        //     return;
        // }


       
        do { 
        System.out.println("---------------------------------------------");
        System.out.println("Welcome to DHEAT Restaurant Management System");
        System.out.println("---------------------------------------------");
        System.out.println("1.Waiter&Cashier");
        System.out.println("2.Kitchen Staff");
        System.out.println("3. END");
        System.out.print("Select your role: ");
        userChoice = sc.nextInt();
        sc.nextLine();
        switch(userChoice)
        {
            case 1 : order.orderSystem();break;
            case 2 :ks.updateStatus();break;
            case 3 :
            {
                System.out.println("Bye. See you next time!");
                System.exit(0);
                break;

            }

            default :
            {
                System.out.println("Invalid choice.Enter 1,2 or 3 only.");
                break;
            }
        }
            
        } while (userChoice!=3);


    }
    
}


