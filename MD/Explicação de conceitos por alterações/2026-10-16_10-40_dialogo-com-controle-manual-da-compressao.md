# Controlar um encoder em execução sem perder estado

## 1. Do problema à divisão de responsabilidades

Antes desta mudança, a busca automática podia executar várias tentativas abaixo de qualidade 55. O usuário acompanhava uma mensagem, mas não via o arquivo anterior nem podia saltar diretamente para parâmetros mais agressivos. Interferir no encoder exige mais do que mudar o texto de um campo: é necessário coordenar interface, serviço, arquivos e callbacks assíncronos.

A interface pertence a `CropVideoActivity`. Ela pode ser destruída quando o usuário sai ou gira a tela. O trabalho pertence a `CompressionService`, que permanece separado desse ciclo de vida. `CompressionJob` guarda o estado durável. A mudança mantém essas responsabilidades: a Activity pede uma ação, o serviço executa, e o estado publicado informa o resultado.

## 2. Comando e estado são dados diferentes

Estado descreve o que aconteceu: “tentativa 5, qualidade 52, arquivo com 640000 bytes”. Comando expressa uma intenção: “interrompa e comece com qualidade 30”. Se ambos forem gravados na mesma fotografia de JSON por objetos diferentes, uma atualização de progresso pode substituir o comando recém-enviado.

A solução usa `state.json` para checkpoints e `control.json` para comandos. `AtomicFile` protege a substituição de cada arquivo: uma gravação interrompida pode recuperar a versão anterior. Isso não transforma dois arquivos em uma transação de banco de dados; a consistência entre eles depende também da revisão do comando e da ordem das operações.

Cada comando recebe uma revisão crescente. Se a revisão em control for maior que `appliedRevision`, o serviço ainda precisa agir. Aplicar um reinício grava os parâmetros e zera histórico/resultados junto com a revisão aplicada. Se o processo morrer antes desse checkpoint, o comando continua pendente e pode ser aplicado novamente. Não se depende de um evento transitório guardado na Activity.

## 3. Cancelar é assíncrono

`FFmpegKit.cancel(sessionId)` pede o cancelamento da sessão específica. Retornar dessa chamada não significa que o código nativo já parou de escrever. Recomeçar imediatamente no mesmo caminho poderia misturar duas gravações.

O serviço usa `CountDownLatch`: o callback de conclusão registra a sessão e libera a espera. Ao detectar reinício ou cancelamento, solicita a parada e aguarda esse callback antes de continuar. O executor com uma única thread serializa as buscas da aplicação; o latch coordena a thread Java com a execução nativa.

O loop consulta comandos a cada 100 ms, enquanto a publicação de progresso continua aproximadamente a cada segundo. Isso reduz a espera para detectar intenção sem escrever um checkpoint a cada consulta. O custo é ler um pequeno arquivo com frequência; não há garantia de latência final de 100 ms, pois a parada depende do FFmpeg e do dispositivo.

## 4. Máquina de estados e corridas

Os estados relevantes são running, done, error e cancelled. Cancelar termina a execução, mas não é um erro de conversão. A Activity deixa o diálogo aberto e a próxima tentativa terminal recebe outro UUID. Um identificador novo evita que o encerramento assíncrono da execução anterior seja confundido com o início da próxima.

Cancelar tem prioridade sobre um reinício ainda não aplicado. `applyRetry` relê o comando mais recente antes de reiniciar. A conclusão final e a aceitação de comandos usam o bloqueio de `CompressionJob.class`, de modo que uma requisição não seja aceita como se a tarefa ainda estivesse ativa depois de a conclusão já ter sido confirmada.

Exemplo: revisão 1 pede qualidade 30; antes de o serviço aplicar, revisão 2 pede cancelar. Ao observar revisão 2, ele encerra como cancelled e não começa uma conversão com os valores da revisão 1.

## 5. Parâmetros e reinício da busca

