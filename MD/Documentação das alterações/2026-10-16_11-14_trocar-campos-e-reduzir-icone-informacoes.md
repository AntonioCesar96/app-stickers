# Trocar campos e reduzir ícone de informações

## Alterações

- Trocadas as posições de Velocidade e Nitidez no `dialog_compression.xml`: Nitidez fica ao lado de Compressão, e Velocidade na linha inferior.
- Mantidos IDs, vínculos dos rótulos e tipos de entrada: nitidez inteira e velocidade decimal.
- Criado `ic_compression_info.xml`, vetor contornado de 20 dp em cinza discreto. Padding de 14 dp no botão de 48 dp reduz o desenho sem diminuir sua área de toque.
- Atualizada a documentação de regras. Nenhuma classe Java foi modificada; não é necessária uma aula de classe para esta alteração de recursos.

## Validação

Conferência estrutural do XML, ordem dos cinco campos e preservação dos IDs/tipos concluídas. Build release passou e APK foi instalado por atualização no celular, preservando dados e usando o certificado local existente. Mantida a exceção `-x checkRelease` pelo identificador de exemplo. Não foram criados testes para esta alteração visual de baixo impacto.

Veja a [aula](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_11-14_trocar-campos-e-reduzir-icone-informacoes.md).
