# SharedMediaImport: importar um recurso de outro aplicativo

Atualização: 2026-10-16_11-46 — America/Cuiaba.
Código: `app/src/main/java/com/example/samplestickerapp/SharedMediaImport.java`.

## Modelo e responsabilidade

A classe final, visível apenas no pacote, reúne uma operação estática de importação e o resultado imutável por referência: file, kind e name. Não tem interface, executor nem dependência do ciclo de vida. A Activity decide em qual thread executá-la. O construtor também permite reconstruir o resultado a partir do estado salvo.

`final File file` impede trocar a referência depois da construção, mas não torna os bytes imutáveis. É importante distinguir imutabilidade da referência e do recurso externo. O retorno significa que uma cópia não vazia foi concluída; não significa que o decoder já certificou o conteúdo.

## receive passo a passo

1. Exige ACTION_SEND. Extrai EXTRA_STREAM com tratamento de valor parcelável inválido. Se ausente, procura um único item ClipData. Exige esquema content.
2. Consulta o MIME do provedor, usando o MIME da Intent como alternativa quando necessário. Rejeita o que não declara imagem/vídeo. Consulta DISPLAY_NAME opcionalmente: falhar em descobrir o nome não impede ler bytes.
3. Cria files/shared_imports e um nome UUID. A extensão JPEG é jpg; outros tipos de imagem começam como png; vídeo começa como mp4. Essas extensões não transcodificam os bytes.
4. Abre InputStream e FileOutputStream com try-with-resources. Esse recurso Java fecha ambos inclusive em exceções. Copia blocos de 64 KiB e verifica interrupção. Ao terminar, sincroniza o descritor e rejeita arquivo vazio.
5. Lê a assinatura de seis bytes. GIF87a/GIF89a determina classificação gif e renomeia a cópia para .gif. Demais arquivos recebem kind video ou image pelo MIME.
6. Só então marca success e retorna. O finally apaga o arquivo parcial se a operação não concluiu.

O trecho `finally { if (!success) file.delete(); }` protege contra arquivos incompletos em falhas normais. Não garante limpeza em encerramento abrupto do processo nem sucesso de toda exclusão. UUID evita colisão sem confiar no nome recebido. Nome externo não é usado como caminho, eliminando essa fonte de travessia de diretórios.

## Custos, estados e erros

Para um arquivo de 10 MiB, há aproximadamente 160 blocos completos de 64 KiB. A memória do buffer permanece constante; o disco usa aproximadamente o tamanho da entrada. A complexidade temporal é linear no número de bytes, acrescida da latência do provedor e sincronização. Um vídeo em nuvem pode exigir download realizado pelo provedor.

Uma interrupção produz InterruptedIOException no laço, mas uma leitura bloqueada pode demorar a devolver controle. SecurityException ou erro do provedor pode propagar para a Activity, que exibe a mensagem genérica. Somente o nome opcional ignora RuntimeException localmente. Não se tenta descobrir `_data`, persistir uma concessão não garantida nem alterar o arquivo original.

## Simulação e alternativas

Entrada image/*, URI content válida e bytes GIF89a: copia para UUID.png, reconhece GIF, renomeia e retorna kind gif. Entrada text/plain: falha antes da cópia. Entrada de zero bytes: fecha streams, falha e remove parcial. Link HTTPS: falha porque o contrato é arquivo concedido por provedor.

Usar a URI original no editor economizaria disco, mas manteria dependência do provedor durante operações longas. Ler tudo em byte[] simplificaria a cópia, mas aumentaria memória proporcionalmente ao vídeo. A solução usa disco privado e memória constante. Uma política de limpeza e limites adicionais de tamanho seriam evoluções possíveis, não implementadas nesta classe.

ShareMediaTest verifica GIF por ClipData e entradas inválidas; o cenário de recriação do vídeo demonstra o uso independente da cópia. Não valida todos os comportamentos de provedores malformados.

**Exercício:** renomear webm para mp4 muda seu codec? **Resposta:** não. A classe copia bytes; decodificação real continua responsabilidade das bibliotecas existentes.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_11-46_receber-compartilhamento-de-midias-do-android.md)
