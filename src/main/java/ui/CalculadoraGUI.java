package ui;

import controller.Controlador;
import controller.ResultadoCalculo;
import model.Equipamento;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface grafica da Calculadora de Link Budget GPON.
 * 
 * <p>Implementada com Java Swing, oferece:
 * <ul>
 *   <li>Selecao de comprimento de onda (1490 nm downstream / 1310 nm upstream)</li>
 *   <li>Campos numericos com validacao em tempo real</li>
 *   <li>Suporte a splitter secundario opcional</li>
 *   <li>Calculo de perda por conectores e fusoes com contagem individual</li>
 *   <li>Areas de resultado e alertas com rolagem</li>
 * </ul>
 * 
 * <p>Valores baseados nas recomendacoes ITU-T G.984.2 (GPON) e G.652 (fibra).</p>
 * 
 * @author Eduardo Tenorio Nunes, Joao Victor Borges Carvalho
 * @version 1.2
 * @see Controlador
 * @see LinkBudget
 * @see Validador
 */
public class CalculadoraGUI extends JFrame {

    private final Controlador controlador;
    private final Equipamento equipamento;

    // Componentes da interface
    private final Map<String, JTextField> campos;
    private JComboBox<String> comboWavelength;
    private JTextField campoAlpha;
    private JComboBox<String> comboSplitter1;
    private JCheckBox chkSplitter2;
    private JComboBox<String> comboSplitter2;
    private JTextField campoNumConectores;
    private JTextField campoPerdaConector;
    private JTextField campoNumFusoes;
    private JTextField campoPerdaFusao;
    private JLabel labelPconTotal;
    private JTextArea areaResultado;
    private JTextArea areaAlertas;
    private final JButton btnCalcular;

    // Valores do splitter
    private static final String[] SPLITTER_LABELS = {
        "1:2", "1:4", "1:8", "1:16", "1:32", "1:64"
    };
    private static final double[] SPLITTER_VALUES = {
        2.0, 4.0, 8.0, 16.0, 32.0, 64.0
    };

    // Comprimentos de onda
    private static final String[] WL_LABELS = {
        "1490 nm (Downstream)", "1310 nm (Upstream)"
    };
    private static final int[] WL_VALUES = { 1490, 1310 };

    /**
     * Construtor — inicializa o controlador, equipamento e constroi a interface.
     */
    public CalculadoraGUI() {
        this.controlador = new Controlador();
        this.equipamento = new Equipamento();
        this.campos = new HashMap<>();

        setTitle("Calculadora de Link Budget — GPON");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(620, 780);
        setMinimumSize(new Dimension(520, 680));
        setLocationRelativeTo(null);

        // Painel principal com BorderLayout
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(new EmptyBorder(10, 10, 10, 10));
        painelPrincipal.setBackground(new Color(245, 245, 250));

        // Painel de entrada com scroll
        JPanel painelEntrada = criarPainelEntrada();
        JScrollPane scrollEntrada = new JScrollPane(painelEntrada);
        scrollEntrada.setBorder(null);
        scrollEntrada.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollEntrada.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollEntrada.getVerticalScrollBar().setUnitIncrement(16);
        painelPrincipal.add(scrollEntrada, BorderLayout.CENTER);

        // Botao Calcular (sempre visivel)
        btnCalcular = new JButton("Calcular");
        btnCalcular.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnCalcular.setBackground(new Color(70, 130, 180));
        btnCalcular.setForeground(Color.WHITE);
        btnCalcular.setFocusPainted(false);
        btnCalcular.setBorderPainted(false);
        btnCalcular.setOpaque(true);
        btnCalcular.setContentAreaFilled(true);
        btnCalcular.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCalcular.addActionListener(e -> onCalcular());

        // Painel combinado: botao + resultado (sempre visiveis no sul)
        JPanel painelSul = new JPanel(new BorderLayout(0, 5));
        painelSul.setOpaque(false);

        JPanel painelBotao = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        painelBotao.setOpaque(false);
        painelBotao.add(btnCalcular);
        painelSul.add(painelBotao, BorderLayout.NORTH);

        JPanel painelSaida = criarPainelSaida();
        painelSul.add(painelSaida, BorderLayout.CENTER);

        painelPrincipal.add(painelSul, BorderLayout.SOUTH);

        add(painelPrincipal);
    }

