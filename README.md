# WheresMyMoney 💸

Controle de finanças pessoais direto no terminal.

## Sobre

WheresMyMoney é uma aplicação TUI (Terminal User Interface) para gerenciar suas finanças pessoais. Registre transações, organize categorias, acompanhe pagamentos e receitas recorrentes, visualize relatórios e encontre lançamentos com busca inteligente — tudo sem sair do terminal.

---

## Telas

### Dashboard `[D]`

Resumo financeiro por período selecionável: total de receitas (verde), despesas (vermelho) e saldo. Exibe breakdown hierárquico por categoria e subcategoria com gráficos de barras proporcionais à largura do terminal. Use o seletor de período para filtrar qualquer intervalo de datas.

<!-- SCREENSHOT: dashboard.png -->

---

### Transações `[T]`

Lista transações em tabela (data, tipo, categoria, subcategoria, descrição, valor) com seletor de período e filtros por categoria. Ordene por qualquer coluna com `[O]` para alternar asc/desc. Rodapé exibe totais de Receitas, Despesas e Saldo das linhas visíveis. Busca semântica via Ollama com fallback textual. Atalho `[F4]` no campo valor abre calculadora. Teclas: `[N]` novo, `[Enter]` editar, `[D]` excluir.

<!-- SCREENSHOT: transacoes.png -->

---

### Categorias `[C]`

Gerencia categorias de receita e despesa com suporte a subcategorias aninhadas. A visualização em árvore permite expandir e recolher subcategorias. Suporta adição, edição e exclusão com remoção em cascata.

<!-- SCREENSHOT: categorias.png -->

---

### Pagamentos Mensais `[P]`

Lista os pagamentos recorrentes esperados para o mês com checklist de quitação. Navegue entre meses e marque os pagamentos realizados — cada marcação registra automaticamente a transação correspondente.

<!-- SCREENSHOT: pagamentos-mensais.png -->

---

### Receitas Mensais `[I]`

Checklist de receitas recorrentes esperadas no mês. Cada item exibe descrição, valor e dia de recebimento — verde se já recebido, âmbar se pendente. Rodapé mostra Previsto, Recebido e A receber. Navegue entre meses com `[PgUp]`/`[PgDn]` ou vírgula/ponto. `[Espaço]`/`[Enter]` marca como recebido; `[N]`/`[E]`/`[D]` gerenciam os modelos recorrentes. Atalho `[F4]` no campo valor abre calculadora.

<!-- SCREENSHOT: receitas-mensais.png -->

---

### Relatórios `[R]`

Análise financeira com seleção livre de período (combo de ano e mês). Navegue entre meses com `[←]`/`[→]`. Exibe gráfico de linha duplo com receitas (verde) e despesas (vermelho) ao longo de 12 meses. Painel lateral detalha categorias do mês selecionado com totais de Receitas, Gastos e Saldo.

<!-- SCREENSHOT: relatorios.png -->

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
