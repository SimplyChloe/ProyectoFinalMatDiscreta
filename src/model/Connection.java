package model;

import java.util.Objects;

public final class Connection {

    // ==========================================================
    // 1. ATRIBUTOS BÁSICOS (9)
    // ==========================================================

    /** Duración de la conexión en segundos. Rango: [0, 58329]. */
    private final double duration;

    /** Protocolo de la capa de transporte: "tcp", "udp" o "icmp". */
    private final String protocolType;

    /**
     * Servicio de red de destino a nivel de aplicación,
     * ej. "http", "ftp", "smtp", "telnet", "private", etc.
     * (70 valores posibles en el dataset original).
     */
    private final String service;

    /**
     * Estado (bandera) normal o de error de la conexión, tal como lo
     * reporta el protocolo. Valores típicos: "SF" (normal, completada),
     * "S0" (intento sin respuesta), "REJ" (rechazada), "RSTO", "RSTR", etc.
     */
    private final String flag;

    /** Bytes de datos enviados desde el origen hacia el destino. */
    private final long srcBytes;

    /** Bytes de datos enviados desde el destino hacia el origen. */
    private final long dstBytes;

    /**
     * Indica si origen y destino están en la misma subred (land attack):
     * {@code true} si {@code srcIp == dstIp} y {@code srcPort == dstPort}.
     */
    private final boolean land;

    /** Número de fragmentos erróneos (wrong fragments) en la conexión. */
    private final int wrongFragment;

    /** Número de paquetes "urgentes" (urgent flag activo) en la conexión. */
    private final int urgent;

    // ==========================================================
    // 2. ATRIBUTOS DE CONTENIDO (13)
    // ==========================================================

    /** Número de "hot indicators" (accesos a directorios sensibles, creación/ejecución de programas, etc.). */
    private final int hot;

    /** Número de intentos fallidos de inicio de sesión. */
    private final int numFailedLogins;

    /** {@code true} si el inicio de sesión fue exitoso (loggedIn). */
    private final boolean loggedIn;

    /** Número de accesos a la "compromised condition" (indicadores de que el sistema ya fue comprometido). */
    private final int numCompromised;

    /** {@code true} si la conexión obtuvo acceso root (root shell). */
    private final boolean rootShell;

    /** {@code true} si se intentó (con éxito o no) un comando "su root". */
    private final boolean suAttempted;

    /** Número de operaciones tipo "root" realizadas dentro de la conexión. */
    private final int numRoot;

    /** Número de operaciones de creación de archivos (file creations). */
    private final int numFileCreations;

    /** Número de accesos a shells (shell prompts) invocados. */
    private final int numShells;

    /** Número de operaciones sobre archivos/comandos de control de acceso (ej. chmod). */
    private final int numAccessFiles;

    /** Número de salidas al comando "outbound" (conexiones ftp salientes, específico de ftp_data). */
    private final int numOutboundCmds;

    /** {@code true} si el inicio de sesión pertenece a la lista "hot" (hot login, ej. admin/root). */
    private final boolean isHotLogin;

    /** {@code true} si el inicio de sesión corresponde al usuario "guest". */
    private final boolean isGuestLogin;

    // ==========================================================
    // 3. ATRIBUTOS DE TRÁFICO — VENTANA DE TIEMPO (2 SEGUNDOS) (9)
    // ==========================================================

    /** Número de conexiones al mismo host destino en los últimos 2 segundos. */
    private final int count;

    /** Número de conexiones al mismo servicio en los últimos 2 segundos. */
    private final int srvCount;

    /** Porcentaje de conexiones (de {@code count}) con errores tipo SYN. */
    private final double serrorRate;

    /** Porcentaje de conexiones (de {@code srvCount}) con errores tipo SYN. */
    private final double srvSerrorRate;

    /** Porcentaje de conexiones (de {@code count}) con errores tipo REJ. */
    private final double rerrorRate;

    /** Porcentaje de conexiones (de {@code srvCount}) con errores tipo REJ. */
    private final double srvRerrorRate;

    /** Porcentaje de conexiones (de {@code count}) al mismo servicio. */
    private final double sameSrvRate;

