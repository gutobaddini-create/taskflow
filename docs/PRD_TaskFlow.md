# PRD — TaskFlow

## App Android de Tarefas, Lembretes Inteligentes, Anexos, Links e Colaboração

**Versão:** 1.0  
**Plataforma inicial:** Android  
**Tecnologia sugerida:** Kotlin + Jetpack Compose  
**Arquitetura sugerida:** MVVM + Repository Pattern  
**Backend sugerido:** Firebase Authentication, Firestore, Firebase Storage e Firebase Cloud Messaging  
**Banco local sugerido:** Room Database + DataStore  

---

# 1. Visão geral

O **TaskFlow** é um aplicativo Android para organização de tarefas, lembretes personalizados, anexos, links, campos complementares e colaboração entre usuários.

A proposta é criar uma solução mais simples que o Trello, mais poderosa que uma lista comum de tarefas e mais prática que usar WhatsApp, agenda e galeria de arquivos separadamente.

O app deve permitir que o usuário crie tarefas, programe lembretes com alto grau de personalização, anexe documentos, tire fotos, inclua links, registre campos extras, compartilhe tarefas e convide outras pessoas para colaborar.

---

# 2. Objetivo do produto

Criar um app mobile moderno, intuitivo e visualmente polido para ajudar pessoas a organizar tarefas e receber lembretes no momento certo, com todos os documentos, links e informações necessárias vinculados à tarefa.

---

# 3. Proposta de valor

> **Organize tarefas, programe lembretes inteligentes e mantenha tudo que precisa junto da tarefa: documentos, fotos, links, campos personalizados e pessoas responsáveis.**

---

# 4. Problema que o app resolve

Muitas pessoas criam lembretes em um app, guardam documentos em outro lugar, enviam informações pelo WhatsApp e acabam perdendo contexto quando chega a hora de executar a tarefa.

O TaskFlow resolve esse problema centralizando:

- tarefa;
- prazo;
- lembrete;
- recorrência;
- responsável;
- participantes;
- documentos;
- fotos;
- arquivos;
- links;
- campos complementares;
- comentários;
- histórico.

---

# 5. Público-alvo

## 5.1 Público principal

- Profissionais autônomos.
- Pequenos escritórios.
- Advogados.
- Consultores.
- Empresários.
- Famílias.
- Estudantes.
- Equipes pequenas.
- Pessoas que precisam controlar prazos, documentos, pendências e lembretes recorrentes.

## 5.2 Casos de uso

- Lembrar de pagar uma conta e anexar o boleto.
- Lembrar de protocolar uma defesa e anexar documentos.
- Criar tarefa para enviar proposta com PDF vinculado.
- Compartilhar uma tarefa com outra pessoa via WhatsApp.
- Programar lembrete quinzenal.
- Programar lembrete mensal em data específica.
- Tirar foto de um documento e vincular à tarefa.
- Guardar link de reunião, pasta do Drive ou página de referência.
- Criar campos como valor, número de processo, cliente, local e contato.

---

# 6. Escopo do MVP

O MVP deverá entregar as funcionalidades essenciais para validar o produto.

## 6.1 Funcionalidades obrigatórias

- Cadastro e login.
- Tela inicial com tarefas do dia.
- Criação de espaços.
- Criação de listas.
- Criação de tarefas.
- Edição de tarefas.
- Status da tarefa.
- Prioridade da tarefa.
- Prazo.
- Responsável.
- Participantes.
- Comentários simples.
- Lembrete único.
- Lembrete recorrente personalizado.
- Lembrete com repetição a cada X dias, semanas, meses ou anos.
- Lembrete mensal em data específica.
- Regra mensal, como último dia útil.
- Múltiplos lembretes por tarefa.
- Adiar lembrete.
- Notificação local.
- Anexar foto tirada na hora.
- Anexar imagem da galeria.
- Anexar PDF e documentos.
- Adicionar links.
- Adicionar campos complementares.
- Checklist simples.
- Compartilhamento por WhatsApp, e-mail e link.
- Convite de participantes.
- Tela de configuração de lembrete.
- Tela de materiais da tarefa.
- Tela de detalhe da tarefa.
- Configurações básicas.

---

# 7. Fora do MVP

As seguintes funcionalidades ficam para versões futuras:

