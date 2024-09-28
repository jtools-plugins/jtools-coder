package dev.coolrequest.tool.views.coder.custom;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import dev.coolrequest.tool.common.CacheConstant;
import dev.coolrequest.tool.common.I18n;
import dev.coolrequest.tool.common.LogContext;
import dev.coolrequest.tool.common.Logger;
import dev.coolrequest.tool.components.DynamicIconAction;
import dev.coolrequest.tool.components.MultiLanguageTextField;
import dev.coolrequest.tool.state.GlobalState;
import dev.coolrequest.tool.state.ProjectStateManager;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RunAction extends DynamicIconAction {

    private final MultiLanguageTextField codeTextField;
    private final Supplier<GroovyShell> groovyShell;
    private final Project project;
    private final Logger contextLogger;
    private final Logger sysLog;

    public RunAction(MultiLanguageTextField codeTextField, Supplier<GroovyShell> groovyShell, Project project, Logger contextLogger) {
        super(() -> I18n.getString("coder.custom.run", project), () -> AllIcons.Actions.Execute);
        this.codeTextField = codeTextField;
        this.groovyShell = groovyShell;
        this.project = project;
        this.contextLogger = contextLogger;
        this.sysLog = LogContext.getSysLog(project);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            String code = this.codeTextField.getText();
            if (StringUtils.isNotBlank(code)) {
                LogContext.show(project);
                GroovyShell shell = groovyShell.get();
                try {
                    Script script = shell.parse(code);
                    Binding binding = new Binding();
                    CoderRegistry coderRegistry = new CoderRegistry(new ArrayList<>());
                    binding.setVariable("coder", coderRegistry);
                    binding.setVariable("log", contextLogger);
                    binding.setVariable("sysLog", sysLog);
                    //项目环境变量
                    binding.setVariable("projectEnv", ProjectStateManager.load(project).getJsonObjCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_ENVIRONMENT));
                    //全局环境变量
                    binding.setVariable("globalEnv", GlobalState.getJsonObjCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_ENVIRONMENT));
                    script.setBinding(binding);
                    FutureTask<Object> futureTask = new FutureTask<>(script::run);
                    try {
                        contextLogger.info("开始尝试运行自定义coder脚本");
                        Thread thread = new Thread(futureTask);
                        thread.start();
                        futureTask.get(10, TimeUnit.SECONDS);
                        contextLogger.info("尝试运行自定义coder脚本成功");
                    } catch (Throwable err) {
                        futureTask.cancel(true);
                        contextLogger.error("尝试运行自定义coder失败,错误信息: " + err);
                    }
                } catch (Throwable err) {
                    throw new RuntimeException(err);
                } finally {
                    shell.getClassLoader().close();
                }
            }
        } catch (Throwable err) {
            contextLogger.error("尝试运行自定义coder脚本失败,错误信息: " + err);
        }
    }
}
