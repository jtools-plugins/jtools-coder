package dev.coolrequest.tool.utils;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.jps.model.java.JavaResourceRootType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ProjectUtils {

    public static <T> T getOrCreate(Project project, Key<T> key, Supplier<T> supplier) {
        T userData = project.getUserData(key);
        if (userData == null) {
            userData = supplier.get();
            project.putUserData(key, userData);
        }
        return userData;
    }

    public static List<File> getAllModuleLibraries(Project project) {
        Map<String, File> map = new HashMap<>();
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        for (Module module : moduleManager.getModules()) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            for (VirtualFile resource : moduleRootManager.getSourceRoots(JavaResourceRootType.RESOURCE)) {
                map.put(resource.getPresentableUrl(), new File(resource.getPresentableUrl()));
            }
            String outputPath = CompilerPaths.getModuleOutputPath(module, false);
            if(StringUtils.isNotBlank(outputPath)){
                map.put(outputPath,new File(outputPath));
            }
            OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
            for (OrderEntry orderEntry : orderEntries) {
                if (orderEntry instanceof LibraryOrderEntry) {
                    LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) orderEntry;
                    if (libraryOrderEntry.getScope() == DependencyScope.COMPILE || DependencyScope.RUNTIME == libraryOrderEntry.getScope()) {
                        Library library = libraryOrderEntry.getLibrary();
                        if (library != null) {
                            for (VirtualFile file : library.getFiles(OrderRootType.CLASSES)) {
                                map.put(file.getName(), new File(file.getPresentableUrl()));
                            }
                        }
                    }
                }
            }
        }

        return new ArrayList<>(map.values());
    }
}
