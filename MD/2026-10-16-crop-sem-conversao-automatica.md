# Escolher a área de crop antes de criar a figurinha

## Solicitação e alteração

O usuário quase sempre ajusta a área antes de gerar a figurinha, tornando a conversão automática inicial desnecessária.

- `CropVideoActivity.java`: ao preparar uma nova edição, exibe vídeo e controles sem chamar a conversão. O usuário pode mover/redimensionar a área e ajustar opções antes de tocar no botão de crop ou de gerar quatro figurinhas. Salvar permanece desabilitado até existir resultado. Vale para vídeo e preview de GIF.
- A recuperação de uma tarefa existente mantém o comportamento anterior: restaura área, andamento ou resultado sem disparar uma conversão nova.
- `BackgroundCompressionTest.java` e `GifPipelineTest.java`: adaptados ao início manual; aguardam controles visíveis e acionam o botão antes de esperar pelo resultado. O teste de background também verifica que abrir o editor não cria tarefa.

## Validação

Compilação da release e do APK de testes instrumentados concluída; 22 testes unitários passaram. Conferido que as únicas chamadas de criação na Activity permanecem nos listeners dos dois botões. Release local instalada por atualização no celular USB preservando dados, usando o certificado existente e `-x checkRelease` pela restrição do identificador de exemplo. Os testes instrumentados foram atualizados e compilados, mas não executados nesta tarefa.
