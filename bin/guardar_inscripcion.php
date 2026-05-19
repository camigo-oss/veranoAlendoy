<?php
// ============================================================
//  guardar_inscripcion.php
//  Recibe los datos del formulario y los guarda en MySQL
// ============================================================

header('Content-Type: application/json; charset=utf-8');

// ── 1. CONFIGURACIÓN DE BASE DE DATOS ──────────────────────
//  Cambia estos valores por los de tu servidor
define('DB_HOST', 'localhost');
define('DB_NAME', 'nombre_base_datos');   // <-- cambia esto
define('DB_USER', 'usuario_db');          // <-- cambia esto
define('DB_PASS', 'contraseña_db');       // <-- cambia esto
define('DB_CHARSET', 'utf8mb4');

// ── 2. CONEXIÓN ─────────────────────────────────────────────
function conectar(): PDO {
    $dsn = 'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=' . DB_CHARSET;
    $opciones = [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ];
    return new PDO($dsn, DB_USER, DB_PASS, $opciones);
}

// ── 3. CREAR LA TABLA SI NO EXISTE ──────────────────────────
//  (Puedes quitar este bloque una vez creada la tabla en tu servidor)
function crearTabla(PDO $pdo): void {
    $pdo->exec("
        CREATE TABLE IF NOT EXISTS inscripciones (
            id                INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            nombre_padre      VARCHAR(150) NOT NULL,
            nombre_hijo       VARCHAR(150) NOT NULL,
            fecha_nacimiento  DATE         NOT NULL,
            colegio           VARCHAR(150)  DEFAULT NULL,
            curso             VARCHAR(60)   DEFAULT NULL,
            discapacidad      ENUM('si','no','') DEFAULT '',
            telefono          VARCHAR(30)  NOT NULL,
            direccion         VARCHAR(255)  DEFAULT NULL,
            email             VARCHAR(150)  DEFAULT NULL,
            comentario        TEXT          DEFAULT NULL,
            derivacion_ss     ENUM('si','no','') DEFAULT '',
            derivacion_gadir  ENUM('si','no','') DEFAULT '',
            auth_actividades  TINYINT(1)   NOT NULL DEFAULT 0,
            auth_imagenes     TINYINT(1)   NOT NULL DEFAULT 0,
            fecha_registro    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    ");
}

// ── 4. HELPERS ───────────────────────────────────────────────
function limpiar(string $valor): string {
    return trim(strip_tags($valor));
}

function respuesta(bool $ok, string $mensaje = '', string $error = ''): never {
    echo json_encode($ok
        ? ['ok' => true,  'mensaje' => $mensaje]
        : ['ok' => false, 'error'   => $error]
    );
    exit;
}

// ── 5. SOLO POST ─────────────────────────────────────────────
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respuesta(false, error: 'Método no permitido.');
}

// ── 6. RECOGER Y VALIDAR CAMPOS ─────────────────────────────
$nombre_padre     = limpiar($_POST['nombre_padre']     ?? '');
$nombre_hijo      = limpiar($_POST['nombre_hijo']      ?? '');
$fecha_nacimiento = limpiar($_POST['fecha_nacimiento'] ?? '');
$telefono         = limpiar($_POST['telefono']         ?? '');

// Campos obligatorios
if (!$nombre_padre || !$nombre_hijo || !$fecha_nacimiento || !$telefono) {
    respuesta(false, error: 'Faltan campos obligatorios.');
}

// Validar formato de fecha
if (!DateTime::createFromFormat('Y-m-d', $fecha_nacimiento)) {
    respuesta(false, error: 'Formato de fecha incorrecto.');
}

// Campos opcionales
$colegio          = limpiar($_POST['colegio']           ?? '');
$curso            = limpiar($_POST['curso']             ?? '');
$discapacidad     = in_array($_POST['discapacidad']     ?? '', ['si','no']) ? $_POST['discapacidad'] : '';
$direccion        = limpiar($_POST['direccion']         ?? '');
$email            = filter_var(limpiar($_POST['email'] ?? ''), FILTER_VALIDATE_EMAIL) ?: null;
$comentario       = limpiar($_POST['comentario']        ?? '');
$derivacion_ss    = in_array($_POST['derivacion_ss']    ?? '', ['si','no']) ? $_POST['derivacion_ss'] : '';
$derivacion_gadir = in_array($_POST['derivacion_gadir'] ?? '', ['si','no']) ? $_POST['derivacion_gadir'] : '';
$auth_actividades = isset($_POST['auth_actividades']) && $_POST['auth_actividades'] === '1' ? 1 : 0;
$auth_imagenes    = isset($_POST['auth_imagenes'])    && $_POST['auth_imagenes']    === '1' ? 1 : 0;

// ── 7. GUARDAR EN BASE DE DATOS ──────────────────────────────
try {
    $pdo = conectar();
    crearTabla($pdo);

    $stmt = $pdo->prepare("
        INSERT INTO inscripciones
            (nombre_padre, nombre_hijo, fecha_nacimiento, colegio, curso,
             discapacidad, telefono, direccion, email, comentario,
             derivacion_ss, derivacion_gadir, auth_actividades, auth_imagenes)
        VALUES
            (:nombre_padre, :nombre_hijo, :fecha_nacimiento, :colegio, :curso,
             :discapacidad, :telefono, :direccion, :email, :comentario,
             :derivacion_ss, :derivacion_gadir, :auth_actividades, :auth_imagenes)
    ");

    $stmt->execute([
        ':nombre_padre'     => $nombre_padre,
        ':nombre_hijo'      => $nombre_hijo,
        ':fecha_nacimiento' => $fecha_nacimiento,
        ':colegio'          => $colegio  ?: null,
        ':curso'            => $curso    ?: null,
        ':discapacidad'     => $discapacidad,
        ':telefono'         => $telefono,
        ':direccion'        => $direccion ?: null,
        ':email'            => $email,
        ':comentario'       => $comentario ?: null,
        ':derivacion_ss'    => $derivacion_ss,
        ':derivacion_gadir' => $derivacion_gadir,
        ':auth_actividades' => $auth_actividades,
        ':auth_imagenes'    => $auth_imagenes,
    ]);

    respuesta(true, 'Inscripción guardada con ID: ' . $pdo->lastInsertId());

} catch (PDOException $e) {
    // En producción no expongas $e->getMessage() al cliente
    error_log('Error BD inscripciones: ' . $e->getMessage());
    respuesta(false, error: 'Error interno al guardar los datos.');
}
