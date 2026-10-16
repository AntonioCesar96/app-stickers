# CropVideoActivity: edição, controle e salvamento de figurinhas

Última atualização: 2026-10-16_10-54 (America/Cuiaba).

Código: `app/src/main/java/com/example/samplestickerapp/CropVideoActivity.java`.

## Papel na arquitetura

A Activity apresenta o vídeo/GIF, permite escolher área e parâmetros, acompanha a tarefa e salva resultados no pacote. O encoder não pertence à tela: a Activity envia comandos a `CompressionJob` e `CompressionService`. Essa separação permite que a interface seja destruída sem encerrar uma conversão por acidente.

Os campos de View representam elementos da tela. `jobId` aponta para o estado persistido; `converting`, `jobRendered` e `progressHidden` controlam a apresentação. Os textos de qualidade/FPS/compressão/velocidade representam preferências de exportação, enquanto `retryDraft` preserva edições do formulário. Confundir essas categorias faria um poll de progresso apagar a digitação do usuário.

## Preparação, reprodução e coordenadas

`onCreate` verifica se há ID de tarefa para recuperar. Nesse caso, reconstrói Intent e parâmetros pelo registro; reinicia o serviço somente se running. Depois vincula controles, arquivos e listeners. Sem job, a criação aguarda clique nos botões de crop ou quatro figurinhas.

O VideoView é silenciado no callback de preparação. MP4 usa looping nativo. GIF usa proxy para reprodução e um Handler que retorna ao início selecionado quando alcança o fim. O export continua usando a origem GIF e o intervalo registrado, não o proxy como fonte de transparência.

`setupContainerAndCrop` cria container quadrado pela largura da tela e ajusta o vídeo preservando proporção. O overlay permite selecionar a área sobre esse quadrado. Na recuperação, as coordenadas normalizadas do job são convertidas à largura atual. A normalização separa seleção proporcional de pixels de um dispositivo específico.

## Ciclo de vida e polling

`onResume` inicia consultas periódicas a cada 750 ms e retoma o preview. `onPause` remove callbacks e pausa o vídeo. `onDestroy` dispensa diálogo e PopupWindow; não cancela a sessão do serviço. Isso evita referências a janelas antigas e preserva o processamento em background.

`onSaveInstanceState` grava jobId e o rascunho dos campos. Um Bundle atende à recriação da tela; estado e comandos duráveis pertencem aos arquivos do job. `onNewIntent` recebe outro ID por notificação e recria a tela para não misturar campos de tarefas diferentes.

## Iniciar e acompanhar

`convertMp4ToWebpAdaptive` impede duplicação durante conversão, verifica capacidade de pacotes e cria uma tarefa com o crop atual. Quatro saídas requerem espaço para quatro figurinhas. A validação de mídia e o encoder ficam no serviço, fora da thread principal.

`refreshJob` carrega uma fotografia do estado, atualiza status, disponibilidade de edição, progresso e resultados. Enquanto running, campos de crop/exportação ficam bloqueados para não mudar a seleção original sem uma tarefa coerente. O formulário de reinício é uma via explícita e separada de intervenção.

`currentAttempt` informa os valores em uso. `lastAttempt` informa os parâmetros e bytes de uma conversão concluída, mesmo que ainda exceda o alvo. O diálogo não associa um tamanho parcial à próxima configuração do algoritmo.

## Diálogo e edição manual

O layout foi reorganizado para distinguir hierarquia de informação: cabeçalho, resumo, cartão da última tentativa, formulário e ações. `last_attempt_size` mostra bytes convertidos para KB separadamente dos metadados. Os textos de estado foram resumidos para evitar repetição da tentativa atual e da mensagem terminal. Campos usam tamanhos explícitos em sp e uma grade compacta, preservando a escala de fonte configurada no aparelho.

