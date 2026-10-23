# Medições de desempenho

## Como reproduzir

```powershell
.\gradlew.bat benchmarkCircuit
```

O relatório fica em `build/reports/benchmarks/circuit.json`, com ID da compilação, Java, sistema, processadores disponíveis, heap máximo, aquecimento, amostras, mediana, p95 e bytes alocados por avaliação (quando suportado pela JVM).

São 12 cenários: linhas e grades de cabos, cadeias das sete portas e redes independentes, em 100, 1.000 e 10.000 componentes principais. As fontes/consumidores podem acrescentar um ou dois nós ao total. Cada cenário aquece 10 vezes e mede 25 avaliações completas. Não há limiar de aprovação de desempenho no CI, pois máquinas e carga variam.

O benchmark mede o avaliador Java e reconstrói o grafo em cada amostra, como uma avaliação real. Exclui descoberta de seções/holders, publicação no Hytale, física, renderização e jogadores. As entradas permanecem constantes durante cada série; tabelas, alterações de topologia e desligamento continuam cobertos pelos testes funcionais.

## No servidor

`/logicenergy` informa os tempos da avaliação completa, do avaliador e do monitoramento de mudanças, com janela de até 256 amostras por mundo. Média, p95, máximo e última medição pertencem à janela atual; não são estatísticas persistidas. Consultar métricas não grava dados no mundo.

Os JSONs nativos por fase incluem uma amostra dos tempos do pequeno universo de integração. Eles demonstram a instrumentação, não a capacidade de uma turma. Para investigar custo em repouso, deixar um circuito carregado sem alterações e observar o monitoramento; depois alternar fontes/remover cabos e comparar as avaliações.

## Decisão de otimização

Ainda se monitora o conjunto carregado por tick e a invalidação pode reconstruir todos os circuitos carregados do mundo. Uma alteração localizada não implica custo somente local. O próximo experimento deve variar tamanho da rede, quantidade de redes independentes, blocos com holders não lógicos, seções e jogadores.

Não definir número máximo de alunos ou afirmar ausência de lag a partir deste benchmark. Introduzir invalidação por rede/região somente se as medições no uso real indicarem necessidade, preservando os testes de seções vazias, orientação e reconstrução.

## Referência local de 23/10/2026

Medição do incremento 1.2.0, ID `1.2.0+794c65612510`, Windows/Java 25.0.3+9-LTS. Valores desta máquina, sem clientes conectados; não são metas de CI.

| Cenário | Componentes principais | Mediana (ms) | p95 (ms) |
| --- | --- | --- | --- |
| `cable-line` | 100 | 0.346 | 0.574 |
| `cable-grid` | 100 | 0.329 | 0.496 |
| `mixed-gate-chain` | 100 | 0.631 | 1.053 |
| `independent-networks` | 100 | 0.106 | 0.129 |
| `cable-line` | 1000 | 0.879 | 1.508 |
| `cable-grid` | 1000 | 0.600 | 0.851 |
| `mixed-gate-chain` | 1000 | 1.520 | 1.862 |
| `independent-networks` | 1000 | 1.218 | 1.564 |
| `cable-line` | 10000 | 6.225 | 7.319 |
| `cable-grid` | 10000 | 2.881 | 5.022 |
| `mixed-gate-chain` | 10000 | 7.875 | 10.879 |
| `independent-networks` | 10000 | 4.080 | 7.003 |

Os relatórios JSON de cada execução guardam as condições e alocações. Pequenas diferenças entre rodadas são esperadas; comparar versões nas mesmas condições. O teste nativo adicional confirmou coleta de tempos e comandos, mas não substitui medição com alunos/jogadores.
