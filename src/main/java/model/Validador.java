package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classe responsavel pela validacao de parametros de enlace GPON
 * contra os padroes ITU-T G.984 (GPON) e G.652 (fibra optica).
 * 
 * <p>Cada metodo de validacao recebe um ou mais parametros e retorna
 * uma lista de strings com os alertas encontrados. Se o parametro
 * estiver dentro dos limites, a lista retornada e vazia.</p>
 * 
 * <h3>Classes de operacao GPON suportadas</h3>
 * <ul>
 *   <li><b>Classe B+:</b> atenuacao maxima 28 dB, distancia tipica ate 20 km</li>
 *   <li><b>Classe C+:</b> atenuacao maxima 32 dB, distancia tipica ate 60 km</li>
 * </ul>
 * 
 * <p>Todas as constantes de limite sao baseadas nas recomendacoes ITU-T:</p>
 * <ul>
 *   <li>G.984.2 — Gigabit-capable Passive Optical Networks (GPON): Physical Media Dependent layer</li>
 *   <li>G.652 — Characteristics of a single-mode optical fibre and cable</li>
 * </ul>
 * 
 * @author Eduardo Tenorio Nunes
 * @version 1.0
 * @see Equipamento
 * @see LinkBudget
 */
public class Validador {

    // ─── Constantes ITU-T G.984 (GPON) ─────────────────────────────────

    /** Limite de atenuacao maxima para Classe B+ (dB). Fonte: ITU-T G.984.2. */
    public static final double LIMITE_ATENUACAO_BPLUS = 28.0;

    /** Limite de atenuacao maxima para Classe C+ (dB). Fonte: ITU-T G.984.2. */
    public static final double LIMITE_ATENUACAO_CPLUS = 32.0;

    /** Distancia maxima tipica para Classe B+ (km). */
    public static final double DISTANCIA_MAX_BPLUS = 20.0;

    /** Distancia maxima tipica para Classe C+ (km). */
    public static final double DISTANCIA_MAX_CPLUS = 60.0;

    /** Potencia de transmissao minima tipica para OLT GPON (dBm). */
    public static final double POTENCIA_TX_MIN = 1.5;

    /** Potencia de transmissao maxima tipica para OLT GPON (dBm). */
    public static final double POTENCIA_TX_MAX = 5.0;

    /** Limite de sensibilidade para Classe B+ (dBm). */
    public static final double SENSIBILIDADE_BPLUS = -28.0;

    /** Limite de sensibilidade para Classe C+ (dBm). */
    public static final double SENSIBILIDADE_CPLUS = -27.0;

    // ─── Constantes para splitter ──────────────────────────────────────

    /** Razao de divisao minima do splitter. */
    public static final double SPLITTER_MIN = 2.0;

    /** Razao de divisao maxima pratica do splitter. */
    public static final double SPLITTER_MAX = 256.0;

    /** Razao de divisao tipica recomendada para GPON. */
    public static final double SPLITTER_TIPICO = 64.0;

    // ─── Constantes para conectores ────────────────────────────────────

    /** Perda maxima aceitavel por conector individual (dB). */
    public static final double PERDA_CONECTOR_MAX = 1.0;

    /** Perda total maxima recomendada para conectores/fusoes (dB). */
    public static final double PERDA_TOTAL_CONECTORES_MAX = 3.0;

    // ─── Metodos de validacao ──────────────────────────────────────────

    /**
     * Valida se a potencia de transmissao esta dentro da faixa tipica GPON.
     * 
     * <p>Faixa esperada: +1.5 a +5.0 dBm (OLT Classe B+).</p>
     * 
     * @param dBm potencia de transmissao a validar
     * @return lista de alertas (vazia se dentro da faixa)
     */
    public List<String> validarPotenciaTx(double dBm) {
        List<String> alertas = new ArrayList<>();

        if (dBm < POTENCIA_TX_MIN) {
            alertas.add(String.format(
                "Potencia de transmissao (%.2f dBm) abaixo do minimo recomendado de %.1f dBm.",
                dBm, POTENCIA_TX_MIN
            ));
        }
        if (dBm > POTENCIA_TX_MAX) {
            alertas.add(String.format(
                "Potencia de transmissao (%.2f dBm) acima do maximo tipico de %.1f dBm para OLT GPON.",
                dBm, POTENCIA_TX_MAX
            ));
        }

        return alertas;
    }

    /**
     * Valida se a sensibilidade do receptor esta dentro dos limites GPON.
     * 
     * <p>Limiares:</p>
     * <ul>
     *   <li>Classe B+: -28 dBm</li>
     *   <li>Classe C+: -27 dBm</li>
     * </ul>
     * <p>Valores acima (menos negativos) que esses limites indicam receptor
     * insuficientemente sensivel para a classe de operacao.</p>
     * 
     * @param dBm sensibilidade do receptor a validar
     * @return lista de alertas (vazia se dentro dos limites)
     */
    public List<String> validarSensibilidade(double dBm) {
        List<String> alertas = new ArrayList<>();

        if (dBm > SENSIBILIDADE_BPLUS) {
            alertas.add(String.format(
                "Sensibilidade (%.2f dBm) acima do limite Classe B+ (%.0f dBm). "
                + "O receptor pode nao ter sensibilidade suficiente para operacao Classe B+.",
                dBm, SENSIBILIDADE_BPLUS
            ));
        }
        if (dBm > SENSIBILIDADE_CPLUS) {
            alertas.add(String.format(
                "Sensibilidade (%.2f dBm) acima do limite Classe C+ (%.0f dBm). "
                + "O receptor pode nao ter sensibilidade suficiente para operacao Classe C+.",
                dBm, SENSIBILIDADE_CPLUS
            ));
        }

        return alertas;
    }

