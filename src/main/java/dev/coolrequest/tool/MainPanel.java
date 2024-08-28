package dev.coolrequest.tool;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import dev.coolrequest.tool.common.I18n;
import dev.coolrequest.tool.common.LogContext;
import dev.coolrequest.tool.views.coder.CoderView;
import dev.coolrequest.tool.views.script.ScriptView;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainPanel extends JPanel implements CoolToolPanel, Disposable {
    private Project project;

    private final List<Disposable> disposables = new ArrayList<>();


    public MainPanel setProject(Project project) {
        this.project = project;
        return this;
    }

    @Override
    public MainPanel createPanel() {
        setLayout(new BorderLayout());
        try {
            //初始化
            LogContext.getInstance(project);
            JBTabs jbTabs = new JBTabsImpl(project);
            CoderView coderView = new CoderView(project, false);

            TabInfo encoderTabInfo = new TabInfo(coderView);
            encoderTabInfo.setText(I18n.getString("coder.title", project));
            jbTabs.addTab(encoderTabInfo);
            ScriptView scriptView = new ScriptView(project);
            TabInfo decoderTabInfo = new TabInfo(scriptView);
            decoderTabInfo.setText(I18n.getString("script.title", project));
            jbTabs.addTab(decoderTabInfo);
            add(jbTabs.getComponent(), BorderLayout.CENTER);
            disposables.add(coderView);
            disposables.add(scriptView);
        } catch (Throwable e) {
            JDialog jd = new JDialog();
            jd.setTitle("启动插件失败提示");
            jd.setSize(600, 400);
            jd.setAlwaysOnTop(true);
            JBTextArea jbTextArea = new JBTextArea();
            jbTextArea.setEditable(false);
            jbTextArea.setText(e.getMessage() + "\n");
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                jbTextArea.append(stackTraceElement.toString() + "\n");
            }
            JBScrollPane jbScrollPane = new JBScrollPane(jbTextArea);
            jd.getContentPane().add(jbScrollPane);
            jd.setLocationRelativeTo(null);
            jd.setVisible(true);
        }
        return this;
    }

    @Override
    public void showTool() {

    }

    @Override
    public void closeTool() {

    }

    @Override
    public void dispose() {
        LogContext.dispose(project);
        disposables.forEach(Disposable::dispose);
    }
}
