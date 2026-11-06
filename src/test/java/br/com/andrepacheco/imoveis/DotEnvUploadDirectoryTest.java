package br.com.andrepacheco.imoveis;

import br.com.andrepacheco.imoveis.service.UploadService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvUploadDirectoryTest {
    @TempDir Path temporaryDirectory;

    @Test void loadsUploadDirectoryFromDotEnvInWorkingDirectory() throws Exception {
        Path uploads = temporaryDirectory.resolve("pasta externa");
        Files.writeString(temporaryDirectory.resolve(".env"),
                "UPLOAD_DIR=" + uploads.toString().replace('\\', '/') + "\n");

        String classpath = Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator))
                .map(entry -> Path.of(entry).toAbsolutePath().toString())
                .collect(Collectors.joining(java.io.File.pathSeparator));
        ProcessBuilder builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classpath, Probe.class.getName());
        builder.directory(temporaryDirectory.toFile()).redirectErrorStream(true);
        builder.environment().remove("UPLOAD_DIR");
        Process process = builder.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly();
        assertThat(finished).as("A aplicação de teste deve finalizar").isTrue();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(process.exitValue()).as(output).isZero();
        assertThat(output).contains("UPLOAD_DIRECTORY=" + uploads.toAbsolutePath().normalize());
        assertThat(uploads).isDirectory();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Probe {
        @Bean UploadService uploads(@Value("${app.upload-dir}") String directory) {
            return new UploadService(directory);
        }

        public static void main(String[] args) {
            SpringApplication application = new SpringApplication(Probe.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            try (ConfigurableApplicationContext context = application.run(args)) {
                System.out.println("UPLOAD_DIRECTORY=" + context.getBean(UploadService.class).getDirectory());
            }
        }
    }
}
