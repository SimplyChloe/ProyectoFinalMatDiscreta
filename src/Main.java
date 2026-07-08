import data.DatasetLoader;
import model.Connection;
import neuralnet.FeatureExtractor;
import neuralnet.NeuralNetwork;
import neuralnet.Trainer;
import rules.EvaluationResult;
import rules.RuleEngine;
import stats.BayesClassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Punto de entrada principal: Detector de Intrusiones integrado.
 * 
 * Orquesta la evaluación de tres modelos de detección:
 *   1. Motor de Reglas (Lógica Proposicional)
 *   2. Clasificador Naive Bayes (Probabilidad)
 *   3. Red Neuronal (Aprendizaje Profundo)
 * 
 * Todos comparten el mismo pipeline y usan el conjunto de entrenamiento
 * KDDTrain+.txt para entrenar y KDDTest+.txt para evaluar. Los resultados se
 * presentan en una tabla comparativa con métricas: precisión, recall,
 * F1-score, tiempo de predicción y desempeño global.
 */
public final class Main {

    private static final String TRAIN_PATH = "data/KDDTrain+.txt";
    private static final String TEST_PATH = "data/KDDTest+.txt";
    private static final long SEED = 42L;  // Reproducibilidad

    private Main() {
        // Clase de utilidad: no se instancia.
    }

