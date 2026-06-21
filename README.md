# Calculadora de Link Budget GPON

Projeto Interdisciplinar — Propagacao de Ondas Eletromagneticas × Engenharia de Software

Calculadora de Link Budget para redes GPON (Gigabit Passive Optical Network). Permite que um engenheiro de telecomunicacoes dimensione enlaces GPON de forma interativa, isolando qualquer variavel da equacao fundamental de propagacao optica.

## Equacao Fundamental

```
Ptx - S = α × d + 10·log₂(N) + Pcon + M
```

| Simbolo | Descricao                     | Unidade | Valores Tipicos              |
|---------|-------------------------------|---------|------------------------------|
| Ptx     | Potencia de transmissao (OLT) | dBm     | +1.5 a +5                    |
| S       | Sensibilidade do receptor     | dBm     | -28 (Classe B+), -27 (C+)    |
| α       | Atenuacao da fibra G.652      | dB/km   | 0.35 (1490 nm DS)            |
| d       | Distancia do enlace           | km      | max 20 (B+), max 60 (C+)     |
| N       | Razao de divisao do splitter  | —       | 2, 4, 8, 16, 32, 64          |
| Pcon    | Perda por conectores/fusoes   | dB      | ~0.5 por conector            |
| M       | Margem de seguranca           | dB      | 3 (recomendado)              |

## Estrutura do Projeto

```
.
├── pom.xml                          # Maven (JUnit 5)
├── PRD.md                           # Documento de Requisitos
├── README.md
├── docs/
│   └── diagrama_classes.png         # Diagrama de Classes UML
├── src/
│   ├── main/java/model/
│   │   └── LinkBudget.java          # Classe de calculo do Link Budget
│   └── test/java/model/
│       └── LinkBudgetTest.java      # Testes unitarios JUnit 5
```

## Pre-requisitos

- **JDK 11+** (desenvolvido com JDK 26)
- **Maven 3.6+** (para build e testes)

## Como Compilar

```bash
mvn compile
```

## Como Executar os Testes

```bash
mvn test
```

Os testes cobrem:
- Calculo isolado de cada uma das 7 variaveis
- Cenarios GPON reais (classe B+, splitter 1:32)
- Round-trip: consistencia bidirecional dos calculos
- Condicoes de erro: multiplas variaveis faltantes, resultados fisicamente impossiveis
- Idempotencia dos calculos

## Como Empacotar

```bash
mvn package
```

## Arquitetura

O projeto segue o padrao **MVC** (Model-View-Controller):

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    Model      │     │    View       │     │  Controller   │
│  LinkBudget   │────▶│ CalculadoraGUI │◀────│ Controlador   │
│  Equipamento  │     │  (Swing)      │     │               │
│  Validador    │     └──────────────┘     └──────────────┘
└──────────────┘
```

## Referencias Tecnicas

- **ITU-T G.984** — Gigabit-capable Passive Optical Networks (GPON)
- **ITU-T G.652** — Characteristics of a single-mode optical fibre and cable
- **GSA-PCS PRO 2026** — Padroes de mercado para dimensionamento GPON

## Autores

- Joao Victor Borges Carvalho (Jkvzin)
- Joao Guilherme Garcia Mangueira (JoaoGarciaM)
