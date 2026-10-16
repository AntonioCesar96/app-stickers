# Sugestões de parâmetros por intensidade

Data: 2026-10-16_17-23 — America/Cuiaba.

## O que mudou e por quê

Removido o botão Edit/Editar de `activity_crop_video.xml`, suas referências e os métodos openDialogOpcoesVideo/resetInputs de CropVideoActivity. Os parâmetros da próxima tentativa permanecem no diálogo de compressão.

Adicionado Spinner Redução com Agressivo, Moderado e Suave e ação Sugerir em dialog_compression.xml. Alterar o modo aplica uma sugestão nos cinco campos; Sugerir reaplica a mesma opção usando registros atualizados. Não reinicia nem interrompe o processo: o usuário pode editar e apertar Reiniciar depois. A vinculação inicial não sobrescreve o rascunho restaurado.

Criada CompressionSuggestion para calcular uma heurística com o excesso de bytes da última conversão concluída. Usa os parâmetros associados a esse tamanho e limites dos parâmetros atuais. Reduz qualidade/FPS/nitidez por intensidade, conserva velocidade, respeita limites e aumenta esforço de compressão até 6. Sem tentativa válida ou quando já cabe, não degrada os inputs. O texto do diálogo informa que são sugestões e que o tamanho depende da mídia.

Criados seis testes unitários e ampliado o cenário instrumentado de apresentação para verificar três opções, ausência de Edit, campos sugeridos, diferença entre intensidades e ausência de reinício automático. Atualizados tema e aulas das classes afetadas.

## Validação

Compilações debug/release concluídas e os 45 testes unitários passaram. O teste instrumentado organizedDialogUses95PercentWidthAndKeepsFieldsVisible passou no aparelho em 5,771 s; usa 680000 bytes sintéticos para verificar UI, não como resultado de um benchmark novo. Captura revisada visualmente: Spinner, valores e ações aparecem no diálogo rolável. Não foram repetidos os demais testes instrumentados da classe nesta tarefa.

A release foi alinhada, assinada com a chave local existente, verificada e instalada por atualização preservando dados. O APK de testes foi removido. A geração usou `-x checkRelease` devido à verificação preexistente do projeto modelo que rejeita o applicationId atual; identificador e verificação foram mantidos. A assinatura é local, compatível com a instalação atual, não configuração de publicação em loja.

## Limites

A relação entre qualidade, detalhe e tamanho WebP não é linear. Os coeficientes expressam intensidades heurísticas; não foram calibrados por um conjunto de benchmarks nem prometem chegar a 500 KB em uma tentativa. O tamanho da última saída não prevê todos os quadrantes do modo Exodia. A busca automática e o protocolo de reinício/cancelamento não foram alterados.

[Aula](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_17-23_sugerir-reducao-de-compressao-por-intensidade.md) · [Regras](../Documentação%20de%20regras/compressao-webp-dialogo-tentativas-parametros-reinicio-cancelamento-background.md)
