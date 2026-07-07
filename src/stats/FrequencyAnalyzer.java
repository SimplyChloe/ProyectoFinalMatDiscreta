package stats;

import model.Connection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilidades de conteo y frecuencia sobre listas de {@link Connection},
 * usadas como base tanto para análisis descriptivo (¿qué tan común es
 * cada protocolo, servicio, etc.?) como para alimentar a
 * {@link BayesClassifier}.
 * <p>
 * También incluye una utilidad de <b>conteo combinatorio</b>: dado el
 * número de valores distintos que puede tomar cada atributo categórico,
 * calcula cuántas combinaciones distintas son posibles en total (principio
 * multiplicativo del conteo). Esto es útil para dimensionar qué tan
 * disperso puede llegar a estar el espacio de características, y por qué
 * el suavizado de Laplace es necesario en {@link BayesClassifier} (con
 * tantas combinaciones posibles, es normal que el entrenamiento nunca haya
 * visto algunas de ellas).
 */
public final class FrequencyAnalyzer {

    private FrequencyAnalyzer() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Cuenta cuántas conexiones caen en cada valor distinto según la
     * función {@code extractor} (ej. {@code Connection::getProtocolType}).
     */
    public static <T> Map<T, Integer> countBy(List<Connection> connections, Function<Connection, T> extractor) {
        Map<T, Integer> counts = new HashMap<>();
        for (Connection c : connections) {
            counts.merge(extractor.apply(c), 1, Integer::sum);
        }
        return counts;
    }

    /**
     * Igual que {@link #countBy}, pero devuelve la frecuencia relativa
     * (proporción, entre 0 y 1) de cada valor en vez del conteo absoluto.
     * Esta es, en esencia, una estimación empírica de P(valor) a partir de
     * los datos observados.
     */
    public static <T> Map<T, Double> relativeFrequency(List<Connection> connections, Function<Connection, T> extractor) {
        Map<T, Integer> counts = countBy(connections, extractor);
        int total = connections.size();
        Map<T, Double> freq = new HashMap<>();
        for (Map.Entry<T, Integer> entry : counts.entrySet()) {
            freq.put(entry.getKey(), entry.getValue() / (double) total);
        }
        return freq;
    }

    /**
     * Aplica el principio multiplicativo del conteo: dado el número de
     * valores distintos que puede tomar cada uno de varios atributos
     * independientes, calcula cuántas combinaciones distintas son posibles
     * en total ({@code n1 * n2 * n3 * ...}).
     * <p>
     * Ejemplo: si hay 3 protocolos, 70 servicios y 11 valores de flag
     * posibles, existen {@code 3 * 70 * 11 = 2310} combinaciones posibles
     * de (protocolo, servicio, flag) — aunque el dataset real solo
     * contenga una fracción de ellas.
     */
    public static long combinatorialSpace(int... distinctValueCounts) {
        long product = 1;
        for (int n : distinctValueCounts) {
            product *= n;
        }
        return product;
    }
}