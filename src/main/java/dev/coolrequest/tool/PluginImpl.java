package dev.coolrequest.tool;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.lhstack.tools.plugins.IPlugin;
import dev.coolrequest.tool.common.LogContext;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class PluginImpl implements IPlugin {

    private final Map<String, Disposable> components = new HashMap<>();

    @Override
    public JComponent createPanel(Project project) {
        return (JComponent) components.computeIfAbsent(project.getLocationHash(), key -> {
            return new MainPanel().setProject(project).createPanel();
        });
    }

    @Override
    public void closeProject(Project project) {
        Disposable disposable = components.remove(project.getLocationHash());
        if(disposable != null){
            Disposer.dispose(disposable);
        }
        Disposer.dispose(LogContext.getInstance(project));
    }


    @Override
    public Icon pluginIcon() {
        return IconLoader.findIcon("logo.svg", PluginImpl.class);
    }

    @Override
    public Icon pluginTabIcon() {
        return IconLoader.findIcon("icons/pluginTab.svg", PluginImpl.class);
    }

    @Override
    public String pluginName() {
        return "Coder";
    }

    @Override
    public String pluginDesc() {
        return "解码编码插件,同时支持groovy脚本调试";
    }

    @Override
    public String pluginVersion() {
        return "v1.0.8";
    }
}
