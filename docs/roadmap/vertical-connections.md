# Conexões entre alturas — avaliação de viabilidade

Avaliação em **22/10/2026**, sobre Hytale 0.7.4. **Viável**, com impacto moderado na topologia e nos modelos dos cabos. Esta funcionalidade ainda não foi implementada; o JAR desta entrega mantém a regra horizontal atual.

## Comportamento recomendado para o primeiro incremento

Permitir que **cabos subam e desçam um degrau de um bloco**, mantendo alavancas, lâmpadas e portas com suas faces laterais atuais. Para alimentar um módulo em outra altura, levar o cabo até o nível dele. Essa regra atende ao uso de escadas inspirado na redstone e mantém a leitura das entradas das portas simples.

Exemplo em vista lateral:

| Altura | X=0 | X=1 | X=2 |
| --- | --- | --- | --- |
| Y=1 | Passagem livre | Cabo alto | Cabo alto |
| Y=0 | Cabo baixo | Bloco de apoio | Bloco de apoio |
| Y=−1 | Bloco de apoio | Chão | Chão |

O cabo baixo se conecta ao alto, na posição adjacente e um nível acima. O sinal deve passar nos dois sentidos. A parte visível da subida acompanha a lateral do apoio. O bloco de apoio não se torna um condutor.

Regras propostas:

- Uma ligação percorre uma direção horizontal e varia Y em 0, +1 ou −1. Não há salto de duas alturas, conexão entre cantos X/Z ou conexão automática de uma coluna na mesma coordenada X/Z.
- O cabo alto exige um apoio de bloco completo e o caminho entre os níveis deve estar livre. Colocar um obstáculo no espaço acima do cabo baixo interrompe a subida. Apoio e bloqueio são verificados nos dois sentidos da mesma ligação.
- Começar com apoios de colisão de cubo completo e face superior de suporte `Full`. Lajes, escadas e outras formas parciais ficam para outra etapa; transparência visual sozinha não define apoio ou bloqueio.
- Cabos e componentes já conectados no mesmo nível continuam funcionando. Portas mantêm uma entrada física por face; não passam a ler automaticamente todos os blocos acima e abaixo daquela face.
- Não adicionar condução por blocos sólidos, atrasos, atenuação, relógios ou novas regras de realimentação neste incremento.

É uma proposta para o LogicGates, sem compromisso de reproduzir todas as exceções da redstone de outras edições do Minecraft.

## O que o código atual já oferece

`BlockPosition` já guarda X, Y e Z, e a simulação trabalha com grafos e redes. A propagação, a paridade por entrada e a avaliação de ciclos podem ser reaproveitadas. Não é necessário reescrever as sete portas.

A limitação horizontal está em pontos explícitos: `neighbor(HorizontalDirection)` mantém Y; descoberta de redes, ligação de produtores, leitura de entradas e iluminação consultam esses vizinhos diretamente. As máscaras visuais de quatro bits só indicam presença lateral, não a altura da ligação.

A API 0.7.4 instalada expõe consulta de bloco/seção carregada, rotação, suporte e colisão: `BlockType.getSupporting(int)`, `getSupport(int)`, `isFullySupportive()`, `getHitboxTypeIndex()` e as variantes de `BlockBoundingBoxes`. A estrutura de suporte e hitboxes está descrita nas referências de [blocos](https://docs.hytale.com/assets/item/block/blocks/) e [hitboxes](https://docs.hytale.com/assets/item/block/hitboxes/). A seleção precisa dos casos permitidos deverá ser comprovada em integração, com a rotação real dos blocos.

## Mudanças necessárias

1. **Snapshot de topologia:** além dos nós lógicos, coletar disponibilidade, ocupação, suporte e colisão das posições relevantes de apoio/passagem. O avaliador deve receber esses fatos; não consultar o mundo diretamente.
2. **Vizinhança compartilhada:** resolver as ligações válidas uma única vez e reutilizá-las na formação de redes e na apresentação. Acrescentar apenas consultas com Y±1 em uma parte do código criaria diferenças entre o cabo desenhado e a rede elétrica.
3. **Estados por face:** representar ausência, conexão plana, subida ou descida. Com uma escolha por face, o teto teórico é `4⁴ = 256` combinações de cabos, antes de descartar configurações impossíveis. Os 16 estados planos existentes devem conservar identificadores e compatibilidade.
4. **Geometria e colisão:** gerar a subida e os encaixes com seleção compatível. Distribuir as peças entre os blocos participantes, mantendo as hitboxes dentro de suas células. Evitar limites negativos ou maiores que um bloco que possam criar fillers ou interferir nos vizinhos; isso exige um protótipo visual e físico no cliente.
5. **Invalidação:** observar também alterações em blocos comuns de apoio e passagem. Hoje as mudanças elétricas acompanham componentes lógicos e seções; um teto colocado acima do cabo também precisaria invalidar a ligação. Uma assinatura local por cabo pode seguir o padrão já usado pelas portas, medindo seu custo antes de expandir.
6. **Seções:** consultar somente dados carregados. Se apoio, passagem ou destino estiver indisponível, a ligação fica suspensa; indisponível não equivale a ar. O retorno de uma seção vazia também precisa invalidar a topologia. A indisponibilidade distante continua interrompendo redes, conforme o comportamento atual; não muda por si só a regra das entradas das portas.

A primeira verificação técnica recomendada é um protótipo com dois cabos e um degrau: transmissão bidirecional, modelo da subida e seleção sem bloquear a passagem ou afetar o bloco de apoio. Só depois ampliar para ramificações e todas as máscaras.

## Critérios para validar a implementação futura

- Subida e descida em quatro orientações, ramificações em diferentes níveis e encadeamento com as sete portas.
- Bloqueio por teto, falta/remoção de apoio e retorno da conexão após remover o obstáculo.
- Ausência de conexões por cantos, saltos maiores ou componentes empilhados sem trajeto válido.
- Paridade preservada: redes conectadas por degraus ainda contam por face na entrada de XOR/XNOR.
- Uso da mesma ligação no visual e na simulação; seleção da subida e remoção com o drop correto.
- Seções de destino, apoio e passagem indisponíveis; retorno de seção vazia; reinício do servidor.
- Preservação dos circuitos planos, sinais derivados reconstruídos e ausência de carregamento forçado.
- Medição com muitos cabos, comparação do custo em repouso e após mudanças e teste com dois jogadores.

**Recomendação:** implementar os degraus primeiro. Conduítes verticais contínuos, ligação direta entre módulos em alturas diferentes e apoio em formas parciais ficam como extensões separadas, pois exigem novas regras visuais e de portas.
