# DESIGN — TaskFlow

## Documento visual dos mockups e diretrizes de interface

**Versão:** 1.0  
**Produto:** TaskFlow  
**Plataforma inicial:** Android mobile  
**Estilo:** moderno, limpo, premium, iOS-like adaptado para Android  

---

# 1. Objetivo deste documento

Este documento descreve a direção visual do app TaskFlow e registra os mockups criados para orientar a implementação da interface.

O construtor do app deverá usar este documento como referência visual para montar telas, componentes, espaçamentos, cores, hierarquia, comportamento dos elementos e organização das informações.

As telas abaixo correspondem aos mockups criados para o projeto, incluindo as últimas funções adicionadas:

- lembretes recorrentes personalizados;
- anexos;
- fotos;
- documentos;
- links;
- campos complementares;
- compartilhamento com contexto da tarefa.

---

# 2. Linguagem visual geral

O TaskFlow deve transmitir uma sensação de app moderno, limpo, confiável e fácil de usar.

## 2.1 Características principais

- Fundo claro/off-white.
- Cards brancos com bordas arredondadas.
- Sombras suaves.
- Gradientes em azul e roxo.
- Botões pill grandes.
- Toggles deslizantes modernos.
- Segmented controls.
- Chips de status e prioridade.
- Ícones minimalistas.
- Espaçamento generoso.
- Tipografia grande e legível.
- Navegação inferior clara.
- Elementos com bastante respiro visual.

## 2.2 Sensação desejada

O app deve parecer:

- moderno;
- simples;
- polido;
- confiável;
- organizado;
- rápido de entender;
- agradável de usar diariamente.

---

# 3. Paleta visual sugerida

## 3.1 Cores principais

| Uso | Cor sugerida |
|---|---|
| Fundo geral | `#F7F8FC` |
| Card | `#FFFFFF` |
| Texto principal | `#07132F` |
| Texto secundário | `#667085` |
| Azul principal | `#2563FF` |
| Roxo principal | `#7C3AED` |
| Gradiente principal | `#2563FF` → `#7C3AED` |
| Verde sucesso | `#22C55E` |
| Vermelho/alta prioridade | `#EF4444` |
| Laranja/média prioridade | `#F59E0B` |
| Cinza de borda | `#E5E7EB` |

## 3.2 Gradiente principal

Usar em botões principais e segmentos ativos:

```css
linear-gradient(90deg, #2563FF 0%, #7C3AED 100%)
```

---

# 4. Tipografia

## 4.1 Estilo

- Fonte sans-serif moderna.
- Peso forte em títulos.
- Texto secundário com cinza suave.
- Títulos grandes e claros.

## 4.2 Hierarquia sugerida

| Elemento | Tamanho sugerido | Peso |
|---|---:|---:|
| Título principal | 32–38sp | Bold |
| Título da tela | 24–30sp | Bold |
| Título de card | 18–22sp | Semibold |
| Texto padrão | 15–17sp | Regular |
| Texto secundário | 13–15sp | Regular |
| Label pequeno | 12–13sp | Medium |

---

# 5. Componentes base

## 5.1 Cards

Cards devem ter:

- fundo branco;
- raio de borda entre 20 e 28dp;
- sombra suave;
- padding interno de 16 a 24dp;
- espaçamento vertical de 12 a 20dp;
- borda sutil quando necessário.

## 5.2 Botões principais

Botões principais devem ser grandes, arredondados e usar gradiente azul/roxo.

Exemplos:

- Salvar.
- Salvar lembrete.
- Enviar convite.
- Concluir.

## 5.3 Botões secundários

Botões secundários devem usar:

- fundo branco;
- borda azul/roxa fina;
- texto azul/roxo;
- raio grande.

## 5.4 Segmented control

Usado em:

- Hoje / Próximas / Concluídas.
- Anexos / Links / Campos.
- Não repetir / Simples / Personalizada.
- Editar / Comentar / Ver.

O item ativo deve usar gradiente azul/roxo.

## 5.5 Toggle switch

Usado em:

- Lembretes ativos.
- Ativar lembrete.
- Próximo lembrete ativo.

O estado ativo deve usar azul/roxo.

## 5.6 Chips

Usados para:

- prioridade;
- status;
- contadores de anexos;
- links;
- campos;
- dias da semana;
- lembrete antecipado.

---

# 6. Mockup 01 — Home / Hoje

**Arquivo de referência:** `mockup_de_tela_de_tarefas_diário.png`

![Mockup Home / Hoje](./mockup_de_tela_de_tarefas_diário.png)

## 6.1 Objetivo da tela

Exibir as tarefas do dia e dar acesso rápido às tarefas próximas, concluídas, lembretes ativos e criação de nova tarefa.

