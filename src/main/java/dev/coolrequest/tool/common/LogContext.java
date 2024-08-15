package dev.coolrequest.tool.common;

import com.intellij.build.BuildTextConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;

import java.util.Collections;

public class LogContext {

    private final Project project;
    private final BuildTextConsoleView consoleView;
    ToolWindow toolWindow;

    public LogContext(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        toolWindow = toolWindowManager.getToolWindow(GlobalConstant.CODER_LOG_CONSOLE);
        //如果窗口存在,需要停掉之前的
        if (toolWindow != null) {
            toolWindow.hide();
            toolWindowManager.unregisterToolWindow(GlobalConstant.CODER_LOG_CONSOLE);
        }
        this.consoleView = new BuildTextConsoleView(project, true, Collections.emptyList());
        toolWindow = toolWindowManager.registerToolWindow(GlobalConstant.CODER_LOG_CONSOLE, false, ToolWindowAnchor.BOTTOM);
        toolWindow.setTitle(GlobalConstant.CODER_LOG_CONSOLE);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(contentFactory.createContent(consoleView.getComponent(), "日志", true));
        this.project = project;
    }

    public Logger getLogger(Class<?> clazz) {
        return new TextAreaLogger(clazz, consoleView);
    }

    public Logger getLogger(String loggerName) {
        return new TextAreaLogger(loggerName, consoleView);
    }


    public static Logger getLogger(String loggerName, Project project) {
        return getInstance(project).getLogger(loggerName);
    }

    public static void show(Project project) {
        getInstance(project).showWindow();
    }

    private void showWindow() {
        toolWindow.activate(() -> {

        });
    }

    public static LogContext getInstance(Project project) {
        LogContext logContext = project.getUserData(GlobalConstant.CODER_LOG_CONSOLE_KEY);
        if (logContext == null) {
            logContext = new LogContext(project);
            project.putUserData(GlobalConstant.CODER_LOG_CONSOLE_KEY, logContext);
        }
        return logContext;
    }

    public static Logger getSysLog(Project project) {
        return getLogger("SysLog", project);
    }
}
