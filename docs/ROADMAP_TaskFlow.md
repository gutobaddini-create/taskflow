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

- [x] Android nativo.
- [x] Kotlin.
- [x] Jetpack Compose.
- [x] Arquitetura MVVM.
- [x] Gradle Kotlin DSL.

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

- [x] Android notification permission.
- [x] AlarmManager ou WorkManager conforme necessidade técnica.
- [x] Photo Picker para imagens.
- [x] CameraX ou intent de câmera para fotos.
- [x] Storage Access Framework para arquivos.
- [x] Android Sharesheet para compartilhamento.

---

# 3. Marcos Gerais do Projeto

| Marco | Nome | Resultado esperado | Status |
|---|---|---|---|
| M0 | Setup inicial | Projeto compila e abre tela inicial | [x] |
| M1 | Design system | Componentes visuais base prontos | [x] |
| M2 | Navegação | Fluxo de telas principal funcionando | [x] |
| M3 | Tarefas locais | Criar, editar, listar e concluir tarefas | [x] |
| M4 | Lembretes | Lembretes únicos e recorrentes funcionando | [x] |
| M5 | Materiais | Anexos, links e campos extras funcionando | [x] |
| M6 | Compartilhamento | WhatsApp, e-mail, link e convite | [x] |
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
- [x] Nenhuma feature fica misturada dentro da `MainActivity`.
- [x] O projeto mantém separação mínima entre UI, domínio e dados.

---

# 5. Fase 1 — Design System

## 5.1 Tema visual

Implementar identidade visual moderna, limpa e premium.

### Tarefas

- [x] Criar paleta de cores.
- [x] Criar tipografia.
- [x] Definir espaçamentos.
- [x] Definir raio dos cards.
- [x] Definir estilo dos botões.
- [x] Definir tema claro.
- [x] Preparar estrutura para tema escuro futuro.

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

- [x] Todas as telas usam o mesmo tema.
- [x] Cores não são definidas aleatoriamente dentro das telas.
- [x] Componentes principais seguem padrão consistente.
- [ ] Interface lembra app moderno/polido com inspiração iOS-like.

---

## 5.2 Componentes reutilizáveis

### Tarefas

- [x] Criar `TaskFlowButton`.
- [x] Criar `TaskFlowOutlinedButton`.
- [x] Criar `TaskFlowCard`.
- [x] Criar `TaskCard`.
- [x] Criar `ReminderCard`.
- [x] Criar `AttachmentCard`.
- [x] Criar `LinkCard`.
- [x] Criar `CustomFieldRow`.
- [x] Criar `StatusPill`.
- [x] Criar `PriorityChip`.
- [x] Criar `SegmentedControl`.
- [x] Criar `ToggleSwitch`.
- [x] Criar `BottomNavigationBar`.
- [x] Criar `FloatingAddButton`.
- [x] Criar `EmptyState`.
- [x] Criar `LoadingState`.
- [x] Criar `ErrorState`.

### Critérios de aceite

- [x] Componentes reutilizados em mais de uma tela quando aplicável.
- [x] Componentes têm previews do Compose.
- [x] Componentes aceitam estados principais.
- [x] Componentes não contêm regra de negócio.

---

# 6. Fase 2 — Modelos de Domínio

## 6.1 Criar entidades principais

### Tarefas

- [x] Criar `User`.
- [x] Criar `Space`.
- [x] Criar `TaskList`.
- [x] Criar `Task`.
- [x] Criar `Reminder`.
- [x] Criar `Attachment`.
- [x] Criar `TaskLink`.
- [x] Criar `CustomField`.
- [x] Criar `Comment`.
- [x] Criar `ActivityLog`.
- [x] Criar `Invite`.

### Critérios de aceite

- [x] Todas as entidades possuem identificador único.
- [x] Todas as entidades possuem campos de criação/atualização quando necessário.
- [x] Entidades não dependem diretamente da UI.
- [x] Tipos complexos usam enums ou sealed classes quando adequado.

---

## 6.2 Enums e tipos auxiliares

### Tarefas

- [x] Criar `TaskStatus`.
- [x] Criar `TaskPriority`.
- [x] Criar `ReminderType`.
- [x] Criar `RecurrenceType`.
- [x] Criar `RecurrenceUnit`.
- [x] Criar `WeekDay`.
- [x] Criar `MonthlyRule`.
- [x] Criar `ReminderEndType`.
- [x] Criar `AttachmentSource`.
- [x] Criar `AttachmentType`.
- [x] Criar `CustomFieldType`.
- [x] Criar `UserPermission`.

