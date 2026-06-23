# Calculadora de Link Budget GPON v1.1

Projeto Interdisciplinar — Propagação de Ondas Eletromagnéticas × Engenharia de Software

Calculadora de Link Budget para redes GPON (ITU-T G.984.2 / G.652). Permite dimensionar enlaces ópticos de forma interativa, isolando qualquer variável da equação fundamental de propagação.

---

## Equação Fundamental

```
Ptx − S = α × d + 10·log₁₀(N) + Pcon + M
```

| Símbolo | Descrição | Unidade | Valores Típicos |
|---------|-----------|---------|-----------------|
| Ptx | Potência de transmissão (OLT) | dBm | +1.5 a +5 |
| S | Sensibilidade do receptor (ONU) | dBm | −28 (Classe B+) |
| α | Atenuação da fibra G.652 | dB/km | 0.28 (1490 nm) / 0.35 (1310 nm) |
| d | Distância do enlace | km | máx 20 (B+), máx 60 (C+) |
| N | Razão de divisão do splitter | — | 1:2 a 1:64 (cascata suportada) |
| Pcon | Perda por conectores + fusões | dB | 0.5 por conector / 0.1 por fusão |
| M | Margem de segurança | dB | ≥ 3 (recomendado) |

---

## Como Usar

### No Windows
Dê 2 cliques no arquivo **`rodar.bat`**

### No Linux / Mac
```bash
./rodar.sh
```

### Se não abrir
Instale o Java: https://adoptium.net/

---

## Funcionamento

1. Preencha os campos com os valores que você conhece
2. Deixe **UM** campo em branco — é o que você quer calcular
3. Escolha o comprimento de onda (1490 nm downstream / 1310 nm upstream)
4. Selecione o splitter e, opcionalmente, um segundo splitter em cascata
5. Informe o número de conectores e fusões (a perda total é calculada automaticamente)
6. Clique em **Calcular** (ou `Ctrl+Enter`)
7. O resultado aparece destacado em verde
8. Alertas de validação ITU-T aparecem abaixo, se houver

---

## O que Dá pra Calcular

Deixe **UM** destes campos vazio e clique em Calcular:

| Campo | O sistema calcula |
|-------|-------------------|
| Potência de Transmissão | Qual Ptx você precisa |
| Sensibilidade do Receptor | Qual sensibilidade a ONU precisa ter |
| Distância | Qual o alcance máximo do enlace |
| Atenuação da Fibra | Qual atenuação a fibra suporta |
| Margem de Segurança | Qual a folga do enlace |

**Exemplo prático:** Você sabe a potência (+3 dBm), a sensibilidade (−28 dBm), a distância (10 km), o splitter (1:32) e os conectores (2 conectores × 0.5 dB + 4 fusões × 0.1 dB = 1.4 dB), mas quer saber a **margem de segurança**. Deixe o campo "Margem" em branco e clique Calcular.

---

## Acessibilidade

A calculadora foi projetada com foco em acessibilidade:

| Recurso | Como usar |
|---------|-----------|
| **A− / A+** | 3 tamanhos de fonte (12pt / 18pt / 24pt) — a janela redimensiona junto |
| **Alto Contraste** | Fundo escuro com texto claro e bordas grossas |
| **Teclado** | `Alt+letra` em cada campo, `Tab` para navegar, `Ctrl+Enter` para calcular |
| **Leitor de tela** | Todos os campos têm nomes e descrições acessíveis (NVDA / JAWS) |
| **Tooltips** | Passe o mouse sobre qualquer campo para ver explicação |

---

## Classes de Operação GPON

| Classe | Atenuação Máx | Alcance Típico | Splitter Típico |
|--------|--------------|----------------|-----------------|
| B+ | 28 dB | ~20 km | 1:32 |
| C+ | 32 dB | ~60 km | 1:64 |

---

## Estrutura do Projeto

```
.
├── pom.xml
├── README.md
├── PRD.md
├── rodar.bat / rodar.sh
├── docs/
│   ├── diagrama_classes.png
│   ├── diagrama_caso_uso.png
│   ├── documento_requisitos.md
│   └── requisitos_gpon_calculadora.pdf
├── src/
│   ├── main/java/
│   │   ├── model/
│   │   │   ├── LinkBudget.java        # Motor de cálculo
│   │   │   ├── Equipamento.java       # Parâmetros físicos GPON
│   │   │   └── Validador.java         # Validação ITU-T G.984 / G.652
│   │   ├── controller/
│   │   │   ├── Controlador.java       # Ponte Model ↔ View
│   │   │   └── ResultadoCalculo.java  # DTO de resultado
│   │   └── ui/
│   │       └── CalculadoraGUI.java    # Interface Swing com acessibilidade
│   └── test/java/
│       ├── model/
│       │   └── LinkBudgetTest.java    # 21 testes unitários
│       └── controller/
│           └── ControladorTest.java   # 19 testes unitários
```

---

## Arquitetura (MVC)

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│    Model      │     │      View         │     │  Controller   │
│  LinkBudget   │────▶│ CalculadoraGUI     │◀────│ Controlador   │
│  Equipamento  │     │  Swing + A11y      │     │               │
│  Validador    │     │  (A−/A+, contraste)│     │               │
└──────────────┘     └──────────────────┘     └──────────────┘
```

---

## Compilar e Testar

```bash
# Compilar
javac -d out src/main/java/model/*.java src/main/java/controller/*.java src/main/java/ui/*.java

# Rodar
java -cp out ui.CalculadoraGUI

# Testes (JUnit 5 — 40 testes)
mvn test
```

---

## Novidades da v1.1

- **Fórmula corrigida:** splitter usa `10·log₁₀(N)` (antes usava log₂ — perdas estavam 3.3× maiores)
- **Comprimento de onda:** seleção 1490 nm (downstream) / 1310 nm (upstream) com alpha automático
- **Splitter secundário:** suporte a dois splitters em cascata
- **Conectores e fusões:** contagem individual com perda por unidade
- **Validador ITU-T:** integrado ao Controlador com alertas de atenuação total
- **Acessibilidade:** A−/A+, alto contraste, teclado completo, leitor de tela
- **Janela responsiva:** redimensiona automaticamente ao mudar o tamanho da fonte

---

## Referências Técnicas

- **ITU-T G.984.2** — GPON: Physical Media Dependent layer
- **ITU-T G.652** — Single-mode optical fibre characteristics
- **ITU-T G.984.1** — GPON: General characteristics

---

## Autores

- João Victor Borges Carvalho ([Jkvzin](https://github.com/Jkvzin))
- Eduardo Tenorio Nunes ([EduardoTenorioNunes](https://github.com/EduardoTenorioNunes))
- João Guilherme Garcia Mangueira ([JoaoGarciaM](https://github.com/JoaoGarciaM))
