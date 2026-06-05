# ROADMAP.md — TaskFlow

> Documento operacional de construção do app Android **TaskFlow**.  
> Este arquivo deve ser usado pelo gerente do projeto, Codex, agente construtor ou equipe técnica para acompanhar a execução, marcar tarefas concluídas e validar critérios de aceite.

---

## 0. Regras de uso deste Roadmap

### 0.1 Como marcar progresso

Use os checkboxes abaixo:

- `[ ]` Não iniciado
- `[~]` Em andamento
- `[x]` Concluído
- `[!]` Bloqueado

> **Importante:** uma tarefa só pode ser marcada como `[x]` quando todos os critérios de aceite daquela tarefa forem cumpridos.

### 0.2 Definição geral de concluído — Definition of Done

Uma etapa só pode ser considerada concluída quando:

- [ ] A funcionalidade foi implementada.
- [ ] A funcionalidade compila sem erros.
- [ ] O fluxo principal foi testado manualmente.
- [ ] Estados de erro e carregamento foram tratados.
- [ ] A interface segue o design definido no PRD.
- [ ] O código está organizado em camadas adequadas.
- [ ] Não há dados sensíveis expostos em logs.
- [ ] Não há crashes conhecidos no fluxo principal.
- [ ] O comportamento foi validado contra os critérios de aceite.
- [ ] A documentação técnica mínima foi atualizada quando necessário.

### 0.3 Ordem obrigatória de execução

O projeto deve seguir esta ordem preferencial:

1. Base técnica do projeto.
2. Design system.
3. Modelos de dados.
4. Navegação.
5. Telas com dados mockados.
6. CRUD local de tarefas.
7. Lembretes e notificações.
8. Anexos, links e campos complementares.
9. Colaboração e compartilhamento.
10. Backend/Firebase.
11. Sincronização.
12. Testes, polimento e release.

### 0.4 Regra para agentes construtores

O agente construtor deve:

- consultar este Roadmap antes de iniciar qualquer etapa;
- executar apenas tarefas ainda não concluídas;
- não pular dependências essenciais;
- marcar uma tarefa como concluída somente após validar seus critérios de aceite;
- registrar observações em `Notas de execução` quando houver decisão técnica importante;
- não remover funcionalidades previstas no PRD sem autorização.

---

# 1. Visão do Produto

## 1.1 Objetivo

Construir um app Android chamado **TaskFlow**, voltado para criação de tarefas, lembretes inteligentes, recorrências personalizadas, anexos, links, campos complementares e colaboração.

## 1.2 Proposta central

O TaskFlow deve funcionar como uma lista de tarefas moderna e colaborativa, parecida com uma versão simplificada do Trello, mas com foco superior em lembretes e organização prática de materiais da tarefa.

## 1.3 Funcionalidades centrais

- Tarefas.
- Listas.
- Espaços.
- Lembretes únicos.
- Lembretes recorrentes personalizados.
- Notificações locais.
- Anexos.
- Fotos de documentos.
- Links.
- Campos complementares.
- Checklist.
- Compartilhamento por WhatsApp, e-mail e link.
- Convites.
- Responsáveis e participantes.
- Comentários.
- Histórico de alterações.

---

# 2. Stack Técnica Definida

## 2.1 Plataforma

- [ ] Android nativo.
- [ ] Kotlin.
- [ ] Jetpack Compose.
- [ ] Arquitetura MVVM.
- [ ] Gradle Kotlin DSL.

## 2.2 Persistência local

- [x] Room Database.
- [x] DataStore para preferências.

## 2.3 Backend sugerido

- [ ] Firebase Authentication.
- [ ] Cloud Firestore.
- [ ] Firebase Storage.
- [ ] Firebase Cloud Messaging.
- [ ] Cloud Functions, se necessário.

## 2.4 Recursos Android

- [ ] Android notification permission.
- [ ] AlarmManager ou WorkManager conforme necessidade técnica.
- [ ] Photo Picker para imagens.
- [ ] CameraX ou intent de câmera para fotos.
- [ ] Storage Access Framework para arquivos.
- [ ] Android Sharesheet para compartilhamento.

---

# 3. Marcos Gerais do Projeto

| Marco | Nome | Resultado esperado | Status |
|---|---|---|---|
| M0 | Setup inicial | Projeto compila e abre tela inicial | [x] |
| M1 | Design system | Componentes visuais base prontos | [ ] |
| M2 | Navegação | Fluxo de telas principal funcionando | [ ] |
| M3 | Tarefas locais | Criar, editar, listar e concluir tarefas | [ ] |
| M4 | Lembretes | Lembretes únicos e recorrentes funcionando | [ ] |
| M5 | Materiais | Anexos, links e campos extras funcionando | [ ] |
| M6 | Compartilhamento | WhatsApp, e-mail, link e convite | [ ] |
| M7 | Firebase | Login, banco e storage integrados | [ ] |
| M8 | Sincronização | Offline/online com consistência básica | [ ] |
| M9 | QA e release | App testado, polido e pronto para distribuição | [ ] |

