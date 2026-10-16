# Ocultar um conjunto de controles no Android

Data: 2026-10-16_17-43 — America/Cuiaba.

O rótulo Redução, o Spinner e Sugerir pertencem ao mesmo LinearLayout em dialog_compression.xml. Alterar a visibilidade desse pai afeta a apresentação dos três filhos de uma vez, evitando deixar um rótulo sem seletor ou uma ação sem opção selecionável.

`gone` significa que a View não aparece e não reserva espaço na medição do layout. `invisible` também a esconderia, mas deixaria um espaço vazio. Como o objetivo é retirar esse conjunto do diálogo, gone é a opção adequada. Os controles ainda são inflados, portanto os listeners existentes podem continuar sendo configurados sem referências nulas.

Ocultar difere de apagar a implementação. A heurística CompressionSuggestion e os listeners permanecem para uma possível reativação; isso não altera a busca automática do encoder. O binding inicial do Spinner já ignora sua posição restaurada, portanto criar o diálogo não dispara uma sugestão para sobrescrever campos. Os inputs manuais e o botão Reiniciar pertencem a outros componentes e continuam disponíveis.

A conferência do atributo verifica a intenção no XML; compilar os recursos valida sua aceitação pelo Android. Não foi criado um teste que apenas repita esse atributo literal, pois a mudança é local e reversível. A tarefa não afeta algoritmos nem cria classes, por isso não exige uma aula de classe fictícia.

[Registro](../Documentação%20das%20alterações/2026-10-16_17-43_ocultar-seletor-de-reducao.md) · [Regras](../Documentação%20de%20regras/compressao-webp-dialogo-tentativas-parametros-reinicio-cancelamento-background.md)
