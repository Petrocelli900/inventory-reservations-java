# Decisões técnicas

## Consistência antes de paralelismo

Um monitor protege o estoque e as reservas em conjunto. A verificação e o débito são atômicos dentro de uma única instância. O teste concorrente disputa 10 unidades entre 100 tentativas e verifica exatamente 10 sucessos.

## Idempotência

Uma chave identifica uma reserva para sempre durante a vida da instância. Repetir o mesmo payload devolve seu estado atual; mudar produto ou quantidade rejeita a operação. Uma reserva cancelada não pode ser reativada com a mesma chave. Repetir a mesma transição final é seguro.

## Estados

ACTIVE pode virar CONFIRMED ou CANCELLED. Cancelar devolve estoque uma única vez. Confirmar mantém o débito já realizado na reserva. Transições entre estados finais são rejeitadas. Quantidades positivas e aritmética verificada impedem saldo inválido e overflow silencioso.

## Limitações

Lock global reduz throughput. A memória de idempotência cresce sem expiração; não existe TTL, persistência ou reserva entre processos. Para evoluir: transações no banco, chave única de idempotência, expiração com relógio injetável e testes de integração. Essas funcionalidades não estão implementadas.