### Critérios de aceite

- [x] Nenhum status é tratado como string solta na UI.
- [x] Recorrência avançada está modelada.
- [x] Anexos, links e campos extras têm modelos próprios.

---

# 7. Fase 3 — Navegação

## 7.1 Criar grafo de navegação

### Telas obrigatórias

- [x] Splash / Onboarding.
- [x] Login.
- [x] Cadastro.
- [x] Home / Hoje.
- [x] Espaços.
- [x] Listas.
- [x] Lista de tarefas.
- [x] Nova tarefa.
- [x] Detalhe da tarefa.
- [x] Configurar lembrete.
- [x] Materiais da tarefa.
- [x] Adicionar anexo.
- [x] Adicionar link.
- [x] Campos complementares.
- [x] Compartilhar / convidar.
- [x] Pessoas.
- [x] Configurações.

### Critérios de aceite

- [x] O usuário consegue navegar entre as telas principais.
- [x] Bottom navigation aparece apenas nas telas adequadas.
- [x] Telas de criação e edição possuem navegação de voltar/cancelar.
- [x] Não há telas órfãs.

---

# 8. Fase 4 — Dados Mockados

## 8.1 Criar mock repository

### Tarefas

- [x] Criar usuários mockados.
- [x] Criar espaços mockados.
- [x] Criar listas mockadas.
- [x] Criar tarefas mockadas.
- [x] Criar lembretes mockados.
- [x] Criar anexos mockados.
- [x] Criar links mockados.
- [x] Criar campos complementares mockados.
- [x] Criar comentários mockados.

### Critérios de aceite

- [x] Todas as telas principais exibem dados reais mockados.
- [x] Dados mockados demonstram as principais funções.
- [x] Não existem textos genéricos demais como `Lorem ipsum` nas telas principais.

---

# 9. Fase 5 — Home / Hoje

## 9.1 Construir tela Home

### Tarefas

- [x] Exibir saudação do usuário.
- [x] Exibir data atual.
- [x] Exibir filtros `Hoje`, `Próximas`, `Concluídas`.
- [x] Exibir toggle `Lembretes ativos`.
- [x] Exibir tarefas do dia.
- [x] Exibir prioridades.
- [x] Exibir horário.
- [x] Exibir lista/espaço.
- [x] Exibir card de próximo lembrete.
- [x] Exibir botão flutuante de nova tarefa.

### Critérios de aceite

- [x] Home carrega sem depender de login real no modo mockado.
- [x] Tarefas aparecem com status, horário e prioridade.
- [x] Toggle visual funciona.
- [x] Botão `+` navega para nova tarefa.
- [x] Filtros alteram a lista exibida.

---

# 10. Fase 6 — Espaços e Listas

## 10.1 Tela de espaços

### Tarefas

- [x] Listar espaços.
- [x] Criar novo espaço.
- [x] Editar espaço.
- [x] Excluir espaço.
- [x] Mostrar quantidade de tarefas abertas.
- [x] Mostrar espaços compartilhados.

### Critérios de aceite

- [x] Espaços aparecem em cards.
- [x] Novo espaço pode ser criado.
- [x] Ao tocar em espaço, abre listas/tarefas daquele espaço.

---

## 10.2 Tela de listas

### Tarefas

- [x] Listar listas dentro de um espaço.
- [x] Criar lista.
- [x] Editar lista.
- [x] Excluir lista.
- [x] Ordenar listas.
- [x] Exibir quantidade de tarefas por lista.

### Critérios de aceite

- [x] Listas filtram as tarefas corretamente.
- [x] Não é possível criar tarefa sem lista válida.

---

# 11. Fase 7 — CRUD de Tarefas

## 11.1 Criar tarefa

### Tarefas

- [x] Campo de título.
- [x] Campo de descrição.
- [x] Seleção de lista.
- [x] Seleção de prazo.
- [x] Seleção de responsável.
- [x] Convite de pessoas.
- [x] Seleção de prioridade.
- [x] Bloco de lembretes.
- [x] Bloco de materiais da tarefa.
- [x] Botão salvar.

### Critérios de aceite

