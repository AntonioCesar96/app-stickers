# SelectedStickers: arquivos, identidade e publicação das coleções

Código: `app/src/main/java/com/example/samplestickerapp/SelectedStickers.java`.
Atualização: 2026-10-16_16-55 — America/Cuiaba.

## Responsabilidade e fundamentos

Esta classe final, com visibilidade de pacote, aplica regras da coleção sem depender de Android Views ou Context. Recebe File como raiz explícita, o que permite usar armazenamento real no app e TemporaryFolder nos testes. Não guarda uma lista em memória entre chamadas; o estado persistente está no filesystem. A Activity é responsável por executar fora da thread principal e publicar o resultado no catálogo.

MARKER é o nome `.selected-stickers`. `isCollection` verifica sua existência como arquivo. É a identidade da funcionalidade; um nome parecido não basta. Result transporta identifier, count, selectedCount, fillerCount e duplicate como campos finais. Seu construtor conta os WebP e classifica temporários pelos bytes atuais; não mantém contador paralelo que possa divergir do disco. A interface Templates fornece names/open para ler amostras sem acoplar esta classe ao Android.

## add: do pedido à decisão

`static synchronized Result add(...)` torna a classe um ponto de serialização no processo. Duas chamadas não escolhem a mesma vaga simultaneamente entre si. Não é um bloqueio entre processos nem com outras classes gravadoras. Os parâmetros identificam assets, origem, nome e tipo. CanonicalFile resolve os caminhos; as comparações de pais proíbem sair da raiz e do pacote. Em seguida exige arquivo não vazio, extensão WebP e tipo compatível com animado.txt.

`digest(input)` calcula SHA-256 por streaming. O diretório é listado e ordenado por nome para escolha determinística. Para cada coleção do tipo, compara hashes apenas das escolhas permanentes. Se achar igualdade, chama reconcile sem entrada nova para garantir preenchimento e retorna duplicate=true. Também lembra a primeira com menos de 30, mas continua procurando duplicatas nos demais diretórios. Uma cópia temporária não impede a escolha explícita da mesma imagem.

Essa ordem importa: suponha coleção A com 2 e coleção B com 30 contendo a entrada. Parar em A faria duplicação; continuar até B reconhece a seleção anterior. Comparação de hash identifica bytes iguais, não semelhança visual. Uma nova versão recodificada pode ser considerada nova mesmo parecendo igual.

## Criação e inserção

Sem destino com vaga, usa nome conforme o tipo e procura sufixo numérico livre. A presença de um pacote comum com esse nome só causa escolha de outro nome; não altera o pacote existente. Prepara um diretório `.selected-UUID` irmão de assets. Copia a escolha como `01_hash.webp`, copia icone.png da origem, prepara as duas amostras necessárias e cria os marcadores da coleção, versão e tipo. Só depois renomeia o diretório para assets, já com três figurinhas.

Com destino existente, reconcile separa escolhas e temporários, prepara a entrada nova se houver e calcula as amostras desejadas. nextIndex mantém a convenção numérica do scanner. Novos arquivos são publicados antes de retirar os temporários excedentes para staging; a versão é incrementada quando algo muda. published e removed registram os efeitos para desfazer em erro. O finally restaura retirados e apaga publicações novas se committed não ficou true. Em falha da restauração, conserva staging e informa seu caminho. A origem nunca é removida.

## Preparação e identificação dos temporários

FILLER_PREFIX é `00_selected-filler_`. isFiller exige esse prefixo, 64 dígitos hexadecimais, extensão WebP e igualdade com o SHA-256 atual. Uma inicial alterada externamente passa a permanente para evitar sua exclusão indevida. Esse padrão é uma convenção reservada da coleção, não uma assinatura de autenticação.

prepareFillers calcula `max(0, 3 - selected.size())`. Com três escolhas, retorna sem acessar templates. Com uma ou duas, coleta hashes permanentes, ordena as amostras fornecidas e copia candidatos por InputStream. Só aceita hashes distintos das escolhas e dos candidatos anteriores. Retorna os nomes desejados quando alcança o necessário; sem quantidade suficiente, lança IOException antes de publicar. O candidato temporário usa template.tmp, que não é reconhecido como figurinha pelo scanner.

Simulação: uma escolha A e temporários B/C. Adicionar D produz A/D e um temporário, mantendo total três. Adicionar E produz A/D/E e nenhum temporário. Clicar de novo em A não muda a contagem de escolhas, mas repara os temporários se faltarem. Clicar explicitamente em B quando ele era preenchimento cria uma escolha permanente B; a cópia de preenchimento correspondente pode ser retirada.

copy tem uma entrada File e outra InputStream: ambas usam buffer de 16 KiB e sincronizam o descritor de saída. O chamador fecha a entrada fornecida. createMarker exige criação; bumpVersion lê v_N.txt, rejeita estouro, cria N+1 e remove antigos. Em falha propaga para a reversão. cleanup elimina o staging quando a operação terminou ou foi desfeita; se a reversão falha, ele é preservado para não apagar dados recuperáveis.

## Custos e limites de recuperação

Para entrada com n bytes e coleções com s bytes examinados, hashing custa O(n+s). Copiar adiciona O(n); o buffer é constante, mas os arrays de diretórios/arquivos crescem com sua quantidade. O hash aparece no nome de saída para identificação legível, mas a deduplicação recalcula o conteúdo em vez de confiar num nome que pode ter sido alterado externamente.

As rotinas restauram o que conseguem em falhas normais; não fornecem journal para morte abrupta, garantia de exclusão em armazenamento defeituoso ou transação com contents.json. O catálogo é atualizado posteriormente pela Activity. Um staging residual fica fora de assets, evitando ser interpretado como pacote, mas pode ocupar disco até limpeza manual. Essas escolhas são uma integração pequena com o armazenamento atual, e não um banco de dados novo.

## Validação e exercícios

SelectedStickersTest cobre dez cenários, incluindo 31 escolhas, falha de versão/templates, promoção de inicial, duplicata reparadora e preservação de preenchimento modificado. SelectedStickersDeviceTest verifica as amostras reais dos dois tipos no Android em raiz isolada. Nenhum desses testes confirma exportação real no WhatsApp.

**Pergunta:** por que o helper recebe assets, em vez de buscar FilesHelper internamente? **Resposta:** separa política de armazenamento e ambiente, facilita testes e evita Context Android nessa lógica.

**Pergunta:** copiar bytes garante preservar a animação? **Resposta:** preserva exatamente a codificação original; sua validade e reprodução ainda dependem do conteúdo original e do decodificador.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
