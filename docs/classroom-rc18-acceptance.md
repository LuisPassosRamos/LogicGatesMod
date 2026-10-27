# Classroom — aceite da 1.4.0-rc.18

Build **1.4.0-rc.18+2e27a7893893**, revisão de produção `0ac666f4ab40`. JAR SHA-256 `2ddb1af75d6b9c27d64ec0ed892ca50ada9857cede976cd3141b9832889d4630`.

Candidata de correção para Hytale **0.7.4**, SQLite **v6**. Corrige exclusivamente o alinhamento inválido que impedia carregar a UI da rc.17 e amplia o verificador correspondente. Mantém progresso, formulários, tabela visual, entregas e notas existentes. Artefatos anteriores são preservados. Não há migração nova, tag, merge ou publicação estável nesta entrega.

## Diagnóstico e correção

O [log enviado pelo usuário](evidence/classroom/2026-10-27-rc17-client.md) identifica `Pages/LogicGates/ClassroomRow.ui (19:141)` e falha ao resolver HorizontalAlignment como LabelAlignment no build **1.4.0-rc.17+1ee2a26cc4dc**. Substituído `Right` por `End`, conforme os documentos nativos 0.7.4. O verificador agora rejeita alinhamentos literais fora de Start/Center/End nas fontes e no JAR; ele não substitui o parser completo do cliente. O JAR rc.17 original foi recusado por essa nova regressão.

## Reteste no cliente — parcial

Em 27/10/2026, o [lote pt-BR com 19 capturas](evidence/classroom/2026-10-27-rc18-ptbr-client.md) confirmou a reabertura e a apresentação docente nesta sessão, no build acima. Resolução configurada informada: **1280×720**; escala de interface não informada. Foram registrados problemas visuais, incluindo o menu de dias na borda inferior, alinhamento do grupo de portas e orientações de rodapé fora de contexto. Não há aprovação integral dos fluxos. O usuário não dispõe de aluno para testar agora; inglês e outras dimensões serão enviados depois.

| Parte do roteiro | Estado após as capturas |
| --- | --- |
| Entrada e carregamento da UI rc.18 | Confirmados nesta sessão capturada; não é teste de reinício |
| Telas docentes pt-BR, seções e modos da tabela | Apresentação documentada, com ressalvas visuais no relatório |
| Consulta versus edição, formulário amplo e confirmação de saída | Estados visuais confirmados; efeitos de salvar/descartar/continuar pendentes |
| Progresso por aluno, entregas, notas e correção | Pendentes: capturas mostram zero alunos e zero entregas |
| Inglês, outras dimensões e escalas | Pendentes; dimensões dos PNGs não provam resolução do viewport ou escala |

O roteiro abaixo permanece para completar comportamento e cobertura:

1. Com mundo/servidor fechado, substituir o JAR rc.17 pelo **LogicGates-1.4.0-rc.18.jar**, conservando somente uma versão do mod em mods. Não apagar mundo, banco, submissions ou configurações. ZIP fica fora de mods.
2. Entrar no mesmo mundo no computador do trabalho; confirmar que não aparece Failed to load CustomUI documents. Informar o build exibido. Em caso de erro, enviar o novo trecho de CustomUI do log.
3. Abrir Classroom como professor: ver resumo e contador junto ao aluno, selecionar a ficha, abrir atividade, consultar e editar. Aluno: conferir catálogo, enunciado e entrega. Confirmar que tabelas/dropdowns e notas continuam funcionando.
4. Continuar o [roteiro funcional rc.17](classroom-rc17-acceptance.md#roteiro-no-cliente--pendente), incluindo resoluções/escalas e tabela visual. O erro de entrada corrigido não constitui aceite desses fluxos.

## Verificação automatizada

Os testes direcionados de ferramentas passaram: seis casos de UI e treze de empacotamento. A nova verificação rejeitou o ClassroomRow.ui do JAR rc.17 preservado e aprovou os seis documentos de origem corrigidos. Uma rodada final de `fullCheck` passou em **2 min 48 s**: **922 testes Java**, **41 Python** e **90 verificações nativas** (63 criação + 27 após reinício real). O [registro automatizado](evidence/classroom/2026-10-27-rc18-server.json) conserva resultados, identidade e checksum; log local `build/tmp/rc18-fullCheck.log`. Fingerprint empacotado confere com as fontes de produção commitadas. O indicador dirty=true registrado durante a compilação decorreu do estado transitório do índice do manifest, sem diferença de conteúdo em relação ao commit; o JAR testado foi preservado.

CI Windows/Linux aprovada na revisão `0ac666f4ab40a01d3960cebefaeeb0fd53aac834`, [run 40498835944](https://github.com/LuisPassosRamos/LogicGatesMod/actions/runs/40498835944). A nova entrada e a apresentação docente pt-BR foram posteriormente documentadas no [lote de capturas](evidence/classroom/2026-10-27-rc18-ptbr-client.md). Interações não demonstradas, aluno, inglês e demais resoluções/escalas continuam pendentes; nenhum resultado de cliente é inferido dos testes de servidor.
