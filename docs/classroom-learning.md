# Classroom — atividades, entregas de áreas e avaliação

Implementação candidata em **1.4.0-rc.18**, para Hytale **0.7.4**. O [roteiro da candidata](classroom-rc18-acceptance.md) distingue verificações no servidor de seleção, preview e interação que exigem cliente. O [fluxo principal da rc.12](evidence/classroom/2026-10-26-rc12-client.md) foi confirmado com professor e aluno; as [capturas rc.13](evidence/classroom/2026-10-26-rc13-client.md) confirmam a apresentação nessa configuração. O [polimento rc.14 foi aprovado por relato](evidence/classroom/2026-10-26-rc14-client.md); as novas interações de critérios/verificação rc.15 exigem aceite no cliente.

## Atividades

Depois de abrir `/classroom`, use **Atividades**. Selecionar uma atividade abre sua **consulta**, sem campos editáveis. O professor usa **Editar atividade**, ao lado do título, para abrir o formulário. **Voltar à visualização** encerra a edição. Ao sair com alterações, escolha **Salvar rascunho e sair**, **Descartar alterações** ou **Continuar editando**. O editor impede o fechamento nativo por Esc: use a navegação ou Fechar para passar pela confirmação. Desconectar encerra os campos temporários não salvos. Salvar rascunho mantém o editor; publicar retorna à consulta. Atividades arquivadas exigem restauração antes de editar. O professor consulta o rascunho salvo quando existir, com indicação de privacidade; alunos continuam vendo exclusivamente a publicação vigente.

O professor cria um rascunho privado, preenche título (até 128 caracteres), enunciado (até 8192), prazo e valor e salva. Rascunhos podem ficar incompletos. **Publicar** salva os campos exibidos e cria explicitamente uma versão imutável; os alunos só veem a última publicação não arquivada da própria turma.

O prazo usa seletores de **dia, mês, ano, hora e minuto**, no fuso **America/Sao_Paulo (Brasília)**. A data começa vazia e o horário em 23:59. Data totalmente vazia salva como ausente no rascunho; parcial exige completar ou limpar os três campos da data. Dias acompanham mês e ano: se a troca invalidar o dia, somente ele é limpo, com aviso. Anos vão do atual aos dez seguintes, incluindo o ano salvo fora dessa janela; horários cobrem 00:00–23:59 sem arredondamento. Publicar exige um instante futuro; o armazenamento usa UTC. Vencimento mantém a consulta e informa **Prazo encerrado**. **Envios iniciais e reenvios são aceitos depois do prazo, marcados como atrasados.** Esta decisão substitui a hipótese antiga de bloquear recebimento após o prazo.

O valor começa em **10**, deve ser positivo e admite até duas casas decimais, usando ponto ou vírgula. Não há arredondamento silencioso. Editar uma publicação prepara um rascunho; os alunos continuam vendo a publicação anterior até republicar. O seletor de filtro oferece todas, publicadas, rascunhos e arquivadas. Uma atividade publicada com alterações privadas também aparece entre rascunhos.

**Arquivar** oculta a atividade para alunos, preservando versões, entregas e rascunho para o professor. **Restaurar** reapresenta a última publicação, sem criar versão ou alterar prazo. Uma atividade sem publicação continua privada. Somente o responsável autorizado administra; ingresso pendente, outra turma e vínculo encerrado não concedem acesso de aluno.

**Nova atividade** abre um formulário temporário em largura total, sem criar registro no banco. O editor tem as seções **Dados**, **Componentes** e **Resultados**, que preservam os campos ao alternar. **Salvar rascunho** cria o registro e mantém o formulário amplo; **Publicar** grava e publica na mesma transação, retornando à consulta com lista. Falha na primeira publicação não deixa uma atividade vazia. Voltar oferece a confirmação descrita acima; descartar um formulário nunca salvo não cria registro. Rascunhos já salvos permanecem no banco. A edição comum e a consulta de alunos/entregas mantêm a lateral.

