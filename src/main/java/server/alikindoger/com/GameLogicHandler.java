package server.alikindoger.com;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;


import database.alikindoger.com.DatabaseManager;

public class GameLogicHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	
    private static final Gson gson = new Gson();
    private static final ChannelGroup channels = 
            new DefaultChannelGroup(GlobalEventExecutor.INSTANCE); //en vez del CTX usamos channels para que se replique en todos los clientes

	
    // Se llama al recibir un menseja del cliente
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
        
    	//con la informacion que llega construimos el JSON
    	String request = frame.text();
        JsonObject jsonObject = gson.fromJson(request, JsonObject.class);
        
    	
        Channel incoming = ctx.channel(); //referencia a la conexion
        
        
        
        System.out.println("Mensaje recibido del cliente: " + request);
        
         

        // PARSEO DE DATOS

        String commandType = jsonObject.get("type").getAsString();
        
        switch (commandType) {
		case "LOGIN":
            handleLoginMessage(incoming, request);
			break;
			
		case "SIGNIN":
			handlerSigninMessage(incoming,request);
			break;
			
		case "LOGOUT":
			handleLogoutMessage();
			break;			
		default:
			break;
		}
        
    }
    
    private void handleLogoutMessage() {
		// TODO Auto-generated method stub
		
	}

	private void handlerSigninMessage(Channel client, String message) {
		String response="";
    	
        try {
            // parsear json
            JsonObject jsonObject = gson.fromJson(message, JsonObject.class);
            String username = jsonObject.get("user").getAsString();
            String email = jsonObject.get("email").getAsString();
            String password = jsonObject.get("pass").getAsString();
            
            boolean isValid = DatabaseManager.registrarUsuario(username, password, email);
            
            if(isValid) {
            	response = "{\"type\":\"SIGNIN_OK\" }";
            	
            	
            }
            else {
            	response = "{\"type\":\"LOGIN_FAILED\", \"reason\":\"Credenciales no válidas, vuelva a intentarlo\"}";
            }
            
        }catch (Exception e) {
			// TODO: handle exception
		}
    	
        client.writeAndFlush(new TextWebSocketFrame(response));

	}

	// Se llama cuando una nueva conexión es establecida (Jugador conectado)
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // TODO: registrar conexiones channel en una lista
        System.out.println("Jugador conectado: " + ctx.channel().id());
    }
    
    // implementa logica de login
    private void handleLoginMessage(Channel client, String message) {
        String response;
        
        try {
            // parsear json
            JsonObject jsonObject = gson.fromJson(message, JsonObject.class);
            String username = jsonObject.get("user").getAsString();
            String password = jsonObject.get("pass").getAsString();

            System.out.println("Intentando iniciar sesión para: " + username);

            //verificar en base de datos
            int playerId = DatabaseManager.iniciarSesion(username, password);

            if (playerId != -1) {
                // exito
                response = String.format("{\"type\":\"LOGIN_OK\", \"id\":%d, \"username\":\"%s\"}", 
                                         playerId, username);
                
                // TODO: guardar referencias de los channels

            } else {
               
            	//client.close(); //kick por fallar login
                response = "{\"type\":\"LOGIN_FAILED\", \"reason\":\"Credenciales no válidas, vuelva a intentarlo\"}";
            }

        } catch (Exception e) {
            // Error en el formato JSON o en la DB
            System.err.println("Error procesando LOGIN: " + e.getMessage());
            response = "{\"type\":\"ERROR\", \"msg\":\"Fallo interno del servidor durante el login\"}";
        }
        
        // enviar response
        client.writeAndFlush(new TextWebSocketFrame(response));
    }
    

    // Se llama cuando la conexión se pierde (Jugador desconectado)
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // Lógica de desconexión, p. ej., guardar el estado del jugador en MariaDB
        System.out.println("Jugador desconectado: " + ctx.channel().id());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}