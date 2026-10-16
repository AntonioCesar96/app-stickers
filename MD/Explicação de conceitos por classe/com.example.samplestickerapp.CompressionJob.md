# CompressionJob: estado durável e comandos independentes

Última atualização: 2026-10-16_10-40 (America/Cuiaba).

Código: `app/src/main/java/com/example/samplestickerapp/CompressionJob.java`.

## O que um job representa

Um job reúne identidade, arquivos e dados de uma criação de uma ou quatro figurinhas. O UUID identifica a tarefa e seu diretório privado. `data` é um JSONObject com fontes, intervalo, crop, parâmetros, resultados, tentativas e estado. A classe não guarda Views, Activity ou sessão nativa, evitando ligar a duração do trabalho à existência da tela.

`create` verifica se há tarefa running antes de criar outra. Converte os dados relevantes do pacote e de suas figurinhas em JSON explícito, preservando-os para reconstrução sem depender de uma referência Parcelable viva. O identificador pendente fica em SharedPreferences; o conteúdo completo fica no diretório do job.

## Coordenadas normalizadas e reconstrução

O crop é dividido pela largura da tela antes de persistir. Isso transforma coordenadas em proporções. `crop(width)` multiplica essas proporções pela largura desejada e arredonda, permitindo aplicar a mesma seleção ao quadrado da mídia ou a uma tela com outra largura.

`screen(context)` reconstrói pacote, figurinhas e Intent a partir do JSON e aponta para `CropVideoActivity`. Dados opcionais de GIF incluem origem e intervalo. A intenção explícita permite à notificação reabrir a tarefa correta.

## Gravação atômica e bloqueio

`save` usa AtomicFile: inicia a escrita, grava UTF-8 e confirma com `finishWrite`; em erro, chama `failWrite`. `load` recupera o JSON ou retorna null se não conseguir interpretá-lo. Isso protege contra arquivos de estado parcialmente substituídos, mas não valida toda a semântica dos parâmetros.

O bloqueio de `CompressionJob.class` serializa operações sobre arquivos dentro do processo. O bloqueio não é uma transação entre processos nem torna toda mutação do JSONObject automaticamente segura. O desenho depende de o serviço ser o escritor principal de `state.json` e a Activity trabalhar com fotografias carregadas separadamente.

## Canal de comandos

`control` lê `control.json` por AtomicFile; ausência retorna um objeto vazio. Usar a leitura do AtomicFile, em vez de testar somente a existência do arquivo base, permite a recuperação da cópia de segurança após uma gravação interrompida.

`request` aceita cancelamento ou reinício com `CompressionSettings`. Sob o bloqueio comum, relê o estado para confirmar running, valida a duração quando conhecida, lê o comando anterior e grava uma revisão maior. Um cancelamento já solicitado impede outro reinício até terminar. A UI não regrava uma fotografia antiga do estado do serviço.

`putSettings` centraliza a representação JSON: alguns parâmetros continuam strings por compatibilidade com o formato existente, enquanto detalhe é numérico. Não há serialização automática de campos pelo Gson nesta classe.

## Reinício terminal e isolamento

`restart` copia os dados de uma tarefa terminada para outro UUID, substitui parâmetros e zera resultados, trials e revisão aplicada. As fontes ainda apontam ao diretório anterior até o serviço copiá-las. Marcar `inputsCopied=false` exige essa cópia antes da conversão.

Um novo identificador evita confundir os callbacks finais da tarefa antiga com a nova. A última tentativa concluída pode permanecer visível como comparação; isso não significa reutilizar seu tamanho como trial da busca reiniciada. Os arquivos antigos não são apagados retroativamente por este método.

## Finalização e navegação

`running` e `done` consultam status. `shouldRestoreAutomatically` só permite running/done quando o usuário não dispensou o editor. `dismissEditor` registra essa escolha em preferências separadas do estado, evitando sobrescrita por um checkpoint de progresso.

`consume` remove a referência pendente quando corresponde ao ID e apaga somente arquivos planos da tarefa terminada. Não apaga figurinhas já salvas em armazenamento compartilhado. `result(index)` define os nomes de saída de uma ou quatro figurinhas. `copy` usa um arquivo `.partial`, sincroniza os dados e renomeia ao concluir, evitando publicar uma cópia incompleta como destino final.

## Exemplo, teste e exercício

Se a UI lê progresso 20% e depois o serviço grava 30%, um comando em arquivo separado não restaura acidentalmente os 20%. Esse é o motivo central para separar intenção e fotografia do estado.

Os testes instrumentados exercitam comandos, reinício e conservação de pendência. Isso não elimina riscos de falta de espaço ou corrupção de armazenamento, tratados como falhas de leitura/escrita.

**Exercício:** por que `restart` não pode apenas reutilizar `trials`? Porque esses tamanhos foram produzidos por configurações anteriores e levariam o algoritmo a decisões sem evidência correspondente aos parâmetros novos.
