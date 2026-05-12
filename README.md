# Data Importer

### Основная информация

Сервис-импортер, обеспечивающий автоматическое наполнение базы данных актуальной информацией из таблиц Google.


## Стек

- Spring Boot 3
- Java 21
- [Google API Java Client](https://github.com/googleapis/google-api-java-client) для работы с Google Sheets API

## Ссылки на репозиторий документации
- [Бизнес аналитика - импорт данных](https://github.com/it-mentor-community-platform/meta/blob/main/business-analytics/functionality/data-import.md)
- [Системная аналитика - импорт данных](https://github.com/it-mentor-community-platform/meta/blob/main/system-analytics/functionality/data-import.md)

## Инструкция по созданию файла credentials.json

https://github.com/it-mentor-community-platform/data-importer/issues/4#issuecomment-3797988709

## Локальный запуск и тестирование
1. Для сервиса Data Importer необходимо получить содержимое credentials.json (из GitHub Secrets или у тимлида).
   - В корне проекта создайте файл .env
   - Добавьте переменную и вставьте в неё весь JSON:
   ```env
   GOOGLE_APPLICATION_CREDENTIALS_JSON={"type": "service_account", ...}
   ```
   - **Важно**: JSON должен быть в одну строку. Чтобы быстро это сделать в IDEA: выделите JSON и нажми Ctrl + Shift + J.
   

2. Запуск
   - Через консоль
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=ide'
   ```

    - В IntelliJ IDEA
      * Run -> Edit Configurations....
      * В поле Active profiles введите имя профиля: `ide`
      

3. Тестирование

   - Запустить тесты из `user-import-test.http`


4. Swagger документация 

   - ```http://localhost:8082/swagger-ui.html```