`CompressionSettings` valida números antes de enviar comandos. Uma qualidade 101 não deve destruir a tentativa atual para só depois descobrir que a entrada é inválida. A duração também depende da velocidade: 6 segundos a 0,5× produzem 12 segundos e ultrapassam o limite de 10.

O algoritmo adaptativo passa a aceitar nitidez inicial. O número representa uma resolução intermediária, não o tamanho final exigido pela figurinha. Em 128, a imagem é reduzida para perder detalhes finos e ampliada de volta para 512. A escada posterior usa apenas valores inferiores, evitando contradizer a escolha manual.

Ao mudar parâmetros, tamanhos de tentativas antigas não podem ser reproduzidos como se tivessem sido gerados pelos novos valores. Por isso o reinício limpa `trials` e `results`. O último registro concluído fica disponível para comparação, mas não é reutilizado como evidência da nova busca. No modo de quatro figurinhas, o conjunto inteiro reinicia para manter configurações coerentes.

## 6. Telemetria e edição da interface

`currentAttempt` registra os parâmetros antes de iniciar o encode. `lastAttempt` só recebe bytes depois de a conversão terminar com sucesso. A cópia do objeto captura os parâmetros efetivamente usados antes de `recordSize` escolher o próximo passo.

Isso evita um erro sutil: mostrar o tamanho da tentativa anterior com a qualidade já reduzida para a próxima. Um arquivo interrompido pode existir no disco, mas sua extensão e tamanho não provam que seja WebP completo; ele não substitui a última tentativa concluída.

Os campos editáveis não são preenchidos novamente a cada poll de 750 ms. O usuário pode estar digitando enquanto o algoritmo muda parâmetros. A cópia só ocorre ao abrir o formulário ou pelo botão explícito. O rascunho vai para o Bundle de estado da Activity; o comando já enviado vai para disco, pois tem exigência de persistência diferente.

O PopupWindow é uma janela ancorada ao ícone de informações. Ele permite consultar mensagens longas por ação explícita, deixando o corpo do diálogo dedicado a progresso, métricas e controles. A janela é dispensada no encerramento da Activity para não reter sua Window.

## 7. Alternativas e limites

Mudar diretamente variáveis da Activity seria simples, mas não alteraria necessariamente o serviço e perderia estado quando a tela saísse. Executar uma segunda sessão sem parar a primeira reduziria o tempo de resposta aparente, mas criaria disputa por CPU e pelo arquivo candidato. O canal separado com revisões oferece persistência e coordenação, ao custo de mais estados e verificações.

O tamanho final ainda depende do conteúdo e da duração. Valores menores não garantem um arquivo válido de qualquer entrada. O serviço mantém a validação do WebP e a busca tem limites finitos. Diminuir o esforço de compressão também pode aumentar o tamanho; não se deve confundir esse parâmetro com qualidade visual.

## 8. Exercícios comentados

**Por que guardar uma cópia dos parâmetros antes de chamar `recordSize`?** Porque o método pode alterar qualidade, FPS ou nitidez para a próxima tentativa. A métrica deve descrever o arquivo que acabou de ser produzido.

**Por que não aceitar uma tentativa parcialmente escrita quando ela já tem menos de 500 KB?** Porque tamanho parcial não demonstra integridade, duração ou quantidade de quadros. A saída precisa terminar e ser validada.

**Cancelar e minimizar devem ter o mesmo efeito no serviço?** Não. Minimizar altera apenas a apresentação. Cancelar solicita uma transição de estado e a parada do trabalho; o diálogo permanece para permitir uma nova decisão.

## Validação desta tarefa

Os testes unitários verificam limites de entrada, duração e a escada de nitidez personalizada. O teste instrumentado utiliza FFmpeg e a Activity reais para verificar métricas, reinício em parâmetros escolhidos, cancelamento sem fechar o diálogo e nova tentativa após cancelar. Os resultados finais e eventuais limitações são registrados no [histórico desta alteração](../Documentação%20das%20alterações/2026-10-16_10-40_dialogo-com-controle-manual-da-compressao.md).
