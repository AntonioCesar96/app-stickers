# CompressionProgress: progresso medido pela timeline

Última atualização: 2026-10-16_11-04 (America/Cuiaba).

Código: `app/src/main/java/com/example/samplestickerapp/CompressionProgress.java`.

## Responsabilidade e entradas

Esta classe calcula o percentual de uma tentativa a partir de timestamps reais, sem conhecer Android ou Views. O construtor recebe duração de saída e FPS positivos. A duração já considera corte e velocidade; usar a duração original ao reproduzir a 2× produziria uma escala incorreta.

`frameLog` procura `pts_time` em mensagens do filtro showinfo. Pattern é uma expressão regular compilada uma vez e Matcher encontra números, inclusive em notação científica. Mensagens sem o filtro ou com números inválidos não alteram o resultado. `statistics` recebe milissegundos do FFmpeg e converte para segundos.

## Cálculo e sincronização

Para um quadro, acrescenta `1 / fps` ao timestamp de início para considerar sua duração. Em um vídeo de 4 segundos a 10 FPS, um quadro em 1,9 segundo alcança 2 segundos de timeline: 50%.

`updateSeconds` rejeita valores não finitos ou negativos e calcula `floor(seconds * 100 / durationSeconds)`. Limita a 99 porque ter alcançado o último quadro filtrado não garante que a codificação e o fechamento do arquivo terminaram.

Callbacks de logs e estatísticas podem chegar separadamente. AtomicInteger com `accumulateAndGet(..., Math::max)` torna a atualização monotônica e segura entre threads: uma estatística atrasada em zero não desfaz um avanço por quadros. Criar outro objeto para outra tentativa reinicia naturalmente em zero.

## Limites e exercício

O valor descreve fração da timeline processada, não fração do tempo de CPU nem bytes finais. O encoder pode demorar para finalizar mesmo perto de 99. Não se estima o número de tentativas necessárias. `get` apenas entrega o inteiro medido; a conclusão e a validação pertencem ao serviço.

**Exercício:** depois de 50%, chega uma estatística de 1 segundo de um vídeo de 4 segundos. O resultado volta a 25%? Não: o máximo atômico preserva 50%. Já uma tentativa nova começa com outro objeto em zero.

Os testes unitários cobrem o fallback quando o muxer informa zero, entradas inválidas, monotonicidade, limite de 99 e reinício. A validação instrumentada confirma que logs do codec instalado realmente alimentam o caminho.
