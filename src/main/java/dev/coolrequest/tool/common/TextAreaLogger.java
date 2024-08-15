package dev.coolrequest.tool.common;

import com.intellij.build.BuildTextConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TextAreaLogger implements Logger {

    private final BuildTextConsoleView consoleView;

    private final String loggerName;

    public TextAreaLogger(Class<?> clazz, BuildTextConsoleView consoleView) {
        this(clazz.getName(), consoleView);
    }

    public TextAreaLogger(String loggerName, BuildTextConsoleView consoleView) {
        this.consoleView = consoleView;
        this.loggerName = loggerName;
    }

    @Override
    public void info(Object msg) {
        this.message("INFO", msg);
    }

    @Override
    public void warn(Object msg) {
        this.message("WARN", msg);
    }

    @Override
    public void debug(Object msg) {
        this.message("DEBUG", msg);
    }

    private void message(String level, Object msg) {

        String message = msg != null ? msg.toString() : "";
        String log = String.format("%s [%-10s] %-5s %-8s - %s", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")), substr(Thread.currentThread().getName(), 10), level, "#" + loggerName, message);
        int contentSize = consoleView.getContentSize();
        if (contentSize > 0) {
            consoleView.print("\n" + log, getType(level));
        } else {
            consoleView.print(log, getType(level));
        }
    }

    private String substr(String text, int len) {
        if (text.length() <= len) {
            return text;
        }
        return text.substring(0, len - 2) + "..";
    }

    private ConsoleViewContentType getType(String level) {
        switch (level) {
            case "INFO":
                return ConsoleViewContentType.LOG_INFO_OUTPUT;
            case "WARN":
                return ConsoleViewContentType.LOG_WARNING_OUTPUT;
            case "DEBUG":
                return ConsoleViewContentType.LOG_DEBUG_OUTPUT;
            default:
                return ConsoleViewContentType.ERROR_OUTPUT;
        }
    }

    @Override
    public void error(Object msg) {
        this.message("ERROR", msg);
    }
}
