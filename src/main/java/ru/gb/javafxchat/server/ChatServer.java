package ru.gb.javafxchat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {

    private final List<ClientHandler> clients;
    private AuthService authService;

    public ChatServer() {
        this.clients = new ArrayList<>();
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(8189);
             AuthService authService = new InMemoryAuthService()) {

            while (true) {
                System.out.println("Ожидаю подключения ...");
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket, this, authService);
                System.out.println("Клиент подключен");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void privateMessaging(String message) {
        String[] split = message.split("\\p{Blank}+");
        String nick1 = split[0];
        String nick2 = split[2];
        String message1 = split[0]+" [для " + nick2 + "] :";
        String message2 = split[0]+":";

        for (int i = 3; i < split.length; i++) {
            message1 = message1 + " " + split[i];
            message2 = message2 + " " + split[i];
        }

        for (ClientHandler client : clients) {
            if (nick2.equals(client.getNick())) {
                client.sendMessage(message2);
            }

            if (nick1.equals(client.getNick())) {
                client.sendMessage(message1);
            }

        }


    }

    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }

    }

    public void subscribe(ClientHandler client) {
        clients.add(client);

    }

    public boolean isNickBusy(String nick) {
        for (ClientHandler client : clients) {
            if (nick.equals(client.getNick())) {
                return true;
            }
        }
        return false;
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
    }
}
