# Documento de Requisitos — Calculadora de Link Budget GPON

## Projeto Interdisciplinar
**Disciplinas:** Propagacao de Ondas Eletromagneticas + Engenharia de Software
**Repositorio:** https://github.com/Jkvzin/interdisciplinar-propagacao-engenharia-de-software
**PRD de referencia:** PRD.md

---

## 1. Introducao

### 1.1 Proposito
Este documento especifica os requisitos funcionais e nao funcionais da Calculadora de Link Budget para redes GPON (Gigabit Passive Optical Network). A ferramenta destina-se a engenheiros de telecomunicacoes que necessitam dimensionar enlaces opticos passivos, determinando a viabilidade do enlace com base nos parametros fisicos da rede.

### 1.2 Escopo
O sistema abrange:
- Entrada de parametros de enlace optico via interface grafica
- Calculo automatico da variavel faltante na equacao de Link Budget
- Validacao dos resultados contra padroes ITU-T G.984 e G.652
- Exibicao de alertas visuais quando parametros excedem limites tecnicos

### 1.3 Definicoes e Acronimos
| Termo | Significado |
|-------|-------------|
| GPON | Gigabit Passive Optical Network |
| OLT | Optical Line Terminal (Central) |
| ONU | Optical Network Unit (Cliente) |
| Link Budget | Balanco de potencia do enlace optico |
| Splitter | Divisor optico passivo |
| dBm | Decibel-miliwatt (unidade de potencia) |
| ITU-T | International Telecommunication Union — Telecom |
| G.984 | Norma ITU-T para GPON |
| G.652 | Norma ITU-T para fibra optica monomodo |

---

## 2. Descricao Geral

### 2.1 Perspectiva do Produto
A Calculadora de Link Budget GPON e uma aplicacao desktop desenvolvida em Java com interface Swing. O sistema substitui planilhas manuais de calculo, automatizando a resolucao da equacao de balanco de potencia e adicionando validacao inteligente baseada em normas tecnicas.

### 2.2 Usuarios Alvo
- **Engenheiros de Telecomunicacoes** — dimensionamento de redes GPON
- **Estudantes de Engenharia** — aprendizado de propagacao em fibra optica

### 2.3 Plataforma
- Desktop (Windows, Linux, macOS)
- JDK 11 ou superior
- Interface grafica Java Swing

---

## 3. Requisitos Funcionais

### RF1 — Calculo de Variavel Faltante

**Prioridade:** Essencial

**Descricao:** O sistema deve calcular qualquer variavel da equacao de Link Budget quando as demais cinco forem fornecidas pelo usuario.

**Equacao de referencia:**
```
Ptx - S = α × d + 10·log₂(N) + Pcon + M
```

**Variaveis:**
| Nome | Simbolo | Unidade | Faixa Aceitavel |
|------|---------|---------|-----------------|
| Potencia de Transmissao | Ptx | dBm | +1.0 a +6.0 |
| Sensibilidade do Receptor | S | dBm | -30.0 a -24.0 |
| Distancia | d | km | 0.1 a 60.0 |
| Razao do Splitter | N | — | {2, 4, 8, 16, 32, 64} |
| Perda por Conectores | Pcon | dB | 0.0 a 10.0 |
| Margem de Seguranca | M | dB | 0.0 a 6.0 |

**Regras:**
1. O usuario preenche 5 campos e deixa 1 vazio
2. O sistema identifica o campo vazio
3. O sistema isola algebricamente a variavel faltante
4. O sistema exibe o resultado com 2 casas decimais

**Tratamento de erros:**
- Se 0 campos vazios: exibir "Todos os campos preenchidos. Deixe um campo vazio para calcular."
- Se 2+ campos vazios: exibir "Deixe exatamente UM campo vazio para calculo."
- Se resultado impossivel (ex: distancia negativa): exibir mensagem especifica

---

### RF2 — Validacao contra Normas Tecnicas

**Prioridade:** Essencial

**Descricao:** O sistema deve validar todas as entradas e a saida calculada contra os limites estabelecidos pelas normas ITU-T G.984 (GPON) e G.652 (fibra).

**Regras de validacao:**

| Parametro | Limite | Norma | Alerta |
|-----------|--------|-------|--------|
| Ptx | Fora de [+1.5, +5.0] dBm | G.984 | "Potencia de transmissao fora do padrao GPON (+1.5 a +5 dBm)" |
| S | Acima de -28 dBm (B+) | G.984 | "Sensibilidade do receptor acima do limite Classe B+ (-28 dBm)" |
| d | Acima de 20 km | G.984 | "Distancia excede o limite tipico GPON de 20 km" |
| atenuacao total | Acima de 28 dB | G.984 | "Atenuacao total acima do limite de 28 dB (Classe B+)" |
| α (fibra) | Fora de [0.30, 0.45] dB/km | G.652 | "Atenuacao da fibra fora do especificado para G.652" |

