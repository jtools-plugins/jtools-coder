package dev.coolrequest.tool.views.coder;

import com.intellij.codeInsight.actions.CodeCleanupCodeProcessor;
import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.codeInsight.actions.RearrangeCodeProcessor;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import dev.coolrequest.tool.common.*;
import dev.coolrequest.tool.components.MultiLanguageTextField;
import dev.coolrequest.tool.components.SimpleFrame;
import dev.coolrequest.tool.state.GlobalState;
import dev.coolrequest.tool.state.ProjectState;
import dev.coolrequest.tool.state.ProjectStateManager;
import dev.coolrequest.tool.state.Scope;
import dev.coolrequest.tool.utils.ClassLoaderUtils;
import dev.coolrequest.tool.utils.ComponentUtils;
import dev.coolrequest.tool.utils.ProjectUtils;
import dev.coolrequest.tool.views.coder.custom.*;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CoderView extends JPanel implements DocumentListener {

    private final JComboBox<String> coderSourceBox = new JComboBox<>();
    private final JComboBox<String> coderTargetBox = new JComboBox<>();
    private final LeftSource leftSource;
    private final RightTarget rightTarget;
    private final List<Coder> baseCoders;
    private final List<Coder> dynamicCoders;
    private final Logger contextLogger;
    private final MultiLanguageTextField leftTextField;
    private final MultiLanguageTextField rightTextField;
    private final Project project;
    private final Logger sysLogger;
    private final MultiLanguageTextField codeTextField;
    private final boolean isEditor;
    private final boolean isConsole;
    private final DefaultComboBoxModel<String> sourceComboBoxModel;

    public CoderView(Project project, boolean isEditor, boolean isConsole) {
        super(new BorderLayout());
        this.isEditor = isEditor;
        this.isConsole = isConsole;
        this.project = project;
        this.contextLogger = LogContext.getLogger("Coder", project);
        this.sysLogger = LogContext.getSysLog(project);
        //加载Coder的实现
        List<Class<?>> coderClasses = ClassLoaderUtils.scan(clazz -> true, "dev.coolrequest.tool.views.coder.impl");
        this.baseCoders = coderClasses.stream().map(item -> {
                    try {
                        return item.getConstructor().newInstance();
                    } catch (Throwable ignore) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .map(Coder.class::cast)
                .sorted(Comparator.comparing(Coder::ordered))
                .collect(Collectors.toList());

        this.leftTextField = new MultiLanguageTextField(PlainTextFileType.INSTANCE, project);
        this.leftTextField.getDocument().addDocumentListener(this);
        this.rightTextField = new MultiLanguageTextField(PlainTextFileType.INSTANCE, project);
        this.rightTextField.setEnabled(false);

        this.codeTextField = ProjectUtils.getOrCreate(project, GlobalConstant.CODER_CUSTOM_CODE_KEY, () -> {
            //代码面板
            LanguageFileType groovyFileType = (LanguageFileType) FileTypeManager.getInstance().getFileTypeByExtension("groovy");
            MultiLanguageTextField codeTextField = new MultiLanguageTextField(groovyFileType, project, "CustomCoder.groovy");
            ProjectState projectState = ProjectStateManager.load(project);
            if (projectState.getScope() == Scope.PROJECT) {
                String script = projectState.getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CODE).orElse("");
                codeTextField.setText(script);
            } else {
                String script = GlobalState.getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CODE).orElse("");
                codeTextField.setText(script);
            }
            return codeTextField;
        });

        leftSource = new LeftSource();
        rightTarget = new RightTarget(project, createGroovyShell(project, () -> {
            ProjectState projectState = ProjectStateManager.load(project);
            if (projectState.getScope() == Scope.PROJECT) {
                return projectState.getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CLASSPATH).orElse("");
            }
            return GlobalState.getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CLASSPATH).orElse("");
        }));

        dynamicCoders = ProjectUtils.getOrCreate(project, GlobalConstant.CODER_KEY, () -> {
            List<Coder> coders = new ArrayList<>(baseCoders);
            ProjectState projectState = ProjectStateManager.load(project);
            String customCoderScript = projectState.getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CODE).orElse(null);
            String globalCustomCoderScript = GlobalState.getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CODE).orElse(null);
            if (customCoderScript != null || globalCustomCoderScript != null) {
                LogContext.show(project);
                if (!isEditor) {
                    contextLogger.info("load custom coders");
                }
                if (customCoderScript != null) {
                    loadCustomCoders(coders, customCoderScript, createGroovyShell(project, () -> ProjectStateManager.load(project).getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CLASSPATH).orElse("")));
                }
                if (globalCustomCoderScript != null) {
                    loadCustomCoders(coders, globalCustomCoderScript, createGroovyShell(project, () -> GlobalState.getOpStrCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_SCRIPT_CLASSPATH).orElse("")));
                }
            }
            coders.sort(Comparator.comparing(Coder::ordered));
            return coders;
        });

        //左侧下拉框内容
        Set<String> source = new LinkedHashSet<>();
        //右侧下拉框内容
        Set<String> target = new HashSet<>();

        //左侧第一个下拉框对应的Coder
        Coder coder = dynamicCoders.get(0);
        dynamicCoders.forEach(coderItem -> {
            //填充左侧下拉框内容
            source.add(coderItem.kind().source);
            //填充右侧下拉框内容,前提是左侧第一个下拉框支持的
            if (StringUtils.equals(coderItem.kind().source, coder.kind().source)) {
                target.add(coderItem.kind().target);
            }
        });

        this.sourceComboBoxModel = ProjectUtils.getOrCreate(project, GlobalConstant.CODER_SOURCE_BOX_MODEL_KEY, () -> new DefaultComboBoxModel<String>(source.toArray(new String[0])));

        DefaultComboBoxModel<String> targetComboBoxModel = ProjectUtils.getOrCreate(project, GlobalConstant.CODER_TARGET_BOX_MODEL_KEY, () -> new DefaultComboBoxModel<String>(target.toArray(new String[0])));

        coderSourceBox.setModel(sourceComboBoxModel);

        coderTargetBox.setModel(targetComboBoxModel);

        //添加左侧下拉框数据变更监听器,当左侧下拉框数据发生变更,联动更新右侧下拉框内容
        coderSourceBox.addItemListener(e -> {
            String sourceValue = String.valueOf(sourceComboBoxModel.getSelectedItem());
            coderTargetBox.removeAllItems();
            dynamicCoders.stream().filter(item -> StringUtils.equals(item.kind().source, sourceValue)).map(item -> item.kind().target).forEach(coderTargetBox::addItem);
//            transform();
        });
        coderTargetBox.addItemListener(e -> transform());
        JBSplitter jbSplitter = new JBSplitter();
        jbSplitter.setFirstComponent(leftSource);
        jbSplitter.setSecondComponent(rightTarget);
        add(jbSplitter, BorderLayout.CENTER);
    }

    /**
     * 加载自定义coder
     *
     * @param coders
     * @param customCoderScript
     * @param groovyShell
     */
    private void loadCustomCoders(List<Coder> coders, String customCoderScript, Supplier<GroovyShell> groovyShell) {
        CoderRegistry coderRegistry = new CoderRegistry(coders);
        Binding binding = new Binding();
        binding.setVariable("coder", coderRegistry);
        binding.setVariable("sysLog", sysLogger);
        binding.setVariable("log", contextLogger);
        binding.setVariable("projectEnv", ProjectStateManager.load(this.project).getJsonObjCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_ENVIRONMENT));
        binding.setVariable("globalEnv", GlobalState.getJsonObjCache(CacheConstant.CODER_VIEW_CUSTOM_CODER_ENVIRONMENT));
        Script script = groovyShell.get().parse(customCoderScript);
        script.setBinding(binding);
        FutureTask<Object> futureTask = new FutureTask<>(script::run);
        try {
            Thread thread = new Thread(futureTask);
            thread.start();
            futureTask.get(10, TimeUnit.SECONDS);
        } catch (Throwable e) {
            futureTask.cancel(true);
            contextLogger.error("安装自定义coder失败,错误信息: " + e.getMessage());
        }
        if (CollectionUtils.isNotEmpty(coderRegistry.getRegistryCoders())) {
            coders.clear();
            coders.addAll(this.baseCoders);
            coders.addAll(coderRegistry.getRegistryCoders());
            coders.sort(Comparator.comparing(Coder::ordered));
            //左侧下拉框内容
            Set<String> source = new LinkedHashSet<>();
            //右侧下拉框内容
            Set<String> target = new LinkedHashSet<>();
            //左侧第一个下拉框对应的Coder
            Coder coder = coders.get(0);
            coders.forEach(coderItem -> {
                //填充左侧下拉框内容
                source.add(coderItem.kind().source);
                //填充右侧下拉框内容,前提是左侧第一个下拉框支持的
                if (StringUtils.equals(coderItem.kind().source, coder.kind().source)) {
                    target.add(coderItem.kind().target);
                }
            });
            this.contextLogger.info("load coders: " + coders);
            this.contextLogger.info("coder sources: " + source);
            this.contextLogger.info("target sources: " + target);
        }
        List<Coder> noRegistryCoders = coderRegistry.getNoRegistryCoders();
        if (CollectionUtils.isNotEmpty(noRegistryCoders)) {
            String noRegistryCodersLog = noRegistryCoders.stream().map(item -> String.format("source: %s, target: %s", item.kind().source, item.kind().target)).collect(Collectors.joining("\n"));
            contextLogger.info("以上coder已经存在,不能注册: \n" + noRegistryCodersLog);
        }
    }

    /**
     * 创建groovyShell
     *
     * @return
     */
    private Supplier<GroovyShell> createGroovyShell(Project project, Supplier<String> classpathSupplier) {
        return () -> {
            GroovyShell groovyShell = new GroovyShell(CoderView.class.getClassLoader());
            if (ProjectStateManager.load(project).isCustomCoderUsingProjectLibrary()) {
                try {
                    for (Library library : LibraryTablesRegistrar.getInstance().getLibraryTable(project).getLibraries()) {
                        for (VirtualFile file : library.getFiles(OrderRootType.CLASSES)) {
                            URL url = new File(file.getPresentableUrl()).toURI().toURL();
                            groovyShell.getClassLoader().addURL(url);
                        }
                    }
                } catch (Throwable e) {
                    contextLogger.error("create groovyShell 失败,错误信息: " + e.getMessage());
                }
            }
            String classPaths = classpathSupplier.get();
            if (StringUtils.isNotBlank(classPaths)) {
                String[] classPathArray = classPaths.split("\n");
                for (String classPathItems : classPathArray) {
                    for (String classPath : classPathItems.split(",")) {
                        if (StringUtils.isNotBlank(StringUtils.trimToEmpty(classPath))) {
                            try {
                                URI uri = URI.create(StringUtils.trimToEmpty(classPath));
                                URL url = uri.toURL();
                                groovyShell.getClassLoader().addURL(url);
                            } catch (Throwable e) {
                                try {
                                    groovyShell.getClassLoader().addURL(new File(StringUtils.trimToEmpty(classPath)).toURI().toURL());
                                } catch (Throwable ignore) {
                                }
                            }
                        }
                    }
                }
            }
            return groovyShell;
        };
    }

    private void transform() {
        Object sourceCoderValue = coderSourceBox.getSelectedItem();
        if (sourceCoderValue == null) return;
        Object targetValue = coderTargetBox.getSelectedItem();
        if (targetValue == null) return;
        if (this.leftTextField.getText().equalsIgnoreCase("")) return;
        for (Coder coder : this.dynamicCoders) {
            if (coder.kind().is(String.valueOf(sourceCoderValue), String.valueOf(targetValue))) {
                //转换
                rightTextField.setText(coder.transform(this.leftTextField.getText()));
            }
        }
    }


    @Override
    public void documentChanged(com.intellij.openapi.editor.event.@NotNull DocumentEvent event) {
        transform();
    }

    private class RightTarget extends JPanel {
        private final Project project;

        private final AtomicBoolean state = new AtomicBoolean(false);
        private SimpleFrame coder;

        public RightTarget(Project project, Supplier<GroovyShell> groovyShell) {
            super(new BorderLayout());
            this.project = project;
            DefaultActionGroup group = new DefaultActionGroup();
            AnAction clearAction = new AnAction(() -> I18n.getString("coder.editor.clear", project), Icons.CLEAR) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                    if (StringUtils.isBlank(leftTextField.getText()) && StringUtils.isBlank(rightTextField.getText())) {
                        return;
                    }
                    contextLogger.warn("清空编辑器中的内容中...");
                    contextLogger.warn("left: \n" + leftTextField.getText());
                    contextLogger.warn("right: \n" + rightTextField.getText());
                    leftTextField.setText("");
                    rightTextField.setText("");
                    contextLogger.warn("清空编辑器中的内容完毕");
                }
            };
            AnAction addAction = new AnAction(() -> I18n.getString("coder.custom.title", project), com.intellij.util.Icons.ADD_ICON) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                    try {
                        customCoderMouseClicked(groovyShell);
                    } catch (Throwable err) {
                        Messages.showErrorDialog(err.getMessage(), I18n.getString("coder.custom.title", project));
                    }
                }
            };
            group.add(clearAction);
            group.add(addAction);
            if (!isEditor) {
                group.add(new AnAction(() -> "在编辑器中打开", Icons.OPEN) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                        PsiFile psiFile = ProjectUtils.getOrCreate(project, GlobalConstant.CODER_EDITOR_PANEL_KEY, () -> {
                            PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(PlainTextLanguage.INSTANCE, "");
                            file.setName("Coder");
                            return file;
                        });
                        FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl) FileEditorManager.getInstance(project);
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
                            for (FileEditor fileEditor : fileEditors) {
                                EditorComposite composite = fileEditorManager.getComposite(fileEditor);
                                if (composite != null) {
                                    JComponent component = composite.getComponent();
                                    component.removeAll();
                                    component.add(new CoderView(project, true, false));
                                }
                            }
                        }
                    }
                });
            }

            if (!isConsole) {
                group.add(new AnAction(() -> "在控制台打开", Icons.CONSOLE) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                        ToolWindow toolWindow = toolWindowManager.getToolWindow(GlobalConstant.CODER_LOG_CONSOLE);
                        ContentManager contentManager = toolWindow.getContentManager();
                        Content coderContent = contentManager.findContent("Coder");
                        if (coderContent == null) {
                            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
                            contentManager.addContent(contentFactory.createContent(new CoderView(project, false, true), "Coder", true));
                        }
                        toolWindow.show();
                    }
                });

            }

            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Coder@Toolbar", group, true);
            JPanel panel = ComponentUtils.createFlowLayoutPanel(FlowLayout.LEFT, coderTargetBox, actionToolbar.getComponent());
            //添加下拉框,左对齐
            add(panel, BorderLayout.NORTH);
            //内容框
            add(new JScrollPane(rightTextField), BorderLayout.CENTER);
        }

        /**
         * 自定义Coder点击事件
         */
        private void customCoderMouseClicked(Supplier<GroovyShell> groovyShell) {

            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(codeTextField.getDocument());
            //当编辑器中存在了,就屏蔽掉
            if (psiFile != null) {
                VirtualFile virtualFile = psiFile.getVirtualFile();
                for (FileEditor allEditor : fileEditorManager.getAllEditors()) {
                    VirtualFile file = allEditor.getFile();
                    //如果和之前不匹配,就关闭掉
                    if (virtualFile.hashCode() != file.hashCode() && StringUtils.equals(virtualFile.getPath(), file.getPath())) {
                        fileEditorManager.closeFile(file);
                    } else if (virtualFile.hashCode() == file.hashCode() && StringUtils.equals(virtualFile.getPath(), file.getPath())) {
                        fileEditorManager.openFile(file, true);
                        return;
                    }
                }
            }

            if (state.compareAndSet(false, true)) {
                List<Runnable> disposes = new ArrayList<>();
                this.coder = new SimpleFrame(createCustomCoderPanel(groovyShell, disposes), I18n.getString("coder.custom.title", project), new Dimension(1000, 600));
                coder.setVisible(true);
                coder.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        disposes.forEach(Runnable::run);
                        state.set(false);
                    }

                    @Override
                    public void windowClosed(WindowEvent e) {
                        disposes.forEach(Runnable::run);
                        state.set(false);
                    }
                });
            } else {
                this.coder.toFront();
            }
        }

        private JComponent createCustomCoderPanel(Supplier<GroovyShell> groovyShell, List<Runnable> disposeRegistry) {
            //设置actionGroup
            DefaultActionGroup defaultActionGroup = new DefaultActionGroup();
            defaultActionGroup.add(new EnvAction(project, disposeRegistry));
            defaultActionGroup.add(new DemoAction(codeTextField, project));
            defaultActionGroup.add(new CompileAction(codeTextField, groovyShell, project, contextLogger));
            defaultActionGroup.add(new InstallAction(codeTextField, groovyShell, sourceComboBoxModel, baseCoders, dynamicCoders, project, contextLogger));
            defaultActionGroup.add(new UsingProjectLibraryAction(project));
            defaultActionGroup.add(new RunAction(codeTextField, groovyShell, project, contextLogger));
            defaultActionGroup.add(new EditClassPathAction(project));
            defaultActionGroup.add(new RefreshClassPathAction(project));
            defaultActionGroup.add(new ChangeScopeAction(codeTextField, project));
            defaultActionGroup.add(new AnAction(() -> "在编辑器中打开", Icons.OPEN) {

                @Override
                public void actionPerformed(@NotNull AnActionEvent event) {
                    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(codeTextField.getDocument());
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
                            DefaultActionGroup editorGroup = new DefaultActionGroup();
                            editorGroup.add(new EnvAction(project, disposeRegistry));
                            editorGroup.add(new DemoAction(codeTextField, project));
                            editorGroup.add(new CompileAction(codeTextField, groovyShell, project, contextLogger));
                            editorGroup.add(new InstallAction(codeTextField, groovyShell, sourceComboBoxModel, baseCoders, dynamicCoders, project, contextLogger));
                            editorGroup.add(new UsingProjectLibraryAction(project));
                            editorGroup.add(new RunAction(codeTextField, groovyShell, project, contextLogger));
                            editorGroup.add(new EditClassPathAction(project));
                            editorGroup.add(new RefreshClassPathAction(project));
                            editorGroup.add(new ChangeScopeAction(codeTextField, project));
                            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("CustomCoderEditor@ToolBar", editorGroup, true);
                            for (FileEditor fileEditor : fileEditors) {
                                fileEditorManager.addTopComponent(fileEditor, ComponentUtils.createFlowLayoutPanel(FlowLayout.RIGHT, actionToolbar.getComponent()));
                            }
                        }

                        coder.dispose();
                    }
                }
            });
            SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, false);
            panel.registerKeyboardAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        try {
                            PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
                            psiDocumentManager.commitAllDocuments();
                            PsiFile psiFile = psiDocumentManager.getPsiFile(codeTextField.getDocument());
                            if (psiFile != null) {
                                ReformatCodeProcessor reformatCodeProcessor = new ReformatCodeProcessor(psiFile, false);
                                RearrangeCodeProcessor rearrangeCodeProcessor = new RearrangeCodeProcessor(reformatCodeProcessor);
                                CodeCleanupCodeProcessor codeCleanupCodeProcessor = new CodeCleanupCodeProcessor(rearrangeCodeProcessor);
                                codeCleanupCodeProcessor.setProcessAllFilesAsSingleUndoStep(false);
                                codeCleanupCodeProcessor.run();
                            }
                        } catch (Throwable err) {
                            contextLogger.error("格式化失败: " + err.getMessage());
                        }
                    });

                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

            panel.registerKeyboardAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        try {
                            PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
                            psiDocumentManager.commitAllDocuments();
                            PsiFile psiFile = psiDocumentManager.getPsiFile(codeTextField.getDocument());
                            if (psiFile != null) {
                                OptimizeImportsProcessor optimizeImportsProcessor = new OptimizeImportsProcessor(project, psiFile);
                                RearrangeCodeProcessor rearrangeCodeProcessor = new RearrangeCodeProcessor(optimizeImportsProcessor);
                                CodeCleanupCodeProcessor codeCleanupCodeProcessor = new CodeCleanupCodeProcessor(rearrangeCodeProcessor);
                                codeCleanupCodeProcessor.setProcessAllFilesAsSingleUndoStep(false);
                                codeCleanupCodeProcessor.run();
                            }
                        } catch (Throwable err) {
                            contextLogger.error("优化依赖失败: " + err.getMessage());
                        }
                    });

                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("custom.coder", defaultActionGroup, false);
            panel.setToolbar(actionToolbar.getComponent());
            //设置内容
            panel.setContent(codeTextField);
            return panel;
        }

    }

    private class LeftSource extends JPanel {

        public LeftSource() {
            super(new BorderLayout());
            //下拉框
            add(ComponentUtils.createFlowLayoutPanel(coderSourceBox, FlowLayout.LEFT), BorderLayout.NORTH);
            //内容框
            add(leftTextField, BorderLayout.CENTER);
        }

    }
}
