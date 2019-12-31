package io.github.giantnuker.fabric.loadcatcher;

import java.io.File;

public interface EntrypointRunnalbe {
    void run(File newRunDir, Object gameInstance);
}
