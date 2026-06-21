package model;

import java.util.Map;

/**
 * Classe responsavel pelo calculo do Link Budget GPON.
 * 
 * <p>A equacao fundamental e:</p>
 * <pre>Ptx - S = α × d + 10·log₂(N) + Pcon + M</pre>
 * 
 * <p>Onde:</p>
 * <ul>
 *   <li><b>Ptx</b> — Potencia de transmissao (dBm)</li>
 *   <li><b>S</b> — Sensibilidade do receptor (dBm)</li>
 *   <li><b>α (alpha)</b> — Atenuacao da fibra (dB/km)</li>
 *   <li><b>d</b> — Distancia do enlace (km)</li>
 *   <li><b>N</b> — Razao de divisao do splitter (ex: 8 para 1:8)</li>
 *   <li><b>Pcon</b> — Perda total por conectores/fusoes (dB)</li>
 *   <li><b>M</b> — Margem de seguranca (dB)</li>
 * </ul>
 * 
 * <p>O metodo {@link #calcular(Map)} recebe um mapa com as variaveis preenchidas
 * e a faltante com valor {@code null}. O sistema identifica automaticamente qual
 * variavel esta ausente, isola-a algebricamente e retorna o valor calculado.</p>
 * 
 * <p>Se mais de uma variavel estiver ausente, ou se o resultado calculado for
 * fisicamente impossivel (ex: distancia negativa), uma excecao com mensagem
 * amigavel e lancada.</p>
 * 
 * @author Joao Guilherme Garcia Mangueira
 * @version 1.0
 * @see IllegalArgumentException
 */
public class LinkBudget {

    /**
     * Calcula a variavel faltante a partir das demais fornecidas no mapa.
     * 
     * <p>O mapa deve conter exatamente as chaves {@code "Ptx"}, {@code "S"},
     * {@code "alpha"}, {@code "d"}, {@code "N"}, {@code "Pcon"} e {@code "M"}.
     * Exatamente uma delas deve ter valor {@code null} — esta sera a variavel
     * a ser calculada. Todas as demais devem ser nao-nulas.</p>
     * 
     * @param parametros mapa com as 7 variaveis do Link Budget; a faltante e {@code null}
     * @return o valor calculado da variavel faltante
     * @throws IllegalArgumentException se nenhuma variavel estiver faltando,
     *         se mais de uma estiver faltando, se o mapa estiver incompleto,
     *         ou se o resultado for fisicamente impossivel
     * @throws NullPointerException se {@code parametros} for {@code null}
     */
    public double calcular(Map<String, Double> parametros) {
        if (parametros == null) {
            throw new NullPointerException("O mapa de parametros nao pode ser nulo.");
        }

        validarChaves(parametros);

        String faltante = null;
        int faltantes = 0;

        for (Map.Entry<String, Double> entry : parametros.entrySet()) {
            if (entry.getValue() == null) {
                faltante = entry.getKey();
                faltantes++;
            }
        }

        if (faltantes == 0) {
            throw new IllegalArgumentException(
                "Nenhuma variavel faltante encontrada. Deixe exatamente um campo em branco para calcular."
            );
        }

        if (faltantes > 1) {
            throw new IllegalArgumentException(
                "Mais de uma variavel esta faltando (" + faltantes + " campos vazios). "
                + "Preencha todos os campos exceto aquele que deseja calcular."
            );
        }

        double resultado = calcularPorVariavel(faltante, parametros);

        if (Double.isNaN(resultado) || Double.isInfinite(resultado)) {
            throw new IllegalArgumentException(
                "O calculo resultou em um valor invalido (" + resultado + "). "
                + "Verifique se os parametros fornecidos estao corretos."
            );
        }

        validarResultado(faltante, resultado);

        return resultado;
    }

