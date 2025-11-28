import java.util.*;

public class Payment {

    protected double tTotal;
    protected int choice;
    protected double cashAmount ;
    Scanner sc = new Scanner(System.in); 

    public double getTotal()
    {
        return tTotal;
    }

    public void payBill()
    {
        //protected double totalbill;
        //show bill

        System.out.println("You total amount is :" ); //+ total price from the order there
        do{
        System.out.println("Payment method: \n1.Cash \n2.QR/Debit card");
        System.out.println("Enter num:");
        choice = sc.nextInt();
        sc.nextLine();
            if(choice == 1)
            {
                System.out.println("Payment method: Cash");
                System.out.println("Total Amount is ");//+total amount from the order page
                System.out.print("Enter amount:");
                cashAmount = sc.nextDouble();

                /*if(cashAmount < totalAmount )
                {
                    do{
                    change = totalAmount - cashAmount;
                    System.out.println("You still need to paid "+ change);
                    System.our.print("Enter amount:" );
                    }while(totalAmount = cashAmount);
                    break;
                
                } 
                    else if(cashAmount > totalAmount )
                    {
                    
                        change = cashAmount - totalAmount;
                        System.out.println("Change: "+ change);
                        break;
                        
                        }
                    
                    }
                        else if(cashAmount == totalAmount )
                            {
                                break;}
                        System.out.println("Payment Successful");
                        break;
                */

            }
            else if(choice == 2)
            {
                System.out.println("Payment method : QR / Debit card");
                System.out.println("Total Amount is ");//+total amount from the order page
                System.out.println("Payment Successful");

            }
            else
                {
                    System.out.println("Invalid number. Try again.");
                }


            }while(choice != 1 && choice !=2 );

            System.out.println("Thank you for your purchace.");
    }

    
}
