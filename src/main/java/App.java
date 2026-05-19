import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "9999"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/guardar", new MiHandler());
        server.createContext("/", new FormularioHandler());

        server.setExecutor(null);

        server.start();

        System.out.println("Servidor iniciado en puerto 9999");
    }

    static class FormularioHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File("formulario.html");
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
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
}