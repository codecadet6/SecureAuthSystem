import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;

public class Server {

    private static final String DB_FILE = "UserDatabase.txt";

    private static HashMap<String, Integer> attempts = new HashMap<>();
    private static HashMap<String, Long> lockTime = new HashMap<>();

    private static String generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        StringBuilder sb = new StringBuilder();
        for (byte b : salt)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static void saveUser(String username, String salt, String hash) throws IOException {
        FileWriter fw = new FileWriter(DB_FILE, true);
        fw.write(username + "|" + salt + "|" + hash + "\n");
        fw.close();
    }

    private static boolean verifyLogin(String username, String password) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(DB_FILE));
        String line;

        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\|");
            if (parts[0].equals(username)) {
                String salt = parts[1];
                String storedHash = parts[2];
                String loginHash = hashPassword(password, salt);
                br.close();
                return storedHash.equals(loginHash);
            }
        }
        br.close();
        return false;
    }

    public static void main(String[] args) {

        System.out.println("=== Secure Authentication Server Started ===");

        try (ServerSocket serverSocket = new ServerSocket(5000)) {

            while (true) {
                Socket client = serverSocket.accept();

                DataInputStream in = new DataInputStream(client.getInputStream());
                DataOutputStream out = new DataOutputStream(client.getOutputStream());

                String action = in.readUTF();
                String username = in.readUTF();
                String password = in.readUTF();

                long currentTime = System.currentTimeMillis();

                // Check lock status
                if (lockTime.containsKey(username)) {
                    if (currentTime - lockTime.get(username) < 30000) {
                        out.writeUTF("Account locked. Try again after 30 seconds");
                        client.close();
                        continue;
                    } else {
                        attempts.put(username, 0);
                        lockTime.remove(username);
                    }
                }

                if (action.equals("REGISTER")) {

                    String salt = generateSalt();
                    String hash = hashPassword(password, salt);
                    saveUser(username, salt, hash);
                    System.out.println("User registered: " + username);
                    out.writeUTF("Registration successful");

                } else if (action.equals("LOGIN")) {

                    if (verifyLogin(username, password)) {
                        attempts.put(username, 0);
                        System.out.println("User " + username + " logged in successfully");
                        out.writeUTF("Login successful");

                    } else {
                        int count = attempts.getOrDefault(username, 0) + 1;
                        attempts.put(username, count);
                        System.out.println("Failed attempt " + count + " for user " + username);

                        if (count >= 3) {
                            lockTime.put(username, currentTime);
                            System.out.println("User " + username + " locked for 30 seconds");
                            out.writeUTF("Account locked due to multiple failed attempts");
                        } else {
                            out.writeUTF("Invalid credentials");
                        }
                    }
                }

                client.close();
            }

        } catch (Exception e) {
            System.out.println("Server error");
        }
    }
}