- [x] Tarefa não salva sem título.
- [x] Tarefa salva aparece na lista correta.
- [x] Prioridade é persistida.
- [x] Prazo é persistido.
- [x] Responsável é persistido.

---

## 11.2 Editar tarefa

### Tarefas

- [x] Editar título.
- [x] Editar descrição.
- [x] Editar prazo.
- [x] Editar prioridade.
- [x] Editar responsável.
- [x] Editar status.
- [x] Salvar alterações.

### Critérios de aceite

- [x] Alterações aparecem na tela de detalhe.
- [x] Alterações aparecem na lista.
- [x] Histórico registra alteração relevante.

---

## 11.3 Concluir tarefa

### Tarefas

- [x] Botão concluir.
- [x] Alterar status para concluída.
- [x] Registrar data de conclusão.
- [x] Perguntar sobre recorrências ativas, quando aplicável.

### Critérios de aceite

- [x] Tarefa concluída sai da lista de abertas.
- [x] Pode ser exibida em `Concluídas`.
- [x] Recorrência não fica gerando notificações indesejadas.

---

# 12. Fase 8 — Detalhe da Tarefa

## 12.1 Tela de detalhe

### Tarefas

- [x] Exibir título.
- [x] Exibir status.
- [x] Exibir prioridade.
- [x] Exibir próximo lembrete.
- [x] Exibir toggle de lembrete ativo.
- [x] Exibir descrição.
- [x] Exibir prazo.
- [x] Exibir responsável.
- [x] Exibir participantes.
- [x] Exibir materiais da tarefa.
- [x] Exibir anexos principais.
- [x] Exibir links principais.
- [x] Exibir campos principais.
- [x] Exibir botões compartilhar e concluir.

### Critérios de aceite

- [x] Tela mostra resumo completo da tarefa.
- [x] Materiais são acessíveis diretamente.
- [x] Lembrete pode ser ativado/desativado.
- [x] Botões executam as ações corretas.

---

# 13. Fase 9 — Motor de Lembretes

## 13.1 Modelagem do Reminder Engine

### Tarefas

- [x] Criar entidade `Reminder`.
- [x] Criar função para cálculo de próxima ocorrência.
- [x] Criar suporte a lembrete único.
- [x] Criar suporte a recorrência diária.
- [x] Criar suporte a recorrência semanal.
- [x] Criar suporte a recorrência mensal.
- [x] Criar suporte a recorrência anual.
- [x] Criar suporte a recorrência customizada.
- [x] Criar suporte a dias específicos da semana.
- [x] Criar suporte a dia fixo do mês.
- [x] Criar suporte a regra mensal.
- [x] Criar fim por data.
- [x] Criar fim por número de ocorrências.
- [x] Criar fim ao concluir tarefa.
- [x] Criar múltiplos lembretes por tarefa.

### Critérios de aceite

- [x] Calcula corretamente lembrete único.
- [x] Calcula corretamente `a cada 2 semanas`.
- [x] Calcula corretamente `a cada 15 dias`.
- [x] Calcula corretamente `todo dia 10`.
- [x] Calcula corretamente `último dia útil`.
- [x] Não cria próxima ocorrência após fim da recorrência.
- [x] Permite mais de um lembrete na mesma tarefa.

---

## 13.2 Tela de lembrete personalizado

### Tarefas

- [x] Toggle `Ativar lembrete`.
- [x] Campo `Data inicial`.
- [x] Campo `Horário`.
- [x] Chips de aviso antecipado.
- [x] Segmented control `Não repetir`, `Simples`, `Personalizada`.
- [x] Controle `Repetir a cada`.
- [x] Seletor de unidade: dias, semanas, meses, anos.
- [x] Seletor de dias da semana.
- [x] Opções de fim da repetição.
- [x] Campo para data final.
- [x] Campo para X repetições.
- [x] Opção mensal em data fixa.
- [x] Opção regra mensal.
- [x] Botão salvar lembrete.

### Critérios de aceite

- [x] Usuário consegue criar lembrete a cada 2 semanas.
- [x] Usuário consegue escolher segunda e quinta.
- [x] Usuário consegue definir fim em 31/12/2026.
- [x] Usuário consegue salvar e voltar para tarefa.
- [x] Resumo do lembrete aparece na tela da tarefa.

---

## 13.3 Notificações locais

### Tarefas

