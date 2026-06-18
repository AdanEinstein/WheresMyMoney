# Especificações Técnicas — WheresMyMoney

## Stack

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.1.0 |
| Build | Gradle 8 (Kotlin DSL) |
| UI | Lanterna 3.1.5 |
| Banco de dados | SQLite via xerial/sqlite-jdbc 3.50.1.0 |
| ORM | Hibernate + Spring Data JPA |
| Utilitários | Lombok 1.18.46 |

## Integração com IA

Busca semântica via Ollama rodando localmente:

- **Modelo:** `nomic-embed-text`
- **URL:** `http://localhost:11434`
- **Timeout:** 5 segundos
- **Estratégia:** scoring híbrido — combina similaridade semântica (embeddings de cosseno) com análise léxica para evitar falsos positivos

Se o Ollama não estiver disponível, a busca cai automaticamente para modo textual.

## Banco de Dados

- **Driver:** SQLite (arquivo local)
- **Localização:** `./data/wheresmymoney.db`
- **DDL:** `hibernate.ddl-auto: update` (migração automática)

### Entidades

| Entidade | Descrição |
|---|---|
| `Transaction` | Lançamento financeiro (receita ou despesa) |
| `Category` | Categoria principal |
| `Subcategory` | Subcategoria aninhada em `Category` |
| `MonthlyPayment` | Template de pagamento recorrente |
| `MonthlyPaymentStatus` | Registro de quitação mensal de pagamento |
| `MonthlyRevenue` | Template de receita recorrente |
| `MonthlyRevenueStatus` | Registro de recebimento mensal de receita |

## Arquitetura de Pacotes

```
src/main/java/...
├── domain/         Entidades JPA e enums de domínio
├── service/        Regras de negócio e acesso a dados
│   └── SemanticSearchService — embeddings + scoring híbrido
├── tui/            Telas e componentes Lanterna
│   ├── MainScreen
│   ├── DashboardScreen
│   ├── TransactionScreen
│   ├── CategoryScreen
│   ├── MonthlyPaymentScreen
│   ├── MonthlyRevenueScreen
│   ├── ReportScreen
│   └── components/  (DatePickerDialog, MoneyMask, etc.)
├── config/         Configuração Spring (JPA, Ollama)
├── util/           CurrencyUtil — formatação Real Brasileiro (9.999,99)
└── init/           Inicialização e dados de bootstrap
```

## Build e Execução

```bash
# Build
./gradlew bootJar

# Execução direta
java --enable-native-access=ALL-UNNAMED \
     -jar build/libs/WheresMyMoney-0.0.1-SNAPSHOT.jar

# Atalho (compila se necessário + executa)
./run.sh
```

## Arquivos de Dados

```
data/
├── wheresmymoney.db    Banco SQLite
└── wheresmymoney.log   Log de aplicação
```

## Configuração do Ollama (opcional)

```bash
# Instalar Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Baixar modelo de embeddings
ollama pull nomic-embed-text

# Iniciar servidor (roda em background por padrão)
ollama serve
```
