# Distributed Ledger

A report on Group T38's solution for phase 3 of the Distributed Systems course project 2022/2023.


# Introduction

This report presents the solution developed for the third phase of the project, aimed at enabling two servers, which share state through the gossip architecture, to respond to client process requests.


# Implemented features

Given that the gossip architecture was implemented in this phase, the following functionalities were added:

Protos of remote procedure messages (createAccount, getLedgerState, balance, transferTo) and User and Admin were modified to include a Vector Clock.
Vector Clock, in this project, corresponds to a vector of integers, which in this case are 2, one for each server.
Protos of ledger operation messages had two Vector Clocks added, one corresponding to User/Admin and the other to the operation's TimeStamp.
Protos of ledger operation messages had a boolean field added to identify whether the operation is stable or not.
The Server has two Vector Clocks indicating ValueTS and ReplicaTS.
Thus, when a Client makes a read request to a server, it either immediately receives the response (updated server), or waits until it has a more suitable version to provide, with only the Client's TimeStamp being updated. If the request is for a read, the Server's corresponding ReplicaTS is incremented. All these operations are added to the Ledger and are never removed. If the previous (Client's TimeStamp) is less than or equal to the Server's ValueTS, the operation is classified as stable and executed, and the ValueTS is updated; otherwise, the stability field is false.

# Challenges

One of the major challenges of this project was implementing the causal ordering model of write operations, as it is an adaptation of the gossip architecture. The fact that server updates only occur at the Admin's request adds complexity to problem resolution and, from the User's perspective, it can be restrictive if the Admin never requests gossip.

Another challenge was preventing inconsistent reads by the same client or violation of causality between operations, resolved by ensuring that the comparison of Vector Clocks was correct.


# Conclusion

Este projeto de um serviço que implementa um ledger distribuído, ao fim de 3 entregas e de 7 semanas de aperfeiçoamento, 
é capaz de responder e executar tudo o que nos foi solicitado. 
A implementação do sistema usando uma arquitetura distribuída permite que o sistema seja escalável e flexível para atender às necessidades e os modelos em que nos baseamos foram os mais benéficos para o objetivo, 
tendo em conta os lecionados e disponíveis no livro principal da disciplina.
