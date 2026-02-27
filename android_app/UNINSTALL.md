# Uninstall Android Development Environment

## 1. Remove Android SDK
```bash
rm -rf ~/Documents/private/android-finance-app/android-sdk
```

## 2. Remove Gradle cache
```bash
rm -rf ~/.gradle
```

## 3. Remove Java
```bash
brew uninstall openjdk@17
```

Then remove from `~/.zshrc`:
```bash
# Java (OpenJDK 17)
export JAVA_HOME=$(brew --prefix openjdk@17)
export PATH="$JAVA_HOME/bin:$PATH"
```

## 4. Remove direnv config
```bash
rm ~/Documents/private/android-finance-app/.envrc
```

## 5. Optional: Remove Android emulator data (if used)
```bash
rm -rf ~/.android
```
