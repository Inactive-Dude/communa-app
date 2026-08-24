# communa-app

Quick run instructions

Windows (PowerShell):

```powershell
cd communa_1
$env:DB_PASSWORD = 'pass' # optional default for local dev
$env:MAIL_PASSWORD = ''   # set your mail password if using email features
.\mvnw.cmd -DskipTests package
java -jar target\communa-0.0.1-SNAPSHOT.jar
```

Windows (cmd.exe):

```
cd communa_1
set DB_PASSWORD=pass
set MAIL_PASSWORD=
mvnw.cmd -DskipTests package
java -jar target\communa-0.0.1-SNAPSHOT.jar
```

Notes:
- The app reads sensitive values from environment variables: `DB_PASSWORD`, `MAIL_PASSWORD`.
- Create the MySQL database `communa` and ensure MySQL is running before starting the app.