    public static void main(String[] args) {
        String trainPath = args.length > 0 ? args[0] : TRAIN_PATH;
        String testPath = args.length > 1 ? args[1] : TEST_PATH;

        try {
            System.out.println("====================================================================");
            System.out.println("  DETECTOR DE INTRUSIONES - PIPELINE INTEGRADO");
            System.out.println("====================================================================\n");

            // 1. Cargar datasets de entrenamiento y prueba
            System.out.println("1. Cargando dataset de entrenamiento desde: " + trainPath);
            List<Connection> trainingSet = DatasetLoader.load(trainPath);
            System.out.println("   ✓ Entrenamiento cargado: " + trainingSet.size() + " conexiones");

            System.out.println("2. Cargando dataset de prueba desde: " + testPath);
            List<Connection> testSet = DatasetLoader.load(testPath);
            System.out.println("   ✓ Prueba cargada: " + testSet.size() + " conexiones");

            // Crear tabla de resultados
            ComparisonTable table = new ComparisonTable();

            // 3. MODELO 1: Motor de Reglas
            System.out.println("\n3. Entrenando Motor de Reglas...");
            ModelResult rulesResult = trainAndEvaluateRules(trainingSet, testSet);
            table.addResult("Reglas (Lógica)", rulesResult);

            // 4. MODELO 2: Clasificador Naive Bayes
            System.out.println("\n4. Entrenando Clasificador Naive Bayes...");
            ModelResult bayesResult = trainAndEvaluateBayes(trainingSet, testSet);
            table.addResult("Bayes (Probabilidad)", bayesResult);

            // 5. MODELO 3: Red Neuronal
            System.out.println("\n5. Entrenando Red Neuronal...");
            ModelResult nnResult = trainAndEvaluateNeuralNetwork(trainingSet, testSet);
            table.addResult("Red Neuronal", nnResult);

            // 6. Mostrar tabla comparativa
            System.out.println("\n====================================================================");
            System.out.println("  TABLA COMPARATIVA DE DESEMPEÑO");
            System.out.println("====================================================================\n");
            table.print();

        } catch (IOException e) {
            System.err.println("Error cargando dataset: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Entrena el motor de reglas y lo evalúa sobre el conjunto de prueba.
     */
    private static ModelResult trainAndEvaluateRules(
            List<Connection> trainingSet, List<Connection> testSet) {

        RuleEngine engine = RuleEngine.createDefaultRuleEngine();
        System.out.println("   ✓ Motor de reglas configurado con " + engine.getRules().size() + " reglas");

        // Las reglas no necesitan entrenamiento con datos, ya están codificadas como heurísticas
        return evaluateModel(engine, testSet, "Reglas");
    }

    /**
     * Entrena el clasificador Naive Bayes y lo evalúa.
     */
    private static ModelResult trainAndEvaluateBayes(
            List<Connection> trainingSet, List<Connection> testSet) {

        BayesClassifier classifier = new BayesClassifier();
        classifier.train(trainingSet);
        System.out.println("   ✓ Clasificador entrenado con " + trainingSet.size() + " conexiones");

        return evaluateClassifier(classifier, testSet, "Bayes");
    }

    /**
     * Entrena la red neuronal y la evalúa.
     */
    private static ModelResult trainAndEvaluateNeuralNetwork(
            List<Connection> trainingSet, List<Connection> testSet) {

        NeuralNetwork network = Trainer.buildDefaultNetwork(SEED);
        System.out.println("   ✓ Red neuronal construida (22 → 12 ReLU → 1 Sigmoide)");

        // Entrenar
        System.out.println("   - Entrenando durante 5 épocas...");
        Trainer.train(network, trainingSet, 5, 0.02, SEED);
        System.out.println("   ✓ Entrenamiento completado");

        return evaluateNeuralNetwork(network, testSet, "NN");
    }

    /**
     * Evalúa el motor de reglas: isSuspicious() indica predicción positiva (ataque).
     */
    private static ModelResult evaluateModel(
            RuleEngine engine, List<Connection> testSet, String modelName) {

        long startTime = System.currentTimeMillis();
        int tp = 0, fp = 0, tn = 0, fn = 0;

        for (Connection conn : testSet) {
            EvaluationResult result = engine.evaluate(conn);
            boolean predicted = result.isSuspicious();  // true = ataque, false = normal
            boolean actual = !conn.isNormal();           // true = ataque, false = normal

            if (predicted && actual) tp++;
            else if (predicted && !actual) fp++;
            else if (!predicted && !actual) tn++;
            else fn++;
        }

        long duration = System.currentTimeMillis() - startTime;
        return new ModelResult(modelName, tp, fp, tn, fn, duration);
    }

    /**
     * Evalúa el clasificador Bayes: predict("attack") indica predicción positiva.
     */
    private static ModelResult evaluateClassifier(
            BayesClassifier classifier, List<Connection> testSet, String modelName) {

        long startTime = System.currentTimeMillis();
        int tp = 0, fp = 0, tn = 0, fn = 0;

        for (Connection conn : testSet) {
            String predicted = classifier.predict(conn);  // "normal" o "attack"
            boolean predAtack = "attack".equals(predicted);
            boolean actual = !conn.isNormal();  // true = ataque

            if (predAtack && actual) tp++;
            else if (predAtack && !actual) fp++;
            else if (!predAtack && !actual) tn++;
            else fn++;
        }

        long duration = System.currentTimeMillis() - startTime;
        return new ModelResult(modelName, tp, fp, tn, fn, duration);
    }

    /**
     * Evalúa la red neuronal: probabilidad > 0.5 indica predicción positiva (ataque).
     */
    private static ModelResult evaluateNeuralNetwork(
            NeuralNetwork network, List<Connection> testSet, String modelName) {

        long startTime = System.currentTimeMillis();
        int tp = 0, fp = 0, tn = 0, fn = 0;

        for (Connection conn : testSet) {
            double[] features = FeatureExtractor.extract(conn);
            double probability = network.predict(features);  // [0, 1]
            boolean predicted = probability > 0.5;           // true = ataque
            boolean actual = !conn.isNormal();

            if (predicted && actual) tp++;
            else if (predicted && !actual) fp++;
            else if (!predicted && !actual) tn++;
            else fn++;
        }

        long duration = System.currentTimeMillis() - startTime;
        return new ModelResult(modelName, tp, fp, tn, fn, duration);
    }

    /**
     * Contenedor de resultados de un modelo con métricas calculadas.
     */
    static class ModelResult {
        String name;
        int tp, fp, tn, fn;
        long durationMs;

        ModelResult(String name, int tp, int fp, int tn, int fn, long durationMs) {
            this.name = name;
            this.tp = tp;
            this.fp = fp;
            this.tn = tn;
            this.fn = fn;
            this.durationMs = durationMs;
        }

        double getPrecision() {
            if (tp + fp == 0) return 0.0;
            return (double) tp / (tp + fp);
        }

        double getRecall() {
            if (tp + fn == 0) return 0.0;
            return (double) tp / (tp + fn);
        }

        double getF1() {
            double p = getPrecision();
            double r = getRecall();
            if (p + r == 0) return 0.0;
            return 2 * (p * r) / (p + r);
        }

        double getAccuracy() {
            int total = tp + fp + tn + fn;
            if (total == 0) return 0.0;
            return (double) (tp + tn) / total;
        }

        double getSpecificity() {
            if (tn + fp == 0) return 0.0;
            return (double) tn / (tn + fp);
        }
    }

    /**
     * Tabla comparativa que muestra resultados de múltiples modelos.
     */
    static class ComparisonTable {
        List<ModelResult> results = new ArrayList<>();

        void addResult(String label, ModelResult result) {
            result.name = label;
            results.add(result);
        }

        void print() {
            // Encabezado
            System.out.printf("%-20s | %10s | %10s | %10s | %10s | %10s | %12s%n",
                    "Modelo", "Precisión", "Recall", "F1-Score", "Exactitud", "Especif.", "Tiempo (ms)");
            System.out.println("-" + "-".repeat(119));

            // Filas
            for (ModelResult r : results) {
                System.out.printf("%-20s | %10.4f | %10.4f | %10.4f | %10.4f | %10.4f | %12d%n",
                        r.name,
                        r.getPrecision(),
                        r.getRecall(),
                        r.getF1(),
                        r.getAccuracy(),
                        r.getSpecificity(),
                        r.durationMs);
            }

            System.out.println("\n" + "=".repeat(121));
            System.out.println("Matriz de confusión de cada modelo:");
            System.out.println("=".repeat(121) + "\n");

            for (ModelResult r : results) {
                System.out.printf("%s:%n", r.name);
                System.out.printf("  TP (Verdaderos Positivos): %5d  FN (Falsos Negativos): %5d%n", r.tp, r.fn);
                System.out.printf("  FP (Falsos Positivos):     %5d  TN (Verdaderos Negativos): %5d%n", r.fp, r.tn);
                System.out.printf("  Total: %d conexiones%n%n", r.tp + r.fp + r.tn + r.fn);
            }
        }
    }
}