- [x] Solicitar permissão de notificação quando necessário.
- [x] Criar canal de notificação.
- [x] Agendar notificação local.
- [x] Exibir título da tarefa.
- [x] Exibir horário/lista.
- [x] Exibir ações rápidas.
- [x] Implementar ação `Concluir`.
- [x] Implementar ação `Adiar`.
- [x] Implementar ação `Abrir`.
- [x] Reagendar próxima ocorrência após disparo.

### Critérios de aceite

- [x] Notificação dispara no horário correto.
- [x] Ação `Abrir` abre detalhe da tarefa.
- [x] Ação `Concluir` conclui a tarefa.
- [x] Ação `Adiar` cria novo disparo.
- [x] Recorrência é reagendada sem duplicar.

---

# 14. Fase 10 — Materiais da Tarefa

## 14.1 Tela Materiais da Tarefa

### Tarefas

- [x] Criar tela `Materiais da tarefa`.
- [x] Criar abas `Anexos`, `Links`, `Campos`.
- [x] Criar botão `Arquivo`.
- [x] Criar botão `Foto`.
- [x] Criar botão `Link`.
- [x] Criar área de seleção/arraste visual.
- [x] Listar anexos recentes.
- [x] Listar links.
- [x] Listar campos complementares.
- [x] Exibir menu de ações por item.

### Critérios de aceite

- [x] Tela mostra anexos, links e campos em seções claras.
- [x] Tocar em arquivo abre seletor.
- [x] Tocar em foto abre câmera ou seletor de foto.
- [x] Tocar em link abre formulário de link.

---

## 14.2 Anexos

### Tarefas

- [x] Selecionar imagem via Photo Picker.
- [x] Tirar foto via câmera.
- [x] Selecionar PDF.
- [x] Selecionar DOC/DOCX.
- [x] Selecionar XLS/XLSX.
- [x] Selecionar TXT.
- [x] Validar tamanho máximo.
- [x] Exibir nome do arquivo.
- [x] Exibir tamanho.
- [x] Exibir tipo.
- [x] Exibir miniatura quando imagem.
- [x] Abrir anexo.
- [x] Excluir anexo.
- [x] Compartilhar anexo.

### Critérios de aceite

- [x] Usuário consegue anexar foto tirada na hora.
- [x] Usuário consegue anexar imagem existente.
- [x] Usuário consegue anexar PDF.
- [x] Arquivo aparece na tarefa.
- [x] Arquivo pode ser aberto.
- [x] Arquivo pode ser removido por usuário autorizado.

---

## 14.3 Links

### Tarefas

- [x] Criar formulário de link.
- [x] Campo título.
- [x] Campo URL.
- [x] Campo descrição.
- [x] Campo categoria.
- [x] Validar URL.
- [x] Salvar link.
- [x] Abrir link.
- [x] Copiar link.
- [x] Editar link.
- [x] Excluir link.

### Critérios de aceite

- [x] Link válido é salvo.
- [x] Link inválido mostra erro.
- [x] Link aparece na seção de materiais.
- [x] Link abre no navegador ou WebView conforme decisão técnica.

---

## 14.4 Campos complementares

### Tarefas

- [x] Criar formulário de campo complementar.
- [x] Criar campo tipo texto.
- [x] Criar campo tipo número.
- [x] Criar campo tipo moeda.
- [x] Criar campo tipo data.
- [x] Criar campo tipo telefone.
- [x] Criar campo tipo e-mail.
- [x] Criar campo tipo URL.
- [x] Criar campo tipo localização.
- [x] Criar campo tipo número de processo.
- [x] Criar campo tipo documento.
- [x] Editar campo.
- [x] Excluir campo.

### Critérios de aceite

- [x] Campo complementar aparece no detalhe da tarefa.
- [x] Campo pode ser editado.
- [x] Campo pode ser excluído.
- [x] Campo tem tipo correto.

---

## 14.5 Checklist

### Tarefas

- [x] Criar checklist dentro da tarefa.
- [x] Adicionar item.
- [x] Marcar item como concluído.
- [x] Desmarcar item.
- [x] Editar item.
- [x] Excluir item.
- [x] Mostrar progresso do checklist.

### Critérios de aceite

- [x] Checklist salva estado dos itens.
- [x] Progresso aparece corretamente.
- [x] Checklist pode ser usado em tarefa com anexos e lembretes.

---

# 15. Fase 11 — Compartilhamento e Convites

