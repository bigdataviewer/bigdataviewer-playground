# File Editing Rules
- Use relative paths only
- Use forward slashes (/) not backslashes (\)
- Navigate to workspace root before any file operations
```

## Alternative 1: Use VS Code Extension

Switch to the VS Code extension for a more stable Windows experience . The VS Code extension has better Windows compatibility than the CLI:

1. Install the Claude Code extension in VS Code
2. Use it instead of the terminal version
3. File editing works much more reliably

## Alternative 2: Use Java for File Edits

If you need Claude to make Java-based file edits, instruct it like this:
```
When editing files, use a Java snippet via bash:
java -cp . -c 'String content = Files.readString(Path.of("file.java"));
content = content.replace("old", "new");
Files.writeString(Path.of("file.java"), content);'
```

Or use Windows batch commands:
```
For file edits, use PowerShell commands:
(Get-Content file.java) -replace 'old', 'new' | Set-Content file.java