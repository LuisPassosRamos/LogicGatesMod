# Índice de fontes externas

**Consulta original:** 16/10/2026. Consolidado em 23/10/2026; as versões de páginas citadas são históricas. O projeto atual usa 0.7.4. Links consultados nesta pesquisa. Os documentos são sínteses originais; não reproduzem integralmente a documentação. As fontes podem ser atualizadas depois desta data.

## Fontes oficiais

“Oficial hospedada na wiki” significa conteúdo atribuído à Hypixel Studios pela seção correspondente. Os demais guias do site são classificados separadamente.

| ID | Fonte e link | Origem / uso nesta síntese |
| --- | --- | --- |
| O01 | [Hytale Documentation](https://docs.hytale.com/) | Portal oficial; versão exibida e pontos de entrada |
| O02 | [Hytale Server API](https://docs.hytale.com/api/) | Oficial; conferir a versão exibida, que era 0.7.2 na consulta original |
| O03 | [Asset Reference](https://docs.hytale.com/assets/) | Oficial; formatos gerados dos codecs |
| O04 | [Official Documentation — Introduction](https://hytalemodding.dev/en/docs/official-documentation) | Declara a procedência do material oficial hospedado |
| O05 | [Hytale New Worlds Modding Contest](https://hytale.com/news/2026/3/hytale-new-worlds-modding-contest) | Hytale identifica HytaleModding como recurso comunitário |
| O06 | [Modder-Facing Patch Notes — Update 7](https://hytale.com/news/2026/10/modder-facing-patch-notes-update-7) | Oficial, 12/10/2026; migrações de blocos, seções e UI |
| O07 | [JavaPlugin](https://docs.hytale.com/api/com/hypixel/hytale/server/core/plugin/JavaPlugin) | Oficial; classe e inicialização |
| O08 | [PluginBase](https://docs.hytale.com/api/com/hypixel/hytale/server/core/plugin/PluginBase) | Oficial; registros e serviços acessíveis pelo plugin |
| O09 | [ECS — component package](https://docs.hytale.com/api/com/hypixel/hytale/component/package-summary) | Oficial; tipos fundamentais do ECS |
| O10 | [World storage package](https://docs.hytale.com/api/com/hypixel/hytale/server/core/universe/world/storage/package-summary) | Oficial; EntityStore e ChunkStore |
| O11 | [Chunk section package](https://docs.hytale.com/api/com/hypixel/hytale/server/core/universe/world/chunk/section/package-summary) | Oficial; componentes por seção |
| O12 | [World accessor package](https://docs.hytale.com/api/com/hypixel/hytale/server/core/universe/world/accessor/package-summary) | Oficial; BlockReader e SectionReader |
| O13 | [Items](https://docs.hytale.com/assets/item/items/) | Oficial; item, referências, BlockType e interações; consulta de seções relevantes |
| O14 | [Blocks](https://docs.hytale.com/assets/item/block/blocks/) | Oficial; apresentação, estados, luz e BlockEntity; consulta de seções relevantes |
| O15 | [Custom UI](https://hytalemodding.dev/en/docs/official-documentation/custom-ui) | Oficial hospedada na wiki; arquitetura, páginas e HUD |
| O16 | [Common Styling](https://hytalemodding.dev/en/docs/official-documentation/custom-ui/common-styling) | Oficial hospedada; estilos e galeria |
| O17 | [Layout](https://hytalemodding.dev/en/docs/official-documentation/custom-ui/layout) | Oficial hospedada; fundamentos de posicionamento |
| O18 | [Markup](https://hytalemodding.dev/en/docs/official-documentation/custom-ui/markup) | Oficial hospedada; estrutura da linguagem UI |
| O19 | [Hytale Modding Strategy and Status](https://hytale.com/news/2025/11/hytale-modding-strategy-and-status) | Oficial, 20/11/2025; contexto histórico e categorias de modding |
| O20 | [Custom UI — Type Documentation](https://hytalemodding.dev/en/docs/official-documentation/custom-ui/type-documentation) | Oficial hospedada; índice dos controles, sem levantamento de todas as propriedades |
| O21 | [NPC Meta](https://hytalemodding.dev/en/docs/official-documentation/npc-doc) | Oficial hospedada; autoria e sumário consultados; catálogo completo não compilado |
| O22 | [Hytale Generator](https://docs.hytale.com/hytale-generator/) | Oficial; índice de categorias de nós, sem compilar cada nó |
| O23 | [BlockOperations](https://docs.hytale.com/api/com/hypixel/hytale/server/core/universe/world/chunk/BlockOperations) | Oficial; operações atuais de alteração de blocos |

## Guias e referências comunitários

São fontes primárias dos respectivos tutoriais do HytaleModding; não constituem garantia oficial de estabilidade da API.

| ID | Página | Uso e observação |
| --- | --- | --- |
| C01 | [Wiki](https://hytalemodding.dev/en/docs/wiki) | Página aberta inicialmente; criação de wiki para um mod |
| C02 | [Introduction](https://hytalemodding.dev/en/docs) | Entrada geral de modding |
| C03 | [Quick Start](https://hytalemodding.dev/en/docs/quick-start) | Mapa dos guias iniciais |
| C04 | [Setting Up Your Development Environment](https://hytalemodding.dev/en/docs/guides/plugin/setting-up-env) | Java, template e dependências |
| C05 | [Build and Test Your Mod](https://hytalemodding.dev/en/docs/guides/plugin/build-and-test) | Build, JAR, instalação e runServer |
| C06 | [Logging](https://hytalemodding.dev/en/docs/guides/plugin/logging) | Logger e diagnóstico |
| C07 | [Creating custom blocks](https://hytalemodding.dev/en/docs/guides/plugin/creating-block) | Asset pack e exemplo de bloco |
| C08 | [Create Custom Item and Interaction](https://hytalemodding.dev/en/docs/guides/plugin/item-interaction) | Interação Java, codec e ligação a assets |
| C09 | [Hytale ECS Theory](https://hytalemodding.dev/en/docs/guides/ecs/hytale-ecs-theory) | Stores, referências e codecs |
| C10 | [Systems](https://hytalemodding.dev/en/docs/guides/ecs/systems) | Sistemas, consultas e ordenação |
| C11 | [Block Components](https://hytalemodding.dev/en/docs/guides/ecs/block-components) | Fluxo de bloco com componente; exemplo contém APIs antigas |
| C12 | [Creating Commands](https://hytalemodding.dev/en/docs/guides/plugin/creating-commands) | Tipos de comando e contexto de thread |
| C13 | [Creating Events Listeners](https://hytalemodding.dev/en/docs/guides/plugin/creating-events) | Eventos tradicionais versus ECS |
| C14 | [Events](https://hytalemodding.dev/en/docs/server/events) | Índice de eventos e marcações de obsolescência |
| C15 | [Storing Persistent Data on the Player](https://hytalemodding.dev/en/docs/guides/plugin/store-persistent-data) | Persistência por componente; exemplos precisam de revisão |
| C16 | [Creating a Configuration File](https://hytalemodding.dev/en/docs/guides/plugin/creating-configuration-file) | Config e codecs |
| C17 | [Interactions](https://hytalemodding.dev/en/docs/server/interaction-reference) | Consulta do modelo e encadeamento; não é exportação integral do catálogo |
| C18 | [Permission Management](https://hytalemodding.dev/en/docs/guides/plugin/permission-management) | Introdução, nós, grupos e módulo de permissões |
| C19 | [Instance System](https://hytalemodding.dev/en/docs/guides/plugin/instances) | Templates, carregamento e transporte |
| C20 | [Browsing the Hytale Server Code](https://hytalemodding.dev/en/docs/guides/plugin/browsing-serverjar) | Rota de acesso ao código-fonte; conteúdo privado não acessado |

## Acesso ao código-fonte

O guia C20 informa que o acesso ao Shared Source passa por [accounts.hytale.com/shared-source](https://accounts.hytale.com/shared-source) e requer conta com licença ativa. Nesta tarefa não houve login, acesso ao repositório privado ou auditoria do código do servidor. Para conferir implementação, use acesso licenciado e a revisão correspondente à dependência do projeto.

## Cobertura e limites

O pacote prioriza o necessário para programar um mod de circuitos e sua camada educacional: ambiente, plugins, assets, ECS, interações, eventos, persistência e UI. Não inclui o catálogo completo de NPCs, sons, entidades, packets, worldgen, controles UI ou todos os guias de publicação.

As chamadas de API citadas são pontos de navegação confirmados nas páginas consultadas. Não foi construído um projeto de demonstração para comprovar todas as combinações de API. O fluxo atual está em [desenvolvimento](../development.md) e [validação](../validation.md).

## Atualização deste contexto

Quando mudar de versão: registrar a nova dependência, reler as notas de migração, revisar as classes e schemas utilizados e executar os testes de integração afetados. Atualizar primeiro o estado real do projeto; depois atualizar o briefing. Uma fonte não fica mais confiável apenas porque foi citada neste arquivo.

## Fontes de autoria e UI

- [Plugin Hytale do Blockbench](https://github.com/JannisX11/hytale-blockbench-plugin): formato e ferramenta de edição dos modelos.
- [Hitboxes](https://docs.hytale.com/assets/item/block/hitboxes/): esquema de seleção/colisão; conferir codecs 0.7.4.
- [InteractiveCustomUIPage, canal pre-release](https://pre-release.docs.hytale.com/api/com/hypixel/hytale/server/core/entity/entities/player/pages/InteractiveCustomUIPage): navegação para futura UI; conferir assinaturas na dependência local.
- [PlayerRef, canal pre-release](https://pre-release.docs.hytale.com/api/com/hypixel/hytale/server/core/universe/PlayerRef): identidade/conexão e abertura de páginas.

Fontes externas não são dependências de build. Não manter cópias integrais desses sites no repositório; registrar aqui o link, o contexto e as divergências relevantes.
