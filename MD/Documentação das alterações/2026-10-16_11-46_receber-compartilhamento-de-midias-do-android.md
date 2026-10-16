# Receber mídias compartilhadas pelo Android

Data: 2026-10-16_11-46 — America/Cuiaba.

O aplicativo passa a aparecer no compartilhamento de um arquivo de imagem ou vídeo. A nova `ShareMediaActivity` permite selecionar um pacote compatível e abrir os editores existentes. Vídeos e GIFs oferecem Aparar duração e Cortar enquadramento; fotos oferecem Cortar foto.

## Implementação e motivo

- `AndroidManifest.xml`: registra uma entrada exportada ACTION_SEND para image/* e video/*.
- `SharedMediaImport.java`: lê uma URI content concedida pelo remetente e copia os bytes para files/shared_imports. Identifica GIF pela assinatura, usa nomes UUID e remove cópias incompletas. Isso evita depender de caminhos externos ou da duração da permissão de leitura.
- `ShareMediaActivity.java` e `activity_share_media.xml`: importação e consulta dos pacotes fora da thread de interface, seleção do destino, mensagens de permissão/ausência de pacotes, restauração do arquivo e pacote na recriação e encaminhamento aos editores.
- `StickerApplication.java`: impede que a recuperação global de uma compressão antiga desvie a entrada de compartilhamento para outro editor.
- `ShareMediaTest.java`: três testes instrumentados cobrem resolução de MIME, foto, vídeo, importação de GIF por ClipData e restauração com o original externo removido.

## Validações e limites

Os três testes instrumentados passaram no Android conectado (5,592 s). Eles abriram os editores reais e verificaram a compatibilidade dos pacotes, sem adicionar figurinhas ao catálogo. O GIF foi validado na camada de importação; o teste não exporta uma figurinha GIF completa. As compilações debug e release e os 28 testes unitários passaram. A release foi alinhada, assinada com a chave local existente, verificada e instalada com atualização preservando os dados. O APK de instrumentação foi removido depois dos testes. O PackageManager confirmou os filtros image/* e video/* na versão instalada.

A compilação release usou `-x checkRelease` porque a verificação do projeto modelo recusa o identificador de exemplo já utilizado pelo app. O identificador e a verificação foram preservados. A assinatura local permite atualizar a instalação atual; não é uma configuração de publicação em loja.

Aceita um arquivo por compartilhamento; links de redes sociais e ACTION_SEND_MULTIPLE não são suportados. O mecanismo existente pode encaminhar vídeos longos para aparar antes do crop. A cópia recebida não exige acesso geral ao armazenamento, mas o catálogo de pacotes usa as permissões existentes. Cópias encaminhadas aos editores podem permanecer privadas para não invalidar processamento em andamento; não foi criada uma política de expiração automática.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_11-46_receber-compartilhamento-de-midias-do-android.md) · [Regras](../Documentação%20de%20regras/compartilhamento-android-fotos-videos-gif-uris-pacotes-aparar-cortar.md)
