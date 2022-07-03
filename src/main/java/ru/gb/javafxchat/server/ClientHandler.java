package ru.gb.javafxchat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ChatServer server;

    public String getNick() {
        return nick;
    }

    private String nick;
    private AuthService authService;

    public ClientHandler(Socket socket, ChatServer server, AuthService authService) {
        try {
            this.authService = authService;
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    authenticate();
                    readMessage();

                } finally {
                    closeConnection();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void authenticate() { // /auth login pass
        while (true) {
            try {
                String message = in.readUTF();
                if (message.startsWith("/auth")) {
                    String[] split = message.split("\\p{Blank}+");
                    String login = split[1];
                    String password = split[2];
                    String nick = authService.getNickByLoginAndPassword(login, password);

                    if (nick != null) {
                        if (server.isNickBusy(nick)) {
                            sendMessage("Пользователь уже авторизован");
                            continue;
                        }
                        sendMessage("/authok " + nick);
                        this.nick = nick;
                        server.broadcast("Пользователь " + nick + " зашел в чат");
                        server.subscribe(this);
                        break;
                    } else {
                        sendMessage("Неверные логин и/или пароль");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeConnection() {
        sendMessage("/end");
        server.unsubscribe(this);
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String s) {
        try {
            out.writeUTF(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMessage() {
        while (true) {
            final String message;
            boolean check = false;
            try {
                message = in.readUTF();
                String[] split = message.split("\\p{Blank}+");
                String cmd = split[0];

                if ("/end".equalsIgnoreCase(message)) {
                    break;
                }

                if (cmd.equals("/w")) {
                    server.privateMessaging(nick + " " + message);

                }
                else {
                    server.broadcast(nick + ": " + message);
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


}
