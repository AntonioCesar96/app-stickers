# Migrar regras para o AGENTS.md global

## Alterações

Transferidas todas as regras do `AGENTS.md` deste projeto para `C:\Users\Dell\.codex\AGENTS.md`, que estava vazio. Nenhum `AGENTS.override.md` global foi encontrado e `CODEX_HOME` não estava definido no processo nem nas variáveis de usuário ou máquina consultadas.

As regras foram preservadas integralmente, com mudança do título para instruções globais e uma introdução esclarecendo que as pastas `MD/` ficam em cada projeto ou pasta de trabalho. O arquivo local passou a conter somente uma referência ao global, evitando duplicação. Atualizada a documentação de regras de organização.

## Validação e alcance

Comparado o conteúdo global com a transformação exata do original antes de reduzir o arquivo local. Escrita e leitura de conferência bem-sucedidas; arquivo global com 10984 bytes. Executado `git diff --check`. O ambiente forneceu as instruções globais atualizadas durante esta tarefa.

A documentação oficial confirma a descoberta global em `~/.codex/AGENTS.md`, a precedência de overrides e a aplicação de regras específicas de projeto: [fonte oficial](https://developers.openai.com/codex/guides/agents-md). Não houve alteração do aplicativo, compilação ou instalação. A configuração é deste perfil local; não é sincronização automática com outros computadores ou ambientes.

Veja a [aula da migração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_10-03_migrar-regras-para-agents-global.md).
