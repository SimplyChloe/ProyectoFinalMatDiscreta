package graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Grafo no dirigido y bipartito que modela relaciones Host–Servicio.
 * <p>
 * Dado que NSL-KDD no incluye direcciones IP reales, este grafo no intenta
 * reconstruir "quién habló con quién" a nivel de host-a-host. En su lugar,
 * modela una relación que sí es genuina en los datos: <b>qué host virtual
 * se conectó a qué servicio real</b> ({@code http}, {@code ftp},
 * {@code telnet}, etc., tomados directamente del atributo {@code service}
 * de cada {@link model.Connection}).
 * <p>
 * Los identificadores de nodo llevan un prefijo para distinguir su tipo y
 * evitar colisiones entre un host y un servicio que coincidan por nombre:
 * <ul>
 *   <li>{@code "host:" + id}</li>
 *   <li>{@code "service:" + nombreServicio}</li>
 * </ul>
 * <p>
 * Es un grafo ponderado: cada arista guarda cuántas conexiones totales
 * representa y cuántas de ellas fueron ataques, lo que permite calcular
 * una "tasa de ataque" por arista además de la estructura del grafo en sí.
 */
public final class NetworkGraph {

    private final Map<String, Set<String>> adjacency = new HashMap<>();
    private final Map<String, Integer> edgeWeight = new HashMap<>();
    private final Map<String, Integer> edgeAttackCount = new HashMap<>();

    /** Prefijo usado para identificar nodos de tipo host. */
    public static final String HOST_PREFIX = "host:";

    /** Prefijo usado para identificar nodos de tipo servicio. */
    public static final String SERVICE_PREFIX = "service:";

    /**
     * Registra una conexión entre un host virtual y un servicio, agregando
     * los nodos si no existían y acumulando el peso de la arista.
     *
     * @param hostId    identificador del host (sin prefijo; se agrega internamente)
     * @param serviceId nombre del servicio (sin prefijo; se agrega internamente)
     * @param isAttack  si esta conexión específica fue etiquetada como ataque
     */
    public void addConnection(String hostId, String serviceId, boolean isAttack) {
        String host = HOST_PREFIX + hostId;
        String service = SERVICE_PREFIX + serviceId;

        adjacency.computeIfAbsent(host, k -> new HashSet<>()).add(service);
        adjacency.computeIfAbsent(service, k -> new HashSet<>()).add(host);

        String key = edgeKey(host, service);
        edgeWeight.merge(key, 1, Integer::sum);
        if (isAttack) {
            edgeAttackCount.merge(key, 1, Integer::sum);
        }
    }

    /**
     * Clave canónica para una arista no dirigida: se ordena
     * alfabéticamente para que {@code edgeKey(a,b) == edgeKey(b,a)}.
     */
    private String edgeKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "::" + b : b + "::" + a;
    }

    public Set<String> getNeighbors(String nodeId) {
        return adjacency.getOrDefault(nodeId, Collections.emptySet());
    }

    public Set<String> getNodes() {
        return adjacency.keySet();
    }

    public int getEdgeWeight(String a, String b) {
        return edgeWeight.getOrDefault(edgeKey(a, b), 0);
    }

    public int getEdgeAttackCount(String a, String b) {
        return edgeAttackCount.getOrDefault(edgeKey(a, b), 0);
    }

    /**
     * Tasa de ataque de una arista: proporción de conexiones que fueron
     * ataques sobre el total de conexiones representadas por esa arista.
     * Devuelve 0.0 si la arista no existe o no tiene peso registrado.
     */
    public double getEdgeAttackRate(String a, String b) {
        int weight = getEdgeWeight(a, b);
        if (weight == 0) return 0.0;
        return (double) getEdgeAttackCount(a, b) / weight;
    }

    public int getNodeCount() {
        return adjacency.size();
    }
}