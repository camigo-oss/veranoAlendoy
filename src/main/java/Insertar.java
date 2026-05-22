import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class Insertar {
    private ConexionMySql SQL = new ConexionMySql();
    private Connection conn = SQL.conectarMySql();
    private String sSQL = "";

    public Insertar() {

    }

    public int insertarPadre(String nombre_padre, String telefono, String direccion, String email, String comentario) {

        try {
            sSQL = "INSERT INTO alendoy.Padre (nombre_padre,telefono, direccion, email, comentario) VALUES(?,?,?,?,?);";
            PreparedStatement pstm = conn.prepareStatement(
                    sSQL,
                    PreparedStatement.RETURN_GENERATED_KEYS);
            pstm.setString(1, nombre_padre);
            pstm.setString(2, telefono);
            pstm.setString(3, direccion);
            pstm.setString(4, email);
            pstm.setString(5, comentario);

            System.out.println(pstm.toString());
            pstm.executeUpdate();
            // res.get

            ResultSet rs = pstm.getGeneratedKeys();

            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return -1;
        }

    }

    public void insertarHijo(int id_padre, String nombre_apellido, LocalDate fecha_nacimiento, String colegio,
            String curso, String discapacidad, String derivacionss, String derivacion_gadir, int auth_actividades,
            int auth_imagenes) {
        try {
            sSQL = "INSERT INTO alendoy.Hijo (id_padre,nombre_apellido, fecha_nacimiento, colegio, curso, discapacidad, derivacionss,derivacion_gadir,auth_actividades,auth_imagenes) VALUES(?,?,?,?,?,?,?,?,?,?);";
            PreparedStatement pstm = conn.prepareStatement(sSQL);

            if (id_padre != 0) {
                pstm.setInt(1, id_padre);
            } else {
                pstm.setNull(1, java.sql.Types.INTEGER);

            }
            // nombre
            if (nombre_apellido != null) {
                pstm.setString(2, nombre_apellido);
            } else {
                pstm.setNull(2, java.sql.Types.VARCHAR);
            }

            // fecha
            if (fecha_nacimiento != null) {
                pstm.setDate(3, Date.valueOf(fecha_nacimiento));
            } else {
                pstm.setNull(3, java.sql.Types.DATE);
            }

            // colegio
            if (colegio != null) {
                pstm.setString(4, colegio);
            } else {
                pstm.setNull(4, java.sql.Types.VARCHAR);
            }

            if (curso != null) {
                pstm.setString(5, curso);
            } else {
                pstm.setNull(5, java.sql.Types.VARCHAR);

            }
            if (discapacidad != null) {
                pstm.setString(6, discapacidad);
            } else {
                pstm.setString(6, "NO");
            }

            pstm.setString(7, derivacionss);
            pstm.setString(8, derivacion_gadir);
            pstm.setInt(9, auth_actividades);
            pstm.setInt(10, auth_imagenes);

            pstm.executeUpdate();

            System.out.println(pstm.toString());

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
