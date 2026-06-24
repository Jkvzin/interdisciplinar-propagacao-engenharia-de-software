# PRD — Calculadora de Link Budget GPON

## Projeto Interdisciplinar: Propagacao de Ondas Eletromagneticas × Engenharia de Software

---

## 1. Visao Geral

Este projeto consiste no desenvolvimento de uma **calculadora de Link Budget para redes GPON** (Gigabit Passive Optical Network). A aplicacao deve transformar as leis fisicas de propagacao e perdas de sinal optico em uma logica de software flexivel, com interface grafica, permitindo que um engenheiro de telecomunicacoes dimensione enlaces GPON de forma interativa.

O principal desafio de um projetista de rede GPON e garantir que o sinal saia da OLT (Central) e chegue a ONU (Cliente) com potencia suficiente para ser interpretado pelo receptor, respeitando as perdas intrinsecas dos componentes passivos da rede.

---

## 2. Requisitos Funcionais

### RF1 — Calculo de Variavel Faltante
O sistema deve ser capaz de calcular qualquer variavel da equacao de Link Budget desde que todas as demais sejam fornecidas pelo usuario. O usuario preenche os campos conhecidos e deixa exatamente um campo vazio — o sistema identifica automaticamente qual variavel esta faltando e a calcula.

**Variaveis do sistema:**
- Potencia de Transmissao — Ptx (dBm)
- Sensibilidade do Receptor — S (dBm)
- Distancia do Enlace — d (km)
- Razao de Divisao do Splitter — N (ex: 8 para splitter 1:8)
- Perda por Conectores/Fusoes — Pcon (dB)
- Margem de Seguranca — M (dB)

### RF2 — Validacao contra Normas Tecnicas
O sistema deve validar os valores inseridos e calculados contra os padroes ITU-T G.984 (GPON) e G.652 (fibra optica), gerando alertas visuais quando parametros estiverem fora das especificacoes.

### RF3 — Interface Grafica
O sistema deve prover interface grafica (Java Swing) com campos numericos validados em tempo real, dropdown para selecao do splitter, botao de calculo e area de resultados/alertas com codigo de cores (verde = dentro do padrao, vermelho/alaranjado = alerta).

---

## 3. Requisitos Nao Funcionais

### RNF1 — Arquitetura MVC
Separacao clara entre interface de usuario (View), logica de controle (Controller) e logica matematica de propagacao (Model), conforme padrao MVC.

### RNF2 — Orientacao a Objetos
Implementacao em Java com classes bem definidas: `Equipamento`, `Validador`, `LinkBudget`, `Controlador` e classe da GUI.

### RNF3 — Testes Automatizados
Cobertura de testes unitarios com JUnit 5 para todas as classes de dominio e calculo. Minimo de 2 assertions por metodo de teste.

### RNF4 — Tratamento de Excecoes
O sistema nao deve travar com entradas invalidas. Erros devem ser exibidos de forma amigavel na interface.

---

## 4. Fundamentos de Propagacao — Link Budget GPON

### Equacao Fundamental

```
Ptx - S = α × d + 10·log₁₀(N) + Pcon + M
```

| Simbolo | Descricao | Unidade | Valores Tipicos |
|---------|-----------|---------|-----------------|
| Ptx | Potencia de transmissao da OLT | dBm | +1.5 a +5 |
| S | Sensibilidade do receptor (ONU) | dBm | -28 (Classe B+), -27 (Classe C+) |
| α | Atenuacao da fibra G.652 | dB/km | 0.28 (1490 nm DS), 0.35 (1310 nm US) |
| d | Distancia do enlace | km | max 20 (tipico), max 60 (Classe C+) |
| N | Razao de divisao do splitter | — | 2, 4, 8, 16, 32, 64 |
| Pcon | Perda por conectores e fusoes | dB | ~0.5 por conector, ~0.1 por fusao |
| M | Margem de seguranca | dB | 3 (recomendado) |

### Perdas por Componentes

| Componente | Perda Tipica |
|-----------|-------------|
| Conector APC | 0.3 a 0.5 dB |
| Fusao mecanica | 0.1 a 0.3 dB |
| Splitter 1:2 | 3.0 dB |
| Splitter 1:4 | 6.0 dB |
| Splitter 1:8 | 9.0 dB |
| Splitter 1:16 | 12.0 dB |
| Splitter 1:32 | 15.0 dB |
| Splitter 1:64 | 18.0 dB |

### Limites de Atenuacao (ITU-T G.984)

