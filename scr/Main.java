import java.util.*;

public class Main {
    
    Scanner sc = new Scanner(System.in);
    
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

    

    public static void main(String[] args) {
        Main m = new Main();  
        Order order = new Order();
        Scanner sc = new Scanner(System.in);
        KitchenStaff ks = new KitchenStaff(order);
        
        

        int userChoice;

        // if (!order.connect()) {
        //     return;
        // }


       
        do { 
        System.out.println("\n----------------------------------------------");
        System.out.println("Welcome to the Food Ordering Management System");
        System.out.println("----------------------------------------------");
        System.out.println("1.Waiter&Cashier");
        System.out.println("2.Kitchen Staff");
        System.out.println("3. END");
        userChoice = m.getRequiredInt("Select your role: ");
        
        switch(userChoice)
        {
            case 1 : order.orderSystem();break;
            case 2 :ks.updateStatus();break;
            case 3 :
            {
                System.out.println("Bye. See you next time!" + "\n");
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


