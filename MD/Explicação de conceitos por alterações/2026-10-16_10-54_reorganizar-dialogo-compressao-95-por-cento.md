# Organizar informação e medir a área visível do diálogo

## Problema e hierarquia visual

O diálogo anterior apresentava muitos textos com o mesmo peso, repetia informações e usava rótulos que quebravam em várias linhas. O usuário precisava procurar métricas e ações. Hierarquia visual significa dar sinais de importância e agrupamento: título identifica o estado, cartão destaca a última saída, formulário reúne ajustes e botão principal indica a ação de reinício.

No layout atual, tamanho e parâmetros deixaram de ser um parágrafo único. `last_attempt_size` destaca o valor; `webp_info_tv` apresenta os metadados menores. A tentativa atual fica no resumo superior. Não se removem dados da tarefa: muda-se a apresentação para facilitar comparação com o limite.

## Geometria da janela e insets

Configurar largura de janela em 95% não garante que o fundo visível tenha essa largura. O drawable padrão pode acrescentar margens internas, chamadas insets. O callback de exibição da Activity substitui esse fundo por uma superfície própria e zera padding do decor, mantendo o cálculo da largura.

O valor em pixels vem de `widthPixels * .95`. O raio do fundo usa densidade para converter dp em pixels. Textos continuam em sp, que consideram a escala de fonte do usuário. Não se resolveu o problema desativando acessibilidade ou alterando configurações do aparelho.

## Grade, rolagem e ações

Quatro campos ficam em duas linhas de duas colunas, com rótulos explicitamente menores e entradas alinhadas. Nitidez ocupa a linha seguinte. Layout weights distribuem a largura disponível. O centro pode rolar quando necessário, enquanto cabeçalho e ações ficam fora do ScrollView.

Essa divisão evita que rolar até um campo esconda o botão principal. Também mantém os papéis distintos: Reiniciar envia parâmetros, Cancelar para a conversão, Fechar apenas oculta o diálogo e Minimizar leva o app para background.

## Validação por medida e por captura

O novo cenário instrumentado abre um job cancelado com metadados sintéticos, verifica a largura medida do decor e as regiões globais visíveis dos campos e ações. Uma captura permite conferir alinhamento e hierarquia, que uma comparação de largura sozinha não demonstra.

O teste passou no aparelho com sua escala de fonte de 1,25. Isso demonstra a apresentação exercitada, não garante todas as combinações de orientação, teclado e tamanho de tela. A rolagem permanece disponível como adaptação a espaços menores. O teste não avalia tamanho gerado pelo codec, pois os dados do cartão foram preparados para inspecionar a interface.

## Exercício comentado

**Por que aumentar a largura sem reduzir repetições não resolveria tudo?** Porque o excesso de linhas e a ausência de agrupamento continuariam disputando espaço e atenção. A geometria e a organização da informação precisam ser tratadas juntas.

**Por que manter sp em vez de fixar texto em pixels?** Porque sp respeita a preferência de leitura do usuário. O layout deve acomodar essa preferência com espaçamentos, grade e rolagem, em vez de anulá-la.

Veja o [registro da alteração](../Documentação%20das%20alterações/2026-10-16_10-54_reorganizar-dialogo-compressao-95-por-cento.md).
