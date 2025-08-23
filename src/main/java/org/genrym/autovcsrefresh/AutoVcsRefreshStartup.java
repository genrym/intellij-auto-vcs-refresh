package org.genrym.autovcsrefresh;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangesViewManager;
import com.intellij.openapi.vcs.changes.InvokeAfterUpdateMode;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;

public class AutoVcsRefreshStartup implements StartupActivity {

    private static final Logger LOG = Logger.getInstance(AutoVcsRefreshStartup.class);

    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("AutoVcsRefresh initialized for project: " + project.getName());

        VirtualFileManager.getInstance().addAsyncFileListener(
                events -> {
                    // If any file was changed externally, trigger a refresh
                    if (!events.isEmpty()) {
                        return new AsyncFileListener.ChangeApplier() {
                            @Override
                            public void afterVfsChange() {
                                var dirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
                                var changeListManager = ChangeListManager.getInstance(project);
                                var changesViewManager = ChangesViewManager.getInstance(project);

                                for (VFileEvent event : events) {
                                    var file = event.getFile();
                                    if (file != null) {
                                        if (changeListManager.isIgnoredFile(file)) {
                                            LOG.info(String.format("AutoVcsRefresh: file is ignored by VCS: %s", file.getPath()));
                                            continue;
                                        }
                                        if (event instanceof VFileCreateEvent) {
                                            LOG.info(String.format("AutoVcsRefresh: new file was created: %s", file.getPath()));
                                            // Mark the parent dir as dirty - to search new files as unversioned
                                            var parent = file.getParent();
                                            if (parent != null) {
                                                LOG.info(String.format("AutoVcsRefresh: marking directory dirty: %s", parent.getPath()));
                                                dirtyScopeManager.dirDirtyRecursively(parent);
                                                changeListManager.invokeAfterUpdate(() -> {
                                                    LOG.info("ChangeListManager update completed for file: " + file.getPath());
                                                    // Refresh Unversioned Files view
                                                    LOG.info("Scheduled refresh for Unversioned Files view");
                                                    changesViewManager.scheduleRefresh();
                                                }, InvokeAfterUpdateMode.SILENT, "AutoVcsRefresh", null);
                                            } else {
                                                LOG.info("AutoVcsRefresh: new file was created but no parent directory");
                                            }
                                        } else {
                                            LOG.info(String.format("AutoVcsRefresh: existing file was changed: %s", file.getPath()));
                                            dirtyScopeManager.fileDirty(file); // Mark the specific file as dirty
                                        }
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