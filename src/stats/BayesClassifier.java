package stats;

import data.DatasetLoader;
import model.Connection;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clasificador Naive Bayes para distinguir conexiones {@code normal} de
 * {@code attack}, basado en el Teorema de Bayes con el supuesto
 * ("naive"/ingenuo) de independencia condicional entre atributos dada la
 * clase:
 * <pre>
 *   P(clase | x1, x2, ..., xn)  ∝  P(clase) * P(x1|clase) * P(x2|clase) * ... * P(xn|clase)
 * </pre>
 * <p>
 * Se usan 4 atributos como evidencia: {@code protocol_type}, {@code service},
 * {@code flag} (categóricos, tal cual vienen en el dataset), y una versión
 * <b>discretizada</b> de {@code same_srv_rate} (una variable continua,
 * convertida a las categorías LOW/MEDIUM/HIGH) — esto último para ilustrar
 * la técnica de discretización, necesaria porque Naive Bayes categórico
 * clásico trabaja con conteos de valores discretos, no con densidades
 * continuas.
 * <p>
 * Para evitar probabilidades cero cuando una combinación (clase, atributo,
 * valor) nunca apareció en el entrenamiento, se aplica <b>suavizado de
 * Laplace</b> (add-one smoothing): en vez de estimar
 * {@code P(valor|clase) = count / total}, se usa
 * {@code P(valor|clase) = (count + 1) / (total + |dominio del atributo|)}.
 * <p>
 * Los productos de probabilidades se calculan en espacio logarítmico
 * (sumando logaritmos en vez de multiplicar probabilidades directamente)
 * para evitar underflow numérico cuando se combinan muchos atributos con
 * probabilidades pequeñas.
 */
public final class BayesClassifier {

    private static final double LAPLACE_SMOOTHING = 1.0;

    /** Conteo de conexiones por clase ("normal" / "attack"). */
    private final Map<String, Integer> classCounts = new HashMap<>();

    /** classFeatureValueCounts[clase][nombreAtributo][valorAtributo] = conteo. */
    private final Map<String, Map<String, Map<String, Integer>>> featureCounts = new HashMap<>();

    /** Dominio (conjunto de valores posibles observados) de cada atributo, usado en el suavizado. */
    private final Map<String, Set<String>> featureDomains = new HashMap<>();

    private int totalCount = 0;

    /**
     * Entrena el clasificador acumulando conteos a partir de una lista de
     * conexiones ya etiquetadas. Se puede llamar varias veces para seguir
     * entrenando con más datos (los conteos se acumulan).
     */
    public void train(List<Connection> connections) {
        for (Connection c : connections) {
            String cls = c.isNormal() ? "normal" : "attack";
            classCounts.merge(cls, 1, Integer::sum);
            totalCount++;

            for (Map.Entry<String, String> feature : extractFeatures(c).entrySet()) {
                featureCounts
                        .computeIfAbsent(cls, k -> new HashMap<>())
                        .computeIfAbsent(feature.getKey(), k -> new HashMap<>())
                        .merge(feature.getValue(), 1, Integer::sum);
                featureDomains
                        .computeIfAbsent(feature.getKey(), k -> new HashSet<>())
                        .add(feature.getValue());
            }
        }
    }

    /**
     * Extrae las características categóricas usadas como evidencia para
     * la clasificación. {@code same_srv_rate} (continua) se discretiza en
     * 3 categorías mediante {@link #bucketize(double)}.
     */
    private Map<String, String> extractFeatures(Connection c) {
        Map<String, String> features = new LinkedHashMap<>();
        features.put("protocol", c.getProtocolType());
        features.put("service", c.getService());
        features.put("flag", c.getFlag());
        features.put("sameSrvBucket", bucketize(c.getSameSrvRate()));
        return features;
    }

    /** Discretiza una tasa (valor entre 0 y 1) en 3 categorías: LOW, MEDIUM, HIGH. */
    private String bucketize(double rate) {
        if (rate < 0.33) return "LOW";
        if (rate < 0.66) return "MEDIUM";
        return "HIGH";
    }

