package server.alikindoger.com;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.ChannelMatchers;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import redis.alikindoger.com.RedisManager;
import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;


import database.alikindoger.com.DatabaseManager;

public class GameLogicHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	
    private static final Gson gson = new Gson();
    private static final Map<ChannelId, String> connectedCharacters = new ConcurrentHashMap<>();
    
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
		case "PLAYER_MOVE":
			handlePlayerMove(incoming,jsonObject);
			break;
		default:
			break;
		}
        
    }
    
    private void handlePlayerMove(Channel client, JsonObject request) {
		
    	String charId = connectedCharacters.get(client.id());
    	
    	if(charId == null) {
    		return; //jugador no autenticado
    	}
    	
    	String newX = request.get("x").getAsString();
    	String newY = request.get("y").getAsString();
    	
    	try(Jedis jedis = RedisManager.getResource()){
    		
    		String redisKey = "char:" + charId;
    		jedis.hset(redisKey,"x",newX);
    		jedis.hset(redisKey,"y",newY);
    		
    	}catch (Exception e) {
			// TODO: handle exception
		}
    	
    	String broadcastMsg = String.format(
    	        "{\"type\":\"PLAYER_POS\", \"char_id\":%s, \"x\":%s, \"y\":%s}",
    	        charId, newX, newY
    	    );
    	    
    	    // send to everyone
    	
    	    channels.writeAndFlush(new TextWebSocketFrame(broadcastMsg), ChannelMatchers.isNot(client));
    	
    	
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
            // parse json
            JsonObject jsonObject = gson.fromJson(message, JsonObject.class);
            String username = jsonObject.get("user").getAsString();
            String password = jsonObject.get("pass").getAsString();
  
            System.out.println("Intentando iniciar sesión para: " + username);

            //verify with DB
            int playerId = DatabaseManager.iniciarSesion(username, password);
            
            if (playerId != -1) {

                
                //prepare data for redis
                Map<String, String> charData = DatabaseManager.loadCharacterData(username);
                
                //we know fr there is a character
                if (charData.isEmpty()) { 
                    client.writeAndFlush(new TextWebSocketFrame("{\"type\":\"LOGIN_FAILED\", \"msg\":\"No se encontró personaje\"}"));
                    return;
                }else {
                	System.out.println("Error en inicio de sesión de "+username);
                }

                String charId = charData.get("char_id");
                String redisKey = "char:" + charId;
                
                try (Jedis jedis = RedisManager.getResource()) {
                    
                    // all character data!!
                    jedis.hset(redisKey, charData); 
                    
                    // TODO: we shall store last session hp
                    jedis.hset(redisKey, "status", "ONLINE");
                    jedis.hset(redisKey, "hp", "100");
                    
                } catch (Exception e) {
                    System.err.println("Error de Redis durante el cacheo del personaje " + charId + ": " + e.getMessage());
                    client.writeAndFlush(new TextWebSocketFrame("{\"type\":\"LOGIN_FAILED\", \"msg\":\"Error interno del servidor\"}"));
                    return;
                }
                //keep track of connected players and their connections
                connectedCharacters.put(client.id(), charId); 
                //keep track of connections
                channels.add(client);
                
                //private message for client logging
                String loginOkMsg = String.format(
                        "{\"type\":\"LOGIN_OK\", \"char_id\":%s, \"name\":\"%s\", \"x\":%s, \"y\":%s}",
                        charData.get("char_id"), charData.get("char_name"), charData.get("x"), charData.get("y"));
                
                client.writeAndFlush(new TextWebSocketFrame(loginOkMsg));
                System.out.println("Personaje " + charData.get("char_name") + " (ID: " + charId + ") cacheado en Redis y conectado.");
                
                //public message
                String joinMsg = String.format("{\"type\":\"PLAYER_JOIN\", \"char_id\":%s, \"name\":\"%s\", \"x\":%s, \"y\":%s}",
                        charId, charData.get("char_name"), charData.get("x"), charData.get("y"));
                //we send it to all client except ours
                channels.writeAndFlush(new TextWebSocketFrame(joinMsg),ChannelMatchers.isNot(client));
                
            } else {
               //mariaDB credentials incorrect
               client.writeAndFlush(new TextWebSocketFrame("{\"type\":\"LOGIN_FAILED\", \"reason\":\"Credenciales no válidas, vuelva a intentarlo\"}"));
            }

        } catch (Exception e) {
            // bad JSON or DB 
            System.err.println("Error procesando LOGIN: " + e.getMessage());
            client.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\", \"msg\":\"Fallo interno del servidor durante el login\"}"));
        }
    }
    

    // player discconected
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
       
    	//player connection
    	Channel client = ctx.channel();
    	
    	//connected player removal
    	String charId = connectedCharacters.remove(client.id());
    	//connection removal
    	channels.remove(client);
    	String redisKey = "char:" + charId;

    	
    	//check for ghost clients
    	if(charId ==null) {
    		System.out.println("[DB]: Player discconected without session");
    		return;
    	}
    	
    	try(Jedis jedis = RedisManager.getResource()){
    		
    		//get all the data
    		Map<String, String> finalState = jedis.hgetAll(redisKey);   
    		
    		if(finalState != null && !finalState.isEmpty()) {
    			
    			if(DatabaseManager.saveCharacterData(charId, finalState)) {
    				System.out.println("[DB] Character data correctly saved");
    			}
    			
    		}
    	}
    	
    	//send data to the rest of clients
    	String leaveMsg = String.format(
    	        "{\"type\":\"PLAYER_LEAVE\", \"char_id\":%s}",
    	        charId
    	    );
    	channels.writeAndFlush(new TextWebSocketFrame(leaveMsg),ChannelMatchers.isNot(client));
    	
    	
    	//jedis
    	try (Jedis jedis = RedisManager.getResource()){
    		
    		Map<String,String> finalState = jedis.hgetAll(redisKey);
    		
    		jedis.del(redisKey);
    		
    		System.out.println("[Redis]: Cleaned cache with id: " + charId + ".");
    		//persistency
    		
    		
    		
    		if(finalState != null && !finalState.isEmpty()) {
    			
    			if(DatabaseManager.saveCharacterData(charId,finalState)) {
    				
    				System.out.println("[DB]: Character correctly saved with id: " + charId);
    				
    			}else {
    				
    				System.out.println("[DB]: Character data couldnt save with id: " + charId);

    			}
    			
    		}
    		
    	}
    	
        System.out.println("[DB]: Jugador desconectado: " + ctx.channel().id());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}