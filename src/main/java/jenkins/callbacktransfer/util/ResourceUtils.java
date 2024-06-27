package jenkins.callbacktransfer.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
public final class ResourceUtils {
    private ResourceUtils() {
    }

    @SneakyThrows
    public static Properties readProperty(String resourcePath) {
        try (InputStream in = readFile(resourcePath)) {
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        }
    }

    public static String readFileStr(String resourcePath) {
        String string = "";
        try (InputStream inputStream = readFile(resourcePath)) {
            try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                string = result.toString(StandardCharsets.UTF_8.name());
            }
        } catch (Exception e) {
            log.error("Read file error", e);
        }
        return string;
    }

    @SneakyThrows
    public static InputStream readFile(String resourcePath) {
        Resource resource = new ClassPathResource(resourcePath);
        return resource.getInputStream();
    }
}
