# Classroom — atividades, entregas de áreas e avaliação

Implementação candidata em **1.4.0-rc.12**, para Hytale **0.7.4**. O [roteiro da candidata](classroom-rc12-acceptance.md) distingue verificações no servidor de seleção, preview e interação que exigem cliente. As evidências administrativas da rc.10 continuam históricas; não aprovam estas telas novas.

## Atividades

Depois de abrir `/classroom`, use **Atividades**. O professor cria um rascunho privado, preenche título (até 128 caracteres), enunciado (até 8192), prazo e valor e salva. Rascunhos podem ficar incompletos. **Publicar** salva os campos exibidos e cria explicitamente uma versão imutável; os alunos só veem a última publicação não arquivada da própria turma.

O prazo usa `AAAA-MM-DD HH:MM`, no fuso **America/Sao_Paulo (Brasília)**. Publicar exige um instante futuro; o armazenamento usa UTC. Vencimento mantém a consulta e informa **Prazo encerrado**. **Envios iniciais e reenvios são aceitos depois do prazo, marcados como atrasados.** Esta decisão substitui a hipótese antiga de bloquear recebimento após o prazo.

O valor começa em **10**, deve ser positivo e admite até duas casas decimais, usando ponto ou vírgula. Não há arredondamento silencioso. Editar uma publicação prepara um rascunho; os alunos continuam vendo a publicação anterior até republicar. O filtro percorre todas, publicadas, rascunhos e arquivadas. Uma atividade publicada com alterações privadas também aparece entre rascunhos.

**Arquivar** oculta a atividade para alunos, preservando versões, entregas e rascunho para o professor. **Restaurar** reapresenta a última publicação, sem criar versão ou alterar prazo. Uma atividade sem publicação continua privada. Somente o responsável autorizado administra; ingresso pendente, outra turma e vínculo encerrado não concedem acesso de aluno.

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

SQLite **v5**, com backup consistente automático antes da migração. As tabelas novas guardam atividades/versões, metadados de revisões, avaliações e reservas; IDs e regras administrativas anteriores permanecem. Conteúdo privado em `submissions/<UUID>.json.gz`, com formato versionado e SHA-256 dos bytes comprimidos. A gravação força o arquivo temporário em disco, publica por movimento atômico e só depois confirma os metadados em transação. Leitura verifica checksum e limite de expansão.

Falha anterior ao commit não oferece a revisão ao professor. No início, a recuperação remove apenas arquivos próprios reconhecidos sem referência e temporários, preservando arquivos referenciados, nomes desconhecidos e diretórios. Falha incerta durante encerramento conserva o arquivo para resolver a referência na próxima inicialização. Ausência/corrupção de conteúdo é erro explícito, sem substituir a entrega por uma área vazia.

Faça backup **do banco e da pasta `submissions` juntos**, com servidor parado, e conserve a configuração. O backup automático pré-migração conserva o banco anterior; para voltar à rc.10, restaure esse banco v4. Não abra o banco v5 em versões antigas. Mundos/circuitos não são migrados por esta entrega.

Avaliação automática, proteção permanente das bancadas, logging ampliado e publicação estável permanecem posteriores.
