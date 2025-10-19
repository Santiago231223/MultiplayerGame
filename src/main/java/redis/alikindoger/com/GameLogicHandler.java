package redis.alikindoger.com;

import com.google.gson.JsonObject;
import io.netty.channel.Channel;
import redis.clients.jedis.Jedis;

public class GameLogicHandler {



	private void handleMoveMessage(Channel client, JsonObject message) {
	    
	    // NOTA: Debes obtener el ID real del jugador después del login. 
	    // Por ahora, usaremos el ID del canal como placeholder.
	    String playerId = client.id().asShortText(); 
	    
	    // ... (Asumo que tienes la lógica para obtener x e y de forma segura) ...
	    int x = message.get("x").getAsInt();
	    int y = message.get("y").getAsInt();
	    
	    // La clave en Redis para el estado de este jugador
	    String playerKey = "player:" + playerId; 

	    // Usamos try-with-resources para asegurar el cierre de la conexión de Jedis
	    try (Jedis jedis = RedisManager.getResource()) {
	        
	        // 1. Escribir la nueva posición en Redis (Hash Set)
	        jedis.hset(playerKey, "x", String.valueOf(x));
	        jedis.hset(playerKey, "y", String.valueOf(y));
	        
	        // 2. Establecer un TTL (Time To Live) o expiración si el jugador está inactivo
	        // Esto es útil para limpiar datos de jugadores que se desconectan bruscamente.
	        // jedis.expire(playerKey, 3600); // Expira en 1 hora si no se usa

	        System.out.println("Redis: Posición de " + playerId + " actualizada a (" + x + ", " + y + ")");

	    } catch (Exception e) {
	        System.err.println("Error de Redis durante la operación de movimiento: " + e.getMessage());
	        // En un juego, podrías querer cerrar la conexión si la DB falla
	    }
	    
	    // 3. Después de la operación ultrarrápida, realiza el Broadcast
	    // ... channels.writeAndFlush(new TextWebSocketFrame(broadcastMsg));
	
	}
}
