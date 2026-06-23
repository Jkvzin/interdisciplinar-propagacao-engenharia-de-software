package controller;

import model.LinkBudget;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Testes unitarios para o Controlador com formula splitter = 10·log10(N).
 * 
 * <p>Cenario base: Ptx=4.0, S=-28.0, alpha=0.28, d=20, N=32, Pcon=1.0, M=10.35</p>
 */
@DisplayName("Controlador")
class ControladorTest {

    private Controlador ctrl;

    @BeforeEach void setUp() { ctrl = new Controlador(); }

    private Map<String, Double> mapa(String faltante) {
        Map<String, Double> m = new HashMap<>();
        m.put("Ptx",   "Ptx".equals(faltante)   ? null : 4.0);
        m.put("S",     "S".equals(faltante)     ? null : -28.0);
        m.put("alpha", "alpha".equals(faltante) ? null : 0.28);
        m.put("d",     "d".equals(faltante)     ? null : 20.0);
        m.put("N",     "N".equals(faltante)     ? null : 32.0);
        m.put("Pcon",  "Pcon".equals(faltante)  ? null : 1.0);
        m.put("M",     "M".equals(faltante)     ? null : 10.35);
        return m;
    }

    @Nested @DisplayName("Processamento de calculo")
    class Processamento {
        @Test @DisplayName("Ptx") void calcularPtx() {
            ResultadoCalculo r = ctrl.processarCalculo(mapa("Ptx"));
            assertFalse(Double.isNaN(r.getValor()));
            assertEquals("Ptx", r.getVariavel());
            assertEquals(4.0, r.getValor(), 0.5);
        }
        @Test @DisplayName("Distancia") void calcularDistancia() {
            ResultadoCalculo r = ctrl.processarCalculo(mapa("d"));
            assertEquals("d", r.getVariavel());
            assertEquals(20.0, r.getValor(), 1.0);
        }
        @Test @DisplayName("Margem") void calcularMargem() {
            ResultadoCalculo r = ctrl.processarCalculo(mapa("M"));
            assertEquals("M", r.getVariavel());
            assertEquals(10.35, r.getValor(), 2.0);
        }
        @Test @DisplayName("Sensibilidade") void resultadoContemVariavel() {
            ResultadoCalculo r = ctrl.processarCalculo(mapa("S"));
            assertEquals("S", r.getVariavel());
            assertEquals(-28.0, r.getValor(), 0.5);
        }
    }

