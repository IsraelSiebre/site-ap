# André Pacheco Imóveis — Spring Boot MVC

Site e painel em **Spring Boot 4.1.2, Java 17+, Thymeleaf, Spring Data JPA e Spring Security**. As páginas são renderizadas no servidor com dados do banco. O CSS e as imagens originais foram preservados.

## Executar com Docker

Instale e inicie o Docker Desktop (ou Docker Engine com o plugin Compose). Na raiz do projeto, crie o arquivo de configuração:

```powershell
Copy-Item .env.example .env
```

Edite o `.env` e substitua `POSTGRES_PASSWORD` e `ADMIN_PASSWORD` por senhas próprias. No Linux/macOS, use `cp .env.example .env`. Depois, inicie os dois containers:

```powershell
docker compose up --build -d
```

O site fica em `http://localhost:8080` e o painel em `http://localhost:8080/admin`. Use o `ADMIN_USER` e a senha definidos no `.env`. Altere `APP_PORT` se precisar de outra porta no computador. Para acompanhar a inicialização, execute `docker compose logs -f app`.

O Compose inicia o PostgreSQL antes da aplicação e mantém o banco acessível apenas pela rede interna do Compose. Os volumes `postgres_data` e `uploads` preservam, respectivamente, os cadastros e as fotos mesmo quando os containers são recriados. `docker compose down` para e remove os containers sem apagar os volumes; `docker compose down -v` também apaga os dados dos dois volumes. Inclua os dois no procedimento de backup.

O PostgreSQL é inicializado a partir dos dados originais na primeira execução. Ele é independente do arquivo H2 usado fora do Docker: cadastros e uploads já existentes no H2 não são migrados automaticamente. As variáveis `POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD` inicializam o banco apenas quando o volume está vazio; depois disso, mudar o `.env` não altera o usuário nem a senha existentes no PostgreSQL.

## Executar localmente sem Docker

Instale um JDK 17 ou superior e configure `JAVA_HOME` para a pasta do JDK. O Maven Wrapper baixa Maven e dependências na primeira execução.

No PowerShell, na raiz do projeto:

```powershell
$env:ADMIN_USER = 'admin'
$env:ADMIN_PASSWORD = 'defina-sua-senha-aqui'
.\mvnw.cmd spring-boot:run
```

No Linux/macOS, defina as mesmas variáveis com `export` e execute `sh mvnw spring-boot:run`.

- Site: http://localhost:8080
- Painel: http://localhost:8080/admin
- Login: usuário e senha definidos acima. Sem `ADMIN_PASSWORD`, uma senha temporária é gerada e exibida no console a cada início. Usuário padrão: `admin`.

No IntelliJ IDEA, importe o `pom.xml` como projeto Maven, selecione um JDK 17+ e execute `br.com.andrepacheco.imoveis.Application`. Configure as variáveis de ambiente na configuração de execução.

Para gerar o executável:

```powershell
.\mvnw.cmd clean verify
java -jar target/imoveis-1.0.0.jar
```

Abra o site pelo servidor Spring Boot. Os templates ficam em `src/main/resources/templates` e os recursos estáticos em `src/main/resources/static`.

## Organização MVC

```text
src/main/java/br/com/andrepacheco/imoveis/
  Application.java
  controller/     SiteController, AdminController e formatação das views
  domain/         Imovel, Corretor, Institucional e objetos incorporados
  repository/     Repositórios Spring Data JPA
  service/        Cadastros, validação e armazenamento de fotos
  config/         Login, recursos estáticos e importação inicial
src/main/resources/
  templates/      Páginas Thymeleaf públicas, fragmentos e painel
  static/css/     CSS original
  static/img/     Logo e fotos originais
  static/js/      Galeria, traduções, interações do painel e chatbot
  data/seed.json  Dados originais importados na primeira inicialização
  application.properties
src/test/         Testes de integração e do chatbot
data/             Banco persistente e uploads da execução
Dockerfile        Build e execução da aplicação em container
docker-compose.yml  Serviços da aplicação e do PostgreSQL
.env.example      Exemplo de configuração do Compose
```

Controllers recebem pedidos e preparam o modelo; serviços executam operações e validações; repositórios persistem entidades; Thymeleaf gera o HTML. Formulários administrativos usam POST e redirecionamento após salvar. Cadastros não usam `localStorage`, nem dependem de uma API JSON para montar páginas.