    /**
     * Valida que o mapa contem exatamente as 7 chaves esperadas.
     */
    private void validarChaves(Map<String, Double> parametros) {
        String[] chavesEsperadas = {"Ptx", "S", "alpha", "d", "N", "Pcon", "M"};
        for (String chave : chavesEsperadas) {
            if (!parametros.containsKey(chave)) {
                throw new IllegalArgumentException(
                    "Chave obrigatoria ausente no mapa: \"" + chave + "\". "
                    + "O mapa deve conter: Ptx, S, alpha, d, N, Pcon, M."
                );
            }
        }
    }

    /**
     * Encaminha para o metodo privado correspondente a variavel faltante.
     */
    private double calcularPorVariavel(String faltante, Map<String, Double> p) {
        switch (faltante) {
            case "Ptx":   return calcularPotenciaTx(p.get("S"), p.get("alpha"), p.get("d"), p.get("N"), p.get("Pcon"), p.get("M"));
            case "S":     return calcularSensibilidade(p.get("Ptx"), p.get("alpha"), p.get("d"), p.get("N"), p.get("Pcon"), p.get("M"));
            case "alpha": return calcularAtenuacao(p.get("Ptx"), p.get("S"), p.get("d"), p.get("N"), p.get("Pcon"), p.get("M"));
            case "d":     return calcularDistancia(p.get("Ptx"), p.get("S"), p.get("alpha"), p.get("N"), p.get("Pcon"), p.get("M"));
            case "N":     return calcularDivisaoSplitter(p.get("Ptx"), p.get("S"), p.get("alpha"), p.get("d"), p.get("Pcon"), p.get("M"));
            case "Pcon":  return calcularPerdaConectores(p.get("Ptx"), p.get("S"), p.get("alpha"), p.get("d"), p.get("N"), p.get("M"));
            case "M":     return calcularMargem(p.get("Ptx"), p.get("S"), p.get("alpha"), p.get("d"), p.get("N"), p.get("Pcon"));
            default:
                throw new IllegalArgumentException("Variavel desconhecida: \"" + faltante + "\".");
        }
    }

    /**
     * Valida se o resultado e fisicamente possivel para a variavel em questao.
     */
    private void validarResultado(String variavel, double valor) {
        switch (variavel) {
            case "Ptx":
                if (valor > 50 || valor < -50) {
                    throw new IllegalArgumentException(
                        "Potencia de transmissao calculada (" + String.format("%.2f", valor) 
                        + " dBm) esta fora dos limites fisicos razoaveis (-50 a +50 dBm)."
                    );
                }
                break;
            case "S":
                if (valor > -10 || valor < -50) {
                    throw new IllegalArgumentException(
                        "Sensibilidade calculada (" + String.format("%.2f", valor) 
                        + " dBm) esta fora dos limites fisicos razoaveis (-50 a -10 dBm)."
                    );
                }
                break;
            case "alpha":
                if (valor <= 0 || valor > 10) {
                    throw new IllegalArgumentException(
                        "Atenuacao calculada (" + String.format("%.2f", valor) 
                        + " dB/km) e fisicamente impossivel. Deve ser positiva e ≤ 10 dB/km."
                    );
                }
                break;
            case "d":
                if (valor < 0) {
                    throw new IllegalArgumentException(
                        "Distancia calculada (" + String.format("%.2f", valor) 
                        + " km) e negativa — fisicamente impossivel."
                    );
                }
                if (valor > 120) {
                    throw new IllegalArgumentException(
                        "Distancia calculada (" + String.format("%.2f", valor) 
                        + " km) excede o limite maximo teorico de 120 km para GPON."
                    );
                }
                break;
            case "N":
                if (valor < 2) {
                    throw new IllegalArgumentException(
                        "Razao de divisao calculada (" + String.format("%.2f", valor) 
                        + ") e invalida. O splitter minimo e 1:2 (N=2)."
                    );
                }
                if (valor > 256) {
                    throw new IllegalArgumentException(
                        "Razao de divisao calculada (" + String.format("%.2f", valor) 
                        + ") e invalida. O splitter maximo pratico e 1:256 (N=256)."
                    );
                }
                break;
            case "Pcon":
                if (valor < 0) {
                    throw new IllegalArgumentException(
                        "Perda por conectores calculada (" + String.format("%.2f", valor) 
                        + " dB) e negativa — fisicamente impossivel."
                    );
                }
                break;
            case "M":
                if (valor < 0) {
                    throw new IllegalArgumentException(
                        "Margem de seguranca calculada (" + String.format("%.2f", valor) 
                        + " dB) e negativa — o enlace e inviavel."
                    );
                }
                break;
        }
    }

