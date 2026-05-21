import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionMySql {

    // Librería de MySQL
    public String driver = "com.mysql.cj.jdbc.Driver";

    // Nombre de la base de datos
    public String database = "alendoy";
    public String hostname = "veranoalendoy2026-veranoalendoy2026.i.aivencloud.com";

    // Puerto
    public String port = "20012";

    // Ruta de nuestra base de datos (desactivamos el uso de SSL con
    // "?useSSL=false")
    public String url = "jdbc:mysql://" + hostname + ":" + port + "/" + database + "?useSSL=true";

    // Nombre de usuario
    public String username = "avnadmin";

    // Clave de usuario
    public String password = "AVNS_rzSeofzltCV3iR8Afsh";

    public Connection conectarMySql() {
        Connection conn = null;

        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, username, password);
            System.out.println("Conectado correctamente");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        return conn;
    }

}