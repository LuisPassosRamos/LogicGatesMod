# Classroom — aceite da 1.4.0-rc.15

Candidata de critérios e verificação assistida sobre Hytale 0.7.4. Build ID, checksum e execução automatizada serão registrados após a revisão final. A [rc.14 foi aprovada por relato](evidence/classroom/2026-10-26-rc14-client.md); isso não comprova as novas interações da rc.15.

## Instalação e dados

Com servidor/mundo parado, instale somente `LogicGates-1.4.0-rc.15.jar` em `mods`, substituindo o JAR anterior. Guarde o ZIP fora dessa pasta. Preserve banco, `submissions`, configuração e mundos.

SQLite passa de v5 a **v6** com backup consistente pré-migração em `backups` e duas colunas de critérios. Atividades, versões, entregas, notas e reservas anteriores são preservadas, com critérios livres. Para voltar à rc.14, restaure o banco v5 compatível; conserve o conjunto banco/arquivos privados. Não abra o banco v6 no mod antigo.

## Configurar uma atividade

1. Professor cria/edita atividade. Abaixo do valor, há quantidades para AND, OR, NOT, NAND, NOR, XOR, XNOR, alavancas, cabos e lâmpadas. Quantidade preenchida é **exata**; vazio deixa livre; zero proíbe. Inclui componentes desconectados na área entregue. Blocos de construção comuns não entram nessas contagens.
2. Para um exemplo AND, escolha AND=1, alavancas=2, lâmpadas=1; deixe cabos livres. Opcionalmente preencha zero para as outras portas. Informe no enunciado a identificação das entradas e saídas.
3. Configure a tabela com **uma combinação por linha**:

```text
00 -> 0
01 -> 0
10 -> 0
11 -> 1
```

Cada bit de entrada é uma alavanca (I1, I2...), cada bit de saída é uma lâmpada (O1, O2...). A ordem é pelas posições **locais da área selecionada: X, depois Y, depois Z**, em ordem crescente; o primeiro bit corresponde a I1/O1. As coordenadas aparecem no relatório do professor e não são coordenadas absolutas do mundo. Posicionar os componentes conforme o enunciado faz parte do contrato deste incremento; não há seleção livre de portas individuais ou alteração automática dessa ordem.

4. Tabela vazia verifica somente quantidades. Rascunhos permitem tabela incompleta. Publicar exige todas as combinações, sem duplicatas, com larguras consistentes: 1–6 entradas e 1–4 saídas. Se as quantidades de alavancas/lâmpadas estiverem definidas, devem coincidir. Publicação inválida reverte o salvamento e mantém os campos em edição para corrigir e tentar novamente.
5. Reabra para conferir persistência dos números, zero, campos livres, tabela e prazo. Aluno vê somente os critérios publicados; edição privada não altera a publicação vigente.

## Verificar uma entrega

- Aluno sem OP entrega pelo fluxo normal. Quantidades/comportamento divergentes **não bloqueiam a entrega**.
- Professor consulta atividade → alunos/entregas → aluno → revisão e clica **Verificar circuito**. Relatório identifica revisão/publicação, quantidades encontradas/exigidas, mapeamento I/O e cada caso esperado/encontrado.
- Conferir uma entrega correta e uma com erro lógico; incluir uma porta extra desconectada e uma porta proibida por zero. Quantidades e comportamento são critérios independentes; circuito equivalente passa no comportamento e ainda precisa atender às quantidades definidas.
- Conferir quantidade de alavancas/lâmpadas incompatível com a tabela: comportamento não executado, sem inventar entradas/saídas. I/O sem ligação física e realimentação não resolvida são inconclusivos, não nota zero. Conexões externas à área não fazem parte da cópia avaliada.
- Verificação não altera nota, feedback em edição, intenção salva das alavancas, construção do aluno ou cópia temporária do professor. O resultado é consulta sob demanda, mantido na página enquanto a mesma revisão estiver selecionada; não há histórico persistido de verificações nem execução automática a cada atualização.
- Salvar nota continua explícito. Republicar com outras regras e verificar revisão antiga continua usando as regras da publicação entregue. Reenvio continua sem nota própria.
- Trocar revisão não reaproveita o relatório anterior. Revogar permissão durante a análise não permite exibir resultado desautorizado; restaurar requer novo contexto autorizado.

## Limites e apresentação

Análise lógica: no máximo **2048 componentes lógicos**, **6 alavancas**, **4 lâmpadas**, **64 combinações**. Construções maiores continuam entregáveis nos limites da área e corrigíveis manualmente; erro de limite não apaga entregas. Contagem sem tabela não executa simulação. Operação pesada utiliza a fila já existente: uma em execução, oito aguardando e uma por conta; pode responder ocupado se coincidir com preview/captura/colagem. Não há verificações periódicas, novos ticks ou consultas extras na troca de campos.

Conferir campos/rótulos/ações/rolagem em pt-BR/en-US e resolução/escala usadas; pelo menos 1280×720 e 1920×1080 quando disponíveis. Conferir relatório com 64 casos sem cortes. Fazer regressão curta de seleção/preview/colagem/descarte/nota em duas contas e reabertura dos dados após reinício.

Automação e integração nativa não validam a renderização do cliente. Tag, publicação estável, logging ampliado, pontuação automática, identificação personalizada de I/O e proteção permanente de bancadas permanecem etapas posteriores.
