# PROJECT START PROMPT — TASKFLOW

Você será responsável por construir integralmente o aplicativo **TaskFlow**, um app Android de tarefas, lembretes inteligentes, anexos, links, campos complementares e colaboração.

Antes de iniciar qualquer implementação, leia integralmente e utilize como fonte principal de verdade os seguintes documentos do projeto:

1. `docs/PRD_TaskFlow.md`
2. `docs/ROADMAP_TaskFlow.md`
3. `docs/DESIGN_TaskFlow.md`
4. Pasta `mockups/` com as imagens de referência visual

---

## 1. Regra principal do projeto

O desenvolvimento deve seguir obrigatoriamente o arquivo:

`docs/ROADMAP_TaskFlow.md`

O roadmap é o documento gestor do projeto. Ele define a ordem de execução, as etapas, os critérios de aceite e as condições para considerar cada tarefa como concluída.

Você deve avançar fase por fase, tarefa por tarefa, sem pular etapas.

---

## 2. Como usar os documentos

### 2.1 PRD

Use o `docs/PRD_TaskFlow.md` para entender:

- o objetivo do aplicativo;
- o escopo do produto;
- as funcionalidades obrigatórias;
- as regras de negócio;
- os modelos de dados;
- os requisitos funcionais;
- os requisitos não funcionais;
- os critérios de aceite do produto.

Não implemente funcionalidades que contradigam o PRD.

### 2.2 Roadmap

Use o `docs/ROADMAP_TaskFlow.md` como plano de execução.

Cada item do roadmap deve ser tratado como uma tarefa de desenvolvimento.

Você só poderá marcar uma tarefa como concluída quando ela estiver:

1. implementada;
2. integrada ao restante do aplicativo;
3. revisada;
4. testada;
5. funcionando conforme os critérios de aceite;
6. sem erros aparentes;
7. coerente com o PRD e com o design.

Nunca marque uma etapa como concluída apenas porque o código foi escrito. A tarefa só estará concluída quando estiver efetivamente funcional, validada e testada.

### 2.3 Design

Use o `docs/DESIGN_TaskFlow.md` e os mockups da pasta `mockups/` como referência visual obrigatória.

A interface deve seguir o padrão visual definido nos mockups:

- visual moderno;
- aparência premium;
- inspiração iOS-like;
- fundo claro;
- cards arredondados;
- gradientes azul/roxo;
- botões em formato pill;
- toggles deslizantes;
- segmented controls;
- chips informativos;
- ícones minimalistas;
- navegação inferior;
- espaçamentos generosos;
- tipografia clara;
- experiência mobile fluida.

As telas não precisam ser uma cópia pixel-perfect dos mockups, mas devem manter a mesma lógica, estilo visual, hierarquia, fluxo e experiência de uso.

---

## 3. Objetivo final

Você deve construir o aplicativo até o final.

Não pare após criar apenas estrutura, protótipo, telas isoladas ou código parcial.

O projeto só poderá ser considerado finalizado quando o aplicativo estiver:

1. completo conforme o PRD;
2. com todas as etapas do roadmap concluídas;
3. com as telas principais implementadas;
4. com navegação funcional;
5. com criação e edição de tarefas funcionando;
6. com lembretes únicos e recorrentes funcionando;
7. com anexos, fotos, documentos, links e campos complementares funcionando;
8. com compartilhamento e convite funcionando;
9. com persistência de dados funcionando;
10. com interface coerente com os mockups;
11. revisado;
12. testado;
13. sem erros críticos;
14. pronto para ser apresentado ao usuário.

---

## 4. Conduta durante o desenvolvimento

Ao iniciar, faça primeiro uma leitura dos documentos e monte internamente o plano de execução com base no roadmap.

Em seguida, comece pela primeira etapa pendente do `docs/ROADMAP_TaskFlow.md`.

Após concluir cada etapa:

1. revise o código;
2. execute os testes possíveis;
3. valide os critérios de aceite;
4. corrija problemas encontrados;
5. somente então marque o item como concluído no roadmap.

Se encontrar conflito entre documentos, siga esta ordem de prioridade:

1. `docs/ROADMAP_TaskFlow.md`, para ordem e controle de execução;
2. `docs/PRD_TaskFlow.md`, para regras do produto e funcionalidades;
3. `docs/DESIGN_TaskFlow.md`, para aparência e experiência visual;
4. pasta `mockups/`, para referência visual complementar.

Se algum ponto estiver ausente ou ambíguo, tome a decisão mais coerente com o PRD, com o roadmap e com os mockups, sem interromper o desenvolvimento desnecessariamente.

---

## 5. Regras de qualidade

O código deve ser organizado, legível e sustentável.

Use arquitetura clara, separando:

- camada de UI;
- componentes reutilizáveis;
- modelos de dados;
- ViewModels;
- repositórios;
- serviços;
- lógica de lembretes;
- lógica de anexos;
- persistência;
- navegação.

Evite código duplicado.

Evite soluções improvisadas.

Evite criar telas bonitas sem funcionalidade real.

Evite implementar funcionalidade sem ligação com o fluxo principal do app.

Cada tela criada deve estar conectada à navegação do aplicativo e cumprir sua função dentro do produto.

---

## 6. Regras específicas do aplicativo

O app deve ser desenvolvido como aplicativo Android mobile.

Use os documentos do projeto para decidir a stack técnica, arquitetura e prioridades.

A implementação deve contemplar, conforme definido no PRD e no roadmap:

- tarefas;
- listas;
- espaços;
- status;
- prioridades;
- responsáveis;
- participantes;
- lembretes únicos;
- lembretes recorrentes personalizados;
- múltiplos lembretes por tarefa;
- notificações;
- anexos;
- fotos;
- documentos;
- links;
- campos complementares;
- comentários;
- compartilhamento;
- convite de participantes;
- tela de detalhes;
- tela de materiais da tarefa;
- tela de configuração de lembretes;
- tela de criação de tarefa;
- home;
- configurações.

---

## 7. Controle de conclusão

Você deve manter o `docs/ROADMAP_TaskFlow.md` atualizado.

Para cada item concluído, altere:

```md
- [ ]
```

para:

```md
- [x]
```

Somente faça isso depois da tarefa estar realmente entregue, revisada e testada.

Se a tarefa estiver parcialmente implementada, mantenha como:

```md
- [ ]
```

e registre o que falta.

---

## 8. Proibição de encerramento prematuro

Não encerre o projeto antes da conclusão integral.

Não entregue apenas uma estrutura inicial.

Não entregue apenas telas estáticas.

Não entregue apenas mockups.

Não entregue apenas modelos de dados.

Não entregue apenas parte do fluxo.

Continue desenvolvendo até que o app esteja completo, funcional, revisado e testado, conforme PRD, roadmap, design e mockups.

---

## 9. Primeira ação obrigatória

Sua primeira ação deve ser:

1. ler `docs/PRD_TaskFlow.md`;
2. ler `docs/ROADMAP_TaskFlow.md`;
3. ler `docs/DESIGN_TaskFlow.md`;
4. analisar todos os arquivos da pasta `mockups/`;
5. identificar a primeira tarefa pendente do roadmap;
6. iniciar a implementação por essa tarefa.

A partir daí, siga o roadmap até o final.
