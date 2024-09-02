package dev.coolrequest.tool.components;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Supplier;

public abstract class DynamicIconAction extends AnAction {

    private final @NotNull Supplier<Icon> dynamicIcon;

    public DynamicIconAction(@NotNull Supplier<@NlsActions.ActionText String> dynamicText, @NotNull Supplier<Icon> dynamicIcon) {
        super(dynamicText, dynamicIcon.get());
        this.dynamicIcon = dynamicIcon;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setIcon(dynamicIcon.get());
    }
}