    /** Porcentaje de conexiones (de {@code count}) a servicios distintos. */
    private final double diffSrvRate;

    /** Porcentaje de conexiones (de {@code srvCount}) a hosts distintos. */
    private final double srvDiffHostRate;

    // ==========================================================
    // 4. ATRIBUTOS DE TRÁFICO — VENTANA DE HOST (100 CONEXIONES) (10)
    // ==========================================================

    /** Número de conexiones dirigidas al mismo host destino (últimas 100 conexiones). */
    private final int dstHostCount;

    /** Número de conexiones dirigidas al mismo servicio en el mismo host destino (últimas 100). */
    private final int dstHostSrvCount;

    /** Porcentaje de conexiones (de {@code dstHostCount}) al mismo servicio. */
    private final double dstHostSameSrvRate;

    /** Porcentaje de conexiones (de {@code dstHostCount}) a servicios distintos. */
    private final double dstHostDiffSrvRate;

    /** Porcentaje de conexiones (de {@code dstHostSrvCount}) que usan el mismo puerto de origen. */
    private final double dstHostSameSrcPortRate;

    /** Porcentaje de conexiones (de {@code dstHostSrvCount}) dirigidas a hosts distintos. */
    private final double dstHostSrvDiffHostRate;

    /** Porcentaje de conexiones (de {@code dstHostCount}) con errores tipo SYN. */
    private final double dstHostSerrorRate;

    /** Porcentaje de conexiones (de {@code dstHostSrvCount}) con errores tipo SYN. */
    private final double dstHostSrvSerrorRate;

    /** Porcentaje de conexiones (de {@code dstHostCount}) con errores tipo REJ. */
    private final double dstHostRerrorRate;

    /** Porcentaje de conexiones (de {@code dstHostSrvCount}) con errores tipo REJ. */
    private final double dstHostSrvRerrorRate;

    // ==========================================================
    // 5. ETIQUETA
    // ==========================================================

    /**
     * Etiqueta de clase original del dataset: {@code "normal"} o el nombre
     * específico del ataque (ej. "neptune", "smurf", "satan", "guess_passwd").
     * Ver {@link #getAttackCategory()} para la categoría agregada
     * (DoS, Probe, R2L, U2R o Normal).
     */
    private final String label;

    /**
     * Nivel de dificultad de clasificación asignado por los creadores del
     * dataset NSL-KDD (0 a 21; a mayor valor, más difícil de clasificar
     * correctamente por los métodos evaluados originalmente). Puede usarse
     * para ponderar el entrenamiento o para excluir instancias triviales.
     */
    private final int difficultyLevel;

    // ==========================================================
    // 6. CAMPOS AUXILIARES (no forman parte del dataset original)
    // ==========================================================

    /**
     * Dirección IP de origen sintetizada. NSL-KDD no incluye IPs reales;
     * este campo es opcional y debe ser asignado externamente (por
     * ejemplo, por {@code DatasetLoader} o por un módulo de simulación de
     * topología) si se necesita construir un grafo de red. Puede ser
     * {@code null} si no se ha asignado.
     */
    private final String srcIp;

    /**
     * Dirección IP de destino sintetizada. Ver {@link #srcIp}.
     * Puede ser {@code null} si no se ha asignado.
     */
    private final String dstIp;

