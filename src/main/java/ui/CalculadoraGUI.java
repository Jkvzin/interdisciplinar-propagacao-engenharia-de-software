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
 * Calculadora de Link Budget GPON com acessibilidade.
 *
 * <p>Barra superior com A-/A+ (fonte) e toggle de alto contraste.
 * Layout em coluna unica com scroll. Suporte a leitores de tela
 * (NVDA/JAWS) e navegacao completa por teclado (Alt+letra).</p>
 */
public class CalculadoraGUI extends JFrame {

    private final Controlador controlador = new Controlador();
    private final Equipamento equipamento = new Equipamento();
    private final Map<String, JTextField> campos = new HashMap<>();

    // Componentes
    private JComboBox<String> comboWL, comboS1, comboS2;
    private JTextField campoAlpha, campoNCon, campoPCon, campoNFus, campoPFus;
    private JCheckBox chkS2;
    private JLabel labelPconTotal;
    private JTextArea areaResultado, areaAlertas;
    private JButton btnCalcular;

    // Acessibilidade
    private int nivelFonte = 1;
    private boolean altoContraste = false;
    private final List<JComponent> todosComp = new ArrayList<>();
    private final List<JLabel> todosLabels = new ArrayList<>();
    private final List<JPanel> secoes = new ArrayList<>();

    private static final int[] FS = {12, 18, 24}, FT = {13, 20, 26}, FB = {14, 22, 28}, FBTN = {16, 24, 32};
    private static final String[] SPLIT = {"1:2","1:4","1:8","1:16","1:32","1:64"};
    private static final double[] SPLIT_V = {2,4,8,16,32,64};
    private static final String[] WL = {"1490 nm (Downstream)","1310 nm (Upstream)"};
    private static final int[] WL_V = {1490,1310};

    public CalculadoraGUI() {
        setTitle("Calculadora de Link Budget — GPON");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(720, 800);
        setMinimumSize(new Dimension(620, 700));
        setLocationRelativeTo(null);
        construir();
        aplicarTema();
    }

    private void construir() {
        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Barra de acessibilidade
        root.add(criarBarraAcessibilidade(), BorderLayout.NORTH);

        // Entrada com scroll
        JPanel entrada = criarPainelEntrada();
        JScrollPane scrollEntrada = new JScrollPane(entrada);
        scrollEntrada.setBorder(null);
        scrollEntrada.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollEntrada.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollEntrada.getVerticalScrollBar().setUnitIncrement(20);
        root.add(scrollEntrada, BorderLayout.CENTER);

        // Sul: botao + resultado
        JPanel sul = new JPanel(new BorderLayout(0, 4));
        sul.setOpaque(false);

        btnCalcular = criarBotaoCalcular();
        JPanel pb = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        pb.setOpaque(false);
        pb.add(btnCalcular);
        sul.add(pb, BorderLayout.NORTH);
        sul.add(criarPainelSaida(), BorderLayout.CENTER);
        root.add(sul, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnCalcular);
        getRootPane().registerKeyboardAction(e -> onCalcular(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        add(root);
    }

    // ═══════════ BARRA ACESSIBILIDADE ═══════════

    private JPanel criarBarraAcessibilidade() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        barra.setBorder(new EmptyBorder(2, 0, 4, 0));

        JButton btnMenos = new JButton("A−");
        btnMenos.setToolTipText("Diminuir tamanho da fonte");
        btnMenos.addActionListener(e -> { if (nivelFonte > 0) { nivelFonte--; aplicarTema(); }});

        JButton btnMais = new JButton("A+");
        btnMais.setToolTipText("Aumentar tamanho da fonte");
        btnMais.addActionListener(e -> { if (nivelFonte < 2) { nivelFonte++; aplicarTema(); }});

        JToggleButton btnContraste = new JToggleButton("◐ Alto Contraste");
        btnContraste.setToolTipText("Alterna modo alto contraste (fundo escuro, texto claro)");
        btnContraste.addItemListener(e -> {
            altoContraste = e.getStateChange() == ItemEvent.SELECTED;
            aplicarTema();
        });

        barra.add(new JLabel("Acessibilidade:"));
        barra.add(btnMenos);
        barra.add(btnMais);
        barra.add(btnContraste);

        todosComp.add(btnMenos);
        todosComp.add(btnMais);
        todosComp.add(btnContraste);
        return barra;
    }

    // ═══════════ PAINEL ENTRADA ═══════════

    private JPanel criarPainelEntrada() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.add(secao("Comprimento de Onda e Fibra", this::wlFibra));
        painel.add(Box.createVerticalStrut(5));
        painel.add(secao("Parametros do Enlace", this::paramsEnlace));
        painel.add(Box.createVerticalStrut(5));
        painel.add(secao("Divisores Opticos (Splitters)", this::splitters));
        painel.add(Box.createVerticalStrut(5));
        painel.add(secao("Conectores e Fusoes", this::conectores));
        painel.add(Box.createVerticalStrut(5));
        painel.add(secao("Margem de Seguranca", this::margem));
        return painel;
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

        g.gridy=0; g.gridx=0; g.weightx=0;
        p.add(label("Numero de conectores:", 'C'), g);
        g.gridx=1; g.weightx=0.3;
        campoNCon = campoInt("2", "Numero de conectores", "Quantidade de conectores no enlace");
        p.add(campoNCon, g);

        g.gridx=2; g.weightx=0;
        p.add(label("  Perda por conector (dB):", 'E'), g);
        g.gridx=3; g.weightx=0.3;
        campoPCon = campoDecimal(String.valueOf(equipamento.getPerdaPorConector()), null,
            "Perda por conector", "Tipico: 0.5 dB (SC/APC)");
        p.add(campoPCon, g);

        g.gridy=1; g.gridx=0; g.weightx=0;
        p.add(label("Numero de fusoes:", 'F'), g);
        g.gridx=1; g.weightx=0.3;
        campoNFus = campoInt("4", "Numero de fusoes", "Quantidade de emendas por fusao");
        p.add(campoNFus, g);

        g.gridx=2; g.weightx=0;
        p.add(label("  Perda por fusao (dB):", 'U'), g);
        g.gridx=3; g.weightx=0.3;
        campoPFus = campoDecimal(String.valueOf(equipamento.getPerdaPorFusao()), null,
            "Perda por fusao", "Tipico: 0.05 a 0.1 dB");
        p.add(campoPFus, g);

        g.gridy=2; g.gridx=0; g.gridwidth=4; g.weightx=1;
        labelPconTotal = new JLabel("Perda total: 1.4 dB");
        labelPconTotal.setToolTipText("Total = conectores x perda/conector + fusoes x perda/fusao");
        p.add(labelPconTotal, g);
    }

