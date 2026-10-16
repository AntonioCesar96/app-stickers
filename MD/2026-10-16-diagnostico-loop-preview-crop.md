# Diagnóstico da aceleração no reinício do preview

Somente investigação solicitada; nenhum código ou APK alterado nesta tarefa.

## Evidências

- `CropVideoActivity.java:182`: MP4 usa `VideoView` com `MediaPlayer.setLooping(true)`. Não há alteração de playback speed nessa reprodução. O parâmetro de velocidade da conversão é separado.
- `CustomVideoRangeActivity.java:293`: aparar MP4 executa `-ss ... -i ... -t ... -c copy`, preservando streams sem recodificação.
- `dumpsys media.player` identifica o app como cliente do NuPlayer em loop, com preview privado da tarefa `17bec8f2-a30c-4b64-94de-302d36ace1d3`, resolução 240×432 e decoder `c2.mtk.avc.decoder`.
- Logcat na tela de crop: em 09:06:27.144, o renderer recebe `nowMediaus -3552018`; aparecem repetidamente `MediaClock: reject anchor time since it is negative`. O mesmo padrão se repete nos loops. Quadros separados por aproximadamente 33333 microssegundos de mídia são processados com intervalos de poucos milissegundos reais durante essa fase negativa.
- O dump do player indicava zero frames descartados; não há evidência de que o efeito seja causado simplesmente por sobrecarga da compressão.

## Interpretação

O efeito está fortemente associado ao tratamento dos timestamps negativos pelo player nativo no reinício. A origem mais provável é o corte sem recodificação em ponto intermediário entre keyframes: o arquivo pode manter preroll anterior ao início selecionado. O player percorre esse trecho rapidamente até conseguir estabelecer o relógio em tempo não negativo. A documentação do FFmpeg confirma a preservação do segmento extra no modo stream copy; não foi realizada inspeção dos pacotes do arquivo privado nesta tarefa, portanto a cadeia exata de metadados permanece uma inferência fundamentada.

Referências: https://ffmpeg.org/ffmpeg.html (opção `-ss`); https://android.googlesource.com/platform/frameworks/av/+/master/media/libmediaplayerservice/nuplayer/NuPlayerRenderer.cpp (agendamento de quadros e relógio).

Logs de trabalho ignorados pelo Git: `app/build/crop-loop-diagnostic.log` e `app/build/crop-player-diagnostic.txt`.

Uma correção futura deve tratar a geração/timeline do preview e validar o loop no dispositivo, sem concluir apenas que trocar parâmetros da compressão WebP resolveria o preview.
