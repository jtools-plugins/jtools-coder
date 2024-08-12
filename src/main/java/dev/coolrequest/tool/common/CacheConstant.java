package dev.coolrequest.tool.common;

public interface CacheConstant {

    /**
     * 缓存ScriptView视图中的自定义的ClassPath内容
     */
    String SCRIPT_VIEW_CACHE_CLASSPATH = "ScriptView:ClassPath";

    /**
     * 缓存ScriptView视图中的代码内容
     */
    String SCRIPT_VIEW_CACHE_CODE = "ScriptView:Code";

    /**
     * 缓存ScriptView视图中使用用户项目依赖状态
     */
    String SCRIPT_VIEW_CACHE_USING_PROJECT_LIBRARY = "ScriptView:UsingProjectLibrary";

    /**
     * 缓存自定义coder代码内容
     */
    String CODER_VIEW_CUSTOM_CODER_SCRIPT_CODE = "CoderView:CustomCoder:Code";

    /**
     * 自定义coder环境缓存 纯文本,编辑器使用
     */
    String CODER_VIEW_CUSTOM_CODER_ENVIRONMENT_TEXT = "CoderView:CustomCoder:EnvironmentText";

    /**
     * 自定义coder环境类型
     */
    String CODER_VIEW_CUSTOM_CODER_ENVIRONMENT_TYPE = "CoderView:CustomCoder:EnvironmentType";

    /**
     * map结构的环境缓存
     */
    String CODER_VIEW_CUSTOM_CODER_ENVIRONMENT = "CoderView:CustomCoder:Environment";

    /**
     * 缓存自定义coder代码依赖的classpath
     */
    String CODER_VIEW_CUSTOM_CODER_SCRIPT_CLASSPATH = "CoderView:CustomCoder:Code:Classpath";
}