A largura nominal de 95% podia coexistir com insets do drawable padrão, reduzindo a área branca visível. O callback de exibição agora usa um GradientDrawable próprio e zera o padding do decor antes de dimensionar a janela. Não se muda o crop nem o algoritmo: esta decisão trata da geometria da janela. O teste no dispositivo verifica a largura e a região visível dos campos e ações.

`createProgressDialog` usa `dialog_compression.xml`, exclusivo de vídeo/GIF. Cabeçalho e ações de reinício/cancelamento ficam fora da área rolável. O diálogo ocupa 95% da largura e 85% da altura para acomodar métricas e formulário; o corpo rola quando necessário. O diálogo compartilhado por imagens permanece separado.

`fillRetryFields` copia valores apenas ao criar o formulário ou por ação explícita. `readRetryDraft` lê os textos. `retryCompression` faz parse, cria `CompressionSettings`, valida duração quando conhecida e envia retry. Se a tarefa terminou, cria outro job e atualiza o ID da tela. Limpa a lista de resultados e desabilita salvar até a nova validação.

`showControlError` mantém uma mensagem de erro de campo, em vez de deixá-la desaparecer no poll seguinte. Um número inválido não deve interromper a sessão. Os controles ficam temporariamente indisponíveis enquanto o comando aguarda aplicação, mas cancelar continua possível durante a espera por reinício.

O listener de Cancelar grava o comando, sem dispensar o diálogo. Quando o serviço publica cancelled, o título e o status mudam, o progresso deixa de ser indeterminado e Reiniciar volta a ficar disponível. Fechar apenas esconde o diálogo; Minimizar manda o app para background.

## Informações por ação explícita

`showCompressionInfo` alterna um PopupWindow ancorado ao ícone “i”. A janela contém avisos de redução extra, informações de background e explicações dos parâmetros. O conteúdo é rolável e a janela aceita dispensa externa. A mensagem não ocupa o corpo fixo do diálogo.

Essa separação é de apresentação, não de algoritmo: esconder uma explicação não altera a decisão da busca. O PopupWindow é destruído com a Activity para evitar uma janela associada a um contexto encerrado.

## Resultado e salvamento

`showResult` monta previews e descrição de uma ou quatro saídas; `setupController` usa Fresco com animação automática e URI com parâmetro de atualização. `outputDirectory` resolve o diretório do job, com fallback para temporários. `getBytes` lê arquivos para operações existentes de validação e cópia.

`openDialogOpcoesVideo` e `resetInputs` mantêm o diálogo de exportação anterior à criação. Ele é distinto do formulário que controla uma busca já iniciada.

`enviarWhatsapp` recarrega o pacote, verifica capacidade e compatibilidade de animação e pode pedir outro pacote. `moverFigurinhasParaPasta` valida limites do WebP, copia arquivos, atualiza catálogo e registra as figurinhas adicionadas. Somente depois consome o job e cancela sua notificação. Isso é uma publicação explícita, diferente de apenas gerar um candidato.

`getNextStickerPrefix` calcula o próximo prefixo numérico a partir de nomes existentes. Ele organiza o nome do arquivo; não é uma garantia geral de exclusão mútua para gravações concorrentes.

## Navegação e limites

Voltar e seta da toolbar chamam `dismissEditor`, evitando recuperação automática daquela tela após uma saída explícita. Não equivalem a cancelar o encoder. A decisão de retomar running/done é avaliada pelo job e pelos pontos de entrada da aplicação.

O teste instrumentado verifica o diálogo e o serviço reais, incluindo tamanho, parâmetros, reinício, cancelamento e PopupWindow. Não testa todas as resoluções, políticas de bateria ou possibilidades de ciclo de vida. O código existente de salvamento também tem responsabilidades amplas; esta tarefa não o reestruturou.

**Exercício:** por que não preencher os EditTexts em todo `refreshJob`? Porque o poll pode ocorrer entre duas teclas e substituir uma edição incompleta. Métricas são atualizadas automaticamente; intenção digitada deve permanecer sob controle do usuário.
