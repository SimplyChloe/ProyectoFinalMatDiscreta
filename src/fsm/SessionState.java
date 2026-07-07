package fsm;

/**
 * Estados posibles de una sesión (host) dentro de la máquina de estados
 * finita de {@link SessionAutomaton}.
 * <p>
 * Transiciones válidas (ver diagrama completo en {@link SessionAutomaton}):
 * <pre>
 *   NORMAL      --[conexión sospechosa]--&gt; SUSPICIOUS
 *   SUSPICIOUS  --[3 strikes acumulados]--&gt; BLOCKED
 *   SUSPICIOUS  --[5 conexiones limpias seguidas]--&gt; NORMAL
 *   cualquiera  --[severidad CRITICAL]--&gt; BLOCKED
 *   BLOCKED     --[estado terminal, sin salida]--
 * </pre>
 */
public enum SessionState {
    /** Sin señales de alerta recientes; comportamiento confiable. */
    NORMAL,

    /** Se han detectado señales de alerta, pero no suficientes para bloquear aún. */
    SUSPICIOUS,

    /** Estado terminal: la sesión se considera hostil y se corta el acceso. */
    BLOCKED
}