All commands provided below are meant to be run from the root folder.

Currently dev compose contains:

- MySQL database
- Mailpit

To start development containers:
`docker compose -f docker/docker-compose.dev.yml up -d`

To run only one of the containers you can use one of the following commands:

- `docker compose -f docker/docker-compose.dev.yml up -d mysql` to run MySQL
- `docker compose -f docker/docker-compose.dev.yml up -d mailpit` to run Mailpit

#### MySQL

MySQL container contains mysql 8.4 database on host port 3306

#### Mailpit

To send e-mails locally set up `application.properties` like that:

```properties
spring.mail.host=localhost
spring.mail.port=1025
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

app.mail.from=no-reply@local.test
```

It will ensure that all e-mails are sent internally. App will send an e-mail from no-reply@local.test and Mailpit will
receive it. The Mailpit mailbox is available via http://localhost:8025/
