package rules;

import data.DatasetLoader;
import model.Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de reglas lógicas para detección de intrusiones. Mantiene un
 * conjunto de {@link Rule} y evalúa cada {@link Connection} contra todas
 * ellas, sin detenerse en la primera que se active (una conexión puede
 * disparar varias señales de alerta a la vez).
 * <p>
 * Este módulo representa la capa de <b>lógica proposicional</b> del
 * proyecto: cada regla es una fórmula booleana compuesta por proposiciones
 * atómicas (comparaciones sobre atributos de la conexión) combinadas con
 * los conectores ∧ (AND), ∨ (OR) y ¬ (NOT).
 */
public final class RuleEngine {

    private final List<Rule> rules = new ArrayList<>();

    public void addRule(Rule rule) {
        rules.add(rule);
    }

    public List<Rule> getRules() {
        return rules;
    }

    /**
     * Evalúa una conexión contra todas las reglas registradas.
     *
     * @return un {@link EvaluationResult} con todas las reglas que se activaron
     */
    public EvaluationResult evaluate(Connection connection) {
        List<Rule> triggered = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.matches(connection)) {
                triggered.add(rule);
            }
        }
        return new EvaluationResult(connection, triggered);
    }

    /**
     * Construye un motor con un conjunto de reglas iniciales, inspiradas en
     * patrones de ataque documentados para el dataset NSL-KDD. Los umbrales
     * (ej. {@code count > 50}) son heurísticos, elegidos para ilustrar el
     * concepto; en un sistema real se calibrarían con datos históricos
     * (ver módulo de estadística, que veremos más adelante).
     */
    public static RuleEngine createDefaultRuleEngine() {
        RuleEngine engine = new RuleEngine();

        // R01: posible SYN flood / escaneo agresivo de puertos.
        // P: la conexión nunca se completó (S0 = intento sin respuesta)
        // Q: hubo muchísimas conexiones al mismo host en la ventana de 2s
        // R: la tasa de error SYN en esa ventana es muy alta
        // Regla = P ∧ Q ∧ R
        engine.addRule(new Rule(
                "R01_SYN_FLOOD",
                "flag=S0 Y count>50 Y serrorRate>0.5 (posible inundación SYN)",
                c -> "S0".equals(c.getFlag()) && c.getCount() > 50 && c.getSerrorRate() > 0.5,
                Severity.HIGH
        ));

        // R02: escaneo de puertos tipo nmap/satan.
        // Muchas conexiones al mismo host, pero a servicios MUY distintos entre sí.
        engine.addRule(new Rule(
                "R02_PORT_SCAN",
                "count>50 Y diffSrvRate>0.5 (posible escaneo de puertos)",
                c -> c.getCount() > 50 && c.getDiffSrvRate() > 0.5,
                Severity.HIGH
        ));

        // R03: ataque de fuerza bruta contra login (ej. guess_passwd).
        engine.addRule(new Rule(
                "R03_BRUTE_FORCE_LOGIN",
                "numFailedLogins>=3 (posible fuerza bruta de contraseña)",
                c -> c.getNumFailedLogins() >= 3,
                Severity.MEDIUM
        ));

        // R04: escalamiento de privilegios / compromiso de root.
        // P: se obtuvo una shell root
        // Q: se registran operaciones de tipo root Y la sesión inició sesión
        // Regla = P ∨ Q  (cualquiera de las dos es motivo de alarma crítica)
        engine.addRule(new Rule(
                "R04_ROOT_COMPROMISE",
                "rootShell=true O (numRoot>0 Y loggedIn=true) (posible escalamiento a root)",
                c -> c.isRootShell() || (c.getNumRoot() > 0 && c.isLoggedIn()),
                Severity.CRITICAL
        ));

        // R05: posible ataque tipo smurf/ICMP flood.
        // Muchísimo tráfico ICMP repetido hacia el mismo servicio.
        engine.addRule(new Rule(
                "R05_ICMP_FLOOD",
                "protocol=icmp Y srvCount>100 Y sameSrvRate>0.9 (posible ICMP flood tipo smurf)",
                c -> "icmp".equals(c.getProtocolType()) && c.getSrvCount() > 100 && c.getSameSrvRate() > 0.9,
                Severity.HIGH
        ));

        // R06: acceso de invitado a un servicio sensible (ej. ftp, telnet).
        // Señal de baja severidad por sí sola, pero relevante combinada con otras.
        engine.addRule(new Rule(
                "R06_GUEST_SENSITIVE_SERVICE",
                "isGuestLogin=true Y service en {ftp, telnet} (acceso de invitado a servicio sensible)",
                c -> c.isGuestLogin() && ("ftp".equals(c.getService()) || "telnet".equals(c.getService())),
                Severity.LOW
        ));

        // R07: creación sospechosa de archivos tras acceso con anomalías previas.
        // P: se crearon archivos durante la sesión
        // Q: además hubo indicios "hot" (accesos a puntos sensibles)
        engine.addRule(new Rule(
                "R07_SUSPICIOUS_FILE_ACTIVITY",
                "numFileCreations>0 Y hot>0 (actividad de archivos tras acceso a puntos sensibles)",
                c -> c.getNumFileCreations() > 0 && c.getHot() > 0,
                Severity.MEDIUM
        ));

        return engine;
    }

    /**
     * Punto de entrada de prueba: carga un dataset NSL-KDD, evalúa cada
     * conexión contra el conjunto de reglas por defecto, y compara los
     * resultados contra la etiqueta real del dataset para medir qué tan
     * bien (o mal) funcionan reglas puramente lógicas sin estadística.
     */
    public static void main(String[] args) {
        String path = args.length > 0 ? args[0] : "data/KDDTrain+.txt";
        RuleEngine engine = createDefaultRuleEngine();

        System.out.println("=== Reglas cargadas ===");
        for (Rule r : engine.getRules()) {
            System.out.println("  " + r);
        }
        System.out.println();

        try {
            List<Connection> connections = DatasetLoader.load(path);
            evaluateAndReport(engine, connections);
        } catch (IOException e) {
            System.err.println("Error al leer el archivo '" + path + "': " + e.getMessage());
        }
    }

    /**
     * Evalúa todas las conexiones y compara contra la etiqueta real para
     * calcular una matriz de confusión simple (verdaderos/falsos
     * positivos/negativos). Esto es un adelanto de lo que formalizaremos
     * más en el módulo de estadística.
     */
    private static void evaluateAndReport(RuleEngine engine, List<Connection> connections) {
        int truePositives = 0;  // regla activada Y es ataque real
        int falsePositives = 0; // regla activada Y en realidad es normal
        int trueNegatives = 0;  // regla NO activada Y es normal
        int falseNegatives = 0; // regla NO activada Y en realidad es ataque

        for (Connection c : connections) {
            EvaluationResult result = engine.evaluate(c);
            boolean flagged = result.isSuspicious();
            boolean actualAttack = !c.isNormal();

            if (flagged && actualAttack) truePositives++;
            else if (flagged && !actualAttack) falsePositives++;
            else if (!flagged && !actualAttack) trueNegatives++;
            else falseNegatives++;
        }

        int total = connections.size();
        System.out.println("=== Resultado de evaluación sobre " + total + " conexiones ===");
        System.out.println("Verdaderos positivos  (ataque detectado correctamente): " + truePositives);
        System.out.println("Falsos positivos       (alerta sobre tráfico normal):    " + falsePositives);
        System.out.println("Verdaderos negativos   (normal, sin alerta, correcto):   " + trueNegatives);
        System.out.println("Falsos negativos       (ataque NO detectado):            " + falseNegatives);

        double precision = truePositives + falsePositives == 0 ? 0.0 :
                (double) truePositives / (truePositives + falsePositives);
        double recall = truePositives + falseNegatives == 0 ? 0.0 :
                (double) truePositives / (truePositives + falseNegatives);

        System.out.printf("Precisión: %.2f%% | Recall (sensibilidad): %.2f%%%n",
                precision * 100, recall * 100);
    }
}