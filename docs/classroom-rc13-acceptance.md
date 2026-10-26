# Classroom — aceite da 1.4.0-rc.13

**Cliente desta candidata: pendente.** O [fluxo principal rc.12](evidence/classroom/2026-10-26-rc12-client.md) foi confirmado em multiplayer. Isso não aprova a renderização do novo dimensionamento ou dos dropdowns. Esquema SQLite v5, entregas e versões preservados; sem publicação estável, tag ou merge em `main`.

Com o mundo parado, instale somente `LogicGates-1.4.0-rc.13.jar` em `mods`, substituindo o anterior. Guarde o ZIP fora de `mods`. Faça backup conjunto de `classroom.db`, `submissions`, configuração e mundos; não apague dados para testar a interface. Identifique cada resultado pelo build ID e SHA-256 entregues.

## Primeira conferência: dimensionamento

Abra Turma, Administração e Atividades como professor e aluno. Em **1280×720, 1366×768, 1920×1080 e 2560×1440**, na escala padrão e na máxima disponível, registre resolução, escala real e captura **completa do viewport**. Se a máquina não oferecer uma resolução, marque não executado.

Critério: painel ocupa pelo menos **90% da largura e 90% da altura úteis** (81% da área); nenhum rótulo, controle ou ação cortado/sobreposto. Navegação e ações quebram linha quando necessário. Listas e detalhes rolam; o bloco de ações continua acessível. Margem de 16 unidades lógicas não prova o percentual em cada escala: medir a captura. Se esta base falhar, registrar a tela/escala antes dos demais casos.

## Criação, consulta e administração

1. Nova atividade: desaparecem lateral, paginação, filtro e Nova atividade; formulário ocupa a largura, enunciado começa com 180 unidades. Salvar rascunho mantém esse modo; Publicar retorna à apresentação normal com lista. Voltar às atividades conserva o rascunho já criado, sem salvar campos automaticamente. Editar atividade existente e consultar alunos mantêm lateral/paginação.
2. Ficha do aluno e correção do professor: título → metadados compactos → enunciado → entrega/preview → avaliação. Textos curtos não deixam reservas vazias de 70–100 unidades; textos longos/multilinha e feedback são integralmente acessíveis. Feedback vazio não reserva espaço. Entrega antiga continua identificando sua publicação.
3. Administração: Salvar alterações à esquerda e Encerrar turma à direita quando couber; empilhamento nessa ordem quando necessário. Confirmar e cancelar encerramento funcionam; confirmar exige responsabilidade/permissão/revisão. Saída e remoção de aluno mantêm confirmações anteriores.
4. Repetir rótulos em pt-BR e en-US; verificar Salvar rascunho, Salvar avaliação, Anterior, Próxima e Voltar às atividades completos.

## Seletores do prazo

| Caso | Resultado esperado |
| --- | --- |
| Data vazia | Hora inicial 23:59; salvar rascunho sem prazo; publicar exige prazo futuro |
| Data parcial | Mensagem pedindo completar/limpar, sem perder os demais campos |
| Fevereiro de 2028/2027 | 29/28 dias; dia inválido removido com aviso, sem alterar hora/minuto/enunciado |
| Trocar mês com dia 31 | Somente o dia é limpo quando inexistente |
| Ano atual + dez seguintes | Todos disponíveis; ano já salvo fora da janela também aparece |
| 00:00 e 23:59 | Sem arredondamento; salvar/reabrir preserva seleção |
| Prazo passado/futuro | Publicação passada recusada; futura aceita; vencida continua consultável |
| Reabertura após reinício | Mesmo instante UTC e mesma data/hora de Brasília |

Não é necessário nem recomendado editar o banco do mundo para fabricar o caso de ano antigo; esse caso tem regressão automatizada e pode ficar pendente no cliente até haver um rascunho adequado. Trocar seletores atualiza apenas o dia, sem consulta ao banco.

## Contagem e atualização

Monte uma turma descartável com rascunho privado, publicada, publicada com edição privada, arquivada e vencida. Professor conta todas as não arquivadas, uma vez cada; aluno conta somente publicadas não arquivadas. Vencimento não reduz contagem. Criar, publicar, arquivar/restaurar atualizam o resumo relacionado automaticamente. Notas, entregas e preview não provocam invalidação da contagem nem mensagem do dia.

Enquanto edita campos e seletores, provoque atualização comum pelo outro computador: campos permanecem. Revogue/restaure a permissão docente com a página aberta: painel restrito desaparece, rascunho visual docente é descartado, restauração carrega os dados persistidos; callback antigo não restaura acesso. Aluno sem OP continua consultando e entregando.

Reaproveite o [gerador isolado de listas extensas](classroom-rc11-acceptance.md#massa-paginada-isolada), sem tocar no banco real, para busca, seleção, paginação e textos grandes. Não use a massa como estimativa de capacidade de jogadores.

Finalize com regressão curta: ingresso/matrícula, entrega, preview, colagem, descarte e nota na revisão correta. A campanha rc.12 permanece histórica; as novas interações e a matriz acima exigem seus próprios resultados. Logging ampliado, materiais adicionais e avaliação automática estão fora desta entrega.