| Classe | Atenuacao Maxima | Alcance Tipico |
|--------|-----------------|----------------|
| B+ | 28 dB | 20 km, 1:64 |
| C+ | 32 dB | 60 km, 1:64 |

---

## 5. Diagrama de Classes (Codigo Final)

```
┌─────────────────────────┐     ┌──────────────────────────┐
│      Equipamento         │     │       Validador           │
│      (Model)             │     │       (Model)             │
├─────────────────────────┤     ├──────────────────────────┤
│ - potenciaTx: double     │     │ + validarPotenciaTx()     │
│ - sensibilidade: double  │     │ + validarSensibilidade()  │
│ - perdaInsercao: double  │     │ + validarDistancia()      │
│ - atenuacao1490: double  │     │ + validarAtenuacao()      │
│ - atenuacao1310: double  │     │ + validarSplitter()       │
│ - perdaPorConector: double│    │ + validarPerdaConectores()│
│ - perdaPorFusao: double  │     │ + validarMargem()         │
│ - margem: double         │     │ - LIMITEs: constantes     │
│ + get/set p/ cada campo  │     └──────────┬───────────────┘
│ + getAtenuacaoFibra(nm)  │                │
└─────────┬────────────────┘                │
          │                                 │
          └──────────────┬──────────────────┘
                         │
              ┌──────────┴───────────┐
              │     LinkBudget        │
              │     (Model)           │
              ├──────────────────────┤
              │ + calcular(Map)       │
              │ - perdaSplitter(N)    │
              │ - calcularPotenciaTx()│
              │ - calcularSensibilidade()│
              │ - calcularAtenuacao() │
              │ - calcularDistancia() │
              │ - calcularDivisaoSplitter()│
              │ - calcularPerdaConectores()│
              │ - calcularMargem()    │
              └──────────┬───────────┘
                         │
              ┌──────────┴───────────┐
              │     Controlador       │
              │     (Controller)      │
              ├──────────────────────┤
              │ - linkBudget          │
              │ - validador           │
              │ + processarCalculo()  │
              │ - validarEntradas()   │
              │ - validarResultado()  │
              │ - validarAtenuacaoTotal()│
              └──────────┬───────────┘
                         │
              ┌──────────┴───────────┐
              │  ResultadoCalculo     │
              │  (Controller / DTO)   │
              ├──────────────────────┤
              │ - valor: double       │
              │ - variavel: String    │
              │ - alertas: List<String>│
              │ + getValor()          │
              │ + getVariavel()       │
              │ + getAlertas()        │
              │ + temAlertas()        │
              └──────────────────────┘

┌─────────────────────────┐
│    CalculadoraGUI        │
│    (View / Swing)        │
├─────────────────────────┤
│ - controlador            │
│ - campos: 7 JTextField   │
│ - cmbWavelength          │
│ - cmbSplitter1           │
│ - cmbSplitter2 (cascata) │
│ - spnConectores          │
│ - spnFusoes              │
│ - lblResultado           │
│ - areaAlertas            │
│ - btnCalcular            │
│ - btnA11y (A−/A+)        │
│ - btnAltoContraste       │
│ + iniciar()              │
│ - onCalcular()           │
│ - exibirResultado()      │
│ - exibirAlertas()        │
│ - aplicarAcessibilidade()│
└─────────────────────────┘
```

---

## 6. Casos de Uso

| Ator | Caso de Uso | Descricao |
|------|-------------|-----------|
| Engenheiro | Inserir Parametros | Preenche os campos conhecidos (potencia, sensibilidade, distancia, splitter, conectores, margem) |
| Engenheiro | Calcular Variavel Faltante | Clica em "Calcular" — sistema identifica o campo vazio e calcula a variavel |
| Engenheiro | Visualizar Alertas | Sistema exibe alertas se valores estiverem fora dos padroes ITU-T |

---

## 7. Entregaveis Obrigatorios

- [x] Documento de Requisitos (este PRD)
- [x] Diagrama de Caso de Uso (UML)
- [x] Diagrama de Classes atualizado (refletindo codigo final)
- [x] Projeto Java com interface grafica (Swing)
- [x] Testes unitarios JUnit 5 (40 testes)
- [x] README.md com instrucoes de execucao

---

## 8. Referencias Tecnicas

- **ITU-T G.984** — Gigabit-capable Passive Optical Networks (GPON)
- **ITU-T G.652** — Characteristics of a single-mode optical fibre and cable
- **GSA-PCS PRO 2026** — Padroes de mercado para dimensionamento de redes GPON