- OCR de documentos.
- Scanner avançado.
- Recorte automático de documentos fotografados.
- Geração automática de PDF.
- Leitura automática de boletos.
- Extração automática de datas.
- Integração com Google Agenda.
- Integração com Outlook.
- Integração com Google Drive.
- Versão web.
- Inteligência artificial para sugerir tarefas e lembretes.
- Assinatura eletrônica.
- Relatórios.
- Planos pagos.
- Equipes avançadas.

---

# 8. Estrutura do produto

O app será organizado em quatro níveis:

## 8.1 Espaços

São áreas maiores de organização.

Exemplos:

- Pessoal.
- Família.
- Trabalho.
- Escritório.
- Clientes.
- Projetos.
- Estudos.

## 8.2 Listas

Cada espaço possui listas.

Exemplos:

- Hoje.
- Esta semana.
- Pendências.
- Prazos.
- Financeiro.
- Documentos.
- Clientes.

## 8.3 Tarefas

Cada lista possui tarefas.

Exemplos:

- Enviar proposta ao cliente.
- Pagar boleto.
- Revisar contrato.
- Protocolar defesa.
- Comprar material.

## 8.4 Materiais da tarefa

Cada tarefa pode conter:

- anexos;
- imagens;
- fotos;
- documentos;
- PDFs;
- links;
- campos complementares;
- checklist;
- comentários.

---

# 9. Modelo da tarefa

Cada tarefa deverá conter:

- ID.
- Título.
- Descrição.
- Espaço.
- Lista.
- Status.
- Prioridade.
- Prazo.
- Criador.
- Responsável.
- Participantes.
- Lembretes.
- Anexos.
- Links.
- Campos complementares.
- Checklist.
- Comentários.
- Histórico.
- Data de criação.
- Data de atualização.
- Link compartilhável.
- Permissões.

---

# 10. Status da tarefa

O app deverá oferecer quatro status principais:

- **A fazer:** tarefa criada, ainda não iniciada.
- **Em andamento:** tarefa em execução.
- **Aguardando:** tarefa depende de resposta, documento, aprovação ou terceiro.
- **Concluída:** tarefa finalizada.

---

# 11. Prioridade

A tarefa poderá ter três níveis de prioridade:

- Baixa.
- Média.
- Alta.

A prioridade deverá ser exibida por chips ou pills coloridos, mantendo o visual limpo.

---

# 12. Sistema avançado de lembretes

## 12.1 Importância

O sistema de lembretes é um dos diferenciais centrais do app. Ele não deve ser limitado a lembretes simples. O usuário precisa ter liberdade para programar alarmes e recorrências conforme sua rotina.

## 12.2 Tipos de lembrete

O app deverá permitir:

- Lembrete único.
- Lembrete antecipado.
- Lembrete diário.
- Lembrete semanal.
- Lembrete mensal.
- Lembrete anual.
- Lembrete personalizado.
- Múltiplos lembretes por tarefa.

## 12.3 Lembrete único

O usuário escolhe data e horário específicos.

Exemplo:

- 10/06/2026 às 09:00.

## 12.4 Lembrete antecipado

O usuário pode ser lembrado antes do prazo.

Opções iniciais:

- Na hora.
- 5 minutos antes.
- 15 minutos antes.
- 30 minutos antes.
- 1 hora antes.
- 2 horas antes.
- 1 dia antes.
- 1 semana antes.
- Personalizado.

## 12.5 Recorrência personalizada

O usuário poderá configurar:

> Repetir a cada [número] [dias/semanas/meses/anos]

Exemplos:

- A cada 2 semanas.
- A cada 15 dias.
- A cada 3 meses.
- A cada 6 meses.
- A cada 1 ano.

## 12.6 Dias específicos da semana

O usuário poderá escolher os dias da semana.

Exemplos:

- Toda segunda-feira.
- Toda segunda e quinta.
- Toda terça, quinta e sábado.
- Todos os dias úteis.
- Todos os finais de semana.

## 12.7 Data específica do mês

O usuário poderá escolher data fixa mensal.

Exemplos:

- Todo dia 5.
- Todo dia 10.
- Todo dia 15.
- Todo dia 30.
- Todo dia 31.

Caso o mês não tenha o dia escolhido, o app deve oferecer:

- lembrar no último dia do mês;
- pular meses sem essa data.

## 12.8 Regra mensal

O app deverá prever regras especiais:

- Primeiro dia útil do mês.
- Último dia útil do mês.
- Último dia do mês.
- Primeira segunda-feira do mês.
- Última sexta-feira do mês.

## 12.9 Fim da repetição

O usuário poderá escolher:

- Nunca.
- Em uma data.
- Após X repetições.
- Quando a tarefa for concluída.

## 12.10 Múltiplos lembretes por tarefa

A mesma tarefa poderá ter vários lembretes.

Exemplo:

- 7 dias antes.
- 1 dia antes.
- 2 horas antes.

## 12.11 Adiar lembrete

Ao receber a notificação, o usuário poderá adiar:

- 10 minutos.
- 30 minutos.
- 1 hora.
- Amanhã.
- Escolher nova data e horário.

## 12.12 Ações rápidas na notificação

A notificação deverá permitir:

- Abrir tarefa.
- Concluir.
- Adiar.
- Reprogramar.
- Ignorar.

---

# 13. Anexos da tarefa

## 13.1 Visão geral

Cada tarefa poderá ter anexos para guardar documentos e materiais necessários à sua execução.

## 13.2 Tipos de anexos

O app deverá permitir:

- Foto tirada na hora.
- Imagem da galeria.
- PDF.
- Documento de texto.
- Planilha.
- Arquivo diverso.
- Comprovante.
- Boleto.
- Contrato.
- Recibo.
- Print.
- Foto de documento físico.

## 13.3 Formas de inclusão

O usuário poderá incluir anexos por:

- câmera;
- galeria;
- seletor de arquivos;
- compartilhamento de outro app para o TaskFlow;
- link externo.

## 13.4 Foto de documento

O app deverá permitir fotografar documentos físicos, como:

- recibos;
- contratos;
- comprovantes;
- boletos;
- declarações;
- atestados;
- documentos trabalhistas;
- documentos pessoais.

No MVP, a foto será simples. Em fase futura, poderá haver modo scanner.

## 13.5 Ações com anexos

O usuário autorizado poderá:

- visualizar;
- baixar;
- compartilhar;
- renomear;
- excluir;
- substituir;
- abrir com app externo;
- adicionar observação.

## 13.6 Limites sugeridos para o MVP

- Até 10 anexos por tarefa.
- Até 20 MB por arquivo.
- Formatos: JPG, PNG, PDF, DOC, DOCX, XLS, XLSX, TXT.
- Upload com barra de progresso.
- Mensagem de erro para arquivos grandes demais.

---

# 14. Links da tarefa

Cada tarefa poderá ter links vinculados.

## 14.1 Campos do link

- ID.
- Task ID.
- Título.
- URL.
- Descrição.
- Categoria.
- Criador.
- Data de criação.
- Importante ou não.

## 14.2 Exemplos de links

- Pasta do cliente no Drive.
- Link da reunião.
- Documento para assinatura.
- Processo no PJe.
- Página do fornecedor.
- Produto para comprar.
- Formulário de cadastro.

## 14.3 Ações com links

- Abrir.
- Copiar.
- Editar.
- Excluir.
- Compartilhar.
- Marcar como importante.

---

# 15. Campos complementares

## 15.1 Visão geral

Além dos campos padrão, cada tarefa poderá ter campos complementares.

## 15.2 Campos sugeridos para MVP

- Observações.
- Checklist.
- Local.
- Contato relacionado.
- Valor.
- Categoria.
- Etiquetas.
- Data limite.
- Tempo estimado.
- Número de processo.
- Número de contrato.
- Número de pedido.
- Protocolo.
- CPF/CNPJ.
- Cliente.

## 15.3 Campos personalizados futuros

Em fase futura, o usuário poderá criar campos personalizados por espaço ou lista.

Exemplo para área jurídica:

- Número do processo.
- Vara.
- Cliente.
- Parte contrária.
- Prazo fatal.
- Link do PJe.

---

# 16. Colaboração

## 16.1 Convites

O usuário poderá convidar pessoas por:

- WhatsApp.
- E-mail.
- Link.

## 16.2 Papéis

### Criador

Pode editar tudo, excluir tarefa e gerenciar participantes.

### Responsável

Pode alterar status, comentar, anexar arquivos e concluir tarefa.

### Participante

Pode visualizar, comentar e anexar arquivos se autorizado.

### Visualizador

Pode apenas visualizar.

## 16.3 Permissões sobre materiais