    /**
     * Cria o painel de entrada completo com todas as secoes.
     */
    private JPanel criarPainelEntrada() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setOpaque(false);

        painel.add(criarSecaoWavelength());
        painel.add(Box.createVerticalStrut(4));
        painel.add(criarSecaoParametros());
        painel.add(Box.createVerticalStrut(4));
        painel.add(criarSecaoSplitters());
        painel.add(Box.createVerticalStrut(4));
        painel.add(criarSecaoConectores());
        painel.add(Box.createVerticalStrut(4));
        painel.add(criarSecaoMargem());

        return painel;
    }

    private JPanel criarSecaoWavelength() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setOpaque(false);
        painel.setBorder(new TitledBorder("Comprimento de Onda e Fibra"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 5, 3, 5);

        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0;
        painel.add(new JLabel("Comprimento de onda:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        comboWavelength = new JComboBox<>(WL_LABELS);
        comboWavelength.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboWavelength.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) atualizarAlphaPorWavelength();
        });
        painel.add(comboWavelength, gbc);

        gbc.gridy = 1; gbc.gridx = 0; gbc.weightx = 0;
        painel.add(new JLabel("Atenuacao da fibra (dB/km):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        campoAlpha = new JTextField(String.valueOf(equipamento.getAtenuacao1490()), 10);
        campoAlpha.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoAlpha.setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument) campoAlpha.getDocument()).setDocumentFilter(new FiltroNumerico());
        campos.put("alpha", campoAlpha);
        painel.add(campoAlpha, gbc);
        return painel;
    }

    private void atualizarAlphaPorWavelength() {
        int idx = comboWavelength.getSelectedIndex();
        if (idx >= 0 && idx < WL_VALUES.length) {
            double alpha = equipamento.getAtenuacaoFibra(WL_VALUES[idx]);
            campoAlpha.setText(String.valueOf(alpha));
        }
    }

    private JPanel criarSecaoParametros() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setOpaque(false);
        painel.setBorder(new TitledBorder("Parametros do Enlace"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 5, 3, 5);

        adicionarCampoGrid(painel, gbc, 0, "Potencia de Transmissao (dBm):", "Ptx", "");
        adicionarCampoGrid(painel, gbc, 1, "Sensibilidade do Receptor (dBm):", "S", "");
        adicionarCampoGrid(painel, gbc, 2, "Distancia (km):", "d", "");
        return painel;
    }

    private JPanel criarSecaoSplitters() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setOpaque(false);
        painel.setBorder(new TitledBorder("Divisores Opticos (Splitters)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 5, 3, 5);

        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0;
        painel.add(new JLabel("Splitter primario:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        comboSplitter1 = new JComboBox<>(SPLITTER_LABELS);
        comboSplitter1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboSplitter1.setSelectedIndex(3); // 1:16 padrao
        painel.add(comboSplitter1, gbc);

        gbc.gridy = 1; gbc.gridx = 0; gbc.gridwidth = 2;
        chkSplitter2 = new JCheckBox("Adicionar splitter secundario");
        chkSplitter2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkSplitter2.setOpaque(false);
        chkSplitter2.addItemListener(e ->
            comboSplitter2.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        painel.add(chkSplitter2, gbc);
        gbc.gridwidth = 1;

        gbc.gridy = 2; gbc.gridx = 0; gbc.weightx = 0;
        painel.add(new JLabel("Splitter secundario:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        comboSplitter2 = new JComboBox<>(SPLITTER_LABELS);
        comboSplitter2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboSplitter2.setSelectedIndex(1); // 1:4
        comboSplitter2.setEnabled(false);
        painel.add(comboSplitter2, gbc);
        return painel;
    }

    private JPanel criarSecaoConectores() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setOpaque(false);
        painel.setBorder(new TitledBorder("Conectores e Fusoes"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 5, 3, 5);

        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0;
        painel.add(new JLabel("Numero de conectores:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        campoNumConectores = criarCampoInteiro("2");
        painel.add(campoNumConectores, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        painel.add(new JLabel("  Perda por conector (dB):"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.3;
        campoPerdaConector = criarCampoDecimal(String.valueOf(equipamento.getPerdaPorConector()));
        painel.add(campoPerdaConector, gbc);

        gbc.gridy = 1; gbc.gridx = 0; gbc.weightx = 0;
        painel.add(new JLabel("Numero de fusoes:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        campoNumFusoes = criarCampoInteiro("4");
        painel.add(campoNumFusoes, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        painel.add(new JLabel("  Perda por fusao (dB):"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.3;
        campoPerdaFusao = criarCampoDecimal(String.valueOf(equipamento.getPerdaPorFusao()));
        painel.add(campoPerdaFusao, gbc);

        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 4; gbc.weightx = 1;
        labelPconTotal = new JLabel("Perda total: 1.4 dB");
        labelPconTotal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelPconTotal.setForeground(new Color(70, 130, 180));
        painel.add(labelPconTotal, gbc);
        return painel;
    }

    private JTextField criarCampoInteiro(String valor) {
        JTextField campo = new JTextField(valor, 5);
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new FiltroInteiro());
        campo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { atualizarPconTotal(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { atualizarPconTotal(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { atualizarPconTotal(); }
        });
        return campo;
    }

    private JTextField criarCampoDecimal(String valor) {
        JTextField campo = new JTextField(valor, 5);
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new FiltroNumerico());
        campo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { atualizarPconTotal(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { atualizarPconTotal(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { atualizarPconTotal(); }
        });
        return campo;
    }

    private JPanel criarSecaoMargem() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setOpaque(false);
        painel.setBorder(new TitledBorder("Margem de Seguranca"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 5, 3, 5);
        adicionarCampoGrid(painel, gbc, 0, "Margem de Seguranca (dB):", "M",
                String.valueOf(equipamento.getMargem()));
        return painel;
    }

    private void atualizarPconTotal() {
        int nc = parseIntro(campoNumConectores.getText());
        double pc = parseDoubleSafe(campoPerdaConector.getText(), equipamento.getPerdaPorConector());
        int nf = parseIntro(campoNumFusoes.getText());
        double pf = parseDoubleSafe(campoPerdaFusao.getText(), equipamento.getPerdaPorFusao());
        double total = nc * pc + nf * pf;
        labelPconTotal.setText(String.format("Perda total: %.2f dB  (%d conect. × %.1f + %d fus. × %.1f)",
                total, nc, pc, nf, pf));
    }

    private int parseIntro(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private double parseDoubleSafe(String s, double d) {
        if (s == null || s.trim().isEmpty()) return d;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return d; }
    }

    private void adicionarCampoGrid(JPanel painel, GridBagConstraints gbc, int linha,
                                     String rotulo, String chave, String valorPadrao) {
        gbc.gridy = linha;
        gbc.gridx = 0; gbc.weightx = 0;
        JLabel label = new JLabel(rotulo);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        painel.add(label, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JTextField campo = new JTextField(valorPadrao, 12);
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new FiltroNumerico());
        campos.put(chave, campo);
        painel.add(campo, gbc);
    }

    private JPanel criarPainelSaida() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setOpaque(false);

        areaResultado = new JTextArea(2, 40);
        areaResultado.setEditable(false);
        areaResultado.setFont(new Font("Segoe UI", Font.BOLD, 14));
        areaResultado.setBackground(new Color(230, 255, 230));
        areaResultado.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("Resultado"), new EmptyBorder(5, 8, 5, 8)));
        areaResultado.setLineWrap(true);
        areaResultado.setWrapStyleWord(true);

        areaAlertas = new JTextArea(6, 40);
        areaAlertas.setEditable(false);
        areaAlertas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        areaAlertas.setBackground(new Color(255, 245, 230));
        areaAlertas.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("Alertas / Avisos"), new EmptyBorder(5, 8, 5, 8)));
        areaAlertas.setLineWrap(true);
        areaAlertas.setWrapStyleWord(true);

        JScrollPane scrollAlertas = new JScrollPane(areaAlertas);
        scrollAlertas.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollAlertas.setPreferredSize(new Dimension(550, 100));

        JPanel painelResultado = new JPanel(new BorderLayout(5, 5));
        painelResultado.setOpaque(false);
        painelResultado.add(areaResultado, BorderLayout.NORTH);
        painelResultado.add(scrollAlertas, BorderLayout.CENTER);
        painel.add(painelResultado, BorderLayout.CENTER);
        return painel;
    }

    private void onCalcular() {
        areaResultado.setText("");
        areaAlertas.setText("");

        Map<String, String> tc = new HashMap<>();
        tc.put("Ptx", campos.get("Ptx").getText().trim());
        tc.put("S", campos.get("S").getText().trim());
        tc.put("d", campos.get("d").getText().trim());
        tc.put("M", campos.get("M").getText().trim());
        tc.put("alpha", campoAlpha.getText().trim());
        tc.put("Pcon", String.valueOf(calcularPconTotal()));
        tc.put("N", String.valueOf(calcularSplitterTotal()));

        Map<String, Double> parametros = new HashMap<>();
        int vazios = 0;
        for (Map.Entry<String, String> e : tc.entrySet()) {
            String k = e.getKey(), v = e.getValue();
            if (v.isEmpty()) { parametros.put(k, null); vazios++; }
            else {
                try { parametros.put(k, Double.parseDouble(v)); }
                catch (NumberFormatException ex) {
                    exibirAlertas(List.of("Valor invalido em '" + getNomeCampo(k) + "': " + v));
                    return;
                }
            }
        }

        if (vazios == 0) {
            exibirAlertas(List.of("Deixe exatamente um campo em branco para calcular."));
            return;
        }
        if (vazios > 1) {
            exibirAlertas(List.of(vazios + " campos vazios. Deixe apenas um em branco."));
            return;
        }

        try {
            ResultadoCalculo r = controlador.processarCalculo(parametros);
            exibirResultado(r);
        } catch (Exception e) {
            exibirAlertas(List.of("Erro: " + e.getMessage()));
        }
    }

    private double calcularPconTotal() {
        return parseIntro(campoNumConectores.getText()) *
               parseDoubleSafe(campoPerdaConector.getText(), equipamento.getPerdaPorConector()) +
               parseIntro(campoNumFusoes.getText()) *
               parseDoubleSafe(campoPerdaFusao.getText(), equipamento.getPerdaPorFusao());
    }

    private double calcularSplitterTotal() {
        double n1 = getSplitterValue(comboSplitter1);
        double n2 = chkSplitter2.isSelected() ? getSplitterValue(comboSplitter2) : 1.0;
        return n1 * n2;
    }

    private double getSplitterValue(JComboBox<String> cb) {
        int i = cb.getSelectedIndex();
        return (i >= 0 && i < SPLITTER_VALUES.length) ? SPLITTER_VALUES[i] : 16.0;
    }

    private void exibirResultado(ResultadoCalculo r) {
        String nome = getNomeCampo(r.getVariavel());
        String uni = getUnidade(r.getVariavel());
        String vf;
        if ("N".equals(r.getVariavel())) {
            vf = String.format("%s (N = %.2f)", formatarRazaoSplitter(r.getValor()), r.getValor());
        } else {
            vf = String.format("%.2f %s", r.getValor(), uni);
        }
        areaResultado.setText("✓ " + nome + " = " + vf);
        areaResultado.setBackground(new Color(230, 255, 230));

        if (r.temAlertas()) {
            exibirAlertas(r.getAlertas());
            boolean soMsgOk = r.getAlertas().size() == 1
                    && r.getAlertas().get(0).contains("Todos os parametros dentro");
            areaResultado.setBackground(soMsgOk ? new Color(230, 255, 230) : new Color(255, 255, 200));
        } else {
            areaAlertas.setText("Nenhum alerta — todos os parametros estao dentro dos padroes ITU-T.");
            areaAlertas.setBackground(new Color(240, 255, 240));
        }
    }

    private void exibirAlertas(List<String> alertas) {
        areaAlertas.setBackground(new Color(255, 240, 230));
        StringBuilder sb = new StringBuilder();
        for (String a : alertas) sb.append("⚠ ").append(a).append("\n");
        areaAlertas.setText(sb.toString().trim());
    }

    private String formatarRazaoSplitter(double n) {
        for (double v : new double[]{2,4,8,16,32,64,128,256})
            if (Math.abs(n - v) < 0.05 * v) return "1:" + (int)v;
        return "~1:" + Math.round(n);
    }

    private String getNomeCampo(String chave) {
        switch (chave) {
            case "Ptx": return "Potencia de Transmissao";
            case "S": return "Sensibilidade do Receptor";
            case "alpha": return "Atenuacao da Fibra";
            case "d": return "Distancia";
            case "N": return "Divisao do Splitter";
            case "Pcon": return "Perda por Conectores";
            case "M": return "Margem de Seguranca";
            default: return chave;
        }
    }

    private String getUnidade(String chave) {
        switch (chave) {
            case "Ptx": case "S": return "dBm";
            case "alpha": return "dB/km";
            case "d": return "km";
            case "Pcon": case "M": return "dB";
            default: return "";
        }
    }

    public void iniciar() { SwingUtilities.invokeLater(() -> setVisible(true)); }

    // ─── Filtros ──────────────────────────────────────────────────────

    private static class FiltroNumerico extends DocumentFilter {
        public void insertString(FilterBypass fb, int off, String s, AttributeSet a) throws BadLocationException {
            if (s != null && ok(fb, off, s, 0)) super.insertString(fb, off, s, a);
        }
        public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a) throws BadLocationException {
            if (s != null && ok(fb, off, s, len)) super.replace(fb, off, len, s, a);
        }
        public void remove(FilterBypass fb, int off, int len) throws BadLocationException { super.remove(fb, off, len); }
        private boolean ok(FilterBypass fb, int off, String s, int len) throws BadLocationException {
            String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
            String nxt = cur.substring(0, off) + s + cur.substring(off + len);
            if (nxt.isEmpty() || nxt.equals("-")) return true;
            String ss = nxt.startsWith("-") ? nxt.substring(1) : nxt;
            return ss.matches("[0-9]*\\.?[0-9]*(?:[eE]-?[0-9]*)?");
        }
    }

    private static class FiltroInteiro extends DocumentFilter {
        public void insertString(FilterBypass fb, int off, String s, AttributeSet a) throws BadLocationException {
            if (s != null && ok(fb, off, s, 0)) super.insertString(fb, off, s, a);
        }
        public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a) throws BadLocationException {
            if (s != null && ok(fb, off, s, len)) super.replace(fb, off, len, s, a);
        }
        public void remove(FilterBypass fb, int off, int len) throws BadLocationException { super.remove(fb, off, len); }
        private boolean ok(FilterBypass fb, int off, String s, int len) throws BadLocationException {
            String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
            String nxt = cur.substring(0, off) + s + cur.substring(off + len);
            return nxt.isEmpty() || nxt.matches("[0-9]+");
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo i : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(i.getName())) { UIManager.setLookAndFeel(i.getClassName()); break; }
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ex) {}
        }
        new CalculadoraGUI().iniciar();
    }
}
