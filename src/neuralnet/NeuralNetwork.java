package neuralnet;

import java.util.List;

/**
 * Red neuronal como composición de {@link Layer} encadenadas: la salida de
 * una capa es la entrada de la siguiente. Matemáticamente, la red completa
 * es una función compuesta:
 * <pre>
 *   red(x) = f_n( f_{n-1}( ... f_1(x) ... ) )
 * </pre>
 * lo cual conecta directamente con el tema de <b>funciones y composición
 * de funciones</b> de matemática discreta: cada capa es una función, y la
 * red es literalmente su composición.
 */
public final class NeuralNetwork {

    private final List<Layer> layers;

    public NeuralNetwork(List<Layer> layers) {
        this.layers = layers;
    }

    /** Propaga una entrada a través de todas las capas en orden, devolviendo la salida final. */
    public Matrix forward(Matrix input) {
        Matrix current = input;
        for (Layer layer : layers) {
            current = layer.forward(current);
        }
        return current;
    }

    /**
     * Predice la probabilidad de que una conexión (representada por su
     * vector de características) sea un ataque. Devuelve un valor entre 0
     * (normal) y 1 (ataque).
     */
    public double predict(double[] features) {
        Matrix output = forward(Matrix.columnVector(features));
        return output.get(0, 0);
    }

    /**
     * Ejecuta un paso completo de entrenamiento (forward + backward) sobre
     * una única muestra, y devuelve la pérdida (entropía cruzada binaria)
     * de esa muestra antes de actualizar los pesos.
     * <p>
     * Para la combinación sigmoide (en la capa de salida) + entropía
     * cruzada binaria, el gradiente de la pérdida respecto a la
     * pre-activación de salida se simplifica algebraicamente a
     * {@code dL/dz = a - y} (donde {@code a} es la predicción y {@code y}
     * la etiqueta real). Esta es una simplificación clásica que evita
     * calcular por separado la derivada de la pérdida y la derivada de la
     * sigmoide (que, multiplicadas, se cancelan parcialmente). Por eso la
     * última capa se retropropaga con {@link Layer#backwardFromKnownDZ}
     * en vez del {@link Layer#backward} genérico.
     *
     * @param features     vector de características de la conexión
     * @param label        etiqueta real: 0.0 = normal, 1.0 = ataque
     * @param learningRate tasa de aprendizaje
     * @return la pérdida (entropía cruzada binaria) de esta muestra
     */
    public double trainStep(double[] features, double label, double learningRate) {
        Matrix input = Matrix.columnVector(features);
        Matrix output = forward(input);
        double predicted = output.get(0, 0);

        double loss = binaryCrossEntropy(predicted, label);

        // Gradiente simplificado dL/dz para la capa de salida (sigmoide + BCE).
        Matrix dZOutput = new Matrix(1, 1);
        dZOutput.set(0, 0, predicted - label);

        int lastIndex = layers.size() - 1;
        Matrix grad = layers.get(lastIndex).backwardFromKnownDZ(dZOutput, learningRate);

        // Para el resto de las capas (ocultas), retropropagación genérica.
        for (int i = lastIndex - 1; i >= 0; i--) {
            grad = layers.get(i).backward(grad, learningRate);
        }

        return loss;
    }

    /** Entropía cruzada binaria: mide qué tan lejos está la predicción {@code p} de la etiqueta real {@code y}. */
    private double binaryCrossEntropy(double predicted, double actual) {
        double eps = 1e-12; // evita log(0), que sería -infinito
        double p = Math.min(Math.max(predicted, eps), 1 - eps);
        return -(actual * Math.log(p) + (1 - actual) * Math.log(1 - p));
    }
}
