# AdaptiveWebpSearch: escolher a próxima tentativa

Última atualização: 2026-10-16_10-40 (America/Cuiaba).

Código: `app/src/main/java/com/example/samplestickerapp/AdaptiveWebpSearch.java`.

## Responsabilidade e estado

A classe decide quais parâmetros testar a seguir a partir do tamanho de uma saída real. Não executa FFmpeg, não copia arquivos e não conhece Activities. O serviço consulta os getters, converte e chama `recordSize(bytes)`.

`quality`, `rateIndex` e `detailIndex` identificam a tentativa atual. `bestQuality` começa em -1, indicando ausência de saída aceitável. `low` e `high` delimitam a busca de qualidade na faixa preferida. `done` informa que não há próximo passo. `extraCompression` distingue a etapa de redução gradual além do piso.

O limite é `500 * 1024` bytes. `recordSize` aceita somente tamanho positivo até esse valor. Seu retorno booleano tem um propósito específico: informar se o serviço deve guardar o candidato como melhor arquivo, e não apenas se o algoritmo terminou.

## Construção das escadas

O construtor de dois argumentos delega ao de três com nitidez 512. O novo construtor adiciona primeiro a nitidez escolhida e depois somente níveis inferiores da escada. Para 200, a lista começa em 200, 192, 160, 128 e continua descendo. A escolha manual não é desfeita por um retorno a 512.

A lista de FPS começa na taxa pedida e inclui taxas inferiores da sequência 48, 30, 24, 20, 18, 15 e 12 quando aplicáveis. Um início em 8 não ganha um passo em 12. O piso de qualidade preferido é `min(55, maxQuality)`, respeitando uma escolha manual abaixo de 55.

## Decisão por tamanho

Se a qualidade máxima couber, a busca termina imediatamente. Se não couber, testa o piso preferido. Um piso excedido tenta uma taxa de quadros menor; o tamanho anterior fornece uma estimativa para pular taxas pouco promissoras, mas toda aceitação depende de conversão real.

Quando há espaço para refinar a qualidade mantendo FPS, `low`, `high` e o ponto médio reduzem o intervalo de tentativas. Isso se aproxima de uma busca binária sobre qualidade, com a ressalva de que tamanho do encoder não é uma função estritamente garantida como monotônica para todas as entradas. O resultado aceito continua sendo um arquivo medido; não se deve interpretar a classe como prova do ótimo global de qualquer vídeo.

Depois de esgotar o piso de FPS, `continueBelowFloor` reduz qualidade um ponto por tentativa. Em zero, passa à próxima nitidez inferior. Esgotar o último nível encerra sem resultado, evitando repetição infinita.

## Simulação

Com qualidade inicial 2, FPS 8 e detalhe 200, rejeitar três resultados excedidos produz a sequência `(2,8,200)`, `(1,8,200)`, `(0,8,200)`. A próxima tentativa será `(0,8,192)`. Se ela couber, `recordSize` retorna true e `done` fica true.

Na faixa preferida, uma saída boa é preservada mesmo se uma tentativa posterior de qualidade maior exceder o limite. `bestQuality` evita descartar evidência válida em uma exploração malsucedida.

## Fonte, velocidade e retomada

`sourceLimitedFps` lê taxas como `24000/1001`, multiplica pela velocidade e limita o pedido, evitando fabricar uma taxa maior que o limite calculado da fonte. Metadados ausentes ou inválidos levam ao pedido original. Não é análise individual de timestamps de todos os quadros.

A busca é determinística para os parâmetros iniciais e a sequência de tamanhos. Reaplicar os tamanhos persistidos reconstrói sua posição. Mudar os parâmetros exige limpar esse histórico: tamanhos de outra configuração não descrevem a busca nova.

## Custo, testes e exercício

Manter listas e inteiros custa pouca memória; o custo dominante está nas conversões chamadas pelo serviço. A etapa de um ponto por vez pode exigir dezenas de encodes. Os testes cobrem retenção de resultado, limites finitos, queda de qualidade e nitidez, FPS baixos e reconstrução. Não medem qualidade perceptiva.

**Exercício:** um tamanho zero deve ser aceito porque está abaixo de 512000? Não: um arquivo vazio não é uma saída válida. O serviço também rejeita esse caso antes de prosseguir.
