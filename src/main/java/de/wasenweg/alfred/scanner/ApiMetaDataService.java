package de.wasenweg.alfred.scanner;

import com.fasterxml.jackson.databind.JsonNode;
import de.wasenweg.alfred.comics.Comic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Service
public class ApiMetaDataService {

  @Autowired
  private ComicVineService comicVineService;

  private List<ScannerIssue> scannerIssues = new ArrayList<ScannerIssue>();

  /**
   * Scrapes and saves information for the given comic.
   *
   * API lookup information is extracted from the file path and/or existing meta
   * data stored in the embedded XML file.
   *
   * @param comic The comic book entity.
   * @return
   */
  public List<ScannerIssue> set(final Comic comic) {
    this.scannerIssues.clear();

    if (!comic.isValid()) {
      // Attempt to extract meta data from file path
      try {
        comic.setPathParts();
      } catch (final InvalidIssueNumberException exception) {
        log.warn(exception.getMessage(), exception);
        this.scannerIssues.add(ScannerIssue.builder()
            .message(exception.getMessage())
            .severity(ScannerIssue.Severity.WARNING)
            .build());
        comic.setPosition(new DecimalFormat("0000.0").format(new BigDecimal(0)));
      }
    }

    // If neither the XML nor the file path contain enough hints about which
    // comic book this is, we inform the user.
    final List<String> missingAttributes = comic.findMissingAttributes();
    if (missingAttributes.size() > 0) {
      this.scannerIssues.add(ScannerIssue.builder()
          .message("Missing meta data: " + String.join(", ", missingAttributes))
          .severity(ScannerIssue.Severity.ERROR)
          .build());
    }

    // Here we can assume to have enough meta data about the comic to make
    // a query to the Comic Vine API.
    try {
      this.query(comic);
    } catch (final Exception exception) {
      log.error("Error while fetching information for " + comic.getPath(), exception);
      this.scannerIssues.add(ScannerIssue.builder()
          .message("Error during Comic Vine API meta data retrieval")
          .severity(ScannerIssue.Severity.ERROR)
          .build());
    }

    return this.scannerIssues;
  }

  private List<JsonNode> filterVolumeSearchResults(
      final String publisher, final String series, final String volume, final JsonNode results) {
    final Stream<JsonNode> volumes = IntStream.range(0, results.size()).mapToObj(results::get);
    return volumes.filter(v -> {
      return publisher.equals(v.get("publisher").get("name").asText())
          && series.equals(v.get("name").asText())
          && volume.equals(v.get("start_year").asText());
    }).collect(Collectors.toList());
  }

  private String findIssueDetailsUrl(final Comic comic, final List<JsonNode> issues) {
    final List<JsonNode> filteredIssues = issues.stream()
        .filter(issue -> {
          return issue.get("issue_number").asText().equals(comic.getNumber());
        })
        .collect(Collectors.toList());

    if (filteredIssues.size() == 0) {
      this.scannerIssues.add(ScannerIssue.builder()
          .message("No matching issue found")
          .severity(ScannerIssue.Severity.ERROR)
          .build());
    }
    if (filteredIssues.size() > 1) {
      this.scannerIssues.add(ScannerIssue.builder()
          .message("No unique issue found")
          .severity(ScannerIssue.Severity.ERROR)
          .build());
    }

    return filteredIssues.get(0).get("api_detail_url").asText();
  }

  @Cacheable("volumeIds")
  public String findVolumeId(final String publisher, final String series, final String volume) {
    int page = 0;
    JsonNode response = this.comicVineService.findVolumesBySeries(series, page);
    List<JsonNode> results = this.filterVolumeSearchResults(publisher, series, volume, response.get("results"));

    final int totalCount = response.get("number_of_total_results").asInt();
    final int limit = response.get("limit").asInt();
    final int lastPage = totalCount / limit;
    while (results.size() == 0 && page < lastPage) {
      page++;
      response = this.comicVineService.findVolumesBySeries(series, page);
      results = this.filterVolumeSearchResults(publisher, series, volume, response.get("results"));
    }

    if (results.size() > 0) {
      return results.get(0).get("id").asText();
    } else {
      this.scannerIssues.add(ScannerIssue.builder()
          .message("No result in volume search")
          .severity(ScannerIssue.Severity.ERROR)
          .build());
      return "";
    }
  }

