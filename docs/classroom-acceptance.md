# Classroom — aceite, pendências e roteiros

Atualizado em **27/10/2026**. Este é o **único documento mantido para o estado atual de aceite, correções pendentes e roteiros de teste do Classroom**. Consolida e substitui os 13 roteiros por candidata, da rc.6 à rc.18. Nas próximas candidatas, atualizar este arquivo, conservando os IDs dos casos; não criar outro arquivo de acceptance por versão.

Capturas, relatos e resultados automatizados permanecem como evidências datadas em `evidence/classroom/`; [validation.md](validation.md) conserva a cronologia das execuções. Uma pendência escrita numa evidência antiga descreve aquela rodada. O estado vigente e a decisão sobre o que testar ficam aqui. Guias de [administração](classroom.md) e [atividades/entregas](classroom-learning.md) descrevem o uso; o [roadmap](roadmap/README.md) e os contratos detalham propostas, sem constituir outro checklist de aceite.

## Navegação

- [Estado atual](#estado-atual)
- [Correções e melhorias abertas](#correções-e-melhorias-abertas)
- [Preparar o teste](#preparar-o-teste)
- [Ordem de execução](#ordem-de-execução)
- [Roteiros e critérios](#roteiros-e-critérios)
- [Massa isolada para paginação](#massa-isolada-para-paginação)
- [Verificação automatizada](#verificação-automatizada)
- [Registro de resultados e fechamento](#registro-de-resultados-e-fechamento)
- [Trabalho futuro fora do aceite atual](#trabalho-futuro-fora-do-aceite-atual)
- [Histórico e rastreabilidade](#histórico-e-rastreabilidade)

## Estado atual

**Candidata 1.4.0-rc.20 em preparação:** exclusão confirmada de rascunhos nunca publicados e uma camada extra na seleção de entrega implementadas. Hytale **0.7.4**, SQLite **v6**. A identidade e os resultados finais serão registrados após a validação. Os 11 ajustes da rc.19 permanecem incorporados; aceite de cliente atual pendente.

| Arquivo entregue para teste | SHA-256 / identificação |
| --- | --- |
| `build/libs/LogicGates-1.4.0-rc.20.jar` | A registrar após a validação final |
| `build/releases/LogicGates-1.4.0-rc.20.zip` | Mesmo JAR validado, documentação final e checksum externo em `LogicGates-1.4.0-rc.20.zip.sha256`, ao lado do ZIP. |

A rc.20 preserva os artefatos anteriores e o esquema/dados existentes. A exclusão atinge somente o rascunho confirmado de atividade nunca publicada. Não atribuir à nova candidata os resultados de cliente da anterior. A instalação de teste usa o JAR final identificado aqui, sem misturar versões em mods.

| Área | Evidência disponível | Estado vigente / o que falta |
| --- | --- | --- |
| Entrada após falha da rc.17 | [Log da falha](evidence/classroom/2026-10-27-rc17-client.md); rc.18 substitui HorizontalAlignment Right por End; novas telas renderizam | Carregamento confirmado na sessão capturada. Reabertura, reconexão e reinício controlados continuam no roteiro. |
| Apresentação docente pt-BR | [19 capturas](evidence/classroom/2026-10-27-rc18-ptbr-client.md) | Visual parcial confirmado na rc.18; UI-01–UI-10 implementados na rc.19, aguardando reteste visual e funcional. |
| Jogo em inglês | [13 capturas](evidence/classroom/2026-10-27-rc18-en-client.md) | **Mistura de idiomas comprovada na rc.18.** I18N-01 implementado na rc.19 com rota única de mensagens; reteste de início/troca de idioma pendente. |
| Tabela e confirmações | Consulta mostra AND completa; visual/texto mostram uma combinação preenchida e contador 1/4; ajuda e aviso de substituição visíveis | Apresentação confirmada nesses estados. Efeitos dos botões, preservação completa e publicação ainda exigem teste. |
| Fluxos administrativos | [rc.4](evidence/classroom/2026-10-24-rc4-client.md), [rc.9](evidence/classroom/2026-10-25-rc9-client-report.md), [rc.10](evidence/classroom/2026-10-26-rc10-client.md) | Confirmações históricas discriminadas abaixo; complementos e regressão proporcional da candidata atual pendentes. |
| Atividade, entrega e correção com duas contas | [Fluxo principal rc.12](evidence/classroom/2026-10-26-rc12-client.md); [polimento rc.14 por relato](evidence/classroom/2026-10-26-rc14-client.md) | Histórico confirmado no escopo descrito. Novos critérios/progresso e casos complementares não são aprovados por esses relatos. |
| Aluno, progresso, critérios e relatório atuais | Últimos lotes mostram zero alunos e zero entregas; usuário não dispõe de aluno neste momento | Testes com aluno, dados reais e verificação assistida continuam pendentes. Indisponibilidade de conta não é reprovação do recurso. |
| Resolução e escala | Ambos os lotes rc.18: **1280×720 relatados**, escala de interface/SO não informada | Outros tamanhos, escala padrão/máxima e ocupação útil não medidos. Dimensões físicas dos PNGs não provam viewport ou escala. |
| Automação da rc.19 | 927 Java, 44 Python, 90 verificações nativas; CI Windows/Linux aprovada | Aprovada nas camadas exercitadas. Não executa renderização/cliques do cliente nem valida idioma resolvido. |
| Novos pedidos rc.20 | [Captura de rascunhos na rc.19](evidence/classroom/2026-10-27-rc19-ptbr-drafts.png) e relato do usuário em 27/10 | Exclusão e camada extra implementadas; validação automatizada final e reteste E08/D10 pendentes. |

Os 32 originais e seus hashes estão nos manifestos [pt-BR](evidence/classroom/2026-10-27-rc18-ptbr/manifest.json) e [jogo em inglês](evidence/classroom/2026-10-27-rc18-en/manifest.json). Os lotes foram recebidos do computador secundário do trabalho. Variação de contagens entre imagens não comprova registro fantasma, nova gravação ou atualização correta após cada operação.

A captura adicional rc.19 tem build visível `1.4.0-rc.19+a15a278adf4c`, pt-BR, SHA-256 `1d1358d9e85227a9c912ea5d4aa886dbc0e1b825abd5e399bfe17e5b6420e470`. Mostra seis rascunhos com referências distintas, seção Dados destacada, prazo antes do enunciado e orientação contextual. Confirma somente essa apresentação: não demonstra cliques, popup, inglês ou todas as correções. Resolução/escala não foram reiteradas nesse print. O problema de altura na seleção foi relatado pelo usuário, sem captura do volume selecionado.

## Correções e melhorias abertas

Os **11 ajustes estão implementados na rc.19**. Permanecem abertos para aceite até o reteste dos casos indicados no cliente. A suíte direcionada de UI passou com 67 testes e a rodada completa foi aprovada conforme registro abaixo. IDs e evidências rc.18 são preservados.

| ID / prioridade | Trabalho registrado | Critério para encerrar / casos | Implementação rc.19 / reteste |
| --- | --- | --- | --- |
| **I18N-01 / alta** | Diagnosticar e corrigir português/inglês misturados em navegação, títulos, formulários, rodapé, ajuda e confirmações | Textos do sistema seguem o idioma nos caminhos de início e troca de língua; conteúdo do usuário e fuso preservados. V01. | 116 textos de três páginas enviados como Message.translation em execução, incluindo títulos e grupos ocultos; markup sem referências de tradução estática. Reteste V01 pendente. |
| **UI-01 / alta** | Conferir o corte do menu de dias na borda inferior e o alcance dos demais seletores | Todos os valores, inclusive o último, acessíveis em captura completa; menu e ações alcançáveis em 720p. V03, P01. Mês/hora/minuto eram recortes: não presumir o mesmo defeito neles. | PanelAlign Bottom, quatro itens visíveis com rolagem nativa; prazo/valor antes do enunciado longo. Reteste de limites no viewport pendente. |
| UI-02 / média | Alinhar grupo de portas com alavancas/cabos/lâmpadas | Grade alinhada à esquerda, com quebra de linha adequada nas dimensões testadas. V02, C01. | Grade de portas ancorada à esquerda, preservando quebra de linha. Reteste visual pendente. |
| UI-03 / média | Trocar a orientação de selecionar atividade durante criação/saída por orientação do contexto atual | Próxima ação coerente nas três seções e na confirmação. V04, E01–E04. | Orientação própria de Dados/Componentes/Resultados e da decisão de saída. Contextos cobertos por testes; reteste no cliente pendente. |
| UI-04 / média | Adaptar resumo/orientação à aba e à ausência de alunos | Turma, Ingressos e Convite não pedem selecionar algo inexistente. V04, A01. | Título/ajuda por aba e estado vazio; cartões do resumo somente na visão de turma/ficha própria. Testes de estados passaram; reteste no cliente pendente. |
| UI-05 / média | Manter as seções Dados/Componentes/Resultados acessíveis durante a rolagem | Botões íntegros e alcançáveis em todas as seções; sem corte parcial do topo. V02, E03, T02. | Seções movidas para fora do conteúdo TopScrolling, preservando campos. Reteste de rolagem/escala pendente. |
| UI-06 / média | Apresentar confirmação de saída próxima do contexto da atividade | Decisão clara, com identificação da atividade e três opções; salvar/descartar/continuar preservam o estado correto, inclusive em erro. E04. | Confirmação no topo do detalhe, com título atual da atividade; conteúdo/ações restantes ocultos sem limpar os campos. Testes de transporte/snapshot passaram; decisões reais pendentes. |
| UI-07 / baixa | Diferenciar foco de teclado e contexto/seção ativa | Seção atual identificável sem depender só do breadcrumb; foco não sugere outra tela ativa. V05. | Seção ativa usa botão primário, demais usam secundário; bindings mantêm as mesmas ações. Estado/bindings testados; foco visual pendente. |
| UI-08 / baixa | Aproximar Dados do vínculo das informações relacionadas | Ação agrupada com seu contexto, sem grande distância vazia na coluna. V04. | Dados do vínculo junto ao detalhe, visível somente quando há registro/ficha própria. Estados de visibilidade testados; reteste visual pendente. |
| UI-09 / baixa | Reduzir repetição do aviso de falta de autorização | Explicação e orientação suficientes, mantendo restrição efetiva e ações de recuperação. V04, X01. | Explicação de bloqueio no corpo e orientação de recuperação distinta no rodapé. Bloqueio preservado/testado; reteste no cliente pendente. |
| UI-10 / baixa | Distinguir rascunhos sem título e conferir textos longos dos cartões | Metadado útil diferencia rascunhos sem exigir título para salvar; texto legível nos dois idiomas/escalas. V02, E07. | Referência curta estável de rascunho sem título na lista, consulta e caminho; cartões com altura explícita ampliada para texto de até três linhas. Referências testadas; textos longos/escala pendentes. |

I18N-01: as capturas mostram diferença entre referências estáticas e mensagens atualizadas em execução. A rc.19 elimina esse caminho divergente nas páginas Classroom: o markup define estrutura/estilo e os textos são enviados pelo builder como mensagens traduzíveis nativas, preservando dados literais do usuário. A origem interna da divergência de idioma/cache no cliente não foi demonstrada; o reteste V01 continua necessário. A regressão verifica os comandos realmente construídos para todos os textos declarados nos layouts, seus IDs e chaves nos dois catálogos.

UI-01/UI-07: os contratos usados foram conferidos em `Common/UI/Custom/Common.ui` e páginas nativas do Assets.zip 0.7.4: EntriesInViewport/PanelAlign Bottom e estilos primário/secundário. O painel do prazo usa quatro entradas de 32 unidades, com rolagem. Essa conferência não substitui medir corte/escala no cliente.

Compatibilidade dos cartões: altura explícita de 100 unidades, sem MinHeight/MaxHeight, ausentes do Anchor.CODEC 0.7.4. O guard agora verifica os campos literais de Anchor nas fontes e no JAR; oito testes direcionados de markup e 14 de empacotamento passaram, incluindo rejeição de bytes com MinHeight. Os 166 documentos UI nativos foram aceitos pelo guard ampliado. Não é um parser completo do cliente. Links de código nas evidências rc.18 foram fixados na revisão correspondente do GitHub para funcionarem também no ZIP, que não distribui fontes Java.

| ID / prioridade | Novo pedido / implementação rc.20 | Critério de aceite |
| --- | --- | --- |
| DRAFT-01 / média | Excluir rascunho salvo nunca publicado, inclusive incompleto/arquivado. Confirmação identificada; cancelar preserva campos; permissão/turma/revisão/histórico conferidos em transação; catálogo e contagens notificadas. Publicadas usam arquivamento. | E08: cancelar/confirmar, reabrir, paginação, autorização e publicação concorrente; preservar entregas/notas. Cliente pendente. |
| SELECT-01 / média | Cantos inclusivos recebem uma camada acima do maior Y. Contorno, dimensões e captura usam o volume ampliado; camada conta nos limites. Colagem docente e arquivos antigos mantêm dimensões. | D10: clicar chão plano/desnivelado em ambas as ordens; snapshot inclui chão/circuito, limites recusam sem corte. Cliente pendente. |

## Preparar o teste

1. Registrar build ID, revisão quando conhecida e SHA-256 do JAR efetivamente instalado. Para o novo reteste, usar o JAR final rc.20 identificado acima; as coletas anteriores pertencem às rc.18/rc.19.
2. Com mundo/servidor parado, preservar **banco, pasta `submissions`, configuração e mundos juntos**, fora de `build/`. Instalar somente uma versão do JAR do mod em `mods`; manter o ZIP fora dessa pasta.
3. SQLite v6 não muda entre rc.15–rc.20. Atualização de v5 tem backup pré-migração consistente; rollback para rc.14 exige banco v5 correspondente, e para versões mais antigas exige esquema compatível. Não abrir v6 no mod antigo nem apagar dados para contornar erro de UI. Consultar [recuperação](classroom-learning.md#persistência-e-recuperação).
4. Usar turma descartável e contas separadas: professor autorizado e aluno **sem OP**. Outra conta/professor é necessária só para concorrência entre turmas e alguns processos simultâneos. Na falta de conta, marcar os casos dependentes como não executados e continuar os independentes.
5. Obter UUIDs com `/uuid`. No console, conceder/remover a permissão específica conforme o cenário:

```text
perm user add <UUID> sisso.logicgates.classroom.teach
perm user remove <UUID> sisso.logicgates.classroom.teach
```

6. Para listas extensas, seguir a [massa isolada](#massa-isolada-para-paginação). Outro mundo no mesmo servidor compartilha o banco; usar diretório de dados independente, sem substituir a turma real.
7. Registrar idioma, resolução configurada, escala real de interface e, quando conhecida, escala do SO e viewport. Capturar tela completa nos casos de corte/ocupação. Quando não informado, escrever “não informada”; não deduzir do tamanho do PNG.

## Ordem de execução

1. **Disponível agora sem aluno:** S01–S03, V01–V05, E01–E08, P01–P05, C01–C03 e T01–T06, nos casos que não dependem de outra conta/rascunho legado. Começar pela exclusão E08, depois a regressão de I18N-01/UI-01 e demais ajustes; se falhar, registrar e corrigir antes de fechar o item.
2. **Com professor e aluno:** executar a [prova curta](#roteiro-com-professor-e-aluno), depois os complementos A, G, D, N e X afetados pela mudança. Não repetir como campanha nova tudo que já foi confirmado historicamente quando o código correspondente não mudou.
3. **Com massa/ambiente isolado:** paginação, textos longos, contas concorrentes, limite de tabela/área e recuperação após interrupção. Não fabricar o caso de ano antigo alterando o banco real.
4. **Matriz visual:** conferir os dois idiomas e as combinações de resolução/escala disponíveis, começando por 1280×720; registrar explicitamente cada combinação.
5. **Fechamento:** associar falhas e correções aos IDs, realizar verificações proporcionais e conferir os critérios de [encerramento do aceite](#registro-de-resultados-e-fechamento).

## Roteiros e critérios

Os estados abaixo conciliam evidências recebidas e implementação até 27/10/2026; testes de cliente ainda referem-se às builds discriminadas, com reteste rc.20 pendente. **Visual parcial** confirma o que a imagem mostra; **histórico confirmado** conserva o resultado naquela build; ambos podem exigir execução funcional na candidata atual. **Pendente** significa que o cenário não foi demonstrado no escopo atual. Uma alteração apenas documental não executa nenhum caso de cliente.

### Abertura e sessão

| ID | Procedimento e resultado esperado | Estado atual |
| --- | --- | --- |
| S01 | Entrar, abrir `/classroom`, Atividades, consulta/editor e caminhos de gravação/erro. Sem Failed to load CustomUI documents, desconexão ou propriedade recusada. Registrar primeiro erro e trecho relevante do log do cliente. | Entrada e telas rc.18 confirmadas; gravação/erro atuais pendentes. |
| S02 | Fechar/reabrir, Esc em consulta, trocar página/mundo, reconectar e iniciar nova sessão. Dados persistidos reaparecem; resposta da página antiga não invade a atual. No editor, conferir E04: Esc não deve fechar descartando alterações. | Pendente na candidata atual. |
| S03 | `/classroom --probe`: atualizar contador, fechar/reabrir e conferir reset. Comparar primeira troca de aba com seguintes, no host e segundo cliente, separando demora de UI e movimento do mundo. | Probe histórico rc.1; abas instantâneas relatadas na rc.10, sem cronometragem. Regressão proporcional pendente. |

### Idiomas, dimensões e apresentação

| ID | Procedimento e resultado esperado | Estado atual |
| --- | --- | --- |
| V01 | Iniciar cliente com en-US selecionado e testar também pt-BR→inglês e caminho inverso. Comparar reabrir página, reconectar e reiniciar. Conferir navegação, rótulos, filtros/dropdowns abertos, estados vazios, erros, ajuda, confirmações, professor/aluno e falta de autorização. Textos do sistema seguem a língua; nomes/enunciado do usuário permanecem literais, assim como identificação do fuso pedagógico. | Falha comprovada na rc.18; implementação de I18N-01 na rc.19 com cobertura de mensagens no build. Reteste de cliente pendente, inclusive início em inglês; reinício continua variável de diagnóstico. |
| V02 | Em pt-BR/en-US, conferir Turma/Ingressos/Convite/Administração, lista/consulta/edição/criação, ficha do aluno e correção. Usar 1280×720 e 1920×1080 nas escalas padrão e máxima disponível; complementar com 1366×768 e 2560×1440, registrando combinações indisponíveis. Painel deve ocupar ao menos **90% da largura e 90% da altura úteis** (81% da área), medidos no viewport conhecido. Sem rótulos sobrepostos/cortados; listas/textos rolam; navegação/paginação/ações quebram linha e continuam alcançáveis. | Parcial visual em 1280×720 relatados, escala desconhecida. Ocupação e demais combinações não medidas. UI-02/UI-05/UI-10 abertos. |
| V03 | Abrir cada seletor de prazo próximo à borda; rolar até último dia/mês/ano/hora/minuto e selecionar. Painel/menu e ações acessíveis no viewport completo, sem depender de recorte da captura. | Corte observado no dia na rc.18; painel inferior/reduzido implementado rc.19. Todos os limites no cliente pendentes. |
| V04 | Conferir estados vazios, página posterior esvaziada, mensagens de operação e falta de autorização. Texto indica próxima ação possível na aba; Anterior permite voltar; confirmação/erro/gravação não são substituídos por ajuda genérica. Conferir Dados do vínculo e ausência de avisos redundantes. | Visual parcial; UI-03/UI-04/UI-08/UI-09 abertos. Prioridade de mensagens/página esvaziada pendentes. |
| V05 | Navegar por teclado e trocar seções/telas. Seleção, breadcrumb e ficha correspondem ao contexto; foco e seção ativa distinguíveis; títulos/bordas dos botões íntegros. | Destaque ativo e bindings implementados/testados na rc.19; foco/interação no cliente pendentes. |

### Roteiro administrativo

| ID | Procedimento e resultado esperado | Estado atual |
| --- | --- | --- |
| A01 | Abrir sem vínculo, com professor e com aluno. Professor vê somente própria turma/processos; aluno somente própria ficha/matrícula e próprios ingressos; conta sem vínculo encontra turmas e próprios processos, sem administração indevida. Conferir listas vazias e busca positiva/negativa. | Histórico rc.4/rc.10 confirmado no escopo relatado; privacidade e regressão atual pendentes. |
| A02 | Solicitar ingresso com matrícula vazia e depois `00123`; professor corrige e aprova. Vazia recusada sem vínculo parcial; zeros preservados ao reabrir/reconectar. Convite aceito também exige conferência/aprovação, sem ingresso imediato. | Fluxo e zeros confirmados historicamente; regressão atual pendente. |
| A03 | Convite para jogador conhecido offline, aceitar após reconectar/reiniciar; combinar solicitação/convite para mesma turma. Um processo aberto, sem sobrescrever matrícula em conferência. | Complementos pendentes. |
| A04 | Recusar convite, cancelar convite/solicitação e rejeitar. Encerrar somente processo correspondente, sem vínculo; histórico identifica tipo, situação e instante. | Rejeição histórica confirmada; demais decisões sem confirmação individual atual. |
| A05 | Duplicar matrícula entre ativos e disputar aprovação do mesmo UUID entre duas turmas. Unicidade por turma com comparação exata; `001a` difere de `001A`; primeiro vínculo ativo vence e encerra demais pendências. Aprovação repetida não duplica vínculo. | Cobertura automatizada; cliente depende de contas adicionais, pendente. |
| A06 | Corrigir matrícula, remover/sair com cancelar/confirmar e reingressar. Confirmação obrigatória, novo vínculo com identidade própria; ação antiga não remove vínculo novo; histórico anterior conservado. | Saída/remoção/reingresso históricos confirmados; callbacks antigos e complementos atuais pendentes. |
| A07 | Salvar nome/descrição/mensagem longos e multilinha; esvaziar mensagem. Painel e conteúdo persistem; mensagem preenchida envia uma vez por conexão, sem duplicar na troca de mundo; vazia não aparece nem envia no login seguinte. | Mensagem/persistência históricas confirmadas; vazia/não duplicação/textos extensos pendentes. |
| A08 | Paginar mais de vinte membros, ingressos, turmas descobertas e histórico; buscar, selecionar, trocar aba e selecionar novamente. Linha/botão atua no UUID exibido sem ação duplicada; segunda página e página esvaziada coerentes. Matrícula longa íntegra na ficha. | Busca rc.10 confirmada; paginação extensa e bindings atuais pendentes. |
| A09 | Em turma descartável, editar sem salvar e iniciar encerramento; cancelar conserva campos/vínculos. Confirmar com aluno e processo pendente libera participantes, cancela pendências, remove da descoberta e atualiza páginas abertas. Criar nova turma/reingressar com `00123`; histórico persiste após reinício. | Encerramento aprovado por relato rc.9; cancelamento, pendências simultâneas e reinício não discriminados. |
| A10 | Revogar antes de confirmar encerramento ou alterar turma em outra página. Sem operação desautorizada; revisão antiga recusada, sem sobrescrever dados. Só recarregar e repetir conscientemente. | Complementos pendentes; aplicar X01/X02. |

### Atividades, edição e publicação

| ID | Procedimento e resultado esperado | Estado atual |
| --- | --- | --- |
| E01 | Abrir Nova atividade, alternar três seções e voltar/descartar sem salvar: nenhum registro. Primeiro salvar cria só um rascunho; primeira publicação inválida reverte criação/gravação e conserva campos. | Formulário amplo confirmado visualmente; efeitos/persistência pendentes. Variação de contagens não prova criação fantasma. |
| E02 | Selecionar atividade existente abre consulta. Editar entra explicitamente; salvar rascunho mantém editor; publicar retorna à consulta/lista. Rascunho salvo privado difere da publicação vigente do aluno. Arquivada consultável pelo professor sem Editar; restaurar permite editar. | Consulta/editor visuais confirmados; funcionamento/polimento rc.14 por relato; novos caminhos atuais pendentes. |
| E03 | Alterar título/enunciado, data, quantidades e tabela; trocar seções/modos/páginas e receber atualização comum. Campos permanecem, mantendo revisão original. Erro aponta seção pertinente e permite correção sem perder conteúdo. | Seções visíveis; preservação funcional atual pendente. |
| E04 | Com alterações, tentar voltar/trocar atividade/filtro/página ou fechar. Conferir Salvar rascunho e sair, Descartar alterações e Continuar editando, além de erro ao salvar. Descartar só mudanças locais; continuar conserva tudo; salvar confirma gravação antes de sair. Esc não fecha o editor; desconectar encerra temporários não salvos. | Três opções visíveis; resultados e UI-06 pendentes. |
| E05 | Publicar título/enunciado vazios, prazo passado/parcial, valor inválido/não positivo ou com mais de duas casas; corrigir e publicar. Sem publicação parcial ou registro fantasma; valor inicial 10. Rascunho aceita incompleto, sem inventar prazo. | Valor inicial e campos visíveis; erro de prazo com preservação confirmado rc.12; regressão completa atual pendente. |
| E06 | Rascunho privado, publicada, publicada com edição privada, arquivada e vencida: aluno vê só publicação disponível. Editar sem republicar conserva anterior; republicar não muda enunciado/valor/critérios de entrega antiga; arquivar/restaurar não cria versão indevida. Vencida continua consultável e aceita envio/reenvio atrasado identificado. | Fluxo principal relatado rc.12; critérios por publicação e complementos atuais pendentes. |
| E07 | Mais de vinte atividades/alunos/revisões: filtros Todas/Publicadas/Rascunhos/Arquivadas, paginação, retorno da lista de alunos e seleção correta. Paginador oculto numa única página; criação ampla sem lista/filtro/Nova atividade. Diferenciar rascunhos sem título. | Lista pequena, filtro aberto e criação ampla visíveis; filtros aplicados/paginação e UI-10 pendentes. |
| E08 | Em turma descartável, excluir rascunho incompleto, recém-salvo e arquivado. Abrir confirmação com campos não salvos: cancelar conserva tudo; confirmar remove somente a atividade identificada, volta à página 1 do filtro e atualiza contagens. Reabrir/reiniciar: não reaparece. Excluir último item da página 2, repetir clique e tentar com aluno/outro professor/permissão revogada. Editar/publicar em outra sessão antes de confirmar deve recusar a revisão antiga. Atividade já publicada com edição privada não oferece exclusão; entregas/notas intactas. | Implementado rc.20; reteste no cliente pendente. |

### Prazo de Brasília

| ID | Procedimento e resultado esperado | Estado atual |
| --- | --- | --- |
| P01 | Conferir cinco seletores completos, data vazia e 23:59 inicial; salvar rascunho sem prazo. Escolher valores até último item. | Controles, menus de dia/mês/hora/minuto e valores iniciais visíveis; ano e seleção completa pendentes, UI-01 aberto. |
| P02 | Data parcial exige completar/limpar, preservando outros campos. Trocar mês com dia 31 e fevereiro de 2028/2027: 29/28 dias; limpar só dia inválido com aviso, preservando hora/minuto/enunciado. | Pendente. |
| P03 | Selecionar ano atual e dez seguintes; abrir ano persistido fora dessa janela. Ano salvo permanece disponível. Usar dado adequado/fixture; não alterar banco real para fabricar o caso. | Cobertura automatizada; cliente pendente quando houver dado adequado. |
| P04 | Selecionar 00:00/23:59, passado/futuro; publicação passada recusada e futura aceita, sem arredondamento. Vencimento não arquiva nem reduz contagem de publicada. | Erro histórico de prazo confirmado; limites e candidata atual pendentes. |
| P05 | Salvar, reabrir, reconectar e reiniciar processo. Mesmo instante UTC e exibição Brasília, sem deslocamento de dia/hora. | Só consistência visual consulta/editor de 27/10/2026 20:00 e valor 10 confirmada; nova gravação/reinício pendentes. |

### Quantidades e tabela esperada

O exemplo completo para os casos abaixo usa AND=1, alavancas=2, lâmpadas=1 e cabos livres; outras portas podem ser proibidas por zero. Cada bit corresponde a alavanca/lâmpada ordenada pela posição **local X, depois Y, depois Z**, em ordem crescente, começando em I1/O1. Explicar a ordem no enunciado. Sem identificação personalizada de I/O neste incremento.

```text
00 -> 0
01 -> 0
10 -> 0
11 -> 1
```

| ID | Procedimento e resultado esperado | Estado atual |
| --- | --- | --- |
| C01 | Preencher dez componentes: alavancas/cabos/lâmpadas e sete portas. Vazio deixa livre, zero proíbe, positivo exige quantidade exata. Conta todos na área, inclusive desconectados; blocos comuns não contam. Conferir alinhamento e limite por campo de 16384. | Campos/regra visíveis; preenchimento/validação atual e UI-02 pendentes. |
| C02 | Publicar sem tabela: somente quantidades são verificadas, sem simulação. Quantidades preenchidas de alavancas/lâmpadas devem coincidir com dimensão da tabela quando ativada. Erro sem salvar parcialmente. | Pendente no cliente. |
| C03 | Salvar/reabrir/reiniciar preserva livre/zero/positivo e critérios; aluno vê só publicados. Republicar não muda critérios da publicação de entrega antiga. | Pendente no cliente. |
| T01 | Ativar 2×1; conferir quatro combinações, saídas —/0/1 e contador de preenchidas. Selecionar 0 e 1, aplicar Exemplo AND e conferir quatro resultados. Ajuda abre/fecha. | Desativada/ativada, saída 1/contador 1/4 e ajuda visíveis. Consulta mostra AND completa; isso não prova acionamento do exemplo. Demais ações pendentes. |
| T02 | Mudar para 6×4, conferir 64 combinações, oito linhas por página/até 32 células visuais. Preencher páginas diferentes e voltar; respostas permanecem entre páginas/seções/modos, todas as ações e linhas alcançáveis. | Só 2×1 visual/texto consistente parcialmente; limite/paginação e UI-05 pendentes. |
| T03 | Mudar dimensões, aplicar exemplo sobre respostas e remover tabela: conferir aviso, cancelar conserva integralmente, confirmar substitui somente respostas locais previstas. Publicação vigente permanece intacta até publicar novamente. | Aviso de substituição visível; decisões/remoção pendentes. |
| T04 | Salvar rascunho com ? e reabrir; publicação incompleta recusada. Completar e publicar: todas combinações, sem duplicatas, larguras consistentes, 1–6 entradas/1–4 saídas. Erro conserva campos e revisão. | Pendente no cliente. |
| T05 | Abrir texto legado inválido em modo avançado: conteúdo exato preservado até correção/substituição confirmada. Alternar visual/texto não perde resposta válida. Não fabricar legado alterando dados reais. | Texto parcial atual visível; legado/ida e volta completos pendentes. |
| T06 | Salvar/reabrir, consultar como professor e aluno e reiniciar. Tabela/quantidades/prazo preservados; professor pode consultar rascunho e aluno apenas publicação; ajuda/legendas/mapeamento compreensíveis nos dois idiomas. | Consulta AND e inglês parcial visíveis; persistência/privacidade atuais pendentes, I18N-01 aberto. |

### Progresso e ficha do aluno

| ID | Procedimento e resultado esperado | Estado atual |
| --- | --- | --- |
| G01 | Três atividades entregues entre quatro publicadas não arquivadas mostram **3/4 entregues** junto ao aluno. Rascunhos excluídos; vencidas incluídas; reenvios/republicações não duplicam. Arquivar/restaurar recalcula. | Pendente: capturas atuais sem alunos. |
| G02 | Ficha paginada mostra atividade, situação, prazo e nota da última revisão. Nota zero é **0/valor**, ausência **—/valor**; clique abre atividade/aluno corretos. Revisão antiga avaliada não atribui nota à nova. Republicação com novo valor mantém nota pelo valor entregue. | Pendente no progresso atual; revisões/notas básicas históricas confirmadas rc.12. |
| G03 | Professor: resumos publicadas/alunos/aguardando avaliação. Aluno: publicadas/entregues/pendentes. Contagem administrativa docente inclui não arquivadas inclusive rascunhos; aluno somente publicadas. Comparar lista/cabeçalho/cartões, sem contar duas vezes edição privada. | Coerência visual em listas pequenas/zero entregas; agregação com dados reais pendente. |
| G04 | Publicar, arquivar/restaurar, entregar e avaliar enquanto outra página está aberta: atualização relacionada após commit, sem perder campos. Entregas/notas atualizam progresso sem invalidar contagem de catálogo/MOTD indevidamente. Sair e reingressar mesma conta não duplica entregas. | Pendente no progresso atual; atualização administrativa e descrição preservada confirmadas rc.10. |
| G05 | Gerenciar aluno exige ação explícita, responsabilidade/permissão/revisão e confirmações. Aluno não recebe matrícula de colegas, rascunhos privados ou notas alheias. | Gestão histórica confirmada; integração com ficha/progresso atual pendente. |

### Roteiro com professor e aluno

Prova curta inicial: E01/E02 → D01/D02 → D03/D04 → N01/N03 → D05 → D06, finalizando em G01/G02. O fluxo principal já foi confirmado na rc.12; repetir proporcionalmente às mudanças atuais e registrar separadamente os complementos ainda não executados.

| ID | Procedimento e resultado esperado | Estado atual |
| --- | --- | --- |
| D01 | Professor publica exemplo; aluno sem OP usa Entregar, equipa Seleção Classroom, esquerdo/direito escolhem cantos inclusivos. Reabrir `/classroom`: atividade/dimensões corretas; enunciado, nova seleção e Minha entrega claramente distintos. Comandos gerais de edição continuam indisponíveis. | Seleção/entrega sem OP confirmadas por relato rc.12; nova apresentação atual pendente. |
| D02 | Entregar área pequena com chão permitido, alavanca ligada, cabos, lâmpada e porta girada. Revisão 1 identificada, nota inicial —/valor. Alterar original depois não modifica snapshot. Critérios divergentes não bloqueiam entrega. | Snapshot/ligada/independência históricos confirmados; critérios atuais pendentes. |
| D03 | Professor abre aluno/revisão, gira/inclina/zoom/move preview. Seleção de outra revisão/aluno não mantém prefab antigo; metadados simples não reenviam prefab. Sem sobreposição de preview e avaliação; conteúdo longo/feedback alcançável, sem espaço artificial para vazio. | Preview fiel confirmado rc.12; gestos e atualização de metadados não discriminados. |
| D04 | Colar em espaço vazio; cópia identifica atividade/aluno/revisão. Professor aciona, quebra cabo e acrescenta bloco dentro da reserva; aluno não coloca/quebra/aciona. Original e revisão salva intactos. | Cópia/edição/proteção confirmadas por relato rc.12; regressão atual pendente. |
| D05 | Descartar com cancelar/confirmar: remove todo volume, inclusive edições, preservando original e entrega. Sair/retornar ao mundo e depois reiniciar processo com região carregada naturalmente: recuperação limpa reserva e preserva arquivo. Interromper colagem somente em ambiente isolado. | Descarte/limpeza ao retornar confirmados rc.12; confirmação atual, interrupção e reinício de processo pendentes. |
| D06 | Nota 7/10 com feedback; aluno vê resultado. Reenviar revisão 2 começa —/10; revisão 1 conserva nota. Avaliar antiga novamente não avalia nova. Testar zero, negativa, acima do valor e casas decimais inválidas; erro preserva campos. | Fluxo/revisões relatados rc.12; zero/erros e regressão atuais pendentes. |
| D07 | Cancelar seleção, desconectar, trocar mundo, expirar sessão; ferramenta antiga/passada para outra conta não envia. Sair/encerrar/republicar durante envio recusa contexto antigo. | Complementos pendentes. |
| D08 | Limite **32×16×32**, área vazia, região indisponível, material especial recusado e conexão externa à seleção. Sem captura parcial nem carregamento forçado. Registrar recusa de material separadamente de erro lógico. | Automação cobre limites; `Soil_Grass_Full` recusado historicamente e continua limitação, sem promessa de suporte adicional. Cliente complementar pendente. |
| D09 | Colar sobre construção/fluido, região protegida ou com outra cópia ativa: falha preserva construção e não deixa reserva utilizável. Conferir plugin de proteção usado no servidor, quando houver. | Complementos pendentes. |
| D10 | Com aluno sem OP, selecionar cantos do chão permitido com esquerdo/direito, em ambas as ordens. Mesmo Y resulta em altura 2 (chão + componentes); em desnível, topo final é maior Y + 1. Contorno e dimensões correspondem ao preview/snapshot; sem pilares de apoio. Testar área máxima incluindo camada extra: excesso deve recusar sem corte, sem carregar regiões e sem burlar reserva/proteção. Colar entrega antiga mantém suas dimensões. | Implementado rc.20; reteste no cliente pendente. Restrições de materiais de D08 continuam. |

### Verificação assistida da entrega salva

| ID | Procedimento e resultado esperado | Estado atual |
| --- | --- | --- |
| N01 | Professor seleciona revisão e usa Verificar circuito. Relatório identifica revisão/publicação, quantidades encontradas/exigidas, coordenadas locais I/O e cada esperado/encontrado. Conferir exemplo AND correto e entrega com erro lógico. | Pendente no cliente desde introdução dos critérios rc.15. |
| N02 | Acrescentar porta desconectada/proibida por zero; comportamento equivalente pode passar e quantidade falhar. Incompatibilidade alavancas/lâmpadas versus tabela não inventa I/O; I/O sem ligação e realimentação não resolvida são inconclusivos. Falha de contexto não é resposta lógica errada. | Cobertura automatizada; cliente pendente. |
| N03 | Enquanto verifica, manter nota/feedback em edição e cópia ativa. Análise não altera nota, campos, intenção salva das alavancas, original ou cópia. Salvar avaliação continua explícito; sem tabela só conta, sem simular. | Pendente no cliente. |
| N04 | Republicar regras/valor e analisar entrega antiga: usar publicação entregue. Trocar revisão descarta relatório antigo; reenvio não herda nota. Resultado é consulta sob demanda da página, sem histórico persistido nem execução periódica. | Pendente no cliente. |
| N05 | Até **2048 componentes lógicos, 6 entradas, 4 saídas e 64 combinações**; relatório máximo legível/rolável. Construção maior pode ser entregue dentro do limite da área e corrigida manualmente; limite da análise não apaga entrega. | Limites automatizados; apresentação/erros no cliente pendentes. |
| N06 | Análise concorrente com captura/preview/colagem: fila pesada existente, uma executando, oito aguardando e uma por conta; ocupado responde sem perder dados. Revogar/trocar página durante análise: resultado antigo não aparece em contexto desautorizado. | Cobertura automatizada; cliente pendente, aplicar X01/X03. |

### Autorização, concorrência e recuperação

| ID | Procedimento e resultado esperado | Estado atual |
| --- | --- | --- |
| X01 | Revogar/restaurar com página administrativa, editor e correção abertos, sem clicar para forçar atualização. Revogação bloqueia e limpa temporários docentes, inclusive prazo/critérios/nota/feedback. Concessão recupera dados persistidos sem reabrir editor; callbacks antigos não restauram dados. | Revogação automática relatada rc.7; rc.18 mostra só estado sem autorização, sem transição. Restauração e todos os contextos atuais pendentes. |
| X02 | Remover OP mantendo permissão docente direta: acesso/rascunho conservados. Durante atualização comum preservar texto inicialmente vazio, seletores e revisão; outra gravação exige conflito em vez de sobrescrever. Recarregar explicitamente descarta local e recupera salvo; texto não salvo não reaparece após revogação/restauração. | Descrição durante atualização confirmada rc.10; demais campos/contextos atuais pendentes. |
| X03 | Fechar/trocar página, conta/conexão ou mundo durante I/O. Resposta antiga não contamina contexto novo; nenhum callback concede acesso ou envia MOTD indevido. Vínculo/histórico corretos ao reingressar. | Complementos pendentes. |
| X04 | Em ambiente isolado, reiniciar processo e conferir turma/UUID, matrícula `00123`, processos, publicação/critério, arquivo/versões/notas e intenção das alavancas. Conferir limpeza de reservas quando seções carregam naturalmente. Backup/rollback conserva banco e arquivos privados compatíveis. | Reinício real automatizado; cliente completo pendente. Retorno ao mundo rc.12 não prova reinício de processo. |

## Massa isolada para paginação

O gerador está somente no classpath de teste, usa serviços/repositório reais, cria destino **novo** e recusa sobrescrever banco existente. Não há comando de carga no mod nem massa empacotada no JAR/ZIP. Obter UUIDs com `/uuid` e substituir os placeholders:

```powershell
.\gradlew.bat prepareClassroomUiFixture -PclassroomFixtureTeacher=<UUID_PROFESSOR> -PclassroomFixtureStudent=<UUID_ALUNO>
```

Saída em `build/classroom-ui-fixtures/<UUID>/`, com `classroom.db`, `submissions` e README próprio. A massa pedagógica introduzida na rc.11 inclui 25 publicações (uma arquivada), três rascunhos privados, 25 alunos com entrega e 25 revisões para a conta do aluno na atividade 01; revisão 1 avaliada e mais recente sem nota. Mantém registros administrativos: 25 alunos ativos, 25 solicitações pendentes, 25 perfis conhecidos sem vínculo, histórico extenso e descrição/mensagem com 2048 caracteres. Conferir o README produzido pela versão executada.

1. Criar save/servidor exclusivo, com diretório de dados do plugin diferente do real; outro mundo no mesmo servidor não isola o banco.
2. Iniciar/encerrar e confirmar o caminho `Classroom database: <caminho>` no log.
3. Com ambiente de teste parado, preservar dados de teste anteriores e copiar **banco e submissions juntos** conforme README, somente ao destino isolado. Nunca substituir a turma real.
4. Conceder permissão ao UUID docente e conectar as duas contas; nomes autenticados substituem os fictícios dessas contas.
5. Conferir página 2 de membros/ingressos/convite/atividades/revisões, seleção e filtros; aluno consulta própria ficha e histórico após sair, sem acesso a dados privados de terceiros.

A massa serve para UI/paginação, não estimativa de jogadores suportados nem prova de autoria. Sem massa/contas disponíveis, manter os casos dependentes pendentes; não editar o banco real para forçá-los.

## Verificação automatizada

**rc.20:** rodada direcionada/final em andamento. Os resultados desta candidata serão registrados aqui, sem converter testes de servidor em aceite de cliente.

**rc.19:** `fullCheck` aprovado em **3 min 5 s**, com **927 testes Java** (zero falhas, erros ou ignorados), **44 Python** e **90 verificações nativas** (63 criação + 27 após reinício real em outro processo). Projeto e distribuição aprovados: **370 chaves em cada um dos dois idiomas**, seis documentos UI e verificações de assets/ZIP. [Registro integral](evidence/classroom/2026-10-27-rc19-server.json); log local `build/tmp/rc19-fullCheck-final.log`; relatório `build/reports/server-integration/20261027-123118-0576c3/summary.json`.

CI aprovada em **Windows e Linux** para a revisão de produção `4048f8f6b341003d1f796ed7a11bb3ae91b2b877`: [run 40531924972](https://github.com/LuisPassosRamos/LogicGatesMod/actions/runs/40531924972). A rodada direcionada anterior passou com 67 testes de UI; a revisão de compatibilidade passou com oito testes de markup e 14 de empacotamento. Logs locais `build/tmp/rc19-ui.log` e `build/tmp/rc19-build-tools.log`.

O novo guard recusou o alinhamento Right no JAR rc.17 preservado e aprovou End na fonte/JAR rc.18; aceita literais Start/Center/End. As regressões também verificam propriedades compatíveis, codecs/bindings, privacidade, contagem distinta, nota zero/ausente/revisão, criação com rollback, tabelas incompletas/legadas, até oito linhas/32 células visuais e preservação de controles. Não há parser completo de Custom UI: fonte/transporte/chaves não validam renderização ou tradução resolvida no cliente, como demonstra I18N-01.

Orçamentos preservados por testes: lista docente de membros com uma conexão/até três SELECTs; quatro com ficha no mesmo snapshot. Sem arquivo de circuito para contar progresso ou consulta por aluno; seções/tabela/data são locais. Sem polling, MOTD extra ou varredura periódica. As comparações rc.9/rc.10 e rc.16/rc.17 continuam em [desempenho](performance.md) e [evidência rc.17](evidence/classroom/2026-10-27-rc17-performance.json); não certificam capacidade de jogadores/latência com muitas entregas.

O fingerprint empacotado da rc.19 é `a15a278adf4c735148a5f8afdead9f0f30c902029a5d0bd5cb88e3010debd0a9` e confere com as fontes de produção commitadas; `dirty=false`. Os bytes do JAR testado são preservados no empacotamento da documentação final, sem recompilação. A atualização documental posterior não altera essa identidade de produção.

Os artefatos rc.18 foram preservados e seus hashes reconferidos: JAR `2ddb1af75d6b9c27d64ec0ed892ca50ada9857cede976cd3141b9832889d4630`, ZIP `c281791b2f4473d1d93849bb95f98e3bdc6b178c7a71ba68770c30cab8f1317f`. Seu [registro automatizado](evidence/classroom/2026-10-27-rc18-server.json) permanece histórico; o campo client pendente antecede as capturas posteriores, já conciliadas neste documento. O `dirty=true` daquele registro foi um estado transitório do índice, sem diferença no conteúdo das fontes.

Para futuras correções, executar testes proporcionais ao código alterado e os casos de cliente atingidos. Rodada final de `fullCheck` quando a candidata de produção estiver pronta, conforme [desenvolvimento](development.md#verificação-proporcional-à-alteração), sem repetir baterias por atualização apenas documental. Atualizar este bloco com a nova identidade/resultado; manter evidências anteriores datadas. A consolidação anterior foi documental; a rodada rc.19 executa os testes identificados neste bloco, sem inferir renderização do cliente.

## Registro de resultados e fechamento

Atualizar **neste arquivo** o estado dos casos e o quadro de correções. Novas capturas/logs/JSONs podem ficar como anexos datados em `evidence/classroom/`, com link daqui. Evitar repetir a lista vigente de pendências nos guias, roadmap ou em novo acceptance por versão.

| Campo do registro | Como preencher |
| --- | --- |
| ID / execução | Caso estável (ex.: T03), data, build ID/revisão e SHA-256 instalado |
| Ambiente | Máquina/host ou segundo cliente, papel, idioma, resolução, escala real e viewport quando conhecido |
| Ação / resultado | Procedimento efetivamente executado, esperado e observado; resultado de confirmar/cancelar e após reabrir quando aplicável |
| Estado | Confirmado funcionalmente, visual parcial, reprovado ou não executado; discriminar relato/captura/automação |
| Evidência / correção | Link do anexo ou relato; ID da falha e revisão da correção; registrar reteste antes de fechar |

Não converter screenshot de botão em teste da ação, consulta de AND em execução do exemplo, retorno ao mundo em reinício de processo, zero alunos em teste de progresso ou PNG grande em resolução/escala. Casos indisponíveis permanecem identificados com motivo.

Critérios para fechar a candidata:

- [ ] Retestar I18N-01/UI-01 e os demais ajustes implementados na rc.19; registrar evidência por caso e decisão explícita para qualquer adiamento. Implementação não fecha o aceite visual.
- [ ] Retestar exclusão de rascunhos E08 e seleção pelo chão D10 da rc.20.
- [ ] Completar os novos fluxos de criação/saída, tabela, progresso e verificação assistida; executar regressão breve com professor/aluno, revisão/nota correta e dados preservados.
- [ ] Registrar cada combinação visual executada e os complementos de autorização/concorrência/persistência; identificar claramente o que não pôde ser testado, sem declarar matriz completa.
- [x] Associar a automação à build/JAR exatos, rodar verificações proporcionais e preservar evidências/checksums do arquivo entregue. Resultados futuros de cliente devem identificar o JAR efetivamente instalado.
- [ ] Obter decisão explícita sobre aceite estável. Tag, merge e publicação são marcos próprios; a consolidação não os realiza.

## Trabalho futuro fora do aceite atual

Propostas já registradas, sem implementação ou prazo assumido por este checklist. Não bloquear o teste de uma correção de UI exigindo todas essas extensões.

| Proposta | Trabalho ainda necessário / referência |
| --- | --- |
| Cabos entre alturas | Protótipo de subida/descida de um bloco, suporte/passagem, visual e simulação coerentes; depois ramificações. Colunas contínuas e apoios parciais são etapas separadas. [Plano](roadmap/vertical-connections.md). |
| Logging seletivo | Falhas operacionais agrupadas e limitadas; diagnóstico temporário desligado por padrão, com expiração/amostragem e sem dados dos formulários. [Viabilidade](reviews/2026-10-26-logging-feasibility.md). |
| Pontuação automática | Definir como converter critérios em nota, aceitar equivalência comportamental sem ignorar quantidades e distinguir falha de contexto de resposta errada. Hoje verificação assistida não atribui nota. [Contratos](roadmap/classroom-learning-contracts.md). |
| Extensões administrativas | Transferência de professor, múltiplos responsáveis, restauração de turma encerrada e retenção/exclusão definitiva exigem contratos próprios. [Plano Classroom](roadmap/classroom.md#fora-do-escopo-atual). |
| Bancadas/autoria e materiais | Propriedade/proteção permanente, prova de autoria, compatibilidade adicional de materiais e identificação personalizada de I/O exigem regras próprias; reserva temporária de correção já existe. [Contratos](roadmap/classroom-learning-contracts.md). |
| Outros idiomas e desempenho | Novos locales conforme contrato, após validar a resolução atual de en-US; otimizações como invalidação por região dependem de medição. [Roadmap](roadmap/README.md). |

Fora da campanha Classroom, o roadmap também conserva verificações manuais de drops, cancelamento/reembolso de fabricação e abertura dos projetos no Blockbench. São validações complementares do projeto, não funcionalidades novas nem motivo para repetir toda a campanha de circuitos por uma alteração documental/administrativa.

## Histórico e rastreabilidade

Os antigos `classroom-rc6-acceptance.md` a `classroom-rc18-acceptance.md` foram incorporados a este arquivo e removidos para impedir checklists concorrentes; versões originais continuam no histórico Git. Instalações antigas e pendências já esclarecidas não devem orientar a execução atual. Identidades/automação por candidata permanecem em [validation.md](validation.md), [histórico anterior](history/validation.md) e JSONs de evidência; não precisam ser recopiadas a cada teste.

| Rodada / evidência | Resultado que deve ser preservado | Limite ao interpretar hoje |
| --- | --- | --- |
| [rc.4](evidence/classroom/2026-10-24-rc4-client.md) | Convite/aprovação, zeros da matrícula, saída/remoção/reingresso, rejeição, atualização e mensagem/persistência relatados | Restauração exigia recarga; não prova concorrência/matriz atual. |
| [Falha rc.5](evidence/classroom/2026-10-24-rc5-client.md), [rc.6](evidence/classroom/2026-10-24-rc6-client.md) | Markup corrigido; abertura docente pt-BR capturada, quatro anexos idênticos | Mesma tela não prova fechar/reabrir ou ações. |
| [rc.7](evidence/classroom/2026-10-24-rc7-multiplayer-report.md) / rc.8 | Multiplayer e revogação automática por relato; custo inicial motivou otimizações | Restauração automática e todos os campos não discriminados; benchmark não reproduz cliente. |
| [rc.9](evidence/classroom/2026-10-25-rc9-client-report.md), [rc.10](evidence/classroom/2026-10-26-rc10-client.md) | Encerramento por relato; cinco testes de abas, atualização, saída/contagem, busca e descrição preservada/salva | Cancelamento/concorrência/longas listas e matriz não individualizados. Demora inicial deixou de ser observada nessa rodada. |
| [Bloqueio rc.11](evidence/classroom/2026-10-26-rc11-client-blocker.md), [fluxo rc.12](evidence/classroom/2026-10-26-rc12-client.md) | `.Disabled` incompatível removido; atividade/entrega sem OP/preview/cópia/edição/proteção/revisões/nota e limpeza ao retornar confirmados | Não repetir “todo fluxo pendente desde rc.11”; complementos, reinício de processo e recursos posteriores continuam abertos. |
| [Capturas rc.13](evidence/classroom/2026-10-26-rc13-client.md), [relato rc.14](evidence/classroom/2026-10-26-rc14-client.md) | Apresentação parcial e satisfação com funcionamento/polimento testados | Relato genérico não discrimina idiomas, resoluções, escalas ou todos os casos. |
| [Automação rc.15](evidence/classroom/2026-10-26-rc15-server.json), [rc.16](evidence/classroom/2026-10-27-rc16-server.json) | Critérios/avaliação isolada e manutenção técnica automatizados | Interação dos critérios/relatório ainda precisa de aceite; nota continua manual. |
| [rc.17 automatizada](evidence/classroom/2026-10-27-rc17-server.json), [falha de cliente](evidence/classroom/2026-10-27-rc17-client.md) | Progresso/editor/tabela implementados; log comprovou alinhamento Right inválido | Automação aprovada não evitou bloqueio de carregamento. |
| [rc.18 automatizada](evidence/classroom/2026-10-27-rc18-server.json), [pt-BR](evidence/classroom/2026-10-27-rc18-ptbr-client.md), [jogo em inglês](evidence/classroom/2026-10-27-rc18-en-client.md) | End corrigiu bloqueio nesta sessão; 32 capturas documentam apresentação e novos estados da tabela | Inglês reprovado por mistura, onze achados abertos; sem aluno ou outra resolução/escala registrada. |
| [rc.19 automatizada](evidence/classroom/2026-10-27-rc19-server.json) | I18N-01 e UI-01–UI-10 implementados; 927 Java, 44 Python, 90 verificações nativas e CI Windows/Linux aprovados | Reteste do cliente nos dois idiomas, limites de tela/escala e casos com aluno continuam pendentes. |
| [Captura rc.19 de rascunhos](evidence/classroom/2026-10-27-rc19-ptbr-drafts.png) | Referências distintas, seção ativa e reorganização/orientação do formulário visíveis; usuário solicita exclusão e camada extra na seleção | Evidência visual parcial em pt-BR; não comprova ações ou outros cenários. Pedidos implementados na rc.20, ainda sem reteste do cliente. |

A coleta e a consolidação anteriores foram documentais. A rc.19 implementa os 11 ajustes acima, mantendo o banco e os artefatos anteriores; confirmações históricas não aprovam sua renderização.