## 6.2 Elementos visuais

- Saudação grande: `Bom dia, Manuel`.
- Data abaixo da saudação.
- Ícone de notificações.
- Ícone de recurso inteligente/atalho visual.
- Segmented control com:
  - Hoje;
  - Próximas;
  - Concluídas.
- Card/toggle de `Lembretes ativos`.
- Lista de tarefas em cards.
- Barra lateral colorida no card para identificar categoria/prioridade.
- Círculo de conclusão à esquerda.
- Horário e categoria abaixo do título.
- Chip de prioridade à direita.
- Ícone de sino quando houver lembrete.
- Card de próximo lembrete destacado.
- Floating action button circular com `+`.
- Bottom navigation com:
  - Hoje;
  - Listas;
  - Pessoas;
  - Ajustes.

## 6.3 Regras de implementação

- A tela deve priorizar clareza e leitura rápida.
- O botão `+` deve ficar em destaque.
- O segmented control deve ser interativo.
- O toggle `Lembretes ativos` deve permitir ligar/desligar a visualização ou estado geral de lembretes ativos.
- Cards devem ser grandes o suficiente para toque confortável.

---

# 7. Mockup 02 — Nova tarefa

**Arquivo de referência:** `tela_de_criação_de_tarefa.png`

![Mockup Nova Tarefa](./tela_de_criação_de_tarefa.png)

## 7.1 Objetivo da tela

Permitir criar uma tarefa completa com dados principais, lembrete, materiais, prioridade e responsáveis.

## 7.2 Elementos visuais

- Top bar com:
  - Cancelar;
  - título `Nova tarefa`.
- Campo grande de título.
- Card com informações principais:
  - Lista;
  - Prazo;
  - Responsável;
  - Convidar pessoas.
- Seção `Lembretes` com toggle ativo.
- Card de recorrência personalizada:
  - `A cada 2 semanas`;
  - `seg e qui`;
  - `termina em 31/12/2026`.
- Seção `Materiais da tarefa`.
- Chips informativos:
  - `2 anexos`;
  - `1 link`;
  - `3 campos`.
- Botões rápidos:
  - Adicionar arquivo;
  - Tirar foto;
  - Adicionar link.
- Segmented control de prioridade:
  - Baixa;
  - Média;
  - Alta.
- Botão principal `Salvar`.

## 7.3 Regras de implementação

- A criação da tarefa deve permitir salvar mesmo sem preencher todos os campos.
- Título deve ser o único campo obrigatório.
- A seção de materiais deve funcionar como porta de entrada para anexos, links e campos.
- O card de lembrete deve abrir a tela de configuração de lembrete personalizado.
- O botão salvar deve usar gradiente azul/roxo.

---

# 8. Mockup 03 — Lembrete personalizado

**Arquivo de referência:** `configurações_de_lembrete_personalizado.png`

![Mockup Lembrete Personalizado](./configurações_de_lembrete_personalizado.png)

## 8.1 Objetivo da tela

Permitir que o usuário configure lembretes únicos e recorrentes com liberdade de programação.

## 8.2 Elementos visuais

- Top bar com botão voltar.
- Título: `Lembrete personalizado`.
- Card `Ativar lembrete` com toggle ativo.
- Campos:
  - Data inicial;
  - Horário.
- Seção `Avisar antes` com chips:
  - 5 min;
  - 15 min;
  - 30 min;
  - 1 h.
- Seção `Repetição` com segmented control:
  - Não repetir;
  - Simples;
  - Personalizada.
- Configuração personalizada:
  - repetir a cada;
  - seletor numérico com menos e mais;
  - unidade: semanas;
  - botões circulares dos dias da semana.
- Seção `Fim da repetição` com radio buttons:
  - Nunca;
  - Em uma data;
  - Após X repetições.
- Opções mensais:
  - Mensal em data fixa;
  - Regra mensal.
- Botão principal `Salvar lembrete`.

## 8.3 Regras de implementação

- O app deve permitir configurar recorrência a cada X dias, semanas, meses ou anos.
- O número do intervalo deve aceitar valores inteiros positivos.
- Os dias da semana devem ser selecionáveis.
- O usuário deve poder definir término da recorrência.
- O botão `Salvar lembrete` só deve ser habilitado quando a configuração for válida.
- O toggle deve ativar/desativar a notificação daquele lembrete.

## 8.4 Exemplo de estado representado no mockup

```text
Ativar lembrete: Sim
Data inicial: 10/06/2026
Horário: 09:00
Avisar antes: 30 min
Repetição: Personalizada
Intervalo: A cada 2 semanas
Dias: terça e quinta
Fim: Em uma data — 31/12/2026
Regra mensal alternativa: Último dia útil
```