O resumo da turma informa **Atividades e rascunhos: N** para o professor (não arquivadas, incluindo rascunhos) e **Atividades disponíveis: N** para o aluno (somente publicadas e não arquivadas). Publicada com edição privada conta uma vez; vencidas continuam contando. A contagem atualiza após criar, publicar, arquivar e restaurar. Entregas e notas também atualizam os indicadores de acompanhamento; preview e verificação não invalidam o catálogo. Não há polling nem reenvio de mensagem do dia.

O aluno alterna entre **Enunciado e critérios** e **Minha entrega**, com nota e situação na lista. Uma seleção em andamento é identificada como nova área; ela não substitui a revisão já salva até confirmar o envio. Na correção docente, preview e avaliação usam colunas que quebram linha. O contexto da cópia temporária identifica atividade, aluno e revisão, e seu descarte pede confirmação.

## Entregar uma área

1. O aluno entra em **Atividades**, seleciona a atividade e usa **Entregar: selecionar área**.
2. A tela fecha e o inventário recebe a ferramenta **Seleção Classroom**. Equipe-a; clique com o botão esquerdo no primeiro canto e com o direito no segundo. Ambos os pontos são blocos inclusivos; caminhe até os cantos se necessário.
3. Abra `/classroom` novamente. A atividade continua associada à seleção e o rodapé informa largura × altura × profundidade.
4. Use **Entregar área selecionada** ou **Cancelar seleção**. Uma nova entrega gera uma revisão; não substitui a anterior.

A ferramenta não concede OP nem acesso aos comandos nativos de edição. O servidor exige a sessão, a conexão e o token do item; outro jogador não ganha autoridade ao receber esse item. Cancelamento, desconexão e saída do mundo encerram a seleção. Ela também expira após 15 minutos. A ferramenta temporária é removida ao concluir/cancelar ou sair do mundo; um item antigo sem sessão não seleciona nem altera blocos.

O servidor captura uma cópia fixa de blocos de construção inertes e componentes LogicGates, com nomes estáveis dos assets, orientação e intenção das alavancas. Sinais derivados são recalculados na cópia. Inventários, NPCs e itens soltos não são copiados. Tipos com estado especial não reproduzível, fluidos, estruturas de várias células/fillers, plantações, blocos que caem ou procedimentos de tick são recusados; o erro identifica o tipo/posição quando detectado na captura. Não se aceita silenciosamente uma cópia parcial. Seções precisam estar carregadas; a operação não gera terreno para completar a área.

A seleção identifica quem enviou, sem comprovar autoria da construção. Bancadas permanentes/proteção de autoria dos alunos continuam fora desta etapa. Se a atividade for republicada, arquivada, o aluno sair ou a turma encerrar antes do commit, a entrega antiga em andamento é recusada. Confirme novamente o contexto e reinicie a seleção.

## Consultar e corrigir

O professor usa **Atividades → atividade → Consultar alunos e entregas → aluno**. A lista inclui alunos atuais e participantes históricos da turma, com indicação de vínculo. Detalhes identificam aluno, matrícula, revisão, publicação, instante em Brasília, dimensões e atraso. Listas de atividades, alunos e revisões são paginadas em 20 registros, sem ler os arquivos de todas as entregas.

O **preview 3D nativo** é enviado somente para a revisão selecionada; permite girar, inclinar, aproximar e mover a visualização. É uma consulta visual. Para acionar o circuito, use **Colar para corrigir**: a ferramenta permite escolher um bloco de apoio no mundo atual, e a cópia será colada acima dele, a partir daquele canto. Botão direito cancela. Todo o volume precisa estar vazio, sem fluido, carregado e autorizado pelos eventos nativos de colocação. As construções existentes não são substituídas.

