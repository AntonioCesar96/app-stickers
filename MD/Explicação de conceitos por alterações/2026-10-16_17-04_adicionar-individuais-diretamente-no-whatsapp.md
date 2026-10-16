# Aula: abrir a inclusão externa e separar dados internos da interface

Data: 2026-10-16_17-04 — America/Cuiaba.

## O objetivo e o limite entre aplicativos

O usuário quer tocar Adicionar e seguir para o WhatsApp, sem administrar uma coleção dentro do app de stickers. A versão anterior confundiu duas responsabilidades: guardar dados para a integração e apresentar um novo pacote na interface. A solução mantém a primeira porque o contrato exige e elimina a segunda.

Aplicativos Android têm áreas privadas separadas. Nosso app não escreve diretamente no banco do WhatsApp. A integração documentada usa um ContentProvider para disponibilizar metadados/arquivos e uma Intent ENABLE_STICKER_PACK para abrir a inclusão. O usuário confirma no destinatário. Por isso “diretamente” significa remover etapas intermediárias do nosso aplicativo, sem prometer confirmação silenciosa externa. [Contrato oficial](https://github.com/WhatsApp/stickers/blob/main/Android/README.md#advanced-development).

## Duas visões dos mesmos dados

O catálogo precisa continuar contendo os dados de individuais para que o WhatsApp os consulte por authority e identifier. Removê-los do JSON para esconder a interface impediria o destinatário de carregar as figurinhas. Em vez disso, filtramos a projeção usada pelas telas.

`ContentsJsonHelper` gera todos os modelos, incluindo os internos. O ContentProvider os disponibiliza. `StickerPackLoader.fetchStickerPacks` lê os metadados e passa por visiblePacks antes de carregar as figurinhas para a UI. O filtro verifica o marcador .selected-stickers e constrói outra ArrayList sem essas entradas.

```text
arquivos → catálogo completo → ContentProvider → WhatsApp
                                    ↓
                         filtro de visibilidade → telas do app
```

Não é uma medida de sigilo nem remoção dos arquivos da memória do celular. É uma política de apresentação. O teste verifica que a lista de origem continua com dois modelos enquanto a lista visível fica com um. Mutar a lista completa no lugar poderia afetar outros consumidores, portanto criamos uma nova lista.

O filtro central beneficia a tela principal, o recebimento por compartilhamento e a movimentação, que já consultam o mesmo loader. Evita duplicar condições em cada tela e impede que um pacote interno volte a aparecer em um seletor secundário.

## Nome público não é identidade persistente

identifier é o endereço estável do pacote junto ao provedor; name é o título que o WhatsApp apresenta. A tarefa usa essa distinção para preservar dados existentes. Diretórios com nomes Selecionadas continuam com os mesmos identificadores, mas os metadados passam a apresentar Individuais.

displayName aplica o tipo e converte a numeração: primeiro diretório → Individuais - estaticas; sufixo interno 2 → Individuais - estaticas +1; sufixo 3 → +2. Animadas têm outra sequência. A expressão regular busca apenas dígitos no final e a conversão tem tratamento de número inválido/fora do intervalo. Pacotes comuns não usam essa transformação.

Preservar identifiers evita criar um pacote diferente apenas por trocar o título. Renomear o diretório poderia deixar o WhatsApp referenciando um caminho lógico que desapareceu. O catálogo e a Intent precisam anunciar o mesmo name: por isso tanto ContentsJsonHelper quanto Result usam a função única displayName.

## Fluxo do clique, da thread de trabalho ao destino

O adapter já entrega a figurinha à Activity. addSelectedSticker bloqueia cliques simultâneos na instância e executa cópia/deduplicação/preenchimento em uma Thread. A regra anterior continua: 1+2, 2+1 e 3+0, com no máximo 30. A 31ª escolha ocupa outro conjunto de dados e recebe título +1.

Depois de reconstruir o catálogo e invalidar o cache do provedor, a Activity volta à thread principal. Se a tela ainda estiver viva, chama `addStickerPackToWhatsApp(result.identifier, result.name)`. Foram retirados a busca do destino no loader de UI e o AlertDialog intermediário: o loader agora esconde esses dados deliberadamente, então não pode ser usado para reencontrar um pacote interno.

AddStickerPackActivity mantém o mínimo físico de três e atualiza o provedor antes de abrir a Intent. Detecta as variantes instaladas. Só Consumer: abre com.whatsapp. Só Business: abre com.whatsapp.w4b. Ambas: oferece o seletor do Android. Nenhuma: mostra orientação. A condição anterior `x || !x` foi removida porque sempre era verdadeira e poderia escolher Consumer mesmo quando o contexto exigia Business.

O processamento e a inclusão são etapas diferentes. Se o usuário cancelar no WhatsApp, os dados preparados permanecem. Outro clique reencontra a escolha por hash, não duplica seus bytes e abre novamente o fluxo. A atualização de image_data_version comunica mudanças; não permite garantir quando cada versão do WhatsApp atualiza seu próprio cache.

## Catálogo testável e tratamento de versão ausente

`ContentsJsonHelper.getStickerPacks()` continua usando a raiz real, mas delega para uma sobrecarga que recebe File. Isso permite verificar nomes públicos em cache isolado sem sobrescrever o catálogo do usuário. A lógica permanece a mesma da produção.

Ao examinar v_N.txt, o scanner precisa distinguir lista nula e lista vazia. Uma pasta pode ser legível e simplesmente não ter marcador, retornando array de tamanho zero. A condição `versaoList != null && versaoList.length > 0` evita acessar [0] nesse caso e mantém versão inicial 1. Null e vazio são estados diferentes; testar apenas null não protege o acesso ao primeiro elemento.

## Custos, alternativas e evidência

visiblePacks custa O(p) para p metadados, com uma consulta de marcador por pacote, e uma lista de referências adicional O(p). Ao filtrar antes de carregar bytes, evita validar/descomprimir os individuais apenas para desenhar a UI. O ContentProvider não perde acesso aos arquivos.

Uma alternativa seria um provider separado só para individuais, mas exigiria outra autoridade e mais infraestrutura para a mesma integração. Outra seria apagar os dados após a confirmação, arriscando quebrar consultas futuras do WhatsApp. Reutilizar o provider completo e filtrar apenas a interface mantém o contrato e os identificadores existentes.

A suíte JVM testa títulos, transbordo, preenchimento, falhas e duplicatas. A instrumentação Android verifica o catálogo produzido e a lista visível com diretórios temporários, sem modificar pacotes do usuário. Os testes não confirmam pacotes no WhatsApp nem enviam mensagens. É importante distinguir “o app preparou e abriu a integração” de “o destinatário confirmou a inclusão”, que permanece uma ação externa.

**Exercício:** esconder os individuais removendo suas entradas do ContentProvider atende ao objetivo? **Resposta:** não; também esconde do WhatsApp os dados que ele precisa. O filtro deve pertencer à leitura da interface.

**Exercício:** por que o sufixo interno 2 vira +1? **Resposta:** o primeiro pacote é a base, sem sufixo público; o segundo é o primeiro adicional.

**Exercício:** se o usuário cancelar no WhatsApp e tocar novamente na mesma figurinha, surge outra cópia? **Resposta:** não; a deduplicação reconhece os bytes, revalida o preenchimento e repete a abertura da inclusão.

[Registro](../Documentação%20das%20alterações/2026-10-16_17-04_adicionar-individuais-diretamente-no-whatsapp.md) · [Regras](../Documentação%20de%20regras/figurinhas-selecionadas-copia-individual-pacotes-estaticos-animados-whatsapp.md)
