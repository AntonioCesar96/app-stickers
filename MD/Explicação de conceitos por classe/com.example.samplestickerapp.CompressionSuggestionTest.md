# CompressionSuggestionTest: propriedades de uma proposta editável

Código: `app/src/test/java/com/example/samplestickerapp/CompressionSuggestionTest.java`.
Atualização: 2026-10-16_17-23 — America/Cuiaba.

## Objetivo e fixture

A classe JUnit roda na JVM, sem Android nem FFmpeg. base é um CompressionSettings com Q75, 30FPS, esforço4, velocidade1,25 e detalhe512. Esse vetor representa uma referência controlada; os tamanhos passados são entradas de teste, não arquivos realmente comprimidos.

O objetivo é verificar propriedades importantes do algoritmo em vez de apenas repetir sua fórmula. Asserções comparam intensidades, respeito aos limites, preservação de tempo e estabilidade com a mesma evidência.

## Os seis cenários

modesHaveOrderedStrengthAndPreserveTiming usa um arquivo duas vezes maior que o alvo. Exige qualidade e detalhe estritamente menores no modo mais forte, FPS não maior, velocidade preservada e esforço aumentado para5. Isso detecta inversão de opções no algoritmo.

largerOvershootProducesStrongerReduction compara520000 e3000000 bytes no modo Moderado. A mesma base precisa receber redução maior quando o excesso cresce. Testa a dependência do tamanho, não apenas o enum.

currentReductionsAreNeverRaisedAndLowFpsIsPreserved usa medição antiga mais alta e estado atual Q20/FPS8/detalhe180. Exige não subir esses parâmetros, manter8FPS e velocidade0,75 e limitar esforço6. Reproduz uma busca automática que avançou desde a última saída medida.

twelveFpsFloorAndAllParameterBoundsSurviveExtremeSize usa qualidade0, detalhe1, FPS12 e Long.MAX_VALUE bytes. Exige não sair dos limites nem produzir um FPS inferior ao piso. Não é uma amostra de arquivo real: é um caso extremo para a aritmética.

fittingOutputDoesNotDegradeAndSameEvidenceDoesNotCompound verifica o limite exato512000 e retorno do mesmo objeto current. Depois calcula duas propostas com evidência idêntica e exige os mesmos valores. Isso evita tratar uma proposta ainda não testada como se já fosse nova medição.

noMeasurementCannotGenerateASuggestion exige IllegalArgumentException com bytes0. Sem medição, o formulário deve orientar aguardar e não inventar uma redução baseada em tamanho desconhecido.

## O que prova e o que não prova

Os seis testes passaram na suíte unitária. Eles mostram coerência do cálculo nos casos exercitados, não garantem uma curva universal de qualidade/tamanho nem validade de um WebP exportado. O cenário Android complementa com Spinner/inputs reais e job cancelado permanecendo cancelado. Nenhum teste desta classe inicia conversão, altera arquivos ou salva figurinhas.

**Exercício:** por que não exigir que FPS de Agressivo seja sempre estritamente menor que o de Suave? **Resposta:** ambos podem estar no piso12. A ordem de intensidade precisa respeitar limites, não violá-los para fabricar diferença numérica.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_17-23_sugerir-reducao-de-compressao-por-intensidade.md)