Há uma cópia por professor. O volume reservado bloqueia colocação, quebra, dano e uso dos demais jogadores pelos eventos nativos; a interação própria das alavancas também verifica a reserva. O responsável pode editar sua montagem com os tipos suportados pela captura; blocos de várias células não podem atravessar a reserva. **Descartar cópia temporária** remove todos os blocos naquele volume, inclusive alterações, sem modificar o arquivo entregue. Não use comandos administrativos/bulk de edição ou APIs que contornem eventos nativos dentro dessas reservas. Proteções externas precisam emitir/observar os eventos normais de colocação/quebra; a compatibilidade concreta deve ser testada com o plugin utilizado pelo servidor.

A reserva é persistida antes de alterar blocos. Interrupções conservam o registro; após reinício, desconexão ou perda de autorização, a limpeza usa somente esse volume, originalmente vazio, quando suas seções estiverem naturalmente carregadas. Não carrega terreno distante e não apaga posições de uma reserva já liberada. Uma região ainda indisponível continua reservada até permitir limpeza. Não se pode criar outra cópia enquanto a anterior estiver registrada.

Cada avaliação identifica explicitamente a revisão. Nota entre **zero e o valor daquela publicação**, até duas casas; feedback opcional de até 4096 caracteres. Antes da avaliação, aparece `—/10`, por exemplo; zero atribuído aparece `0/10`. Reenvio começa sem avaliação, e avaliar uma revisão antiga nunca preenche a mais recente. Correções de nota preservam o histórico no banco; a interface exibe a última avaliação de cada revisão. Versões anteriores do enunciado são apresentadas quando a entrega se refere a uma publicação anterior.

## Critérios e verificação assistida — rc.15

O professor define quantidades exatas das sete portas, alavancas, cabos e lâmpadas: vazio deixa livre; zero proíbe. Os critérios são privados no rascunho e imutáveis em cada publicação. Na seção **Resultados**, pode ativar a tabela visual: escolha de uma a seis entradas e de uma a quatro saídas. As combinações são geradas; para cada saída, selecione **0**, **1** ou **—** (ainda não definida). São oito combinações por página. Alavancas I e lâmpadas O seguem as posições locais X/Y/Z. O exemplo AND e a ajuda explicam o preenchimento. Alterar dimensões, substituir respostas pelo exemplo ou remover uma tabela preenchida exige confirmação. O **modo texto avançado** continua aceitando `entradas -> saídas`, uma combinação por linha; texto antigo inválido permanece nesse modo até correção ou substituição confirmada. Até seis entradas, quatro saídas e 64 combinações.

Em uma entrega selecionada, **Verificar circuito** confere quantidades e simula a cópia fixa no avaliador existente, fora do contexto do mundo e pela fila pesada limitada. Não altera nota, feedback ou blocos; divergência não impede o envio. Circuito equivalente pode passar no comportamento sem satisfazer restrições de módulos. Quantidade de I/O incompatível não executa a tabela; I/O sem conexão física e realimentação não resolvida são inconclusivos. Somente componentes dentro da área entregue participam. A análise comportamental admite até 2048 componentes lógicos; volumes maiores continuam disponíveis para correção manual.

O relatório pertence à revisão selecionada e não é persistido; verificação é explícita, sem varredura periódica ou atualização de outras telas. Nota e feedback continuam manuais. Cada revisão usa os critérios da publicação entregue mesmo após republicação. A [validação rc.15](classroom-rc15-acceptance.md) explica configuração, mapeamento e limites.

### Exemplo: duas alavancas e uma lâmpada AND

As quantidades verificam as peças usadas; a tabela verifica se foram conectadas para produzir o comportamento pedido. Para exigir uma porta AND, duas alavancas e uma lâmpada, preencha essas quantidades e deixe cabos livres. Se a lâmpada só pode acender quando ambas as alavancas estão ligadas, use uma linha por combinação:

```text
00 -> 0
01 -> 0
10 -> 0
11 -> 1
```