---

# 9. Mockup 04 — Materiais da tarefa

**Arquivo de referência:** `interface_moderna_de_materiais_da_tarefa.png`

![Mockup Materiais da Tarefa](./interface_moderna_de_materiais_da_tarefa.png)

## 9.1 Objetivo da tela

Permitir gerenciar documentos, fotos, links e campos complementares vinculados à tarefa.

## 9.2 Elementos visuais

- Top bar com botão voltar.
- Título: `Materiais da tarefa`.
- Segmented control:
  - Anexos;
  - Links;
  - Campos.
- Botões principais de ação:
  - Arquivo;
  - Foto;
  - Link.
- Área de upload com borda tracejada:
  - `Arraste ou toque para selecionar`;
  - formatos aceitos;
  - limite de tamanho.
- Seção `Anexos recentes`.
- Lista de anexos:
  - ícone PDF;
  - miniatura de imagem;
  - nome do arquivo;
  - tamanho;
  - menu de opções.
- Seção `Links`.
- Card de link:
  - título;
  - URL;
  - data de atualização;
  - menu de opções.

## 9.3 Regras de implementação

- A tela deve abrir por padrão na aba `Anexos`.
- A aba `Links` deve listar links associados à tarefa.
- A aba `Campos` deve listar campos complementares.
- O botão `Arquivo` deve abrir seletor de documentos.
- O botão `Foto` deve abrir câmera ou Photo Picker.
- O botão `Link` deve abrir formulário para inclusão de link.
- Cada anexo deve ter menu com:
  - visualizar;
  - renomear;
  - compartilhar;
  - excluir.

---

# 10. Mockup 05 — Detalhe da tarefa

**Arquivo de referência:** `detalhe_da_tarefa_em_app_moderno.png`

![Mockup Detalhe da Tarefa](./detalhe_da_tarefa_em_app_moderno.png)

## 10.1 Objetivo da tela

Exibir a tarefa completa com status, prioridade, lembrete, descrição, prazo, responsáveis, participantes e materiais vinculados.

## 10.2 Elementos visuais

- Top bar com:
  - botão voltar;
  - título `Detalhe da tarefa`;
  - menu de opções.
- Título grande da tarefa:
  - `Enviar proposta ao cliente`.
- Chips:
  - Em andamento;
  - Alta.
- Card de próximo lembrete:
  - título `Próximo lembrete`;
  - recorrência;
  - próximo disparo;
  - toggle ativo.
- Card de descrição.
- Card com dados principais:
  - prazo;
  - responsável;
  - participantes.
- Card `Materiais da tarefa`.
- Chips de contagem:
  - `2 anexos`;
  - `1 link`;
  - `3 campos`.
- Lista resumida de materiais:
  - PDF;
  - link;
  - campo complementar.
- Botões inferiores:
  - Compartilhar;
  - Concluir.

## 10.3 Regras de implementação

- A tela deve mostrar um resumo completo da tarefa sem exigir navegação excessiva.
- O card de materiais deve abrir a tela `Materiais da tarefa`.
- O toggle do lembrete deve ativar/desativar o próximo lembrete.
- O botão `Concluir` deve atualizar o status para concluída.
- O botão `Compartilhar` deve abrir a tela de convite.

---

# 11. Mockup 06 — Compartilhar / Convidar

**Arquivo de referência:** `tela_de_compartilhamento_de_convite.png`

![Mockup Compartilhar](./tela_de_compartilhamento_de_convite.png)

## 11.1 Objetivo da tela

Permitir convidar alguém para participar da tarefa, definindo permissão e canal de envio.

## 11.2 Elementos visuais

- Top bar com botão voltar.
- Título: `Compartilhar`.
- Subtítulo: `Convide alguém para esta tarefa`.
- Card `Permissão`.
- Segmented control:
  - Editar;
  - Comentar;
  - Ver.
- Seção `Compartilhar por`.
- Cards de canais:
  - WhatsApp;
  - E-mail;
  - Copiar link.
- Card `Prévia da mensagem`.
- Mini card da tarefa contendo:
  - título;
  - prazo;
  - categoria;
  - chips de anexos e links.
- Botão principal `Enviar convite`.

## 11.3 Regras de implementação

- O usuário deve selecionar a permissão antes de enviar convite.
- O canal de compartilhamento deve usar Android Sharesheet quando aplicável.
- O conteúdo compartilhado deve incluir contexto da tarefa.
- O convite deve respeitar permissões de anexos, links e campos.
- O mini card deve mostrar se a tarefa possui anexos e links.

---

