package pointofsale;

import java.io.IOException;
import java.net.Socket;

// IOException should probably be "stuff broke temporarily, on the outside"
class ModemDidNotConnectException extends IOException {

    public ModemDidNotConnectException() {
//      super("Modem cannot connect to server.Restart might help!!");
      super("Modem cannot connect to server.Restart might help!!",new Throwable("Modem machine is very old"));
    }
}

class ModemLibrary {

    public static void dialModem(int number) throws ModemDidNotConnectException {
       throw new ModemDidNotConnectException();
    }
}

public class PointOfSale {

    public static boolean USE_INTERNET = false;

    public void getPaid(int amount) throws ModemDidNotConnectException , IOException {

        boolean success = false;
        int retryCount = 3;
        while (!success && retryCount > 0) {
            try {
                if (USE_INTERNET) {
                    Socket s = new Socket("127.0.0.1", 9000);
                } else {
                    ModemLibrary.dialModem(1234);
                    // request payment
                    // if no money?
                    // if successful
                }
                success = true;
            } catch (ModemDidNotConnectException me /*| IOException me*/) {
                retryCount--;
                // log??
                if (retryCount == 0) throw me;
    //      } catch (IOException me) {
    //        retryCount--;
    //        // log??
    //        if (retryCount == 0) {
    //          throw me;
    //        }
            }
        }
    }

    public void sellStuff() /*throws ModemDidNotConnectException*/ {
        // add up individual items
        // charge payment
        // if (pay by card)
        try {
            getPaid(1000);
        } catch (ModemDidNotConnectException me) {
          System.out.println("Error Message : "+me.getMessage());
          System.out.println("Error Cause : "+me.getCause().getMessage());
        } catch (IOException me) {
            // ask for other payment type -- BUSINESS LOGIC RECOVERY!!!
        }
    }

    public static void main(String[] args) {
        PointOfSale pos = new PointOfSale();
        pos.sellStuff();
    }
}