    // ─── Metodos auxiliares privados ───────────────────────────────────────

    /**
     * Calcula a perda por divisao do splitter: 10 * log2(N).
     * 
     * @param N razao de divisao do splitter
     * @return perda em dB
     */
    private double perdaSplitter(double N) {
        return 10.0 * (Math.log(N) / Math.log(2));
    }

    /**
     * Calcula a potencia de transmissao (Ptx) a partir das demais variaveis.
     * <p>Formula: Ptx = S + α·d + 10·log₂(N) + Pcon + M</p>
     */
    private double calcularPotenciaTx(double S, double alpha, double d, double N, double Pcon, double M) {
        return S + alpha * d + perdaSplitter(N) + Pcon + M;
    }

    /**
     * Calcula a sensibilidade do receptor (S) a partir das demais variaveis.
     * <p>Formula: S = Ptx − (α·d + 10·log₂(N) + Pcon + M)</p>
     */
    private double calcularSensibilidade(double Ptx, double alpha, double d, double N, double Pcon, double M) {
        return Ptx - (alpha * d + perdaSplitter(N) + Pcon + M);
    }

    /**
     * Calcula a atenuacao da fibra (alpha) a partir das demais variaveis.
     * <p>Formula: α = (Ptx − S − 10·log₂(N) − Pcon − M) / d</p>
     */
    private double calcularAtenuacao(double Ptx, double S, double d, double N, double Pcon, double M) {
        return (Ptx - S - perdaSplitter(N) - Pcon - M) / d;
    }

    /**
     * Calcula a distancia do enlace (d) a partir das demais variaveis.
     * <p>Formula: d = (Ptx − S − 10·log₂(N) − Pcon − M) / α</p>
     */
    private double calcularDistancia(double Ptx, double S, double alpha, double N, double Pcon, double M) {
        return (Ptx - S - perdaSplitter(N) - Pcon - M) / alpha;
    }

    /**
     * Calcula a razao de divisao do splitter (N) a partir das demais variaveis.
     * <p>Primeiro calcula a perda do splitter: perda = Ptx − S − α·d − Pcon − M</p>
     * <p>Depois isola N: N = 2^(perda / 10)</p>
     */
    private double calcularDivisaoSplitter(double Ptx, double S, double alpha, double d, double Pcon, double M) {
        double perda = Ptx - S - alpha * d - Pcon - M;
        return Math.pow(2.0, perda / 10.0);
    }

    /**
     * Calcula a perda por conectores/fusoes (Pcon) a partir das demais variaveis.
     * <p>Formula: Pcon = Ptx − S − α·d − 10·log₂(N) − M</p>
     */
    private double calcularPerdaConectores(double Ptx, double S, double alpha, double d, double N, double M) {
        return Ptx - S - alpha * d - perdaSplitter(N) - M;
    }

    /**
     * Calcula a margem de seguranca (M) a partir das demais variaveis.
     * <p>Formula: M = Ptx − S − α·d − 10·log₂(N) − Pcon</p>
     */
    private double calcularMargem(double Ptx, double S, double alpha, double d, double N, double Pcon) {
        return Ptx - S - alpha * d - perdaSplitter(N) - Pcon;
    }
}
