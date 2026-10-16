# Progresso percentual real por tentativa

## Alterações

- Barra horizontal sempre determinada, inclusive em zero, com porcentagem numérica separada no diálogo. Removida a duplicação de percentual no resumo de parâmetros.
- `CompressionProgress` mede timestamps dos quadros de saída e estatísticas do FFmpeg, sem simular pelo tempo decorrido. A timeline vem do filtro final `showinfo=checksum=0`, usado porque o muxer WebP pode não publicar tempo intermediário.
- Cada encode começa com outro medidor. Atualização monotônica, máximo 99 enquanto a sessão pode estar finalizando; done permanece 100 depois de validar resultados.
- Notificação também determinada em zero. Ajuda esclarece que o percentual é da tentativa atual, não do processo inteiro.

## Validação

28 testes unitários passaram. Dois cenários instrumentados passaram em 7,457 segundos: publicação de percentual intermediário durante um encode real e apresentação determinada com valor sintético de 42%. A captura `app/build/compression-dialog-progress42.png` foi revisada; o valor da captura é uma fixture de interface, não uma medição do teste nativo.

Não se mede porcentagem de tempo de CPU nem quantidade de tentativas restantes. A verificação nativa cancela após observar o progresso, evitando uma espera longa desnecessária. Build release concluído e APK instalado por atualização no celular, com o certificado local já utilizado, sem limpar dados. Mantida a exceção `-x checkRelease` pela restrição do identificador de exemplo. Pacote de testes removido após a validação.

Veja a [aula](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_11-04_progresso-percentual-real-por-tentativa.md).
