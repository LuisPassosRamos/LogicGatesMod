# rc.18 — evidência visual pt-BR, lote 1

Registro datado da coleta. O **[aceite único](../../classroom-acceptance.md)** mantém a situação vigente, os achados e os roteiros; as conclusões abaixo preservam o escopo destas capturas.

Recebido em **27/10/2026**, computador secundário do trabalho. Build visível nos rodapés das capturas amplas: **1.4.0-rc.18+2e27a7893893**, Hytale **0.7.4**. O usuário informou resolução configurada **1280×720**. Escala de interface e escala do sistema operacional **não informadas**. O usuário informou que **não pode testar com aluno neste momento** e que enviará inglês e outras dimensões depois.

São **19 imagens originais preservadas**, com [manifesto de dimensões e SHA-256](2026-10-27-rc18-ptbr/manifest.json). Há quinze capturas amplas e quatro recortes de menus. Os PNGs amplos medem de 1882 a 1907 px de largura e de 1071 a 1088 px de altura. Essas medidas não coincidem com a resolução configurada relatada: não inferimos resolução do viewport, escala/DPI ou percentual de ocupação a partir delas. A configuração declarada pelo usuário permanece registrada como 1280×720.

Este registro distingue **apresentação confirmada**, **indício parcial** e **comportamento não demonstrado**. Capturas de estados diferentes não substituem uma sequência de ações ou o resultado de persistência. Nenhum novo teste de execução foi realizado pelo agente nesta rodada; a verificação desta alteração é documental. Código, JAR e ZIP da candidata permanecem os mesmos durante a coleta.

## Inventário das capturas

| ID / original preservado | Conteúdo | Dimensão do arquivo |
| --- | --- | --- |
| [S01](2026-10-27-rc18-ptbr/01-turma.png) | Turma sem alunos e resumo docente | 1890×1072 |
| [S02](2026-10-27-rc18-ptbr/02-catalogo.png) | Catálogo com uma publicada e um rascunho | 1887×1071 |
| [S03](2026-10-27-rc18-ptbr/03-nova-dados.png) | Nova atividade: dados iniciais | 1885×1071 |
| [S04](2026-10-27-rc18-ptbr/04-nova-componentes.png) | Nova atividade: componentes | 1886×1082 |
| [S05](2026-10-27-rc18-ptbr/05-resultados-desativados.png) | Resultados esperados desativados | 1890×1075 |
| [S06](2026-10-27-rc18-ptbr/06-tabela-visual.png) | Tabela visual: duas entradas e uma saída | 1896×1081 |
| [S07](2026-10-27-rc18-ptbr/07-tabela-texto.png) | Tabela em modo texto com saídas indefinidas | 1885×1082 |
| [S08](2026-10-27-rc18-ptbr/08-prazo-dia.png) | Prazo: menu de dias aberto | 1891×1077 |
| [S09](2026-10-27-rc18-ptbr/09-prazo-mes.png) | Prazo: menu de meses (recorte) | 777×401 |
| [S10](2026-10-27-rc18-ptbr/10-prazo-hora.png) | Prazo: menu de horas (recorte) | 247×410 |
| [S11](2026-10-27-rc18-ptbr/11-prazo-minuto.png) | Prazo: menu de minutos (recorte) | 198×403 |
| [S12](2026-10-27-rc18-ptbr/12-consulta-atividade.png) | Atividade publicada em consulta | 1887×1076 |
| [S13](2026-10-27-rc18-ptbr/13-edicao-atividade.png) | Atividade existente em edição | 1890×1077 |
| [S14](2026-10-27-rc18-ptbr/14-alteracoes-nao-salvas.png) | Confirmação de alterações não salvas | 1882×1071 |
| [S15](2026-10-27-rc18-ptbr/15-ingressos.png) | Ingressos sem registros pendentes | 1900×1080 |
| [S16](2026-10-27-rc18-ptbr/16-convidar-jogador.png) | Convidar jogador sem resultados | 1892×1082 |
| [S17](2026-10-27-rc18-ptbr/17-administracao.png) | Administração da turma | 1907×1077 |
| [S18](2026-10-27-rc18-ptbr/18-sem-autorizacao.png) | Tela de conta sem autorização | 1896×1088 |
| [S19](2026-10-27-rc18-ptbr/19-filtro.png) | Opções do filtro (recorte) | 537×183 |

