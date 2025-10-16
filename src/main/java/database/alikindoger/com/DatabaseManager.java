package database.alikindoger.com;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

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
        String sql = "INSERT INTO usuarios (nombre_usuario, contrasena_hash, email) VALUES (?, ?, ?)";

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
        String sql = "SELECT id, contrasena_hash FROM usuarios WHERE nombre_usuario = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 1. Asignar el nombre de usuario
            pstmt.setString(1, nombreUsuario);

            // 2. Ejecutar la consulta
            try (ResultSet rs = pstmt.executeQuery()) {
                
                if (rs.next()) {
                    // 3. Obtener el hash almacenado
                    String hashedPassword = rs.getString("contrasena_hash");
                    usuarioId = rs.getInt("id");

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
    	
        String sql = "TRUNCATE TABLE USUARIOS";
        
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
    
}