    @Nested @DisplayName("Alertas de validacao")
    class Alertas {
        @Test @DisplayName("Cenario dentro do padrao")
        void dentroDoPadrao() {
            ResultadoCalculo r = ctrl.processarCalculo(mapa("Ptx"));
            boolean ok = r.getAlertas().stream().anyMatch(a -> a.contains("dentro dos padroes"));
            boolean semViolacao = r.getAlertas().stream()
                .noneMatch(a -> a.contains("abaixo") || a.contains("acima")
                             || a.contains("negativa") || a.contains("inviavel"));
            assertTrue(ok || semViolacao);
        }
        @Test @DisplayName("Ptx acima do maximo")
        void alertaPotenciaTxFora() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 10.0); p.put("S", -28.0); p.put("alpha", 0.28);
            p.put("d", 10.0); p.put("N", 4.0); p.put("Pcon", 0.5); p.put("M", null);
            ResultadoCalculo r = ctrl.processarCalculo(p);
            assertTrue(r.getAlertas().stream()
                .anyMatch(a -> a.contains("Potencia de transmissao") && a.contains("acima")));
        }
        @Test @DisplayName("Sensibilidade acima do limite")
        void alertaSensibilidade() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 3.0); p.put("S", -20.0); p.put("alpha", 0.28);
            p.put("d", 1.0); p.put("N", 2.0); p.put("Pcon", 0.5); p.put("M", null);
            ResultadoCalculo r = ctrl.processarCalculo(p);
            assertTrue(r.getAlertas().stream()
                .anyMatch(a -> a.contains("Sensibilidade") && a.contains("acima do limite")));
        }
        @Test @DisplayName("Distancia acima do limite")
        void alertaDistancia() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 3.0); p.put("S", -28.0); p.put("alpha", 0.28);
            p.put("d", 50.0); p.put("N", 2.0); p.put("Pcon", 0.5); p.put("M", null);
            ResultadoCalculo r = ctrl.processarCalculo(p);
            assertTrue(r.getAlertas().stream()
                .anyMatch(a -> a.contains("Distancia") && a.contains("excede")));
        }
        @Test @DisplayName("Margem negativa")
        void alertaMargemNegativa() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 3.0); p.put("S", -28.0); p.put("alpha", 0.28);
            p.put("d", 20.0); p.put("N", 512.0); p.put("Pcon", 3.0); p.put("M", null);
            // M = 31 - 5.6 - 27.09 - 3 = -4.69 → erro do LinkBudget
            ResultadoCalculo r = ctrl.processarCalculo(p);
            assertTrue(r.temAlertas());
        }
    }

    @Nested @DisplayName("Tratamento de erros")
    class Erros {
        @Test @DisplayName("Mapa nulo")
        void mapaNulo() {
            assertThrows(IllegalArgumentException.class, () -> ctrl.processarCalculo(null));
        }
        @Test @DisplayName("Multiplas faltantes")
        void multiplasFaltantes() {
            Map<String, Double> p = mapa(null);
            p.put("Ptx", null); p.put("d", null);
            ResultadoCalculo r = ctrl.processarCalculo(p);
            assertTrue(Double.isNaN(r.getValor()));
            assertEquals("ERRO", r.getVariavel());
            assertTrue(r.getAlertas().get(0).contains("faltando"));
        }
        @Test @DisplayName("Divisao por zero")
        void resultadoComErro() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 4.0); p.put("S", -28.0); p.put("alpha", 0.0);
            p.put("d", null); p.put("N", 32.0); p.put("Pcon", 1.0); p.put("M", 10.35);
            ResultadoCalculo r = ctrl.processarCalculo(p);
            assertTrue(r.temAlertas() || Double.isNaN(r.getValor()));
        }
    }

    @Nested @DisplayName("Nomes e unidades")
    class Nomes {
        @Test @DisplayName("Nomes") void nomesAmigaveis() {
            assertEquals("Potencia de Transmissao", Controlador.nomeAmigavel("Ptx"));
            assertEquals("Sensibilidade do Receptor", Controlador.nomeAmigavel("S"));
            assertEquals("Atenuacao da Fibra", Controlador.nomeAmigavel("alpha"));
            assertEquals("Distancia do Enlace", Controlador.nomeAmigavel("d"));
            assertEquals("Razao de Divisao do Splitter", Controlador.nomeAmigavel("N"));
            assertEquals("Perda por Conectores/Fusoes", Controlador.nomeAmigavel("Pcon"));
            assertEquals("Margem de Seguranca", Controlador.nomeAmigavel("M"));
        }
        @Test @DisplayName("Desconhecida") void chaveDesconhecida() {
            assertEquals("XYZ", Controlador.nomeAmigavel("XYZ"));
        }
        @Test @DisplayName("Unidades") void unidades() {
            assertEquals("dBm", Controlador.unidade("Ptx"));
            assertEquals("dBm", Controlador.unidade("S"));
            assertEquals("dB/km", Controlador.unidade("alpha"));
            assertEquals("km", Controlador.unidade("d"));
            assertEquals("", Controlador.unidade("N"));
            assertEquals("dBm", Controlador.unidade("Pcon"));
            assertEquals("dBm", Controlador.unidade("M"));
        }
    }

    @Nested @DisplayName("ResultadoCalculo")
    class Resultado {
        @Test @DisplayName("Construtor") void construtor() {
            ResultadoCalculo r = new ResultadoCalculo(42.5, "Ptx", Arrays.asList("A1", "A2"));
            assertEquals(42.5, r.getValor(), 0.001);
            assertEquals("Ptx", r.getVariavel());
            assertEquals(2, r.getAlertas().size());
            assertTrue(r.temAlertas());
        }
        @Test @DisplayName("Imutavel") void alertasImutaveis() {
            List<String> a = new ArrayList<>(); a.add("t");
            ResultadoCalculo r = new ResultadoCalculo(1.0, "d", a);
            assertThrows(UnsupportedOperationException.class, () -> r.getAlertas().add("n"));
        }
        @Test @DisplayName("Sem alertas") void semAlertas() {
            assertFalse(new ResultadoCalculo(3.0, "M", Collections.emptyList()).temAlertas());
        }
        @Test @DisplayName("toString") void toStringTest() {
            String s = new ResultadoCalculo(30.0, "Ptx", Collections.singletonList("Alerta")).toString();
            assertTrue(s.contains("Ptx") && s.contains("30") && s.contains("1"));
        }
    }
}
