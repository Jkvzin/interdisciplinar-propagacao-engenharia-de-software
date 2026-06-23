package ui;

import controller.Controlador;
import controller.ResultadoCalculo;
import model.Equipamento;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Calculadora de Link Budget GPON com acessibilidade embutida.
 *
 * <p>Layout em duas colunas com todas as secoes visiveis sem scroll.</p>
 * <p>Botao de acessibilidade (⚙) abre dialogo com opcoes de
 * fonte e alto contraste. Suporte a leitor de tela e navegacao
 * completa por teclado (Alt+letra).</p>
 */
public class CalculadoraGUI extends JFrame {

    private final Controlador controlador;
    private final Equipamento equipamento;
    private final Map<String, JTextField> campos = new HashMap<>();

    // Componentes
    private JComboBox<String> comboWL;
    private JTextField campoAlpha;
    private JComboBox<String> comboS1, comboS2;
    private JCheckBox chkS2;
    private JTextField campoNCon, campoPCon, campoNFus, campoPFus;
    private JLabel labelPconTotal;
    private JTextArea areaResultado, areaAlertas;
    private JButton btnCalcular, btnAcessibilidade;

    // Estado
    private int nivelFonte = 1; // 0=normal 1=grande 2=enorme
    private boolean altoContraste = false;
    private final List<JComponent> todosComp = new ArrayList<>();
    private final List<JLabel> todosLabels = new ArrayList<>();
    private final List<JPanel> secoes = new ArrayList<>();
    private final List<JTextField> camposConectores = new ArrayList<>();

    private static final int[] FS = {12, 18, 24};
    private static final int[] FT = {13, 20, 26};
    private static final int[] FB = {14, 22, 28};
    private static final int[] FBTN = {16, 24, 32};

    private static final String[] SPLIT = {"1:2","1:4","1:8","1:16","1:32","1:64"};
    private static final double[] SPLIT_V = {2,4,8,16,32,64};
    private static final String[] WL = {"1490 nm (Downstream)","1310 nm (Upstream)"};
    private static final int[] WL_V = {1490,1310};

    public CalculadoraGUI() {
        controlador = new Controlador();
        equipamento = new Equipamento();
        setTitle("Calculadora de Link Budget GPON");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(880, 720);
        setMinimumSize(new Dimension(800, 650));
        setLocationRelativeTo(null);
        construir();
        aplicarTema();
    }

    // ═══════════════ CONSTRUCAO ═══════════════

    private void construir() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Cabecalho: titulo + botao acessibilidade
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Link Budget — GPON (ITU-T G.984.2 / G.652)");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.add(titulo, BorderLayout.WEST);

