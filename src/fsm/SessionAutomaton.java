package fsm;

import rules.EvaluationResult;
import rules.Severity;

/**
 * Máquina de estados finita (autómata) que modela el comportamiento de
 * <b>una única sesión</b> (típicamente, un host de origen) a lo largo del
 * tiempo, a medida que se van evaluando sus conexiones sucesivas contra
 * el {@link rules.RuleEngine}.
 * <p>
 * Formalmente, este autómata se define como una 5-tupla (Q, Σ, δ, q₀, F):
 * <ul>
 *   <li><b>Q</b> (estados) = {@link SessionState#NORMAL}, {@link SessionState#SUSPICIOUS}, {@link SessionState#BLOCKED}</li>
 *   <li><b>Σ</b> (alfabeto de entrada) = el conjunto de posibles {@link EvaluationResult}
 *       producidos por el RuleEngine (en la práctica, se simplifica a si hubo
 *       alerta o no, y de qué severidad)</li>
 *   <li><b>δ</b> (función de transición) = implementada en {@link #process(EvaluationResult)}</li>
 *   <li><b>q₀</b> (estado inicial) = {@link SessionState#NORMAL}</li>
 *   <li><b>F</b> (estados de aceptación/terminales) = {@link SessionState#BLOCKED}</li>
 * </ul>
 * <p>
 * Este autómata añade un poco de "memoria" (los contadores {@code strikeCount}
 * y {@code goodStreak}) que técnicamente lo convierten en algo más expresivo
 * que un autómata finito puro (que no tiene memoria más allá del estado
 * actual). Se documenta esta simplificación conscientemente: para efectos
 * del proyecto, el comportamiento observable (los 3 estados y sus
 * transiciones) es lo relevante, y los contadores se tratan como parte
 * "oculta" de la configuración del estado {@code SUSPICIOUS}.
 */
public final class SessionAutomaton {

    /** Número de conexiones sospechosas consecutivas antes de bloquear. */
    private static final int STRIKES_TO_BLOCK = 3;

    /** Número de conexiones limpias consecutivas para recuperar el estado NORMAL. */
    private static final int GOOD_STREAK_TO_RECOVER = 5;

    private final String sessionId;
    private SessionState state;
    private int strikeCount;
    private int goodStreak;

    public SessionAutomaton(String sessionId) {
        this.sessionId = sessionId;
        this.state = SessionState.NORMAL;
        this.strikeCount = 0;
        this.goodStreak = 0;
    }

    /**
     * Función de transición δ: procesa el resultado de evaluar una nueva
     * conexión de esta sesión contra el motor de reglas, y actualiza el
     * estado interno del autómata según corresponda.
     * <p>
     * Si el autómata ya está en {@link SessionState#BLOCKED}, no se
     * procesa nada más (estado terminal/absorbente).
     *
     * @param result el veredicto del RuleEngine para la conexión más reciente de esta sesión
     * @return el nuevo estado del autómata tras procesar la conexión
     */
    public SessionState process(EvaluationResult result) {
        if (state == SessionState.BLOCKED) {
            return state; // Estado terminal: ya no hay transiciones de salida.
        }

        // Transición inmediata e incondicional ante severidad crítica,
        // sin importar el estado actual (NORMAL o SUSPICIOUS).
        if (result.getMaxSeverity() == Severity.CRITICAL) {
            state = SessionState.BLOCKED;
            return state;
        }

        if (result.isSuspicious()) {
            goodStreak = 0;
            strikeCount++;
            state = (strikeCount >= STRIKES_TO_BLOCK) ? SessionState.BLOCKED : SessionState.SUSPICIOUS;
        } else {
            // Conexión limpia: solo importa para la recuperación si ya estábamos en SUSPICIOUS.
            if (state == SessionState.SUSPICIOUS) {
                goodStreak++;
                if (goodStreak >= GOOD_STREAK_TO_RECOVER) {
                    strikeCount = 0;
                    goodStreak = 0;
                    state = SessionState.NORMAL;
                }
            }
        }

        return state;
    }

    public String getSessionId() {
        return sessionId;
    }

    public SessionState getState() {
        return state;
    }

    public int getStrikeCount() {
        return strikeCount;
    }

    @Override
    public String toString() {
        return "Sesión[" + sessionId + "] estado=" + state + " strikes=" + strikeCount;
    }
}