O criador poderá definir se participantes podem:

- visualizar anexos;
- adicionar anexos;
- excluir anexos próprios;
- excluir qualquer anexo;
- abrir links;
- editar links;
- adicionar links;
- editar campos.

---

# 17. Compartilhamento

O app deverá permitir compartilhar tarefas por:

- WhatsApp.
- E-mail.
- Link.
- Android Sharesheet.

## 17.1 Mensagem compartilhada

Exemplo:

```text
Você foi convidado para participar desta tarefa:

Tarefa: Enviar proposta ao cliente
Prazo: Hoje às 14:00
Responsável: Manuel
Status: Em andamento
Lembrete: a cada 2 semanas
Anexos: 2 documentos
Links: 1 link

Abrir tarefa: [link]
```

---

# 18. Notificações

## 18.1 Tipos de notificação

- Lembrete de tarefa.
- Lembrete recorrente.
- Convite recebido.
- Comentário recebido.
- Alteração de status.
- Novo anexo.
- Novo link.
- Tarefa concluída.
- Tarefa atribuída.

## 18.2 Notificação com contexto

A notificação poderá mostrar:

- título;
- horário;
- espaço/lista;
- quantidade de anexos;
- existência de links;
- ações rápidas.

Exemplo:

```text
Lembrete: Enviar proposta ao cliente
Hoje às 14:00 • Trabalho
2 anexos • 1 link
```

Ações:

- Abrir.
- Concluir.
- Adiar.

---

# 19. Design e experiência

O app deverá ter visual moderno, claro, limpo, premium e intuitivo.

## 19.1 Características visuais

- Fundo off-white.
- Cards arredondados.
- Sombras suaves.
- Gradientes azul/roxo.
- Botões em formato pill.
- Segmented controls.
- Toggles deslizantes.
- Chips coloridos.
- Ícones minimalistas.
- Navegação inferior.
- Tipografia grande.
- Espaçamento generoso.

## 19.2 Componentes principais

- TaskCard.
- ReminderCard.
- AttachmentCard.
- LinkCard.
- CustomFieldCard.
- StatusPill.
- PriorityChip.
- ToggleSwitch.
- SegmentedControl.
- FloatingAddButton.
- BottomNavigation.
- ReminderBuilder.
- AttachmentPicker.
- ShareSheet.
- ChecklistItem.

---

# 20. Telas principais do MVP

- Home / Hoje.
- Espaços / Listas.
- Nova tarefa.
- Configurar lembrete.
- Materiais da tarefa.
- Detalhe da tarefa.
- Compartilhar / Convidar.
- Configurações.

---

# 21. Arquitetura técnica

## 21.1 Frontend Android

- Kotlin.
- Jetpack Compose.
- Material Design 3 customizado.
- MVVM.
- Navigation Compose.
- Coroutines.
- Flow/StateFlow.

## 21.2 Backend

- Firebase Authentication.
- Cloud Firestore.
- Firebase Storage.
- Firebase Cloud Messaging.
- Cloud Functions.

## 21.3 Local/offline

- Room Database.
- DataStore.
- Sincronização posterior.
- Cache de metadados dos anexos.

## 21.4 Arquivos

- Photo Picker para imagens.
- Câmera para foto.
- Storage Access Framework para documentos.
- Firebase Storage para armazenamento.

---

# 22. Modelo de dados

## 22.1 User

```kotlin
data class User(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val createdAt: Long,
    val notificationPermissionStatus: String
)
```

## 22.2 Space

```kotlin
data class Space(
    val id: String,
    val name: String,
    val ownerId: String,
    val members: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)
```

## 22.3 TaskList

```kotlin
data class TaskList(
    val id: String,
    val spaceId: String,
    val name: String,
    val order: Int,
    val createdAt: Long,
    val updatedAt: Long
)
```

## 22.4 Task

