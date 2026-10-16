# StickerApplication: inicialização e retorno ao primeiro plano

Atualização: 2026-10-16_11-46 — America/Cuiaba.
Código: `app/src/main/java/com/example/samplestickerapp/StickerApplication.java`.

## Fundamentos

Application é o objeto de inicialização do processo Android. Não é uma tela. `onCreate` inicializa Fresco, biblioteca usada para imagens, disponibiliza contexto a FilesHelper e registra ActivityLifecycleCallbacks. Esses callbacks observam o ciclo de vida das Activities do processo e permitem uma política comum de recuperação de compressões.

## Contagem e decisão

O callback mantém started e returning. Ao iniciar uma Activity, `if (started++ == 0)` detecta a transição de nenhuma tela iniciada para uma tela iniciada. started++ compara o valor anterior e depois incrementa. Em onActivityStopped, o contador diminui sem ficar negativo. Ao retomar, returning é consumido uma vez: não se repete recuperação em cada callback da mesma entrada no primeiro plano.

Exemplo: app em segundo plano tem started=0. Uma tela inicia: o contador passa a 1 e returning=true. Ao retomar, a política é avaliada e returning=false. Navegar entre telas já iniciadas não deve disparar o mesmo processo a cada passo. Essa contagem usa eventos de Activity; não representa a execução do serviço de compressão.

## Recuperação e exceções de navegação

CropVideoActivity já é o editor de processamento, SplashActivity controla sua própria entrada e ShareMediaActivity representa uma nova ação explícita do usuário. Essas telas retornam antes da recuperação global. A mudança atual acrescentou a tela de compartilhamento a essa lista, evitando sobrepor a escolha de edição com um crop anterior.

Nas outras telas, CompressionJob.pending recupera o estado persistido. shouldRestoreAutomatically decide se o job permite retorno automático, incluindo as regras de descarte do editor. Se aplicável, pending.screen constrói a Intent. CLEAR_TOP permite voltar à instância adequada da pilha; SINGLE_TOP permite reutilizá-la quando já estiver no topo. A Activity de destino precisa tratar seu próprio estado; a Application apenas encaminha.

Uma exceção ao abrir é registrada com Log.w. Isso evita quebrar o retorno ao app, mas não corrige por si só um job inválido. Os outros callbacks obrigatórios permanecem vazios porque a política não necessita de seus eventos.

## Arquitetura, limites e validação

Centralizar a observação evita copiar recuperação para todas as telas. Em contrapartida, uma política global precisa respeitar entradas novas: compartilhamento e notificações têm intenção própria. A exclusão local mantém a recuperação nos lugares anteriores, sem apagar job ou cancelar serviço. O código executa nos callbacks de interface; a classe não cria threads nem executa FFmpeg.

Os testes de compartilhamento verificam a navegação real, mas não constroem uma matriz completa de jobs pendentes. Para validar a regra específica manualmente, retorne ao app com um job restaurável e depois abra um novo compartilhamento: a segunda ação deve permanecer na seleção da mídia.

**Exercício:** excluir ShareMediaActivity cancela a compressão anterior? **Resposta:** não. Apenas impede o encaminhamento automático nesse callback; serviço e estado persistido têm responsabilidades separadas.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_11-46_receber-compartilhamento-de-midias-do-android.md)
