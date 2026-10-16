# Aula: do compartilhamento Android aos editores existentes

Data: 2026-10-16_11-46 — America/Cuiaba.

## 1. O problema e as duas responsabilidades

Antes, o usuário iniciava a importação dentro do app. Faltava uma porta de entrada para arquivos enviados por outros aplicativos. A solução separa transportar os bytes de decidir como editá-los. `SharedMediaImport` cuida do transporte; `ShareMediaActivity` organiza a escolha; os editores existentes continuam responsáveis por recorte e compressão.

Essa separação evita duplicar o algoritmo de compressão. Uma nova entrada não exige uma segunda implementação de toda a funcionalidade. O fluxo é: remetente → URI concedida → cópia privada → escolha do pacote → editor existente.

## 2. Intent não é o arquivo

Intent é uma mensagem do Android. ACTION_SEND descreve a intenção de compartilhar; o MIME descreve o tipo; EXTRA_STREAM normalmente contém uma URI que identifica os bytes. O Manifest registra quais mensagens o app deseja receber. `android:exported="true"` permite que outro app alcance essa Activity.

Uma URI content não é um caminho no disco. Ela identifica um recurso servido por um provedor. Tentar obter um suposto caminho real falha com provedores em nuvem e armazenamento restrito. Por isso `openInputStream(uri)` pede os bytes ao provedor, respeitando sua permissão. O nome retornado por DISPLAY_NAME serve para a interface; não controla o nome do arquivo privado.

O filtro do Manifest não substitui validação. Uma chamada explícita pode alcançar a Activity com dados errados. A importação verifica ação, URI, esquema e tipo. Falhas são convertidas em orientação para compartilhar novamente. Não houve investigação de um crash anterior: trata-se de implementação de uma funcionalidade nova.

## 3. Copiar para controlar a vida útil

O acesso concedido pelo outro app não deve ser a base de um processamento longo. A cópia privada transforma um recurso externo temporário em um arquivo pertencente ao receptor. `UUID.randomUUID()` fornece um nome independente do remetente, evitando colisões e uso de nomes externos como caminhos.

O laço usa um buffer de 65.536 bytes: lê um bloco, verifica interrupção e escreve. Para n bytes, o trabalho de cópia é O(n), e o buffer usa memória constante. Não há carregamento do vídeo inteiro em RAM. `sync()` solicita a sincronização do arquivo antes de seguir. Isso não torna o fluxo inteiro transacional: por exemplo, um encerramento abrupto ainda pode deixar arquivos órfãos.

Pseudocódigo:

```text
validar mensagem e URI
abrir entrada e arquivo privado
enquanto houver bytes:
    se interrompido: falhar
    escrever bloco
sincronizar e fechar
se vazio: falhar
se assinatura GIF: usar extensão .gif e tipo gif
retornar arquivo, tipo e nome
em falha: remover parcial
```

Os seis primeiros bytes GIF87a/GIF89a distinguem GIF de uma foto comum. Isso importa porque GIF deve selecionar pacote animado e preservar o fluxo específico existente. A extensão de saída de vídeos é uma convenção para o helper, não uma conversão para MP4; os bytes continuam os mesmos e a capacidade real depende dos decodificadores existentes.

## 4. Interface, estado e concorrência

A thread principal desenha e recebe toques. Copiar um vídeo nela bloquearia a tela. `Executors.newSingleThreadExecutor()` agenda cópia e leitura do catálogo fora dela; `runOnUiThread` aplica o resultado visual. Uma thread também serializa essas tarefas, reduzindo sobreposição desnecessária. `loadingPacks` evita consultas duplicadas enquanto uma consulta está em andamento.

A Activity pode desaparecer enquanto o trabalho termina. As verificações `isDestroyed()` e `isFinishing()` evitam atualizar uma tela morta. Se a cópia terminou para uma tela já encerrada, ela é descartada. `shutdownNow()` pede interrupção no encerramento; interrupção é cooperativa, não garante interromper imediatamente uma leitura bloqueada pelo provedor.

Recriar a tela destrói a instância Java. Por isso o Bundle guarda valores simples, como importedPath e pack. O arquivo permanece no armazenamento privado. Na próxima instância, `new File(path).isFile()` confirma sua existência antes de reutilizar. O teste remove a URI externa, recria a tela e ainda abre o editor: isso verifica a independência da fonte após a cópia. Não prova sobrevivência a desinstalação ou remoção manual de dados.

## 5. Regras do destino e reutilização

O predicado `pack.animatedStickerPack == !"image".equals(media.kind)` combina tipo da mídia com tipo do pacote. O limite `size() < 30` exclui pacotes cheios. Por exemplo: PNG + pacote animado é rejeitado; GIF + pacote animado com 29 itens é aceito; o mesmo pacote com 30 itens é excluído.

`openEditor` transforma a escolha em chamada ao fluxo existente. Foto usa Intent explícita para CropImageActivity. Vídeo/GIF usa FileExplorerHelper com a classe desejada. Essa colaboração mantém preparação de GIF e regras de duração em um único lugar. Consequentemente, escolher Cortar em um vídeo longo ainda pode exigir aparar primeiro.

`StickerApplication` observa o retorno ao primeiro plano para recuperar jobs. Essa política global poderia tomar a tela do compartilhamento. A exclusão explícita de ShareMediaActivity dá prioridade à nova ação do usuário sem remover a recuperação nas demais telas.

## 6. Validação, alternativas e exercícios

Os testes instrumentados executam APIs Android reais, ao contrário de testes unitários Java isolados. ActivityMonitor observa qual editor foi aberto; performClick aciona o botão na thread principal. Arquivos de teste são publicados no MediaStore e depois removidos. Eles validam integração e navegação, mas não certificam todos os provedores, codecs ou resultados finais do encoder.

Uma alternativa seria persistir a permissão da URI. Isso depende do contrato do provedor e não pode ser presumido para um ACTION_SEND genérico. Outra seria colocar a cópia em cache; a limpeza do sistema tornaria seu tempo de vida menos previsível para tarefas longas. Usar files exige espaço e uma futura política de limpeza; a implementação prefere conservar entradas já encaminhadas ao editor para não quebrar jobs.

**Exercício:** por que não usar DISPLAY_NAME diretamente no caminho? **Resposta:** o nome pertence ao remetente e pode colidir ou conter estrutura indevida; UUID separa identidade interna e apresentação.

**Exercício:** se GIF tiver nome foto.png, qual pacote aparece? **Resposta:** a assinatura dos bytes define gif, então aparecem pacotes animados disponíveis.

**Exercício:** o MIME video/* garante que qualquer vídeo funcionará? **Resposta:** não. É um contrato de entrada amplo; o conteúdo ainda precisa ser lido e decodificado pelas bibliotecas do app.

[Registro e validações](../Documentação%20das%20alterações/2026-10-16_11-46_receber-compartilhamento-de-midias-do-android.md) · [Regras atuais](../Documentação%20de%20regras/compartilhamento-android-fotos-videos-gif-uris-pacotes-aparar-cortar.md)