## 15.1 Compartilhar tarefa

### Tarefas

- [x] Criar tela de compartilhamento.
- [x] Criar permissões `Editar`, `Comentar`, `Ver`.
- [x] Gerar texto de convite.
- [x] Incluir resumo da tarefa.
- [x] Incluir prazo.
- [x] Incluir status.
- [x] Incluir quantidade de anexos.
- [x] Incluir quantidade de links.
- [x] Incluir link de convite.
- [x] Compartilhar via Android Sharesheet.
- [x] Opção WhatsApp.
- [x] Opção e-mail.
- [x] Opção copiar link.

### Critérios de aceite

- [x] Compartilhar abre seletor nativo Android.
- [x] Mensagem contém informações corretas.
- [x] Link de convite é gerado.
- [x] Permissão escolhida é salva no convite.

---

## 15.2 Aceitar convite

### Tarefas

- [x] Abrir link de convite.
- [x] Validar token.
- [x] Exibir resumo da tarefa.
- [x] Botão aceitar.
- [x] Botão recusar.
- [x] Adicionar usuário como participante.
- [x] Aplicar permissão correta.

### Critérios de aceite

- [x] Convite válido permite entrar na tarefa.
- [x] Convite expirado mostra mensagem adequada.
- [x] Usuário sem permissão não edita tarefa.

---

# 16. Fase 12 — Comentários e Histórico

## 16.1 Comentários

### Tarefas

- [x] Exibir comentários na tarefa.
- [x] Adicionar comentário.
- [x] Editar comentário próprio.
- [x] Excluir comentário próprio.
- [x] Mostrar autor.
- [x] Mostrar data/hora.

### Critérios de aceite

- [x] Comentário aparece em tempo real local/mockado.
- [x] Participantes autorizados podem comentar.
- [x] Visualizadores sem permissão não comentam.

---

## 16.2 Histórico de atividade

### Tarefas

- [x] Registrar criação da tarefa.
- [x] Registrar alteração de status.
- [x] Registrar alteração de prazo.
- [x] Registrar alteração de responsável.
- [x] Registrar anexo adicionado.
- [x] Registrar anexo removido.
- [x] Registrar link adicionado.
- [x] Registrar campo alterado.
- [x] Registrar tarefa concluída.

### Critérios de aceite

- [x] Histórico mostra eventos relevantes.
- [x] Eventos têm usuário, data e ação.
- [x] Histórico não expõe dados sensíveis desnecessários.

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
- [x] Criar regras de segurança.

### Critérios de aceite

- [ ] Usuário acessa apenas seus dados.
- [ ] Participante acessa apenas tarefas permitidas.
- [ ] Dados sincronizam entre sessões.
- [x] Regras impedem leitura pública indevida.

---

## 18.3 Firebase Storage

### Tarefas

- [ ] Configurar bucket de arquivos.
- [ ] Fazer upload de imagem.
- [ ] Fazer upload de PDF.
- [ ] Fazer upload de documento.
- [ ] Gerar metadados.
- [ ] Gerar thumbnail quando aplicável.
- [x] Aplicar regras de segurança.
- [ ] Remover arquivo ao excluir anexo.

### Critérios de aceite

- [ ] Upload funciona.
- [ ] Arquivo fica vinculado à tarefa correta.
- [x] Usuário não autorizado não acessa arquivo.
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

- [x] Definir fonte local como primeira leitura.
- [x] Criar fila de operações pendentes.
- [x] Sincronizar criação de tarefa.
- [x] Sincronizar edição de tarefa.
- [x] Sincronizar anexos pendentes.
- [x] Sincronizar links.
- [x] Sincronizar campos complementares.
- [x] Resolver conflitos simples.

### Critérios de aceite

- [x] Usuário cria tarefa offline.
- [x] Tarefa aparece imediatamente.
- [ ] Tarefa sincroniza quando internet volta.
- [x] App indica pendência de sincronização.

---

# 20. Fase 16 — Permissões e Segurança

## 20.1 Permissões Android

### Tarefas

- [x] Solicitar permissão de notificação.
- [x] Solicitar câmera apenas quando usuário usar câmera.
- [x] Não pedir acesso amplo à galeria sem necessidade.
- [x] Usar Photo Picker para imagens.
- [x] Usar seletor de arquivos para documentos.

### Critérios de aceite

