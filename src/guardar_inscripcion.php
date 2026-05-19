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

// ── 3. CREAR TABLA SI NO EXISTE ────────────────────────────
function crearTabla(PDO $pdo): void {

    $pdo->exec("
        CREATE TABLE IF NOT EXISTS inscripciones (

            id                INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

            nombre_padre      VARCHAR(150) NOT NULL,
            telefono          VARCHAR(30)  NOT NULL,
            email             VARCHAR(150) NOT NULL,
            direccion         VARCHAR(255) NOT NULL,

            nombre_hijo       VARCHAR(150) NOT NULL,
            fecha_nacimiento  DATE         NOT NULL,
            colegio           VARCHAR(150) NOT NULL,
            curso             VARCHAR(60)  NOT NULL,

            discapacidad      ENUM('si','no') NOT NULL,

            derivacion_ss     ENUM('si','no') NOT NULL,
            derivacion_gadir  ENUM('si','no') NOT NULL,

            comentario        TEXT DEFAULT NULL,

            auth_actividades  TINYINT(1) NOT NULL DEFAULT 0,
            auth_imagenes     TINYINT(1) NOT NULL DEFAULT 0,

            fecha_registro    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP

        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    ");
}

// ── 4. HELPERS ─────────────────────────────────────────────
function limpiar(string $valor): string {
    return trim(strip_tags($valor));
}

function respuesta(bool $ok, string $mensaje = '', string $error = ''): never {

    echo json_encode(
        $ok
            ? [
                'ok' => true,
                'mensaje' => $mensaje
            ]
            : [
                'ok' => false,
                'error' => $error
            ]
    );

    exit;
}

// ── 5. SOLO MÉTODO POST ────────────────────────────────────
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respuesta(false, error: 'Método no permitido.');
}

// ── 6. RECOGER DATOS ───────────────────────────────────────
$nombre_padre     = limpiar($_POST['nombre_padre'] ?? '');
$telefono         = limpiar($_POST['telefono'] ?? '');
$email_raw        = limpiar($_POST['email'] ?? '');
$direccion        = limpiar($_POST['direccion'] ?? '');

$nombre_hijo      = limpiar($_POST['nombre_hijo'] ?? '');
$fecha_nacimiento = limpiar($_POST['fecha_nacimiento'] ?? '');
$colegio          = limpiar($_POST['colegio'] ?? '');
$curso            = limpiar($_POST['curso'] ?? '');

$discapacidad     = $_POST['discapacidad'] ?? '';
$derivacion_ss    = $_POST['derivacion_ss'] ?? '';
$derivacion_gadir = $_POST['derivacion_gadir'] ?? '';

$comentario       = limpiar($_POST['comentario'] ?? '');

$auth_actividades = ($_POST['auth_actividades'] ?? '0') === '1' ? 1 : 0;
$auth_imagenes    = ($_POST['auth_imagenes'] ?? '0') === '1' ? 1 : 0;

// ── 7. VALIDACIONES ────────────────────────────────────────

// Todos obligatorios excepto comentario
$campos_obligatorios = [

    $nombre_padre,
    $telefono,
    $email_raw,
    $direccion,

    $nombre_hijo,
    $fecha_nacimiento,
    $colegio,
    $curso,

    $discapacidad,
    $derivacion_ss,
    $derivacion_gadir
];

foreach ($campos_obligatorios as $campo) {

    if ($campo === '') {
        respuesta(false, error: 'Debes rellenar todos los campos obligatorios.');
    }
}

// Validar email
$email = filter_var($email_raw, FILTER_VALIDATE_EMAIL);

if (!$email) {
    respuesta(false, error: 'Correo electrónico no válido.');
}

// Validar fecha
if (!DateTime::createFromFormat('Y-m-d', $fecha_nacimiento)) {
    respuesta(false, error: 'Formato de fecha incorrecto.');
}

// Validar radios
if (!in_array($discapacidad, ['si', 'no'])) {
    respuesta(false, error: 'Selecciona una opción en discapacidad.');
}

if (!in_array($derivacion_ss, ['si', 'no'])) {
    respuesta(false, error: 'Selecciona una opción en derivación SS.');
}

if (!in_array($derivacion_gadir, ['si', 'no'])) {
    respuesta(false, error: 'Selecciona una opción en derivación GADIR.');
}

// Validar autorizaciones
if (!$auth_actividades || !$auth_imagenes) {
    respuesta(false, error: 'Debes aceptar las autorizaciones.');
}

// ── 8. GUARDAR EN BASE DE DATOS ────────────────────────────
try {

    $pdo = conectar();

    crearTabla($pdo);

    $stmt = $pdo->prepare("

        INSERT INTO inscripciones (

            nombre_padre,
            telefono,
            email,
            direccion,

            nombre_hijo,
            fecha_nacimiento,
            colegio,
            curso,

            discapacidad,

            derivacion_ss,
            derivacion_gadir,

            comentario,

            auth_actividades,
            auth_imagenes

        )

        VALUES (

            :nombre_padre,
            :telefono,
            :email,
            :direccion,

            :nombre_hijo,
            :fecha_nacimiento,
            :colegio,
            :curso,

            :discapacidad,

            :derivacion_ss,
            :derivacion_gadir,

            :comentario,

            :auth_actividades,
            :auth_imagenes
        )

    ");

    $stmt->execute([

        ':nombre_padre'     => $nombre_padre,
        ':telefono'         => $telefono,
        ':email'            => $email,
        ':direccion'        => $direccion,

        ':nombre_hijo'      => $nombre_hijo,
        ':fecha_nacimiento' => $fecha_nacimiento,
        ':colegio'          => $colegio,
        ':curso'            => $curso,

        ':discapacidad'     => $discapacidad,

        ':derivacion_ss'    => $derivacion_ss,
        ':derivacion_gadir' => $derivacion_gadir,

        ':comentario'       => $comentario ?: null,

        ':auth_actividades' => $auth_actividades,
        ':auth_imagenes'    => $auth_imagenes,
    ]);

    respuesta(
        true,
        'Inscripción guardada correctamente con ID: ' . $pdo->lastInsertId()
    );

} catch (PDOException $e) {

    error_log('Error BD inscripciones: ' . $e->getMessage());

    respuesta(
        false,
        error: 'Error interno al guardar los datos.'
    );
}
?>