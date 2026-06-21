package controller;

import model.LinkBudget;
import java.util.*;

/**
 * Controlador que faz a ponte entre a interface grafica (View) e o motor
 * de calculo (Model). Responsavel por receber os parametros do usuario,
 * acionar o {@link LinkBudget}, validar os resultados contra os padroes
 * ITU-T G.984/G.652 e retornar um {@link ResultadoCalculo} consolidado.
 * 
 * <p>Fluxo: UI → Controlador → LinkBudget.calcular() + validacao → UI</p>
 * 
 * <p>Quando a classe {@code Validador} estiver implementada (issue #2),
 * a validacao inline nos metodos {@code validar*} deve ser substituida
 * por chamadas ao Validador.</p>
 * 
 * @author Joao Victor Borges Carvalho
 * @version 1.0
 * @see LinkBudget
 * @see ResultadoCalculo
 */
public class Controlador {

    private final LinkBudget linkBudget;

    // TODO: substituir por Validador quando disponivel (issue #2)
    // private final Validador validador;

    /** Limites ITU-T G.984 para validacao */
    private static final double PTX_MIN = 1.5;       // dBm
    private static final double PTX_MAX = 5.0;       // dBm
    private static final double S_MAX = -28.0;       // dBm (Classe B+)
    private static final double D_MAX = 20.0;        // km (Classe B+)
    private static final double ATENUACAO_MAX = 28.0; // dB (Classe B+)

    /** Nomes amigaveis das variaveis para exibicao */
    private static final Map<String, String> NOMES_VARIAVEIS = new LinkedHashMap<>();
    static {
        NOMES_VARIAVEIS.put("Ptx",   "Potencia de Transmissao");
        NOMES_VARIAVEIS.put("S",     "Sensibilidade do Receptor");
        NOMES_VARIAVEIS.put("alpha", "Atenuacao da Fibra");
        NOMES_VARIAVEIS.put("d",     "Distancia do Enlace");
        NOMES_VARIAVEIS.put("N",     "Razao de Divisao do Splitter");
        NOMES_VARIAVEIS.put("Pcon",  "Perda por Conectores/Fusoes");
        NOMES_VARIAVEIS.put("M",     "Margem de Seguranca");
    }

    /**
     * Cria um Controlador com uma nova instancia de {@link LinkBudget}.
     */
    public Controlador() {
        this.linkBudget = new LinkBudget();
    }

    /**
     * Cria um Controlador com uma instancia injetada de LinkBudget
     * (util para testes).
     */
    public Controlador(LinkBudget linkBudget) {
        this.linkBudget = linkBudget;
    }

    /**
     * Processa o calculo do Link Budget a partir dos parametros fornecidos.
     * 
     * <p>O mapa deve conter as chaves {@code Ptx, S, alpha, d, N, Pcon, M}.
     * Exatamente uma delas deve ter valor {@code null} — esta sera calculada.</p>
     * 
     * @param parametros mapa com as variaveis do Link Budget; a faltante e {@code null}
     * @return resultado com valor calculado, nome da variavel e lista de alertas
     * @throws IllegalArgumentException se o mapa for nulo
     */
    public ResultadoCalculo processarCalculo(Map<String, Double> parametros) {
        if (parametros == null) {
            throw new IllegalArgumentException("O mapa de parametros nao pode ser nulo.");
        }

        double valor;
        String variavelFaltante;

        try {
            valor = linkBudget.calcular(parametros);
            variavelFaltante = identificarFaltante(parametros);
        } catch (IllegalArgumentException e) {
            // Erro amigavel: retorna resultado vazio com a mensagem como alerta
            return new ResultadoCalculo(
                Double.NaN,
                "ERRO",
                Collections.singletonList(e.getMessage())
            );
        }

        List<String> alertas = new ArrayList<>();
        alertas.addAll(validarEntradas(parametros));
        alertas.addAll(validarResultado(variavelFaltante, valor));

        // Se tudo OK, adiciona mensagem verde
        if (alertas.isEmpty()) {
            alertas.add("Todos os parametros dentro dos padroes ITU-T G.984");
        }

        return new ResultadoCalculo(valor, variavelFaltante, alertas);
    }