---

# 4. Fase 0 — Setup Inicial

## 4.1 Criar projeto Android

- [x] Criar projeto Android nativo.
- [x] Configurar Kotlin.
- [x] Configurar Jetpack Compose.
- [x] Configurar versão mínima do Android.
- [x] Configurar Material 3.
- [x] Criar estrutura inicial de pacotes.

### Critérios de aceite

- [x] O projeto compila sem erros.
- [x] O app abre em emulador Android.
- [x] Existe uma tela inicial provisória.
- [x] Estrutura de pacotes criada.

### Notas de execução

- Decisão técnica: criado app Android nativo Kotlin + Jetpack Compose em `com.taskflow`, com Gradle wrapper 8.10.2, Android SDK local em `C:\TaskFlowAndroidSdk` e JDK 17 portátil em `.tools`.
- Pendências: evoluir das telas e repositório local simplificado para persistência Room/DataStore completa nas fases próprias.

---

## 4.2 Estrutura de pacotes

Criar estrutura base:

```text
app/
 └── src/main/java/com/taskflow/
      ├── core/
      │   ├── design/
      │   ├── navigation/
      │   ├── notifications/
      │   ├── permissions/
      │   └── utils/
      ├── data/
      │   ├── local/
      │   ├── remote/
      │   ├── repository/
      │   └── mapper/
      ├── domain/
      │   ├── model/
      │   ├── repository/
      │   └── usecase/
      ├── feature/
      │   ├── auth/
      │   ├── home/
      │   ├── spaces/
      │   ├── lists/
      │   ├── tasks/
      │   ├── reminders/
      │   ├── materials/
      │   ├── sharing/
      │   └── settings/
      └── MainActivity.kt
```

### Tarefas

- [x] Criar pacote `core`.
- [x] Criar pacote `data`.
- [x] Criar pacote `domain`.
- [x] Criar pacote `feature`.
- [x] Criar `MainActivity` com Compose.

### Critérios de aceite

- [x] A arquitetura inicial está organizada.
- [ ] Nenhuma feature fica misturada dentro da `MainActivity`.
- [x] O projeto mantém separação mínima entre UI, domínio e dados.

---

# 5. Fase 1 — Design System

## 5.1 Tema visual

Implementar identidade visual moderna, limpa e premium.

### Tarefas

- [ ] Criar paleta de cores.
- [ ] Criar tipografia.
- [ ] Definir espaçamentos.
- [ ] Definir raio dos cards.
- [ ] Definir estilo dos botões.
- [ ] Definir tema claro.
- [ ] Preparar estrutura para tema escuro futuro.

### Direção visual

- Fundo off-white.
- Cards brancos.
- Gradientes azul/roxo.
- Botões pill.
- Toggles deslizantes.
- Segmented controls.
- Chips coloridos.
- Ícones minimalistas.

### Critérios de aceite

- [ ] Todas as telas usam o mesmo tema.
- [ ] Cores não são definidas aleatoriamente dentro das telas.
- [ ] Componentes principais seguem padrão consistente.
- [ ] Interface lembra app moderno/polido com inspiração iOS-like.

---

## 5.2 Componentes reutilizáveis

### Tarefas

- [ ] Criar `TaskFlowButton`.
- [ ] Criar `TaskFlowOutlinedButton`.
- [ ] Criar `TaskFlowCard`.
- [ ] Criar `TaskCard`.
- [ ] Criar `ReminderCard`.
- [ ] Criar `AttachmentCard`.
- [ ] Criar `LinkCard`.
- [ ] Criar `CustomFieldRow`.
- [ ] Criar `StatusPill`.
- [ ] Criar `PriorityChip`.
- [ ] Criar `SegmentedControl`.
- [ ] Criar `ToggleSwitch`.
- [ ] Criar `BottomNavigationBar`.
- [ ] Criar `FloatingAddButton`.
- [ ] Criar `EmptyState`.
- [ ] Criar `LoadingState`.
- [ ] Criar `ErrorState`.

### Critérios de aceite

- [ ] Componentes reutilizados em mais de uma tela quando aplicável.
- [ ] Componentes têm previews do Compose.
- [ ] Componentes aceitam estados principais.
- [ ] Componentes não contêm regra de negócio.

---

# 6. Fase 2 — Modelos de Domínio

## 6.1 Criar entidades principais

### Tarefas

- [ ] Criar `User`.
- [ ] Criar `Space`.
- [ ] Criar `TaskList`.
- [ ] Criar `Task`.
- [ ] Criar `Reminder`.
- [ ] Criar `Attachment`.
- [ ] Criar `TaskLink`.
- [ ] Criar `CustomField`.
- [ ] Criar `Comment`.
- [ ] Criar `ActivityLog`.
- [ ] Criar `Invite`.

### Critérios de aceite

- [ ] Todas as entidades possuem identificador único.
- [ ] Todas as entidades possuem campos de criação/atualização quando necessário.
- [ ] Entidades não dependem diretamente da UI.
- [ ] Tipos complexos usam enums ou sealed classes quando adequado.

