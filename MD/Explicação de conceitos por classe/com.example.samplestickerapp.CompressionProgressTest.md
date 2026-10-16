# CompressionProgressTest: verificar cálculo e limites

Última atualização: 2026-10-16_11-04 (America/Cuiaba).

Código: `app/src/test/java/com/example/samplestickerapp/CompressionProgressTest.java`.

JUnit constrói medidores sem Android para testar cenários determinísticos. O primeiro caso mantém estatísticas em zero, entrega um log de quadro em 1,9 segundo a 10 FPS e confirma 50% para quatro segundos. Depois entrega zero novamente, comprovando que dados atrasados não fazem a porcentagem retroceder.

O segundo caso alcança o último quadro e verifica 99, inclusive se as estatísticas ultrapassarem a duração esperada. Um novo medidor deve começar em zero, comprovando isolamento entre tentativas. A finalização em 100 pertence ao serviço, e não a um palpite desta classe.

O terceiro caso testa estatísticas fora de ordem, NaN, mensagem nula, texto sem prefixo showinfo e número malformado. Também rejeita duração zero. Esses testes exercitam o parser e o cálculo, não demonstram que o FFmpeg de um aparelho emitirá o formato esperado; esse ponto é coberto pela instrumentação.

**Exercício:** por que entregar um log sem a palavra showinfo? Para garantir que um timestamp de mensagem alheia não seja interpretado como avanço dos quadros de saída.
