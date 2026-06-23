package controller;

import model.LinkBudget;
import model.Validador;
import java.util.*;

/**
 * Controlador que faz a ponte entre a interface grafica (View) e o motor
 * de calculo (Model). Responsavel por receber os parametros do usuario,
 * acionar o {@link LinkBudget}, validar os resultados contra os padroes
 * ITU-T G.984/G.652 e retornar um {@link ResultadoCalculo} consolidado.
 * 
 * <p>Fluxo: UI → Controlador → LinkBudget.calcular() + Validador → UI</p>
 * 
 * @author Joao Victor Borges Carvalho
 * @version 1.1
 * @see LinkBudget
 * @see Validador
 * @see ResultadoCalculo
 */
public class Controlador {

    private final LinkBudget linkBudget;
    private final Validador validador;

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
     * Cria um Controlador com novas instancias de {@link LinkBudget} e {@link Validador}.
     */
    public Controlador() {
        this.linkBudget = new LinkBudget();
        this.validador = new Validador();
    }

    /**
     * Cria um Controlador com instancias injetadas (util para testes).
     */
    public Controlador(LinkBudget linkBudget, Validador validador) {
        this.linkBudget = linkBudget;
        this.validador = validador;
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
        alertas.addAll(validarAtenuacaoTotal(parametros, variavelFaltante, valor));

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

    // ─── Validacao via Validador ────────────────────────────────────────

    /**
     * Valida os valores de entrada fornecidos pelo usuario usando o {@link Validador}.
     * Ignora a variavel que esta com valor {@code null} (a ser calculada).
     */
    private List<String> validarEntradas(Map<String, Double> parametros) {
        List<String> alertas = new ArrayList<>();

        Double ptx = parametros.get("Ptx");
        Double s   = parametros.get("S");
        Double d   = parametros.get("d");
        Double n   = parametros.get("N");
        Double pcon = parametros.get("Pcon");
        Double m   = parametros.get("M");

        if (ptx != null) {
            alertas.addAll(validador.validarPotenciaTx(ptx));
        }
        if (s != null) {
            alertas.addAll(validador.validarSensibilidade(s));
        }
        if (d != null) {
            alertas.addAll(validador.validarDistancia(d));
        }
        if (n != null) {
            alertas.addAll(validador.validarSplitter(n));
        }
        if (pcon != null) {
            alertas.addAll(validador.validarPerdaConectores(pcon));
        }
        if (m != null) {
            alertas.addAll(validador.validarMargem(m));
        }

        return alertas;
    }

    /**
     * Valida o valor calculado contra os padroes ITU-T usando o {@link Validador}.
     */
    private List<String> validarResultado(String variavel, double valor) {
        List<String> alertas = new ArrayList<>();

        if (Double.isNaN(valor)) return alertas; // ja tratado como erro

        switch (variavel) {
            case "Ptx":
                alertas.addAll(validador.validarPotenciaTx(valor));
                break;
            case "S":
                alertas.addAll(validador.validarSensibilidade(valor));
                break;
            case "d":
                alertas.addAll(validador.validarDistancia(valor));
                break;
            case "N":
                alertas.addAll(validador.validarSplitter(valor));
                break;
            case "Pcon":
                alertas.addAll(validador.validarPerdaConectores(valor));
                break;
            case "M":
                alertas.addAll(validador.validarMargem(valor));
                break;
        }

        return alertas;
    }

    /**
     * Valida a atenuacao total do enlace reconstruindo o cenario completo.
     * 
     * <p>Apos o calculo, temos todos os valores (a faltante foi preenchida).
     * Reconstruimos o cenario para calcular Ptx - S e validar contra os
     * limites de atenuacao da classe de operacao.</p>
     */
    private List<String> validarAtenuacaoTotal(Map<String, Double> parametros,
                                                String variavelFaltante, double valor) {
        List<String> alertas = new ArrayList<>();

        // Reconstroi o cenario completo com o valor calculado
        Map<String, Double> completo = new HashMap<>(parametros);
        completo.put(variavelFaltante, valor);

        Double ptx = completo.get("Ptx");
        Double s = completo.get("S");

        if (ptx != null && s != null) {
            double atenuacaoTotal = ptx - s;
            alertas.addAll(validador.validarAtenuacao(atenuacaoTotal, Validador.LIMITE_ATENUACAO_CPLUS));
        }

        return alertas;
    }
}
