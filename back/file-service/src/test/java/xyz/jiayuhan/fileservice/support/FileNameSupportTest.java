package xyz.jiayuhan.fileservice.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 文件名安全清理规则测试。 */
class FileNameSupportTest {

    @Test
    void removesClientPathAndControlCharacters() {
        assertThat(FileNameSupport.sanitize("C:\\fake\\path\\report\n.pdf"))
                .isEqualTo("report.pdf");
    }

    @Test
    void usesFallbackForBlankName() {
        assertThat(FileNameSupport.sanitize("  ")).isEqualTo("unnamed-file");
    }
}