    /**
     * Construye una conexión inmutable con todos sus atributos.
     * Se recomienda usar {@code DatasetLoader} para construir instancias
     * a partir de una línea del archivo NSL-KDD en vez de llamar a este
     * constructor directamente.
     */
    public Connection(double duration, String protocolType, String service, String flag,
                       long srcBytes, long dstBytes, boolean land, int wrongFragment, int urgent,
                       int hot, int numFailedLogins, boolean loggedIn, int numCompromised,
                       boolean rootShell, boolean suAttempted, int numRoot, int numFileCreations,
                       int numShells, int numAccessFiles, int numOutboundCmds, boolean isHotLogin,
                       boolean isGuestLogin, int count, int srvCount, double serrorRate,
                       double srvSerrorRate, double rerrorRate, double srvRerrorRate,
                       double sameSrvRate, double diffSrvRate, double srvDiffHostRate,
                       int dstHostCount, int dstHostSrvCount, double dstHostSameSrvRate,
                       double dstHostDiffSrvRate, double dstHostSameSrcPortRate,
                       double dstHostSrvDiffHostRate, double dstHostSerrorRate,
                       double dstHostSrvSerrorRate, double dstHostRerrorRate,
                       double dstHostSrvRerrorRate, String label, int difficultyLevel,
                       String srcIp, String dstIp) {
        this.duration = duration;
        this.protocolType = protocolType;
        this.service = service;
        this.flag = flag;
        this.srcBytes = srcBytes;
        this.dstBytes = dstBytes;
        this.land = land;
        this.wrongFragment = wrongFragment;
        this.urgent = urgent;
        this.hot = hot;
        this.numFailedLogins = numFailedLogins;
        this.loggedIn = loggedIn;
        this.numCompromised = numCompromised;
        this.rootShell = rootShell;
        this.suAttempted = suAttempted;
        this.numRoot = numRoot;
        this.numFileCreations = numFileCreations;
        this.numShells = numShells;
        this.numAccessFiles = numAccessFiles;
        this.numOutboundCmds = numOutboundCmds;
        this.isHotLogin = isHotLogin;
        this.isGuestLogin = isGuestLogin;
        this.count = count;
        this.srvCount = srvCount;
        this.serrorRate = serrorRate;
        this.srvSerrorRate = srvSerrorRate;
        this.rerrorRate = rerrorRate;
        this.srvRerrorRate = srvRerrorRate;
        this.sameSrvRate = sameSrvRate;
        this.diffSrvRate = diffSrvRate;
        this.srvDiffHostRate = srvDiffHostRate;
        this.dstHostCount = dstHostCount;
        this.dstHostSrvCount = dstHostSrvCount;
        this.dstHostSameSrvRate = dstHostSameSrvRate;
        this.dstHostDiffSrvRate = dstHostDiffSrvRate;
        this.dstHostSameSrcPortRate = dstHostSameSrcPortRate;
        this.dstHostSrvDiffHostRate = dstHostSrvDiffHostRate;
        this.dstHostSerrorRate = dstHostSerrorRate;
        this.dstHostSrvSerrorRate = dstHostSrvSerrorRate;
        this.dstHostRerrorRate = dstHostRerrorRate;
        this.dstHostSrvRerrorRate = dstHostSrvRerrorRate;
        this.label = label;
        this.difficultyLevel = difficultyLevel;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
    }

    // ==========================================================
    // GETTERS
    // ==========================================================

    public double getDuration() { return duration; }
    public String getProtocolType() { return protocolType; }
    public String getService() { return service; }
    public String getFlag() { return flag; }
    public long getSrcBytes() { return srcBytes; }
    public long getDstBytes() { return dstBytes; }
    public boolean isLand() { return land; }
    public int getWrongFragment() { return wrongFragment; }
    public int getUrgent() { return urgent; }

    public int getHot() { return hot; }
    public int getNumFailedLogins() { return numFailedLogins; }
    public boolean isLoggedIn() { return loggedIn; }
    public int getNumCompromised() { return numCompromised; }
    public boolean isRootShell() { return rootShell; }
    public boolean isSuAttempted() { return suAttempted; }
    public int getNumRoot() { return numRoot; }
    public int getNumFileCreations() { return numFileCreations; }
    public int getNumShells() { return numShells; }
    public int getNumAccessFiles() { return numAccessFiles; }
    public int getNumOutboundCmds() { return numOutboundCmds; }
    public boolean isHotLogin() { return isHotLogin; }
    public boolean isGuestLogin() { return isGuestLogin; }

    public int getCount() { return count; }
    public int getSrvCount() { return srvCount; }
    public double getSerrorRate() { return serrorRate; }
    public double getSrvSerrorRate() { return srvSerrorRate; }
    public double getRerrorRate() { return rerrorRate; }
    public double getSrvRerrorRate() { return srvRerrorRate; }
    public double getSameSrvRate() { return sameSrvRate; }
    public double getDiffSrvRate() { return diffSrvRate; }
    public double getSrvDiffHostRate() { return srvDiffHostRate; }

