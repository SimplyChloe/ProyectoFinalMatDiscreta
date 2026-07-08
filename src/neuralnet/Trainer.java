package neuralnet;

import data.DatasetLoader;
import model.Connection;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Orquesta la construcción, entrenamiento y evaluación de la
 * {@link NeuralNetwork} sobre el dataset NSL-KDD.
 * <p>
 * Arquitectura por defecto: 22 (entrada) → 12 (oculta, ReLU) → 1 (salida, sigmoide).
 * Hiperparámetros (tasa de aprendizaje, épocas) fueron calibrados
 * experimentalmente sobre el propio dataset de entrenamiento antes de
 * fijarse aquí.
 */
public final class Trainer {

    private static final int HIDDEN_SIZE = 12;
    private static final double LEARNING_RATE = 0.02;
    private static final int EPOCHS = 5;
    private static final long SEED = 42L;

    private Trainer() {
        // Clase de utilidad: no se instancia.
    }

    /** Construye la red con la arquitectura por defecto (22 → 12 ReLU → 1 Sigmoide). */
    public static NeuralNetwork buildDefaultNetwork(long seed) {
        Random rnd = new Random(seed);
        Layer hidden = new Layer(FeatureExtractor.FEATURE_COUNT, HIDDEN_SIZE, Layer.Activation.RELU, rnd);
        Layer output = new Layer(HIDDEN_SIZE, 1, Layer.Activation.SIGMOID, rnd);
        return new NeuralNetwork(List.of(hidden, output));
    }

    /**
     * Entrena la red durante {@code epochs} pasadas completas sobre
     * {@code trainingSet}, barajando el orden en cada época (para que la
     * red no aprenda un patrón artificial del orden de los datos).
     * Imprime la pérdida promedio de cada época.
     */
    public static void train(NeuralNetwork network, List<Connection> trainingSet, int epochs,
                              double learningRate, long seed) {
        Random rnd = new Random(seed);

        for (int epoch = 1; epoch <= epochs; epoch++) {
            Collections.shuffle(trainingSet, rnd);
            double totalLoss = 0.0;

            for (Connection c : trainingSet) {
                double[] features = FeatureExtractor.extract(c);
                double label = FeatureExtractor.extractLabel(c);
                totalLoss += network.trainStep(features, label, learningRate);
            }

            double avgLoss = totalLoss / trainingSet.size();
            System.out.printf("Época %d/%d - pérdida promedio: %.4f%n", epoch, epochs, avgLoss);
        }
    }

    /**
     * Punto de entrada de prueba: carga el dataset, hace un split 80/20
     * (igual que en {@link stats.BayesClassifier}, para poder comparar
     * ambos modelos bajo las mismas condiciones), entrena la red, y
     * evalúa su desempeño con una matriz de confusión.
     */
    public static void main(String[] args) {
        String path = args.length > 0 ? args[0] : "data/KDDTrain+.txt";

        try {
            List<Connection> all = DatasetLoader.load(path);
            Collections.shuffle(all, new Random(SEED));

            int splitIndex = (int) (all.size() * 0.8);
            List<Connection> trainSet = all.subList(0, splitIndex);
            List<Connection> testSet = all.subList(splitIndex, all.size());

            System.out.println("=== Entrenando red neuronal ===");
            System.out.println("Arquitectura: " + FeatureExtractor.FEATURE_COUNT + " -> " + HIDDEN_SIZE + " (ReLU) -> 1 (Sigmoide)");
            System.out.println("Conexiones de entrenamiento: " + trainSet.size());
            System.out.println("Conexiones de prueba: " + testSet.size());
            System.out.println();

            NeuralNetwork network = buildDefaultNetwork(SEED);
            train(network, trainSet, EPOCHS, LEARNING_RATE, SEED);

            evaluateAndReport(network, testSet);
        } catch (IOException e) {
            System.err.println("Error al leer el archivo '" + path + "': " + e.getMessage());
        }
    }

    private static void evaluateAndReport(NeuralNetwork network, List<Connection> testSet) {
        int truePositives = 0, falsePositives = 0, trueNegatives = 0, falseNegatives = 0;

        for (Connection c : testSet) {
            double probability = network.predict(FeatureExtractor.extract(c));
            boolean predictedAttack = probability > 0.5;
            boolean actualAttack = !c.isNormal();

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
