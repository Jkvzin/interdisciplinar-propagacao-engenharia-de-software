package model;

import java.util.Collections;
import java.util.List;

/**
 * Classe que encapsula o resultado de um calculo de Link Budget.
 * 
 * <p>Contem o valor calculado, o nome da variavel que foi calculada
 * e a lista de alertas gerados pelo {@link Validador}.</p>
 * 
 * @author Eduardo Tenorio Nunes
 * @version 1.0
 * @see Controlador
 * @see CalculadoraGUI
 */
public class ResultadoCalculo {

    private final double valor;
    private final String variavel;
    private final List<String> alertas;

    /**
     * Construtor completo.
     * 
     * @param valor    valor calculado da variavel faltante
     * @param variavel nome da variavel que foi calculada (ex: "Ptx", "d")
     * @param alertas  lista de alertas de validacao (pode ser vazia, nunca nula)
     * @throws NullPointerException se {@code alertas} for {@code null}
     */
    public ResultadoCalculo(double valor, String variavel, List<String> alertas) {
        if (alertas == null) {
            throw new NullPointerException("A lista de alertas nao pode ser nula.");
        }
        this.valor = valor;
        this.variavel = variavel;
        this.alertas = Collections.unmodifiableList(alertas);
    }

    /**
     * @return valor calculado da variavel faltante
     */
    public double getValor() {
        return valor;
    }

    /**
     * @return nome da variavel que foi calculada
     */
    public String getVariavel() {
        return variavel;
    }

    /**
     * @return lista imutavel de alertas de validacao (vazia se tudo OK)
     */
    public List<String> getAlertas() {
        return alertas;
    }

    /**
     * @return {@code true} se existem alertas de validacao
     */
    public boolean temAlertas() {
        return !alertas.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ResultadoCalculo{variavel='%s', valor=%.2f, alertas=%d}",
                variavel, valor, alertas.size());
    }
}
