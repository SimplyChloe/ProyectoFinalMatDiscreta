package neuralnet;

import model.Connection;

/**
 * Convierte una {@link Connection} en un vector numérico de 22 dimensiones,
 * apto como entrada de {@link NeuralNetwork}. Se eligieron deliberadamente
 * atributos que <b>ya están naturalmente en el rango [0, 1]</b> (tasas y
 * valores binarios), para evitar tener que construir un pipeline de
 * normalización adicional en esta primera versión del módulo.
 * <p>
 * Composición del vector (22 posiciones, en este orden):
 * <ol>
 *   <li>[0-3] 4 atributos binarios: {@code land}, {@code loggedIn}, {@code rootShell}, {@code isGuestLogin}</li>
 *   <li>[4-18] 15 tasas (todas ya entre 0 y 1 en el dataset original): serrorRate,
 *       srvSerrorRate, rerrorRate, srvRerrorRate, sameSrvRate, diffSrvRate,
 *       srvDiffHostRate, dstHostSameSrvRate, dstHostDiffSrvRate,
 *       dstHostSameSrcPortRate, dstHostSrvDiffHostRate, dstHostSerrorRate,
 *       dstHostSrvSerrorRate, dstHostRerrorRate, dstHostSrvRerrorRate</li>
 *   <li>[19-21] codificación one-hot de {@code protocol_type}: tcp, udp, icmp</li>
 * </ol>
 * <p>
 * Nota de diseño: atributos como {@code src_bytes}, {@code dst_bytes},
 * {@code count} o {@code duration} se dejaron fuera de esta primera
 * versión porque tienen rangos muy amplios y no acotados (requieren
 * normalización min-max o logarítmica antes de poder alimentarlos a la
 * red de forma segura). Queda como una extensión natural del proyecto.
 */
public final class FeatureExtractor {

    /** Número de posiciones del vector de características producido por {@link #extract}. */
    public static final int FEATURE_COUNT = 22;

    private FeatureExtractor() {
        // Clase de utilidad: no se instancia.
    }

    public static double[] extract(Connection c) {
        String protocol = c.getProtocolType();

        return new double[]{
                c.isLand() ? 1.0 : 0.0,
                c.isLoggedIn() ? 1.0 : 0.0,
                c.isRootShell() ? 1.0 : 0.0,
                c.isGuestLogin() ? 1.0 : 0.0,

                c.getSerrorRate(),
                c.getSrvSerrorRate(),
                c.getRerrorRate(),
                c.getSrvRerrorRate(),
                c.getSameSrvRate(),
                c.getDiffSrvRate(),
                c.getSrvDiffHostRate(),
                c.getDstHostSameSrvRate(),
                c.getDstHostDiffSrvRate(),
                c.getDstHostSameSrcPortRate(),
                c.getDstHostSrvDiffHostRate(),
                c.getDstHostSerrorRate(),
                c.getDstHostSrvSerrorRate(),
                c.getDstHostRerrorRate(),
                c.getDstHostSrvRerrorRate(),

                "tcp".equals(protocol) ? 1.0 : 0.0,
                "udp".equals(protocol) ? 1.0 : 0.0,
                "icmp".equals(protocol) ? 1.0 : 0.0
        };
    }

    /** Etiqueta numérica esperada por la red: 0.0 si es normal, 1.0 si es cualquier tipo de ataque. */
    public static double extractLabel(Connection c) {
        return c.isNormal() ? 0.0 : 1.0;
    }
}
