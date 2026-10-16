# CompressionSettingsTest: testar regras sem encoder

Última atualização: 2026-10-16_10-40 (America/Cuiaba).

Código: `app/src/test/java/com/example/samplestickerapp/CompressionSettingsTest.java`.

## Responsabilidade e isolamento

Esta classe JUnit testa regras numéricas e decisões da busca sem iniciar Android ou FFmpeg. Esse isolamento torna os testes rápidos e permite distinguir erro de validação de uma falha do codec. Os métodos não possuem estado compartilhado e cada cenário constrói seus próprios objetos.

## Partições de entrada

`rejectsInvalidOverridesBeforeInterruptingEncoder` usa `assertThrows` para verificar qual exceção rejeita valores fora do domínio. Testa qualidade abaixo e acima dos limites, FPS inválido, compressão excessiva, velocidade zero/NaN e detalhe zero. Esses casos representam classes de erro, não todas as combinações possíveis de números.

`speedMustRespectAnimatedStickerDuration` mostra a diferença entre campos válidos e combinação válida. Dez segundos a 1× passam; seis a 0,5× não passam porque resultam em doze. A operação precisa ser avaliada antes de interromper uma tentativa útil.

## Sequência de estados

`customStartingSharpnessIsNeverRaisedByFallback` cria busca em qualidade 2, FPS 8 e detalhe 200. Cada tamanho excedido força uma transição observável. O teste confirma 2, 1 e 0 com detalhe constante, depois detalhe 192 e FPS ainda 8. Um tamanho aceitável deve encerrar a busca e pedir retenção.

Essa simulação testa uma propriedade do algoritmo e não somente a presença de um getter: a preferência manual precisa permanecer coerente ao longo de várias decisões.

## Limites e exercício

Os tamanhos usados são entradas simuladas. Eles não demonstram que um vídeo real produzirá esses bytes, nem validam frames, transparência ou qualidade perceptiva. O teste instrumentado complementa essa camada usando codec real.

**Exercício:** o que mudaria se o teste verificasse somente o detalhe inicial 200? Ele deixaria passar um erro que aumentasse a nitidez em uma tentativa posterior. Propriedades de transição exigem observar uma sequência, não apenas a construção do objeto.
