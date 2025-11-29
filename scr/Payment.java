import java.util.*;

public class Payment {

    protected double tTotal;
    protected int choice;
    protected double cashAmount;
    Scanner sc = new Scanner(System.in);

    public double getTotal() {
        return tTotal;
    }

    public void payBill(double totalAmount) {

        System.out.println("Your total amount is: RM " + totalAmount);

        // --- Choose payment method ---
        do {
            System.out.println("\nPayment Method:");
            System.out.println("1. Cash");
            System.out.println("2. QR / Debit Card");
            System.out.print("Enter number: ");
            choice = sc.nextInt();
        } while (choice != 1 && choice != 2);

        // --- CASH PAYMENT ---
        if (choice == 1) {
            System.out.println("\nPayment method: CASH");
            System.out.println("Total Amount: RM " + totalAmount);

            double cash;
            do {
                System.out.print("Enter cash amount: RM ");
                cash = sc.nextDouble();

                if (cash < totalAmount) {
                    System.out.println("❌ Not enough! You still need RM " + (totalAmount - cash));
                }

            } while (cash < totalAmount);

            double change = cash - totalAmount;

            System.out.println("Payment Successful!");

            if (change > 0) {
                System.out.println("Your change: RM " + change);
            }
        }

        // --- QR / Debit Card ---
        else if (choice == 2) {
            System.out.println("\nPayment method: QR / Debit Card");
            System.out.println("Processing...");
            System.out.println("Payment Successful!");
        }

        System.out.println("\nThank you for your purchase!");
    }
}
