# Abreviação de parâmetros sem alterar seus valores

## Formatação e escopo

O resumo exibe dados do JSON da tentativa atual. Transformar qualidade 75 em `Q-75` muda apenas a representação textual: a entrada do encoder continua 75. C e N indicam compressão e nitidez, respectivamente. FPS e velocidade conservam suas unidades.

O formatador anterior era compartilhado pelo resumo atual e pela última tentativa concluída. Alterá-lo diretamente afetaria ambos. O método `currentAttemptSettings` concentra o formato novo e é chamado somente pelo texto sob a barra. O formatador anterior permanece no outro bloco, e nenhum EditText foi alterado.

## Linha lógica e quebra visual

Remover o caractere de nova linha entre parâmetros permite ao Android aproveitar a largura disponível. Isso não garante uma única linha em todos os aparelhos: fonte, largura e quantidade de dígitos influenciam o espaço ocupado. Preservar quebra natural evita cortar informações em configurações de acessibilidade maiores.

## Verificação e exercício

A revisão confere a chamada específica e os campos usados em cada marcador; a compilação verifica a integração Java. Essa alteração não modifica o algoritmo nem exige repetir conversões pesadas.

**Exercício:** por que não trocar também os IDs dos inputs por Q, C e N? Porque o pedido é de apresentação textual. Os IDs identificam valores para os listeners e não precisam acompanhar a abreviação visível.

Veja o [registro](../Documentação%20das%20alterações/2026-10-16_11-22_abreviar-parametros-da-tentativa-atual.md).
