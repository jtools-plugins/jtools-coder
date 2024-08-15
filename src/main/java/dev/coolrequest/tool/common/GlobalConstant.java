package dev.coolrequest.tool.common;

import com.intellij.openapi.util.Key;
import com.intellij.ui.components.JBTextArea;
import dev.coolrequest.tool.components.MultiLanguageTextField;

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
}
