import java.util.*;

public class KitchenStaff {

    private Order o;              // shared Order object from Main
    private Scanner sc = new Scanner(System.in);

    public KitchenStaff(Order order) {
        this.o = order;
    }

    public void updateStatus() {

        while (true) {

            // Show only pending orders
            List<String> pendingOrders = o.viewOrderByStatus("PENDING");  
            if (pendingOrders.isEmpty()) {
                System.out.println("No pending orders at the moment.");
            } else {
                System.out.println("Pending Orders:");
                System.out.println("------------------------------------------");
                for (String orderInfo : pendingOrders) {
                    System.out.println(orderInfo);
                }
                System.out.println("------------------------------------------");
            }

            System.out.println("1. Update Order Status to READY");
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
            boolean updated = o.updateOrderStatus(orderID, "READY");

            if (updated) {
                System.out.println("Order " + orderID + " marked as READY! Notify waiter.");
            } else {
                System.out.println("Order not found or already completed.");
            }
        }
    }
}
