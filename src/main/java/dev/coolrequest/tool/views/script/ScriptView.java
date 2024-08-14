package dev.coolrequest.tool.views.script;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import dev.coolrequest.tool.common.*;
import dev.coolrequest.tool.components.MultiLanguageTextField;
import dev.coolrequest.tool.components.SimpleFrame;
import dev.coolrequest.tool.state.ProjectState;
import dev.coolrequest.tool.state.ProjectStateManager;
import dev.coolrequest.tool.utils.ClassLoaderUtils;
import dev.coolrequest.tool.utils.LibraryUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScriptView extends JPanel {

    private final Logger sysLog;
    private final Logger contextLogger;
    private Project project;

    private final JBTextArea classPathTextArea = new JBTextArea();


    public ScriptView(Project project) {
        super(new BorderLayout());
        this.project = project;
        sysLog = LogContext.getSysLog(project);
        this.contextLogger = LogContext.getLogger("Groovy", project);
        ProjectStateManager.load(project)
                .getOpStrCache(CacheConstant.SCRIPT_VIEW_CACHE_CLASSPATH)
                .ifPresent(text -> {
                    DumbService.getInstance(project).runWhenSmart(() -> {
                        LibraryUtils.refreshLibrary(project, "Coder:Groovy: ", text);
                    });
                    Font font = classPathTextArea.getFont();
                    if (font != null) {
                        classPathTextArea.setFont(new Font(font.getName(), font.getStyle(), 14));
                    } else {
                        classPathTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
                    }
                    classPathTextArea.setText(text);
                });

        CodePanel codePanel = new CodePanel(project);
        this.add(codePanel, BorderLayout.CENTER);
    }

    private void runScript(String script) {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        try (GroovyClassLoader groovyClassLoader = new GroovyClassLoader(ScriptView.class.getClassLoader(), compilerConfiguration)) {
            GroovyShell groovyShell = new GroovyShell(groovyClassLoader, compilerConfiguration);
            ProjectState projectState = ProjectStateManager.load(project);
            if (projectState.getBooleanCache(CacheConstant.SCRIPT_VIEW_CACHE_USING_PROJECT_LIBRARY)) {
                for (Library library : LibraryTablesRegistrar.getInstance().getLibraryTable(project).getLibraries()) {
                    for (VirtualFile file : library.getFiles(OrderRootType.CLASSES)) {
                        URL url = new File(file.getPresentableUrl()).toURI().toURL();
                        groovyClassLoader.addURL(url);
                    }
                }
            }
            String classPaths = classPathTextArea.getText();
            if (StringUtils.isNotBlank(classPaths)) {
                String[] classPathArray = classPaths.split("\n");
                for (String classPathItems : classPathArray) {
                    for (String classPath : classPathItems.split(",")) {
                        if (StringUtils.isNotBlank(StringUtils.trimToEmpty(classPath))) {
                            try {
                                URI uri = URI.create(StringUtils.trimToEmpty(classPath));
                                URL url = uri.toURL();
                                groovyClassLoader.addURL(url);
                            } catch (Throwable e) {
                                groovyClassLoader.addURL(new File(StringUtils.trimToEmpty(classPath)).toURI().toURL());
                            }
                        }
                    }
                }
            }
            Script groovyScript = groovyShell.parse(script);
            Binding binding = new Binding();
            binding.setVariable("sysLog", sysLog);
            binding.setVariable("log", contextLogger);
            groovyScript.setBinding(binding);
            FutureTask<Object> futureTask = new FutureTask<>(groovyScript::run);
            try {
                Thread thread = new Thread(futureTask);
                thread.start();
                futureTask.get(10, TimeUnit.SECONDS);
            } catch (Throwable e) {
                futureTask.cancel(true);
                contextLogger.error("脚本执行失败,错误信息: " + e);
            }
            ProjectStateManager.load(project).putCache(CacheConstant.SCRIPT_VIEW_CACHE_CODE, script);
            ProjectStateManager.store(project);
        } catch (Throwable e) {
            sysLog.error("groovy脚本执行错误: " + e.getMessage() + "\n");
        }
    }


    private class CodePanel extends JPanel {
        private final LanguageTextField languageTextField;

        public CodePanel(Project project) {
            super(new BorderLayout());
            LanguageFileType groovyFileType = (LanguageFileType) FileTypeManager.getInstance().getFileTypeByExtension("groovy");
            this.languageTextField = new MultiLanguageTextField(groovyFileType, project);
            ProjectStateManager.load(project).getOpStrCache(CacheConstant.SCRIPT_VIEW_CACHE_CODE).ifPresent(languageTextField::setText);
            JButton button = new JButton(I18n.getString("script.addclasspath.title", project));
            button.addMouseListener(new MouseAdapter() {

                private SimpleFrame frame;
                private final AtomicBoolean state = new AtomicBoolean(false);

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (state.compareAndSet(false, true)) {
                            this.frame = new SimpleFrame(new JBScrollPane(classPathTextArea), I18n.getString("script.addclasspath.title", project), new Dimension(800, 600));
                            frame.setVisible(true);
                            frame.addWindowListener(new WindowAdapter() {
                                @Override
                                public void windowClosing(WindowEvent e) {
                                    if (StringUtils.isNotBlank(classPathTextArea.getText())) {
                                        DumbService.getInstance(project).runWhenSmart(() -> {
                                            LibraryUtils.refreshLibrary(project, "Coder:Groovy: ", classPathTextArea.getText());
                                        });
                                        ProjectStateManager.load(project).putCache(CacheConstant.SCRIPT_VIEW_CACHE_CLASSPATH, classPathTextArea.getText());
                                        ProjectStateManager.store(project);
                                    } else {
                                        DumbService.getInstance(project).runWhenSmart(() -> LibraryUtils.refreshLibrary(project, "Coder:Groovy: ", ""));
                                        ProjectStateManager.load(project).putCache(CacheConstant.SCRIPT_VIEW_CACHE_CLASSPATH, "");
                                        ProjectStateManager.store(project);
                                    }
                                    state.set(false);
                                }
                            });
                        } else {
                            frame.toFront();
                        }
                    }
                }
            });
            JButton templateCodeButton = new JButton(I18n.getString("script.code.template", project));
            String templateCode = ClassLoaderUtils.getResourceToString("template/ScriptTemplateCode.groovy");
            templateCodeButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        String script = languageTextField.getText();
                        if (StringUtils.isNotBlank(script)) {
                            if (!StringUtils.equals(script, templateCode)) {
                                int i = JOptionPane.showConfirmDialog(null, "点击确定会覆盖原有代码,请谨慎操作", "警告", JOptionPane.YES_NO_OPTION);
                                if (i == JOptionPane.YES_OPTION) {
                                    languageTextField.setText(templateCode);
                                }
                            }
                        } else {
                            String templateCode = ClassLoaderUtils.getResourceToString("template/ScriptTemplateCode.groovy");
                            languageTextField.setText(templateCode);
                        }
                    }
                }
            });
            DefaultActionGroup defaultActionGroup = new DefaultActionGroup();
            ProjectState projectState = ProjectStateManager.load(project);
            defaultActionGroup.add(new ToggleAction(() -> I18n.getString("script.usingProjectLibrary", project), Icons.LIBRARY) {

                @Override
                public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
                    return projectState.getBooleanCache(CacheConstant.SCRIPT_VIEW_CACHE_USING_PROJECT_LIBRARY);
                }

                @Override
                public void setSelected(@NotNull AnActionEvent event, boolean state) {
                    boolean currentState = projectState.getBooleanCache(CacheConstant.SCRIPT_VIEW_CACHE_USING_PROJECT_LIBRARY);
                    if (currentState != state) {
                        projectState.putCache(CacheConstant.SCRIPT_VIEW_CACHE_USING_PROJECT_LIBRARY, state);
                        ProjectStateManager.store(project);
                    }
                }
            });
            defaultActionGroup.add(new AnAction(() -> "刷新依赖", Icons.REFRESH) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    DumbService.getInstance(project).runWhenSmart(() -> {
                        LibraryUtils.refreshLibrary(project, "Coder:Groovy: ", classPathTextArea.getText());
                    });
                }
            });
            defaultActionGroup.add(new AnAction(() -> "运行", Icons.RUN) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    //显示日志面板
                    LogContext.show(project);
                    String code = languageTextField.getText();
                    if (StringUtils.isNotBlank(code)) {
                        runScript(languageTextField.getText());
                    } else {
                        contextLogger.warn("执行代码为空");
                    }
                }
            });
            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("ScriptView", defaultActionGroup, true);
            add(createFlowLayoutPanel(button, templateCodeButton, actionToolbar.getComponent()), BorderLayout.NORTH);
            add(languageTextField, BorderLayout.CENTER);
        }
    }


    private JPanel createFlowLayoutPanel(JComponent... components) {
        JPanel jPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent component : components) {
            jPanel.add(component);
        }
        return jPanel;
    }
}
