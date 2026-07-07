package rules;

import model.Connection;

import java.util.Collections;
import java.util.List;

/**
 * Resultado de evaluar una {@link Connection} contra un conjunto de reglas
 * en {@link RuleEngine}. Contiene todas las reglas que se activaron (puede
 * ser ninguna, una, o varias simultáneamente) y la severidad máxima entre
 * ellas.
 */
public final class EvaluationResult {

    private final Connection connection;
    private final List<Rule> triggeredRules;

    public EvaluationResult(Connection connection, List<Rule> triggeredRules) {
        this.connection = connection;
        this.triggeredRules = Collections.unmodifiableList(triggeredRules);
    }

    /** @return {@code true} si al menos una regla se activó para esta conexión. */
    public boolean isSuspicious() {
        return !triggeredRules.isEmpty();
    }

    /**
     * @return la severidad más alta entre todas las reglas activadas, o
     *         {@code null} si ninguna regla se activó (conexión no sospechosa).
     */
    public Severity getMaxSeverity() {
        return triggeredRules.stream()
                .map(Rule::getSeverity)
                .max(Severity::compareTo)
                .orElse(null);
    }

    public Connection getConnection() {
        return connection;
    }

    public List<Rule> getTriggeredRules() {
        return triggeredRules;
    }

    @Override
    public String toString() {
        if (!isSuspicious()) {
            return "OK - ninguna regla activada";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ALERTA [").append(getMaxSeverity()).append("] - reglas activadas: ");
        for (Rule r : triggeredRules) {
            sb.append(r.getId()).append(" ");
        }
        return sb.toString();
    }
}