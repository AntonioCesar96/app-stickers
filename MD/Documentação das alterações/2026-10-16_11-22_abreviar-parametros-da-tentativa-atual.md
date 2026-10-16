# Abreviar parâmetros da tentativa atual

O texto informativo abaixo da barra passa a usar `Q-75`, `C-6` e `N-512`. Os parâmetros aparecem sem quebra explícita entre si, por exemplo `Q-75 · 30 FPS · C-6 · N-512 · 1×`.

Criado um formatador específico na `CropVideoActivity` para limitar a mudança ao resumo atual. Inputs e texto da última tentativa concluída permanecem no formato anterior. Atualizadas as regras e a aula da classe.

Validação: escopo da chamada conferido, build release concluído e APK assinado/verificado com o certificado local existente. Usuário autorizou instalar imediatamente mesmo com compressão ativa. Atualização instalada com sucesso por adb install -r, preservando dados; app reaberto para retomar a tarefa a partir do estado persistido. Não foram criados testes para esta alteração textual de baixo impacto. A linha ainda pode quebrar naturalmente em telas estreitas ou fontes maiores, sem cortar valores.

Veja a [aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_11-22_abreviar-parametros-da-tentativa-atual.md).
