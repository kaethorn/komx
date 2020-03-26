package de.wasenweg.alfred.unit;

import de.wasenweg.alfred.settings.Setting;
import de.wasenweg.alfred.settings.SettingRepository;
import de.wasenweg.alfred.settings.SettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SettingsServiceTest {

  @Mock
  private transient Environment environment;

  @Mock
  private transient SettingRepository settingRepository;

  @InjectMocks
  private transient SettingsService settingsService;

  @Captor
  private transient ArgumentCaptor<Setting> settingCaptor;

  @Test
  public void setupWithoutExistingSettingsAndWithoutEnvironmentValue() throws Exception {
    when(this.settingRepository.findByKey("comics.path")).thenReturn(Optional.ofNullable(null));
    when(this.environment.getProperty("comics.path")).thenReturn(null);

    this.settingsService.setup();

    verify(this.settingRepository, times(4)).save(this.settingCaptor.capture());
    assertThat(this.settingCaptor.getAllValues().get(0).getKey()).isEqualTo("comics.path");
    assertThat(this.settingCaptor.getAllValues().get(0).getValue()).isEqualTo("/comics");
  }

  @Test
  public void setupWithExistingSettingsAndWithoutEnvironmentValue() throws Exception {
    final Setting setting = Setting.builder()
        .key("comics.path")
        .name("Path")
        .value("/existing/comics")
        .comment("")
        .build();
    when(this.settingRepository.findByKey("comics.path")).thenReturn(Optional.of(setting));

    this.settingsService.setup();

    verify(this.settingRepository, times(4)).save(this.settingCaptor.capture());
    assertThat(this.settingCaptor.getAllValues().get(0).getKey()).isEqualTo("comics.path");
    assertThat(this.settingCaptor.getAllValues().get(0).getValue()).isEqualTo("/existing/comics");
  }

  @Test
  public void setupWithoutExistingSettingsAndWithEnvironmentValue() throws Exception {
    when(this.settingRepository.findByKey("comics.path")).thenReturn(Optional.ofNullable(null));
    when(this.environment.getProperty("comics.path")).thenReturn("/environment/comics");

    this.settingsService.setup();

    verify(this.settingRepository, times(4)).save(this.settingCaptor.capture());
    assertThat(this.settingCaptor.getAllValues().get(0).getKey()).isEqualTo("comics.path");
    assertThat(this.settingCaptor.getAllValues().get(0).getValue()).isEqualTo("/environment/comics");
  }

  @Test
  public void setupWithExistingSettingsAndWithEnvironmentValue() throws Exception {
    final Setting setting = Setting.builder()
        .key("comics.path")
        .name("Path")
        .value("/existing/comics")
        .comment("")
        .build();
    when(this.settingRepository.findByKey("comics.path")).thenReturn(Optional.of(setting));
    when(this.environment.getProperty("comics.path")).thenReturn("/environment/comics");

    this.settingsService.setup();

    verify(this.settingRepository, times(4)).save(this.settingCaptor.capture());
    assertThat(this.settingCaptor.getAllValues().get(0).getKey()).isEqualTo("comics.path");
    assertThat(this.settingCaptor.getAllValues().get(0).getValue()).isEqualTo("/environment/comics");
  }
}