        btnAcessibilidade = new JButton("⚙ Acessibilidade");
        btnAcessibilidade.setToolTipText("Abrir opcoes de acessibilidade (tamanho da fonte, alto contraste)");
        btnAcessibilidade.addActionListener(e -> abrirDialogoAcessibilidade());
        acessivel(btnAcessibilidade, "Acessibilidade", "Abre dialogo com opcoes de fonte e contraste");
        header.add(btnAcessibilidade, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Painel de entrada em 2 colunas
        root.add(criarEntrada2Colunas(), BorderLayout.CENTER);

        // Sul: botao calcular + resultado
        JPanel sul = new JPanel(new BorderLayout(0, 6));
        sul.setOpaque(false);

        btnCalcular = criarBotaoCalcular();
        JPanel pb = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        pb.setOpaque(false);
        pb.add(btnCalcular);
        sul.add(pb, BorderLayout.NORTH);
        sul.add(criarPainelSaida(), BorderLayout.CENTER);
        root.add(sul, BorderLayout.SOUTH);

        // Tecla de atalho
        getRootPane().setDefaultButton(btnCalcular);
        getRootPane().registerKeyboardAction(e -> onCalcular(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        add(root);
    }

    private JPanel criarEntrada2Colunas() {
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 8, 4, 8);
        g.anchor = GridBagConstraints.NORTHWEST;

        // Coluna esquerda
        JPanel colEsq = new JPanel();
        colEsq.setLayout(new BoxLayout(colEsq, BoxLayout.Y_AXIS));
        colEsq.add(secao("Comprimento de Onda e Fibra", this::wlFibra));
        colEsq.add(Box.createVerticalStrut(6));
        colEsq.add(secao("Parametros do Enlace", this::paramsEnlace));

        // Coluna direita
        JPanel colDir = new JPanel();
        colDir.setLayout(new BoxLayout(colDir, BoxLayout.Y_AXIS));
        colDir.add(secao("Divisores Opticos (Splitters)", this::splitters));
        colDir.add(Box.createVerticalStrut(6));
        colDir.add(secao("Conectores e Fusoes", this::conectores));
        colDir.add(Box.createVerticalStrut(6));
        colDir.add(secao("Margem de Seguranca", this::margem));

        g.gridx=0; g.gridy=0; g.weightx=0.5; g.weighty=1;
        grid.add(colEsq, g);
        g.gridx=1; g.weightx=0.5;
        grid.add(colDir, g);

        return grid;
    }

    private JPanel secao(String titulo, java.util.function.Consumer<JPanel> fn) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder(titulo));
        fn.accept(p);
        secoes.add(p);
        return p;
    }

    private void wlFibra(JPanel p) {
        GridBagConstraints g = gbc();
        g.gridy=0; g.gridx=0; g.weightx=0;
        p.add(label("Comprimento de onda:", 'O'), g);
        g.gridx=1; g.weightx=1;
        comboWL = combo(WL, 0, "Comprimento de onda", "1490 nm downstream ou 1310 nm upstream");
        comboWL.addItemListener(e -> { if(e.getStateChange()==ItemEvent.SELECTED) attAlpha(); });
        p.add(comboWL, g);

        g.gridy=1; g.gridx=0; g.weightx=0;
        p.add(label("Atenuacao (dB/km):", 'A'), g);
        g.gridx=1; g.weightx=1;
        campoAlpha = campoDecimal(String.valueOf(equipamento.getAtenuacao1490()), "alpha",
            "Atenuacao da fibra", "G.652: 0.28 dB/km @1490nm, 0.35 dB/km @1310nm");
        p.add(campoAlpha, g);
    }

    private void paramsEnlace(JPanel p) {
        GridBagConstraints g = gbc();
        addCampo(p, g, 0, "Potencia de Transmissao (dBm):", 'P', "Ptx", "OLT GPON: +1.5 a +5 dBm");
        addCampo(p, g, 1, "Sensibilidade do Receptor (dBm):", 'S', "S", "ONU Classe B+: -28 dBm");
        addCampo(p, g, 2, "Distancia do enlace (km):", 'D', "d", "Comprimento total do enlace optico");
    }

    private void splitters(JPanel p) {
        GridBagConstraints g = gbc();
        g.gridy=0; g.gridx=0; g.weightx=0;
        p.add(label("Splitter primario:", '1'), g);
        g.gridx=1; g.weightx=1;
        comboS1 = combo(SPLIT, 3, "Splitter primario", "Razao de divisao do primeiro splitter");
        p.add(comboS1, g);

        g.gridy=1; g.gridx=0; g.gridwidth=2;
        chkS2 = new JCheckBox("Adicionar splitter secundario");
        chkS2.setMnemonic('2');
        chkS2.setToolTipText("Habilita segundo splitter em cascata");
        chkS2.addItemListener(e -> comboS2.setEnabled(e.getStateChange()==ItemEvent.SELECTED));
        acessivel(chkS2, "Splitter secundario", "Habilita splitter adicional");
        p.add(chkS2, g);
        g.gridwidth=1;

        g.gridy=2; g.gridx=0; g.weightx=0;
        p.add(label("Splitter secundario:", '3'), g);
        g.gridx=1; g.weightx=1;
        comboS2 = combo(SPLIT, 1, "Splitter secundario", "Segundo splitter em cascata");
        comboS2.setEnabled(false);
        p.add(comboS2, g);
    }

    private void conectores(JPanel p) {
        GridBagConstraints g = gbc();
        // Linha 0: conectores
        g.gridy=0; g.gridx=0; g.weightx=0;
        p.add(label("Nº conectores:", 'C'), g);
        g.gridx=1; g.weightx=0.4;
        campoNCon = campoInt("2", "Numero de conectores", "Quantidade de conectores no enlace");
        p.add(campoNCon, g);
        g.gridx=2; g.weightx=0;
        p.add(label(" Perda (dB):", 'E'), g);
        g.gridx=3; g.weightx=0.4;
        campoPCon = campoDecimal(String.valueOf(equipamento.getPerdaPorConector()), null,
            "Perda por conector", "Tipico: 0.5 dB (SC/APC)");
        p.add(campoPCon, g);

        // Linha 1: fusoes
        g.gridy=1; g.gridx=0; g.weightx=0;
        p.add(label("Nº fusoes:", 'F'), g);
        g.gridx=1; g.weightx=0.4;
        campoNFus = campoInt("4", "Numero de fusoes", "Quantidade de emendas por fusao");
        p.add(campoNFus, g);
        g.gridx=2; g.weightx=0;
        p.add(label(" Perda (dB):", 'U'), g);
        g.gridx=3; g.weightx=0.4;
        campoPFus = campoDecimal(String.valueOf(equipamento.getPerdaPorFusao()), null,
            "Perda por fusao", "Tipico: 0.05 a 0.1 dB");
        p.add(campoPFus, g);

        // Total
        g.gridy=2; g.gridx=0; g.gridwidth=4; g.weightx=1;
        labelPconTotal = new JLabel("Perda total: 1.4 dB");
        labelPconTotal.setToolTipText("Total = conectores × perda/conector + fusoes × perda/fusao");
        p.add(labelPconTotal, g);
    }

    private void margem(JPanel p) {
        GridBagConstraints g = gbc();
        addCampo(p, g, 0, "Margem de Seguranca (dB):", 'M', "M",
            "Folga para variacoes ambientais. Minimo recomendado: 3 dB");
        campos.get("M").setText(String.valueOf(equipamento.getMargem()));
    }

    private JPanel criarPainelSaida() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setOpaque(false);

        areaResultado = areaTexto(2, "Resultado", "Valor calculado da variavel");
        areaAlertas = areaTexto(5, "Alertas", "Alertas de validacao ITU-T");

        JScrollPane sp = new JScrollPane(areaAlertas);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(800, 100));

        p.add(areaResultado, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JTextArea areaTexto(int rows, String titulo, String desc) {
        JTextArea ta = new JTextArea(rows, 60);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder(titulo), new EmptyBorder(6, 10, 6, 10)));
        acessivel(ta, titulo, desc);
        return ta;
    }

    private JButton criarBotaoCalcular() {
        JButton b = new JButton("Calcular Link Budget");
        b.setMnemonic('L');
        b.setToolTipText("Calcula a variavel em branco (atalho: Ctrl+Enter)");
        b.setFocusPainted(true);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> onCalcular());
        acessivel(b, "Calcular", "Executa o calculo do Link Budget");
        return b;
    }

    // ═══════════════ DIALOGO ACESSIBILIDADE ═══════════════

    private void abrirDialogoAcessibilidade() {
        JDialog dlg = new JDialog(this, "Acessibilidade", true);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.getContentPane().setBackground(SystemColor.control);

        JPanel conteudo = new JPanel(new GridBagLayout());
        conteudo.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 5, 8, 5);

        // Tamanho da fonte
        g.gridy=0; g.gridx=0; g.gridwidth=3;
        conteudo.add(new JLabel("Tamanho da fonte:"), g);
        g.gridwidth=1;

        JRadioButton rbNormal = new JRadioButton("Normal (12pt)");
        JRadioButton rbGrande = new JRadioButton("Grande (18pt)");
        JRadioButton rbEnorme = new JRadioButton("Enorme (24pt)");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbNormal); grupo.add(rbGrande); grupo.add(rbEnorme);
        switch (nivelFonte) {
            case 0: rbNormal.setSelected(true); break;
            case 1: rbGrande.setSelected(true); break;
            case 2: rbEnorme.setSelected(true); break;
        }

        g.gridy=1; g.gridx=0; conteudo.add(rbNormal, g);
        g.gridx=1; conteudo.add(rbGrande, g);
        g.gridx=2; conteudo.add(rbEnorme, g);

        // Alto contraste
        g.gridy=2; g.gridx=0; g.gridwidth=3;
        JCheckBox chkContraste = new JCheckBox("Modo alto contraste (fundo escuro, texto claro)");
        chkContraste.setSelected(altoContraste);
        conteudo.add(chkContraste, g);

        // Botoes
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = new JButton("Aplicar");
        JButton btnCancel = new JButton("Cancelar");
        btnOk.addActionListener(e -> {
            if (rbNormal.isSelected()) nivelFonte = 0;
            else if (rbGrande.isSelected()) nivelFonte = 1;
            else nivelFonte = 2;
            altoContraste = chkContraste.isSelected();
            aplicarTema();
            dlg.dispose();
        });
        btnCancel.addActionListener(e -> dlg.dispose());
        botoes.add(btnOk);
        botoes.add(btnCancel);

        dlg.add(conteudo, BorderLayout.CENTER);
        dlg.add(botoes, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ═══════════════ TEMA ═══════════════

    private void aplicarTema() {
        int fs = FS[nivelFonte], ft = FT[nivelFonte], fb = FB[nivelFonte], fbtn = FBTN[nivelFonte];
        Font fp = new Font("Segoe UI", Font.PLAIN, fs);
        Font fbold = new Font("Segoe UI", Font.BOLD, fb);
        Font ftitle = new Font("Segoe UI", Font.BOLD, ft);
        Font fbutton = new Font("Segoe UI", Font.BOLD, fbtn);

        Color bg = altoContraste ? new Color(30,30,35) : new Color(245,245,250);
        Color fg = altoContraste ? new Color(230,230,240) : Color.BLACK;
        Color fieldBg = altoContraste ? new Color(50,50,55) : Color.WHITE;
        Color btnCalcBg = altoContraste ? new Color(255,180,0) : new Color(0,120,50);
        Color btnCalcFg = altoContraste ? Color.BLACK : Color.WHITE;
        Color accent = altoContraste ? new Color(255,200,50) : new Color(0,100,50);
        Color resOk = altoContraste ? new Color(40,100,40) : new Color(200,250,200);
        Color resWarn = altoContraste ? new Color(100,100,40) : new Color(255,255,180);
        Color alertBg = altoContraste ? new Color(60,40,30) : new Color(255,245,230);

        for (JLabel lb : todosLabels) { lb.setFont(fp); lb.setForeground(fg); }

        for (JTextField tf : campos.values()) {
            tf.setFont(fp); tf.setForeground(fg); tf.setBackground(fieldBg); tf.setCaretColor(fg);
        }
        for (JTextField tf : new JTextField[]{campoAlpha, campoNCon, campoPCon, campoNFus, campoPFus}) {
            if (tf != null) { tf.setFont(fp); tf.setForeground(fg); tf.setBackground(fieldBg); tf.setCaretColor(fg); }
        }
        for (JComboBox<?> cb : new JComboBox[]{comboWL, comboS1, comboS2}) {
            if (cb != null) { cb.setFont(fp); cb.setForeground(fg); cb.setBackground(fieldBg); }
        }
        if (chkS2 != null) { chkS2.setFont(fp); chkS2.setForeground(fg); }

        for (JComponent c : todosComp) c.setFont(fp);

        btnCalcular.setFont(fbutton);
        btnCalcular.setBackground(btnCalcBg);
        btnCalcular.setForeground(btnCalcFg);
        btnCalcular.setBorder(new LineBorder(btnCalcBg.darker(), 2));
        btnCalcular.setPreferredSize(new Dimension(280, 40 + nivelFonte*12));

        btnAcessibilidade.setFont(fp);
        btnAcessibilidade.setForeground(fg);

        if (labelPconTotal != null) { labelPconTotal.setFont(fbold); labelPconTotal.setForeground(accent); }

        areaResultado.setFont(fbold);
        areaResultado.setBackground(resOk);
        areaResultado.setForeground(altoContraste ? Color.WHITE : Color.BLACK);

        areaAlertas.setFont(fp);
        areaAlertas.setBackground(alertBg);
        areaAlertas.setForeground(altoContraste ? Color.WHITE : Color.BLACK);

        for (JPanel sec : secoes) {
            sec.setBackground(bg);
            if (sec.getBorder() instanceof TitledBorder tb) {
                tb.setTitleFont(ftitle);
                tb.setTitleColor(accent);
            }
        }

        getContentPane().setBackground(bg);
        revalidate();
        repaint();
    }

    // ═══════════════ HELPERS ═══════════════

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(3, 5, 3, 5);
        return g;
    }

    private JLabel label(String texto, char mn) {
        JLabel lb = new JLabel(texto);
        lb.setDisplayedMnemonic(mn);
        todosLabels.add(lb);
        return lb;
    }

    private JComboBox<String> combo(String[] itens, int sel, String nome, String desc) {
        JComboBox<String> cb = new JComboBox<>(itens);
        cb.setSelectedIndex(sel);
        acessivel(cb, nome, desc);
        return cb;
    }

    private JTextField campoDecimal(String valor, String chave, String nome, String desc) {
        JTextField tf = new JTextField(valor, 10);
        tf.setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument)tf.getDocument()).setDocumentFilter(new FiltroNumerico());
        acessivel(tf, nome, desc);
        if (chave != null) campos.put(chave, tf);
        return tf;
    }

    private JTextField campoInt(String valor, String nome, String desc) {
        JTextField tf = new JTextField(valor, 5);
        tf.setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument)tf.getDocument()).setDocumentFilter(new FiltroInteiro());
        tf.getDocument().addDocumentListener(new DocListener(this::attPconTotal));
        camposConectores.add(tf);
        acessivel(tf, nome, desc);
        return tf;
    }

    private void addCampo(JPanel p, GridBagConstraints g, int lin, String txt, char mn, String ch, String tip) {
        g.gridy=lin; g.gridx=0; g.weightx=0;
        JLabel lb = label(txt, mn);
        lb.setToolTipText(tip + " (Alt+"+mn+")");
        p.add(lb, g);
        g.gridx=1; g.weightx=1;
        JTextField tf = campoDecimal("", ch, txt, tip);
        lb.setLabelFor(tf);
        p.add(tf, g);
    }

    private void acessivel(JComponent c, String nome, String desc) {
        c.getAccessibleContext().setAccessibleName(nome);
        c.getAccessibleContext().setAccessibleDescription(desc);
        c.setToolTipText(desc);
        todosComp.add(c);
    }

    private void attAlpha() {
        int i = comboWL.getSelectedIndex();
        if (i>=0 && i<WL_V.length) campoAlpha.setText(String.valueOf(equipamento.getAtenuacaoFibra(WL_V[i])));
    }

    private void attPconTotal() {
        int nc = parseInt(campoNCon.getText());
        double pc = parseDouble(campoPCon.getText(), equipamento.getPerdaPorConector());
        int nf = parseInt(campoNFus.getText());
        double pf = parseDouble(campoPFus.getText(), equipamento.getPerdaPorFusao());
        double t = nc*pc + nf*pf;
        labelPconTotal.setText(String.format("Perda total: %.2f dB  (%d c.×%.1f + %d f.×%.1f)", t, nc, pc, nf, pf));
    }

    // ═══════════════ CALCULO ═══════════════

    private void onCalcular() {
        areaResultado.setText(""); areaAlertas.setText("");
        Map<String,String> tc = new LinkedHashMap<>();
        tc.put("Ptx",campos.get("Ptx").getText().trim());
        tc.put("S",campos.get("S").getText().trim());
        tc.put("d",campos.get("d").getText().trim());
        tc.put("M",campos.get("M").getText().trim());
        tc.put("alpha",campoAlpha.getText().trim());
        tc.put("Pcon",String.valueOf(calcPcon()));
        tc.put("N",String.valueOf(calcN()));

        Map<String,Double> p = new HashMap<>(); int v=0;
        for (var e:tc.entrySet()) {
            if (e.getValue().isEmpty()){p.put(e.getKey(),null);v++;}
            else try{p.put(e.getKey(),Double.parseDouble(e.getValue()));}
            catch(NumberFormatException ex){err("Valor invalido: "+nome(e.getKey())+" = "+e.getValue());return;}
        }
        if (v==0){err("Deixe exatamente UM campo em branco.");return;}
        if (v>1){err(v+" campos vazios. Deixe apenas UM.");return;}
        try{exibir(controlador.processarCalculo(p));}catch(Exception e){err("Erro: "+e.getMessage());}
    }

    private double calcPcon(){return parseInt(campoNCon.getText())*parseDouble(campoPCon.getText(),equipamento.getPerdaPorConector())+parseInt(campoNFus.getText())*parseDouble(campoPFus.getText(),equipamento.getPerdaPorFusao());}
    private double calcN(){double n1=getS(comboS1);double n2=chkS2.isSelected()?getS(comboS2):1;return n1*n2;}
    private double getS(JComboBox<String> cb){int i=cb.getSelectedIndex();return(i>=0&&i<SPLIT_V.length)?SPLIT_V[i]:16;}

    private void exibir(ResultadoCalculo r){
        String nm=nome(r.getVariavel()),un=unid(r.getVariavel());
        String vf="N".equals(r.getVariavel())?fmtN(r.getValor()):String.format("%.2f %s",r.getValor(),un);
        areaResultado.setText("✓ "+nm+" = "+vf);
        areaResultado.setBackground(altoContraste?new Color(40,100,40):new Color(200,250,200));
        if(r.temAlertas()){
            boolean soOk=r.getAlertas().size()==1&&r.getAlertas().get(0).contains("Todos os parametros dentro");
            if(!soOk)areaResultado.setBackground(altoContraste?new Color(100,100,40):new Color(255,255,180));
            mostrarAlertas(r.getAlertas());
        }else{areaAlertas.setText("Nenhum alerta — parametros dentro dos padroes ITU-T.");}
    }
    private void err(String m){areaAlertas.setText("⚠ "+m);areaAlertas.setBackground(altoContraste?new Color(80,30,30):new Color(255,230,230));}
    private void mostrarAlertas(List<String> a){StringBuilder sb=new StringBuilder();for(String s:a)sb.append("⚠ ").append(s).append("\n");areaAlertas.setText(sb.toString().trim());}
    private String fmtN(double n){for(double v:new double[]{2,4,8,16,32,64,128,256})if(Math.abs(n-v)<0.05*v)return"1:"+(int)v;return"~1:"+Math.round(n);}
    private String nome(String c){return switch(c){case"Ptx"->"Potencia Tx";case"S"->"Sensibilidade";case"alpha"->"Atenuacao";case"d"->"Distancia";case"N"->"Splitter";case"Pcon"->"Conectores";case"M"->"Margem";default->c;};}
    private String unid(String c){return switch(c){case"Ptx","S"->"dBm";case"alpha"->"dB/km";case"d"->"km";case"Pcon","M"->"dB";default->"";};}
    private int parseInt(String s){if(s==null||s.trim().isEmpty())return 0;try{return Integer.parseInt(s.trim());}catch(NumberFormatException e){return 0;}}
    private double parseDouble(String s,double d){if(s==null||s.trim().isEmpty())return d;try{return Double.parseDouble(s.trim());}catch(NumberFormatException e){return d;}}

    // ═══════════════ FILTROS ═══════════════

    private static class FiltroNumerico extends DocumentFilter {
        public void insertString(FilterBypass fb,int o,String s,AttributeSet a)throws BadLocationException{if(s!=null&&ok(fb,o,s,0))super.insertString(fb,o,s,a);}
        public void replace(FilterBypass fb,int o,int l,String s,AttributeSet a)throws BadLocationException{if(s!=null&&ok(fb,o,s,l))super.replace(fb,o,l,s,a);}
        public void remove(FilterBypass fb,int o,int l)throws BadLocationException{super.remove(fb,o,l);}
        private boolean ok(FilterBypass fb,int o,String s,int l)throws BadLocationException{
            String c=fb.getDocument().getText(0,fb.getDocument().getLength());
            String n=c.substring(0,o)+s+c.substring(o+l);
            if(n.isEmpty()||n.equals("-"))return true;
            String ns=n.startsWith("-")?n.substring(1):n;
            return ns.matches("[0-9]*\\.?[0-9]*(?:[eE]-?[0-9]*)?");
        }
    }
    private static class FiltroInteiro extends DocumentFilter {
        public void insertString(FilterBypass fb,int o,String s,AttributeSet a)throws BadLocationException{if(s!=null&&ok(fb,o,s,0))super.insertString(fb,o,s,a);}
        public void replace(FilterBypass fb,int o,int l,String s,AttributeSet a)throws BadLocationException{if(s!=null&&ok(fb,o,s,l))super.replace(fb,o,l,s,a);}
        public void remove(FilterBypass fb,int o,int l)throws BadLocationException{super.remove(fb,o,l);}
        private boolean ok(FilterBypass fb,int o,String s,int l)throws BadLocationException{
            String c=fb.getDocument().getText(0,fb.getDocument().getLength());
            return (c.substring(0,o)+s+c.substring(o+l)).matches("[0-9]*");
        }
    }
    private record DocListener(Runnable fn) implements javax.swing.event.DocumentListener {
        public void insertUpdate(javax.swing.event.DocumentEvent e){fn.run();}
        public void removeUpdate(javax.swing.event.DocumentEvent e){fn.run();}
        public void changedUpdate(javax.swing.event.DocumentEvent e){fn.run();}
    }

    public void iniciar(){SwingUtilities.invokeLater(()->setVisible(true));}
    public static void main(String[] args){
        try{for(UIManager.LookAndFeelInfo i:UIManager.getInstalledLookAndFeels())if("Nimbus".equals(i.getName())){UIManager.setLookAndFeel(i.getClassName());break;}}
        catch(Exception e){try{UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());}catch(Exception ex){}}
        new CalculadoraGUI().iniciar();
    }
}