    /**
     * Identifica qual variavel esta com valor {@code null} no mapa.
     */
    private String identificarFaltante(Map<String, Double> parametros) {
        for (Map.Entry<String, Double> entry : parametros.entrySet()) {
            if (entry.getValue() == null) {
                return entry.getKey();
            }
        }
        return "DESCONHECIDO";
    }

    /**
     * Retorna o nome amigavel de uma variavel para exibicao na interface.
     */
    public static String nomeAmigavel(String chave) {
        return NOMES_VARIAVEIS.getOrDefault(chave, chave);
    }

    /**
     * Retorna a unidade de medida de uma variavel.
     */
    public static String unidade(String chave) {
        switch (chave) {
            case "Ptx": case "S": case "Pcon": case "M":
                return "dBm";
            case "alpha":
                return "dB/km";
            case "d":
                return "km";
            case "N":
                return "";
            default:
                return "";
        }
    }

    // ─── Validacao inline (TODO: migrar para classe Validador) ──────────

    /**
     * Valida os valores de entrada fornecidos pelo usuario.
     * Ignora a variavel que esta com valor {@code null} (a ser calculada).
     */
    private List<String> validarEntradas(Map<String, Double> parametros) {
        List<String> alertas = new ArrayList<>();

        Double ptx = parametros.get("Ptx");
        Double s   = parametros.get("S");
        Double d   = parametros.get("d");

        if (ptx != null && (ptx < PTX_MIN || ptx > PTX_MAX)) {
            alertas.add(String.format(
                "Potencia de transmissao (%.1f dBm) fora do padrao GPON (+%.1f a +%.1f dBm)",
                ptx, PTX_MIN, PTX_MAX));
        }

        if (s != null && s > S_MAX) {
            alertas.add(String.format(
                "Sensibilidade do receptor (%.1f dBm) acima do limite Classe B+ (%.1f dBm)",
                s, S_MAX));
        }

        if (d != null && d > D_MAX) {
            alertas.add(String.format(
                "Distancia (%.1f km) excede o limite tipico GPON de %.0f km (Classe B+)",
                d, D_MAX));
        }

        return alertas;
    }

    /**
     * Valida o valor calculado contra os padroes ITU-T.
     */
    private List<String> validarResultado(String variavel, double valor) {
        List<String> alertas = new ArrayList<>();

        if (Double.isNaN(valor)) return alertas; // ja tratado como erro

        switch (variavel) {
            case "Ptx":
                if (valor < PTX_MIN || valor > PTX_MAX) {
                    alertas.add(String.format(
                        "Potencia de transmissao calculada (%.1f dBm) fora do padrao GPON (+%.1f a +%.1f dBm)",
                        valor, PTX_MIN, PTX_MAX));
                }
                break;
            case "S":
                if (valor > S_MAX) {
                    alertas.add(String.format(
                        "Sensibilidade calculada (%.1f dBm) acima do limite Classe B+ (%.1f dBm)",
                        valor, S_MAX));
                }
                break;
            case "d":
                if (valor > D_MAX) {
                    alertas.add(String.format(
                        "Distancia calculada (%.1f km) excede o limite tipico de %.0f km",
                        valor, D_MAX));
                }
                break;
            case "M":
                if (valor < 0) {
                    alertas.add(String.format(
                        "Margem de seguranca negativa (%.1f dB) — o enlace e inviavel",
                        valor));
                }
                break;
        }

        // Validacao de atenuacao total para qualquer calculo
        // Ptx - S = atenuacao total
        // So podemos verificar se temos ambos
        // Como so uma variavel e calculada, nao temos o quadro completo aqui.
        // Essa validacao sera feita pela GUI apos montar o cenario completo.

        return alertas;
    }
}