S01/S02 mostram duas atividades totais, com uma publicada e um rascunho. S12/S13 mostram uma publicada e dois rascunhos sem título; S15–S17 mostram total três. Os valores são visualmente coerentes entre cabeçalho/lista/resumo nos estados mostrados. Não foi informado quais operações ocorreram entre as capturas; **não concluímos criação fantasma, atualização correta após cada operação ou persistência** apenas dessa diferença.

## Critérios satisfeitos no escopo visual

| Critério | Resultado demonstrado | Evidência / limite |
| --- | --- | --- |
| Carregamento após a correção de LabelAlignment | **Confirmado nesta sessão:** Classroom e atividades renderizam com rodapé rc.18 | S01–S08, S12–S18. O bloqueio de carregamento da rc.17 não se manifesta nesta sessão; não é prova de reinício ou todos os cenários. |
| Português nos elementos exibidos | **Confirmado visualmente:** rótulos, acentos, mensagens e opções legíveis, sem chaves de tradução expostas | Lote inteiro. Não cobre todas as traduções ou mensagens de erro. “Classroom” é o nome exibido do sistema. |
| Navegação principal docente | **Confirmada a apresentação:** Turma, Atividades, Ingressos, Convidar jogador e Administração | S01, S15–S17. Estados das telas aparecem; não é teste exaustivo de todos os caminhos. |
| Resumos e ausência de dados | **Confirmada a apresentação:** publicadas, alunos ativos e entregas para avaliar; mensagens de lista vazia | S01/S02/S15/S16. Zero alunos e zero entregas não validam agregação com dados reais. |
| Filtro e paginação de lista pequena | **Confirmada a apresentação:** dropdown oferece Todas/Publicadas/Rascunhos/Arquivadas; paginador ausente nas listas pequenas mostradas | S02/S12/S19. Resultado de selecionar cada filtro e paginação acima de vinte registros não demonstrados. |
| Nova atividade em largura total | **Confirmado visualmente:** lista e paginação ausentes; retorno às atividades disponível | S03–S08. Abertura sem persistir registro ainda exige teste de ida/volta sem salvar. |
| Três seções no editor | **Confirmada a apresentação:** Dados da atividade, Componentes, Resultados esperados | S03–S08/S13. Preservação de campos ao alternar não foi demonstrada por capturas com valores modificados. |
| Dados iniciais e valor | **Confirmado visualmente:** título/enunciado vazios, data vazia, hora 23 e minuto 59, valor 10 alinhado à esquerda | S03. Não comprova validação de entradas ou publicação. |
| Seletores de prazo | **Confirmada a renderização:** controles Dia/Mês/Ano/Hora/Minuto; menus de dia/mês/hora/minuto abertos | S03/S08–S11/S13. Menu do ano, seleção completa, limites e persistência pendentes; há ressalva de corte em S08. |
| Quantidades de componentes | **Confirmada a apresentação:** alavancas, cabos, lâmpadas e sete portas, com regra vazio/livre e zero/proibido | S04. Campos estão vazios; não comprova contagem exata ou recusa de valores inválidos. |
| Tabela opcional e modos visual/texto | **Confirmados os estados exibidos:** tabela desativada, ativada 2×1, contador 0/4, linhas visuais e texto das quatro combinações com ? | S05–S07. Exemplo AND, ajuda expandida, seleção de bits, mudança de dimensões, paginação e preenchimento completo pendentes. |
| Consulta separada da edição | **Confirmada a apresentação:** consulta com Editar atividade e editor com campos e Voltar à visualização | S12/S13. Não autoriza afirmar que alterações foram salvas/publicadas. |
| Prazo/valor ao abrir atividade existente | **Consistência visual confirmada:** consulta mostra 2026-10-27 20:00 (Brasília), valor 10; editor mostra 27/OUTUBRO/2026, 20:00 e 10 | S12/S13. Não é prova de conversão UTC, nova gravação ou persistência após reinício. |
| Confirmação de alterações não salvas | **Confirmada a exibição:** Salvar rascunho e sair / Descartar alterações / Continuar editando | S14. Resultado de cada escolha, manutenção de campos e fechamento por Esc não demonstrados. |
| Administração | **Confirmado visualmente:** formulário e Salvar alterações / Encerrar turma na mesma linha, encerramento em vermelho | S17. Gravação, encerramento, confirmação e revisões concorrentes pendentes. |
| Estado sem autorização | **Confirmada a apresentação restrita:** aviso e ausência dos formulários administrativos | S18. Não identifica como a permissão foi retirada nem comprova revogação ao vivo, restauração ou rejeição de operações forjadas. Não é teste de aluno. |