    /**
     * Valida se a distancia do enlace esta dentro dos limites GPON.
     * 
     * <p>Verifica contra o limite da Classe B+ (20 km) e C+ (60 km).</p>
     * 
     * @param km distancia do enlace optico
     * @return lista de alertas (vazia se dentro dos limites)
     */
    public List<String> validarDistancia(double km) {
        List<String> alertas = new ArrayList<>();

        if (km < 0) {
            alertas.add(String.format(
                "Distancia (%.2f km) e negativa — valor fisicamente impossivel.", km
            ));
        }
        if (km > DISTANCIA_MAX_BPLUS) {
            alertas.add(String.format(
                "Distancia (%.2f km) excede o limite tipico de %.0f km para Classe B+.",
                km, DISTANCIA_MAX_BPLUS
            ));
        }
        if (km > DISTANCIA_MAX_CPLUS) {
            alertas.add(String.format(
                "Distancia (%.2f km) excede o limite maximo de %.0f km para Classe C+.",
                km, DISTANCIA_MAX_CPLUS
            ));
        }

        return alertas;
    }

    /**
     * Valida a atenuacao total do enlace contra um limite especificado.
     * 
     * <p>Exemplo de uso:</p>
     * <pre>
     *   validador.validarAtenuacao(atenuacaoTotal, Validador.LIMITE_ATENUACAO_BPLUS);
     * </pre>
     * 
     * @param atenuacaoTotal atenuacao total do enlace (dB)
     * @param limite         limite de atenuacao da classe de operacao (dB)
     * @return lista de alertas (vazia se dentro do limite)
     */
    public List<String> validarAtenuacao(double atenuacaoTotal, double limite) {
        List<String> alertas = new ArrayList<>();

        if (atenuacaoTotal < 0) {
            alertas.add(String.format(
                "Atenuacao total (%.2f dB) e negativa — valor fisicamente impossivel.", atenuacaoTotal
            ));
        }
        if (atenuacaoTotal > limite) {
            alertas.add(String.format(
                "Atenuacao total (%.2f dB) excede o limite de %.0f dB da classe de operacao. "
                + "O enlace pode nao funcionar corretamente.",
                atenuacaoTotal, limite
            ));
        }

        return alertas;
    }

    /**
     * Valida se a razao de divisao do splitter esta dentro dos limites praticos.
     * 
     * <p>Limites: minimo 1:2 (N=2), maximo pratico 1:256 (N=256).
     * Valores tipicos para GPON: 1:32, 1:64.</p>
     * 
     * @param n razao de divisao do splitter
     * @return lista de alertas (vazia se dentro dos limites)
     */
    public List<String> validarSplitter(double n) {
        List<String> alertas = new ArrayList<>();

        if (n < SPLITTER_MIN) {
            alertas.add(String.format(
                "Razao de divisao do splitter (%.1f) abaixo do minimo de %.0f (1:2).", n, SPLITTER_MIN
            ));
        }
        if (n > SPLITTER_MAX) {
            alertas.add(String.format(
                "Razao de divisao do splitter (%.1f) acima do maximo pratico de %.0f (1:256). "
                + "Atenuacao por divisao pode ser excessiva.",
                n, SPLITTER_MAX
            ));
        }
        if (n > SPLITTER_TIPICO) {
            alertas.add(String.format(
                "Razao de divisao do splitter (%.1f) acima do valor tipico recomendado de %.0f (1:64).",
                n, SPLITTER_TIPICO
            ));
        }

        return alertas;
    }

    /**
     * Valida se a perda total por conectores/fusoes esta dentro dos limites aceitaveis.
     * 
     * <p>Limite recomendado: ate 3.0 dB total (aproximadamente 6 conectores a 0.5 dB cada).</p>
     * 
     * @param perdaTotal perda total por conectores/fusoes (dB)
     * @return lista de alertas (vazia se dentro dos limites)
     */
    public List<String> validarPerdaConectores(double perdaTotal) {
        List<String> alertas = new ArrayList<>();

        if (perdaTotal < 0) {
            alertas.add(String.format(
                "Perda por conectores (%.2f dB) e negativa — valor fisicamente impossivel.", perdaTotal
            ));
        }
        if (perdaTotal > PERDA_TOTAL_CONECTORES_MAX) {
            alertas.add(String.format(
                "Perda total por conectores (%.2f dB) excede o maximo recomendado de %.1f dB. "
                + "Verifique o numero de conexoes/fusoes no enlace.",
                perdaTotal, PERDA_TOTAL_CONECTORES_MAX
            ));
        }

        return alertas;
    }

    /**
     * Valida se a margem de seguranca do enlace e adequada.
     * 
     * <p>A margem deve ser positiva para garantir operacao confiavel.
     * O valor recomendado e de pelo menos 3 dB para compensar
     * variacoes de temperatura, envelhecimento e reparos.</p>
     * 
     * @param margem margem de seguranca (dB)
     * @return lista de alertas (vazia se adequada)
     */
    public List<String> validarMargem(double margem) {
        List<String> alertas = new ArrayList<>();

        if (margem < 0) {
            alertas.add(String.format(
                "Margem de seguranca (%.2f dB) e negativa — o enlace e inviavel.", margem
            ));
        }
        if (margem >= 0 && margem < 3.0) {
            alertas.add(String.format(
                "Margem de seguranca (%.2f dB) abaixo do recomendado de 3.0 dB. "
                + "O enlace pode sofrer degradacao com variacoes ambientais.",
                margem
            ));
        }

        return alertas;
    }
}
