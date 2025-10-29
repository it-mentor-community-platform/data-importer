# Data Importer

### Основная информация

Сервис-импортер, обеспечивающий автоматическое наполнение базы данных актуальной информацией из таблиц Google.


## Стек

- Spring Boot 3
- Java 21
- [Google API Java Client](https://github.com/googleapis/google-api-java-client) для работы с Google Sheets API



### Ссылки на репозиторий документации
- [Бизнес аналитика - импорт данных](https://github.com/it-mentor-community-platform/meta/blob/main/business-analytics/functionality/data-import.md)
- [Системная аналитика - импорт данных](https://github.com/it-mentor-community-platform/meta/blob/main/system-analytics/functionality/data-import.md)

### Сборка и запуск Docker-образа

1. Сборка образа
    ```bash
    docker build -t data-importer:local-stack .
    ```
2. Запуск контейнера с активным профилем `local-stack`
    ```bash
    docker run -d -p 8080:8080 --name data-importer -e SPRING_PROFILES_ACTIVE=local-stack data-importer:local-stack
    ```
