Feature: Paypal Create and Retrieve


  @Smoke
  Scenario Outline:  Validate search results for search parameter type artist for sceanrio "<Scenario>"
    Given I have authorization token
    And I have API "<API>"
    And I set request body for "<RequestDataset>"
    When I call method "POST"
    Then I verify response code is "<ExpectedStatusCode>"
    Then I verify attribute values match "<ValidationData>" in Response
    Examples:
      | API             | RequestDataset                      | ExpectedStatusCode  | ValidationData                      |
      | create_product  | CreateProductRestApiAutomationJava  | 200                 | CreateProductRestApiAutomationJava  |
      | create_product  | CreateProductRestApiAutomationPython| 200                 | CreateProductRestApiAutomationPython|