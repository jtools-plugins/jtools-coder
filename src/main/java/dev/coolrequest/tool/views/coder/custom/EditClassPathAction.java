package dev.coolrequest.tool.views.coder.custom;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.util.Icons;
import dev.coolrequest.tool.common.CacheConstant;
import dev.coolrequest.tool.components.MultiLanguageTextField;
import dev.coolrequest.tool.components.SimpleFrame;
import dev.coolrequest.tool.state.GlobalState;
import dev.coolrequest.tool.state.ProjectState;
import dev.coolrequest.tool.state.ProjectStateManager;
import dev.coolrequest.tool.state.Scope;
import dev.coolrequest.tool.utils.LibraryUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 添加类路径操作
 *
 * @author lhstack
 * @date 2024/08/12
 */
public class EditClassPathAction extends AnAction {

    private final Project project;

    private final AtomicBoolean state = new AtomicBoolean(false);
    private final MultiLanguageTextField multiLanguageTextField;
    private SimpleFrame frame;

    public EditClassPathAction(Project project) {
        super(() -> "编辑依赖", Icons.EDIT);
        this.project = project;
        this.multiLanguageTextField = new MultiLanguageTextField(PlainTextFileType.INSTANCE, project);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if (state.compareAndSet(false, true)) {
            ProjectState projectState = ProjectStateManager.load(project);
            if (projectState.getScope() == Scope.PROJECT) {
                multiLanguageTextField.setText(projectState.getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CLASSPATH).orElse(""));
            } else {
                multiLanguageTextField.setText(GlobalState.getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CLASSPATH).orElse(""));
            }
            this.frame = new SimpleFrame(multiLanguageTextField, "编辑classpath依赖", new Dimension(800, 600));
            this.frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    String text = multiLanguageTextField.getText();
                    if (projectState.getScope() == Scope.PROJECT) {
                        projectState.putCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CLASSPATH, text);
                        ProjectStateManager.store(project);
                        LibraryUtils.refreshLibrary(project,"Coder:Custom:Project: ",text);
                    } else {
                        GlobalState.putCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CLASSPATH, text);
                        LibraryUtils.refreshLibrary(project,"Coder:Custom:Global: ",text);
                    }

                    state.set(false);
                }
            });
            this.frame.setVisible(true);
        } else {
            this.frame.toFront();
        }
    }
}
