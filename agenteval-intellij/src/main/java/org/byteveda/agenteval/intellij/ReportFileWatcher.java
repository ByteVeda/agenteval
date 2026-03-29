package org.byteveda.agenteval.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

/**
 * VFS listener that watches for changes to agenteval-report.json files
 * and triggers a tool window refresh.
 */
public class ReportFileWatcher implements VirtualFileListener {

    private static final String REPORT_FILENAME = "agenteval-report.json";

    private final Project project;

    public ReportFileWatcher(Project project) {
        this.project = project;
    }

    /**
     * Registers this watcher with the VFS.
     */
    public void register() {
        VirtualFileManager.getInstance().addVirtualFileListener(this);
    }

    @Override
    public void contentsChanged(@NotNull VirtualFileEvent event) {
        if (REPORT_FILENAME.equals(event.getFile().getName())) {
            refreshToolWindow();
        }
    }

    @Override
    public void fileCreated(@NotNull VirtualFileEvent event) {
        if (REPORT_FILENAME.equals(event.getFile().getName())) {
            refreshToolWindow();
        }
    }

    private void refreshToolWindow() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow("AgentEval");
        if (toolWindow != null) {
            Content content = toolWindow.getContentManager().getContent(0);
            if (content != null && content.getComponent() instanceof AgentEvalToolWindow panel) {
                panel.refresh();
            }
        }
    }
}