```kotlin
data class Task(
    val id: String,
    val spaceId: String,
    val listId: String,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val priority: Priority,
    val createdBy: String,
    val assignedTo: String?,
    val participants: List<String>,
    val dueDate: Long?,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

## 22.5 Reminder

```kotlin
data class Reminder(
    val id: String,
    val taskId: String,
    val userId: String,
    val type: ReminderType,
    val startDate: Long,
    val startTime: String,
    val recurrenceType: RecurrenceType,
    val recurrenceInterval: Int?,
    val recurrenceUnit: RecurrenceUnit?,
    val selectedWeekDays: List<Int>,
    val selectedMonthDay: Int?,
    val monthlyRule: String?,
    val endType: ReminderEndType,
    val endDate: Long?,
    val maxOccurrences: Int?,
    val occurrencesCompleted: Int,
    val nextTriggerAt: Long?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

## 22.6 Attachment

```kotlin
data class Attachment(
    val id: String,
    val taskId: String,
    val uploadedBy: String,
    val fileName: String,
    val originalFileName: String,
    val fileType: String,
    val mimeType: String,
    val fileSize: Long,
    val storagePath: String,
    val secureUrl: String?,
    val thumbnailUrl: String?,
    val description: String?,
    val source: AttachmentSource,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)
```

## 22.7 TaskLink

```kotlin
data class TaskLink(
    val id: String,
    val taskId: String,
    val createdBy: String,
    val title: String,
    val url: String,
    val description: String?,
    val category: String?,
    val isImportant: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

## 22.8 CustomField

```kotlin
data class CustomField(
    val id: String,
    val taskId: String,
    val fieldName: String,
    val fieldType: CustomFieldType,
    val fieldValue: String,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

---

# 23. Regras de negócio

- Uma tarefa pertence a uma lista.
- Uma lista pertence a um espaço.
- Uma tarefa pode ter nenhum, um ou vários lembretes.
- Uma tarefa pode ter nenhum, um ou vários anexos.
- Uma tarefa pode ter nenhum, um ou vários links.
- Uma tarefa pode ter campos complementares.
- Uma tarefa pode ter apenas um responsável principal.
- Uma tarefa pode ter vários participantes.
- Apenas usuários autorizados acessam anexos.
- Apenas usuários autorizados alteram tarefa compartilhada.
- Ao concluir tarefa recorrente, o app deve perguntar se encerra os lembretes futuros.
- O app deve evitar duplicidade de notificações.
- O app deve registrar histórico de alterações importantes.
- Arquivos sensíveis não devem ser expostos por link público permanente.

---

# 24. Critérios de aceite do MVP

O MVP será considerado pronto quando:

Nota: este aceite reflete o MVP local-first validado em emulador. Login/cadastro usam a conta local placeholder prevista para esta entrega, enquanto Firebase real e validação em aparelho físico permanecem pendências externas registradas no roadmap e no relatório de QA.

- [x] Usuário consegue criar conta.
- [x] Usuário consegue fazer login.
- [x] Usuário consegue criar espaço.
- [x] Usuário consegue criar lista.
- [x] Usuário consegue criar tarefa.
- [x] Usuário consegue editar tarefa.
- [x] Usuário consegue definir status.
- [x] Usuário consegue definir prioridade.
- [x] Usuário consegue definir prazo.
- [x] Usuário consegue criar lembrete único.
- [x] Usuário consegue criar lembrete recorrente personalizado.
- [x] Usuário consegue criar lembrete a cada 2 semanas.
- [x] Usuário consegue criar lembrete mensal em data específica.
- [x] Usuário recebe notificação no horário correto.
- [x] Usuário consegue adiar lembrete.
- [x] Usuário consegue anexar foto tirada na hora.
- [x] Usuário consegue anexar imagem da galeria.
- [x] Usuário consegue anexar PDF.
- [x] Usuário consegue adicionar link.
- [x] Usuário consegue adicionar campo complementar.
- [x] Usuário consegue compartilhar tarefa.
- [x] Usuário convidado consegue acessar tarefa.
- [x] Usuário autorizado consegue alterar status.
- [x] Usuário autorizado consegue visualizar anexo.
- [x] App não duplica notificações.
- [x] App mantém visual moderno e intuitivo.

---

# 25. Prompt base para Codex

Construa o aplicativo Android nativo TaskFlow usando Kotlin, Jetpack Compose e arquitetura MVVM.

O app deve seguir integralmente este PRD e o documento `DESIGN_TaskFlow.md`.

Priorize:

1. Navegação.
2. Telas principais.
3. Criação de tarefas.
4. Lembretes personalizados.
5. Anexos.
6. Links.
7. Campos complementares.
8. Compartilhamento.
9. Persistência local.
10. Integração Firebase.

Cada etapa só deve ser marcada como concluída quando os critérios de aceite correspondentes estiverem implementados e testados.
