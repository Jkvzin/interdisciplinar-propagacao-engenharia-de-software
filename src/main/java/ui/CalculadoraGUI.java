package ui;

import controller.Controlador;
import controller.ResultadoCalculo;
import model.Equipamento;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * Calculadora de Link Budget GPON com acessibilidade.
 *
 * <p>Recursos de acessibilidade:</p>
 * <ul>
 *   <li>3 tamanhos de fonte (Normal / Grande / Enorme)</li>
 *   <li>Modo alto contraste (fundo escuro, texto claro)</li>
 *   <li>Nomes acessiveis para leitores de tela (NVDA, JAWS)</li>
 *   <li>Atalhos de teclado (Alt+letra nos campos)</li>
 *   <li>Tooltips descritivos em todos os campos</li>
 *   <li>Indicadores de foco visiveis (borda grossa colorida)</li>
 * </ul>
 */
public class CalculadoraGUI extends JFrame {

    private final Controlador controlador;
    private final Equipamento equipamento;
    private final Map<String, JTextField> campos;

    // Componentes
    private JComboBox<String> comboWavelength;
    private JTextField campoAlpha;
    private JComboBox<String> comboSplitter1;
    private JCheckBox chkSplitter2;
    private JComboBox<String> comboSplitter2;
    private JTextField campoNumConectores, campoPerdaConector, campoNumFusoes, campoPerdaFusao;
    private JLabel labelPconTotal;
    private JTextArea areaResultado, areaAlertas;
    private JButton btnCalcular;
    private JPanel painelPrincipal;
    private JScrollPane scrollEntrada;
    private final List<JComponent> todosComponentes = new ArrayList<>();
    private final List<JLabel> todosLabels = new ArrayList<>();
    private final List<JPanel> paineisSecao = new ArrayList<>();

    // Estados
    private int tamanhoFonte = 1; // 0=normal, 1=grande, 2=enorme
    private boolean altoContraste = false;

    // Tamanhos de fonte por nivel
    private static final int[] FONT_SIZES = {12, 18, 24};
    private static final int[] FONT_TITLES = {13, 20, 26};
    private static final int[] FONT_BOLD = {14, 22, 28};
    private static final int[] FONT_BTN = {16, 24, 32};

    // Cores
    private static final Color BG_NORMAL = new Color(245, 245, 250);
    private static final Color BG_DARK = new Color(30, 30, 35);
    private static final Color FG_DARK = new Color(230, 230, 240);
    private static final Color ACCENT = new Color(70, 130, 180);
    private static final Color ACCENT_HC = new Color(255, 200, 50);
    private static final Color RESULT_OK = new Color(180, 255, 180);
    private static final Color RESULT_OK_DARK = new Color(40, 100, 40);
    private static final Color RESULT_WARN = new Color(255, 255, 180);
    private static final Color RESULT_WARN_DARK = new Color(100, 100, 40);
    private static final Color ALERT_BG = new Color(255, 240, 230);
    private static final Color ALERT_BG_DARK = new Color(60, 40, 30);

    // Splitters
    private static final String[] SPLITTER_LABELS = {"1:2","1:4","1:8","1:16","1:32","1:64"};
    private static final double[] SPLITTER_VALUES = {2,4,8,16,32,64};
    private static final String[] WL_LABELS = {"1490 nm (Downstream)","1310 nm (Upstream)"};
    private static final int[] WL_VALUES = {1490, 1310};

    public CalculadoraGUI() {
        this.controlador = new Controlador();
        this.equipamento = new Equipamento();
        this.campos = new HashMap<>();

        setTitle("Calculadora de Link Budget — GPON");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(680, 820);
        setMinimumSize(new Dimension(560, 700));
        setLocationRelativeTo(null);

        construirInterface();
        aplicarAcessibilidade();
    }

    // ═══════════════════════════════════════════════════════════════
    // Construcao da interface
    // ═══════════════════════════════════════════════════════════════

