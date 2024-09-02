package dev.coolrequest.tool.views.coder.custom;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import dev.coolrequest.tool.common.CacheConstant;
import dev.coolrequest.tool.common.Icons;
import dev.coolrequest.tool.components.DynamicIconAction;
import dev.coolrequest.tool.state.GlobalState;
import dev.coolrequest.tool.state.ProjectState;
import dev.coolrequest.tool.state.ProjectStateManager;
import dev.coolrequest.tool.state.Scope;
import dev.coolrequest.tool.utils.LibraryUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class RefreshClassPathAction extends DynamicIconAction {

    private final Project project;

    public RefreshClassPathAction(Project project) {
        super(() -> "刷新依赖", Icons.REFRESH);
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ProjectState projectState = ProjectStateManager.load(project);
        if (projectState.getScope() == Scope.PROJECT) {
            projectState.getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CLASSPATH).filter(StringUtils::isNotBlank).ifPresent(classpath -> LibraryUtils.refreshLibrary(project, "Coder:Custom:Project: ", classpath));
        } else {
            GlobalState.getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CLASSPATH).filter(StringUtils::isNotBlank).ifPresent(classpath -> LibraryUtils.refreshLibrary(project, "Coder:Custom:Global: ", classpath));
        }
    }
}
