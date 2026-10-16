# Corrigir abertura recorrente na tela de crop

## Causa

Tanto a Splash quanto o callback de retorno ao foreground abriam qualquer tarefa pendente, inclusive as que terminaram com erro. O botão Voltar também não registrava a intenção de sair do editor, deixando a tarefa apta a redirecionar em toda abertura.

## Correção

- `CompressionJob.java`: centraliza a regra de recuperação automática: somente tarefas em andamento ou concluídas, desde que o usuário não tenha saído explicitamente do editor. A saída fica persistida por UUID em SharedPreferences, separada dos checkpoints escritos pelo serviço para evitar que uma gravação de progresso a sobrescreva.
- `StickerApplication.java` e `SplashActivity.java`: usam a regra comum. Tarefas com erro não redirecionam a abertura do app.
- `CropVideoActivity.java`: Voltar do Android e seta da toolbar registram a saída explícita. Isso não cancela processamento, apaga resultados nem remove o acesso pela notificação. Minimizar mantém o comportamento de recuperação automática.
- `BackgroundCompressionTest.java`: regressão para tarefas running/done/error, persistência da saída após checkpoint do serviço, manutenção do estado/notificação e recuperação de uma nova tarefa com UUID diferente. Preserva a tarefa anterior do usuário.

## Validação

Builds debug/release e 22 testes unitários passaram. O teste instrumentado `errorsAndDismissedEditorsDoNotHijackAppReturn` passou no celular USB. Release instalada preservando os dados; a abertura chegou a `StickerPackListActivity` mesmo mantendo o registro antigo de erro. Nova invocação da abertura manteve a listagem, sem redirecionar para crop. Sem novos erros AndroidRuntime na verificação. Pacote de testes removido. Release local usa o certificado já instalado, sem limpar dados; build mantém a exceção existente `-x checkRelease`.
