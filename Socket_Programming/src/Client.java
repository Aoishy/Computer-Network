
import java.io.IOException;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException ,ClassNotFoundException{
        System.out.println("Place Your Input:");
        Scanner scn = new Scanner(System.in);
        String s = scn.nextLine();
        String input = s.split(" ")[0];
        while(!s.equalsIgnoreCase("exit")  && input.equalsIgnoreCase("upload")){
            new http_clientThread(s.split(" ")[1]);
            s = scn.nextLine();
            input = s.split(" ")[0];
        }
        scn.close();
    }
}


