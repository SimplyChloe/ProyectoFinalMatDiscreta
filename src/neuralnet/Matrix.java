package neuralnet;

import java.util.Random;
import java.util.function.DoubleUnaryOperator;

/**
 * Implementación mínima de álgebra matricial, construida desde cero (sin
 * librerías externas), suficiente para soportar la propagación hacia
 * adelante y la retropropagación de {@link NeuralNetwork}.
 * <p>
 * Todas las operaciones devuelven una <b>nueva</b> instancia de
 * {@code Matrix} en vez de modificar la actual (estilo inmutable), lo cual
 * simplifica razonar sobre el flujo de datos durante el entrenamiento, a
 * costa de crear más objetos temporales de los estrictamente necesarios
 * para una implementación de producción optimizada.
 * <p>
 * Esta clase representa la capa de <b>estructuras algebraicas</b> del
 * proyecto: las matrices con suma y multiplicación forman, junto con sus
 * propiedades (asociatividad, elemento neutro, etc.), la estructura sobre
 * la que se construye toda la red neuronal.
 */
public final class Matrix {

    private final int rows;
    private final int cols;
    private final double[][] data;

    public Matrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.data = new double[rows][cols];
    }

    public Matrix(double[][] data) {
        this.rows = data.length;
        this.cols = data.length == 0 ? 0 : data[0].length;
        this.data = data;
    }

    public double get(int r, int c) {
        return data[r][c];
    }

    public void set(int r, int c, double value) {
        data[r][c] = value;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    /** Construye un vector columna (matriz de {@code n} filas y 1 columna) a partir de un arreglo. */
    public static Matrix columnVector(double[] values) {
        Matrix m = new Matrix(values.length, 1);
        for (int i = 0; i < values.length; i++) {
            m.set(i, 0, values[i]);
        }
        return m;
    }

    /**
     * Extrae los valores de un vector columna (matriz de 1 sola columna)
     * como un arreglo plano. Lanza una excepción si la matriz no tiene
     * exactamente 1 columna.
     */
    public double[] toColumnArray() {
        if (cols != 1) {
            throw new IllegalStateException("toColumnArray() requiere una matriz de 1 columna, tiene " + cols);
        }
        double[] result = new double[rows];
        for (int i = 0; i < rows; i++) {
            result[i] = data[i][0];
        }
        return result;
    }

    /**
     * Inicializa una matriz con valores aleatorios uniformes en
     * {@code [-scale, scale]}. El parámetro {@code scale} debe elegirse
     * según el tamaño de la capa (ver {@link Layer}, que usa
     * inicialización estilo He/Xavier) para evitar que las activaciones
     * "exploten" o se "desvanezcan" en redes con varias capas.
     */
    public static Matrix random(int rows, int cols, Random rnd, double scale) {
        Matrix m = new Matrix(rows, cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                m.set(r, c, (rnd.nextDouble() * 2 - 1) * scale);
            }
        }
        return m;
    }

    /** Multiplicación matricial estándar: {@code (this.rows x other.cols)}. Requiere {@code this.cols == other.rows}. */
    public Matrix multiply(Matrix other) {
        if (this.cols != other.rows) {
            throw new IllegalArgumentException(
                    "Dimensiones incompatibles para multiplicar: (" + this.rows + "x" + this.cols +
                            ") * (" + other.rows + "x" + other.cols + ")");
        }
        Matrix result = new Matrix(this.rows, other.cols);
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < other.cols; j++) {
                double sum = 0;
                for (int k = 0; k < this.cols; k++) {
                    sum += this.data[i][k] * other.data[k][j];
                }
                result.set(i, j, sum);
            }
        }
        return result;
    }

    /** Suma elemento a elemento. Requiere que ambas matrices tengan las mismas dimensiones. */
    public Matrix add(Matrix other) {
        requireSameShape(other, "add");
        Matrix result = new Matrix(rows, cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.set(r, c, this.data[r][c] + other.data[r][c]);
            }
        }
        return result;
    }

    /** Resta elemento a elemento. Requiere que ambas matrices tengan las mismas dimensiones. */
    public Matrix subtract(Matrix other) {
        requireSameShape(other, "subtract");
        Matrix result = new Matrix(rows, cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.set(r, c, this.data[r][c] - other.data[r][c]);
            }
        }
        return result;
    }

    /** Producto de Hadamard (elemento a elemento). Requiere las mismas dimensiones. */
    public Matrix hadamard(Matrix other) {
        requireSameShape(other, "hadamard");
        Matrix result = new Matrix(rows, cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.set(r, c, this.data[r][c] * other.data[r][c]);
            }
        }
        return result;
    }

    /** Multiplica cada elemento por un escalar. */
    public Matrix scale(double factor) {
        Matrix result = new Matrix(rows, cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.set(r, c, this.data[r][c] * factor);
            }
        }
        return result;
    }

    /** Transpuesta: intercambia filas por columnas. */
    public Matrix transpose() {
        Matrix result = new Matrix(cols, rows);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.set(c, r, this.data[r][c]);
            }
        }
        return result;
    }

    /** Aplica una función escalar a cada elemento, devolviendo una nueva matriz (map funcional). */
    public Matrix map(DoubleUnaryOperator fn) {
        Matrix result = new Matrix(rows, cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.set(r, c, fn.applyAsDouble(this.data[r][c]));
            }
        }
        return result;
    }

    private void requireSameShape(Matrix other, String operation) {
        if (this.rows != other.rows || this.cols != other.cols) {
            throw new IllegalArgumentException(
                    "Dimensiones incompatibles para " + operation + ": (" + this.rows + "x" + this.cols +
                            ") vs (" + other.rows + "x" + other.cols + ")");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                sb.append(String.format("%.4f ", data[r][c]));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
