package controller;

import model.LinkBudget;
import model.ResultadoCalculo;
import model.Validador;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controlador que orquestra o fluxo de calculo e validacao do Link Budget GPON.
 * 
 * <p>Coordena a interacao entre {@link LinkBudget} (motor de calculo),
 * {@link Validador} (regras ITU-T) e a camada de apresentacao.</p>
 * 
 * <p>Fluxo principal:</p>
 * <ol>
 *   <li>Recebe o mapa de parametros com a variavel faltante ({@code null})</li>
 *   <li>Executa {@link LinkBudget#calcular(Map)} para obter o valor</li>
 *   <li>Executa as validacoes via {@link Validador} sobre os parametros e resultado</li>
 *   <li>Retorna {@link ResultadoCalculo} com valor, nome da variavel e alertas</li>
 * </ol>
 * 
 * @author Eduardo Tenorio Nunes
 * @version 1.0
 * @see LinkBudget
 * @see Validador
 * @see ResultadoCalculo
 */
public class Controlador {

    private final LinkBudget linkBudget;
    private final Validador validador;

    /**
     * Construtor padrao — inicializa com instancias novas de {@link LinkBudget} e {@link Validador}.
     */
    public Controlador() {
        this.linkBudget = new LinkBudget();
        this.validador = new Validador();
    }

    /**
     * Construtor para injecao de dependencias (util para testes).
     * 
     * @param linkBudget instancia do motor de calculo
     * @param validador  instancia do validador
     */
    public Controlador(LinkBudget linkBudget, Validador validador) {
        this.linkBudget = linkBudget;
        this.validador = validador;
    }

    /**
     * Processa o calculo completo: identifica a variavel faltante, calcula e valida.
     * 
     * <p>O mapa deve conter as 7 chaves do Link Budget com exatamente uma delas
     * com valor {@code null}.</p>
     * 
     * @param parametros mapa com os parametros do enlace
     * @return resultado contendo valor, nome da variavel e alertas
     * @throws IllegalArgumentException se o mapa for invalido (propagado do {@link LinkBudget})
     */
    public ResultadoCalculo processarCalculo(Map<String, Double> parametros) {
        // Identifica qual variavel esta faltando
        String faltante = null;
        for (Map.Entry<String, Double> entry : parametros.entrySet()) {
            if (entry.getValue() == null) {
                faltante = entry.getKey();
                break;
            }
        }

        // Executa o calculo
        double valor = linkBudget.calcular(parametros);

        // Gera alertas de validacao
        List<String> alertas = gerarAlertas(parametros, faltante, valor);

        return new ResultadoCalculo(valor, faltante, alertas);
    }

    /**
     * Retorna apenas os alertas de validacao para os parametros fornecidos,
     * sem executar o calculo. Util para validacao em tempo real na interface.
     * 
     * @param parametros mapa com os parametros do enlace (todos preenchidos)
     * @return lista de alertas (vazia se tudo OK)
     */
    public List<String> getAlertas(Map<String, Double> parametros) {
        List<String> alertas = new ArrayList<>();

        Double ptx = parametros.get("Ptx");
        Double s = parametros.get("S");
        Double d = parametros.get("d");
        Double n = parametros.get("N");
        Double pcon = parametros.get("Pcon");
        Double m = parametros.get("M");

        if (ptx != null) alertas.addAll(validador.validarPotenciaTx(ptx));
        if (s != null) alertas.addAll(validador.validarSensibilidade(s));
        if (d != null) alertas.addAll(validador.validarDistancia(d));
        if (n != null) alertas.addAll(validador.validarSplitter(n));
        if (pcon != null) alertas.addAll(validador.validarPerdaConectores(pcon));
        if (m != null) alertas.addAll(validador.validarMargem(m));

        return alertas;
    }

    /**
     * Gera alertas de validacao para o resultado do calculo e parametros de entrada.
     */
    private List<String> gerarAlertas(Map<String, Double> parametros, String faltante, double valor) {
        List<String> alertas = new ArrayList<>();

        // Cria uma copia dos parametros com o valor calculado inserido
        Map<String, Double> completo = new java.util.HashMap<>(parametros);
        completo.put(faltante, valor);

        Double ptx = completo.get("Ptx");
        Double s = completo.get("S");
        Double alpha = completo.get("alpha");
        Double d = completo.get("d");
        Double n = completo.get("N");
        Double pcon = completo.get("Pcon");
        Double m = completo.get("M");

        // Validacoes especificas por variavel
        if (ptx != null) alertas.addAll(validador.validarPotenciaTx(ptx));
        if (s != null) alertas.addAll(validador.validarSensibilidade(s));
        if (d != null) alertas.addAll(validador.validarDistancia(d));
        if (n != null) alertas.addAll(validador.validarSplitter(n));
        if (pcon != null) alertas.addAll(validador.validarPerdaConectores(pcon));
        if (m != null) alertas.addAll(validador.validarMargem(m));

        // Validacao de atenuacao total
        if (alpha != null && d != null && n != null && pcon != null && m != null) {
            double atenuacaoTotal = alpha * d + 10.0 * (Math.log(n) / Math.log(2)) + pcon + m;
            alertas.addAll(validador.validarAtenuacao(atenuacaoTotal, Validador.LIMITE_ATENUACAO_BPLUS));
        }

        return alertas;
    }
}
