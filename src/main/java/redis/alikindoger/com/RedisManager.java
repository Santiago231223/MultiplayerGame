package redis.alikindoger.com;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Jedis;

public class RedisManager {

    // --- Configuración (AJUSTAR si Redis no está en localhost) ---
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static JedisPool pool;

    private RedisManager() {} // Constructor privado para evitar instanciación

    // Bloque estático para inicializar el pool al cargar la clase
    static {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(128); // Máximo de conexiones en el pool
            poolConfig.setMaxIdle(32);   // Máximo de conexiones inactivas
            
            // Inicialización del pool de Jedis
            pool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, 2000); // 2000ms timeout
            
            // Prueba de conexión
            try (Jedis jedis = pool.getResource()) {
                if ("PONG".equals(jedis.ping())) {
                    System.out.println("Redis Pool inicializado y conexión exitosa.");
                } else {
                    throw new RuntimeException("Error: Redis no respondió PONG.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar RedisManager: " + e.getMessage());
            // System.exit(1);
        }
    }

    /**
     * Obtiene una conexión de Jedis del pool.
     * Es crucial usar esta conexión dentro de un bloque try-with-resources.
     */
    public static Jedis getResource() {
        return pool.getResource();
    }
}