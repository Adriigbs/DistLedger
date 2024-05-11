# Distributed Ledger

Um relatório acerca da solução do grupo T38 para a fase 3 do projeto da cadeira de Sistemas Distribuídos 2022/2023.


# Introdução

Este relatório apresenta a solução criada para a terceira fase do projeto, que tem como objetivo que dois servidores, 
que partilham o estado através da arquitetura gossip, respondam às solicitações dos processos cliente. 


# Ferramentas Implementadas

Dado que nesta fase foi implementada a gossip architecture, foram adicionadas as seguintes funcionalidades:
Aos proto das mensagens de procedimentos remotos (createAccount, getLedgerState, 
balance, transferTo) e os User e Admin foram alterados para conterem um Vector Clock.
Vector Clock, neste projeto, corresponde a um vector de inteiros, que neste caso são 2, um para cada servidor.
Aos proto das mensagens de operações no ledger foram adicionados dois Vector Clock, um correspondente ao do User/Admin e o outro ao TimeStamp da operação.
Aos proto das mensagens de operações no ledger foi adicionado um campo booleano para identificar se a operação é ou não estável.
O Server tem dois Vector Clocks indicando o ValueTS e o ReplicaTS.

Deste modo, o Cliente, quando faz um pedido de leitura a um servidor, ou obtém imediatamente a resposta (servidor atualizado), ou aguarda até que este tenha uma versão mais adequada que possa fornecer, 
existindo apenas atualização do TimeStamp do Cliente.
Caso o pedido seja de leitura, o ReplicaTS correspondente ao Server é incrementado. Todas estas operações são adicionadas ao Ledger e nunca são removidas. 
Caso o prev (TimeStamp do Cliente) seja menor ou igual ao ValueTS do Server, a operação é classificada como estável e executada e o ValueTS é atualizado, 
caso não seja, o campo da estabilidade fica falso.


# Desafios

Um dos maiores desafios deste projeto foi implementar o modelo de ordenação causal de operações de escrita, visto ser uma adaptação do gossip architecture. 
O facto de a atualização dos servidores ser apenas a pedido do Admin, adiciona complexidade na resolução do problema bem como, do ponto de vista do User, 
pode ser restritivo no caso do Admin nunca solicitar gossip.

Outro desafio foi impedir leituras incoerentes pelo mesmo cliente ou a violação da causalidade entre operações, 
resolvida certificando que a comparação de Vector Clocks era a correta.


# Conclusão

Este projeto de um serviço que implementa um ledger distribuído, ao fim de 3 entregas e de 7 semanas de aperfeiçoamento, 
é capaz de responder e executar tudo o que nos foi solicitado. 
A implementação do sistema usando uma arquitetura distribuída permite que o sistema seja escalável e flexível para atender às necessidades e os modelos em que nos baseamos foram os mais benéficos para o objetivo, 
tendo em conta os lecionados e disponíveis no livro principal da disciplina.
