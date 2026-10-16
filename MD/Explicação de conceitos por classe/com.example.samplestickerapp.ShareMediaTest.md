# ShareMediaTest: integração Android com dados controlados

Atualização: 2026-10-16_11-46 — America/Cuiaba.
Código: `app/src/androidTest/java/com/example/samplestickerapp/ShareMediaTest.java`.

## Por que instrumentação

Testes unitários isolados não demonstram que o Android encontra o app no compartilhamento nem que uma URI do MediaStore chega a uma Activity. Este teste executa no dispositivo com Instrumentation, Context do app, provedores reais e editores reais. JUnit oferece asserções; Instrumentation organiza navegação e chamadas na thread principal.

Os campos instrumentation e context dão acesso ao ambiente testado. `incoming` monta ACTION_SEND com MIME, EXTRA_STREAM, ClipData e concessão de leitura. ClipData é configurado separadamente porque setClipData não retorna a Intent nesta API. `publish` insere uma mídia de teste no provedor e copia os bytes; a URI resultante simula o contrato recebido de fora.

## Espera e observação

`ready` consulta o botão de crop na thread principal e usa AtomicBoolean para comunicar o resultado à thread do teste. O limite de 15 segundos evita espera infinita. Polling de 50 ms permite aguardar cópia e catálogo sem presumir que terminam imediatamente. Isso demonstra prontidão da tela, não progresso do encoder.

ActivityMonitor observa a classe aberta; waitForMonitorWithTimeout limita a espera. performClick roda por runOnMainSync porque componentes visuais pertencem à thread principal. Os monitores são removidos no finally para não contaminar os testes seguintes.

## Cenários concretos

`sendResolverSupportsImagesAndVideosButNotText` consulta o PackageManager para MP4, WebM, JPEG, PNG e GIF, exclui text/plain e verifica falha de entradas sem URI/ação indevida. A consulta prova o registro dos filtros, não compatibilidade completa de codecs.

`sharedVideoSurvivesRecreationAndOpensBothExistingEditors` primeiro exige ausência de CompressionService para não interferir em uma compressão ativa. Gera vídeo sintético de 2 s por FFmpeg, publica e compartilha. Depois de pronta a cópia, apaga o original externo, recria a tela, abre Aparar, volta e abre Crop. Confirma existência do arquivo privado e pacote animado. A remoção antes da recriação é o ponto que distingue cópia independente de simples reutilização da URI.

`photoUsesImageCropAndGifKeepsAnimatedImport` publica uma imagem verde, verifica ausência de aparar, abre CropImageActivity e exige pacote estático. Depois gera GIF sintético, remove EXTRA_STREAM e exercita apenas ClipData na importação; confirma kind gif e extensão. Não exporta uma figurinha final de GIF e não afirma testar essa etapa.

## Isolamento, custos e limites

As amostras usam nomes UUID no MediaStore e são removidas no finally. Arquivos de fixture e telas são limpos quando possível. O catálogo existente é lido para encontrar pacotes compatíveis; os testes dependem de haver pacotes estático e animado com vaga. Não adicionam figurinhas ao catálogo. Cópias privadas entregues aos editores podem seguir a política conservadora de retenção da implementação.

Gerar amostras consome CPU, mas os vídeos pequenos mantêm o teste curto. Timeouts são limites de segurança, não metas de desempenho. Um resultado positivo não cobre todos os apps remetentes, morte forçada do processo, todas as permissões ou todos os formatos. Os três cenários passaram no aparelho conectado em 5,592 s.

**Exercício:** por que apagar o original depois de ready e antes de recreate? **Resposta:** antes de ready impediria a cópia; depois de recreate não provaria que a restauração já independe do original. A ordem constrói a condição que se deseja testar.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_11-46_receber-compartilhamento-de-midias-do-android.md)
