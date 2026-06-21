package controller;

import model.LinkBudget;
import java.util.*;

/**
 * Resultado de um calculo de Link Budget GPON, contendo o valor calculado,
 * a variavel que foi determinada e a lista de alertas de validacao.
 * 
 * @author Joao Victor Borges Carvalho
 * @version 1.0
 */
public class ResultadoCalculo {

    private final double valor;
    private final String variavel;
    private final List<String> alertas;

    /**
     * Cria um resultado de calculo.
     * 
     * @param valor    valor calculado da variavel faltante
     * @param variavel nome da variavel que foi calculada (Ptx, S, alpha, d, N, Pcon, M)
     * @param alertas  lista de alertas de validacao (vazia se tudo OK)
     */
    public ResultadoCalculo(double valor, String variavel, List<String> alertas) {
        this.valor = valor;
        this.variavel = variavel;
        this.alertas = Collections.unmodifiableList(new ArrayList<>(alertas));
    }

    /** @return valor calculado da variavel faltante */
    public double getValor() {
        return valor;
    }

    /** @return nome da variavel que foi calculada */
    public String getVariavel() {
        return variavel;
    }

    /** @return lista imutavel de alertas (vazia se nenhum problema) */
    public List<String> getAlertas() {
        return alertas;
    }

    /** @return true se ha pelo menos um alerta */
    public boolean temAlertas() {
        return !alertas.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ResultadoCalculo{%s = %.2f, alertas=%d}",
                variavel, valor, alertas.size());
    }
}
