# Conversão de figurinhas animadas

O arquivo final é WebP animado, não MP4. O orçamento usado pelo app é
500 × 1024 = 512.000 bytes, em um canvas de 512 × 512 pixels e até 10 segundos.
FPS, duração, detalhe e movimento competem por esse orçamento. Nenhuma configuração
garante 30 FPS com boa nitidez para qualquer vídeo de 10 segundos.

## Busca equilibrada

`AdaptiveWebpSearch` começa com a qualidade e o FPS escolhidos (75 e 30 por padrão).
O FPS é limitado pela taxa média da fonte, considerando a velocidade de reprodução.

1. Testa a qualidade escolhida na taxa de quadros atual.
2. Se exceder o limite, testa qualidade 55, ou a qualidade escolhida se ela já for menor.
3. Se essa tentativa couber, faz busca binária entre os limites de qualidade e
   guarda o melhor arquivo válido encontrado. Uma tentativa maior não o substitui.
4. Se nem o piso couber, estima a taxa que caberia pela proporção entre tamanho
   permitido e medido. Testa uma faixa acima dessa estimativa, entre 48, 30, 24,
   20, 18, 15 e 12 FPS, evitando recodificações claramente grandes demais.
   O tamanho real continua decidindo se o resultado cabe. Fontes ou configurações
   abaixo de 12 FPS são respeitadas.
5. Se não houver resultado válido, pede um trecho mais curto ou outro recorte.
   Não apresenta um arquivo acima do limite como conversão bem-sucedida.

A busca é uma aproximação: tamanho e qualidade não têm relação perfeitamente
monótona, e a escala de qualidade do encoder não é uma medida de qualidade percebida.
O piso 55 é uma escolha inicial para equilibrar nitidez e fluidez, não uma garantia
de qualidade visual. Os testes limitam a busca e verificam a retenção do resultado válido.

Cada tentativa parte da mesma fonte. No Exodia, os quatro quadrantes são processados
sequencialmente, com buscas independentes, e só são liberados para salvar quando
todos estiverem válidos. A duração e a velocidade permanecem comuns, mas os FPS
podem diferir conforme a complexidade de cada quadrante.

## Encoder e importação

- Usa `libwebp_anim`, `-lossless 0`, `-quality`, `-compression_level` e `-preset none`.
- No FFmpeg 6, um preset explícito substitui o método de compressão. O antigo
  `-preset default` fazia o método voltar a 4, mesmo com `-compression_level 6`.
  Fonte: https://github.com/FFmpeg/FFmpeg/blob/n6.0/libavcodec/libwebpenc_common.c#L88-L101
- Compressão 6 prioriza tamanho e pode custar bastante tempo no celular.
- Usa Lanczos no redimensionamento e preserva o preenchimento transparente.
- A importação opcional para MP4 usa CRF 18, preserva a taxa de quadros em vez de
  forçar 24 FPS e separa o cache novo com o sufixo `_hq`. Selecionar uma resolução
  pequena ainda reduz detalhe; usar o vídeo original evita essa etapa com perdas.
- A prévia informa FPS médio real e tamanho em disco, não quantidade de quadros
  rotulada como FPS ou tamanho da imagem decodificada em memória.

### GIF importado

GIFs usam um fluxo próprio, sem a seleção prévia de resolução. Um MP4 temporário
serve somente para reprodução nos players Android; ele apresenta a transparência
sobre fundo cinza claro. A figurinha final é codificada diretamente do GIF original,
preservando o alfa e evitando a perda acumulada das conversões intermediárias.

Aparar um GIF guarda o início e o fim selecionados. O mesmo MP4 de prévia acompanha
a edição e a exportação aplica esse intervalo à fonte original. O loop embutido
é ignorado na leitura para processar apenas um ciclo. A busca de tamanho continua
ativa e o GIF original não é alterado. As prévias ficam em `cache/gif_previews`,
separadas dos vídeos processados que aparecem no explorador.

`GifPipelineTest` gera sua própria amostra transparente com dimensões ímpares e
quadros de durações diferentes. Ele verifica os comandos nativos e o fluxo real
de importação, aparar e recortar, sem incluir arquivos pessoais no APK.

## Verificação

`gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`

`WebpEncodingTest` é uma comparação opcional para uma amostra de 720 × 484,
usando seus primeiros 8 segundos e enquadramento inteiro. O arquivo não é incluído
no repositório ou no APK. Para executar, instale os APKs de debug e de testes,
copie a amostra para `files/webp-benchmark/input.mp4` usando `adb shell run-as`
e execute o runner diretamente:

```text
adb shell am instrument -w -r -e class com.example.samplestickerapp.WebpEncodingTest com.example.antoniosamplestickerapp.test/androidx.test.runner.AndroidJUnitRunner
```

O teste usa o FFmpeg embarcado, compara a busca antiga com a nova e verifica
tamanho, dimensões, duração, tempos dos quadros e o método de compressão efetivo.
Os arquivos `old.webp`, `new.webp` e `comparison.txt` ficam na mesma pasta interna.
Sem a amostra, esse teste é ignorado. A comparação não mede qualidade perceptual
nem substitui a avaliação visual do usuário.

## Comparação medida em 15/10/2026

Amostra de vídeo 720 × 484, aproximadamente 60 FPS, com preenchimento transparente
até 512 × 512, sem alteração de velocidade:

| Conversão | Trecho inicial | FPS | Parâmetro quality | Bytes | KB (1024 bytes) |
| --- | ---: | ---: | ---: | ---: | ---: |
| Antiga, FFmpegKit no Android 16 | 8 s | 11 | 60 | 502046 | 490,3 |
| Nova, FFmpegKit no Android 16 | 8 s | 12 | 55 | 509214 | 497,3 |
| Alternativa curta, FFmpeg 8.0.1 no Windows | 4 s | 24 | 75 | 486216 | 474,8 |

O ganho no trecho de 8 segundos foi pequeno e exigiu reduzir o parâmetro de
qualidade. A alternativa curta preserva menos conteúdo, mas permite muito mais
fluidez e um parâmetro de qualidade maior. Ela foi gerada em outro build do encoder;
não é uma medição do app Android. Não houve avaliação perceptual automatizada.

Passaram os oito testes unitários e a comparação instrumentada no Android. As duas
primeiras tentativas novas, acima do limite, foram reaproveitadas de medições já
concluídas com a mesma amostra e encoder. Os arquivos finais foram efetivamente
codificados e validados. Compressão 6 teve custo de processamento significativo;
essa mudança busca melhorar a escolha do resultado, não garante conversão mais rápida.
