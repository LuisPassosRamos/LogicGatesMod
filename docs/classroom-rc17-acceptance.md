# Classroom — aceite da 1.4.0-rc.17

Candidata de acompanhamento e clareza da interface, Hytale **0.7.4**, SQLite **v6**. Build **1.4.0-rc.17+1ee2a26cc4dc**, revisão de produção `a8e231672b57`, compilação limpa. JAR SHA-256 `2cb243eaaa389d50548ef8c8c2c607602fbffc29c313a211692ef41f798a7ca6`. A renderização da candidata no cliente **está pendente**; não é inferida dos testes de transporte nem das capturas rc.15. Artefatos rc.16 são preservados. Sem tag, merge ou publicação estável nesta entrega.

## Comportamentos esperados

| Área | Critério verificável |
| --- | --- |
| Turma docente | Aluno com três entregas entre quatro publicadas não arquivadas mostra **3/4 entregues**; rascunhos não entram. Expiradas entram; reenvios e republicações não duplicam; arquivar/restaurar atualiza. |
| Ficha do aluno | Atividades paginadas com situação, prazo e nota da última revisão. Zero é **0/valor** e ausência é **—/valor**. Clique abre atividade/aluno corretos. Gestão de matrícula/remoção exige Gerenciar aluno e mantém confirmação/revisão/permissão. |
| Resumos e navegação | Professor vê publicadas/alunos/aguardando avaliação; aluno vê publicadas/entregues/pendentes. Atividades na navegação superior. Paginador de uma página oculto; filtros nomeados em dropdown. |
| Nova atividade | Abrir e descartar não cria registro. Primeiro salvar cria rascunho; primeira publicação inválida reverte tudo. Salvar mantém formulário amplo; publicar retorna à consulta. Selecionar atividade existente só consulta; Editar entra no formulário. |
| Sair da edição | Campos alterados, inclusive prazo/quantidades/tabela, oferecem salvar rascunho e sair, descartar alterações ou continuar. Esc não fecha o editor. Erro ao salvar conserva os campos e aponta a seção. Desconexão encerra campos temporários não salvos. |
| Tabela visual | De uma a seis entradas e uma a quatro saídas. Todas as combinações geradas, oito por página, saídas —/0/1. Paginar preserva respostas; exemplo AND, ajuda e confirmação de substituição/remoção funcionam. |
| Compatibilidade de critérios | Rascunho com ? reabre sem perda; publicação incompleta falha. Texto antigo inválido abre no modo avançado e permanece exato até correção/substituição confirmada. Quantidades exatas/blank livre/zero proibido continuam. |
| Entrega e correção | Alternar enunciado/minha entrega; nova área identificada separadamente da entrega salva. Preview e avaliação sem sobreposição; nota limitada à publicação entregue. Verificar circuito não atribui nota. Cópia ativa identifica atividade/aluno/revisão; descarte confirma e preserva entrega original. |
| Atualização e privacidade | Criar/publicar/arquivar/restaurar/enviar/avaliar atualiza páginas relacionadas após commit, sem resetar campos durante atualização comum. Revogação limpa campos, callbacks antigos não restauram dados, concessão não reabre editor. Aluno não recebe rascunho/nota de terceiros. |
| Consultas | Uma conexão/até três SELECTs na lista docente; quatro com ficha, mesmo snapshot. Sem leitura de arquivos de circuito para contagem ou consulta por aluno. Seções/tabela/data são locais; sem polling/MOTD adicional. |

## Roteiro no cliente — pendente

1. Usar contas separadas: professor autorizado e aluno sem OP. Guardar banco/pasta submissions/mundo antes de trocar o JAR, com servidor parado. Instalar somente o JAR rc.17 em mods; ZIP fora dessa pasta.
2. Testar 1280×720 e 1920×1080 nas escalas padrão e máxima disponível; anotar a escala real. Smoke em 1366×768 e 2560×1440. Capturas completas do viewport permitem medir ocupação. pt-BR e en-US: conferir textos, foco, dropdowns, quebra de linha, rolagem e alcance das ações.
3. Usar turma vazia e com mais de vinte alunos/atividades. Selecionar aluno, paginar ficha e abrir atividade; voltar ao catálogo. Conferir resumo, matrícula e Dados técnicos; remoção/saída/encerramento continuam exigindo confirmação e acesso correto.
4. Misturar rascunho privado, publicada, publicada com edição privada, arquivada e vencida. Enviar duas revisões da mesma atividade; avaliar antiga, depois a mais recente com zero. Republicar com novo valor; conferir a nota pelo valor antigo entregue. Arquivar/restaurar e reingressar a mesma conta na mesma turma.
5. Abrir Nova atividade, alternar as três seções e voltar sem salvar; nenhum rascunho fantasma. Repetir salvando incompleto, reabrir e publicar. Exercitar as três escolhas de saída, fechamento e erro de prazo/tabela. Atualização concorrente preserva os campos; revisão desatualizada é recusada.
6. Selecionar datas vazias/parciais, bissexto, troca de mês com dia 31, ano salvo fora da janela, 00:00/23:59, passado/futuro. Salvar/reabrir/reiniciar preserva instante UTC e exibição Brasília.
7. Ativar tabela 2×1, aplicar exemplo AND e conferir quatro linhas. Mudar para 6×4, preencher páginas distintas, confirmar/cancelar mudança de dimensão e remoção. Salvar incompleta, reabrir, completar e publicar. Abrir rascunho legado inválido no modo texto e confirmar que não houve perda silenciosa.
8. Aluno: enunciado → selecionar área → conferir nova seleção → enviar → Minha entrega. Professor: preview → verificar circuito → salvar nota/feedback → colar → editar → confirmar descarte. Alternar revisão mantém nota correspondente e conteúdo original. Reabrir após reinício.
9. Revogar/restaurar autorização com editor e correção abertos; trocar página/conexão enquanto consulta está em andamento. Nenhum callback antigo deve preencher dados desautorizados. Confirmar que entregas/notas não reenviam mensagem do dia.

## Verificação automatizada

Uma rodada final de `fullCheck` passou em **2 min 57 s**: **922 testes Java**, **38 Python**, **90 verificações nativas** (63 criação + 27 após reinício real). Dos Java, 169 pertencem a Classroom. A [evidência automatizada](evidence/classroom/2026-10-27-rc17-server.json) conserva identidade, checks e limites; log local `build/tmp/rc17-fullCheck.log`.

Verificados isolamento de rascunhos, contagem distinta, nota zero/ausente/revisão anterior, orçamento de consultas, publicação inicial com rollback, respostas incompletas/legadas, limite de oito linhas/32 células e preservação de controles nativos durante atualização. Verificador documental aprovou links locais, **361 chaves por idioma** e **seis documentos UI**. Conferências de fonte e transporte não executam o renderizador Custom UI do cliente.

O [benchmark comparativo](evidence/classroom/2026-10-27-rc17-performance.json) executou o mesmo harness administrativo contra os JARs rc.16/rc.17, três JVMs novas por versão, alternando a ordem. Não houve falha nas rajadas de 25/100/250 consultas. A fixture tem 25 membros e nenhuma atividade publicada: esse ensaio mede o custo administrativo, não certifica latência com muitas entregas. Correção e orçamento do progresso são exercitados pelos testes de banco. Consulte [desempenho](performance.md).

A CI Windows/Linux passou na revisão `a8e231672b57951dae093322c50fae827f6029f6`: [run 40492988939](https://github.com/LuisPassosRamos/LogicGatesMod/actions/runs/40492988939). A documentação/evidência final é incorporada ao ZIP sem regenerar os bytes do JAR testado.