    public int getDstHostCount() { return dstHostCount; }
    public int getDstHostSrvCount() { return dstHostSrvCount; }
    public double getDstHostSameSrvRate() { return dstHostSameSrvRate; }
    public double getDstHostDiffSrvRate() { return dstHostDiffSrvRate; }
    public double getDstHostSameSrcPortRate() { return dstHostSameSrcPortRate; }
    public double getDstHostSrvDiffHostRate() { return dstHostSrvDiffHostRate; }
    public double getDstHostSerrorRate() { return dstHostSerrorRate; }
    public double getDstHostSrvSerrorRate() { return dstHostSrvSerrorRate; }
    public double getDstHostRerrorRate() { return dstHostRerrorRate; }
    public double getDstHostSrvRerrorRate() { return dstHostSrvRerrorRate; }

    public String getLabel() { return label; }
    public int getDifficultyLevel() { return difficultyLevel; }

    public String getSrcIp() { return srcIp; }
    public String getDstIp() { return dstIp; }

    /**
     * Indica si esta conexión corresponde a tráfico normal.
     *
     * @return {@code true} si {@link #label} es exactamente {@code "normal"}.
     */
    public boolean isNormal() {
        return "normal".equalsIgnoreCase(label);
    }

    /**
     * Clasifica la etiqueta específica del ataque (ej. "neptune", "smurf")
     * dentro de una de las 4 categorías agregadas usadas en la literatura
     * de KDD Cup / NSL-KDD:
     * <ul>
     *   <li><b>DoS</b> (Denial of Service): ej. neptune, smurf, back, teardrop, pod, land.</li>
     *   <li><b>Probe</b> (sondeo/reconocimiento): ej. satan, ipsweep, nmap, portsweep.</li>
     *   <li><b>R2L</b> (Remote to Local, acceso remoto no autorizado): ej. guess_passwd, ftp_write, warezclient.</li>
     *   <li><b>U2R</b> (User to Root, escalamiento de privilegios): ej. buffer_overflow, rootkit, perl.</li>
     * </ul>
     * Esta clasificación agregada es útil para el módulo estadístico y
     * para evaluar el desempeño del clasificador por categoría en vez de
     * por ataque específico (hay más de 20 tipos de ataque distintos, pero
     * solo 4 categorías).
     *
     * @return la categoría de ataque, o {@code "normal"} si no es un ataque,
     *         o {@code "unknown"} si la etiqueta no está en el mapeo conocido.
     */
    public String getAttackCategory() {
        if (isNormal()) {
            return "normal";
        }
        String l = label.toLowerCase();
        switch (l) {
            case "neptune": case "smurf": case "back": case "teardrop":
            case "pod": case "land": case "apache2": case "udpstorm":
            case "processtable": case "mailbomb":
                return "dos";
            case "satan": case "ipsweep": case "nmap": case "portsweep":
            case "mscan": case "saint":
                return "probe";
            case "guess_passwd": case "ftp_write": case "imap": case "phf":
            case "multihop": case "warezmaster": case "warezclient":
            case "spy": case "xlock": case "xsnoop": case "snmpguess":
            case "snmpgetattack": case "httptunnel": case "sendmail":
            case "named": case "worm":
                return "r2l";
            case "buffer_overflow": case "loadmodule": case "rootkit":
            case "perl": case "sqlattack": case "xterm": case "ps":
                return "u2r";
            default:
                return "unknown";
        }
    }

    @Override
    public String toString() {
        return "Connection{" +
                "protocol=" + protocolType +
                ", service=" + service +
                ", flag=" + flag +
                ", duration=" + duration +
                ", srcBytes=" + srcBytes +
                ", dstBytes=" + dstBytes +
                ", label=" + label +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Connection)) return false;
        Connection that = (Connection) o;
        return Double.compare(that.duration, duration) == 0 &&
                srcBytes == that.srcBytes &&
                dstBytes == that.dstBytes &&
                Objects.equals(protocolType, that.protocolType) &&
                Objects.equals(service, that.service) &&
                Objects.equals(flag, that.flag) &&
                Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration, protocolType, service, flag, srcBytes, dstBytes, label);
    }
}