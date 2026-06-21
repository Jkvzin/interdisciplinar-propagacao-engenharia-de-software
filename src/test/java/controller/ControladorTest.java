package controller;

import model.LinkBudget;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Testes unitarios para o Controlador.
 * Cobre processamento de calculo, validacao de alertas,
 * tratamento de erros e nomes amigaveis.
 */
@DisplayName("Controlador")
class ControladorTest {

    private Controlador ctrl;

    @BeforeEach
    void setUp() {
        ctrl = new Controlador();
    }

    // ─── Helper ──────────────────────────────────────────────────────────

    private Map<String, Double> mapa(String faltante) {
        Map<String, Double> m = new HashMap<>();
        m.put("Ptx",   faltante.equals("Ptx")   ? null : 3.0);
        m.put("S",     faltante.equals("S")     ? null : -28.0);
        m.put("alpha", faltante.equals("alpha") ? null : 0.35);
        m.put("d",     faltante.equals("d")     ? null : 10.0);
        m.put("N",     faltante.equals("N")     ? null : 32.0);
        m.put("Pcon",  faltante.equals("Pcon")  ? null : 1.5);
        m.put("M",     faltante.equals("M")     ? null : 3.0);
        return m;
    }

    // ─── Calculo basico ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Processamento de calculo")
    class Processamento {

        @Test
        @DisplayName("Calcula Ptx corretamente")
        void calcularPtx() {
            ResultadoCalculo r = ctrl.processarCalculo(mapa("Ptx"));
            assertFalse(Double.isNaN(r.getValor()), "Valor nao pode ser NaN");
            assertEquals("Ptx", r.getVariavel());
            assertTrue(r.getValor() > 0, "Ptx deve ser positiva");
        }

        @Test
        @DisplayName("Calcula distancia corretamente")
        void calcularDistancia() {
            ResultadoCalculo r = ctrl.processarCalculo(mapa("d"));
            assertEquals("d", r.getVariavel());
            assertTrue(r.getValor() >= 0, "Distancia nao pode ser negativa");
            assertEquals(10.0, r.getValor(), 2.0, "Distancia ~10 km");
        }

        @Test
        @DisplayName("Calcula margem corretamente")
        void calcularMargem() {
            ResultadoCalculo r = ctrl.processarCalculo(mapa("M"));
            assertEquals("M", r.getVariavel());
            assertFalse(Double.isNaN(r.getValor()));
            assertEquals(3.0, r.getValor(), 5.0);
        }

        @Test
        @DisplayName("Resultado inclui nome da variavel calculada")
        void resultadoContemVariavel() {
            ResultadoCalculo r = ctrl.processarCalculo(mapa("S"));
            assertEquals("S", r.getVariavel());
            assertEquals(-28.0, r.getValor(), 2.0);
        }
    }

    // ─── Alertas de validacao ────────────────────────────────────────────

    @Nested
    @DisplayName("Alertas de validacao")
    class Alertas {

        @Test
        @DisplayName("Cenario dentro do padrao nao gera alertas de violacao")
        void dentroDoPadrao() {
            ResultadoCalculo r = ctrl.processarCalculo(mapa("Ptx"));
            // Deve ter a mensagem "tudo OK" e nenhum alerta de violacao
            boolean temMensagemOk = r.getAlertas().stream()
                .anyMatch(a -> a.contains("dentro dos padroes"));
            assertTrue(temMensagemOk, "Deve conter mensagem de conformidade");
        }

        @Test
        @DisplayName("Potencia Tx fora do padrao gera alerta")
        void alertaPotenciaTxFora() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 10.0);    // fora do padrao (+1.5 a +5)
            p.put("S", -28.0);
            p.put("alpha", 0.35);
            p.put("d", 10.0);
            p.put("N", 32.0);
            p.put("Pcon", 1.5);
            p.put("M", null);      // calcular margem

