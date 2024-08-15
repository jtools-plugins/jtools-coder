package dev.coolrequest.tool.components;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.impl.ContextMenuPopupHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PopupMenu extends JPopupMenu {

    public static PopupMenu create(JMenuItem... items) {
        PopupMenu popupMenu = new PopupMenu();
        for (JMenuItem item : items) {
            popupMenu.add(item);
        }
        return popupMenu;
    }

    public static void attachClearMenu(String title, Icon icon, JTextArea textArea){
        PopupMenu popupMenu = new PopupMenu();
        popupMenu.add(new JMenuItem(title,icon){
            @Override
            protected void processMouseEvent(MouseEvent e) {
                if (e.getID() == MouseEvent.MOUSE_RELEASED && SwingUtilities.isLeftMouseButton(e)) {
                    textArea.setText("");
                }
                super.processMouseEvent(e);
            }
        });
        textArea.setComponentPopupMenu(popupMenu);
//        textArea.addMouseListener(popupMenu.createRightClickMouseAdapter());
    }

    public static void attachClearMenu(String title, Icon icon, EditorEx editorEx){
        editorEx.installPopupHandler(new ContextMenuPopupHandler(){

            @Override
            public ActionGroup getActionGroup(@NotNull EditorMouseEvent editorMouseEvent) {
                return new DefaultActionGroup(new AnAction(() -> title,icon) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent event) {
                        editorEx.getDocument().setText("");
                    }
                });
            }
        });
    }

    public void rightClickShow(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            this.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public MouseAdapter createRightClickMouseAdapter() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                PopupMenu.this.rightClickShow(e);
            }
        };
    }
}
