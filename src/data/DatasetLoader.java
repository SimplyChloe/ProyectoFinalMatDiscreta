package data;

import model.Connection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga el dataset NSL-KDD desde un archivo de texto plano (formato CSV
 * sin encabezados) y lo convierte en una lista de objetos {@link Connection}.
 * <p>
 * Formato esperado de cada línea (43 campos separados por comas, en este
 * orden exacto):
 * <pre>
 * duration, protocol_type, service, flag, src_bytes, dst_bytes, land,
 * wrong_fragment, urgent, hot, num_failed_logins, logged_in,
 * num_compromised, root_shell, su_attempted, num_root, num_file_creations,
 * num_shells, num_access_files, num_outbound_cmds, is_hot_login,
 * is_guest_login, count, srv_count, serror_rate, srv_serror_rate,
 * rerror_rate, srv_rerror_rate, same_srv_rate, diff_srv_rate,
 * srv_diff_host_rate, dst_host_count, dst_host_srv_count,
 * dst_host_same_srv_rate, dst_host_diff_srv_rate,
 * dst_host_same_src_port_rate, dst_host_srv_diff_host_rate,
 * dst_host_serror_rate, dst_host_srv_serror_rate, dst_host_rerror_rate,
 * dst_host_srv_rerror_rate, label, difficulty_level
 * </pre>
 * <p>
 * NSL-KDD no incluye IPs reales; este loader asigna {@code null} a
 * {@code srcIp}/{@code dstIp}. Su asignación (real o simulada) se hará en
 * un módulo posterior, cuando construyamos el grafo de red.
 */
public final class DatasetLoader {

    /** Número de campos esperados por línea (41 atributos + label + dificultad). */
    private static final int EXPECTED_FIELDS = 43;

    private DatasetLoader() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Lee el archivo indicado y devuelve la lista de conexiones parseadas.
     * Las líneas vacías se ignoran. Si una línea no tiene el número
     * esperado de campos, se lanza una excepción indicando el número de
     * línea problemático (para facilitar depuración de archivos corruptos).
     *
     * @param filePath ruta al archivo (ej. "data/KDDTrain+.txt")
     * @return lista de conexiones en el mismo orden del archivo
     * @throws IOException si el archivo no existe o no se puede leer
     */
    public static List<Connection> load(String filePath) throws IOException {
        List<Connection> connections = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",");
                if (fields.length < EXPECTED_FIELDS) {
                    throw new IOException("Línea " + lineNumber + " tiene " + fields.length +
                            " campos, se esperaban " + EXPECTED_FIELDS + ". Contenido: " + line);
                }

