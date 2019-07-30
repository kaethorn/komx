package de.wasenweg.alfred.scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.wasenweg.alfred.settings.SettingsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ComicVineService {

  private SettingsService settingsService;

  private String baseUrl = "https://comicvine.gamespot.com/api/";
  private String apiKey;
  private ObjectMapper mapper;

  @Autowired
  public ComicVineService(final SettingsService settingsService) {
    this.settingsService = settingsService;
    this.mapper = new ObjectMapper();
    this.apiKey = this.settingsService.get("comics.comicVineApiKey");
  }

  private String encodeValue(final String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    } catch (final UnsupportedEncodingException exception) {
      exception.printStackTrace();
      return "";
    }
  }

  // Get details about a specific issue
  private String getIssueUrl(final String url) {
    final Map<String, String> requestParams = new HashMap<>();
    requestParams.put("api_key", this.apiKey);
    requestParams.put("format", "json");
    requestParams.put("field_list", "character_credits,cover_date,location_credits,team_credits,person_credits");

    return requestParams.keySet().stream()
        .map(key -> key + "=" + this.encodeValue(requestParams.get(key)))
        .collect(Collectors.joining("&", url + "?", ""));
  }

  // Search for volumes by name
  public JsonNode findVolumesBySeries(final String series, final int page) {
    // The query parameter will match entries matching any one of the terms, so searching
    // for `Foo Bar` will match `Foo` and `Bar` entries. This looks like a bug on the API
    // side. As a workaround, using the first term actually improves result numbers.
    final String query = series.split(" ")[0];

    final Map<String, String> requestParams = new HashMap<>();
    requestParams.put("api_key", this.apiKey);
    requestParams.put("format", "json");
    requestParams.put("resources", "volume");
    requestParams.put("query", query);
    requestParams.put("field_list", "id,name,publisher,start_year");
    if (page > 1) {
      requestParams.put("page", String.valueOf(page));
    }

    final String url = requestParams.keySet().stream()
        .map(key -> key + "=" + this.encodeValue(requestParams.get(key)))
        .collect(Collectors.joining("&", this.baseUrl + "search/?", ""));

    return this.query(url);
  }

  // Get details about all issues in a volume
  public JsonNode findIssuesInVolume(final String volumeId) {
    final Map<String, String> requestParams = new HashMap<>();
    requestParams.put("api_key", this.apiKey);
    requestParams.put("format", "json");
    requestParams.put("filter", "volume:" + volumeId);
    requestParams.put("field_list", "id,name,description,api_detail_url,issue_number,site_detail_url"); // TODO

    final String url = requestParams.keySet().stream()
        .map(key -> key + "=" + this.encodeValue(requestParams.get(key)))
        .collect(Collectors.joining("&", this.baseUrl + "issues/?", ""));

    return this.query(url);
  }

  private JsonNode query(final String url) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    headers.add("user-agent", "curl/7.52.1");
    final HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
    final ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.GET, entity, String.class);
    JsonNode root = null;
    try {
      root = this.mapper.readTree(response.getBody());
    } catch (final IOException exception) {
      exception.printStackTrace();
    }
    return root;
  }
}