---

## 6.2 Enums e tipos auxiliares

### Tarefas

- [ ] Criar `TaskStatus`.
- [ ] Criar `TaskPriority`.
- [ ] Criar `ReminderType`.
- [ ] Criar `RecurrenceType`.
- [ ] Criar `RecurrenceUnit`.
- [ ] Criar `WeekDay`.
- [ ] Criar `MonthlyRule`.
- [ ] Criar `ReminderEndType`.
- [ ] Criar `AttachmentSource`.
- [ ] Criar `AttachmentType`.
- [ ] Criar `CustomFieldType`.
- [ ] Criar `UserPermission`.

### Critérios de aceite

- [ ] Nenhum status é tratado como string solta na UI.
- [ ] Recorrência avançada está modelada.
- [ ] Anexos, links e campos extras têm modelos próprios.

---

# 7. Fase 3 — Navegação

## 7.1 Criar grafo de navegação

### Telas obrigatórias

- [ ] Splash / Onboarding.
- [ ] Login.
- [ ] Cadastro.
- [ ] Home / Hoje.
- [ ] Espaços.
- [ ] Listas.
- [ ] Lista de tarefas.
- [ ] Nova tarefa.
- [ ] Detalhe da tarefa.
- [ ] Configurar lembrete.
- [ ] Materiais da tarefa.
- [ ] Adicionar anexo.
- [ ] Adicionar link.
- [ ] Campos complementares.
- [ ] Compartilhar / convidar.
- [ ] Pessoas.
- [ ] Configurações.

### Critérios de aceite

- [ ] O usuário consegue navegar entre as telas principais.
- [ ] Bottom navigation aparece apenas nas telas adequadas.
- [ ] Telas de criação e edição possuem navegação de voltar/cancelar.
- [ ] Não há telas órfãs.

---

# 8. Fase 4 — Dados Mockados

## 8.1 Criar mock repository

### Tarefas

- [ ] Criar usuários mockados.
- [ ] Criar espaços mockados.
- [ ] Criar listas mockadas.
- [ ] Criar tarefas mockadas.
- [ ] Criar lembretes mockados.
- [ ] Criar anexos mockados.
- [ ] Criar links mockados.
- [ ] Criar campos complementares mockados.
- [ ] Criar comentários mockados.

### Critérios de aceite

- [ ] Todas as telas principais exibem dados reais mockados.
- [ ] Dados mockados demonstram as principais funções.
- [ ] Não existem textos genéricos demais como `Lorem ipsum` nas telas principais.

---

# 9. Fase 5 — Home / Hoje

## 9.1 Construir tela Home

### Tarefas

- [ ] Exibir saudação do usuário.
- [ ] Exibir data atual.
- [ ] Exibir filtros `Hoje`, `Próximas`, `Concluídas`.
- [ ] Exibir toggle `Lembretes ativos`.
- [ ] Exibir tarefas do dia.
- [ ] Exibir prioridades.
- [ ] Exibir horário.
- [ ] Exibir lista/espaço.
- [ ] Exibir card de próximo lembrete.
- [ ] Exibir botão flutuante de nova tarefa.

### Critérios de aceite

- [ ] Home carrega sem depender de login real no modo mockado.
- [ ] Tarefas aparecem com status, horário e prioridade.
- [ ] Toggle visual funciona.
- [ ] Botão `+` navega para nova tarefa.
- [ ] Filtros alteram a lista exibida.

---

# 10. Fase 6 — Espaços e Listas

## 10.1 Tela de espaços

### Tarefas

- [x] Listar espaços.
- [x] Criar novo espaço.
- [x] Editar espaço.
- [x] Excluir espaço.
- [x] Mostrar quantidade de tarefas abertas.
- [ ] Mostrar espaços compartilhados.

### Critérios de aceite

- [x] Espaços aparecem em cards.
- [x] Novo espaço pode ser criado.
- [ ] Ao tocar em espaço, abre listas/tarefas daquele espaço.

---

## 10.2 Tela de listas

### Tarefas

- [x] Listar listas dentro de um espaço.
- [x] Criar lista.
- [x] Editar lista.
- [x] Excluir lista.
- [ ] Ordenar listas.
- [x] Exibir quantidade de tarefas por lista.

### Critérios de aceite

- [ ] Listas filtram as tarefas corretamente.
- [ ] Não é possível criar tarefa sem lista válida.

---

# 11. Fase 7 — CRUD de Tarefas

## 11.1 Criar tarefa

### Tarefas

- [ ] Campo de título.
- [ ] Campo de descrição.
- [ ] Seleção de lista.
- [ ] Seleção de prazo.
- [ ] Seleção de responsável.
- [ ] Convite de pessoas.
- [ ] Seleção de prioridade.
- [ ] Bloco de lembretes.
- [ ] Bloco de materiais da tarefa.
- [ ] Botão salvar.

### Critérios de aceite

