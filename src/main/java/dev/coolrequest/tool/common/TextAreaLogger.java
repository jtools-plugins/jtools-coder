package dev.coolrequest.tool.common;

import com.intellij.ui.components.JBTextArea;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TextAreaLogger implements Logger {

    private final JBTextArea textArea;

    private final String loggerName;
    private final JScrollBar verticalScrollBar;
    private final JScrollBar horizontalScrollBar;

    public TextAreaLogger(Class<?> clazz, JBTextArea textArea, JScrollBar verticalScrollBar, JScrollBar horizontalScrollBar) {
        this(clazz.getName(), textArea, verticalScrollBar, horizontalScrollBar);
    }

    public TextAreaLogger(String loggerName, JBTextArea textArea, JScrollBar verticalScrollBar, JScrollBar horizontalScrollBar) {
        this.textArea = textArea;
        this.loggerName = loggerName;
        this.verticalScrollBar = verticalScrollBar;
        this.horizontalScrollBar = horizontalScrollBar;
    }

    @Override
    public void info(Object msg) {
        this.message("INFO", msg);
    }

    @Override
    public void warn(Object msg) {
        this.message("WARN", msg);
    }

    private void message(String level, Object msg) {
        String text = textArea.getText();
        String message = msg != null ? msg.toString() : "";
        String log = String.format("%s [%s] %s #%s - %s", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")), Thread.currentThread().getName(), level, loggerName, message);
        if (StringUtils.isNotBlank(text)) {
            textArea.append("\n");
            textArea.append(log);
        } else {
            textArea.append(log);
        }
        horizontalScrollBar.setValue(horizontalScrollBar.getMinimum());
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }

    @Override
    public void error(Object msg) {
        this.message("ERROR", msg);
    }
}
