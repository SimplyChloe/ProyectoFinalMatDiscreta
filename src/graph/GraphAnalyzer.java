package graph;

import data.DatasetLoader;
import model.Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analiza un {@link NetworkGraph} para extraer información estructural
 * relevante para detección de intrusiones:
 * <ul>
 *   <li><b>Grado de un nodo</b>: para un host, el grado es el número de
 *       servicios distintos a los que se conectó. Un grado anormalmente
 *       alto es la firma clásica de un escaneo de puertos (ej. nmap,
 *       satan) — el mismo fenómeno que la regla {@code R02_PORT_SCAN} del
 *       {@link rules.RuleEngine} intenta capturar mediante {@code diffSrvRate},
 *       pero aquí lo vemos desde una perspectiva estructural/topológica en
 *       vez de puramente estadística.</li>
 *   <li><b>Componentes conexas</b>: calculadas con una búsqueda en
 *       profundidad (DFS) implementada de forma <b>recursiva</b>, para
 *       identificar grupos de hosts y servicios mutuamente alcanzables.</li>
 * </ul>
 */
public final class GraphAnalyzer {

    private final NetworkGraph graph;

    public GraphAnalyzer(NetworkGraph graph) {
        this.graph = graph;
    }

    /** Grado de un nodo: cantidad de vecinos distintos. */
    public int degree(String nodeId) {
        return graph.getNeighbors(nodeId).size();
    }

    /**
     * Devuelve los nodos cuyo identificador comienza con {@code prefix}
     * (típicamente {@link NetworkGraph#HOST_PREFIX}) y cuyo grado supera
     * {@code threshold}, ordenados de mayor a menor grado. Estos son los
     * candidatos más fuertes a estar realizando un escaneo de puertos:
     * conectarse a muchísimos servicios distintos es un patrón atípico
     * para un host con comportamiento normal.
     */
    public List<String> findHighDegreeNodes(String prefix, int threshold) {
        List<String> result = new ArrayList<>();
        for (String node : graph.getNodes()) {
            if (node.startsWith(prefix) && degree(node) > threshold) {
                result.add(node);
            }
        }
        result.sort((a, b) -> degree(b) - degree(a));
        return result;
    }

    /**
     * Explora recursivamente (DFS) todos los nodos alcanzables desde
     * {@code start} y los agrega a {@code visited}. Esta es la función
     * recursiva propiamente dicha: el caso base es un nodo ya visitado
     * (no se vuelve a explorar), y el paso recursivo llama a
     * {@code dfs} sobre cada vecino no visitado.
     */
    private void dfs(String node, Set<String> visited) {
        if (visited.contains(node)) {
            return; // caso base: ya visitado, no hay más que hacer
        }
        visited.add(node);
        for (String neighbor : graph.getNeighbors(node)) {
            dfs(neighbor, visited); // llamada recursiva sobre cada vecino
        }
    }

    /**
     * Devuelve el conjunto de todos los nodos que pertenecen a la misma
     * componente conexa que {@code start} (incluyéndolo a él).
     */
    public Set<String> exploreComponent(String start) {
        Set<String> visited = new HashSet<>();
        dfs(start, visited);
        return visited;
    }

    /**
     * Calcula todas las componentes conexas del grafo. En un grafo
     * Host–Servicio como el nuestro, es esperable que la gran mayoría de
     * los nodos terminen en una única componente gigante (porque muchos
     * hosts comparten servicios muy comunes como {@code http} o
     * {@code private}), pero servicios muy raros usados por un único host
     * aislado pueden formar componentes pequeñas separadas.
     */
    public List<Set<String>> findConnectedComponents() {
        Set<String> globalVisited = new HashSet<>();
        List<Set<String>> components = new ArrayList<>();

        for (String node : graph.getNodes()) {
            if (!globalVisited.contains(node)) {
                Set<String> component = exploreComponent(node);
                globalVisited.addAll(component);
                components.add(component);
            }
        }

        return components;
    }

    /**
     * Grado promedio de todos los nodos que empiezan con {@code prefix}.
     */
    public double averageDegree(String prefix) {
        int total = 0;
        int count = 0;
        for (String node : graph.getNodes()) {
            if (node.startsWith(prefix)) {
                total += degree(node);
                count++;
            }
        }
        return count == 0 ? 0.0 : (double) total / count;
    }

    /**
     * Construye el grafo Host–Servicio a partir de una lista de
     * conexiones, usando la misma estrategia de "hosts virtuales" por
     * round-robin que {@link fsm.SessionManager}, documentada allí y
     * reutilizada aquí por consistencia narrativa del proyecto (NSL-KDD
     * no trae identidad de host real).
     */
    public static NetworkGraph buildGraph(List<Connection> connections, int simulatedHostCount) {
        NetworkGraph graph = new NetworkGraph();
        for (int i = 0; i < connections.size(); i++) {
            Connection c = connections.get(i);
            String hostId = "sim_" + (i % simulatedHostCount);
            graph.addConnection(hostId, c.getService(), !c.isNormal());
        }
        return graph;
    }

    /**
     * Punto de entrada de prueba: carga el dataset, construye el grafo, e
     * imprime estadísticas estructurales junto con los hosts virtuales de
     * mayor grado (candidatos a escaneo de puertos).
     */
    public static void main(String[] args) {
        String path = args.length > 0 ? args[0] : "data/KDDTrain+.txt";
        int simulatedHostCount = 12000; // mismo valor usado en fsm.SessionManager

        try {
            List<Connection> connections = DatasetLoader.load(path);
            NetworkGraph graph = buildGraph(connections, simulatedHostCount);
            GraphAnalyzer analyzer = new GraphAnalyzer(graph);

            System.out.println("=== Estadísticas del grafo Host-Servicio ===");
            System.out.println("Total de nodos: " + graph.getNodeCount());
            System.out.printf("Grado promedio (hosts): %.2f%n",
                    analyzer.averageDegree(NetworkGraph.HOST_PREFIX));
            System.out.printf("Grado promedio (servicios): %.2f%n",
                    analyzer.averageDegree(NetworkGraph.SERVICE_PREFIX));

            List<Set<String>> components = analyzer.findConnectedComponents();
            System.out.println("Número de componentes conexas: " + components.size());
            components.sort((a, b) -> b.size() - a.size());
            System.out.println("Tamaño de la componente más grande: " +
                    (components.isEmpty() ? 0 : components.get(0).size()));

            System.out.println("\n--- Top 10 hosts con mayor grado (candidatos a escaneo de puertos) ---");
            List<String> highDegreeHosts = analyzer.findHighDegreeNodes(NetworkGraph.HOST_PREFIX, 0);
            for (int i = 0; i < Math.min(10, highDegreeHosts.size()); i++) {
                String host = highDegreeHosts.get(i);
                System.out.println("  " + host + " -> grado=" + analyzer.degree(host));
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo '" + path + "': " + e.getMessage());
        }
    }
}