Esses resultados constituem **aceite visual parcial docente em pt-BR**, na configuração informada. O conjunto não equivale a aprovação funcional integral da rc.18 nem à medição da meta de ocupação da tela.

## Achados e melhorias registrados para o próximo polimento

Prioridade é proposta de triagem, não defeito funcional reproduzido pelo agente. Nenhuma dessas mudanças foi implementada nesta coleta.

| ID / prioridade | Observação nas capturas | Recomendação concreta e critério para conferir |
| --- | --- | --- |
| UI-01 / alta | S08: painel do seletor de dia chega ao limite inferior da imagem, com continuação cortada; cobre ações do rodapé. S09–S11 são recortes e não permitem julgar seus limites no viewport. | Conferir todos os menus em captura completa e rolar até o último valor. Ajustar altura/posicionamento com opções nativas verificadas; reduzir itens visíveis ou subir controles se necessário. Painel aberto e todos os valores devem permanecer acessíveis no viewport, inclusive em 720p. Não presumir bug de mês/hora só por recorte. |
| UI-02 / média | S04: grupo das sete portas inicia bem mais à direita que Alavancas/Cabos/Lâmpadas, apesar do espaço livre à esquerda. | Alinhar o bloco de portas com os demais campos e manter grade com quebra de linha. Learning.ui contém grupo com MaxWidth 1000 sem ancoragem explícita à esquerda, compatível com o deslocamento observado; validar o ajuste no cliente. |
| UI-03 / média | S03–S08/S14: rodapé diz “Selecione uma atividade; professores também podem criar rascunhos” durante criação e confirmação de saída. | Orientação por contexto: preenchimento, componentes/resultados e decisão sobre alterações. LearningPage.guidance usa chooseActivity quando activity é null, sem distinguir creating. O texto deve indicar a próxima ação da tela atual. |
| UI-04 / média | S01/S15/S16: resumo pede selecionar aluno com zero alunos e repete a mesma orientação nas abas Ingressos e Convidar jogador. | Mensagem conforme aba e estado vazio: convidar jogador, aguardar ingresso ou acompanhar alunos. ClassroomScreenRenderer.overview escolhe a ajuda apenas pelo papel docente/aluno. Não sugerir selecionar algo que não existe. |
| UI-05 / média | S08: topo dos botões de seção aparece parcialmente cortado dentro da região rolável. | Manter navegação de seções fora da rolagem do formulário ou reposicionar a rolagem de maneira nativa comprovada. Em todas as seções, nomes e bordas dos botões precisam ficar íntegros e alcançáveis. Learning.ui coloca essas ações dentro de TopScrolling. |
| UI-06 / média | S14: confirmação aparece na base de uma tela quase vazia, perdendo o contexto do formulário. | Apresentar confirmação compacta perto do contexto, com identificação da atividade e as três opções claras; preservar o snapshot de campos. Testar salvar/descartar/continuar e falha de gravação. Não tratar somente a presença dos botões como confirmação funcional. |
| UI-07 / baixa | S03: destaque de foco em Turma enquanto o caminho informa Nova atividade; seções têm aparência semelhante e não indicam claramente a seção ativa. | Separar visualmente foco de teclado e contexto ativo, usando recursos nativos válidos. O usuário deve identificar atividade/seção atual sem depender só do breadcrumb. |
| UI-08 / baixa | S01/S15/S16: Dados do vínculo fica longe do resumo, ao pé da coluna, com grande intervalo vazio. | Agrupar a ação secundária perto das informações às quais se refere. Conservar espaço vazio útil sem preenchimento decorativo ou repetição do resumo em todas as abas. |
| UI-09 / baixa | S18: aviso de falta de autorização é repetido no topo e no rodapé. | Uma explicação principal e uma orientação de recuperação suficientes; conservar bloqueio de ações e Recarregar/Fechar. |
| UI-10 / baixa | S12/S13: dois itens indistinguíveis “Atividade sem título”; S02: rótulo longo do cartão de rascunhos quebra linha. | Identificar rascunhos incompletos com metadado útil disponível/planejado, sem exigir título para salvar. Conferir cartões/rótulos longos também em inglês e telas menores; quebra de linha por si só não é erro. |

