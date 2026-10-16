# Compressão WebP: tentativas, parâmetros e controle em background

Última atualização: 2026-10-16_10-54 (America/Cuiaba).

## Funcionamento e limite

A tela de crop aguarda uma ação do usuário para criar uma tarefa. `CompressionJob` persiste os dados e `CompressionService` executa o FFmpeg em foreground service. A Activity consulta o estado a cada 750 ms; fechar ou minimizar a tela não interrompe o encoder.

O alvo é até 512000 bytes, apresentados como 500 KB. O serviço valida WebP com 512×512, pelo menos dois quadros, duração até 10 segundos e quadros com pelo menos 8 ms. O tamanho sozinho não demonstra validade. GIF é exportado do original com o intervalo selecionado; MP4 usa o trecho aparado.

## Busca automática

`AdaptiveWebpSearch` começa na qualidade e nos FPS escolhidos. Tenta preservar qualidade e movimento na faixa preferida; quando o piso de qualidade não cabe, reduz FPS na sequência prevista, sem aumentar um FPS inicial inferior. Abaixo do piso, reduz qualidade de um em um até zero e depois detalhes espaciais. A primeira saída válida abaixo do piso encerra a busca. O histórico de tamanhos permite reconstruir a busca após interrupção.

O parâmetro de nitidez define a resolução intermediária de detalhes, seguida de ampliação para a saída de 512×512. Um início manual em 200 não volta a 512 durante a busca; depois de esgotar a qualidade, os próximos níveis de detalhe são inferiores a 200.

## Diálogo

A janela tem 95% da largura, com fundo próprio sem os insets extras do tema, e 85% da altura. Cabeçalho e ações ficam fixos; conteúdo central pode rolar. O resumo atual é compacto, o tamanho da última saída tem um cartão próprio e os cinco parâmetros usam uma grade com tipografia explícita. Não há repetição do status nos campos de ajuda e de ação. A barra de progresso é ocultada quando a tarefa não está em andamento.

O diálogo mostra progresso, configurações da tentativa atual e tamanho/configurações da última tentativa concluída. Os registros têm número, quadrante, qualidade, FPS efetivo, compressão, velocidade e nitidez. Uma tentativa abortada não substitui a última concluída, pois seu arquivo pode estar incompleto.

Campos permitem qualidade 0–100, FPS 1–60, compressão 0–6, velocidade positiva e nitidez 1–512. A velocidade precisa manter a duração resultante até 10 segundos. FPS solicitado pode ser limitado pela taxa da fonte. Compressão maior não significa reduzir qualidade: aumenta o esforço do encoder e pode demorar mais.

Os campos não acompanham automaticamente cada alteração do algoritmo, para não apagar edições do usuário. “Copiar valores da tentativa atual” preenche-os explicitamente. O ícone de informações no canto superior direito abre um PopupWindow com as mensagens de redução extra, background e explicações dos controles. Os avisos extensos não ficam no corpo do diálogo nem na notificação.

## Reinício manual e cancelamento

“Reiniciar” valida os campos, grava um comando em `control.json` e interrompe a tentativa em andamento. O serviço consulta comandos em intervalos de 100 ms, cancela somente sua sessão nativa e aguarda o callback antes de reutilizar `candidate.webp`. O intervalo de consulta não é uma promessa de parada completa em 100 ms.

O reinício zera o histórico da busca e os resultados anteriores, preservando fonte, intervalo e crop. Começa nos parâmetros informados e continua automaticamente. No modo de quatro figurinhas, todas são refeitas, evitando misturar resultados de configurações diferentes.

“Cancelar” grava um comando persistente, para a sessão e marca a tarefa como `cancelled`. O diálogo permanece aberto para ajustar valores e tentar novamente. Cancelamento tem prioridade sobre reinício pendente. Tarefas canceladas não forçam retorno automático ao editor, mas seu registro continua disponível.

Depois de um estado terminal, tentar novamente cria um novo UUID e copia as entradas duráveis antes de converter. Isso isola a nova execução dos callbacks de encerramento da anterior. Os registros privados anteriores permanecem disponíveis; não há limpeza retroativa de tarefas neste recurso.

## Persistência e limites

Estado e comandos usam AtomicFile e arquivos separados. Checkpoints do encoder não sobrescrevem pedidos da interface. Uma revisão crescente identifica comandos aplicados, permitindo repetir com segurança a aplicação após morte do processo. O comando só é aceito enquanto a tarefa ainda está running; a conclusão usa o mesmo bloqueio da requisição para resolver essa disputa.

Cancelar não publica um arquivo parcial como resultado. Reiniciar não salva figurinhas no pacote: salvar continua sendo uma ação explícita e mantém as validações existentes. O código responsável está em `CompressionJob`, `CompressionService`, `CompressionSettings`, `AdaptiveWebpSearch` e `CropVideoActivity`, no pacote `com.example.samplestickerapp`.