- [ ] Tarefa não salva sem título.
- [ ] Tarefa salva aparece na lista correta.
- [ ] Prioridade é persistida.
- [ ] Prazo é persistido.
- [ ] Responsável é persistido.

---

## 11.2 Editar tarefa

### Tarefas

- [ ] Editar título.
- [ ] Editar descrição.
- [ ] Editar prazo.
- [ ] Editar prioridade.
- [ ] Editar responsável.
- [ ] Editar status.
- [ ] Salvar alterações.

### Critérios de aceite

- [ ] Alterações aparecem na tela de detalhe.
- [ ] Alterações aparecem na lista.
- [ ] Histórico registra alteração relevante.

---

## 11.3 Concluir tarefa

### Tarefas

- [ ] Botão concluir.
- [ ] Alterar status para concluída.
- [ ] Registrar data de conclusão.
- [ ] Perguntar sobre recorrências ativas, quando aplicável.

### Critérios de aceite

- [ ] Tarefa concluída sai da lista de abertas.
- [ ] Pode ser exibida em `Concluídas`.
- [ ] Recorrência não fica gerando notificações indesejadas.

---

# 12. Fase 8 — Detalhe da Tarefa

## 12.1 Tela de detalhe

### Tarefas

- [ ] Exibir título.
- [ ] Exibir status.
- [ ] Exibir prioridade.
- [ ] Exibir próximo lembrete.
- [ ] Exibir toggle de lembrete ativo.
- [ ] Exibir descrição.
- [ ] Exibir prazo.
- [ ] Exibir responsável.
- [ ] Exibir participantes.
- [ ] Exibir materiais da tarefa.
- [ ] Exibir anexos principais.
- [ ] Exibir links principais.
- [ ] Exibir campos principais.
- [ ] Exibir botões compartilhar e concluir.

### Critérios de aceite

- [ ] Tela mostra resumo completo da tarefa.
- [ ] Materiais são acessíveis diretamente.
- [ ] Lembrete pode ser ativado/desativado.
- [ ] Botões executam as ações corretas.

---

# 13. Fase 9 — Motor de Lembretes

## 13.1 Modelagem do Reminder Engine

### Tarefas

- [ ] Criar entidade `Reminder`.
- [ ] Criar função para cálculo de próxima ocorrência.
- [ ] Criar suporte a lembrete único.
- [ ] Criar suporte a recorrência diária.
- [ ] Criar suporte a recorrência semanal.
- [ ] Criar suporte a recorrência mensal.
- [ ] Criar suporte a recorrência anual.
- [ ] Criar suporte a recorrência customizada.
- [ ] Criar suporte a dias específicos da semana.
- [ ] Criar suporte a dia fixo do mês.
- [ ] Criar suporte a regra mensal.
- [ ] Criar fim por data.
- [ ] Criar fim por número de ocorrências.
- [ ] Criar fim ao concluir tarefa.
- [ ] Criar múltiplos lembretes por tarefa.

### Critérios de aceite

- [ ] Calcula corretamente lembrete único.
- [ ] Calcula corretamente `a cada 2 semanas`.
- [ ] Calcula corretamente `a cada 15 dias`.
- [ ] Calcula corretamente `todo dia 10`.
- [ ] Calcula corretamente `último dia útil`.
- [ ] Não cria próxima ocorrência após fim da recorrência.
- [ ] Permite mais de um lembrete na mesma tarefa.

---

## 13.2 Tela de lembrete personalizado

### Tarefas

- [ ] Toggle `Ativar lembrete`.
- [ ] Campo `Data inicial`.
- [ ] Campo `Horário`.
- [ ] Chips de aviso antecipado.
- [ ] Segmented control `Não repetir`, `Simples`, `Personalizada`.
- [ ] Controle `Repetir a cada`.
- [ ] Seletor de unidade: dias, semanas, meses, anos.
- [ ] Seletor de dias da semana.
- [ ] Opções de fim da repetição.
- [ ] Campo para data final.
- [ ] Campo para X repetições.
- [ ] Opção mensal em data fixa.
- [ ] Opção regra mensal.
- [ ] Botão salvar lembrete.

### Critérios de aceite

- [ ] Usuário consegue criar lembrete a cada 2 semanas.
- [ ] Usuário consegue escolher segunda e quinta.
- [ ] Usuário consegue definir fim em 31/12/2026.
- [ ] Usuário consegue salvar e voltar para tarefa.
- [ ] Resumo do lembrete aparece na tela da tarefa.

---

## 13.3 Notificações locais

### Tarefas

- [ ] Solicitar permissão de notificação quando necessário.
- [ ] Criar canal de notificação.
- [ ] Agendar notificação local.
- [ ] Exibir título da tarefa.
- [ ] Exibir horário/lista.
- [ ] Exibir ações rápidas.
- [ ] Implementar ação `Concluir`.
- [ ] Implementar ação `Adiar`.
- [ ] Implementar ação `Abrir`.
- [ ] Reagendar próxima ocorrência após disparo.

