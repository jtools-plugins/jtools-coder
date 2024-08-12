package dev.coolrequest.tool.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LibraryUtils {

    public static void refreshLibrary(Project project, String prefix, String classpathText) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            installLibrary(project, prefix, "");
            if (StringUtils.isBlank(classpathText)) {
                return;
            } else {
                installLibrary(project, prefix, classpathText);
            }
        });
    }


    private static void installLibrary(Project project, String prefix, String classpathText) {
        try {
            //本次添加的依赖
            HashSet<File> uris = new HashSet<>();
            HashSet<String> addNames = new HashSet<>();
            if (StringUtils.isNotBlank(classpathText)) {
                String[] classPathArray = classpathText.split("\n");
                for (String classPathItems : classPathArray) {
                    for (String classPath : classPathItems.split(",")) {
                        if (StringUtils.isNotBlank(StringUtils.trimToEmpty(classPath))) {
                            try {
                                File file = new File(StringUtils.trimToEmpty(classPath));
                                if (file.exists()) {
                                    uris.add(file);
                                    if (file.isDirectory()) {
                                        addNames.add(prefix + DigestUtils.md5Hex(file.getAbsolutePath()));
                                    } else {
                                        addNames.add(prefix + file.getName());
                                    }
                                }
                            } catch (Throwable ignore) {

                            }
                        }
                    }
                }
            }
            LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
            HashSet<String> removeLibraries = new HashSet<>();
            for (Library library : libraryTable.getLibraries()) {
                if (StringUtils.startsWith(library.getName(), prefix) && !addNames.contains(library.getName())) {
                    removeLibraries.add(library.getName());
                    libraryTable.removeLibrary(library);
                }
            }
            LibraryTable.ModifiableModel modifiableModel = libraryTable.getModifiableModel();
            List<Library.ModifiableModel> modifiableModels = new ArrayList<>();
            ModuleManager moduleManager = ModuleManager.getInstance(project);
            Module[] modules = moduleManager.getModules();
            List<Library> addLibraries = new ArrayList<>();
            for (File file : uris) {
                Library existLibrary = modifiableModel.getLibraryByName(prefix + file.getName());
                if (existLibrary != null) {
                    continue;
                }

                if (file.isFile()) {
                    String pathUrl = VirtualFileManager.constructUrl("jar", file.getAbsolutePath() + "!/");
                    VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(pathUrl);
                    Library library = modifiableModel.createLibrary(prefix + file.getName());
                    Library.ModifiableModel libraryModifiableModel = library.getModifiableModel();
                    libraryModifiableModel.addRoot(virtualFile, OrderRootType.CLASSES);
                    modifiableModels.add(libraryModifiableModel);
                    addLibraries.add(library);
                } else {
                    String pathUrl = VirtualFileManager.constructUrl("file", file.getAbsolutePath());
                    VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(pathUrl);
                    Library library = modifiableModel.createLibrary(prefix + DigestUtils.md5Hex(file.getAbsolutePath()));
                    Library.ModifiableModel libraryModifiableModel = library.getModifiableModel();
                    libraryModifiableModel.addRoot(virtualFile, OrderRootType.CLASSES);
                    modifiableModels.add(libraryModifiableModel);
                    addLibraries.add(library);
                }

            }
            modifiableModels.forEach(Library.ModifiableModel::commit);
            modifiableModel.commit();
            for (Module module : modules) {

                ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                ModifiableRootModel modifiableRootModel = moduleRootManager.getModifiableModel();
                for (OrderEntry orderEntry : modifiableRootModel.getOrderEntries()) {
                    if (orderEntry instanceof LibraryOrderEntry) {
                        LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) orderEntry;
                        String libraryName = libraryOrderEntry.getLibraryName();
                        if (removeLibraries.contains(libraryName)) {
                            modifiableRootModel.removeOrderEntry(orderEntry);
                        }
                    }
                }
                for (Library library : addLibraries) {
                    OrderEntry[] orderEntries = modifiableRootModel.getOrderEntries();
                    boolean existLibrary = false;
                    for (OrderEntry orderEntry : orderEntries) {
                        if (orderEntry instanceof LibraryOrderEntry) {
                            LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) orderEntry;
                            String libraryName = libraryOrderEntry.getLibraryName();
                            if (StringUtils.equals(libraryName, library.getName())) {
                                existLibrary = true;
                            }
                        }
                    }
                    if (!existLibrary) {
                        ModuleRootModificationUtil.addDependency(module, library, DependencyScope.TEST, false);
                    }
                }
                modifiableRootModel.commit();
            }
        } catch (Throwable e) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                sb.append(stackTraceElement.toString() + "\n");
            }
            sb.append(e.getMessage());
            Messages.showErrorDialog(sb.toString(), "错误提示");
        }


    }
}
