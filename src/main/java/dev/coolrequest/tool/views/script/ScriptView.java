package dev.coolrequest.tool.views.script;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.messages.MessageBusConnection;
import dev.coolrequest.tool.common.*;
import dev.coolrequest.tool.components.MultiLanguageTextField;
import dev.coolrequest.tool.components.SimpleFrame;
import dev.coolrequest.tool.state.ProjectState;
import dev.coolrequest.tool.state.ProjectStateManager;
import dev.coolrequest.tool.utils.ClassLoaderUtils;
import dev.coolrequest.tool.utils.ComponentUtils;
import dev.coolrequest.tool.utils.LibraryUtils;
import dev.coolrequest.tool.utils.ProjectUtils;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class ScriptView extends JPanel {

    private final Logger sysLog;
    private final Logger contextLogger;
    private final Project project;

    private final MultiLanguageTextField languageTextField;

    private final JBTextArea classPathTextArea;


    public ScriptView(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.sysLog = LogContext.getSysLog(project);
        this.contextLogger = LogContext.getLogger("Groovy", project);
        this.languageTextField = ProjectUtils.getOrCreate(project, GlobalConstant.CODER_GROOVY_CODE_KEY, () -> {
            LanguageFileType groovyFileType = (LanguageFileType) FileTypeManager.getInstance().getFileTypeByExtension("groovy");
            return new MultiLanguageTextField(groovyFileType, project, "CoderGroovy.groovy");
        });
        this.classPathTextArea = ProjectUtils.getOrCreate(project, GlobalConstant.CODER_GROOVY_CLASSPATH_KEY, () -> {
            JBTextArea jbTextArea = new JBTextArea();
            Font font = jbTextArea.getFont();
            if (font != null) {
                jbTextArea.setFont(new Font(font.getName(), font.getStyle(), 14));
            } else {
                jbTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
            }
            return jbTextArea;
        });
        ProjectStateManager.load(project)
                .getOpStrCache(CacheConstant.SCRIPT_VIEW_CACHE_CLASSPATH)
                .ifPresent(classPathTextArea::setText);
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


        public JButton classPathButton() {
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
            return button;
        }

        public JButton templateCodeButton() {
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
            return templateCodeButton;
        }

        public CodePanel(Project project) {
            super(new BorderLayout());
            ProjectStateManager.load(project).getOpStrCache(CacheConstant.SCRIPT_VIEW_CACHE_CODE).ifPresent(languageTextField::setText);
            DefaultActionGroup defaultActionGroup = new DefaultActionGroup();
            ProjectState projectState = ProjectStateManager.load(project);
            ToggleAction usingProjectLibrary = new ToggleAction(() -> I18n.getString("script.usingProjectLibrary", project), Icons.LIBRARY) {

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
            };
            AnAction refreshDepend = new AnAction(() -> "刷新依赖", Icons.REFRESH) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    DumbService.getInstance(project).runWhenSmart(() -> {
                        LibraryUtils.refreshLibrary(project, "Coder:Groovy: ", classPathTextArea.getText());
                    });
                }
            };
            AnAction run = new AnAction(() -> "运行", Icons.RUN) {
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
            };
            defaultActionGroup.add(usingProjectLibrary);
            defaultActionGroup.add(refreshDepend);
            defaultActionGroup.add(run);
            defaultActionGroup.add(new AnAction(() -> "在编辑器中打开", Icons.OPEN) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(languageTextField.getDocument());
                    if (psiFile != null) {
                        VirtualFile virtualFile = psiFile.getVirtualFile();
                        boolean hasExist = false;
                        for (FileEditor allEditor : fileEditorManager.getAllEditors()) {
                            VirtualFile file = allEditor.getFile();
                            if (virtualFile.hashCode() != file.hashCode() && StringUtils.equals(virtualFile.getPath(), file.getPath())) {
                                fileEditorManager.closeFile(file);
                            } else if (virtualFile.hashCode() == file.hashCode() && StringUtils.equals(virtualFile.getPath(), file.getPath())) {
                                fileEditorManager.openFile(file, true);
                                hasExist = true;
                            }
                        }
                        if (!hasExist) {
                            FileEditor[] fileEditors = fileEditorManager.openFile(psiFile.getVirtualFile(), true);
                            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("ScriptView", new DefaultActionGroup(usingProjectLibrary, refreshDepend, run), true);
                            for (FileEditor fileEditor : fileEditors) {
                                fileEditorManager.addTopComponent(fileEditor, ProjectUtils.getOrCreate(project, GlobalConstant.CODER_GROOVY_FILE_EDITOR_TOP_COMPONENT, () -> ComponentUtils.createFlowLayoutPanel(FlowLayout.RIGHT, classPathButton(), templateCodeButton(), actionToolbar.getComponent())));
                            }

                            Set<String> cacheKeys = ProjectUtils.getOrCreate(project, GlobalConstant.CODER_STATE_CACHE, HashSet::new);
                            //未初始化文件监听器
                            if (!cacheKeys.contains(GlobalConstant.CODER_GROOVY_FILE_EDITOR_LISTEN_INIT_KEY)) {
                                //拿到消息总线
                                MessageBusConnection connection = ProjectUtils.getOrCreate(project, GlobalConstant.MESSAGE_BUS_CONNECTION_KEY, () -> project.getMessageBus().connect());
                                //增加订阅文件监听器
                                connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                                    @Override
                                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                                        //拿到文件
                                        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(languageTextField.getDocument());
                                        //拿到文件编辑管理
                                        FileEditorManagerImpl editorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(project);
                                        if (psiFile != null) {
                                            VirtualFile psiFileVirtualFile = psiFile.getVirtualFile();
                                            VirtualFile newFile = event.getNewFile();
                                            //判断是否是同一个文件
                                            if (psiFileVirtualFile.hashCode() == newFile.hashCode() && StringUtils.equals(psiFileVirtualFile.getPath(), newFile.getPath())) {
                                                //拿到缓存的topComponent
                                                JComponent topComponent = ProjectUtils.getOrCreate(project, GlobalConstant.CODER_GROOVY_FILE_EDITOR_TOP_COMPONENT, () -> ComponentUtils.createFlowLayoutPanel(FlowLayout.RIGHT, classPathButton(), templateCodeButton(), actionToolbar.getComponent()));
                                                //拿到文件的所有的文件编辑
                                                for (FileEditor fileEditor : editorManager.getEditors(newFile)) {
                                                    EditorComposite composite = editorManager.getComposite(newFile);
                                                    if (composite != null) {
                                                        Optional<Component> optionalComponent = Stream.of(composite.getComponent().getComponents()).filter(component -> component == topComponent).findFirst();
                                                        //判断组件是否存在,如果不存在,则添加按钮
                                                        if (optionalComponent.isEmpty()) {
                                                            fileEditorManager.addTopComponent(fileEditor, topComponent);
                                                        }
                                                    }

                                                }
                                            }
                                        }
                                    }
                                });
                                cacheKeys.add(GlobalConstant.CODER_GROOVY_FILE_EDITOR_LISTEN_INIT_KEY);
                            }

                        }
                    }
                }
            });
            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("ScriptView", defaultActionGroup, true);
            add(ComponentUtils.createFlowLayoutPanel(FlowLayout.LEFT, classPathButton(), templateCodeButton(), actionToolbar.getComponent()), BorderLayout.NORTH);
            add(languageTextField, BorderLayout.CENTER);
        }
    }

}
