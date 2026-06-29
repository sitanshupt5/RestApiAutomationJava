# Created by Sitanshu at 28-12-2024
Feature: Spotify Search
  # The feature explores and tests the search operation in Spotify app with different search parameters like
  # album, artist, track, year, upc.


  @Smoke
  Scenario Outline:  Validate search results for search parameter type artist for sceanrio "<Scenario>"
    Given I have authorization token
    And I have API "<API>"
    And I set request body for "<RequestDataset>"
    When I call method "GET"
    Then I verify response code is "<ExpectedStatusCode>"
    Examples:
      | Scenario       |  | API            | RequestDataset                   | ExpectedStatusCode |
      | Arijit Singh   |  | spotify_search | verifyArtistDetailsArijitSingh   | 403                |
      | Sonu Nigam     |  | spotify_search | verifyArtistDetailsSonuNigam     | 403                |
      | Shreya Ghoshal |  | spotify_search | verifyArtistDetailsShreyaGhoshal | 403                |
      | Kishore Kumar  |  | spotify_search | verifyArtistDetailsKishoreKumar  | 403                |

  @Smoke
  Scenario Outline:  Validate search results for search parameter type album for scenario "<Scenario>"
    Given I have authorization token
    And I have API "<API>"
    And I set request body for "<RequestDataset>"
    When I call method "GET"
    Then I verify response code is "<ExpectedStatusCode>"
    Examples:
      | Scenario      | API            | RequestDataset                | ExpectedStatusCode |
      | Aashiqui 2    | spotify_search | verifyAlbumDetailsAashiqui2   | 403                |
      | Veer Zaara    | spotify_search | verifyAlbumDetailsVeerZaara   | 403                |
      | Chak De India | spotify_search | verifyAlbumDetailsChakDeIndia | 403                |
      | RHTDM         | spotify_search | verifyAlbumDetailsRHTDM       | 403                |