  @Cacheable("volumeIssues")
  public List<JsonNode> findVolumeIssues(final String volumeId) {
    int page = 0;
    final JsonNode response = this.comicVineService.findIssuesInVolume(volumeId, page);
    JsonNode results = response.get("results");
    final List<JsonNode> issues = IntStream.range(0, results.size()).mapToObj(results::get)
        .collect(Collectors.toList());

    final int totalCount = response.get("number_of_total_results").asInt();
    final int limit = response.get("limit").asInt();
    final int lastPage = totalCount / limit;
    while (page < lastPage) {
      page++;
      results = this.comicVineService.findIssuesInVolume(volumeId, page).get("results");
      issues.addAll(IntStream.range(0, results.size()).mapToObj(results::get)
          .collect(Collectors.toList()));
    }

    if (issues.isEmpty()) {
      this.scannerIssues.add(ScannerIssue.builder()
          .message("Empty volume")
          .severity(ScannerIssue.Severity.ERROR)
          .build());
    }
    return issues;
  }

  private String getEntities(final JsonNode entities) {
    return IntStream.range(0, entities.size()).mapToObj(entities::get)
        .map(character -> character.get("name").asText())
        .collect(Collectors.joining(", "));
  }

  private String getCharacters(final JsonNode details) {
    if (details.has("character_credits")) {
      return this.getEntities(details.get("character_credits"));
    }
    return "";
  }

  private String getTeams(final JsonNode details) {
    if (details.has("team_credits")) {
      return this.getEntities(details.get("team_credits"));
    }
    return "";
  }

  private String getLocations(final JsonNode details) {
    if (details.has("location_credits")) {
      return this.getEntities(details.get("location_credits"));
    }
    return "";
  }

  /**
   * Gathers a comma separated list of persons per role.
   * @param details The array of persons
   * @return
   */
  private Map<String, String> getPersons(final JsonNode details) {
    final JsonNode persons = details.get("person_credits");
    return IntStream.range(0, persons.size())
        .mapToObj(persons::get)
        .collect(Collectors.groupingBy(
            person -> person.get("role").asText(),
            Collectors.mapping(
                person -> person.get("name").asText(),
                Collectors.joining(", "))));
  }

  private String getNodeText(final JsonNode response, final String key) {
    if (response.has(key)) {
      return response.get(key).asText();
    }
    return "";
  }

  public void applyIssueDetails(final String url, final Comic comic) {
    final JsonNode response = this.comicVineService.getIssueDetails(url).get("results");
    comic.setTitle(this.getNodeText(response, "name"));
    comic.setSummary(this.getNodeText(response, "description"));
    final String[] coverDate = response.get("cover_date").asText().split("-");
    comic.setYear(Short.valueOf(coverDate[0]));
    comic.setMonth(Short.valueOf(coverDate[1]));
    comic.setCharacters(this.getCharacters(response));
    comic.setTeams(this.getTeams(response));
    comic.setLocations(this.getLocations(response));
    final Map<String, String> persons = this.getPersons(response);
    comic.setWriter(persons.get("writer"));
    comic.setPenciller(persons.get("penciller"));
    comic.setInker(persons.get("inker"));
    comic.setColorist(persons.get("colorist"));
    comic.setLetterer(persons.get("letterer"));
    comic.setCoverArtist(persons.get("artist, cover"));
    comic.setEditor(persons.get("editor"));
    comic.setWeb(response.get("site_detail_url").asText());
  }

  private void query(final Comic comic) {
    final String volumeId = this.findVolumeId(comic.getPublisher(), comic.getSeries(), comic.getVolume());
    if ("".equals(volumeId)) {
      return;
    }
    final List<JsonNode> issues = this.findVolumeIssues(volumeId);
    if (issues.isEmpty()) {
      return;
    }
    final String issueDetailsUrl = this.findIssueDetailsUrl(comic, issues);
    this.applyIssueDetails(issueDetailsUrl, comic);
  }
}
