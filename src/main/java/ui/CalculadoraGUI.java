package ui;

import controller.Controlador;
import model.Equipamento;
import model.ResultadoCalculo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface grafica da Calculadora de Link Budget GPON.
 * 
 * <p>Implementada com Java Swing, oferece campos numericos com validacao
 * em tempo real, dropdown para selecao do splitter e areas de resultado
 * e alertas com rolagem.</p>
 * 
 * <p>Fluxo de uso:</p>
 * <ol>
 *   <li>Preencha todos os campos exceto aquele que deseja calcular</li>
 *   <li>Clique em "Calcular"</li>
 *   <li>O resultado aparece na area destacada</li>
 *   <li>Alertas de validacao ITU-T aparecem abaixo, se houver</li>
 * </ol>
 * 
 * @author Eduardo Tenorio Nunes
 * @version 1.0
 * @see Controlador
 * @see LinkBudget
 * @see Validador
 */
public class CalculadoraGUI extends JFrame {

    private final Controlador controlador;
    private final Equipamento equipamento;

    // Componentes da interface
    private final Map<String, JTextField> campos;
    private JComboBox<String> comboSplitter;
    private final JTextArea areaResultado;
    private final JTextArea areaAlertas;
    private final JButton btnCalcular;

    // Valores do splitter correspondentes aos itens do combo
    private static final String[] SPLITTER_LABELS = {
        "1:2", "1:4", "1:8", "1:16", "1:32", "1:64"
    };
    private static final double[] SPLITTER_VALUES = {
        2.0, 4.0, 8.0, 16.0, 32.0, 64.0
    };