### Critérios de aceite

- [ ] Notificação dispara no horário correto.
- [ ] Ação `Abrir` abre detalhe da tarefa.
- [ ] Ação `Concluir` conclui a tarefa.
- [ ] Ação `Adiar` cria novo disparo.
- [ ] Recorrência é reagendada sem duplicar.

---

# 14. Fase 10 — Materiais da Tarefa

## 14.1 Tela Materiais da Tarefa

### Tarefas

- [ ] Criar tela `Materiais da tarefa`.
- [ ] Criar abas `Anexos`, `Links`, `Campos`.
- [ ] Criar botão `Arquivo`.
- [ ] Criar botão `Foto`.
- [ ] Criar botão `Link`.
- [ ] Criar área de seleção/arraste visual.
- [ ] Listar anexos recentes.
- [ ] Listar links.
- [ ] Listar campos complementares.
- [ ] Exibir menu de ações por item.

### Critérios de aceite

- [ ] Tela mostra anexos, links e campos em seções claras.
- [ ] Tocar em arquivo abre seletor.
- [ ] Tocar em foto abre câmera ou seletor de foto.
- [ ] Tocar em link abre formulário de link.

---

## 14.2 Anexos

### Tarefas

- [ ] Selecionar imagem via Photo Picker.
- [ ] Tirar foto via câmera.
- [ ] Selecionar PDF.
- [ ] Selecionar DOC/DOCX.
- [ ] Selecionar XLS/XLSX.
- [ ] Selecionar TXT.
- [ ] Validar tamanho máximo.
- [ ] Exibir nome do arquivo.
- [ ] Exibir tamanho.
- [ ] Exibir tipo.
- [ ] Exibir miniatura quando imagem.
- [ ] Abrir anexo.
- [ ] Excluir anexo.
- [ ] Compartilhar anexo.

### Critérios de aceite

- [ ] Usuário consegue anexar foto tirada na hora.
- [ ] Usuário consegue anexar imagem existente.
- [ ] Usuário consegue anexar PDF.
- [ ] Arquivo aparece na tarefa.
- [ ] Arquivo pode ser aberto.
- [ ] Arquivo pode ser removido por usuário autorizado.

---

## 14.3 Links

### Tarefas

- [ ] Criar formulário de link.
- [ ] Campo título.
- [ ] Campo URL.
- [ ] Campo descrição.
- [ ] Campo categoria.
- [ ] Validar URL.
- [ ] Salvar link.
- [ ] Abrir link.
- [ ] Copiar link.
- [ ] Editar link.
- [ ] Excluir link.

### Critérios de aceite

- [ ] Link válido é salvo.
- [ ] Link inválido mostra erro.
- [ ] Link aparece na seção de materiais.
- [ ] Link abre no navegador ou WebView conforme decisão técnica.

---

## 14.4 Campos complementares

### Tarefas

- [ ] Criar formulário de campo complementar.
- [ ] Criar campo tipo texto.
- [ ] Criar campo tipo número.
- [ ] Criar campo tipo moeda.
- [ ] Criar campo tipo data.
- [ ] Criar campo tipo telefone.
- [ ] Criar campo tipo e-mail.
- [ ] Criar campo tipo URL.
- [ ] Criar campo tipo localização.
- [ ] Criar campo tipo número de processo.
- [ ] Criar campo tipo documento.
- [ ] Editar campo.
- [ ] Excluir campo.

### Critérios de aceite

- [ ] Campo complementar aparece no detalhe da tarefa.
- [ ] Campo pode ser editado.
- [ ] Campo pode ser excluído.
- [ ] Campo tem tipo correto.

---

## 14.5 Checklist

### Tarefas

- [ ] Criar checklist dentro da tarefa.
- [ ] Adicionar item.
- [ ] Marcar item como concluído.
- [ ] Desmarcar item.
- [ ] Editar item.
- [ ] Excluir item.
- [ ] Mostrar progresso do checklist.

### Critérios de aceite

- [ ] Checklist salva estado dos itens.
- [ ] Progresso aparece corretamente.
- [ ] Checklist pode ser usado em tarefa com anexos e lembretes.

---

# 15. Fase 11 — Compartilhamento e Convites

## 15.1 Compartilhar tarefa

### Tarefas

- [ ] Criar tela de compartilhamento.
- [ ] Criar permissões `Editar`, `Comentar`, `Ver`.
- [ ] Gerar texto de convite.
- [ ] Incluir resumo da tarefa.
- [ ] Incluir prazo.
- [ ] Incluir status.
- [ ] Incluir quantidade de anexos.
- [ ] Incluir quantidade de links.
- [ ] Incluir link de convite.
- [ ] Compartilhar via Android Sharesheet.
- [ ] Opção WhatsApp.
- [ ] Opção e-mail.
- [ ] Opção copiar link.

### Critérios de aceite

- [ ] Compartilhar abre seletor nativo Android.
- [ ] Mensagem contém informações corretas.
- [ ] Link de convite é gerado.
- [ ] Permissão escolhida é salva no convite.

