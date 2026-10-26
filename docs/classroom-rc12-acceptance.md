# Classroom — aceite da 1.4.0-rc.12

**Cliente: fluxo principal confirmado em multiplayer**, por [capturas e relatos discriminados](evidence/classroom/2026-10-26-rc12-client.md). Abertura, atividade, entrega sem OP, preview, cópia, edição/proteção, revisões, nota e limpeza ao retornar ao mundo foram confirmados no escopo descrito. Resoluções/escalas e casos complementares permanecem pendentes. Esta candidata corrige a [desconexão da rc.11](evidence/classroom/2026-10-26-rc11-client-blocker.md) causada por `.Disabled` em campos de texto. A correção não muda o esquema v5 nem o conteúdo já salvo. As verificações automatizadas não aprovam renderização no cliente.

Com o mundo parado, substitua o JAR anterior por `LogicGates-1.4.0-rc.12.jar` em `mods`, mantendo apenas uma versão do mod. Guarde o ZIP fora de `mods`. Preserve o banco e a pasta `submissions`; não apague dados para corrigir este erro.

## Primeiro: desbloquear a interface

1. Professor: conectar, abrir `/classroom` → Atividades, aguardar a lista e trocar de tela/reabrir. Esperado: permanece conectado, inclusive se a lista estiver vazia.
2. Criar atividade, preencher e salvar rascunho. Testar título/enunciado, prazo futuro de Brasília e valor 10; publicar. Esperado: campos e botões funcionam sem desconexão.
3. Tentar publicar com dados inválidos e depois corrigir. Esperado: mensagem de validação, campos preservados e possibilidade de tentar novamente.
4. Arquivar e restaurar; conferir que o aluno vê apenas a publicação disponível. Abrir Atividades com a conta de aluno e selecionar a publicação. Esperado: consulta sem erro de Custom UI.
5. Havendo uma entrega: professor seleciona aluno/revisão, salva nota e feedback; tenta nota acima do valor e corrige. Esperado: resultado/erro sem desconexão e sem perder os campos ao receber atualização comum.

As etapas de gravação e erro são necessárias porque a propriedade incompatível também era enviada nesses caminhos. Registre build ID/SHA-256 e a primeira etapa que falhar; uma captura da mensagem exata basta para localizar o comando recusado.

## Depois: entrega e correção

Retomar a [prova curta e os casos complementares](classroom-rc11-acceptance.md#primeiro-prova-curta-com-professor-e-aluno): seleção sem OP, preview, cópia editável, proteção, descarte, revisões, notas e reinício. A massa isolada de paginação e as regras de backup continuam as mesmas. Os resultados administrativos históricos não aprovam essas novas interações.

Sem aceite estável, tag, publicação ou merge automático em `main`. Logging ampliado e avaliação automática continuam posteriores.