## Funcionalidades

- Páginas inicial, imóveis, detalhe, corretores, sobre, contato e terreno + construção, mantendo endereços `.html`.
- Busca por negócio, tipo, cidade, bairro e preço; ordenação e mais resultados processados no servidor.
- Cadastro, edição e exclusão de imóveis e corretores. Excluir um corretor mantém os imóveis e remove a associação.
- Upload de fotos de imóveis, escolha da capa e remoção da galeria; foto do corretor e fundo do banner.
- Edição de dados da empresa, missão, visão, valores, contatos, terreno + construção e escurecimento do banner.
- Galeria em tela cheia, compartilhamento, PT/EN/ES, pesquisas recentes e formulários de WhatsApp.
- Login com sessão, BCrypt, autorização no servidor e CSRF nos formulários.

## Banco e fotos

O banco padrão é H2 em arquivo: `data/imobiliaria.mv.db`, persistente entre reinícios. A primeira execução importa a empresa, os três corretores e os imóveis 101/102 originais; apagar cadastros não faz a carga inicial recriá-los.

Uploads ficam em `data/uploads`, fora do JAR, publicados em `/uploads/`. São aceitos JPEG, PNG e WebP de até **10 MB por arquivo**, **100 MB por envio** e até **60 fotos por imóvel**. O servidor verifica o conteúdo real e limita dimensões a 12.000 pixels por lado e 40 megapixels. Cada novo upload é comprimido e armazenado em WebP, com nome aleatório. Imagens cadastradas anteriormente continuam disponíveis no formato original.

Fotos são enviadas ao salvar. Remover uma imagem de um cadastro retira a referência; arquivos antigos não são apagados automaticamente do disco. Inclua **banco e uploads** nos backups. Pare a aplicação antes de copiar o arquivo H2 para obter uma cópia consistente.

## Chatbot

O chatbot funciona **inteiramente no navegador**, em `src/main/resources/static/js/chat.js`, e a conversa acontece pelas opções apresentadas na tela. Perguntas, estado, filtros e respostas são JavaScript. O Thymeleaf inclui na página os dados públicos usados na busca. Não há endpoint `/chat`, chamadas de IA, envio de mensagens ou estado de conversa no backend.

`localStorage` armazena somente idioma e pesquisas recentes. Textos cadastrados no painel permanecem no idioma em que foram escritos.

## Configuração

| Variável | Padrão / finalidade |
|---|---|
| `PORT` | `8080` |
| `ADMIN_USER` | `admin` |
| `ADMIN_PASSWORD` | Vazia gera senha temporária no início |
| `DATABASE_URL` | `jdbc:h2:file:./data/imobiliaria;DB_CLOSE_ON_EXIT=FALSE` |
| `DATABASE_USERNAME` | `sa` |
| `DATABASE_PASSWORD` | Vazia |
| `UPLOAD_DIR` | `./data/uploads` |
| `SEED_ENABLED` | `true`; carga inicial dos dados existentes |
| `SESSION_COOKIE_SECURE` | `false` para localhost; use `true` com HTTPS |

Caminhos relativos usam o diretório de onde a aplicação é executada. Preserve esse diretório ou configure caminhos absolutos na instalação. A aplicação inclui os drivers H2 e PostgreSQL; no Compose, as variáveis do banco são configuradas automaticamente.

## Testes

```powershell
.\mvnw.cmd test
node --test src/test/js/chat.test.cjs
```

Os testes Java usam H2 em memória e fotos em `target/test-uploads`, sem alterar dados da execução normal. Cobrem SSR, filtros, autenticação, CSRF, validação, CRUD, fotos/capa, empresa e proteção contra HTML injetado.

O teste `PostgresIntegrationTest` é opcional e só executa quando `POSTGRES_TEST_URL`, `POSTGRES_TEST_USER` e `POSTGRES_TEST_PASSWORD` estiverem definidos. Ele cria e remove tabelas: use exclusivamente um banco PostgreSQL descartável e dedicado ao teste.

Referências oficiais: [Spring Boot](https://docs.spring.io/spring-boot/index.html), [Thymeleaf com Spring](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html).
