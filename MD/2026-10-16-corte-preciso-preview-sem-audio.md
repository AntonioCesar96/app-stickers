# Corte preciso e reprodução silenciosa

## Solicitação

Corrigir a aceleração no começo dos loops do preview de crop e reproduzir vídeos sem áudio.

## Implementação

- `VideoTrim.java`: gera argumentos estruturados para FFmpeg, incluindo caminhos com espaços. O trecho é decodificado e recodificado em H.264 (`veryfast`, CRF 18), com `setpts=PTS-STARTPTS`, sem B-frames e sem áudio, legendas ou dados. Desse modo o corte descarta preroll anterior ao início selecionado e começa com timestamps novos em zero. Mantém FPS de origem e dimensões; dimensões ímpares recebem no máximo um pixel de padding para yuv420p.
- `CustomVideoRangeActivity.java`: substitui `-c copy` pelo corte preciso, valida intervalo e desativa o botão durante o processamento, exibindo “Preparando vídeo…”. Restaura o botão ao concluir ou falhar e evita abrir tela depois de a Activity ser encerrada. ExoPlayer fica com volume zero, inclusive ao ser recriado. O fluxo de GIF permanece usando seu arquivo original e intervalo.
- `CropVideoActivity.java`: silencia o MediaPlayer no callback de preparação, inclusive para arquivos antigos que ainda tenham áudio.
- `strings.xml` e `activity_custom_video_range.xml`: texto do botão de aparar em recurso compartilhado para restauração após o processamento.
- `VideoTrimTest.java`: regressão instrumentada com vídeo H.264 de 12 segundos, keyframes distantes e áudio. Corta de 3,7 a 9,7 segundos usando o FFmpeg real do Android e inspeciona a saída pelo MediaExtractor nativo.

## Validação

- 22 testes unitários passaram; builds debug, testes instrumentados e release concluídos.
- Teste instrumentado no celular USB passou: saída com uma única faixa de vídeo H.264, 240×432, duração de 6 segundos, exatamente 180 amostras a 30 FPS, primeira amostra em zero e timestamps estritamente crescentes. Seek nativo de retorno ao início aponta para zero. Nenhuma faixa de áudio.
- Validação cobre timestamps/corte e seek pelo extrator nativo; não foi realizada comparação visual automatizada de vários loops na tela de crop.
- APK release local assinado com o certificado já utilizado para atualizar preservando dados; build com `-x checkRelease` por conta da proteção existente do identificador de exemplo. Pacote de testes removido após a validação.

## Uso

É necessário aparar novamente o vídeo para regenerar o preview com a nova timeline. Arquivos privados de tarefas anteriores não são reescritos. O corte agora pode demorar um pouco mais e envolve recodificação com pequena perda, controlada pelo CRF 18; o algoritmo posterior para gerar WebP de até 500 KB continua o mesmo.
