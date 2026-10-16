# Compressão além do limite preferido

## Solicitação

Continuar tentando gerar WebP de até 500 KB quando qualidade 55 e 12 FPS não forem suficientes. Avisar o usuário e reduzir qualidade lentamente. Preferência confirmada: ao esgotar qualidade, reduzir nitidez mantendo os FPS.

## Alterações

- `AdaptiveWebpSearch.java`: preserva a busca preferida existente. Depois da última tentativa no piso, testa qualidade de 1 em 1 até 0, parando no primeiro arquivo realmente dentro de 512000 bytes. Se necessário, passa a reduzir detalhes espaciais progressivamente, sem reduzir mais os FPS. Valores explicitamente inferiores a 12 FPS continuam respeitados. A busca tem limites finitos para evitar loops em arquivos impossíveis.
- `GifMedia.java`: filtro de redução de detalhes por downsampling seguido de ampliação para a saída de 512×512. Mantém enquadramento e não diminui a figurinha dentro da tela.
- `CompressionService.java`: integra cada etapa ao FFmpeg e ao histórico persistente de tentativas; publica aviso na notificação expandida, registra a utilização da redução extra e informa o resultado. Mantém validação estrutural, tamanho e duração do WebP. Retomada reconstrói a busca a partir das tentativas gravadas.
- `CropVideoActivity.java` e `strings.xml`: aviso no diálogo e no estado de progresso, sem exigir confirmação ou interromper o trabalho em background.
- Testes em `AdaptiveWebpSearchTest`, `GifMediaTest` e `BackgroundCompressionTest`: redução de um ponto, parada no primeiro resultado, preservação de FPS, limites finitos, retomada e filtro de nitidez.

## Validação

- 22 testes unitários passaram; compilação debug e release concluída.
- Teste instrumentado no Android USB passou: duas conversões reais pelo codec do celular, uma retomando em qualidade 54 e outra em qualidade 0 com nitidez reduzida. Ambas mantiveram 12 FPS e passaram pela validação de WebP de 512×512 e até 500 KB do serviço.
- As tentativas anteriores desses dois cenários foram simuladas por checkpoints de tamanho excedido; não é um benchmark completo de todas as iterações nem uma nova conversão integral do vídeo relatado. O registro anterior do usuário foi preservado e restaurado após os testes.
- Release local usa o certificado já instalado para atualizar sem remover dados; não é uma chave de publicação. Build com `-x checkRelease` por causa da restrição existente ao identificador de exemplo.

## Comportamento esperado

A busca extra pode demorar por executar uma conversão para cada ponto de qualidade. O serviço continua em background e mantém os avisos de progresso. Entradas inválidas ou resultados impossíveis ainda encerram com erro, em vez de repetir indefinidamente.
