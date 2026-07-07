package rules;

/**
 * Nivel de gravedad asociado a una {@link Rule}. Se usa para priorizar
 * alertas cuando varias reglas se activan simultáneamente sobre la misma
 * conexión (el motor reporta la severidad máxima entre todas las reglas
 * activadas).
 * <p>
 * El orden de declaración importa: se usa {@link Enum#compareTo} para
 * determinar cuál severidad es "mayor" (LOW &lt; MEDIUM &lt; HIGH &lt; CRITICAL).
 */
public enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}