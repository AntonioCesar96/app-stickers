# O que uma porcentagem de vídeo consegue medir

## Progresso definido e indeterminado

Um indicador animado só comunica que existe trabalho. Uma barra determinada exige numerador e denominador: quanto da mídia foi percorrido e qual é a duração esperada. O código anterior voltava ao modo indeterminado quando o inteiro era zero. Agora zero é um valor válido, mostrado numericamente, sem animação indefinida.

## Por que somente as estatísticas não bastavam

O encoder WebP animado pode demorar a produzir pacotes do arquivo de saída. Por isso o tempo nas estatísticas pode continuar em zero enquanto quadros são processados. O filtro final showinfo fornece o timestamp dos quadros após crop, FPS e ajuste de velocidade. Ele observa a timeline realmente encaminhada ao encoder, sem modificar os pixels por si só. Fonte: [documentação do filtro showinfo](https://ffmpeg.org/ffmpeg-filters.html#showinfo).

O serviço combina esse sinal com as estatísticas normais. Não acrescenta percentuais com base em segundos de relógio: um telefone mais lento não recebe um avanço inventado.

## Fórmula, exemplo e significado

Um quadro começa em `pts_time` e dura aproximadamente `1 / fps`. A fração medida é `(pts_time + 1/fps) / duraçãoDeSaída`. Em quatro segundos a 10 FPS, o quadro que começa em 1,9 segundo alcança 2 segundos: 50% da timeline.

Isso não quer dizer que metade do tempo de CPU já passou. Quadros diferentes custam trabalhos diferentes e o encoder pode finalizar o arquivo depois de receber o último quadro. Por isso o medidor fica no máximo em 99 enquanto a sessão não terminou e a UI informa finalização. Done em 100 exige que o serviço termine e valide os resultados.

## Concorrência e tentativas independentes

Logs e estatísticas chegam por callbacks. O máximo atômico entre percentual anterior e novo impede retrocesso por mensagens fora de ordem. Um medidor novo em cada tentativa permite voltar corretamente a zero quando os parâmetros mudam, sem confundir monotonicidade de uma tentativa com progresso global.

Não existe denominador conhecido para o conjunto de tentativas: a próxima saída depende do conteúdo e das configurações. A barra deve, portanto, explicitar “Tentativa atual”, em vez de parecer uma previsão de todo o processo.

## Verificação e exercício

Testes unitários verificam cálculo, parsing e limites. O teste nativo observa um percentual intermediário real e cancela depois; o teste visual injeta 42% para conferir que texto e barra concordam. Separar esses cenários evita confundir uma captura preparada com uma medição real do encoder.

**Exercício:** se o vídeo de oito segundos roda a 2×, qual duração entra no denominador? Quatro segundos. Os timestamps observados já pertencem à timeline após velocidade, portanto comparar com oito diminuiria o percentual incorretamente.

Veja o [registro da tarefa](../Documentação%20das%20alterações/2026-10-16_11-04_progresso-percentual-real-por-tentativa.md).