    /**
     * Construtor — inicializa o controlador, equipamento e constroi a interface.
     */
    public CalculadoraGUI() {
        this.controlador = new Controlador();
        this.equipamento = new Equipamento();
        this.campos = new HashMap<>();

        setTitle("Calculadora de Link Budget — GPON");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        // Painel principal
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(new EmptyBorder(15, 15, 15, 15));
        painelPrincipal.setBackground(new Color(245, 245, 250));

        // Painel de entrada
        JPanel painelEntrada = criarPainelEntrada();
        painelPrincipal.add(painelEntrada, BorderLayout.NORTH);

        // Botao Calcular
        btnCalcular = new JButton("Calcular");
        btnCalcular.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnCalcular.setBackground(new Color(70, 130, 180));
        btnCalcular.setForeground(Color.WHITE);
        btnCalcular.setFocusPainted(false);
        btnCalcular.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCalcular.addActionListener(e -> onCalcular());

        JPanel painelBotao = new JPanel(new FlowLayout(FlowLayout.CENTER));
        painelBotao.setOpaque(false);
        painelBotao.add(btnCalcular);
        painelPrincipal.add(painelBotao, BorderLayout.CENTER);

        // Painel de resultado e alertas
        JPanel painelSaida = criarPainelSaida();
        painelPrincipal.add(painelSaida, BorderLayout.SOUTH);

        // Area de resultado
        areaResultado = new JTextArea(3, 40);
        areaResultado.setEditable(false);
        areaResultado.setFont(new Font("Segoe UI", Font.BOLD, 14));
        areaResultado.setBackground(new Color(230, 255, 230));
        areaResultado.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("Resultado"),
            new EmptyBorder(5, 8, 5, 8)
        ));
        areaResultado.setLineWrap(true);
        areaResultado.setWrapStyleWord(true);

        // Area de alertas
        areaAlertas = new JTextArea(10, 40);
        areaAlertas.setEditable(false);
        areaAlertas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        areaAlertas.setBackground(new Color(255, 245, 230));
        areaAlertas.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("Alertas / Avisos"),
            new EmptyBorder(5, 8, 5, 8)
        ));
        areaAlertas.setLineWrap(true);
        areaAlertas.setWrapStyleWord(true);

        JScrollPane scrollAlertas = new JScrollPane(areaAlertas);
        scrollAlertas.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollAlertas.setPreferredSize(new Dimension(500, 180));

        JPanel painelResultado = new JPanel(new BorderLayout(5, 10));
        painelResultado.setOpaque(false);
        painelResultado.add(areaResultado, BorderLayout.NORTH);
        painelResultado.add(scrollAlertas, BorderLayout.CENTER);
        painelSaida.add(painelResultado, BorderLayout.CENTER);

        add(painelPrincipal);
    }

    /**
     * Cria o painel de entrada com os 6 campos numericos e o dropdown do splitter.
     */
    private JPanel criarPainelEntrada() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 5, 4, 5);

        // Linha 0: Potencia de Transmissao
        adicionarCampo(painel, gbc, 0, "Potencia de Transmissao (dBm):", "Ptx", "");

        // Linha 1: Sensibilidade do Receptor
        adicionarCampo(painel, gbc, 1, "Sensibilidade do Receptor (dBm):", "S", "");

        // Linha 2: Distancia
        adicionarCampo(painel, gbc, 2, "Distancia (km):", "d", "");

        // Linha 3: Divisao do Splitter (dropdown)
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel lblSplitter = new JLabel("Divisao do Splitter:");
        lblSplitter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        painel.add(lblSplitter, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        comboSplitter = new JComboBox<>(SPLITTER_LABELS);
        comboSplitter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboSplitter.setSelectedIndex(3); // 1:16 como padrao
        painel.add(comboSplitter, gbc);

        // Linha 4: Perda por Conectores
        adicionarCampo(painel, gbc, 4, "Perda por Conectores (dB):", "Pcon", "");

        // Linha 5: Margem de Seguranca (padrao 3 dB)
        adicionarCampo(painel, gbc, 5, "Margem de Seguranca (dB):", "M",
                String.valueOf(equipamento.getMargem()));

        return painel;
    }

    /**
     * Adiciona um campo rotulado ao painel com validacao numerica.
     */
    private void adicionarCampo(JPanel painel, GridBagConstraints gbc, int linha,
                                String rotulo, String chave, String valorPadrao) {
        gbc.gridy = linha;

        // Rotulo
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel label = new JLabel(rotulo);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        painel.add(label, gbc);

        // Campo de texto
        gbc.gridx = 1;
        gbc.weightx = 1;
        JTextField campo = new JTextField(valorPadrao, 15);
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setHorizontalAlignment(JTextField.RIGHT);

        // Filtro para aceitar apenas numeros, ponto decimal e sinal negativo
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new FiltroNumerico());
        campos.put(chave, campo);
        painel.add(campo, gbc);
    }

    /**
     * Cria o painel de saida com areas de resultado e alertas.
     */
    private JPanel criarPainelSaida() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setOpaque(false);
        return painel;
    }

    /**
     * Manipulador do botao "Calcular".
     * 
     * <p>Identifica o campo vazio, constroi o mapa de parametros,
     * executa o calculo via {@link Controlador} e exibe os resultados.</p>
     */
    private void onCalcular() {
        // Limpa resultados anteriores
        areaResultado.setText("");
        areaAlertas.setText("");

        // Coleta valores dos campos
        Map<String, String> textoCampos = new HashMap<>();
        textoCampos.put("Ptx", campos.get("Ptx").getText().trim());
        textoCampos.put("S", campos.get("S").getText().trim());
        textoCampos.put("d", campos.get("d").getText().trim());
        textoCampos.put("Pcon", campos.get("Pcon").getText().trim());
        textoCampos.put("M", campos.get("M").getText().trim());

        // Conta campos vazios e converte para Double
        Map<String, Double> parametros = new HashMap<>();
        String faltante = null;
        int vazios = 0;

        for (Map.Entry<String, String> entry : textoCampos.entrySet()) {
            String chave = entry.getKey();
            String texto = entry.getValue();

            if (texto.isEmpty()) {
                parametros.put(chave, null);
                faltante = chave;
                vazios++;
            } else {
                try {
                    parametros.put(chave, Double.parseDouble(texto));
                } catch (NumberFormatException e) {
                    exibirAlertas(List.of(
                        String.format("Valor invalido no campo '%s': '%s'. Use apenas numeros.",
                            getNomeCampo(chave), texto)
                    ));
                    return;
                }
            }
        }

        // Adiciona alpha (atenuacao da fibra) do Equipamento
        parametros.put("alpha", equipamento.getAtenuacaoFibra());

        // Adiciona N (splitter) do dropdown
        int idx = comboSplitter.getSelectedIndex();
        if (idx >= 0 && idx < SPLITTER_VALUES.length) {
            parametros.put("N", SPLITTER_VALUES[idx]);
        } else {
            parametros.put("N", 32.0); // fallback
        }

        // Valida contagem de vazios
        if (vazios == 0) {
            exibirAlertas(List.of(
                "Nenhum campo esta vazio. Deixe exatamente um campo em branco " +
                "para que o sistema calcule a variavel correspondente."
            ));
            return;
        }

        if (vazios > 1) {
            exibirAlertas(List.of(
                String.format("Mais de um campo esta vazio (%d campos). " +
                    "Preencha todos os campos exceto aquele que deseja calcular.", vazios)
            ));
            return;
        }

        // Executa o calculo
        try {
            ResultadoCalculo resultado = controlador.processarCalculo(parametros);
            exibirResultado(resultado);
        } catch (IllegalArgumentException | NullPointerException e) {
            exibirAlertas(List.of("Erro: " + e.getMessage()));
        }
    }

    /**
     * Exibe o resultado do calculo na area destacada.
     */
    private void exibirResultado(ResultadoCalculo resultado) {
        String nomeExibicao = getNomeCampo(resultado.getVariavel());
        String unidade = getUnidade(resultado.getVariavel());

        // Se for o splitter (N), mostra como razao
        String valorFormatado;
        if ("N".equals(resultado.getVariavel())) {
            valorFormatado = String.format("1:%d (N = %.2f)",
                    Math.round(resultado.getValor()), resultado.getValor());
        } else {
            valorFormatado = String.format("%.2f %s", resultado.getValor(), unidade);
        }

        areaResultado.setText(String.format(
            "✓ Variavel calculada: %s = %s", nomeExibicao, valorFormatado
        ));
        areaResultado.setBackground(new Color(230, 255, 230));

        // Exibe alertas
        if (resultado.temAlertas()) {
            exibirAlertas(resultado.getAlertas());
            areaResultado.setBackground(new Color(255, 255, 200));
        } else {
            areaAlertas.setText("Nenhum alerta — todos os parametros estao dentro dos padroes ITU-T.");
            areaAlertas.setBackground(new Color(240, 255, 240));
        }
    }

    /**
     * Exibe uma lista de alertas na area de alertas.
     */
    private void exibirAlertas(List<String> alertas) {
        areaAlertas.setBackground(new Color(255, 240, 230));
        StringBuilder sb = new StringBuilder();
        for (String alerta : alertas) {
            sb.append("⚠ ").append(alerta).append("\n");
        }
        areaAlertas.setText(sb.toString().trim());
    }

    /**
     * Retorna o nome amigavel da chave do mapa.
     */
    private String getNomeCampo(String chave) {
        switch (chave) {
            case "Ptx":   return "Potencia de Transmissao";
            case "S":     return "Sensibilidade do Receptor";
            case "alpha": return "Atenuacao da Fibra";
            case "d":     return "Distancia";
            case "N":     return "Divisao do Splitter";
            case "Pcon":  return "Perda por Conectores";
            case "M":     return "Margem de Seguranca";
            default:      return chave;
        }
    }

    /**
     * Retorna a unidade de medida da variavel.
     */
    private String getUnidade(String chave) {
        switch (chave) {
            case "Ptx": case "S": return "dBm";
            case "alpha": return "dB/km";
            case "d": return "km";
            case "Pcon": case "M": return "dB";
            case "N": return "";
            default: return "";
        }
    }

    /**
     * Inicia a interface grafica no Event Dispatch Thread.
     */
    public void iniciar() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    /**
     * Filtro de documento que permite apenas caracteres numericos validos.
     * Aceita: digitos (0-9), ponto decimal (.), sinal negativo (-) no inicio,
     * e notacao cientifica (e/E).
     */
    private static class FiltroNumerico extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string == null) return;

            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String newText = currentText.substring(0, offset) + string + currentText.substring(offset);

            if (isValidNumericInput(newText)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null) return;

            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
            String newText = currentText.substring(0, offset) + text + currentText.substring(offset + length);

            if (isValidNumericInput(newText)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            super.remove(fb, offset, length);
        }

        /**
         * Valida se a string representa uma entrada numerica valida (incluindo parcial).
         * Permite: string vazia, sinal negativo inicial, numeros com ponto decimal,
         * e inicio de notacao cientifica.
         */
        private boolean isValidNumericInput(String text) {
            if (text.isEmpty()) return true;

            // Permite um unico sinal negativo no inicio
            if (text.equals("-")) return true;

            // Remove sinal negativo para validacao
            String semSinal = text.startsWith("-") ? text.substring(1) : text;

            // Verifica se contem apenas caracteres validos
            return semSinal.matches("[0-9]*\\.?[0-9]*(?:[eE]-?[0-9]*)?");
        }
    }

    /**
     * Ponto de entrada para testes independentes da GUI.
     */
    public static void main(String[] args) {
        // Define look and feel do sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback para o LookAndFeel padrao do Java
        }

        CalculadoraGUI gui = new CalculadoraGUI();
        gui.iniciar();
    }
}
