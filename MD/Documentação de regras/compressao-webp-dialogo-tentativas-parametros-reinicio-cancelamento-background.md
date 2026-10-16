# Compressão WebP: tentativas, parâmetros e controle em background

Última atualização: 2026-10-16_17-23 (America/Cuiaba).

## Funcionamento e limite

A tela de crop aguarda uma ação do usuário para criar uma tarefa. `CompressionJob` persiste os dados e `CompressionService` executa o FFmpeg em foreground service. A Activity consulta o estado a cada 750 ms; fechar ou minimizar a tela não interrompe o encoder.

O alvo é até 512000 bytes, apresentados como 500 KB. O serviço valida WebP com 512×512, pelo menos dois quadros, duração até 10 segundos e quadros com pelo menos 8 ms. O tamanho sozinho não demonstra validade. GIF é exportado do original com o intervalo selecionado; MP4 usa o trecho aparado.

## Busca automática

`AdaptiveWebpSearch` começa na qualidade e nos FPS escolhidos. Tenta preservar qualidade e movimento na faixa preferida; quando o piso de qualidade não cabe, reduz FPS na sequência prevista, sem aumentar um FPS inicial inferior. Abaixo do piso, reduz qualidade de um em um até zero e depois detalhes espaciais. A primeira saída válida abaixo do piso encerra a busca. O histórico de tamanhos permite reconstruir a busca após interrupção.

O parâmetro de nitidez define a resolução intermediária de detalhes, seguida de ampliação para a saída de 512×512. Um início manual em 200 não volta a 512 durante a busca; depois de esgotar a qualidade, os próximos níveis de detalhe são inferiores a 200.

## Diálogo

A toolbar do crop de vídeo/GIF não possui mais a opção Edit/Editar. Foram removidos seu botão e o diálogo antigo de opções; os ajustes de uma conversão existente continuam no diálogo de compressão.

O formulário tem o Spinner **Redução** com **Agressivo, Moderado e Suave**, inicialmente Moderado, e o botão **Sugerir**. Mudar a opção preenche uma sugestão; Sugerir permite reaplicar a mesma opção com os dados mais recentes. A seleção inicial/restaurada não modifica os campos automaticamente. O modo escolhido e os rascunhos dos campos são conservados na recriação da Activity.

`CompressionSuggestion` usa o tamanho da última tentativa concluída e seus parâmetros, limitados pelos parâmetros da tentativa atual para não desfazer reduções já em uso. Sem tamanho positivo não altera os campos; se já está em até 512000 bytes, mantém os campos. O cálculo é uma heurística, não uma garantia de tamanho final ou modelo treinado.

O excesso relativo é `1 - 512000/bytes`. A intensidade é esse excesso multiplicado por 1,35 (Agressivo), 0,85 (Moderado) ou 0,40 (Suave), limitada a 0,85. Qualidade, FPS e nitidez são reduzidos proporcionalmente. Qualidade respeita zero; FPS mantém pelo menos 12 quando a taxa de entrada da sugestão era 12 ou maior, sem aumentar valores inferiores; nitidez respeita 1. A velocidade permanece a atual, preservando o tempo do vídeo. Compressão é esforço e sobe um nível até 6, pois reduzi-la não equivale a reduzir o tamanho.

Cada sugestão é calculada novamente a partir dos registros do encoder, não do resultado da sugestão anterior nem de um tamanho parcial. Selecionar outro modo substitui os valores editados nos campos de forma explícita. O usuário pode modificá-los antes de **Reiniciar com estes valores**. O Spinner e Sugerir não enviam comandos ao serviço, não cancelam e não reiniciam a tarefa em andamento. O estado automático continua evoluindo enquanto o formulário é editado.

O resumo da tentativa atual sob a barra usa `Q-75 · 30 FPS · C-6 · N-512 · 1×`, sem quebra de linha forçada entre parâmetros. Q significa qualidade, C esforço de compressão e N nitidez. Essa abreviação se aplica somente ao resumo atual; os rótulos dos inputs e os metadados da última tentativa concluída mantêm seu formato.

A grade organiza Qualidade/FPS na primeira linha e Compressão/Nitidez na segunda; Velocidade ocupa a linha inferior. O ícone de informações é um vetor contornado de 20 dp, com padding dentro de um botão de 48 dp para preservar a área de toque. A ajuda e os parâmetros continuam vinculados aos mesmos IDs.

A barra é determinada desde 0%, com percentual numérico da tentativa atual. O serviço combina estatísticas e timestamps dos quadros filtrados pelo FFmpeg, pois o muxer WebP animado pode não informar tempo intermediário. Usa a duração de saída após velocidade e recorte. Não há avanço simulado pelo relógio, e uma nova tentativa cria um medidor novo.

Dentro da mesma tentativa o percentual não retrocede e é limitado a 99% enquanto o encoder finaliza o arquivo. O estado done grava 100% após validação dos resultados. A porcentagem não estima o conjunto de tentativas nem o tempo restante. A notificação também é determinada em zero.

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