            ResultadoCalculo r = ctrl.processarCalculo(p);
            boolean temAlertaPtx = r.getAlertas().stream()
                .anyMatch(a -> a.contains("Potencia de transmissao") && a.contains("fora do padrao"));
            assertTrue(temAlertaPtx, "Deve alertar sobre Ptx fora do padrao");
        }

        @Test
        @DisplayName("Sensibilidade acima do limite gera alerta")
        void alertaSensibilidade() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 3.0);
            p.put("S", -20.0);     // acima do limite (-28 dBm)
            p.put("alpha", 0.35);
            p.put("d", 10.0);
            p.put("N", 32.0);
            p.put("Pcon", 1.5);
            p.put("M", null);

            ResultadoCalculo r = ctrl.processarCalculo(p);
            boolean temAlertaS = r.getAlertas().stream()
                .anyMatch(a -> a.contains("Sensibilidade") && a.contains("acima do limite"));
            assertTrue(temAlertaS, "Deve alertar sobre sensibilidade alta");
        }

        @Test
        @DisplayName("Distancia acima do limite gera alerta")
        void alertaDistancia() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 3.0);
            p.put("S", -28.0);
            p.put("alpha", 0.35);
            p.put("d", 50.0);      // > 20 km
            p.put("N", 32.0);
            p.put("Pcon", 1.5);
            p.put("M", null);

            ResultadoCalculo r = ctrl.processarCalculo(p);
            boolean temAlertaD = r.getAlertas().stream()
                .anyMatch(a -> a.contains("Distancia") && a.contains("excede"));
            assertTrue(temAlertaD, "Deve alertar sobre distancia excessiva");
        }

        @Test
        @DisplayName("Margem negativa gera alerta de enlace inviavel")
        void alertaMargemNegativa() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 3.0);
            p.put("S", -28.0);
            p.put("alpha", 0.35);
            p.put("d", 80.0);
            p.put("N", 128.0);
            p.put("Pcon", 3.0);
            p.put("M", null);

            ResultadoCalculo r = ctrl.processarCalculo(p);
            boolean temAlertaM = r.getAlertas().stream()
                .anyMatch(a -> a.contains("Margem") && a.contains("negativa"));
            assertTrue(temAlertaM, "Deve alertar sobre margem negativa (enlace inviavel)");
        }
    }

    // ─── Tratamento de erros ─────────────────────────────────────────────

    @Nested
    @DisplayName("Tratamento de erros")
    class Erros {

        @Test
        @DisplayName("Mapa nulo lanca excecao")
        void mapaNulo() {
            assertThrows(IllegalArgumentException.class,
                () -> ctrl.processarCalculo(null));
        }

        @Test
        @DisplayName("Multiplas variaveis faltantes retorna erro amigavel")
        void multiplasFaltantes() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", null);
            p.put("S", -28.0);
            p.put("alpha", 0.35);
            p.put("d", null);       // duas faltando
            p.put("N", 32.0);
            p.put("Pcon", 1.5);
            p.put("M", 3.0);

            ResultadoCalculo r = ctrl.processarCalculo(p);
            assertTrue(Double.isNaN(r.getValor()), "Valor deve ser NaN em caso de erro");
            assertEquals("ERRO", r.getVariavel());
            assertFalse(r.getAlertas().isEmpty(), "Deve conter mensagem de erro");
            assertTrue(r.getAlertas().get(0).contains("faltando"),
                "Mensagem deve mencionar variaveis faltantes");
        }

        @Test
        @DisplayName("Resultado NaN retorna alerta de erro")
        void resultadoComErro() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 3.0);
            p.put("S", -28.0);
            p.put("alpha", 0.0);    // forca divisao por zero se d for null
            p.put("d", null);
            p.put("N", 32.0);
            p.put("Pcon", 1.5);
            p.put("M", 3.0);

            ResultadoCalculo r = ctrl.processarCalculo(p);
            // Com alpha=0 e d null, calcularDistancia faz (Ptx-S-splitter-Pcon-M)/0 = Infinity
            // LinkBudget deve lancar excecao por valor infinito
            assertTrue(r.temAlertas() || Double.isNaN(r.getValor()),
                "Deve reportar erro para divisao por zero");
        }
    }

    // ─── Nomes amigaveis ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Nomes amigaveis e unidades")
    class Nomes {

        @Test
        @DisplayName("Retorna nome amigavel para cada variavel")
        void nomesAmigaveis() {
            assertEquals("Potencia de Transmissao", Controlador.nomeAmigavel("Ptx"));
            assertEquals("Sensibilidade do Receptor", Controlador.nomeAmigavel("S"));
            assertEquals("Atenuacao da Fibra", Controlador.nomeAmigavel("alpha"));
            assertEquals("Distancia do Enlace", Controlador.nomeAmigavel("d"));
            assertEquals("Razao de Divisao do Splitter", Controlador.nomeAmigavel("N"));
            assertEquals("Perda por Conectores/Fusoes", Controlador.nomeAmigavel("Pcon"));
            assertEquals("Margem de Seguranca", Controlador.nomeAmigavel("M"));
        }

        @Test
        @DisplayName("Retorna a propria chave para variavel desconhecida")
        void chaveDesconhecida() {
            assertEquals("XYZ", Controlador.nomeAmigavel("XYZ"));
        }

        @Test
        @DisplayName("Retorna unidade correta para cada variavel")
        void unidades() {
            assertEquals("dBm", Controlador.unidade("Ptx"));
            assertEquals("dBm", Controlador.unidade("S"));
            assertEquals("dB/km", Controlador.unidade("alpha"));
            assertEquals("km", Controlador.unidade("d"));
            assertEquals("", Controlador.unidade("N"));
            assertEquals("dBm", Controlador.unidade("Pcon"));
            assertEquals("dBm", Controlador.unidade("M"));
        }
    }

    // ─── ResultadoCalculo ────────────────────────────────────────────────

    @Nested
    @DisplayName("ResultadoCalculo")
    class Resultado {

        @Test
        @DisplayName("Construtor e getters funcionam")
        void construtor() {
            List<String> alertas = Arrays.asList("Alerta 1", "Alerta 2");
            ResultadoCalculo r = new ResultadoCalculo(42.5, "Ptx", alertas);

            assertEquals(42.5, r.getValor(), 0.001);
            assertEquals("Ptx", r.getVariavel());
            assertEquals(2, r.getAlertas().size());
            assertTrue(r.temAlertas());
        }

        @Test
        @DisplayName("Lista de alertas e imutavel")
        void alertasImutaveis() {
            List<String> alertas = new ArrayList<>();
            alertas.add("teste");
            ResultadoCalculo r = new ResultadoCalculo(1.0, "d", alertas);

            assertThrows(UnsupportedOperationException.class,
                () -> r.getAlertas().add("novo"),
                "Lista de alertas deve ser imutavel");
        }

        @Test
        @DisplayName("Sem alertas, temAlertas retorna false")
        void semAlertas() {
            ResultadoCalculo r = new ResultadoCalculo(3.0, "M", Collections.emptyList());
            assertFalse(r.temAlertas());
        }

        @Test
        @DisplayName("toString contem informacoes relevantes")
        void toStringTest() {
            ResultadoCalculo r = new ResultadoCalculo(30.0, "Ptx",
                Collections.singletonList("Alerta teste"));
            String s = r.toString();
            assertTrue(s.contains("Ptx"), "toString deve conter variavel");
            assertTrue(s.contains("30"), "toString deve conter valor");
            assertTrue(s.contains("1"), "toString deve conter quantidade de alertas");
        }
    }
}
