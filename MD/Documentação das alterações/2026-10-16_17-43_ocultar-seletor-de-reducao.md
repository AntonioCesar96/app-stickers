# Ocultar seletor de redução

Data: 2026-10-16_17-43 — America/Cuiaba.

O container do seletor Redução em `app/src/main/res/layout/dialog_compression.xml` passou a usar `android:visibility="gone"`. Isso esconde conjuntamente rótulo, Spinner Agressivo/Moderado/Suave e botão Sugerir, sem reservar espaço. A lógica de sugestões foi conservada; campos manuais e reinício não foram alterados. Atualizada a regra do tema para indicar que o recurso está oculto.

Validação: conferência do XML, diff sem erros e compilação release concluída. A release foi alinhada, assinada com a chave local existente, verificada e instalada por atualização no celular, preservando dados. A geração usou `-x checkRelease` pela restrição preexistente do projeto modelo ao applicationId atual. Não há classes afetadas nem aula por classe nova; não foram criados ou executados testes instrumentados para esta alteração visual reversível.

[Conceitos](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_17-43_ocultar-seletor-de-reducao.md)
