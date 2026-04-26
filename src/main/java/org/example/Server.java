package org.example;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    static Vector<ClientHandler> ar = new Vector<>();
    static List<String> chatHistory = new ArrayList<>();
    static final String HISTORY_FILE = "chat_history.txt";

    static {
        // Load existing history from file when server starts
        try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                chatHistory.add(line);
            }
            System.out.println("Loaded " + chatHistory.size() + " messages from history.");
        } catch (FileNotFoundException e) {
            System.out.println("No previous chat history file found. Starting fresh.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static synchronized void addMessageToHistory(String msg) {
        chatHistory.add(msg);
        try (FileWriter fw = new FileWriter(HISTORY_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(6666);
        System.out.println("Server started on port 6666");

        while (true) {
            Socket s = ss.accept();
            System.out.println("New client request received: " + s);

            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            // 1. Read the username sent by client
            String username = dis.readUTF();
            // 2. Generate random 5-digit ID
            int id = 10000 + new Random().nextInt(90000);
            String formattedId = String.format("%05d", id);
            String fullName = formattedId + ":" + username;

            System.out.println("New user: " + fullName);

            ClientHandler mtch = new ClientHandler(s, fullName, dis, dos);
            Thread t = new Thread(mtch);
            ar.add(mtch);
            t.start();
        }
    }
}

class ClientHandler implements Runnable {
    Scanner scn = new Scanner(System.in);
    private String name;
    final DataInputStream dis;
    final DataOutputStream dos;
    Socket s;
    boolean isloggedin;

    // constructor

    public ClientHandler(Socket s, String name, DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
        this.name = name;
        this.s = s;
        this.isloggedin = true;
    }

    @Override
    public void run() {
        // Send welcome message with active users and history
        try {
            // Send list of active users (except self)
            StringBuilder userList = new StringBuilder("--- Active users: ");
            for (ClientHandler mc : Server.ar) {
                if (mc != this && mc.isloggedin) {
                    userList.append(mc.name).append(", ");
                }
            }
            if (userList.length() > 16) {
                userList.setLength(userList.length() - 2);
            }
            userList.append(" ---");
            dos.writeUTF(userList.toString());

            // Send chat history
            dos.writeUTF("--- Chat History ---");
            for (String msg : Server.chatHistory) {
                dos.writeUTF(msg);
            }
            dos.writeUTF("--- End of History ---");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String received;
        while (true) {
            try {
                received = dis.readUTF();
                System.out.println(received);  // server log

                if (received.equalsIgnoreCase("logout")) {
                    // Notify all others about leaving
                    String leaveMsg = this.name + " has left the chat.";
                    for (ClientHandler mc : Server.ar) {
                        if (mc.isloggedin && mc != this) {
                            mc.dos.writeUTF(leaveMsg);
                        }
                    }
                    this.isloggedin = false;
                    this.s.close();
                    break;
                }

                // Add timestamp
                String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                String formattedMsg = "[" + timeStamp + "] " + this.name + ": " + received;

                // Save to history
                Server.addMessageToHistory(formattedMsg);

                // Broadcast to all logged-in clients
                for (ClientHandler mc : Server.ar) {
                    if (mc.isloggedin) {
                        mc.dos.writeUTF(formattedMsg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        // Clean up
        try {
            this.dis.close();
            this.dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}