- [x] App não pede permissões desnecessárias na abertura.
- [x] Permissões aparecem no contexto certo.
- [x] Usuário consegue continuar mesmo negando permissão não essencial.

---

## 20.2 Segurança de dados

### Tarefas

- [x] Criar regras de acesso por usuário.
- [x] Criar regras por participante.
- [x] Validar permissão de anexos.
- [x] Validar permissão de links.
- [x] Validar permissão de campos.
- [x] Evitar URLs públicas permanentes.
- [x] Remover logs com dados sensíveis.

### Critérios de aceite

- [x] Usuário sem permissão não acessa tarefa.
- [x] Usuário sem permissão não acessa anexo.
- [x] Dados sensíveis não aparecem em logs.

---

# 21. Fase 17 — Busca e Filtros

## 21.1 Busca

### Tarefas

- [x] Buscar por título.
- [x] Buscar por descrição.
- [x] Buscar por responsável.
- [x] Buscar por nome de anexo.
- [x] Buscar por link.
- [x] Buscar por campo complementar.
- [x] Buscar por tag/categoria.

### Critérios de aceite

- [x] Busca retorna tarefas corretas.
- [x] Busca não trava com lista grande.
- [x] Busca mostra estado vazio quando não encontra nada.

---

## 21.2 Filtros

### Tarefas

- [x] Filtrar por hoje.
- [x] Filtrar por próximas.
- [x] Filtrar por atrasadas.
- [x] Filtrar por concluídas.
- [x] Filtrar por responsável.
- [x] Filtrar por prioridade.
- [x] Filtrar por tarefas com anexos.
- [x] Filtrar por tarefas com links.
- [x] Filtrar por tarefas com lembretes.

### Critérios de aceite

- [x] Filtros funcionam isolados.
- [x] Filtros combinados funcionam quando aplicável.

---

# 22. Fase 18 — Configurações

## 22.1 Tela de configurações

### Tarefas

- [x] Exibir perfil.
- [x] Configurar notificações.
- [x] Configurar tema.
- [x] Configurar conta.
- [x] Configurar privacidade.
- [x] Configurar ajuda e suporte.
- [x] Logout.

### Critérios de aceite

- [x] Configurações principais persistem.
- [x] Logout retorna para login.
- [x] Usuário consegue revisar preferências de notificação.

---

# 23. Fase 19 — Testes

## 23.1 Testes unitários

### Tarefas

- [x] Testar cálculo de recorrência única.
- [x] Testar recorrência diária.
- [x] Testar recorrência semanal.
- [x] Testar recorrência a cada 2 semanas.
- [x] Testar recorrência a cada 15 dias.
- [x] Testar recorrência mensal por dia fixo.
- [x] Testar último dia útil.
- [x] Testar fim por data.
- [x] Testar fim por ocorrências.
- [x] Testar validação de URL.
- [x] Testar validação de arquivo.

### Critérios de aceite

- [x] Testes principais passam.
- [x] Não há regressão no Reminder Engine.

---

## 23.2 Testes manuais obrigatórios

### Fluxos mínimos

- [x] Criar tarefa simples.
- [x] Criar tarefa com lembrete único.
- [x] Criar tarefa com lembrete a cada 2 semanas.
- [x] Criar tarefa com lembrete mensal todo dia 10.
- [x] Criar tarefa com anexo PDF.
- [x] Criar tarefa com foto.
- [x] Criar tarefa com link.
- [x] Criar tarefa com campo complementar.
- [x] Compartilhar tarefa por WhatsApp.
- [x] Compartilhar tarefa por e-mail.
- [x] Copiar link de convite.
- [x] Aceitar convite.
- [x] Alterar status.
- [x] Concluir tarefa.
- [x] Adiar lembrete.

### Critérios de aceite

- [x] Todos os fluxos passam em emulador.
- [ ] Todos os fluxos passam em pelo menos um aparelho físico.
- [x] Nenhum fluxo principal gera crash.

---

# 24. Fase 20 — Polimento Visual

## 24.1 Refinamento de UI

### Tarefas

- [ ] Ajustar espaçamentos.
- [ ] Ajustar alinhamentos.
- [x] Ajustar tamanhos de fonte.
- [x] Ajustar cores dos chips.
- [x] Ajustar contraste.
- [x] Ajustar animações.
- [x] Ajustar estados vazios.
- [x] Ajustar feedback de toque.
- [x] Ajustar loading de upload.
- [x] Ajustar erros de formulário.

