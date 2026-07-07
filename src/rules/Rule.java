package rules;

import model.Connection;

import java.util.function.Predicate;

/**
 * Representa una regla de detección como una proposición lógica sobre una
 * {@link Connection}: una función que, dada una conexión, devuelve
 * verdadero o falso.
 * <p>
 * Conceptualmente, cada {@code Rule} es una proposición atómica o compuesta
 * en el sentido de la lógica proposicional. Por ejemplo, la regla
 * "posible SYN flood" se expresa como:
 * <pre>
 *   P: flag == "S0"
 *   Q: count > 50
 *   R: serrorRate > 0.5
 *   Regla = P ∧ Q ∧ R
 * </pre>
 * Esta clase permite construir dichas combinaciones mediante los métodos
 * {@link #and(Rule)}, {@link #or(Rule)} y {@link #negate()}, que se
 * corresponden directamente con los conectores lógicos ∧, ∨ y ¬.
 * <p>
 * Es inmutable: los combinadores devuelven una nueva instancia de
 * {@code Rule} en vez de modificar la actual.
 */
public final class Rule {

    private final String id;
    private final String description;
    private final Predicate<Connection> condition;
    private final Severity severity;

    /**
     * @param id          identificador corto y único de la regla (ej. "R01_SYN_FLOOD")
     * @param description explicación en lenguaje natural de qué detecta la regla
     * @param condition   la proposición lógica en sí, como función booleana sobre una Connection
     * @param severity    gravedad asignada si la regla se activa
     */
    public Rule(String id, String description, Predicate<Connection> condition, Severity severity) {
        this.id = id;
        this.description = description;
        this.condition = condition;
        this.severity = severity;
    }

    /**
     * Evalúa la proposición lógica de esta regla sobre una conexión dada.
     *
     * @return {@code true} si la conexión cumple la condición (la regla se "activa")
     */
    public boolean matches(Connection connection) {
        return condition.test(connection);
    }

    /**
     * Conector lógico AND (∧): combina esta regla con otra, produciendo una
     * nueva regla que solo se activa si AMBAS condiciones son verdaderas.
     * La severidad resultante es la mayor entre las dos reglas combinadas.
     */
    public Rule and(Rule other) {
        return new Rule(
                this.id + "_AND_" + other.id,
                "(" + this.description + ") Y (" + other.description + ")",
                this.condition.and(other.condition),
                maxSeverity(this.severity, other.severity)
        );
    }

    /**
     * Conector lógico OR (∨): combina esta regla con otra, produciendo una
     * nueva regla que se activa si CUALQUIERA de las dos condiciones es
     * verdadera. La severidad resultante es la mayor entre las dos reglas.
     */
    public Rule or(Rule other) {
        return new Rule(
                this.id + "_OR_" + other.id,
                "(" + this.description + ") O (" + other.description + ")",
                this.condition.or(other.condition),
                maxSeverity(this.severity, other.severity)
        );
    }

    /**
     * Conector lógico NOT (¬): produce una nueva regla que se activa
     * exactamente cuando esta NO se activaría.
     */
    public Rule negate() {
        return new Rule(
                "NOT_" + this.id,
                "NO (" + this.description + ")",
                this.condition.negate(),
                this.severity
        );
    }

    private static Severity maxSeverity(Severity a, Severity b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + id + ": " + description;
    }
}