package dev.coolrequest.tool.views.coder.custom;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import dev.coolrequest.tool.common.I18n;
import dev.coolrequest.tool.common.LogContext;
import dev.coolrequest.tool.common.Logger;
import dev.coolrequest.tool.components.DynamicIconAction;
import dev.coolrequest.tool.components.MultiLanguageTextField;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class CompileAction extends DynamicIconAction {

    private final MultiLanguageTextField codeTextField;
    private final Supplier<GroovyShell> groovyShell;
    private final Logger logger;
    private final Project project;

    public CompileAction(MultiLanguageTextField codeTextField, Supplier<GroovyShell> groovyShell, Project project, Logger contextLogger) {
        super(() -> I18n.getString("coder.custom.compile", project), () -> AllIcons.Actions.Compile);
        this.codeTextField = codeTextField;
        this.groovyShell = groovyShell;
        this.logger = contextLogger;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        if (StringUtils.isNotBlank(this.codeTextField.getText())) {
            LogContext.show(project);
            try {
                GroovyShell shell = groovyShell.get();
                try {
                    shell.parse(this.codeTextField.getText());
                    logger.info("编译代码通过");
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                } finally {
                    shell.getClassLoader().close();
                }
            } catch (Throwable e) {
                logger.info(String.format("编译失败,error: %s", e.getMessage()));
            }
        }
    }
}
