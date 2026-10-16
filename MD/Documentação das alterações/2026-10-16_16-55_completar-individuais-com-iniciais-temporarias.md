# Completar coleções individuais com iniciais temporárias

Data: 2026-10-16_16-55 — America/Cuiaba.

## Alteração e motivo

O pacote individual passa a manter no mínimo três figurinhas desde a primeira escolha usando amostras iniciais como preenchimento temporário. Cada adição, incluindo repetida, reavalia o estado: uma escolhida + duas iniciais, duas escolhidas + uma inicial, três escolhidas + nenhuma inicial. Assim o usuário pode adicionar o pacote ao WhatsApp sem esperar fazer três escolhas próprias.

`SelectedStickers` passa a receber uma fonte de templates, distinguir escolhas e preenchimentos, ignorar preenchimentos na deduplicação e reconciliar a quantidade automaticamente. Result informa contagens separadas. Arquivos novos são preparados fora de assets; trocas retiram apenas temporários verificados, com tentativa de reversão em erro.

`InitialStickerTemplates` lê Pacote 1/Pacote 2 do APK, os mesmos arquivos usados pela Splash, mantendo a disponibilidade mesmo se o usuário tiver apagado ou editado os pacotes iniciais no armazenamento. `SplashActivity` não precisou ser alterada. `StickerPackDetailsActivity` fornece essa fonte e apresenta escolhidas, temporárias e total na confirmação.

Foram adaptados os testes JVM/Android e as aulas das classes afetadas, e criada aula da nova classe. A documentação de regras foi atualizada para substituir o comportamento de aguardar três escolhas pelo preenchimento automático. O validador mantém tolerância aos rascunhos legados; a barreira de exportação inferior a três permanece.

## Verificações

Compilações debug e release passaram. Os 38 testes unitários passaram, incluindo dez cenários de SelectedStickersTest. Os três testes Android passaram no celular conectado em 4,176 s: prévia, cópia de WebP real e transição completa com os assets estáticos e animados do APK. Esses cenários usam raízes isoladas para não modificar as coleções reais do usuário. Não foram manipuladas conversas nem confirmada instalação de pacotes no WhatsApp por automação.

A release foi alinhada, assinada com a chave local existente, verificada e instalada por atualização preservando dados. O APK de testes foi removido. A geração usa `-x checkRelease` devido à verificação preexistente do projeto modelo que recusa o applicationId atual; o identificador e a verificação foram preservados. A assinatura é local, compatível com a instalação existente, sem preparação de publicação na loja.

## Limitações

Reparo ocorre quando há uma tentativa de adição individual, não como migração global ao abrir o app. Arquivos temporários alterados externamente são preservados como permanentes. Escolher explicitamente uma amostra inicial a torna permanente; a remoção automática se aplica apenas às cópias de preenchimento. Concorrência com outros gravadores e encerramento abrupto continuam fora de uma transação global com o catálogo JSON.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-55_completar-individuais-com-iniciais-temporarias.md) · [Regras atuais](../Documentação%20de%20regras/figurinhas-selecionadas-copia-individual-pacotes-estaticos-animados-whatsapp.md)
