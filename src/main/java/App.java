import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class App {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "9999"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/guardar", new MiHandler());
        server.createContext("/", new FormularioHandler());
        server.createContext("/admin", new AdminHandler());

        server.setExecutor(null);

        server.start();

        System.out.println("Servidor iniciado en puerto " + port);
    }

    static class FormularioHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                InputStream is = App.class.getResourceAsStream("/formulario.html");
                System.out.println("¿Recurso encontrado? " + (is != null));
                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                String response = "Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    static class MiHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // ── AÑADE ESTA LÍNEA ──────────────────────────────────────
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            // ─────────────────────────────────────────────────────────
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                body.append(line);
            }

            Map<String, String> datos = parseForm(body.toString());
            System.out.println(datos);

            try {
                Insertar insertar = new Insertar();

                int idPadre = insertar.insertarPadre(
                        datos.get("nombre_padre"),
                        datos.get("telefono"),
                        datos.get("direccion"),
                        datos.get("email"),
                        datos.get("comentario"));

                insertar.insertarHijo(
                        idPadre,
                        datos.get("nombre_hijo"),
                        LocalDate.parse(datos.get("fecha_nacimiento")),
                        datos.get("colegio"),
                        datos.get("curso"),
                        datos.get("discapacidad"),
                        datos.get("derivacion_ss"),
                        datos.get("derivacion_gadir"),
                        Integer.parseInt(datos.get("auth_actividades")),
                        Integer.parseInt(datos.get("auth_imagenes")));

                String response = "{\"ok\":true}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
                String response = "{\"ok\":false}";
                exchange.sendResponseHeaders(500, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        public static Map<String, String> parseForm(String formData) throws UnsupportedEncodingException {

            Map<String, String> map = new HashMap<>();

            String[] pairs = formData.split("&");

            for (String pair : pairs) {

                String[] keyValue = pair.split("=");

                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);

                String value = keyValue.length > 1
                        ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8)
                        : "";

                map.put(key, value);
            }

            return map;
        }
    }

    static class AdminHandler implements HttpHandler {
        private static final String PASSWORD = "alendoy2026";

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Verificar contraseña por query param
            String query = exchange.getRequestURI().getQuery();
            boolean autenticado = query != null && query.contains("pass=alendoy2026");

            if (!autenticado) {
                InputStream is = App.class.getResourceAsStream("/login.html");
                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
                return;
            }
            // Si pide HTML sirve admin.html
            String acceptHeader = exchange.getRequestHeaders().getFirst("Accept");
            if (acceptHeader != null && acceptHeader.contains("text/html")) {
                InputStream is = App.class.getResourceAsStream("/admin.html");
                byte[] bytes = is.readAllBytes();
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
                return;
            }

            // Obtener datos de la BD
            try {
                ConexionMySql sql = new ConexionMySql();
                java.sql.Connection conn = sql.conectarMySql();
                java.sql.PreparedStatement stmt = conn.prepareStatement(
                        "SELECT p.id, p.nombre_padre, p.telefono, p.email, p.direccion, " +
                                "h.nombre_apellido, h.fecha_nacimiento, h.colegio, h.curso, " +
                                "h.discapacidad, h.derivacionss, h.derivacion_gadir, " +
                                "h.auth_actividades, h.auth_imagenes, p.comentario " +
                                "FROM alendoy.Padre p JOIN alendoy.Hijo h ON p.id = h.id_padre " +
                                "ORDER BY p.id DESC");
                java.sql.ResultSet rs = stmt.executeQuery();

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first)
                        json.append(",");
                    first = false;
                    json.append("{")
                            .append("\"id\":").append(rs.getInt("id")).append(",")
                            .append("\"nombre_padre\":\"").append(rs.getString("nombre_padre")).append("\",")
                            .append("\"telefono\":\"").append(rs.getString("telefono")).append("\",")
                            .append("\"email\":\"").append(rs.getString("email") != null ? rs.getString("email") : "")
                            .append("\",")
                            .append("\"direccion\":\"")
                            .append(rs.getString("direccion") != null ? rs.getString("direccion") : "").append("\",")
                            .append("\"nombre_hijo\":\"").append(rs.getString("nombre_apellido")).append("\",")
                            .append("\"fecha_nacimiento\":\"").append(rs.getString("fecha_nacimiento")).append("\",")
                            .append("\"colegio\":\"")
                            .append(rs.getString("colegio") != null ? rs.getString("colegio") : "").append("\",")
                            .append("\"curso\":\"").append(rs.getString("curso") != null ? rs.getString("curso") : "")
                            .append("\",")
                            .append("\"discapacidad\":\"")
                            .append(rs.getString("discapacidad") != null ? rs.getString("discapacidad") : "")
                            .append("\",")
                            .append("\"derivacion_ss\":\"")
                            .append(rs.getString("derivacionss") != null ? rs.getString("derivacionss") : "")
                            .append("\",")
                            .append("\"derivacion_gadir\":\"")
                            .append(rs.getString("derivacion_gadir") != null ? rs.getString("derivacion_gadir") : "")
                            .append("\",")
                            .append("\"auth_actividades\":").append(rs.getInt("auth_actividades")).append(",")
                            .append("\"auth_imagenes\":").append(rs.getInt("auth_imagenes")).append(",")
                            .append("\"comentario\":\"")
                            .append(rs.getString("comentario") != null ? rs.getString("comentario") : "").append("\"")
                            .append("}");
                }
                json.append("]");
                conn.close();

                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = json.toString().getBytes("UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
                String response = "[]";
                exchange.sendResponseHeaders(500, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
}