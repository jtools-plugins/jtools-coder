package dev.coolrequest.tool.common;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.messages.MessageBusConnection;
import dev.coolrequest.tool.components.MultiLanguageTextField;
import dev.coolrequest.tool.views.coder.Coder;

import javax.swing.*;
import java.util.List;
import java.util.Set;

public interface GlobalConstant {

    String[] ENV_SUPPORT_TYPES = new String[]{"properties", "xml", "yaml", "json5"};

    String CODER_LOG_CONSOLE = "CoderConsole";

    /**
     * 缓存日志上下文到项目
     */
    Key<LogContext> CODER_LOG_CONSOLE_KEY = Key.create("CoderLogConsole");

    /**
     * 缓存groovy 代码组件
     */
    Key<MultiLanguageTextField> CODER_GROOVY_CODE_KEY = Key.create("CoderGroovyCode");

    /**
     * 缓存groovy classpath编辑组件到上下文
     */
    Key<JBTextArea> CODER_GROOVY_CLASSPATH_KEY = Key.create("CoderGroovyClasspath");


    /**
     * 缓存自定义Coder代码
     */
    Key<MultiLanguageTextField> CODER_CUSTOM_CODE_KEY = Key.create("CoderCustomCode");

    /**
     * 缓存自定义Coder环境参数
     */
    Key<MultiLanguageTextField> CODER_CUSTOM_ENV_KEY = Key.create("CoderCustomEnv");

    /**
     * 缓存在编辑器中打开coder的面板
     */
    Key<PsiFile> CODER_EDITOR_PANEL_KEY = Key.create("CoderEditorPanel");

    /**
     * 缓存coders
     */
    Key<List<Coder>> CODER_KEY = Key.create("CoderKey");


    /**
     * 缓存source comboBox
     */
    Key<DefaultComboBoxModel<String>> CODER_SOURCE_BOX_MODEL_KEY = Key.create("CoderSourceBoxModel");

    /**
     * 缓存target comboBox
     */
    Key<DefaultComboBoxModel<String>> CODER_TARGET_BOX_MODEL_KEY = Key.create("CoderTargetBoxModel");

    /**
     * 缓存项目状态,存在则已经处理
     */
    Key<Set<String>> CODER_STATE_CACHE = Key.create("CoderStateCache");


    /**
     * 初始化CoderGroovyFileEditorListener监听器的key
     */
    String CODER_GROOVY_FILE_EDITOR_LISTEN_INIT_KEY = "CoderGroovyFileEditorListenInitKey";


    /**
     * 初始化CoderCustomFileEditorListenInitKey监听器的key
     */
    String CODER_CUSTOM_FILE_EDITOR_LISTEN_INIT_KEY = "CoderCustomFileEditorListenInitKey";


    /**
     * 全局消息总线
     */
    Key<MessageBusConnection> MESSAGE_BUS_CONNECTION_KEY = Key.create("MessageBusConnection");

    /**
     * Groovy 在编辑器中打开的top组件
     */
    Key<JComponent> CODER_GROOVY_FILE_EDITOR_TOP_COMPONENT = Key.create("CoderGroovyFileEditorTopComponent");

    /**
     * 自定义Coder 在编辑器中打开的top组件
     */
    Key<JComponent> CODER_CUSTOM_FILE_EDITOR_TOP_COMPONENT = Key.create("CoderCustomFileEditorTopComponent");

}
