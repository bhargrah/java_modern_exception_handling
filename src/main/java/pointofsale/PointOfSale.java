package pointofsale;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

// IOException should probably be "stuff broke temporarily, on the outside"
class ModemDidNotConnectException extends IOException /*Exception*/ {}

class ModemLibrary {
  public static void dialModem(int number)
    throws ModemDidNotConnectException {}
}

public class PointOfSale {
  public static boolean USE_INTERNET = true;
  public void getPaid(int amount)
    throws ModemDidNotConnectException, IOException {
    boolean success = false;
    int retryCount = 3;
    while (! success && retryCount > 0) {
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
      } catch (/*ModemDidNotConnectException | */IOException me) {
        retryCount--;
        // log??
        if (retryCount == 0) {
          throw me;
        }
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
//    } catch (ModemDidNotConnectException me) {
    } catch (IOException me) {
      // ask for other payment type -- BUSINESS LOGIC RECOVERY!!!
    }
  }

  public static void main(String[] args) {

  }
}
