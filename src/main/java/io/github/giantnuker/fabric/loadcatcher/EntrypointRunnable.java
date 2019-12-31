package io.github.giantnuker.fabric.loadcatcher;

import java.io.File;

public interface EntrypointRunnable {
    void run(File newRunDir, Object gameInstance);
}