    /**
     * Predice la clase más probable ("normal" o "attack") para una
     * conexión nueva, eligiendo la clase que maximiza
     * {@code log P(clase) + Σ log P(atributo_i | clase)}.
     */
    public String predict(Connection c) {
        Map<String, String> features = extractFeatures(c);
        double bestLogProb = Double.NEGATIVE_INFINITY;
        String bestClass = null;

        for (String cls : classCounts.keySet()) {
            double logProb = Math.log(classCounts.get(cls) / (double) totalCount);

            for (Map.Entry<String, String> feature : features.entrySet()) {
                String featureName = feature.getKey();
                String featureValue = feature.getValue();

                int domainSize = featureDomains.getOrDefault(featureName, Collections.emptySet()).size();
                int count = featureCounts
                        .getOrDefault(cls, Collections.emptyMap())
                        .getOrDefault(featureName, Collections.emptyMap())
                        .getOrDefault(featureValue, 0);
                int classTotal = classCounts.get(cls);

                double likelihood = (count + LAPLACE_SMOOTHING) / (classTotal + LAPLACE_SMOOTHING * domainSize);
                logProb += Math.log(likelihood);
            }

            if (logProb > bestLogProb) {
                bestLogProb = logProb;
                bestClass = cls;
            }
        }

        return bestClass;
    }

    /**
     * Punto de entrada de prueba: carga el dataset, hace un split 80/20
     * (entrenamiento/prueba) dentro del mismo archivo, entrena el
     * clasificador, y evalúa su desempeño con una matriz de confusión.
     * <p>
     * Se usa un split interno (en vez de requerir {@code KDDTest+.txt})
     * porque, en este punto del proyecto, es posible que aún no tengas
     * ese segundo archivo en tu carpeta {@code data/}.
     */
    public static void main(String[] args) {
        String path = args.length > 0 ? args[0] : "data/KDDTrain+.txt";

        try {
            List<Connection> all = DatasetLoader.load(path);
            Collections.shuffle(all, new java.util.Random(42));

            int splitIndex = (int) (all.size() * 0.8);
            List<Connection> trainSet = all.subList(0, splitIndex);
            List<Connection> testSet = all.subList(splitIndex, all.size());

            BayesClassifier classifier = new BayesClassifier();
            classifier.train(trainSet);

            System.out.println("=== Entrenamiento completado ===");
            System.out.println("Conexiones de entrenamiento: " + trainSet.size());
            System.out.println("Conexiones de prueba: " + testSet.size());

            evaluateAndReport(classifier, testSet);
        } catch (IOException e) {
            System.err.println("Error al leer el archivo '" + path + "': " + e.getMessage());
        }
    }

    private static void evaluateAndReport(BayesClassifier classifier, List<Connection> testSet) {
        int truePositives = 0, falsePositives = 0, trueNegatives = 0, falseNegatives = 0;

        for (Connection c : testSet) {
            String predicted = classifier.predict(c);
            boolean actualAttack = !c.isNormal();
            boolean predictedAttack = "attack".equals(predicted);

            if (predictedAttack && actualAttack) truePositives++;
            else if (predictedAttack && !actualAttack) falsePositives++;
            else if (!predictedAttack && !actualAttack) trueNegatives++;
            else falseNegatives++;
        }

        int total = testSet.size();
        double accuracy = (truePositives + trueNegatives) / (double) total;
        double precision = truePositives + falsePositives == 0 ? 0.0 :
                (double) truePositives / (truePositives + falsePositives);
        double recall = truePositives + falseNegatives == 0 ? 0.0 :
                (double) truePositives / (truePositives + falseNegatives);

        System.out.println("\n=== Resultado sobre el conjunto de prueba (" + total + " conexiones) ===");
        System.out.println("Verdaderos positivos: " + truePositives);
        System.out.println("Falsos positivos:     " + falsePositives);
        System.out.println("Verdaderos negativos: " + trueNegatives);
        System.out.println("Falsos negativos:     " + falseNegatives);
        System.out.printf("Accuracy: %.2f%% | Precisión: %.2f%% | Recall: %.2f%%%n",
                accuracy * 100, precision * 100, recall * 100);
    }
}