package org.genrym.autovcsrefresh;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;

public class AutoVcsRefreshStartup implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        VirtualFileManager.getInstance().addAsyncFileListener(
                events -> {
                    // If any file was changed externally, trigger a refresh
                    if (!events.isEmpty()) {
                        return new AsyncFileListener.ChangeApplier() {
                            @Override
                            public void afterVfsChange() {
                                var dirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
                                for (VFileEvent event : events) {
                                    var file = event.getFile();
                                    if (file != null) {
                                        // Mark the specific file as dirty
                                        dirtyScopeManager.fileDirty(file);
                                    }
                                }
                            }
                        };
                    }
                    return null;
                },
                project
        );
    }
}