# ContentsJsonHelper: reconstruir catálogo a partir dos diretórios

Código: `app/src/main/java/com/example/samplestickerapp/ContentsJsonHelper.java`.
Atualização: 2026-10-16_17-04 — America/Cuiaba.

## Modelo persistente e comunicação em memória

O projeto guarda pacotes em diretórios e reconstrói contents.json para o ContentProvider. Esta classe concentra leitura dos diretórios, construção de modelos, escrita JSON, versão e remoção. Os campos estáticos stickerPackAlterado, packsChanged e stickersAlterados sinalizam mudanças entre telas no processo; não são persistência confiável após encerramento. Os arquivos e JSON são a parte persistente.

## Ler os diretórios

getStickerPacks() obtém a raiz real por FilesHelper e delega a getStickerPacks(File). A sobrecarga recebe raiz explícita para poder executar a mesma construção em armazenamento isolado nos testes. Não muda a origem de produção.

O scanner lista subdiretórios, ordena pela data de modificação e inspeciona animado.txt, v_N.txt e arquivos WebP. Sem marcador de versão, usa 1; a verificação agora exige array não nulo e não vazio antes de acessar [0]. Uma pasta legível sem esse marcador retorna array vazio, que é diferente de null.

Os nomes WebP são ordenados pelo prefixo numérico antes de underscore. Essa convenção explica por que SelectedStickers usa índices e preenchimento com prefixo 00. Nomes fora dessa convenção podem causar erro de conversão; a tarefa não substituiu a organização existente.

Cada Pasta intermediária vira StickerPackModel com identificador, nome público, editor, ícone, contato, versão e tipo. O scanner limita a 30 figurinhas e atribui os emojis padrão existentes. Para pacotes comuns, nome continua sendo o diretório. Para diretórios com .selected-stickers, usa SelectedStickers.displayName: título Individuais e sufixos +1/+2, sem trocar identifier.

Isso mantém duas funções distintas: o identificador localiza os arquivos e o título é a apresentação externa. Todos os modelos, inclusive individuais, permanecem no catálogo. Quem filtra a interface é StickerPackLoader, depois da leitura do provider.

## Escrever e anunciar mudanças

atualizaContentsJson constrói ContentsModel com a lista e links gerais e serializa com Gson. FileWriter escreve contents.json. O código existente captura exceções e imprime o erro; a gravação não é uma transação atômica com todas as alterações de arquivos. Uma escrita falha pode ser percebida mais tarde ao recarregar. A tarefa não migrou o catálogo para banco ou journal.

updatePack lê a versão v_N.txt, remove o marcador antigo, cria N+1 e regenera o catálogo. A versão é codificada no nome do marcador; seu conteúdo não é a fonte da leitura. Esse método preexistente tem pressupostos sobre existência/validade da versão. SelectedStickers tem sua própria atualização mais defensiva na etapa de cópia; a exportação ainda utiliza este helper como antes.

atualizaContentProvider obtém ContentProviderClient pela authority e, quando é local e do tipo esperado, chama clearCache. Essa operação força o provider a reler o JSON e notificar mudanças. O client é liberado depois. Atualizar o cache local não garante o instante de atualização do cache pertencente ao WhatsApp.

Os métodos atualizaContentsJsonAndContentProvider e updatePackAndContentProvider combinam essas etapas com tratamento visual de erro por Toast. Eles não fornecem um resultado transacional ao chamador. Seus limites preexistentes precisam ser considerados quando se descreve uma operação como concluída.

## Remover dados

removerFigurinha e removerFigurinhas apagam os arquivos indicados, atualizam versão e invalidam o provider. removerPacote chama deleteRecursive e regenera catálogo. deleteRecursive percorre filhos antes de apagar o diretório: é uma travessia em profundidade. A função não verifica todos os resultados de delete; portanto sua conclusão não é uma garantia de remoção bem-sucedida em qualquer armazenamento.

Esses métodos recebem identificadores/nomes já fornecidos pelos fluxos internos. Esta tarefa não redesenhou a validação de caminhos da classe. A ação individual preserva suas fontes e faz a preparação por SelectedStickers, que valida caminhos canônicos antes de copiar.

## Custos e validação

Reconstruir o catálogo percorre diretórios e arquivos e ordena os conjuntos: em termos gerais, O(p log p + soma de f log f), com p pacotes e f arquivos por pasta. Não lê todos os bytes das imagens nessa fase; o loader/validador fazem isso depois. Gson e os modelos ocupam memória proporcional à descrição do catálogo.

O teste Android usa getStickerPacks(File) numa raiz temporária com pacote comum e individuais para verificar os dois modelos, o título público e a identidade preservada. Depois testa que a projeção visual exclui apenas o interno. Não sobrescreve o contents.json real do usuário nem confirma inclusão no WhatsApp.

**Exercício:** trocar apenas o nome público exige renomear o diretório? **Resposta:** não; name e identifier têm responsabilidades diferentes. Mantê-los separados permite mudar a apresentação sem invalidar a referência persistente usada pelo destinatário.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_17-04_adicionar-individuais-diretamente-no-whatsapp.md)