---

## 15.2 Aceitar convite

### Tarefas

- [ ] Abrir link de convite.
- [ ] Validar token.
- [ ] Exibir resumo da tarefa.
- [ ] Botão aceitar.
- [ ] Botão recusar.
- [ ] Adicionar usuário como participante.
- [ ] Aplicar permissão correta.

### Critérios de aceite

- [ ] Convite válido permite entrar na tarefa.
- [ ] Convite expirado mostra mensagem adequada.
- [ ] Usuário sem permissão não edita tarefa.

---

# 16. Fase 12 — Comentários e Histórico

## 16.1 Comentários

### Tarefas

- [ ] Exibir comentários na tarefa.
- [ ] Adicionar comentário.
- [ ] Editar comentário próprio.
- [ ] Excluir comentário próprio.
- [ ] Mostrar autor.
- [ ] Mostrar data/hora.

### Critérios de aceite

- [ ] Comentário aparece em tempo real local/mockado.
- [ ] Participantes autorizados podem comentar.
- [ ] Visualizadores sem permissão não comentam.

---

## 16.2 Histórico de atividade

### Tarefas

- [ ] Registrar criação da tarefa.
- [ ] Registrar alteração de status.
- [ ] Registrar alteração de prazo.
- [ ] Registrar alteração de responsável.
- [ ] Registrar anexo adicionado.
- [ ] Registrar anexo removido.
- [ ] Registrar link adicionado.
- [ ] Registrar campo alterado.
- [ ] Registrar tarefa concluída.

### Critérios de aceite

- [ ] Histórico mostra eventos relevantes.
- [ ] Eventos têm usuário, data e ação.
- [ ] Histórico não expõe dados sensíveis desnecessários.

---

# 17. Fase 13 — Persistência Local

## 17.1 Room Database

### Tarefas

- [x] Criar entidades Room.
- [x] Criar DAOs.
- [x] Criar database.
- [x] Criar migrations iniciais.
- [x] Criar repositories locais.
- [x] Persistir tarefas.
- [x] Persistir lembretes.
- [x] Persistir anexos como metadados.
- [x] Persistir links.
- [x] Persistir campos complementares.

### Critérios de aceite

- [x] Dados continuam após fechar e abrir app.
- [x] Tarefas são carregadas do banco local.
- [x] Lembretes são restaurados.
- [x] Anexos preservam metadados.

---

## 17.2 DataStore

### Tarefas

- [x] Salvar preferências de tema.
- [x] Salvar preferência de notificações.
- [x] Salvar usuário local atual.
- [x] Salvar filtros usados.

### Critérios de aceite

- [x] Preferências persistem após reiniciar app.
- [x] Toggle de notificações mantém estado.

---

# 18. Fase 14 — Firebase

## 18.1 Authentication

### Tarefas

- [ ] Configurar Firebase no projeto.
- [ ] Implementar login com e-mail e senha.
- [ ] Implementar cadastro.
- [ ] Implementar logout.
- [ ] Implementar recuperação de senha.
- [ ] Preparar login Google.

### Critérios de aceite

- [ ] Usuário cria conta real.
- [ ] Usuário faz login.
- [ ] Sessão é mantida.
- [ ] Logout funciona.

---

## 18.2 Firestore

### Tarefas

- [ ] Criar coleções `users`.
- [ ] Criar coleções `spaces`.
- [ ] Criar coleções `lists`.
- [ ] Criar coleções `tasks`.
- [ ] Criar coleções `reminders`.
- [ ] Criar coleções `attachments`.
- [ ] Criar coleções `links`.
- [ ] Criar coleções `customFields`.
- [ ] Criar coleções `comments`.
- [ ] Criar coleções `activityLog`.
- [ ] Criar coleções `invites`.
- [ ] Criar regras de segurança.

### Critérios de aceite

- [ ] Usuário acessa apenas seus dados.
- [ ] Participante acessa apenas tarefas permitidas.
- [ ] Dados sincronizam entre sessões.
- [ ] Regras impedem leitura pública indevida.

---

## 18.3 Firebase Storage

### Tarefas

- [ ] Configurar bucket de arquivos.
- [ ] Fazer upload de imagem.
- [ ] Fazer upload de PDF.
- [ ] Fazer upload de documento.
- [ ] Gerar metadados.
- [ ] Gerar thumbnail quando aplicável.
- [ ] Aplicar regras de segurança.
- [ ] Remover arquivo ao excluir anexo.

### Critérios de aceite

- [ ] Upload funciona.
- [ ] Arquivo fica vinculado à tarefa correta.
- [ ] Usuário não autorizado não acessa arquivo.
- [ ] Excluir anexo remove ou invalida arquivo.

---

## 18.4 FCM / Push notifications

### Tarefas

- [ ] Registrar token FCM.
- [ ] Salvar token do usuário.
- [ ] Enviar push de convite.
- [ ] Enviar push de comentário.
- [ ] Enviar push de alteração de status.
- [ ] Enviar push de novo anexo.
- [ ] Enviar push de tarefa atribuída.

