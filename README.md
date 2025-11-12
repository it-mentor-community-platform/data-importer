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

   
### Локальный запуск и тестирование
1. Для сервиса Data Importer необходим файл credentials.json 
   - Скопируй его в src/main/resources
   - В корне проекта создай файл .env:
   ```env
   SPRING_PROFILES_ACTIVE=ide
   GOOGLE_APPLICATION_CREDENTIALS=src/main/resources/credentials.json
   ```
   
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