Arquivos relacionados, consultados para contextualizar os achados: [ClassroomScreenRenderer](https://github.com/LuisPassosRamos/LogicGatesMod/blob/0ac666f4ab40a01d3960cebefaeeb0fd53aac834/src/main/java/sisso/logicsim/classroom/ui/ClassroomScreenRenderer.java#L140), [LearningPage.guidance](https://github.com/LuisPassosRamos/LogicGatesMod/blob/0ac666f4ab40a01d3960cebefaeeb0fd53aac834/src/main/java/sisso/logicsim/classroom/ui/LearningPage.java#L402), [Learning.ui](https://github.com/LuisPassosRamos/LogicGatesMod/blob/0ac666f4ab40a01d3960cebefaeeb0fd53aac834/src/main/resources/Common/UI/Custom/Pages/LogicGates/Learning.ui), [Classroom.ui](https://github.com/LuisPassosRamos/LogicGatesMod/blob/0ac666f4ab40a01d3960cebefaeeb0fd53aac834/src/main/resources/Common/UI/Custom/Pages/LogicGates/Classroom.ui) e traduções [pt-BR](https://github.com/LuisPassosRamos/LogicGatesMod/blob/0ac666f4ab40a01d3960cebefaeeb0fd53aac834/src/main/resources/Server/Languages/pt-BR/server.lang). Causas visuais prováveis são separadas dos fatos capturados; renderização do ajuste proposto ainda não ocorreu.

## Testes que continuam pendentes

- **Aluno indisponível:** catálogo privado, progresso entregue/total junto ao nome, ficha com atividades/nota/prazo, entregas, reenvios, notas zero/ausentes, correção, relatório, preview e cópia temporária. As telas mostram zero alunos e zero entregas; não há nova validação desses fluxos.
- **Criação e persistência:** abrir/voltar sem salvar não criar registro; salvar rascunho incompleto, publicar, retornar/reabrir e reiniciar. Publicação inválida sem registro fantasma. A variação de contagens entre prints não comprova o resultado das operações.
- **Prazo:** selecionar os cinco valores, ano, último item dos menus, limpar data, data parcial, bissexto, mês de 31 dias, passado/futuro e persistência UTC/Brasília. Só estão provados controles abertos e consistência dos valores exibidos em consulta/edição.
- **Tabela:** escolhas 0/1, contador de preenchimento, exemplo AND, ajuda, 6×4 e paginação, preservação de respostas entre páginas/seções/modos, confirmação de substituição/remoção, salvar ? em rascunho e recusar publicação incompleta. S06 não mostra integralmente todas as linhas na mesma posição de rolagem.
- **Navegação e gestão:** resultados efetivos dos quatro filtros; listas com mais de vinte itens; rolagem de textos longos; cada decisão da confirmação de saída; salvar/encerrar turma e confirmações. Estado sem autorização não cobre transições ao vivo ou callbacks antigos.
- **Atualização de catálogo/progresso:** publicar, arquivar/restaurar e novas entregas/notas durante outra página aberta, sem perder campos nem reenviar MOTD.
- **Matriz visual:** inglês, outras resoluções e escalas de interface. Não marcar escala padrão/máxima, 1920×1080 ou meta de 90% por eixo a partir das dimensões dos PNGs deste lote. A resolução relatada desta rodada é 1280×720, com escala não informada.

## Próxima coleta

Acrescentar inglês e outras dimensões ao registro da **mesma build**, identificando resolução configurada, escala de interface e, quando conhecido, tamanho da janela/viewport. Capturas completas com menus abertos ajudam a separar corte real de recorte da imagem. Para comportamento, acrescentar sequência/relato do que foi clicado e resultado após voltar/reabrir; isso permite promover um item de apresentação parcial a teste funcional confirmado. Testes com aluno permanecem pendentes até haver conta disponível.

## Atualização posterior — lote do jogo em inglês

Recebidas [13 capturas com o jogo em inglês](2026-10-27-rc18-en-client.md), na mesma resolução relatada 1280×720 e mesma build. Localização reprovada por mistura de idiomas, I18N-01. O novo lote acrescenta saída 1/contador 1 de 4, consistência visual com texto, ajuda expandida, confirmação de substituição e consulta com AND completa; somente esses estados deixam de estar sem evidência visual. Resultados de ações, persistência, aluno e outras dimensões/escalas permanecem pendentes. Este registro pt-BR conserva o escopo e as conclusões do primeiro lote.
