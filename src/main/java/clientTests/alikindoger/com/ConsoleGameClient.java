package clientTests.alikindoger.com;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class ConsoleGameClient {

    private static final String SERVER_URI = "ws://localhost:8080/ws";

    public static void main(String[] args) throws URISyntaxException {
        
        // El cliente de java-websocket es una subclase de WebSocketClient
        WebSocketClient client = new WebSocketClient(new URI(SERVER_URI)) {
         
            //eventos conexion
    
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("CONECTADO al servidor Netty.");
                System.out.println("Escribe tus mensajes y presiona Enter para enviar:");

                //String loginMsg = "{\"type\":\"LOGIN\", \"user\":\"JugadorAlfa\", \"pass\":\"P@ssword123\"}";
                String loginMsg = "{\"type\":\"SIGNIN\",\"user\":\"JugadorBeta\",\"email\":\"betaEmail@gmail.com\",\"pass\":\"P@ssword123\"}";
                send(loginMsg);
                System.out.println("-> Enviado: " + loginMsg);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("DESCONECTADO. Código: " + code + ", Razón: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("ERROR en la conexión WebSocket:");
                ex.printStackTrace();
            }

            
            @Override
            public void onMessage(String message) {
                System.out.println("<- RECIBIDO: " + message);
            }
        };

        // Conectar al servidor
        client.connect();

    }
}