    private void construirInterface() {
        painelPrincipal = new JPanel(new BorderLayout(6, 6));
        painelPrincipal.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Barra de acessibilidade
        painelPrincipal.add(criarBarraAcessibilidade(), BorderLayout.NORTH);

        // Entrada com scroll
        JPanel entrada = criarPainelEntrada();
        scrollEntrada = new JScrollPane(entrada);
        scrollEntrada.setBorder(null);
        scrollEntrada.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollEntrada.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollEntrada.getVerticalScrollBar().setUnitIncrement(20);
        painelPrincipal.add(scrollEntrada, BorderLayout.CENTER);

        // Sul: botao + resultado
        JPanel sul = new JPanel(new BorderLayout(0, 4));
        sul.setOpaque(false);

        JPanel pb = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        pb.setOpaque(false);
        btnCalcular = criarBotao("Calcular", 'C', "Realiza o calculo do Link Budget (atalho: Ctrl+Enter)");
        pb.add(btnCalcular);
        sul.add(pb, BorderLayout.NORTH);
        sul.add(criarPainelSaida(), BorderLayout.CENTER);
        painelPrincipal.add(sul, BorderLayout.SOUTH);

        // Bind Ctrl+Enter no root pane
        getRootPane().setDefaultButton(btnCalcular);
        getRootPane().registerKeyboardAction(e -> onCalcular(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        add(painelPrincipal);
    }

    private JPanel criarBarraAcessibilidade() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        barra.setBorder(new EmptyBorder(2, 0, 4, 0));

        JButton btnFonte = criarBotaoPequeno("A-", "Diminuir fonte");
        JButton btnFonteP = criarBotaoPequeno("A+", "Aumentar fonte");
        JToggleButton btnContraste = new JToggleButton("◐ Alto Contraste");
        btnContraste.setToolTipText("Alterna modo alto contraste (fundo escuro, texto claro)");

        btnFonte.addActionListener(e -> { if (tamanhoFonte > 0) { tamanhoFonte--; aplicarAcessibilidade(); }});
        btnFonteP.addActionListener(e -> { if (tamanhoFonte < 2) { tamanhoFonte++; aplicarAcessibilidade(); }});
        btnContraste.addItemListener(e -> { altoContraste = e.getStateChange() == ItemEvent.SELECTED; aplicarAcessibilidade(); });

        barra.add(new JLabel("Acessibilidade:"));
        barra.add(btnFonte);
        barra.add(btnFonteP);
        barra.add(btnContraste);

        todosComponentes.add(btnFonte);
        todosComponentes.add(btnFonteP);
        todosComponentes.add(btnContraste);
        return barra;
    }

    private JPanel criarPainelEntrada() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));

        painel.add(criarSecao("Comprimento de Onda e Fibra", this::povoarSecaoWavelength));
        painel.add(Box.createVerticalStrut(6));
        painel.add(criarSecao("Parametros do Enlace", this::povoarSecaoParametros));
        painel.add(Box.createVerticalStrut(6));
        painel.add(criarSecao("Divisores Opticos (Splitters)", this::povoarSecaoSplitters));
        painel.add(Box.createVerticalStrut(6));
        painel.add(criarSecao("Conectores e Fusoes", this::povoarSecaoConectores));
        painel.add(Box.createVerticalStrut(6));
        painel.add(criarSecao("Margem de Seguranca", this::povoarSecaoMargem));

        return painel;
    }

    private JPanel criarSecao(String titulo, java.util.function.Consumer<JPanel> povoar) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder(titulo));
        povoar.accept(p);
        paineisSecao.add(p);
        return p;
    }

    private void povoarSecaoWavelength(JPanel p) {
        GridBagConstraints g = gbc();
        g.gridy=0; g.gridx=0; g.weightx=0;
        p.add(label("Comprimento de onda:", 'O', "Selecione 1490 nm (downstream) ou 1310 nm (upstream)"), g);
        g.gridx=1; g.weightx=1;
        comboWavelength = new JComboBox<>(WL_LABELS);
        comboWavelength.addItemListener(e -> { if (e.getStateChange()==ItemEvent.SELECTED) atualizarAlpha(); });
        acessivel(comboWavelength, "Comprimento de onda", "1490 nm downstream ou 1310 nm upstream");
        p.add(comboWavelength, g);

        g.gridy=1; g.gridx=0; g.weightx=0;
        p.add(label("Atenuacao (dB/km):", 'A', "Coeficiente de atenuacao da fibra optica em dB por km"), g);
        g.gridx=1; g.weightx=1;
        campoAlpha = campoDecimal(String.valueOf(equipamento.getAtenuacao1490()), "alpha",
            "Atenuacao da fibra", "Valor em dB/km. G.652: 0.28 a 1490 nm, 0.35 a 1310 nm");
        p.add(campoAlpha, g);
    }

    private void povoarSecaoParametros(JPanel p) {
        GridBagConstraints g = gbc();
        adicionarCampo(p, g, 0, "Potencia de Transmissao (dBm):", 'P', "Ptx",
            "Potencia de saida do transmissor optico. GPON tipico: +1.5 a +5 dBm");
        adicionarCampo(p, g, 1, "Sensibilidade do Receptor (dBm):", 'S', "S",
            "Sensibilidade minima do receptor. GPON Classe B+: -28 dBm");
        adicionarCampo(p, g, 2, "Distancia (km):", 'D', "d",
            "Comprimento total do enlace optico em quilometros");
    }

    private void povoarSecaoSplitters(JPanel p) {
        GridBagConstraints g = gbc();
        g.gridy=0; g.gridx=0; g.weightx=0;
        p.add(label("Splitter primario:", '1', "Razao de divisao do splitter optico principal"), g);
        g.gridx=1; g.weightx=1;
        comboSplitter1 = new JComboBox<>(SPLITTER_LABELS);
        comboSplitter1.setSelectedIndex(3);
        acessivel(comboSplitter1, "Splitter primario", "Razao de divisao do primeiro splitter optico");
        p.add(comboSplitter1, g);

        g.gridy=1; g.gridx=0; g.gridwidth=2;
        chkSplitter2 = new JCheckBox("Adicionar splitter secundario");
        chkSplitter2.setMnemonic('2');
        chkSplitter2.setToolTipText("Marque para habilitar um segundo splitter em cascata");
        chkSplitter2.addItemListener(e -> comboSplitter2.setEnabled(e.getStateChange()==ItemEvent.SELECTED));
        acessivel(chkSplitter2, "Splitter secundario", "Habilita ou desabilita o segundo splitter optico");
        p.add(chkSplitter2, g);
        g.gridwidth=1;

        g.gridy=2; g.gridx=0; g.weightx=0;
        p.add(label("Splitter secundario:", '3', "Razao de divisao do segundo splitter (se habilitado)"), g);
        g.gridx=1; g.weightx=1;
        comboSplitter2 = new JComboBox<>(SPLITTER_LABELS);
        comboSplitter2.setSelectedIndex(1);
        comboSplitter2.setEnabled(false);
        acessivel(comboSplitter2, "Splitter secundario", "Razao de divisao do segundo splitter optico");
        p.add(comboSplitter2, g);
    }

    private void povoarSecaoConectores(JPanel p) {
        GridBagConstraints g = gbc();

        g.gridy=0; g.gridx=0; g.weightx=0;
        p.add(label("Nº conectores:", 'C', "Quantidade de conectores no enlace"), g);
        g.gridx=1; g.weightx=0.3;
        campoNumConectores = campoInteiro("2", "Numero de conectores", "Quantos conectores opticos existem no enlace");
        p.add(campoNumConectores, g);

        g.gridx=2; g.weightx=0;
        p.add(label(" Perda/conector (dB):", 'E', "Perda por conector individual em dB"), g);
        g.gridx=3; g.weightx=0.3;
        campoPerdaConector = campoDecimal(String.valueOf(equipamento.getPerdaPorConector()), null,
            "Perda por conector", "Perda tipica: 0.5 dB por conector SC/APC");
        p.add(campoPerdaConector, g);

        g.gridy=1; g.gridx=0; g.weightx=0;
        p.add(label("Nº fusoes:", 'F', "Quantidade de emendas por fusao no enlace"), g);
        g.gridx=1; g.weightx=0.3;
        campoNumFusoes = campoInteiro("4", "Numero de fusoes", "Quantas emendas por fusao existem no enlace");
        p.add(campoNumFusoes, g);

        g.gridx=2; g.weightx=0;
        p.add(label(" Perda/fusao (dB):", 'U', "Perda por fusao individual em dB"), g);
        g.gridx=3; g.weightx=0.3;
        campoPerdaFusao = campoDecimal(String.valueOf(equipamento.getPerdaPorFusao()), null,
            "Perda por fusao", "Perda tipica: 0.05 a 0.1 dB por fusao");
        p.add(campoPerdaFusao, g);

        g.gridy=2; g.gridx=0; g.gridwidth=4; g.weightx=1;
        labelPconTotal = new JLabel("Perda total: 1.4 dB");
        labelPconTotal.setToolTipText("Perda total = conectores × perda/conector + fusoes × perda/fusao");
        p.add(labelPconTotal, g);
    }

    private void povoarSecaoMargem(JPanel p) {
        GridBagConstraints g = gbc();
        adicionarCampo(p, g, 0, "Margem de Seguranca (dB):", 'M', "M",
            "Margem para variacoes de temperatura, envelhecimento e reparos. Minimo recomendado: 3 dB");
        campos.get("M").setText(String.valueOf(equipamento.getMargem()));
    }

    private JPanel criarPainelSaida() {
        JPanel painel = new JPanel(new BorderLayout());

        areaResultado = new JTextArea(2, 40);
        areaResultado.setEditable(false);
        areaResultado.setLineWrap(true);
        areaResultado.setWrapStyleWord(true);
        areaResultado.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("Resultado"), new EmptyBorder(6, 10, 6, 10)));
        acessivel(areaResultado, "Resultado do calculo", "Mostra o valor calculado da variavel");

        areaAlertas = new JTextArea(5, 40);
        areaAlertas.setEditable(false);
        areaAlertas.setLineWrap(true);
        areaAlertas.setWrapStyleWord(true);
        areaAlertas.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("Alertas / Avisos"), new EmptyBorder(6, 10, 6, 10)));
        acessivel(areaAlertas, "Alertas e avisos", "Lista de alertas de validacao ITU-T");

        JScrollPane scrollAlertas = new JScrollPane(areaAlertas);
        scrollAlertas.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollAlertas.setPreferredSize(new Dimension(600, 110));

        JPanel pr = new JPanel(new BorderLayout(5, 5));
        pr.setOpaque(false);
        pr.add(areaResultado, BorderLayout.NORTH);
        pr.add(scrollAlertas, BorderLayout.CENTER);
        painel.add(pr, BorderLayout.CENTER);
        return painel;
    }

    // ═══════════════════════════════════════════════════════════════
    // Acessibilidade
    // ═══════════════════════════════════════════════════════════════

    private void aplicarAcessibilidade() {
        int fs = FONT_SIZES[tamanhoFonte];
        int ft = FONT_TITLES[tamanhoFonte];
        int fb = FONT_BOLD[tamanhoFonte];
        int fbtn = FONT_BTN[tamanhoFonte];

        Font fontPlain  = new Font("Segoe UI", Font.PLAIN, fs);
        Font fontBold   = new Font("Segoe UI", Font.BOLD, fb);
        Font fontTitle  = new Font("Segoe UI", Font.BOLD, ft);
        Font fontButton = new Font("Segoe UI", Font.BOLD, fbtn);

        // Cores de fundo/texto
        Color bg = altoContraste ? BG_DARK : BG_NORMAL;
        Color fg = altoContraste ? FG_DARK : Color.BLACK;
        Color accent = altoContraste ? ACCENT_HC : ACCENT;
        Color resultOk = altoContraste ? RESULT_OK_DARK : RESULT_OK;
        Color resultWarn = altoContraste ? RESULT_WARN_DARK : RESULT_WARN;
        Color alertBg = altoContraste ? ALERT_BG_DARK : ALERT_BG;

        painelPrincipal.setBackground(bg);

        // Aplica em todos os labels
        for (JLabel lb : todosLabels) {
            lb.setFont(fontPlain);
            lb.setForeground(fg);
        }

        // Aplica em componentes
        for (JComponent c : todosComponentes) {
            c.setFont(fontPlain);
            c.setForeground(fg);
            if (c instanceof JButton && c != btnCalcular) {
                c.setFont(fontPlain);
            }
        }

        // Campos de texto
        for (JTextField tf : campos.values()) {
            tf.setFont(fontPlain);
            tf.setForeground(fg);
            tf.setBackground(altoContraste ? new Color(50,50,55) : Color.WHITE);
            tf.setCaretColor(fg);
        }

        // Campos extras
        if (campoAlpha != null) {
            campoAlpha.setFont(fontPlain); campoAlpha.setForeground(fg);
            campoAlpha.setBackground(altoContraste ? new Color(50,50,55) : Color.WHITE);
            campoAlpha.setCaretColor(fg);
        }
        for (JTextField tf : new JTextField[]{campoNumConectores, campoPerdaConector, campoNumFusoes, campoPerdaFusao}) {
            if (tf != null) { tf.setFont(fontPlain); tf.setForeground(fg);
                tf.setBackground(altoContraste ? new Color(50,50,55) : Color.WHITE); tf.setCaretColor(fg); }
        }

        // Combos
        for (JComboBox<?> cb : new JComboBox[]{comboWavelength, comboSplitter1, comboSplitter2}) {
            if (cb != null) { cb.setFont(fontPlain); cb.setForeground(fg);
                cb.setBackground(altoContraste ? new Color(60,60,65) : Color.WHITE); }
        }
        if (chkSplitter2 != null) { chkSplitter2.setFont(fontPlain); chkSplitter2.setForeground(fg); }

        // Areas de texto
        for (JTextArea ta : new JTextArea[]{areaResultado, areaAlertas}) {
            if (ta != null) { ta.setFont(fontBold); }
        }
        if (areaResultado != null) areaResultado.setBackground(resultOk);
        if (areaAlertas != null) areaAlertas.setBackground(alertBg);

        // Botao calcular
        btnCalcular.setFont(fontButton);
        btnCalcular.setBackground(accent);
        btnCalcular.setForeground(altoContraste ? Color.BLACK : Color.WHITE);
        btnCalcular.setBorder(new LineBorder(accent, altoContraste ? 3 : 1));

        // Label Pcon
        if (labelPconTotal != null) {
            labelPconTotal.setFont(fontBold);
            labelPconTotal.setForeground(accent);
        }

        // Paineis de secao
        for (JPanel sec : paineisSecao) {
            sec.setBackground(bg);
            if (sec.getBorder() instanceof TitledBorder tb) {
                tb.setTitleFont(fontTitle);
                tb.setTitleColor(accent);
            }
        }

        // Scroll
        if (scrollEntrada != null) {
            scrollEntrada.getViewport().getView().setBackground(bg);
        }

        // Foco visivel
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", evt -> {
            Component foco = (Component) evt.getNewValue();
            if (foco instanceof JTextField tf) {
                tf.setBorder(new LineBorder(accent, altoContraste ? 3 : 2));
            }
        });

        // Revalida layout
        painelPrincipal.revalidate();
        painelPrincipal.repaint();

        // Atualiza tamanho da janela
        int w = 640 + tamanhoFonte * 60;
        int h = 780 + tamanhoFonte * 80;
        setSize(w, h);
    }

    private void acessivel(JComponent c, String nome, String descricao) {
        c.getAccessibleContext().setAccessibleName(nome);
        c.getAccessibleContext().setAccessibleDescription(descricao);
        c.setToolTipText(descricao);
        todosComponentes.add(c);
    }

    private JLabel label(String texto, char mnemonic, String tooltip) {
        JLabel lb = new JLabel(texto);
        lb.setDisplayedMnemonic(mnemonic);
        lb.setToolTipText(tooltip + " (Alt+" + mnemonic + ")");
        lb.getAccessibleContext().setAccessibleName(texto);
        lb.getAccessibleContext().setAccessibleDescription(tooltip);
        todosLabels.add(lb);
        return lb;
    }

    private JButton criarBotao(String texto, char mnemonic, String tooltip) {
        JButton b = new JButton(texto);
        b.setMnemonic(mnemonic);
        b.setToolTipText(tooltip);
        b.setFocusPainted(true);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> onCalcular());
        acessivel(b, texto, tooltip);
        return b;
    }

    private JButton criarBotaoPequeno(String texto, String tooltip) {
        JButton b = new JButton(texto);
        b.setToolTipText(tooltip);
        b.setFocusPainted(true);
        b.getAccessibleContext().setAccessibleName(texto);
        b.getAccessibleContext().setAccessibleDescription(tooltip);
        todosComponentes.add(b);
        return b;
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(3, 6, 3, 6);
        return g;
    }

    private void adicionarCampo(JPanel p, GridBagConstraints g, int linha, String rotulo, char mn, String chave, String tooltip) {
        g.gridy = linha; g.gridx = 0; g.weightx = 0;
        JLabel lb = label(rotulo, mn, tooltip);
        p.add(lb, g);
        // Associar label ao campo via mnemonic
        g.gridx = 1; g.weightx = 1;
        JTextField tf = campoDecimal("", chave, rotulo, tooltip);
        lb.setLabelFor(tf);
        p.add(tf, g);
    }

    private JTextField campoDecimal(String valor, String chave, String nome, String tooltip) {
        JTextField tf = new JTextField(valor, 12);
        tf.setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument) tf.getDocument()).setDocumentFilter(new FiltroNumerico());
        acessivel(tf, nome, tooltip);
        if (chave != null) campos.put(chave, tf);
        return tf;
    }

    private JTextField campoInteiro(String valor, String nome, String tooltip) {
        JTextField tf = new JTextField(valor, 5);
        tf.setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument) tf.getDocument()).setDocumentFilter(new FiltroInteiro());
        tf.getDocument().addDocumentListener(new DocListener(this::atualizarPconTotal));
        acessivel(tf, nome, tooltip);
        return tf;
    }

    private void atualizarAlpha() {
        int i = comboWavelength.getSelectedIndex();
        if (i >= 0 && i < WL_VALUES.length)
            campoAlpha.setText(String.valueOf(equipamento.getAtenuacaoFibra(WL_VALUES[i])));
    }

    private void atualizarPconTotal() {
        int nc = parseInt(campoNumConectores.getText());
        double pc = parseDouble(campoPerdaConector.getText(), equipamento.getPerdaPorConector());
        int nf = parseInt(campoNumFusoes.getText());
        double pf = parseDouble(campoPerdaFusao.getText(), equipamento.getPerdaPorFusao());
        double t = nc*pc + nf*pf;
        labelPconTotal.setText(String.format("Perda total: %.2f dB  (%d con. × %.1f + %d fus. × %.1f)", t, nc, pc, nf, pf));
    }

    // ═══════════════════════════════════════════════════════════════
    // Logica de calculo
    // ═══════════════════════════════════════════════════════════════

    private void onCalcular() {
        areaResultado.setText(""); areaAlertas.setText("");

        Map<String, String> tc = new LinkedHashMap<>();
        tc.put("Ptx", campos.get("Ptx").getText().trim());
        tc.put("S", campos.get("S").getText().trim());
        tc.put("d", campos.get("d").getText().trim());
        tc.put("M", campos.get("M").getText().trim());
        tc.put("alpha", campoAlpha.getText().trim());
        tc.put("Pcon", String.valueOf(calcPcon()));
        tc.put("N", String.valueOf(calcN()));

        Map<String, Double> p = new HashMap<>();
        int vazios = 0;
        for (var e : tc.entrySet()) {
            if (e.getValue().isEmpty()) { p.put(e.getKey(), null); vazios++; }
            else { try { p.put(e.getKey(), Double.parseDouble(e.getValue())); }
                   catch (NumberFormatException ex) { mostrarErro("Valor invalido em " + nomeCampo(e.getKey()) + ": " + e.getValue()); return; } }
        }

        if (vazios == 0) { mostrarErro("Deixe exatamente um campo em branco para calcular."); return; }
        if (vazios > 1) { mostrarErro(vazios + " campos vazios. Deixe apenas UM em branco."); return; }

        try { exibirResultado(controlador.processarCalculo(p)); }
        catch (Exception e) { mostrarErro("Erro: " + e.getMessage()); }
    }

    private double calcPcon() {
        return parseInt(campoNumConectores.getText()) * parseDouble(campoPerdaConector.getText(), equipamento.getPerdaPorConector())
             + parseInt(campoNumFusoes.getText())     * parseDouble(campoPerdaFusao.getText(), equipamento.getPerdaPorFusao());
    }
    private double calcN() {
        double n1 = getSplitter(comboSplitter1);
        double n2 = chkSplitter2.isSelected() ? getSplitter(comboSplitter2) : 1.0;
        return n1 * n2;
    }
    private double getSplitter(JComboBox<String> cb) { int i = cb.getSelectedIndex(); return (i>=0 && i<SPLITTER_VALUES.length) ? SPLITTER_VALUES[i] : 16; }

    private void exibirResultado(ResultadoCalculo r) {
        String nome = nomeCampo(r.getVariavel());
        String uni = unidade(r.getVariavel());
        String vf = "N".equals(r.getVariavel()) ? formatarN(r.getValor()) : String.format("%.2f %s", r.getValor(), uni);
        areaResultado.setText("✓ " + nome + " = " + vf);

        if (r.temAlertas()) {
            boolean soOk = r.getAlertas().size() == 1 && r.getAlertas().get(0).contains("Todos os parametros dentro");
            areaResultado.setBackground(soOk ? (altoContraste ? RESULT_OK_DARK : RESULT_OK) : (altoContraste ? RESULT_WARN_DARK : RESULT_WARN));
            mostrarAlertas(r.getAlertas());
        } else {
            areaAlertas.setText("Nenhum alerta — parametros dentro dos padroes ITU-T.");
            areaAlertas.setBackground(altoContraste ? RESULT_OK_DARK : RESULT_OK);
        }
    }

    private void mostrarErro(String msg) { areaAlertas.setBackground(altoContraste ? new Color(80,30,30) : new Color(255,230,230)); areaAlertas.setText("⚠ " + msg); }
    private void mostrarAlertas(List<String> alertas) {
        areaAlertas.setBackground(altoContraste ? ALERT_BG_DARK : ALERT_BG);
        StringBuilder sb = new StringBuilder();
        for (String a : alertas) sb.append("⚠ ").append(a).append("\n");
        areaAlertas.setText(sb.toString().trim());
    }

    private String formatarN(double n) {
        for (double v : new double[]{2,4,8,16,32,64,128,256})
            if (Math.abs(n-v) < 0.05*v) return "1:"+(int)v;
        return "~1:"+Math.round(n);
    }

    private String nomeCampo(String chave) { return switch(chave) {
        case "Ptx"->"Potencia de Transmissao"; case "S"->"Sensibilidade do Receptor";
        case "alpha"->"Atenuacao da Fibra"; case "d"->"Distancia";
        case "N"->"Divisao do Splitter"; case "Pcon"->"Perda por Conectores";
        case "M"->"Margem de Seguranca"; default->chave; }; }
    private String unidade(String chave) { return switch(chave) {
        case "Ptx","S"->"dBm"; case "alpha"->"dB/km"; case "d"->"km";
        case "Pcon","M"->"dB"; default->""; }; }

    private int parseInt(String s) { if (s==null||s.trim().isEmpty()) return 0; try {return Integer.parseInt(s.trim());} catch(NumberFormatException e){return 0;} }
    private double parseDouble(String s, double def) { if (s==null||s.trim().isEmpty()) return def; try {return Double.parseDouble(s.trim());} catch(NumberFormatException e){return def;} }

    // ═══════════════════════════════════════════════════════════════
    // Filtros
    // ═══════════════════════════════════════════════════════════════

    private static class FiltroNumerico extends DocumentFilter {
        public void insertString(FilterBypass fb, int off, String s, AttributeSet a) throws BadLocationException { if (s!=null&&ok(fb,off,s,0)) super.insertString(fb,off,s,a); }
        public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a) throws BadLocationException { if (s!=null&&ok(fb,off,s,len)) super.replace(fb,off,len,s,a); }
        public void remove(FilterBypass fb, int off, int len) throws BadLocationException { super.remove(fb,off,len); }
        private boolean ok(FilterBypass fb, int off, String s, int len) throws BadLocationException {
            String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
            String nxt = cur.substring(0,off)+s+cur.substring(off+len);
            if (nxt.isEmpty()||nxt.equals("-")) return true;
            String ss = nxt.startsWith("-")?nxt.substring(1):nxt;
            return ss.matches("[0-9]*\\.?[0-9]*(?:[eE]-?[0-9]*)?");
        }
    }

    private static class FiltroInteiro extends DocumentFilter {
        public void insertString(FilterBypass fb, int off, String s, AttributeSet a) throws BadLocationException { if (s!=null&&ok(fb,off,s,0)) super.insertString(fb,off,s,a); }
        public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a) throws BadLocationException { if (s!=null&&ok(fb,off,s,len)) super.replace(fb,off,len,s,a); }
        public void remove(FilterBypass fb, int off, int len) throws BadLocationException { super.remove(fb,off,len); }
        private boolean ok(FilterBypass fb, int off, String s, int len) throws BadLocationException {
            String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
            String nxt = cur.substring(0,off)+s+cur.substring(off+len);
            return nxt.isEmpty()||nxt.matches("[0-9]+");
        }
    }

    private static class DocListener implements javax.swing.event.DocumentListener {
        private final Runnable fn;
        DocListener(Runnable fn) { this.fn = fn; }
        public void insertUpdate(javax.swing.event.DocumentEvent e) { fn.run(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { fn.run(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { fn.run(); }
    }

    // ═══════════════════════════════════════════════════════════════

    public void iniciar() { SwingUtilities.invokeLater(() -> setVisible(true)); }

    public static void main(String[] args) {
        try { for (UIManager.LookAndFeelInfo i : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(i.getName())) { UIManager.setLookAndFeel(i.getClassName()); break; } }
        catch (Exception e) { try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ex) {} }
        // Forca fonte base maior no Nimbus
        for (var entry : UIManager.getDefaults().entrySet())
            if (entry.getKey().toString().contains("font")) UIManager.put(entry.getKey(), new Font("Segoe UI", Font.PLAIN, 14));
        new CalculadoraGUI().iniciar();
    }
}
