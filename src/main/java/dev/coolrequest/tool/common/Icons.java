package dev.coolrequest.tool.common;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.util.function.Supplier;

public interface Icons {

    Supplier<Icon> DEMO = () -> load("icons/demo", "svg");
    Supplier<Icon> LIBRARY = () -> load("icons/library", "svg");
    Supplier<Icon> ENV = () -> load("icons/env", "svg");
    Supplier<Icon> CLEAR = () -> load("icons/clear", "svg");
    Supplier<Icon> REFRESH = () -> load("icons/refresh", "svg");
    Supplier<Icon> OPEN = () -> load("icons/open", "svg");
    Supplier<Icon> CONSOLE = () -> load("icons/console", "svg");
    Supplier<Icon> ADD = () -> load("icons/add", "svg");
    Supplier<Icon> EDIT = () -> load("icons/edit", "svg");
    Supplier<Icon> INSTALL = () -> load("icons/install", "svg");


    static Icon load(String path, String ext) {
        if (UIUtil.isUnderDarcula()) {
            return IconLoader.findIcon(path + "_light." + ext, Icons.class.getClassLoader());
        }
        return IconLoader.findIcon(path + "_dark." + ext, Icons.class.getClassLoader());
    }
}