# 12. Mockup geral — Poster das telas

**Arquivo de referência:** `a_clean_light_ui_mockup_poster_app_design_prese.png`

![Poster geral dos mockups](./a_clean_light_ui_mockup_poster_app_design_prese.png)

## 12.1 Objetivo

Este arquivo serve como referência panorâmica do fluxo visual do app. Ele apresenta uma visão geral de várias telas e reforça a linguagem de interface desejada.

## 12.2 Telas representadas

- Boas-vindas.
- Dashboard.
- Criar/editar lembrete.
- Detalhes do lembrete.
- Incluir documento/imagem.
- Links.
- Campos personalizados.
- Categorias.
- Calendário.
- Notificações.
- Configurações.
- Alarme ativo.

## 12.3 Observação importante

Os mockups individuais finais devem prevalecer sobre o poster geral quando houver diferença de layout, pois eles representam a versão mais recente da direção visual.

---

# 13. Telas adicionais previstas

Além dos mockups criados, o app também deve prever as seguintes telas, mantendo o mesmo design system:

## 13.1 Onboarding

Deve apresentar:

- logo ou ícone do app;
- frase de valor;
- benefícios principais;
- botão `Começar`;
- botão `Já tenho conta`.

## 13.2 Login / Cadastro

Deve conter:

- login com e-mail;
- login com Google;
- criar conta;
- recuperar senha.

## 13.3 Espaços / Listas

Deve conter:

- lista de espaços;
- filtros;
- cards de espaços;
- botão novo espaço.

## 13.4 Configurações

Deve conter:

- perfil;
- notificações;
- tema;
- conta;
- privacidade;
- ajuda;
- sair.

---

# 14. Estados vazios

## 14.1 Sem tarefas hoje

```text
Tudo limpo por aqui.
Nenhuma tarefa para hoje.
```

Botão:

```text
Criar tarefa
```

## 14.2 Sem anexos

```text
Nenhum material adicionado.
Inclua arquivos, fotos, links ou campos para deixar a tarefa completa.
```

Botões:

```text
Adicionar arquivo
Tirar foto
Adicionar link
```

## 14.3 Sem lembretes

```text
Nenhum lembrete configurado.
Crie um lembrete para não perder essa tarefa.
```

Botão:

```text
Configurar lembrete
```

---

# 15. Microinterações

O app deverá usar microinterações discretas:

- animação suave ao ativar toggles;
- feedback visual ao selecionar chips;
- transição suave entre abas;
- sombra mais intensa ao pressionar card;
- animação do botão `+`;
- loading de upload de anexos;
- confirmação visual ao salvar lembrete;
- snackbar para ações concluídas.

---

# 16. Acessibilidade

A interface deve respeitar:

- contraste adequado;
- áreas de toque mínimas de 48dp;
- suporte a fonte ampliada;
- labels claros;
- ícones acompanhados de texto quando a função não for óbvia;
- leitura por leitores de tela.

---

# 17. Regras finais de implementação visual

- Não usar telas poluídas.
- Não exibir excesso de informações no primeiro nível.
- Usar cards expansíveis ou navegação para detalhes.
- Priorizar ações principais no rodapé ou em botões destacados.
- Manter consistência entre todas as telas.
- Usar o gradiente azul/roxo apenas para estados ativos e ações principais.
- Não usar muitas cores simultaneamente.
- Priorizar clareza sobre enfeite visual.

---

# 18. Checklist visual para aceite

A interface será considerada aderente ao design quando:

- [x] Usa fundo claro/off-white.
- [x] Usa cards brancos arredondados.
- [x] Usa gradiente azul/roxo nos botões principais.
- [x] Usa segmented controls nos filtros e seleções principais.
- [x] Usa toggles modernos em lembretes.
- [x] Usa chips para status, prioridade e contadores.
- [x] Usa bottom navigation com quatro itens.
- [x] Usa floating action button na Home.
- [x] A tela Home se parece com `mockup_de_tela_de_tarefas_diário.png`.
- [x] A tela Nova tarefa se parece com `tela_de_criação_de_tarefa.png`.
- [x] A tela Lembrete personalizado se parece com `configurações_de_lembrete_personalizado.png`.
- [x] A tela Materiais da tarefa se parece com `interface_moderna_de_materiais_da_tarefa.png`.
- [x] A tela Detalhe da tarefa se parece com `detalhe_da_tarefa_em_app_moderno.png`.
- [x] A tela Compartilhar se parece com `tela_de_compartilhamento_de_convite.png`.
- [x] A interface mantém espaçamentos generosos.
- [x] Os textos são legíveis.
- [x] As ações principais são óbvias.
- [x] O app parece moderno, limpo e premium.