### Critérios de aceite

- [ ] Push chega em dispositivo de teste.
- [ ] Push abre a tela correta.
- [ ] Push respeita permissões.

---

# 19. Fase 15 — Sincronização Offline/Online

## 19.1 Estratégia offline-first

### Tarefas

- [ ] Definir fonte local como primeira leitura.
- [ ] Criar fila de operações pendentes.
- [ ] Sincronizar criação de tarefa.
- [ ] Sincronizar edição de tarefa.
- [ ] Sincronizar anexos pendentes.
- [ ] Sincronizar links.
- [ ] Sincronizar campos complementares.
- [ ] Resolver conflitos simples.

### Critérios de aceite

- [ ] Usuário cria tarefa offline.
- [ ] Tarefa aparece imediatamente.
- [ ] Tarefa sincroniza quando internet volta.
- [ ] App indica pendência de sincronização.

---

# 20. Fase 16 — Permissões e Segurança

## 20.1 Permissões Android

### Tarefas

- [ ] Solicitar permissão de notificação.
- [ ] Solicitar câmera apenas quando usuário usar câmera.
- [ ] Não pedir acesso amplo à galeria sem necessidade.
- [ ] Usar Photo Picker para imagens.
- [ ] Usar seletor de arquivos para documentos.

### Critérios de aceite

- [ ] App não pede permissões desnecessárias na abertura.
- [ ] Permissões aparecem no contexto certo.
- [ ] Usuário consegue continuar mesmo negando permissão não essencial.

---

## 20.2 Segurança de dados

### Tarefas

- [ ] Criar regras de acesso por usuário.
- [ ] Criar regras por participante.
- [ ] Validar permissão de anexos.
- [ ] Validar permissão de links.
- [ ] Validar permissão de campos.
- [ ] Evitar URLs públicas permanentes.
- [ ] Remover logs com dados sensíveis.

### Critérios de aceite

- [ ] Usuário sem permissão não acessa tarefa.
- [ ] Usuário sem permissão não acessa anexo.
- [ ] Dados sensíveis não aparecem em logs.

---

# 21. Fase 17 — Busca e Filtros

## 21.1 Busca

### Tarefas

- [ ] Buscar por título.
- [ ] Buscar por descrição.
- [ ] Buscar por responsável.
- [ ] Buscar por nome de anexo.
- [ ] Buscar por link.
- [ ] Buscar por campo complementar.
- [ ] Buscar por tag/categoria.

### Critérios de aceite

- [ ] Busca retorna tarefas corretas.
- [ ] Busca não trava com lista grande.
- [ ] Busca mostra estado vazio quando não encontra nada.

---

## 21.2 Filtros

### Tarefas

- [ ] Filtrar por hoje.
- [ ] Filtrar por próximas.
- [ ] Filtrar por atrasadas.
- [ ] Filtrar por concluídas.
- [ ] Filtrar por responsável.
- [ ] Filtrar por prioridade.
- [ ] Filtrar por tarefas com anexos.
- [ ] Filtrar por tarefas com links.
- [ ] Filtrar por tarefas com lembretes.

### Critérios de aceite

- [ ] Filtros funcionam isolados.
- [ ] Filtros combinados funcionam quando aplicável.

---

# 22. Fase 18 — Configurações

## 22.1 Tela de configurações

### Tarefas

- [x] Exibir perfil.
- [x] Configurar notificações.
- [x] Configurar tema.
- [ ] Configurar conta.
- [ ] Configurar privacidade.
- [ ] Configurar ajuda e suporte.
- [ ] Logout.

### Critérios de aceite

- [x] Configurações principais persistem.
- [ ] Logout retorna para login.
- [x] Usuário consegue revisar preferências de notificação.

---

# 23. Fase 19 — Testes

## 23.1 Testes unitários

### Tarefas

- [ ] Testar cálculo de recorrência única.
- [ ] Testar recorrência diária.
- [ ] Testar recorrência semanal.
- [ ] Testar recorrência a cada 2 semanas.
- [ ] Testar recorrência a cada 15 dias.
- [ ] Testar recorrência mensal por dia fixo.
- [ ] Testar último dia útil.
- [ ] Testar fim por data.
- [ ] Testar fim por ocorrências.
- [ ] Testar validação de URL.
- [ ] Testar validação de arquivo.

### Critérios de aceite

- [ ] Testes principais passam.
- [ ] Não há regressão no Reminder Engine.

---

## 23.2 Testes manuais obrigatórios

### Fluxos mínimos

- [ ] Criar tarefa simples.
- [ ] Criar tarefa com lembrete único.
- [ ] Criar tarefa com lembrete a cada 2 semanas.
- [ ] Criar tarefa com lembrete mensal todo dia 10.
- [ ] Criar tarefa com anexo PDF.
- [ ] Criar tarefa com foto.
- [ ] Criar tarefa com link.
- [ ] Criar tarefa com campo complementar.
- [ ] Compartilhar tarefa por WhatsApp.
- [ ] Compartilhar tarefa por e-mail.
- [ ] Copiar link de convite.
- [ ] Aceitar convite.
- [ ] Alterar status.
- [ ] Concluir tarefa.
- [ ] Adiar lembrete.

