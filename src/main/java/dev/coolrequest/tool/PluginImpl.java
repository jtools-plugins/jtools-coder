package dev.coolrequest.tool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.lhstack.tools.plugins.IPlugin;
import dev.coolrequest.tool.common.LogContext;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class PluginImpl implements IPlugin {

    private final Map<String, JComponent> components = new HashMap<String, JComponent>();

    private final Map<String, Project> projectMap = new HashMap<>();

    @Override
    public JComponent createPanel(Project project) {
        return components.computeIfAbsent(project.getLocationHash(), key -> {
            return new MainPanel().setProject(project).createPanel();
        });
    }

    @Override
    public void unInstall() {
        ResourceBundle.clearCache();
        for (Project project : projectMap.values()) {
            LogContext.getInstance(project).release();
        }
    }

    @Override
    public void openProject(Project project) {
        projectMap.put(project.getLocationHash(), project);
    }

    @Override
    public void closeProject(String projectHash) {
        components.remove(projectHash);
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
        return "1.0.8";
    }
}
