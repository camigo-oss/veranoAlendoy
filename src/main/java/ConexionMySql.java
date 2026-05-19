import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionMySql {

    // Librería de MySQL
    public String driver = "com.mysql.cj.jdbc.Driver";

    // Nombre de la base de datos
    public String database = "railway";
    public String hostname = "viaduct.proxy.rlwy.net";

    // Puerto
    public String port = "23797";

    // Ruta de nuestra base de datos (desactivamos el uso de SSL con
    // "?useSSL=false")
    public String url = "jdbc:mysql://" + hostname + ":" + port + "/" + database + "?useSSL=false";

    // Nombre de usuario
    public String username = "root";

    // Clave de usuario
    public String password = "DhtZRfVikwveKLGwXuexlAduBFMNieWl";

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