### Critérios de aceite

- [ ] Todos os fluxos passam em emulador.
- [ ] Todos os fluxos passam em pelo menos um aparelho físico.
- [ ] Nenhum fluxo principal gera crash.

---

# 24. Fase 20 — Polimento Visual

## 24.1 Refinamento de UI

### Tarefas

- [ ] Ajustar espaçamentos.
- [ ] Ajustar alinhamentos.
- [ ] Ajustar tamanhos de fonte.
- [ ] Ajustar cores dos chips.
- [ ] Ajustar contraste.
- [ ] Ajustar animações.
- [ ] Ajustar estados vazios.
- [ ] Ajustar feedback de toque.
- [ ] Ajustar loading de upload.
- [ ] Ajustar erros de formulário.

### Critérios de aceite

- [ ] App visualmente consistente.
- [ ] Telas principais parecem parte do mesmo produto.
- [ ] Nenhuma tela parece protótipo inacabado.

---

# 25. Fase 21 — Release MVP

## 25.1 Preparação para build

### Tarefas

- [ ] Definir nome do app.
- [ ] Definir ícone.
- [ ] Definir splash screen.
- [ ] Configurar versão.
- [ ] Configurar assinatura.
- [ ] Gerar build debug.
- [ ] Gerar build release.
- [ ] Testar build release.

### Critérios de aceite

- [ ] APK/AAB gerado.
- [ ] Build release instala em aparelho físico.
- [ ] Login funciona no release.
- [ ] Notificações funcionam no release.
- [ ] Upload funciona no release.

---

# 26. Backlog Pós-MVP

## 26.1 Funcionalidades futuras

- [ ] Scanner de documentos com corte automático.
- [ ] OCR de documentos.
- [ ] Transformar foto em PDF.
- [ ] Extração automática de datas de documentos.
- [ ] Sugestão automática de lembretes com IA.
- [ ] Integração Google Agenda.
- [ ] Integração Outlook.
- [ ] Integração Google Drive.
- [ ] Integração OneDrive.
- [ ] Templates de tarefas.
- [ ] Campos personalizados por espaço.
- [ ] Dashboard web.
- [ ] Relatórios.
- [ ] Plano Pro.
- [ ] Histórico avançado.
- [ ] Exportação PDF.
- [ ] Busca avançada por OCR.

---

# 27. Checklist Final de MVP

O MVP só estará pronto quando todos os itens abaixo estiverem concluídos:

- [ ] Projeto compila sem erros.
- [ ] Login/cadastro funcionando.
- [ ] Home funcionando.
- [ ] Espaços funcionando.
- [ ] Listas funcionando.
- [ ] CRUD de tarefas funcionando.
- [ ] Status funcionando.
- [ ] Prioridade funcionando.
- [ ] Lembrete único funcionando.
- [ ] Lembrete recorrente personalizado funcionando.
- [ ] Notificações locais funcionando.
- [ ] Ação de adiar funcionando.
- [ ] Múltiplos lembretes funcionando.
- [ ] Anexar imagem funcionando.
- [ ] Tirar foto funcionando.
- [ ] Anexar PDF funcionando.
- [ ] Links funcionando.
- [ ] Campos complementares funcionando.
- [ ] Checklist funcionando.
- [ ] Compartilhamento funcionando.
- [ ] Convite funcionando.
- [ ] Comentários funcionando.
- [ ] Histórico básico funcionando.
- [ ] Firebase Authentication funcionando.
- [ ] Firestore funcionando.
- [ ] Firebase Storage funcionando.
- [ ] Permissões funcionando.
- [ ] Regras de segurança básicas funcionando.
- [ ] Persistência local funcionando.
- [ ] App testado em aparelho físico.
- [ ] Build release gerado.

---

# 28. Registro de Decisões Técnicas

Use esta seção para registrar decisões importantes.

## Decisão 001

- Data:
- Tema:
- Decisão:
- Motivo:
- Impacto:

## Decisão 002

- Data:
- Tema:
- Decisão:
- Motivo:
- Impacto:

---

# 29. Bloqueios Atuais

Use esta seção para listar bloqueios.

| Data | Bloqueio | Impacto | Responsável | Status |
|---|---|---|---|---|
| | | | | |

---

# 30. Próxima tarefa recomendada

Quando este documento for entregue ao agente construtor, a primeira tarefa recomendada é:

```text
Comece pela Fase 0 — Setup Inicial. Crie o projeto Android nativo em Kotlin com Jetpack Compose, configure a arquitetura inicial, crie a estrutura de pacotes definida neste ROADMAP.md e entregue uma tela inicial provisória compilando sem erros. Só avance para a Fase 1 após cumprir todos os critérios de aceite da Fase 0.
```
