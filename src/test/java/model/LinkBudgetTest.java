package model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitarios para LinkBudget — formula splitter = 10·log10(N).
 * 
 * <p>Cenario base (ITU-T G.984.2 / G.652):</p>
 * <ul>
 *   <li>Ptx=4.0 dBm, S=-28.0 dBm, alpha=0.28 dB/km, d=20 km, N=32, Pcon=1.0 dB, M=10.35 dB</li>
 *   <li>perdaSplitter = 10·log10(32) ≈ 15.05 dB</li>
 *   <li>Orcamento = 32 dB, Perdas = 5.6+15.05+1.0+10.35 = 32.0 dB ✓</li>
 * </ul>
 */
@DisplayName("LinkBudget")
class LinkBudgetTest {

    private final LinkBudget lb = new LinkBudget();

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

    @Nested @DisplayName("Calculo isolado de cada variavel")
    class CalculoIsolado {
        @Test @DisplayName("Ptx") void calcularPotenciaTx() {
            double r = lb.calcular(mapa("Ptx"));
            assertEquals(4.0, r, 0.5);
            assertTrue(r > 0 && r < 50);
        }
        @Test @DisplayName("S") void calcularSensibilidade() {
            double r = lb.calcular(mapa("S"));
            assertEquals(-28.0, r, 0.5);
            assertTrue(r < -10 && r > -50);
        }
        @Test @DisplayName("alpha") void calcularAtenuacao() {
            double r = lb.calcular(mapa("alpha"));
            assertEquals(0.28, r, 0.1);
            assertTrue(r > 0);
        }
        @Test @DisplayName("d") void calcularDistancia() {
            double r = lb.calcular(mapa("d"));
            assertEquals(20.0, r, 1.0);
            assertTrue(r >= 0);
        }
        @Test @DisplayName("N") void calcularDivisaoSplitter() {
            double r = lb.calcular(mapa("N"));
            assertEquals(32.0, r, 2.0);
            assertTrue(r >= 2);
        }
        @Test @DisplayName("Pcon") void calcularPerdaConectores() {
            double r = lb.calcular(mapa("Pcon"));
            assertEquals(1.0, r, 1.0);
            assertTrue(r >= 0);
        }
        @Test @DisplayName("M") void calcularMargem() {
            double r = lb.calcular(mapa("M"));
            assertEquals(10.35, r, 2.0);
            assertFalse(Double.isNaN(r));
        }
    }

    @Nested @DisplayName("Cenarios GPON reais")
    class CenariosReais {
        @Test @DisplayName("Cenario tipico Classe B+ com splitter 1:32")
        void cenarioTipico() {
            double ptx = lb.calcular(mapa("Ptx"));
            assertEquals(4.0, ptx, 0.5);
        }
        @Test @DisplayName("Splitter 1:8 — enlace mais curto")
        void cenarioSplitter8() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", null); p.put("S", -28.0); p.put("alpha", 0.28);
            p.put("d", 10.0); p.put("N", 8.0); p.put("Pcon", 1.0); p.put("M", 3.0);
            // 10*log10(8)=9.03, total=2.8+9.03+1+3=15.83, Ptx=-28+15.83=-12.17
            assertEquals(-12.17, lb.calcular(p), 1.0);
        }
        @Test @DisplayName("Margem negativa lanca excecao (enlace inviavel)")
        void margemNegativaEnlaceInviavel() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 3.0); p.put("S", -28.0); p.put("alpha", 0.28);
            p.put("d", 5.0); p.put("N", 512.0); p.put("Pcon", 3.0); p.put("M", null);
            // 10*log10(512)=27.09, M=31-1.4-27.09-3=-0.49  → negativo!
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> lb.calcular(p));
            assertTrue(ex.getMessage().contains("negativa") || ex.getMessage().contains("inviavel"));
        }
    }

    @Nested @DisplayName("Tratamento de erros")
    class TratamentoErros {
        @Test @DisplayName("2+ faltantes")
        void multiplasFaltantes() {
            Map<String, Double> p = mapa(null);
            p.put("Ptx", null); p.put("d", null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> lb.calcular(p));
            assertTrue(ex.getMessage().contains("2"));
        }
        @Test @DisplayName("Nenhuma faltante")
        void nenhumaFaltante() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 4.0); p.put("S", -28.0); p.put("alpha", 0.28);
            p.put("d", 20.0); p.put("N", 32.0); p.put("Pcon", 1.0); p.put("M", 10.35);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> lb.calcular(p));
            assertTrue(ex.getMessage().contains("Nenhuma"));
        }
        @Test @DisplayName("Mapa nulo")
        void mapaNulo() {
            assertThrows(NullPointerException.class, () -> lb.calcular(null));
        }
        @Test @DisplayName("Chave ausente")
        void chaveAusente() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", null);
            assertThrows(IllegalArgumentException.class, () -> lb.calcular(p));
        }
        @Test @DisplayName("Distancia negativa")
        void distanciaNegativa() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 1.0); p.put("S", -28.0); p.put("alpha", 0.28);
            p.put("d", null); p.put("N", 1024.0); p.put("Pcon", 0.0); p.put("M", 0.0);
            // 10*log10(1024)=30.10, d=(29-30.10)/0.28=-3.93 → negativo
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> lb.calcular(p));
            assertTrue(ex.getMessage().contains("negativa"));
        }
        @Test @DisplayName("Margem negativa")
        void margemNegativa() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 3.0); p.put("S", -28.0); p.put("alpha", 0.28);
            p.put("d", 20.0); p.put("N", 512.0); p.put("Pcon", 3.0); p.put("M", null);
            // 10*log10(512)=27.09, M=31-5.6-27.09-3=-4.69
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> lb.calcular(p));
            assertTrue(ex.getMessage().contains("negativa") || ex.getMessage().contains("inviavel"));
        }
        @Test @DisplayName("N < 2")
        void divisaoSplitterInvalida() {
            Map<String, Double> p = new HashMap<>();
            p.put("Ptx", 3.0); p.put("S", -28.0); p.put("alpha", 0.28);
            p.put("d", 1.0); p.put("N", null); p.put("Pcon", 30.0); p.put("M", 0.0);
            // perda = 31-0.28-30-0=0.72, N=10^(0.72/10)=1.18 < 2
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> lb.calcular(p));
            assertTrue(ex.getMessage().contains("2") || ex.getMessage().contains("minimo"));
        }
    }

    @Nested @DisplayName("Consistencia")
    class Consistencia {
        @Test @DisplayName("Round-trip Ptx→S")
        void roundTripPtx() {
            Map<String, Double> p = mapa("Ptx");
            p.put("Ptx", lb.calcular(p)); p.put("S", null);
            assertEquals(-28.0, lb.calcular(p), 0.01);
        }
        @Test @DisplayName("Round-trip d→Ptx")
        void roundTripDistancia() {
            Map<String, Double> p = mapa("d");
            p.put("d", lb.calcular(p)); p.put("Ptx", null);
            assertEquals(4.0, lb.calcular(p), 0.01);
        }
        @Test @DisplayName("Round-trip N→M")
        void roundTripSplitter() {
            Map<String, Double> p = mapa("N");
            p.put("N", lb.calcular(p)); p.put("M", null);
            assertEquals(10.35, lb.calcular(p), 0.01);
        }
    }

    @Test @DisplayName("Idempotencia")
    void idempotencia() {
        double r1 = lb.calcular(mapa("d"));
        double r2 = lb.calcular(mapa("d"));
        assertEquals(r1, r2, 0.0001);
        assertEquals(r2, lb.calcular(mapa("d")), 0.0001);
    }
}