                connections.add(parseLine(fields));
            }
        }

        return connections;
    }

    /**
     * Convierte un arreglo de campos de texto (ya separados por coma) en
     * un objeto {@link Connection}. El orden de {@code fields} debe
     * coincidir con el documentado en la clase.
     */
    private static Connection parseLine(String[] f) {
        int i = 0;

        double duration = Double.parseDouble(f[i++]);
        String protocolType = f[i++];
        String service = f[i++];
        String flag = f[i++];
        long srcBytes = Long.parseLong(f[i++]);
        long dstBytes = Long.parseLong(f[i++]);
        boolean land = parseBoolean(f[i++]);
        int wrongFragment = Integer.parseInt(f[i++]);
        int urgent = Integer.parseInt(f[i++]);

        int hot = Integer.parseInt(f[i++]);
        int numFailedLogins = Integer.parseInt(f[i++]);
        boolean loggedIn = parseBoolean(f[i++]);
        int numCompromised = Integer.parseInt(f[i++]);
        boolean rootShell = parseBoolean(f[i++]);
        boolean suAttempted = parseBoolean(f[i++]);
        int numRoot = Integer.parseInt(f[i++]);
        int numFileCreations = Integer.parseInt(f[i++]);
        int numShells = Integer.parseInt(f[i++]);
        int numAccessFiles = Integer.parseInt(f[i++]);
        int numOutboundCmds = Integer.parseInt(f[i++]);
        boolean isHotLogin = parseBoolean(f[i++]);
        boolean isGuestLogin = parseBoolean(f[i++]);

        int count = Integer.parseInt(f[i++]);
        int srvCount = Integer.parseInt(f[i++]);
        double serrorRate = Double.parseDouble(f[i++]);
        double srvSerrorRate = Double.parseDouble(f[i++]);
        double rerrorRate = Double.parseDouble(f[i++]);
        double srvRerrorRate = Double.parseDouble(f[i++]);
        double sameSrvRate = Double.parseDouble(f[i++]);
        double diffSrvRate = Double.parseDouble(f[i++]);
        double srvDiffHostRate = Double.parseDouble(f[i++]);

        int dstHostCount = Integer.parseInt(f[i++]);
        int dstHostSrvCount = Integer.parseInt(f[i++]);
        double dstHostSameSrvRate = Double.parseDouble(f[i++]);
        double dstHostDiffSrvRate = Double.parseDouble(f[i++]);
        double dstHostSameSrcPortRate = Double.parseDouble(f[i++]);
        double dstHostSrvDiffHostRate = Double.parseDouble(f[i++]);
        double dstHostSerrorRate = Double.parseDouble(f[i++]);
        double dstHostSrvSerrorRate = Double.parseDouble(f[i++]);
        double dstHostRerrorRate = Double.parseDouble(f[i++]);
        double dstHostSrvRerrorRate = Double.parseDouble(f[i++]);

        String label = f[i++].trim();
        // El archivo original a veces trae un punto final tras la etiqueta
        // (ej. "normal.") heredado del formato KDD Cup 99; lo removemos.
        if (label.endsWith(".")) {
            label = label.substring(0, label.length() - 1);
        }
        int difficultyLevel = Integer.parseInt(f[i++].trim());

        return new Connection(duration, protocolType, service, flag, srcBytes, dstBytes, land,
                wrongFragment, urgent, hot, numFailedLogins, loggedIn, numCompromised, rootShell,
                suAttempted, numRoot, numFileCreations, numShells, numAccessFiles, numOutboundCmds,
                isHotLogin, isGuestLogin, count, srvCount, serrorRate, srvSerrorRate, rerrorRate,
                srvRerrorRate, sameSrvRate, diffSrvRate, srvDiffHostRate, dstHostCount,
                dstHostSrvCount, dstHostSameSrvRate, dstHostDiffSrvRate, dstHostSameSrcPortRate,
                dstHostSrvDiffHostRate, dstHostSerrorRate, dstHostSrvSerrorRate, dstHostRerrorRate,
                dstHostSrvRerrorRate, label, difficultyLevel, /* srcIp */ null, /* dstIp */ null);
    }

    /**
     * NSL-KDD representa los campos booleanos como "0"/"1". Este helper
     * interpreta también "true"/"false" por robustez ante variantes del
     * archivo.
     */
    private static boolean parseBoolean(String value) {
        return value.equals("1") || value.equalsIgnoreCase("true");
    }

    /**
     * Punto de entrada de prueba: carga el archivo indicado (o
     * {@code data/KDDTrain+.txt} por defecto) e imprime un resumen
     * estadístico básico. Sirve para verificar que el parser funciona
     * correctamente antes de construir los módulos de análisis.
     */
    public static void main(String[] args) {
        String path = args.length > 0 ? args[0] : "data/KDDTrain+.txt";

        try {
            List<Connection> connections = load(path);
            printSummary(connections);
        } catch (IOException e) {
            System.err.println("Error al leer el archivo '" + path + "': " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error al parsear un valor numérico: " + e.getMessage());
        }
    }

    /**
     * Imprime un resumen: total de conexiones, cuántas son normales vs.
     * ataque, y el desglose por categoría de ataque (dos, probe, r2l, u2r).
     */
    private static void printSummary(List<Connection> connections) {
        int total = connections.size();
        int normal = 0;
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (Connection c : connections) {
            if (c.isNormal()) {
                normal++;
            } else {
                String category = c.getAttackCategory();
                categoryCounts.merge(category, 1, Integer::sum);
            }
        }

        System.out.println("=== Resumen de carga NSL-KDD ===");
        System.out.println("Total de conexiones cargadas: " + total);
        System.out.println("Normales: " + normal + " (" + porcentaje(normal, total) + "%)");
        System.out.println("Ataques: " + (total - normal) + " (" + porcentaje(total - normal, total) + "%)");
        System.out.println("--- Desglose por categoría de ataque ---");
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() +
                    " (" + porcentaje(entry.getValue(), total) + "%)");
        }
    }

    private static String porcentaje(int parte, int total) {
        if (total == 0) return "0.00";
        return String.format("%.2f", (parte * 100.0) / total);
    }
}