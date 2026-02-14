# BookmarksCC - Coding Guidelines

## Project Overview
BookmarksCC is a JavaFX desktop application for managing bookmarks with Portuguese Citizen Card integration. The application uses SQLite for local data storage and Bouncy Castle for cryptographic operations.

## Architecture & Technology Stack
- **Language**: Java 21
- **UI Framework**: JavaFX with FXML support
- **Database**: SQLite via JDBC
- **Cryptography**: Bouncy Castle (bcprov-jdk18on)
- **Build System**: Maven
- **Package Structure**: `pt.isep.bookmarkscc.*`

## Key Dependencies
```xml
<!-- JavaFX for UI -->
<dependency>
  <groupId>org.openjfx</groupId>
  <artifactId>javafx-controls</artifactId>
  <version>21.0.4</version>
</dependency>

<!-- SQLite for data persistence -->
<dependency>
  <groupId>org.xerial</groupId>
  <artifactId>sqlite-jdbc</artifactId>
  <version>3.46.1.0</version>
</dependency>

<!-- Bouncy Castle for cryptography -->
<dependency>
  <groupId>org.bouncycastle</groupId>
  <artifactId>bcprov-jdk18on</artifactId>
  <version>1.78.1</version>
</dependency>
```

## Development Workflow
- **Build**: `mvn clean compile`
- **Run**: `mvn javafx:run` (uses JavaFX maven plugin configured for `pt.isep.bookmarkscc.App`)
- **Package**: `mvn package` (creates executable JAR)

## Code Patterns & Conventions
- **Main Class**: `pt.isep.bookmarkscc.App` extends `javafx.application.Application`
- **JavaFX Integration**: Use FXML for complex UI layouts, programmatic creation for simple components
- **Database Access**: Use JDBC with prepared statements for SQLite operations
- **Cryptography**: Leverage Bouncy Castle providers for secure bookmark storage and citizen card operations

## Security Considerations
- Use Bouncy Castle for all cryptographic operations involving citizen card data
- Implement proper key management for bookmark encryption
- Follow SQLite best practices for data integrity

## File Structure Expectations
```
src/main/java/pt/isep/bookmarkscc/
├── App.java              # Main application class
├── model/                # Data models (Bookmark, User, etc.)
├── view/                 # FXML controllers and UI logic
├── controller/           # Business logic controllers
├── service/              # Database and external service integrations
└── util/                 # Utility classes (CryptoUtils, DBUtils, etc.)
```

## Integration Points
- **Citizen Card**: Implement authentication and data retrieval using Portuguese Citizen Card APIs
- **Database**: SQLite database stored locally (consider `~/.bookmarkscc/db.sqlite`)
- **Cryptography**: Encrypt sensitive bookmark data using user-specific keys

## Common Tasks
- Adding new bookmark types: Extend model classes and update FXML controllers
- Database schema changes: Use SQLite ALTER TABLE or migration scripts
- UI enhancements: Create FXML files in `src/main/resources/` and corresponding controllers</content>
<parameter name="filePath">/home/pr/bookmarkscc/.github/copilot-instructions.md