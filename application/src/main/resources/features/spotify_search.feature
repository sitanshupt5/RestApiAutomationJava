# Created by Sitanshu at 28-12-2024
Feature: Spotify Search
  # The feature explores and tests the search operation in Spotify app with different search parameters like
  # album, artist, track, year, upc.


  @Smoke
  Scenario Outline:  Validate search results for search parameter type artist for sceanrio "<Scenario>"
    Given I have authorization token
    And I have API "<API>"
    And I set request body for "<RequestDataset>"
    When I call method "POST"
    Then I verify response code is "<ExpectedStatusCode>"
    Then I verify attribute values match "<ValidationData>" in Response
    Examples:
      | Scenario       | API            | RequestDataset                   | ValidationData                   |
      | Arijit Singh   | spotify_search | verifyArtistDetailsArijitSingh   | verifyArtistDetailsArijitSingh   |
      | Sonu Nigam     | spotify_search | verifyArtistDetailsSonuNigam     | verifyArtistDetailsSonuNigam     |
      | Shreya Ghoshal | spotify_search | verifyArtistDetailsShreyaGhoshal | verifyArtistDetailsShreyaGhoshal |
      | Kishore Kumar  | spotify_search | verifyArtistDetailsKishoreKumar  | verifyArtistDetailsKishoreKumar  |

  @Smoke
  Scenario Outline:  Validate search results for search parameter type album for scenario "<Scenario>"
    Given I have authorization token
    And I have API "<API>"
    And I set request body for "<RequestDataset>"
    When I call method "POST"
    Then I verify response code is "<ExpectedStatusCode>"
    Then I verify attribute values match "<ValidationData>" in Response
    Examples:
      | Scenario      | API            | RequestDataset                | ValidationData                |
      | Aashiqui 2    | spotify_search | verifyAlbumDetailsAashiqui2   | verifyAlbumDetailsAashiqui2   |
      | Veer Zaara    | spotify_search | verifyAlbumDetailsVeerZaara   | verifyAlbumDetailsVeerZaara   |
      | Chak De India | spotify_search | verifyAlbumDetailsChakDeIndia | verifyAlbumDetailsChakDeIndia |
      | RHTDM         | spotify_search | verifyAlbumDetailsRHTDM       | verifyAlbumDetailsRHTDM       |