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
 *   <li>Atenuacao 1490 nm (downstream): 0.28 dB/km (G.652)</li>
 *   <li>Atenuacao 1310 nm (upstream): 0.35 dB/km (G.652)</li>
 *   <li>Perda por conector: 0.5 dB</li>
 *   <li>Perda por fusao: 0.1 dB</li>
 *   <li>Margem de seguranca: 3.0 dB</li>
 * </ul>
 * 
 * @author Eduardo Tenorio Nunes
 * @version 1.1
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

    /** Coeficiente de atenuacao da fibra optica a 1490 nm (dB/km). */
    private double atenuacao1490;

    /** Coeficiente de atenuacao da fibra optica a 1310 nm (dB/km). */
    private double atenuacao1310;

    /** Perda por conector individual (dB). */
    private double perdaPorConector;

    /** Perda por fusao individual (dB). */
    private double perdaPorFusao;

    /** Margem de seguranca do enlace (dB). */
    private double margem;

    /**
     * Construtor com valores padrao baseados em equipamentos GPON reais.
     * 
     * <p>Utiliza os parametros tipicos de uma OLT Classe B+ com fibra G.652:</p>
     * <ul>
     *   <li>Potencia Tx: +3.0 dBm</li>
     *   <li>Sensibilidade: -28.0 dBm</li>
     *   <li>Atenuacao 1490 nm: 0.28 dB/km</li>
     *   <li>Atenuacao 1310 nm: 0.35 dB/km</li>
     *   <li>Perda por conector: 0.5 dB</li>
     *   <li>Perda por fusao: 0.1 dB</li>
     *   <li>Margem: 3.0 dB</li>
     * </ul>
     */
    public Equipamento() {
        this.potenciaTx = 3.0;
        this.sensibilidade = -28.0;
        this.perdaInsercao = 0.5;
        this.atenuacao1490 = 0.28;
        this.atenuacao1310 = 0.35;
        this.perdaPorConector = 0.5;
        this.perdaPorFusao = 0.1;
        this.margem = 3.0;
    }

    /**
     * Construtor parametrizado.
     * 
     * @param potenciaTx       potencia de transmissao (dBm)
     * @param sensibilidade    sensibilidade do receptor (dBm)
     * @param perdaInsercao    perda por conectores (dB)
     * @param atenuacao1490    atenuacao da fibra a 1490 nm (dB/km)
     * @param atenuacao1310    atenuacao da fibra a 1310 nm (dB/km)
     * @param perdaPorConector perda por conector individual (dB)
     * @param perdaPorFusao    perda por fusao individual (dB)
     * @param margem           margem de seguranca (dB)
     * @throws IllegalArgumentException se algum valor for invalido
     */
    public Equipamento(double potenciaTx, double sensibilidade, double perdaInsercao,
                       double atenuacao1490, double atenuacao1310,
                       double perdaPorConector, double perdaPorFusao, double margem) {
        setPotenciaTx(potenciaTx);
        setSensibilidade(sensibilidade);
        setPerdaInsercao(perdaInsercao);
        setAtenuacao1490(atenuacao1490);
        setAtenuacao1310(atenuacao1310);
        setPerdaPorConector(perdaPorConector);
        setPerdaPorFusao(perdaPorFusao);
        setMargem(margem);
    }

    // ─── Getters ────────────────────────────────────────────────────────

    public double getPotenciaTx() { return potenciaTx; }
    public double getSensibilidade() { return sensibilidade; }
    public double getPerdaInsercao() { return perdaInsercao; }

    /** @return atenuacao da fibra a 1490 nm (dB/km) */
    public double getAtenuacao1490() { return atenuacao1490; }

    /** @return atenuacao da fibra a 1310 nm (dB/km) */
    public double getAtenuacao1310() { return atenuacao1310; }

    /** @return perda por conector individual (dB) */
    public double getPerdaPorConector() { return perdaPorConector; }

    /** @return perda por fusao individual (dB) */
    public double getPerdaPorFusao() { return perdaPorFusao; }

    /** @return margem de seguranca (dB) */
    public double getMargem() { return margem; }

    /**
     * Retorna a atenuacao da fibra para o comprimento de onda especificado.
     * @param nm comprimento de onda em nanometros (1490 ou 1310)
     * @return atenuacao em dB/km
     */
    public double getAtenuacaoFibra(int nm) {
        return nm == 1310 ? atenuacao1310 : atenuacao1490;
    }

    // ─── Setters com validacao ──────────────────────────────────────────

    public void setPotenciaTx(double potenciaTx) {
        if (potenciaTx < -50 || potenciaTx > 50) {
            throw new IllegalArgumentException(
                String.format("Potencia de transmissao (%.2f dBm) fora dos limites fisicos (-50 a +50 dBm).", potenciaTx)
            );
        }
        this.potenciaTx = potenciaTx;
    }

    public void setSensibilidade(double sensibilidade) {
        if (sensibilidade < -50 || sensibilidade > -10) {
            throw new IllegalArgumentException(
                String.format("Sensibilidade (%.2f dBm) fora dos limites fisicos (-50 a -10 dBm).", sensibilidade)
            );
        }
        this.sensibilidade = sensibilidade;
    }

    public void setPerdaInsercao(double perdaInsercao) {
        if (perdaInsercao < 0) {
            throw new IllegalArgumentException(
                String.format("Perda de insercao (%.2f dB) nao pode ser negativa.", perdaInsercao)
            );
        }
        this.perdaInsercao = perdaInsercao;
    }

    public void setAtenuacao1490(double atenuacao1490) {
        if (atenuacao1490 <= 0 || atenuacao1490 > 10) {
            throw new IllegalArgumentException(
                String.format("Atenuacao 1490 nm (%.2f dB/km) invalida. Deve ser positiva e ≤ 10 dB/km.", atenuacao1490)
            );
        }
        this.atenuacao1490 = atenuacao1490;
    }

    public void setAtenuacao1310(double atenuacao1310) {
        if (atenuacao1310 <= 0 || atenuacao1310 > 10) {
            throw new IllegalArgumentException(
                String.format("Atenuacao 1310 nm (%.2f dB/km) invalida. Deve ser positiva e ≤ 10 dB/km.", atenuacao1310)
            );
        }
        this.atenuacao1310 = atenuacao1310;
    }

    public void setPerdaPorConector(double perdaPorConector) {
        if (perdaPorConector < 0 || perdaPorConector > 5) {
            throw new IllegalArgumentException(
                String.format("Perda por conector (%.2f dB) invalida. Deve estar entre 0 e 5 dB.", perdaPorConector)
            );
        }
        this.perdaPorConector = perdaPorConector;
    }

    public void setPerdaPorFusao(double perdaPorFusao) {
        if (perdaPorFusao < 0 || perdaPorFusao > 2) {
            throw new IllegalArgumentException(
                String.format("Perda por fusao (%.2f dB) invalida. Deve estar entre 0 e 2 dB.", perdaPorFusao)
            );
        }
        this.perdaPorFusao = perdaPorFusao;
    }

    public void setMargem(double margem) {
        if (margem < 0) {
            throw new IllegalArgumentException(
                String.format("Margem de seguranca (%.2f dB) nao pode ser negativa.", margem)
            );
        }
        this.margem = margem;
    }

    @Override
    public String toString() {
        return String.format(
            "Equipamento{Ptx=%.2f dBm, S=%.2f dBm, alpha1490=%.2f, alpha1310=%.2f dB/km, "
            + "perdaConector=%.2f dB, perdaFusao=%.2f dB, margem=%.2f dB}",
            potenciaTx, sensibilidade, atenuacao1490, atenuacao1310,
            perdaPorConector, perdaPorFusao, margem
        );
    }
}
