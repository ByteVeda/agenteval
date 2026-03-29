package org.byteveda.agenteval.datasets.version;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GitResolverTest {

    @Test
    void shouldDetectNonGitDirectory(@TempDir Path tempDir) {
        var resolver = new GitResolver(tempDir);

        assertThat(resolver.isGitRepository()).isFalse();
        assertThat(resolver.resolve()).isNull();
    }

    @Test
    void shouldResolveGitMetadataInGitRepo(@TempDir Path tempDir) throws Exception {
        // Initialize a git repo with a commit
        runCmd(tempDir, "git", "init");
        runCmd(tempDir, "git", "config", "user.name", "Test User");
        runCmd(tempDir, "git", "config", "user.email", "test@example.com");
        java.nio.file.Files.writeString(tempDir.resolve("file.txt"), "content");
        runCmd(tempDir, "git", "add", ".");
        runCmd(tempDir, "git", "commit", "-m", "initial commit");

        var resolver = new GitResolver(tempDir);

        assertThat(resolver.isGitRepository()).isTrue();

        GitMetadata metadata = resolver.resolve();
        assertThat(metadata).isNotNull();
        assertThat(metadata.commitHash()).isNotBlank();
        assertThat(metadata.commitHash()).hasSize(40);
        assertThat(metadata.shortHash()).isNotBlank();
        assertThat(metadata.branch()).isNotNull();
        assertThat(metadata.timestamp()).isNotNull();
        assertThat(metadata.authorName()).isEqualTo("Test User");
        assertThat(metadata.authorEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldReturnNullTagWhenNoTagsExist(@TempDir Path tempDir) throws Exception {
        runCmd(tempDir, "git", "init");
        runCmd(tempDir, "git", "config", "user.name", "Test");
        runCmd(tempDir, "git", "config", "user.email", "test@example.com");
        java.nio.file.Files.writeString(tempDir.resolve("file.txt"), "content");
        runCmd(tempDir, "git", "add", ".");
        runCmd(tempDir, "git", "commit", "-m", "commit");

        var resolver = new GitResolver(tempDir);
        GitMetadata metadata = resolver.resolve();

        assertThat(metadata).isNotNull();
        assertThat(metadata.tag()).isNull();
    }

    private static void runCmd(Path dir, String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd)
                .directory(dir.toFile())
                .redirectErrorStream(true);
        // Clear git env vars that hooks set (GIT_AUTHOR_NAME etc.)
        // so local git config takes precedence in test repos
        pb.environment().remove("GIT_AUTHOR_NAME");
        pb.environment().remove("GIT_AUTHOR_EMAIL");
        pb.environment().remove("GIT_COMMITTER_NAME");
        pb.environment().remove("GIT_COMMITTER_EMAIL");
        pb.environment().remove("GIT_AUTHOR_DATE");
        pb.environment().remove("GIT_COMMITTER_DATE");
        int exit = pb.start().waitFor();
        if (exit != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", cmd));
        }
    }
}
