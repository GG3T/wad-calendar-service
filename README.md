# WAD Calendar Service

Serviço para gerenciamento de agendamentos integrado com Google Calendar.

## Descrição

O WAD Calendar Service é uma aplicação Spring Boot responsável por gerenciar agendamentos, com funcionalidades de verificação de disponibilidade, criação, cancelamento, reagendamento e consulta de agendamentos. O serviço também se integra com o Google Calendar para sincronização dos eventos.

## Funcionalidades

- **Verificação de Disponibilidade**: Verifica se um horário específico está disponível para agendamento. Se não estiver, sugere os próximos 4 dias disponíveis (excluindo fins de semana).
- **Agendamento**: Permite criar um novo agendamento, verificando disponibilidade e integração com Google Calendar.
- **Cancelamento**: Permite cancelar um agendamento existente.
- **Reagendamento**: Permite reagendar um agendamento para outra data e horário.
- **Consulta**: Permite consultar um agendamento pelo número de telefone.
- **Confirmação Automática**: Serviço agendado para enviar confirmações 24 horas antes do agendamento.
- **Integração com Google Calendar**: Criação, atualização e cancelamento de eventos no Google Calendar.
- **Recepção de Atualizações via Google Push**: Recebe notificações do Google Calendar e atualiza o banco de dados.

## Tecnologias Utilizadas

- Java 17
- Spring Boot 3.4.4
- Spring Data JPA
- PostgreSQL
- Google Calendar API
- RESTful API
- JUnit 5 para testes

## Endpoints

### Verificar Disponibilidade
```
GET /api/appointments/availability?date=2023-06-01&time=14:00
```
Resposta de sucesso (Disponível):
```json
{
  "available": true,
  "requestedDate": "2023-06-01",
  "requestedTime": "14:00"
}
```
Resposta de sucesso (Indisponível):
```json
{
  "available": false,
  "requestedDate": "2023-06-01",
  "requestedTime": "14:00",
  "alternativeDates": [
    {
      "date": "2023-06-02",
      "time": "14:00"
    },
    ...
  ]
}
```

### Criar Agendamento
```
POST /api/appointments
```
Corpo da requisição:
```json
{
  "date": "2023-06-01",
  "time": "14:00",
  "phoneNumber": "11999887766",
  "summary": "Resumo do agendamento"
}
```
Resposta de sucesso:
```json
{
  "message": "Agendamento criado com sucesso"
}
```

### Cancelar Agendamento
```
POST /api/appointments/cancel
```
Corpo da requisição:
```json
{
  "phoneNumber": "11999887766"
}
```
Resposta de sucesso:
```json
{
  "message": "Agendamento cancelado com sucesso"
}
```

### Reagendar Agendamento
```
POST /api/appointments/reschedule
```
Corpo da requisição:
```json
{
  "date": "2023-06-02",
  "time": "15:00",
  "phoneNumber": "11999887766",
  "summary": "Novo resumo do agendamento"
}
```
Resposta de sucesso:
```json
{
  "message": "Agendamento reagendado com sucesso"
}
```

### Consultar Agendamento
```
GET /api/appointments?phoneNumber=11999887766
```
Resposta de sucesso:
```json
{
  "phoneNumber": "11999887766",
  "date": "2023-06-01",
  "time": "14:00",
  "summary": "Resumo do agendamento",
  "status": "AGENDADA"
}
```

## Configuração

### Arquivo application.properties

Configuração do banco de dados, Google Calendar e outras propriedades:

```properties
# Configurações do Banco de Dados
spring.datasource.url=jdbc:postgresql://localhost:5432/wad_calendar
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha

# Configurações do Google Calendar
google.calendar.id=primary
google.credentials.file=classpath:credentials/google-calendar-credentials.json

# Serviço de Notificações (se houver)
notification.service.url=http://seu-servico-notificacao.com/api
```

### Google Calendar

Para configurar a integração com o Google Calendar:

1. Crie um projeto no Google Cloud Console
2. Ative a API do Google Calendar
3. Crie credenciais de serviço (Service Account)
4. Baixe o arquivo JSON com as credenciais
5. Coloque o arquivo em `src/main/resources/credentials/google-calendar-credentials.json`

## Executando o Projeto

### Pré-requisitos
- Java 17
- Maven
- PostgreSQL

### Passos
1. Clone o repositório
2. Configure o arquivo `application.properties`
3. Execute `mvn clean install` para compilar o projeto
4. Execute `mvn spring-boot:run` para iniciar a aplicação

## Testes

Para executar os testes:
```
mvn test
```

## Autor

Equipe de Desenvolvimento WAD
