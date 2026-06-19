# WheresMyMoney 💸

Controle de finanças pessoais direto no terminal.

## Sobre

WheresMyMoney é uma aplicação TUI (Terminal User Interface) para gerenciar suas finanças pessoais. Registre transações, organize categorias, acompanhe pagamentos e receitas recorrentes, visualize relatórios e encontre lançamentos com busca inteligente — tudo sem sair do terminal.

---

## Telas

### Dashboard `[D]`

Resumo financeiro por período selecionável: total de receitas (verde), despesas (vermelho) e saldo. Exibe breakdown hierárquico por categoria e subcategoria com gráficos de barras proporcionais à largura do terminal. Use o seletor de período para filtrar qualquer intervalo de datas.

<!-- SCREENSHOT: dashboard.png -->
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/1947066f-b1be-4d53-9ff1-5570111705b2" />


---

### Transações `[T]`

Lista transações em tabela (data, tipo, categoria, subcategoria, descrição, valor) com seletor de período e filtros por categoria. Ordene por qualquer coluna com `[O]` para alternar asc/desc. Rodapé exibe totais de Receitas, Despesas e Saldo das linhas visíveis. Busca semântica via Ollama com fallback textual. Atalho `[F4]` no campo valor abre calculadora. Teclas: `[N]` novo, `[Enter]` editar, `[D]` excluir.

<!-- SCREENSHOT: transacoes.png -->
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/2968349c-65c4-4893-a706-cd822f23d944" />

---

### Categorias `[C]`

Gerencia categorias de receita e despesa com suporte a subcategorias aninhadas. A visualização em árvore permite expandir e recolher subcategorias. Suporta adição, edição e exclusão com remoção em cascata.

<!-- SCREENSHOT: categorias.png -->
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/cf1075f0-f473-4ab9-a778-1747fd9ddb5e" />

---

### Pagamentos Mensais `[P]`

Lista os pagamentos recorrentes esperados para o mês com checklist de quitação. Navegue entre meses e marque os pagamentos realizados — cada marcação registra automaticamente a transação correspondente.

<!-- SCREENSHOT: pagamentos-mensais.png -->
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/76627217-a69f-458a-a8ce-cf880446e8f5" />

---

### Receitas Mensais `[I]`

Checklist de receitas recorrentes esperadas no mês. Cada item exibe descrição, valor e dia de recebimento — verde se já recebido, âmbar se pendente. Rodapé mostra Previsto, Recebido e A receber. Navegue entre meses com `[PgUp]`/`[PgDn]` ou vírgula/ponto. `[Espaço]`/`[Enter]` marca como recebido; `[N]`/`[E]`/`[D]` gerenciam os modelos recorrentes. Atalho `[F4]` no campo valor abre calculadora.

<!-- SCREENSHOT: receitas-mensais.png -->
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/67bd4d7d-90bd-44f0-b1e8-a7ff493a3682" />

---

### Relatórios `[R]`

Análise financeira com seleção livre de período (combo de ano e mês). Navegue entre meses com `[←]`/`[→]`. Exibe gráfico de linha duplo com receitas (verde) e despesas (vermelho) ao longo de 12 meses. Painel lateral detalha categorias do mês selecionado com totais de Receitas, Gastos e Saldo.

<!-- SCREENSHOT: relatorios.png -->
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/5bd28bde-9b4e-4c9a-bc3b-f90c7de19471" />

---

### Busca Inteligente `[/]`

Busca semântica por transações usando embeddings do Ollama. Encontra lançamentos por contexto, não apenas por palavra exata. Requer Ollama rodando localmente; cai para busca textual como fallback.

<!-- SCREENSHOT: busca-inteligente.png -->

---

## Instalação

**Pré-requisitos:**

- Java 25+
- Gradle 8+ (ou use o wrapper `./gradlew` incluso)
- (Opcional) [Ollama](https://ollama.com) com modelo `nomic-embed-text` para busca semântica

**Build:**

```bash
./gradlew bootJar
```

---

## Como usar

```bash
./run.sh
```

O script compila o projeto se necessário e executa a aplicação. Navegue pelas telas com as teclas indicadas entre colchetes no menu principal. Use `Esc` ou `Q` para voltar ao menu anterior.

---

## Especificações Técnicas

Veja [TECHNICAL.md](TECHNICAL.md) para detalhes sobre stack, arquitetura, banco de dados e integração com IA.