---

### RF3 — Interface Grafica

**Prioridade:** Essencial

**Descricao:** O sistema deve prover interface grafica com os seguintes elementos:

**Componentes da tela:**
1. Titulo: "Calculadora de Link Budget — GPON"
2. Seis campos de entrada numericos com validacao em tempo real
3. Dropdown para razao do splitter (1:2, 1:4, 1:8, 1:16, 1:32, 1:64)
4. Botao "Calcular"
5. Painel de resultado (variavel calculada em destaque)
6. Painel de alertas (JTextArea com scroll, nao editavel)

**Codigo de cores dos alertas:**
- Verde (#1a6b1a): "Todos os parametros dentro dos padroes ITU-T G.984"
- Laranja (#b85c00): Alertas de atencao
- Vermelho (#b80000): Alertas criticos (limite excedido)

**Validacao em tempo real:**
- Campos numericos aceitam apenas digitos e ponto decimal
- Virgula convertida automaticamente para ponto
- Campo vazio permitido (para calculo da variavel faltante)

---

## 4. Requisitos Nao Funcionais

### RNF1 — Arquitetura MVC
**Criterio de aceitacao:** Classes organizadas em pacotes `model`, `view` e `controller`.

### RNF2 — Orientacao a Objetos
**Criterio de aceitacao:** Codigo Java com classes `Equipamento`, `Validador`, `LinkBudget`, `Controlador` e GUI.

### RNF3 — Testes Unitarios
**Criterio de aceitacao:** Minimo de 10 testes JUnit 5 com pelo menos 2 assertions por metodo. Comando `mvn test` (ou `gradle test`) com 100% de testes passando.

### RNF4 — Tratamento de Excecoes
**Criterio de aceitacao:** Nenhuma entrada invalida trava a interface. Erros exibidos no painel de alertas.

### RNF5 — Documentacao do Codigo
**Criterio de aceitacao:** Javadoc em todas as classes e metodos publicos.

---

## 5. Regras de Negocio

### 5.1 Perda por Splitter
A perda por divisao otica e calculada por `10 × log₁₀(N)` onde N e a razao de divisao.

| Razao | Perda |
|-------|-------|
| 1:2 | 3.0 dB |
| 1:4 | 6.0 dB |
| 1:8 | 9.0 dB |
| 1:16 | 12.0 dB |
| 1:32 | 15.0 dB |
| 1:64 | 18.0 dB |

### 5.2 Atenuacao da Fibra
Valores de referencia para fibra G.652:
- 1490 nm (downstream): 0.35 dB/km
- 1310 nm (upstream): 0.40 dB/km

### 5.3 Limites de Classe GPON
| Classe | Orcamento de Potencia | Alcance Maximo |
|--------|----------------------|----------------|
| B+ | 28 dB | 20 km |
| C+ | 32 dB | 60 km |

---

## 6. Diagrama de Classes (Especificacao)

Ver `PRD.md` Secao 5 para o diagrama completo.

```
model/
  Equipamento.java    — Atributos de componentes GPON
  Validador.java      — Regras ITU-T G.984 e G.652
  LinkBudget.java     — Motor de calculo com isolamento de variaveis

controller/
  Controlador.java    — Ponte entre View e Model

view/
  CalculadoraGUI.java — Interface Swing
```

---

## 7. Criterios de Aceitacao (Checklist)

- [ ] RF1: Calcular cada uma das 6 variaveis quando as demais sao fornecidas
- [ ] RF1: Erro amigavel com 2+ campos vazios
- [ ] RF2: Alerta quando Ptx fora de [+1.5, +5.0] dBm
- [ ] RF2: Alerta quando S acima de -28 dBm
- [ ] RF2: Alerta quando d > 20 km
- [ ] RF2: Alerta quando atenuacao total > 28 dB
- [ ] RF3: Interface abre e exibe todos os componentes
- [ ] RF3: Calculo executado ao clicar "Calcular"
- [ ] RF3: Alertas exibidos com codigo de cores
- [ ] RNF1: Pacotes model/view/controller separados
- [ ] RNF3: 10+ testes JUnit passando
- [ ] RNF4: Interface nao trava com entradas invalidas
