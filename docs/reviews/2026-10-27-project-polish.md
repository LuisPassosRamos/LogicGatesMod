# Polimento técnico e documental — rc.16

Base: `codex/classroom-ui`, revisão `c08e84159fb3`, rc.15, Hytale 0.7.4 e SQLite v6. Entrega de manutenção, sem novo esquema, comandos, critérios, layout ou regras de circuitos. A identidade e os resultados dessa rodada ficam no [registro de validação](../validation.md); os roteiros e o estado vigente estão no [aceite consolidado](../classroom-acceptance.md).

## Revisão e mudanças

| Área | Evidência e decisão |
| --- | --- |
| SQLite | Turmas e atividades repetiam abertura/configuração de conexão, transação/rollback, binding de UUID e leitura/paginação. `SqliteAccess` concentra essa infraestrutura; cada repositório mantém SQL, autorização e tradução das falhas. Leitura agregada permanece diferida, gravação imediata e consultas simples mantêm o modo anterior. |
| Recursos em falha | Conexão fecha se a configuração falhar; causa original é preservada com falhas de fechamento/rollback suprimidas. Transações também revertem erros inesperados antes de devolver a falha ao chamador. |
| Publicações | Consulta idêntica por atividade/número reutilizada em `LearningStore`, sem outra conexão/SELECT. Versão entregue continua sendo a fonte dos critérios e valores. |
| Orientação | `GateOrientation` compartilha a mesma leitura de frente local +X e exige bloco em pé. Mundo mantém fallback seguro com entradas indisponíveis; análise recusa rotação não suportada. |
| Interface | `LearningPage` separa renderização da lista, ações, editor, aluno, entrega e avaliação em helpers coesos. Bindings, renderização de dados já carregados, refresh e limpeza de campos após revogação têm um caminho compartilhado. IDs, codecs, revisão de edição e preservação de campos permanecem. |
| Verificação assistida | Leitura/validação da tabela e emissão do relatório organizadas em métodos pequenos; padrões de validação reutilizados e limites com nomes. Não há nota automática ou trabalho periódico novo. |
| Documentação | README/índice apresentam a versão e os recursos atuais; guias de uso, arquitetura, desenvolvimento e roadmap têm responsabilidades claras. Registros históricos e seus anchors continuam intactos, separados do resumo atual. Exemplo AND explicado em linguagem simples no guia pedagógico. |

## Remoções justificadas

Foram removidos somente `ClassroomRuntime.browse`, `players` e `members`, wrappers sem consumidores internos. A interface utiliza `screen`; métodos de serviço/repositório usados por testes, fixtures e leituras continuam. Grafo e busca de fontes foram combinados: alguns callers atribuídos pelo grafo a esses nomes eram chamadas de `ClassroomService`, conferidas no código dos testes. Ausência de aresta sozinha não foi usada como autorização para exclusão.

Nas ferramentas, foram removidos dois imports sem uso: `read` em `generate_gate_assets.py` e `Path` em `model_icons.py`. O helper `generate_terminal_assets.read` e reexportações `png`/`texture` têm consumidores e permanecem. Nenhum gerador, script CLI ou formato de arte foi excluído.

## O que permanece e por quê

- Codecs, componentes marcadores, handlers nativos e callbacks podem ser descobertos indiretamente pelo Hytale. São contratos de execução/persistência, não placeholders.
- Fábricas de nós e constantes utilizadas na integração continuam importantes para comparar circuito real e modelo puro. Métricas alimentam `/logicenergy` e benchmarks.
- Monitoramento de holders/seções/portas cobre callbacks que o SDK não emite em todos os casos; removê-lo exigiria outra solução comprovada, não uma limpeza de código.
- `/classroom --probe` isola diagnóstico de Custom UI; logging seletivo continua uma decisão futura, não foi acrescentado nesta manutenção.
- Testes de combinações/orientações, banco concorrente, migração, autorização e transporte cobrem invariantes distintas. Mantidos; testes/harnesses não são empacotados no mod instalado.

A inspeção usou o grafo de código com cobertura por arquivo/escopo e leitura das fontes. PNGs/cache não indexados não foram tratados como código órfão. Não se afirma ausência formal de todo código morto nem auditoria de APIs externas: as remoções estão limitadas aos candidatos acima e aos consumidores deste repositório.

## Verificação proporcional

Os 167 testes direcionados passaram; uma única rodada final de `fullCheck` foi aprovada em 3 min 23 s, com 910 JUnit, 38 Python e 90 verificações nativas, incluindo reinício real. Regressões acrescentadas para configuração de conexão em falha, erro de commit/rollback, paginação/binding compartilhados, limpeza dos campos após revogação e rotações inclinadas. O orçamento de uma conexão/até três SELECTs continua aprovado nos testes de leitura existentes. A [evidência da candidata](../evidence/classroom/2026-10-27-rc16-server.json) identifica o JAR exato, logs e execução CI; a documentação final é empacotada sem recompilar esse JAR.

Esta manutenção reduz duplicação e torna responsabilidades legíveis; contagem de linhas não é métrica de desempenho. Não há promessa de maior capacidade de jogadores. Cenários de cliente continuam no roteiro, especialmente critérios/relatório rc.15 ainda sem aceite completo. Evidências/artefatos rc.15 são preservados, sem tag, merge ou publicação estável automática.
