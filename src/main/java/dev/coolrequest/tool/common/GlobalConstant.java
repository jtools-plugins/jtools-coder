package dev.coolrequest.tool.common;

import com.intellij.openapi.util.Key;
import dev.coolrequest.tool.components.MultiLanguageTextField;

public interface GlobalConstant {

    String[] ENV_SUPPORT_TYPES = new String[]{"properties", "xml", "yaml", "json5"};

    String CODER_LOG_CONSOLE = "CoderConsole";

    Key<LogContext> CODER_LOG_CONSOLE_KEY = Key.create("CoderLogConsole");

    Key<MultiLanguageTextField> CODER_GROOVY_CODE_KEY = Key.create("CoderGroovyCode");
}
