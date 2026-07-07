package fsm;

import data.DatasetLoader;
import model.Connection;
import rules.EvaluationResult;
import rules.RuleEngine;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administra una colección de {@link SessionAutomaton}, una por cada
 * sesión (identificada por {@code sessionId}), y las alimenta con los
 * resultados del {@link RuleEngine} a medida que llegan nuevas conexiones.
 * <p>
 * <b>Nota importante sobre el origen de {@code sessionId} en este proyecto:</b>
 * NSL-KDD no incluye direcciones IP reales (fueron eliminadas del dataset
 * original por privacidad). Por lo tanto, no existe una forma nativa de
 * saber qué conexiones pertenecen al mismo host real. Para poder demostrar
 * el funcionamiento de la máquina de estados sobre una secuencia de
 * eventos, el método {@code main} de esta clase **simula** sesiones
 * agrupando conexiones consecutivas del archivo en "hosts virtuales" de
 * forma round-robin. Esto es una simplificación deliberada y documentada:
 * en una implementación real (o cuando construyamos el módulo de grafos),
 * {@code sessionId} debería ser la IP de origen real u otro identificador
 * de host genuino.
 */
public final class SessionManager {

    /** Número de "hosts virtuales" usados para simular sesiones en la demo. */
    private static final int SIMULATED_HOST_COUNT = 12000;

    private final RuleEngine ruleEngine;
    private final Map<String, SessionAutomaton> sessions = new HashMap<>();

    public SessionManager(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * Procesa una nueva conexión perteneciente a la sesión indicada:
     * evalúa la conexión contra el motor de reglas y avanza la máquina de
     * estados correspondiente a esa sesión (creándola si es la primera vez
     * que se ve ese {@code sessionId}).
     *
     * @return el nuevo estado de la sesión tras procesar esta conexión
     */
    public SessionState processConnection(String sessionId, Connection connection) {
        SessionAutomaton automaton = sessions.computeIfAbsent(sessionId, SessionAutomaton::new);
        EvaluationResult result = ruleEngine.evaluate(connection);
        return automaton.process(result);
    }

    public Map<String, SessionAutomaton> getSessions() {
        return sessions;
    }

    /**
     * Punto de entrada de prueba: carga el dataset, simula la asignación
     * de conexiones a hosts virtuales, procesa toda la secuencia a través
     * de sus respectivas máquinas de estado, e imprime un resumen de en
     * qué estado terminó cada sesión.
     */
    public static void main(String[] args) {
        String path = args.length > 0 ? args[0] : "data/KDDTrain+.txt";
        RuleEngine ruleEngine = RuleEngine.createDefaultRuleEngine();
        SessionManager manager = new SessionManager(ruleEngine);

        try {
            List<Connection> connections = DatasetLoader.load(path);

            for (int i = 0; i < connections.size(); i++) {
                // Simulación: agrupamos conexiones en hosts virtuales por
                // round-robin, ya que NSL-KDD no trae identidad de host real.
                String sessionId = "host_sim_" + (i % SIMULATED_HOST_COUNT);
                manager.processConnection(sessionId, connections.get(i));
            }

            printSummary(manager);
        } catch (IOException e) {
            System.err.println("Error al leer el archivo '" + path + "': " + e.getMessage());
        }
    }

    private static void printSummary(SessionManager manager) {
        int normal = 0, suspicious = 0, blocked = 0;

        for (SessionAutomaton automaton : manager.getSessions().values()) {
            switch (automaton.getState()) {
                case NORMAL -> normal++;
                case SUSPICIOUS -> suspicious++;
                case BLOCKED -> blocked++;
            }
        }

        int total = manager.getSessions().size();
        System.out.println("=== Resumen de sesiones simuladas (" + total + " hosts virtuales) ===");
        System.out.println("Terminaron en NORMAL:     " + normal);
        System.out.println("Terminaron en SUSPICIOUS: " + suspicious);
        System.out.println("Terminaron en BLOCKED:    " + blocked);

        System.out.println("\n--- Ejemplos de sesiones bloqueadas ---");
        int shown = 0;
        for (SessionAutomaton automaton : manager.getSessions().values()) {
            if (automaton.getState() == SessionState.BLOCKED && shown < 5) {
                System.out.println("  " + automaton);
                shown++;
            }
        }
    }
}