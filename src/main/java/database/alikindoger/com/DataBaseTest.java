package database.alikindoger.com;

public class DataBaseTest {

    public static void main(String[] args) {
        
        // --- Prueba de Registro ---
        System.out.println("--- REGISTRO ---");
        boolean registrado = DatabaseManager.registrarUsuario("JugadorAlfa", "P@ssword123", "alfa@juego.com");
        if (registrado) {
            System.out.println("Registro exitoso para JugadorAlfa.");
        } else {
            System.out.println("Registro fallido (Usuario/Email ya existe o error DB).");
        }

        System.out.println("\n--- INICIO DE SESIÓN ---");
        
        // --- Prueba de Inicio de Sesión (Correcta) ---
        int id1 = DatabaseManager.iniciarSesion("JugadorAlfa", "P@ssword123");
        if (id1 != -1) {
            System.out.println("Inicio de sesión exitoso. ID de Jugador: " + id1);
        } else {
            System.out.println("Inicio de sesión fallido (Credenciales incorrectas).");
        }
        
        // --- Prueba de Inicio de Sesión (Incorrecta) ---
        int id2 = DatabaseManager.iniciarSesion("JugadorAlfa", "ContraseñaMala");
        if (id2 == -1) {
            System.out.println("Fallo de inicio de sesión esperado (Contraseña incorrecta).");
        }
        
        //DatabaseManager.ResetTable();
    }

}
