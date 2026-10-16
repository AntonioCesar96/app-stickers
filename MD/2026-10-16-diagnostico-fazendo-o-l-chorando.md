# Diagnóstico do vídeo fazendo-o-l-chorando.mp4

## Escopo

Investigação solicitada sem alterar código. Nenhum arquivo de código, configuração do aplicativo, instalação ou figurinha do usuário foi modificado. O vídeo foi copiado de `/sdcard/Download/` para `app/build/fazendo-analysis/`, e as conversões de diagnóstico ocorreram somente no computador. Os intervalos informados pelo usuário foram corrigidos para 8 e 6 segundos.

## Evidências

- Original: 395.237 bytes (385,97 KiB), H.264 High, 240×432, 30 FPS, 12,1 segundos, 363 quadros, sem áudio. A contagem de quadros decodificados corresponde aos metadados.
- Logcat de duas tarefas com entrada de 6,21 segundos: saídas aproximadas de 1.375 KiB a 25 FPS, 1.309 KiB a 25 FPS e 630 KiB a 12 FPS; término com `IOException: Encurte a duração ou recorte uma área menor para manter a fluidez em até 500 KB`. O encoder termina normalmente; o ramo `!search.hasResult()` rejeita os tamanhos. Os parâmetros de qualidade dessas tarefas não constam explicitamente no log coletado, portanto não foram presumidos.
- `AdaptiveWebpSearch` estabelece limite de 512.000 bytes. Se a qualidade solicitada é maior ou igual a 55, o piso é 55; a escada automática de FPS termina em 12. Qualidade/FPS menores podem ser escolhidos manualmente nas opções, mas não são tentados automaticamente a partir dos valores normais.
- A conversão gera canvas 512×512 com escala Lanczos e preenchimento transparente. Não é compressão do mesmo MP4: transforma vídeo H.264 em WebP animado. O tamanho do MP4 não é um teto para o resultado. WebP usa codificação intra de imagens VP8, conforme a [especificação oficial](https://developers.google.com/speed/webp/docs/riff_container); não oferece a mesma compensação de movimento entre quadros do H.264.

## Reprodução local

FFmpeg 8.0.1 no computador, primeiros 6 ou 8 segundos do original, sem recorte espacial adicional: normalização de timestamps, FPS fixo, RGBA, preenchimento 432×432, escala/pad 512×512, `libwebp_anim`, lossy, compressão 6, preset `none`, loop infinito e sem áudio. O Android usa FFmpeg 6.0, e os trechos/recortes escolhidos no celular não puderam ser recuperados dos registros privados da release; portanto não são reproduções binárias idênticas das tarefas do aparelho.

| Duração | FPS | Qualidade | Bytes | KiB | Até 500 KiB |
| --- | --- | --- | --- | --- | --- |
| 6 s | 12 | 55 | 571.366 | 557,97 | Não |
| 8 s | 12 | 55 | 765.866 | 747,92 | Não |
| 6 s | 12 | 35 | 465.492 | 454,58 | Sim |
| 8 s | 8 | 45 | 470.820 | 459,79 | Sim |

Os dois exemplos menores atendem aos limites estruturais conferidos: canvas 512×512, 72/64 quadros, duração de 5.999/8.000 ms, menor duração por quadro de 83/125 ms, loop infinito, tamanho dentro do orçamento e comprimento RIFF consistente. Não foram importados no WhatsApp nem submetidos ao decoder Fresco no aparelho.

Também foi testado o corte por stream copy de 3,7 a 9,9 segundos: o arquivo reportou 6,3 segundos e decodificou 188 quadros/6,3 segundos após a mesma normalização de timestamps. Não foi confirmada uma expansão oculta do trecho pelo corte. O teste com 6 segundos exatos já excede o limite a qualidade 55/12 FPS.

## Conclusão

Há resultados abaixo do limite para este vídeo, com concessões de qualidade ou fluidez. A busca atual pode rejeitar o vídeo porque não explora valores abaixo dos pisos definidos; ela não garante encontrar toda combinação possível até 500 KiB. O tamanho reduzido do original decorre de outro codec, resolução e modo de compressão, e não contradiz esses resultados.

Logs, cópia do original, probes, quadro de referência, saídas WebP e `results.json` estão em `app/build/`, ignorado pelo Git. Este documento é o único arquivo rastreável criado pela investigação.