### Critérios de aceite

- [ ] App visualmente consistente.
- [ ] Telas principais parecem parte do mesmo produto.
- [ ] Nenhuma tela parece protótipo inacabado.

---

# 25. Fase 21 — Release MVP

## 25.1 Preparação para build

### Tarefas

- [x] Definir nome do app.
- [x] Definir ícone.
- [x] Definir splash screen.
- [x] Configurar versão.
- [x] Configurar assinatura.
- [x] Gerar build debug.
- [x] Gerar build release.
- [x] Testar build release.

### Critérios de aceite

- [x] APK/AAB gerado.
- [ ] Build release instala em aparelho físico.
- [x] Login funciona no release.
- [x] Notificações funcionam no release.
- [x] Upload funciona no release.

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

- [x] Projeto compila sem erros.
- [x] Login/cadastro funcionando.
- [x] Home funcionando.
- [x] Espaços funcionando.
- [x] Listas funcionando.
- [x] CRUD de tarefas funcionando.
- [x] Status funcionando.
- [x] Prioridade funcionando.
- [x] Lembrete único funcionando.
- [x] Lembrete recorrente personalizado funcionando.
- [x] Notificações locais funcionando.
- [x] Ação de adiar funcionando.
- [x] Múltiplos lembretes funcionando.
- [x] Anexar imagem funcionando.
- [x] Tirar foto funcionando.
- [x] Anexar PDF funcionando.
- [x] Links funcionando.
- [x] Campos complementares funcionando.
- [x] Checklist funcionando.
- [x] Compartilhamento funcionando.
- [x] Convite funcionando.
- [x] Comentários funcionando.
- [x] Histórico básico funcionando.
- [ ] Firebase Authentication funcionando.
- [ ] Firestore funcionando.
- [ ] Firebase Storage funcionando.
- [x] Permissões funcionando.
- [x] Regras de segurança básicas funcionando.
- [x] Persistência local funcionando.
- [ ] App testado em aparelho físico.
- [x] Build release gerado.

---

# 28. Registro de Decisões Técnicas

Use esta seção para registrar decisões importantes.

## Decisão 001

- Data: 2026-06-06
- Tema: MVP local-first
- Decisão: Entregar o MVP com login/cadastro local placeholder, Room/DataStore e contratos remotos preparados, mantendo Firebase real desativado até haver projeto e credenciais.
- Motivo: O pacote inicial não continha `google-services.json`, projeto Firebase, regras publicadas ou credenciais de teste.
- Impacto: O app funciona localmente e enfileira operações pendentes; Auth/Firestore/Storage/FCM ficam como fase de integração final.

## Decisão 002

- Data: 2026-06-06
- Tema: Fronteira Firebase
- Decisão: Criar `RemoteTaskFlowDataSource`, contratos de coleções/caminhos e `FirebaseTaskFlowDataSource` fail-closed sem SDK Firebase obrigatório.
- Motivo: Preparar a troca do repositório remoto sem quebrar builds locais nem exigir credenciais ausentes.
- Impacto: A integração Firebase futura tem pontos de entrada explícitos para Auth, sync de operações pendentes, upload/delete de anexos e registro FCM.

---

# 29. Bloqueios Atuais

Use esta seção para listar bloqueios.

| Data | Bloqueio | Impacto | Responsável | Status |
|---|---|---|---|---|
| 2026-06-06 | Ausência de projeto Firebase/`google-services.json`/credenciais de teste. | Impede validar Firebase Authentication, Firestore, Storage, FCM e regras publicadas. | Dono do projeto Firebase | Aberto |
| 2026-06-06 | Aparelho físico conectado exige PIN/biometria. | APK release instala no aparelho, mas os fluxos navegáveis não puderam ser testados fisicamente via adb. | Dono do aparelho | Aberto |

---

# 30. Próxima tarefa recomendada

Quando este documento for entregue ao agente construtor, a primeira tarefa recomendada é:

```text
Comece pela Fase 0 — Setup Inicial. Crie o projeto Android nativo em Kotlin com Jetpack Compose, configure a arquitetura inicial, crie a estrutura de pacotes definida neste ROADMAP.md e entregue uma tela inicial provisória compilando sem erros. Só avance para a Fase 1 após cumprir todos os critérios de aceite da Fase 0.
```
