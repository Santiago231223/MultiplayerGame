package database.alikindoger.com;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

import redis.alikindoger.com.RedisManager;

public class DatabaseManager {

    // --- Configuración de la Base de Dato
    private static final String JDBC_URL = "jdbc:mariadb://localhost:3306/myGameDB";
    private static final String DB_USER = "gameDev"; 
    private static final String DB_PASSWORD = "gameDev";

    // Constructor privado para evitar instanciación, usamos métodos estáticos
    private DatabaseManager() {}


    private static Connection getConnection() {
        try {
            // Cargar el driver JDBC (no siempre necesario, pero buena práctica)
            Class.forName("org.mariadb.jdbc.Driver");
            
            // Establecer la conexión
            return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Driver de MariaDB no encontrado.");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            System.err.println("Error de conexión a la base de datos. Verifica URL, usuario y contraseña.");
            e.printStackTrace();
            return null;
        }
    }

// --------------------------------------------------------------------------
// MÉTODO DE REGISTRO (INSERTAR USUARIO)
// --------------------------------------------------------------------------

    /**
     * Registra un nuevo usuario en la base de datos.
     * La contraseña se hashea usando BCrypt antes de almacenarse.
     * @return true si el registro fue exitoso, false en caso contrario (p.ej., usuario ya existe).
     */
    public static boolean registrarUsuario(String nombreUsuario, String password, String email) {
        // 1. Hashear la contraseña de forma segura
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // 2. Definir la consulta SQL
        String sql = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 3. Asignar los valores a los parámetros (?)
            pstmt.setString(1, nombreUsuario);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, email);

            // 4. Ejecutar la inserción
            int filasAfectadas = pstmt.executeUpdate();
            
            return filasAfectadas > 0;

        } catch (SQLException e) {
            // Error: lo más probable es que el nombre_usuario o email ya existan (UNIQUE constraint)
            System.err.println("Error al registrar usuario: " + e.getMessage());
            return false;
        }
    }

// --------------------------------------------------------------------------
// MÉTODO DE INICIO DE SESIÓN (CONSULTAR Y VERIFICAR)
// --------------------------------------------------------------------------

    /**
     * Verifica las credenciales de un usuario para iniciar sesión.
     * Compara la contraseña proporcionada con el hash almacenado.
     * @return El ID del usuario si las credenciales son correctas, -1 en caso de fallo.
     */
    public static int iniciarSesion(String nombreUsuario, String password) {
        int usuarioId = -1;
        // Solo necesitamos el hash y el ID
        String sql = "SELECT user_id, password_hash FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 1. Asignar el nombre de usuario
            pstmt.setString(1, nombreUsuario);

            // 2. Ejecutar la consulta
            try (ResultSet rs = pstmt.executeQuery()) {
                
                if (rs.next()) {
                    // 3. Obtener el hash almacenado
                    String hashedPassword = rs.getString("password_hash");
                    usuarioId = rs.getInt("user_id");

                    // 4. Verificar la contraseña con BCrypt
                    // BCrypt.checkpw compara la contraseña en texto plano con el hash
                    if (BCrypt.checkpw(password, hashedPassword)) {
                        // Contraseña Correcta
                        return usuarioId; 
                    } else {
                        // Contraseña Incorrecta
                        return -1;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al intentar iniciar sesión: " + e.getMessage());
        }
        // Usuario no encontrado o error
        return -1; 
    }
    
    public static void ResetTable() {
    	
        String sql = "TRUNCATE TABLE users";
        
        try(Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)){
        		if(pstmt.execute()) {
        			System.out.println("Tabla usuarios borrada con exito");
        		}

        	
        } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	
    }
    
 // --------------------------------------------------------------------------
 // MÉTODO DE CARGA DE PERSONAJES (SOLO DEVUELVE EL PRIMERO)
 // --------------------------------------------------------------------------
    
    public static Map<String, String> loadCharacterData(String username) {
        
        // los datos del personaje que pertenece al 'username'.
        String sql = "SELECT c.char_id, c.char_name, c.level, c.experience, c.pos_x, c.pos_y " +
                     "FROM characters c JOIN users u ON c.user_id = u.user_id " +
                     "WHERE u.username = ? LIMIT 1"; // esta linea carga el primer personaje
        
        Map<String, String> charData = new HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    charData.put("char_id", String.valueOf(rs.getInt("char_id")));
                    charData.put("char_name", rs.getString("char_name"));
                    charData.put("level", String.valueOf(rs.getInt("level")));
                    charData.put("experience", String.valueOf(rs.getInt("experience")));
                    charData.put("x", String.valueOf(rs.getInt("pos_x")));
                    charData.put("y", String.valueOf(rs.getInt("pos_y")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar datos del personaje: " + e.getMessage());
        }
        return charData;
    }
    
    public static boolean saveCharacterData(String charId, Map<String,String> data) {
    	
    	String sql = "UPDATE characters SET pos_x = ?, pos_y = ?, level = ?, experience = ?, last_login = NOW() WHERE char_id = ?";
    	
    	try ( Connection conn = getConnection();
    			PreparedStatement pstmt = conn.prepareStatement(sql)){
    		
    		pstmt.setFloat(1, Float.valueOf(data.get("x")));
    		pstmt.setFloat(2, Float.valueOf(data.get("y")));
    		pstmt.setInt(3, Integer.valueOf(data.get("level")));
    		pstmt.setInt(4, Integer.valueOf(data.get("experience")));
    		pstmt.setInt(5, Integer.valueOf(charId));

    		int rows = pstmt.executeUpdate();
    		if(rows>0) {
    			System.out.println("[DB]: Player save succesful in DB");
    			return true;
    		}else {
    			System.out.println("[DB]: Player couldnt save");
    			return false;
    		}
    		
    	}catch(SQLException e){
            System.err.println("[DB]: Error saving character " + e.getMessage());
    	}
    	
    	return true;
    }
    
    
}