    private void margem(JPanel p) {
        GridBagConstraints g = gbc();
        addCampo(p, g, 0, "Margem de Seguranca (dB):", 'M', "M",
            "Folga para variacoes ambientais. Minimo recomendado: 3 dB");
        campos.get("M").setText(String.valueOf(equipamento.getMargem()));
    }

    // ═══════════ PAINEL SAIDA ═══════════

    private JPanel criarPainelSaida() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setOpaque(false);

        areaResultado = new JTextArea(2, 40);
        areaResultado.setEditable(false);
        areaResultado.setLineWrap(true);
        areaResultado.setWrapStyleWord(true);
        areaResultado.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("Resultado"), new EmptyBorder(6, 10, 6, 10)));
        acessivel(areaResultado, "Resultado", "Valor calculado da variavel");

        areaAlertas = new JTextArea(5, 40);
        areaAlertas.setEditable(false);
        areaAlertas.setLineWrap(true);
        areaAlertas.setWrapStyleWord(true);
        areaAlertas.setBorder(BorderFactory.createCompoundBorder(
            new TitledBorder("Alertas / Avisos"), new EmptyBorder(6, 10, 6, 10)));
        acessivel(areaAlertas, "Alertas", "Alertas de validacao ITU-T");

        JScrollPane sp = new JScrollPane(areaAlertas);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(560, 110));

        p.add(areaResultado, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ═══════════ BOTAO CALCULAR ═══════════

    private JButton criarBotaoCalcular() {
        JButton b = new JButton("Calcular");
        b.setMnemonic('C');
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

    // ═══════════ TEMA ═══════════

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
        for (JTextField tf : campos.values()) { tf.setFont(fp); tf.setForeground(fg); tf.setBackground(fieldBg); tf.setCaretColor(fg); }
        for (JTextField tf : new JTextField[]{campoAlpha,campoNCon,campoPCon,campoNFus,campoPFus})
            if (tf != null) { tf.setFont(fp); tf.setForeground(fg); tf.setBackground(fieldBg); tf.setCaretColor(fg); }
        for (JComboBox<?> cb : new JComboBox[]{comboWL,comboS1,comboS2})
            if (cb != null) { cb.setFont(fp); cb.setForeground(fg); cb.setBackground(fieldBg); }
        if (chkS2 != null) { chkS2.setFont(fp); chkS2.setForeground(fg); }
        for (JComponent c : todosComp) c.setFont(fp);

        btnCalcular.setFont(fbutton);
        btnCalcular.setBackground(btnCalcBg);
        btnCalcular.setForeground(btnCalcFg);
        btnCalcular.setBorder(new LineBorder(btnCalcBg.darker(), altoContraste?3:2));
        btnCalcular.setPreferredSize(new Dimension(260, 40 + nivelFonte*12));

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
                tb.setTitleFont(ftitle); tb.setTitleColor(accent);
            }
        }

        getContentPane().setBackground(bg);
        revalidate(); repaint();
    }

    // ═══════════ HELPERS ═══════════

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
        labelPconTotal.setText(String.format("Perda total: %.2f dB  (%d con. x %.1f + %d fus. x %.1f)", t, nc, pc, nf, pf));
    }

    // ═══════════ CALCULO ═══════════

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
            catch(NumberFormatException ex){err("Valor invalido em "+nome(e.getKey())+": "+e.getValue());return;}
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

    // ═══════════ FILTROS ═══════════

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
