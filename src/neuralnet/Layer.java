package neuralnet;

import java.util.Random;

/**
 * Una capa densa (fully connected) de la red neuronal: aplica una
 * transformación lineal ({@code z = W*x + b}) seguida de una función de
 * activación no lineal ({@code a = f(z)}).
 * <p>
 * Cada capa es responsable de:
 * <ol>
 *   <li>Calcular su salida hacia adelante ({@link #forward(Matrix)}), guardando
 *       internamente los valores intermedios necesarios para la retropropagación.</li>
 *   <li>Calcular sus propios gradientes y actualizar sus pesos durante la
 *       retropropagación ({@link #backward(Matrix, double)}), devolviendo el
 *       gradiente que le corresponde propagar a la capa anterior.</li>
 * </ol>
 * Esta clase procesa <b>una muestra a la vez</b> (vectores columna de una
 * sola columna), es decir, implementa descenso de gradiente estocástico
 * (SGD) puro en vez de mini-batches. Se eligió así por simplicidad y
 * claridad pedagógica: cada paso de entrenamiento corresponde exactamente
 * a "ver una conexión, calcular el error, corregir los pesos un poco".
 */
public final class Layer {

    /** Funciones de activación soportadas. */
    public enum Activation { RELU, SIGMOID }

    private Matrix weights; // outputSize x inputSize
    private Matrix bias;    // outputSize x 1
    private final Activation activation;

    // Estado cacheado durante forward(), necesario para backward().
    private Matrix lastInput;  // inputSize x 1
    private Matrix lastZ;      // outputSize x 1 (pre-activación)
    private Matrix lastOutput; // outputSize x 1 (post-activación)

    /**
     * @param inputSize  dimensión del vector de entrada
     * @param outputSize número de neuronas de esta capa
     * @param activation función de activación a usar
     * @param rnd        generador de números aleatorios (compartido entre capas, para reproducibilidad con una semilla)
     */
    public Layer(int inputSize, int outputSize, Activation activation, Random rnd) {
        this.activation = activation;

        // Inicialización de pesos: escalas inspiradas en He (para ReLU) y
        // Xavier/Glorot (para sigmoide), que evitan que las activaciones se
        // "saturen" o "desvanezcan" al pasar por varias capas.
        double scale = (activation == Activation.RELU)
                ? Math.sqrt(2.0 / inputSize)
                : Math.sqrt(1.0 / inputSize);

        this.weights = Matrix.random(outputSize, inputSize, rnd, scale);
        this.bias = new Matrix(outputSize, 1); // los sesgos empiezan en cero
    }

    /** Propagación hacia adelante: calcula {@code a = f(W*x + b)} y cachea los valores intermedios. */
    public Matrix forward(Matrix input) {
        this.lastInput = input;
        this.lastZ = weights.multiply(input).add(bias);
        this.lastOutput = lastZ.map(this::activate);
        return lastOutput;
    }

    private double activate(double z) {
        return activation == Activation.RELU ? Math.max(0, z) : sigmoid(z);
    }

    private static double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }

    /**
     * Derivada de la activación respecto a {@code z}, evaluada usando los
     * valores ya calculados de {@code z} y {@code a = f(z)} (para
     * sigmoide, la derivada se expresa convenientemente como
     * {@code a*(1-a)}, evitando recalcular la exponencial).
     */
    private double activationDerivative(double z, double a) {
        return activation == Activation.RELU ? (z > 0 ? 1.0 : 0.0) : a * (1 - a);
    }

    /**
     * Retropropagación estándar: recibe el gradiente de la pérdida
     * respecto a la salida <b>post-activación</b> de esta capa
     * ({@code dL/da}), lo convierte al gradiente respecto a la
     * pre-activación ({@code dL/dz = dL/da * f'(z)}, regla de la cadena),
     * actualiza los pesos y sesgos mediante descenso de gradiente, y
     * devuelve el gradiente que le corresponde a la capa anterior
     * ({@code dL/dInput}).
     *
     * @param dOutput      gradiente de la pérdida respecto a la salida de esta capa (dL/da)
     * @param learningRate tasa de aprendizaje (tamaño del paso de descenso de gradiente)
     * @return el gradiente de la pérdida respecto a la entrada de esta capa (dL/dInput)
     */
    public Matrix backward(Matrix dOutput, double learningRate) {
        Matrix dZ = new Matrix(lastZ.getRows(), 1);
        for (int i = 0; i < lastZ.getRows(); i++) {
            double z = lastZ.get(i, 0);
            double a = lastOutput.get(i, 0);
            dZ.set(i, 0, dOutput.get(i, 0) * activationDerivative(z, a));
        }
        return applyGradientAndUpdate(dZ, learningRate);
    }

    /**
     * Variante de retropropagación para el caso especial de la capa de
     * salida combinada con sigmoide + entropía cruzada binaria, donde el
     * gradiente {@code dL/dz} ya viene simplificado analíticamente como
     * {@code (a - y)} (ver {@link NeuralNetwork#trainStep}). En ese caso
     * NO hay que volver a multiplicar por la derivada de la activación
     * (ya está incorporada en la simplificación), así que este método
     * recibe {@code dZ} directamente en vez de {@code dOutput}.
     */
    public Matrix backwardFromKnownDZ(Matrix dZ, double learningRate) {
        return applyGradientAndUpdate(dZ, learningRate);
    }

    /** Lógica común: calcula dWeights/dBias a partir de dZ, actualiza los parámetros, y devuelve dInput. */
    private Matrix applyGradientAndUpdate(Matrix dZ, double learningRate) {
        // dL/dW = dZ * inputTranspuesta   (regla de la cadena para la parte lineal)
        Matrix dWeights = dZ.multiply(lastInput.transpose());
        // dL/db = dZ
        // dL/dInput = W^T * dZ  (esto es lo que se propaga a la capa anterior)
        Matrix dInput = weights.transpose().multiply(dZ);

        // Descenso de gradiente: parámetro -= tasaAprendizaje * gradiente
        this.weights = weights.subtract(dWeights.scale(learningRate));
        this.bias = bias.subtract(dZ.scale(learningRate));

        return dInput;
    }

    public Matrix getWeights() {
        return weights;
    }

    public Matrix getBias() {
        return bias;
    }
}
