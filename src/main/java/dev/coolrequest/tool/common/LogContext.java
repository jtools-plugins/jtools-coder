package dev.coolrequest.tool.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.content.ContentFactory;
import dev.coolrequest.tool.components.PopupMenu;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogContext {

    private static final Map<String, LogContext> CACHE = new ConcurrentHashMap<>();
    private final JBTextArea textArea;
    private final JBScrollPane scrollLogPanel;
    private final Project project;
    private final JScrollBar verticalScrollBar;
    private final JScrollBar horizontalScrollBar;
    private Boolean isInstall = false;

    public LogContext(Project project) {
        this.textArea = new JBTextArea();
        this.textArea.setEditable(false);
        PopupMenu.attachClearMenu(I18n.getString("script.clearLog", project), Icons.CLEAR, this.textArea);
        this.scrollLogPanel = new JBScrollPane(textArea);
        this.scrollLogPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.scrollLogPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.verticalScrollBar = this.scrollLogPanel.getVerticalScrollBar();
        this.horizontalScrollBar = this.scrollLogPanel.getHorizontalScrollBar();
        this.project = project;
    }

    public Logger getLogger(Class<?> clazz) {
        return new TextAreaLogger(clazz, textArea, this.verticalScrollBar, this.horizontalScrollBar);
    }

    public Logger getLogger(String loggerName) {
        return new TextAreaLogger(loggerName, textArea, this.verticalScrollBar, this.horizontalScrollBar);
    }


    public static Logger getLogger(String loggerName, Project project) {
        return getInstance(project).getLogger(loggerName);
    }

    public static void show(Project project) {
        getInstance(project).showWindow(project);
    }

    private void showWindow(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow;
        if (isInstall) {
            toolWindow = toolWindowManager.getToolWindow(GlobalConstant.CODER_LOG_CONSOLE);
            if (toolWindow != null) {
                toolWindow.show();
            } else {
                toolWindow = toolWindowManager.registerToolWindow(GlobalConstant.CODER_LOG_CONSOLE, false, ToolWindowAnchor.BOTTOM);
                toolWindow.setTitle(GlobalConstant.CODER_LOG_CONSOLE);
                ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
                toolWindow.getContentManager().addContent(contentFactory.createContent(scrollLogPanel, "日志", true));
                toolWindow.show();
            }
        } else {
            toolWindowManager.unregisterToolWindow(GlobalConstant.CODER_LOG_CONSOLE);
            toolWindow = toolWindowManager.registerToolWindow(GlobalConstant.CODER_LOG_CONSOLE, false, ToolWindowAnchor.BOTTOM);
            toolWindow.setTitle(GlobalConstant.CODER_LOG_CONSOLE);
            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            toolWindow.getContentManager().addContent(contentFactory.createContent(scrollLogPanel, "日志", true));
            isInstall = true;
            toolWindow.show();
        }

    }

    public static LogContext getInstance(Project project) {
        return CACHE.computeIfAbsent(project.getLocationHash(), k -> new LogContext(project));
    }

    public static Logger getSysLog(Project project) {
        return getLogger("系统日志", project);
    }
}
