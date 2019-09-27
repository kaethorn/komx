package de.wasenweg.alfred.integration;

import de.wasenweg.alfred.settings.Setting;
import de.wasenweg.alfred.settings.SettingRepository;

import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

@Component
public class IntegrationTestHelper {

  @Autowired
  private SettingRepository settingsRepository;

  public Flux<String> triggerScan(final int port) {
    return WebClient
        .create("http://localhost:" + port + "/api")
        .get()
        .uri("/scan-progress")
        .accept(TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(new ParameterizedTypeReference<String>() { });
  }

  public void setComicsPath(final String comicsPath, final TemporaryFolder temp) {
    this.copyResources(temp, comicsPath);
    final Setting comicsPathSetting = this.settingsRepository.findByKey("comics.path").get();
    comicsPathSetting.setValue(temp.getRoot().getAbsolutePath());
    this.settingsRepository.save(comicsPathSetting);
  }

  private void copyResources(final TemporaryFolder temp, final String resourcePath) {
    final Path source = Paths.get(resourcePath);
    try {
      Files.walk(source).forEach(file -> {
        try {
          Files.copy(file, temp.getRoot().toPath().resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException exception) {
          exception.printStackTrace();
        }
      });
    } catch (final IOException exception) {
      exception.printStackTrace();
    }
  }
}
