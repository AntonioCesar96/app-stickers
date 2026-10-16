# SelectedStickers: arquivos, identidade e publicação das coleções

Código: `app/src/main/java/com/example/samplestickerapp/SelectedStickers.java`.
Atualização: 2026-10-16_16-38 — America/Cuiaba.

## Responsabilidade e fundamentos

Esta classe final, com visibilidade de pacote, aplica regras da coleção sem depender de Android Views ou Context. Recebe File como raiz explícita, o que permite usar armazenamento real no app e TemporaryFolder nos testes. Não guarda uma lista em memória entre chamadas; o estado persistente está no filesystem. A Activity é responsável por executar fora da thread principal e publicar o resultado no catálogo.

MARKER é o nome `.selected-stickers`. `isCollection` verifica sua existência como arquivo. É a identidade da funcionalidade; um nome parecido não basta. A classe interna Result transporta identifier, count e duplicate como campos finais. Seu construtor conta os WebP atuais do diretório: resultado inclui o estado observável após a operação, sem manter um contador paralelo que poderia divergir do disco.

## add: do pedido à decisão

`static synchronized Result add(...)` torna a classe um ponto de serialização no processo. Duas chamadas não escolhem a mesma vaga simultaneamente entre si. Não é um bloqueio entre processos nem com outras classes gravadoras. Os parâmetros identificam assets, origem, nome e tipo. CanonicalFile resolve os caminhos; as comparações de pais proíbem sair da raiz e do pacote. Em seguida exige arquivo não vazio, extensão WebP e tipo compatível com animado.txt.

`digest(input)` calcula SHA-256 por streaming. O diretório é listado e ordenado por nome para escolha determinística. Para cada coleção do tipo, o método lê seus WebP e compara hashes. Se achar igualdade, retorna duplicate=true antes de copiar. Também lembra a primeira com menos de 30, mas continua procurando duplicatas nos demais diretórios.

Essa ordem importa: suponha coleção A com 2 e coleção B com 30 contendo a entrada. Parar em A faria duplicação; continuar até B reconhece a seleção anterior. Comparação de hash identifica bytes iguais, não semelhança visual. Uma nova versão recodificada pode ser considerada nova mesmo parecendo igual.

## Criação e inserção

Sem destino com vaga, usa nome conforme o tipo e procura sufixo numérico livre. A presença de um pacote comum com esse nome só causa escolha de outro nome; não altera o pacote existente. Prepara um diretório `.selected-UUID` irmão de assets. Copia a figurinha como `01_hash.webp`, copia icone.png da origem, cria marcador da coleção, v_1.txt e, se animada, animado.txt. Só depois renomeia o diretório para o nome público dentro de assets.

Com destino existente, `nextIndex` escolhe um prefixo numérico maior que os presentes. O índice mantém a convenção de ordenação do ContentsJsonHelper, inclusive se houve remoções anteriores. Cria um .tmp dentro do destino, copia, renomeia para WebP e chama bumpVersion. committed só fica true ao final. O finally remove temporário e, se necessário, a nova figurinha. Não toca na origem.

`copy` abre streams com try-with-resources, usa buffer de 16 KiB e sincroniza o descritor antes de fechar. `createMarker` exige criação bem-sucedida. `bumpVersion` procura v_N.txt, encontra a maior versão, rejeita estouro de Integer.MAX_VALUE, cria N+1 e remove marcadores anteriores. Em falha de remoção tenta apagar o novo marcador e propaga erro; add desfaz a nova cópia. `cleanup` remove arquivos de staging e o diretório quando ele ainda existe; após rename, o caminho antigo desapareceu e a coleção publicada fica intacta.

## Custos e limites de recuperação

Para entrada com n bytes e coleções com s bytes examinados, hashing custa O(n+s). Copiar adiciona O(n); o buffer é constante, mas os arrays de diretórios/arquivos crescem com sua quantidade. O hash aparece no nome de saída para identificação legível, mas a deduplicação recalcula o conteúdo em vez de confiar num nome que pode ter sido alterado externamente.

As rotinas restauram o que conseguem em falhas normais; não fornecem journal para morte abrupta, garantia de exclusão em armazenamento defeituoso ou transação com contents.json. O catálogo é atualizado posteriormente pela Activity. Um staging residual fica fora de assets, evitando ser interpretado como pacote, mas pode ocupar disco até limpeza manual. Essas escolhas são uma integração pequena com o armazenamento atual, e não um banco de dados novo.

## Validação e exercícios

SelectedStickersTest cobre seis cenários com diretórios temporários, incluindo 31 escolhas e falha de versão. SelectedStickersDeviceTest exercita um WebP válido no Android em raiz isolada. Nenhum desses testes confirma exportação real no WhatsApp.

**Pergunta:** por que o helper recebe assets, em vez de buscar FilesHelper internamente? **Resposta:** separa política de armazenamento e ambiente, facilita testes e evita Context Android nessa lógica.

**Pergunta:** copiar bytes garante preservar a animação? **Resposta:** preserva exatamente a codificação original; sua validade e reprodução ainda dependem do conteúdo original e do decodificador.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
