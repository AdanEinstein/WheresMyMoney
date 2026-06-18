# WheresMyMoney 💸

Controle de finanças pessoais direto no terminal.

## Sobre

WheresMyMoney é uma aplicação TUI (Terminal User Interface) para gerenciar suas finanças pessoais. Registre transações, organize categorias, acompanhe pagamentos e receitas recorrentes, visualize relatórios e encontre lançamentos com busca inteligente — tudo sem sair do terminal.

---

## Telas

### Dashboard `[D]`

Resumo financeiro do mês: total de receitas, despesas e saldo. Exibe as categorias com maior gasto em gráfico de barras, com destaque em verde para saldo positivo e vermelho para negativo.

<!-- SCREENSHOT: dashboard.png -->

---

### Transações `[T]`

Lista todas as transações do período em tabela (data, tipo, categoria, descrição, valor). Permite adicionar `[N]`, editar `[Enter]` e excluir `[D]` lançamentos. O formulário suporta categoria e subcategoria, notas e seleção de data via calendário navegável.

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

Espelha a tela de pagamentos, mas para receitas recorrentes. Acompanhe o que já foi recebido no mês e registre as entradas diretamente pelo checklist.

<!-- SCREENSHOT: receitas-mensais.png -->

---

### Relatórios `[R]`

Análise financeira por período: mês atual, mês anterior ou ano inteiro. Exibe breakdown por categoria com gráficos de barras e totais consolidados.

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
