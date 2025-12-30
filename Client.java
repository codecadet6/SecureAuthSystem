import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {

        try (Socket socket = new Socket("localhost", 5000)) {

            Scanner sc = new Scanner(System.in);

            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            System.out.print("Enter action (REGISTER / LOGIN): ");
            String action = sc.nextLine();

            System.out.print("Username: ");
            String username = sc.nextLine();

            System.out.print("Password: ");
            String password = sc.nextLine();

            out.writeUTF(action);
            out.writeUTF(username);
            out.writeUTF(password);

            String response = in.readUTF();
            System.out.println(response);

        } catch (Exception e) {
            System.out.println("Connection failed");
        }
    }
}
