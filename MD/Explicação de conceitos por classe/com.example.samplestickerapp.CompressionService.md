# CompressionService: execução, interrupção e publicação de resultados

Última atualização: 2026-10-16_11-04 (America/Cuiaba).

Código: `app/src/main/java/com/example/samplestickerapp/CompressionService.java`.

## Arquitetura e ciclo de vida

A classe é um Service Android que mantém a conversão fora do ciclo de vida da Activity. `start` usa `startForegroundService`; `onStartCommand` carrega o job, publica uma notificação foreground e agenda `convert` em um executor de uma thread. Trabalho pesado não roda na thread principal.

O mesmo ID em execução não é enfileirado repetidamente. `START_REDELIVER_INTENT` permite redelivery pelo Android em condições suportadas; persistência permite reconstruir trabalho. Isso não garante sobrevivência a toda intervenção do sistema ou do usuário.

Um wake lock parcial mantém CPU disponível durante o trabalho. Aquisição e liberação são coordenadas; `onDestroy` marca encerramento, cancela a sessão específica, interrompe o executor e libera o lock. Destruição da Activity não chama esse encerramento do serviço.

## Fontes e geometria

`runSearch` copia preview e original GIF para diretório durável antes de converter. FFprobe lê stream, duração e dimensões. O serviço valida velocidade, duração e área, normaliza o recorte sobre um quadrado e calcula um ou quatro quadrantes.

O filtro reúne timeline, FPS, formato RGBA, padding transparente, crop, redução de detalhe e padding final. GIF usa a timeline do original e o intervalo escolhido; MP4 usa o trecho já aparado. Detalhe reduzido continua gerando canvas 512×512.

## Busca e checkpoints

Cada quadrante possui uma nova `AdaptiveWebpSearch`. Os tamanhos committed em `trials` são reaplicados para retomar a decisão. Antes do encode, `currentAttempt` captura número, quadrante e parâmetros efetivos. Depois de conclusão bem-sucedida, `lastAttempt` copia esses dados e acrescenta bytes.

O candidato só substitui o melhor quando `recordSize` retorna true. O arquivo melhor é copiado antes de persistir o trial; caso haja interrupção entre operações, a tentativa pode ser repetida sem aceitar um candidato não guardado. Resultados dos quadrantes são acrescentados ao JSON apenas depois da validação.

## Sessão nativa e sincronização

`encode` inicia FFmpeg assíncrono. O callback de conclusão publica a sessão em AtomicReference e libera CountDownLatch. Estatísticas e logs do filtro final `showinfo=checksum=0` atualizam `CompressionProgress`, que usa AtomicInteger para compartilhar o percentual. O timestamp do quadro após os filtros representa a timeline de saída; o medidor adiciona a duração de um quadro e compara com a duração total ajustada pela velocidade. Isso evita depender exclusivamente do tempo emitido pelo muxer WebP, que pode ficar em zero durante a conversão.

O percentual fica limitado a 99 enquanto há trabalho nativo, e a interface informa finalização. Não é porcentagem de tempo de CPU: filtros e codificação podem ter custos diferentes ao longo do vídeo. O medidor é novo a cada encode, e a notificação usa barra determinada mesmo em zero.

A thread espera em intervalos de 100 ms e chama `checkControl`. Aproximadamente a cada segundo grava progresso e notifica. Quando há controle pendente, cancela o sessionId exato, aguarda a conclusão e só depois permite reuso do candidato. A espera evita duas sessões escrevendo no mesmo caminho.

`interruptedAttempts` registra interrupções pedidas enquanto o latch ainda não foi liberado. Esse contador dá evidência útil ao teste, mas não é uma métrica de tempo de cancelamento.

## Controle, estados e retomada

`checkControl` verifica destruição, cancelamento e revisão não aplicada. Exceções internas de controle distinguem reinício/cancelamento de falha de mídia. Elas são sinais de fluxo internos ao worker; não representam crashes esperados na interface.

`convert` mantém um loop: executar busca; se receber RetryRequested, aplicar e começar novamente. Isso evita recursão acumulando chamadas a cada ajuste do usuário. `applyRetry` relê o comando para dar prioridade ao cancelamento, valida parâmetros, zera histórico e resultados, remove saídas antigas e grava a revisão aplicada.

CancelRequested leva a cancelled, remove candidato parcial e preserva o registro para o diálogo. Outras exceções levam a error, salvo destruição do serviço, quando o estado running permanece para recuperação. O caminho de sucesso confirma done sob o mesmo bloqueio usado na aceitação dos comandos, reduzindo a disputa entre concluir e reiniciar.

## Validação e notificações

`describe` abre WebP pelo Fresco, verifica bytes, dimensões, duração, quadros e duração mínima de cada quadro, produzindo a descrição do resultado. O objeto nativo é liberado em finally. Ter menos de 500 KB não basta para passar por essas regras.

`notification` usa PendingIntent explícito e URI por UUID para direcionar o editor correto. Canais distinguem andamento e término. `notice` produz a mensagem de redução extra para consulta pelo ícone da UI, sem ocupar o corpo fixo do diálogo ou a notificação.

No finally, o serviço libera o wake lock e agenda a conclusão visual na main thread. A comparação de `runningId` evita parar uma nova tarefa que já foi enfileirada enquanto a anterior encerrava.

## Custo, alternativas e exercício

Os encodes dominam o custo de CPU e memória. Consultar controle a cada 100 ms tem custo adicional de leitura de arquivo; usar só um campo em memória seria mais barato, mas perderia o pedido após morte do processo. Executar sessões simultâneas aumentaria a complexidade e a disputa por recursos.

**Exercício:** por que não chamar `stopService` como implementação completa do botão Cancelar? Porque destruir o serviço deixa o registro running para recuperação, que é correto para interrupção inesperada e incorreto para uma intenção explícita de parar. Cancelar precisa persistir cancelled.

Os testes do diálogo usam o serviço e o codec reais. Não medem toda política de bateria de fabricantes nem garantem latência fixa de parada.
