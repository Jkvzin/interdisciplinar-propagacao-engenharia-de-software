package model;

/**
 * Classe que modela os componentes fisicos de uma rede GPON.
 * 
 * <p>Representa os parametros tecnicos do enlace optico, incluindo
 * caracteristicas do transmissor, receptor, fibra e conectores.
 * Os valores padrao sao baseados em equipamentos GPON reais
 * em conformidade com as recomendacoes ITU-T G.984 e G.652.</p>
 * 
 * <h3>Valores padrao</h3>
 * <ul>
 *   <li>Potencia de transmissao: 3.0 dBm (OLT Classe B+)</li>
 *   <li>Sensibilidade: -28.0 dBm (ONU Classe B+)</li>
 *   <li>Perda de insercao: 0.5 dB (conector tipico)</li>
 *   <li>Atenuacao da fibra: 0.35 dB/km (G.652 @ 1490 nm)</li>
 *   <li>Margem de seguranca: 3.0 dB</li>
 * </ul>
 * 
 * @author Eduardo Tenorio Nunes
 * @version 1.0
 * @see Validador
 * @see LinkBudget
 */
public class Equipamento {

    /** Potencia de saida do transmissor optico (dBm). */
    private double potenciaTx;

    /** Sensibilidade do receptor optico (dBm). */
    private double sensibilidade;

    /** Perda por insercao de conectores/fusoes (dB). */
    private double perdaInsercao;

    /** Coeficiente de atenuacao da fibra optica (dB/km). */
    private double atenuacaoFibra;

    /** Margem de seguranca do enlace (dB). */
    private double margem;

    /**
     * Construtor com valores padrao baseados em equipamentos GPON reais.
     * 
     * <p>Utiliza os parametros tipicos de uma OLT Classe B+ com fibra G.652:</p>
     * <ul>
     *   <li>Potencia Tx: +3.0 dBm</li>
     *   <li>Sensibilidade: -28.0 dBm</li>
     *   <li>Perda de insercao: 0.5 dB</li>
     *   <li>Atenuacao da fibra: 0.35 dB/km</li>
     *   <li>Margem: 3.0 dB</li>
     * </ul>
     */
    public Equipamento() {
        this.potenciaTx = 3.0;
        this.sensibilidade = -28.0;
        this.perdaInsercao = 0.5;
        this.atenuacaoFibra = 0.35;
        this.margem = 3.0;
    }

    /**
     * Construtor parametrizado.
     * 
     * @param potenciaTx     potencia de transmissao (dBm)
     * @param sensibilidade  sensibilidade do receptor (dBm)
     * @param perdaInsercao  perda por conectores (dB)
     * @param atenuacaoFibra atenuacao da fibra (dB/km)
     * @param margem         margem de seguranca (dB)
     * @throws IllegalArgumentException se algum valor for invalido
     */
    public Equipamento(double potenciaTx, double sensibilidade, double perdaInsercao,
                       double atenuacaoFibra, double margem) {
        setPotenciaTx(potenciaTx);
        setSensibilidade(sensibilidade);
        setPerdaInsercao(perdaInsercao);
        setAtenuacaoFibra(atenuacaoFibra);
        setMargem(margem);
    }

    // ─── Getters ────────────────────────────────────────────────────────

    /**
     * @return potencia de transmissao (dBm)
     */
    public double getPotenciaTx() {
        return potenciaTx;
    }

    /**
     * @return sensibilidade do receptor (dBm)
     */
    public double getSensibilidade() {
        return sensibilidade;
    }

    /**
     * @return perda por insercao de conectores (dB)
     */
    public double getPerdaInsercao() {
        return perdaInsercao;
    }

    /**
     * @return coeficiente de atenuacao da fibra (dB/km)
     */
    public double getAtenuacaoFibra() {
        return atenuacaoFibra;
    }

    /**
     * @return margem de seguranca (dB)
     */
    public double getMargem() {
        return margem;
    }

    // ─── Setters com validacao ──────────────────────────────────────────

    /**
     * Define a potencia de transmissao.
     * 
     * @param potenciaTx potencia em dBm (deve estar entre -50 e +50 dBm)
     * @throws IllegalArgumentException se o valor estiver fora dos limites fisicos
     */
    public void setPotenciaTx(double potenciaTx) {
        if (potenciaTx < -50 || potenciaTx > 50) {
            throw new IllegalArgumentException(
                String.format("Potencia de transmissao (%.2f dBm) fora dos limites fisicos (-50 a +50 dBm).", potenciaTx)
            );
        }
        this.potenciaTx = potenciaTx;
    }

    /**
     * Define a sensibilidade do receptor.
     * 
     * @param sensibilidade sensibilidade em dBm (deve estar entre -50 e -10 dBm)
     * @throws IllegalArgumentException se o valor estiver fora dos limites fisicos
     */
    public void setSensibilidade(double sensibilidade) {
        if (sensibilidade < -50 || sensibilidade > -10) {
            throw new IllegalArgumentException(
                String.format("Sensibilidade (%.2f dBm) fora dos limites fisicos (-50 a -10 dBm).", sensibilidade)
            );
        }
        this.sensibilidade = sensibilidade;
    }

    /**
     * Define a perda por insercao de conectores/fusoes.
     * 
     * @param perdaInsercao perda em dB (nao pode ser negativa)
     * @throws IllegalArgumentException se o valor for negativo
     */
    public void setPerdaInsercao(double perdaInsercao) {
        if (perdaInsercao < 0) {
            throw new IllegalArgumentException(
                String.format("Perda de insercao (%.2f dB) nao pode ser negativa.", perdaInsercao)
            );
        }
        this.perdaInsercao = perdaInsercao;
    }

    /**
     * Define o coeficiente de atenuacao da fibra.
     * 
     * @param atenuacaoFibra atenuacao em dB/km (deve ser positiva e ≤ 10 dB/km)
     * @throws IllegalArgumentException se o valor for invalido
     */
    public void setAtenuacaoFibra(double atenuacaoFibra) {
        if (atenuacaoFibra <= 0 || atenuacaoFibra > 10) {
            throw new IllegalArgumentException(
                String.format("Atenuacao da fibra (%.2f dB/km) invalida. Deve ser positiva e ≤ 10 dB/km.", atenuacaoFibra)
            );
        }
        this.atenuacaoFibra = atenuacaoFibra;
    }

    /**
     * Define a margem de seguranca do enlace.
     * 
     * @param margem margem em dB (nao pode ser negativa)
     * @throws IllegalArgumentException se o valor for negativo
     */
    public void setMargem(double margem) {
        if (margem < 0) {
            throw new IllegalArgumentException(
                String.format("Margem de seguranca (%.2f dB) nao pode ser negativa.", margem)
            );
        }
        this.margem = margem;
    }

    /**
     * Retorna uma representacao em string do equipamento com seus parametros.
     * 
     * @return string formatada com todos os atributos
     */
    @Override
    public String toString() {
        return String.format(
            "Equipamento{potenciaTx=%.2f dBm, sensibilidade=%.2f dBm, perdaInsercao=%.2f dB, atenuacaoFibra=%.2f dB/km, margem=%.2f dB}",
            potenciaTx, sensibilidade, perdaInsercao, atenuacaoFibra, margem
        );
    }
}