`0` significa desligado/apagado e `1`, ligado/aceso. Antes de `->` ficam as entradas, depois ficam as saídas. Em `10 -> 0`, a primeira alavanca está ligada, a segunda desligada e a lâmpada deve ficar apagada. O primeiro dígito corresponde a I1/O1; o relatório identifica as posições locais de cada entrada/saída. Essa ordem é definida por X, depois Y, depois Z, não pela ordem de colocação: descreva a disposição exigida no enunciado quando trocar as entradas/saídas mudar o resultado.

Tabela desativada/vazia verifica somente quantidades. Respostas `?` são preservadas em rascunhos, mas publicar exige todas as saídas definidas com 0 ou 1. Quando preenchida, precisa cobrir todas as combinações: uma alavanca tem duas, duas têm quatro e três têm oito. Um circuito pode passar nas quantidades e falhar no comportamento, ou vice-versa. O professor consulta o relatório e decide a nota no campo de avaliação manual.

## Limites e configuração

Arquivo opcional `learning.properties`, ao lado de `classroom.db` no diretório de dados do plugin; alterar com o servidor parado. A ausência usa os padrões. Configuração inválida bloqueia Classroom sem interromper os circuitos.

```properties
maxWidth=32
maxHeight=16
maxDepth=32
maxContentBytes=8388608
maxClassroomBytes=1073741824
queueCapacity=8
selectionMillis=900000
```

As dimensões incluem ar: máximo padrão **16.384 posições**. Limite de conteúdo **8 MiB descomprimidos por revisão**, cota inicial **1 GiB por turma** sobre arquivos comprimidos referenciados. Cota cheia recusa apenas a nova revisão; nenhuma entrega existente é eliminada. Aumente com cautela; o parser admite no máximo 64 por dimensão, 32 MiB por arquivo e fila 32. Reduzir limites de dimensão/conteúdo também pode impedir a abertura de conteúdos maiores antigos; preserve configuração/backup compatíveis.

Uma fila separada executa uma operação pesada por vez e admite oito aguardando; saturação retorna ocupado. Cada conta tem no máximo uma operação pesada em andamento, inclusive preview. Seleção não inicia captura; apenas a confirmação faz isso. Arquivos/compressão/checksum e montagem do pacote de preview ficam fora do contexto do mundo. Leitura/alteração de blocos usa o contexto do Hytale. Cópias coladas participam da simulação elétrica enquanto existirem: a fila não limita a complexidade de todos os circuitos já colocados no servidor. Os limites não garantem capacidade de jogadores.

## Persistência e recuperação

SQLite **v6**, com backup consistente automático antes da migração. A v6 acrescenta critérios (`draft_rules` e `rules`) ao rascunho/publicação; dados v5 recebem critérios livres, conservando versões, conteúdo, reservas e notas. As tabelas pedagógicas introduzidas na v5 guardam atividades/versões, metadados de revisões, avaliações e reservas; IDs e regras administrativas anteriores permanecem. Conteúdo privado em `submissions/<UUID>.json.gz`, com formato versionado e SHA-256 dos bytes comprimidos. A gravação força o arquivo temporário em disco, publica por movimento atômico e só depois confirma os metadados em transação. Leitura verifica checksum e limite de expansão.

Falha anterior ao commit não oferece a revisão ao professor. No início, a recuperação remove apenas arquivos próprios reconhecidos sem referência e temporários, preservando arquivos referenciados, nomes desconhecidos e diretórios. Falha incerta durante encerramento conserva o arquivo para resolver a referência na próxima inicialização. Ausência/corrupção de conteúdo é erro explícito, sem substituir a entrega por uma área vazia.

Faça backup **do banco e da pasta `submissions` juntos**, com servidor parado, e conserve a configuração. O backup automático pré-migração conserva o banco anterior; para voltar à rc.10, restaure esse banco v4. Não abra o banco v6 em versões antigas; para voltar à rc.14, restaure o banco v5 do backup. Mundos/circuitos não são migrados por esta entrega.


Pontuação automática, proteção permanente das bancadas, logging ampliado e publicação estável permanecem posteriores.
