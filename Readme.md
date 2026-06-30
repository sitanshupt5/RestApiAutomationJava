# coredata-test-automation

BDD API test automation framework for the CoreData microservice suite,
built with Cucumber 6.9.1, REST Assured 4.5.1, JUnit 4.13.2,
Allure 2.17.2, and Gradle 7.4 targeting Java 11.

---

## Project Structure

```
coredata-test-automation/
├── commons/                                   # Shared framework – all step definitions, HTTP machinery, utilities
│   ├── build.gradle                          # Commons dependencies (REST Assured, Allure, AWS SDK, POI, etc.)
│   └── src/main/java/com/microservice/test/accelerator/
│       ├── constants/
│       │   ├── ConfigConstants.java          # Config key names (env keys, timeout, auth type strings)
│       │   ├── Entity.java                   # JSON field name constants and CADA entity identifiers
│       │   ├── FilePaths.java                # Path templates for request.json, testdata.yml, validation_mapping.yml
│       │   └── Headers.java                  # HTTP header name constants
│       ├── customexceptions/                 # ContentNotFoundException, CustomException, InvalidFileFormatException
│       ├── enums/
│       │   └── ApiContext.java              # Keys for TestScenarioContext
│       ├── httpoperations/
│       │   ├── HttpOperations.java          # Enum wrapping REST Assured GET/POST/PUT/DELETE/PATCH calls
│       │   ├── HttpsCertTrust.java          # TrustManager for relaxed HTTPS validation
│       │   └── WaitCondition.java           # Polling/retry helper
│       ├── httpservicemanager/
│       │   ├── ConfigManager.java           # Loads envconfig.yml
│       │   ├── HttpRequestManager.java      # Builds and holds REST Assured RequestSpecification
│       │   ├── HttpResponseManager.java     # Executes requests
│       │   ├── RestRequestManager.java      # Accumulates headers/body/query params/form params
│       │   ├── RestServiceConfigManager.java# Assembles RestAssuredConfig
│       │   ├── HttpServiceAssertion.java    # Assertion methods
│       │   └── ApiResponseMatcher.java      # Hamcrest custom matchers
│       ├── listeners/
│       │   └── CucumberListener.java        # Cleans allure-results/allure-reports before each run
│       ├── stepdefs/
│       │   ├── TestManagerContext.java      # PicoContainer DI root
│       │   ├── TestScenarioContext.java     # Scenario key/value store
│       │   ├── Hooks.java                   # @Before/@After hooks
│       │   ├── CommonGivenTestSteps.java
│       │   ├── CommonWhenTestSteps.java
│       │   └── CommonThenTestSteps.java
│       └── utils/
│           ├── ApiUtilManager.java          # Loads request.json/testdata.yml/validation_mapping.yml
│           ├── DateUtils.java
│           ├── FileUtil.java
│           ├── GsonUtils.java
│           ├── JsonUtil.java
│           ├── TimeUnit.java
│           ├── TimeUtil.java
│           ├── TokenGeneratorUtility.java   # Fetches OAuth2 token
│           ├── YamlReaderUtils.java
│           └── ZipUtility.java              # Zips Allure report
│
├── cada/                                    # CADA API test module
│   ├── build.gradle
│   └── src/main/
│       ├── java/ca.coredata.automation.cada/
│       │   └── Runner.java                 # JUnit4 @CucumberOptions runner
│       └── resources/
│           ├── allure.properties           # allure.results.directory=build/allure-results
│           ├── config/
│           │   └── envconfig.yml           # Per-env host URIs, DB credentials, Cognito IDs/secrets
│           ├── apischema/
│           │   └── <env>/<ApiName>/
│           │       ├── request.json        # Request template
│           │       ├── testdata.yml        # Named datasets
│           │       └── validation_mapping.yml
│           └── features/
│               ├── non-prod/<ApiName>/<api_name>_cada.feature
│               └── prod/<ApiName>/<api_name>_cada.feature
│
├── configuration/
│   ├── pipeline-config.env
│   └── cloudformation/
│       ├── oidc-provider.yml
│       ├── oidc-role.yml
│       └── s3-reports.yml
│
├── webApp/
│   ├── index.html                          # Report portal
│   └── js/
│       └── s3BucketListing.js
│
├── deploy-cfn.sh                           # CloudFormation deploy script
├── bitbucket-pipelines.yml                 # Three custom pipelines
├── settings.gradle
└── gradlew / gradlew.bat
```

---

## Running Tests

### Prerequisites

| Tool        | Version | Notes |
|-------------|---------|-------|
| JDK         | 11      | JAVA_HOME must point to a JDK 11 installation |
| Gradle      | 7.4     | Use included wrapper |
| Network     | -       | Must reach target API host and Cognito endpoint |
| Credentials | -       | Cognito client ID/secret must be available |

### Common invocations

```bash
# Run all CADA regression tests against dev
./gradlew :cada:cucumber

# Run against a specific environment
./gradlew :cada:cucumber -Denv.type=dev
./gradlew :cada:cucumber -Denv.type=uat
./gradlew :cada:cucumber -Denv.type=prod

# Run a specific tag subset
./gradlew :cada:cucumber -Denv.type=dev -Dtags=@CADARegression
./gradlew :cada:cucumber -Denv.type=dev -Dtags=@CMDBRegression
./gradlew :cada:cucumber -Denv.type=dev -Dtags=@PerformanceAnalysis

# Combine tag and environment
./gradlew :cada:cucumber -Denv.type=uat -Dtags="@MarsCADARegression"

# Enable per-scenario request/response logs
./gradlew :cada:cucumber -Denv.type=dev  -Dtags=@CADARegression -Dlogs.extract=true
```

### CLI arguments

| Argument          | Default                        | Description                                                                                                                          |
|-------------------|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `-Denv.type`      | `dev`                          | Target environment. Valid values: `dev`, `uat`, `prod`. Selects the matching block from `envconfig.yml`.                             |
| `-Dtags`          | `(none – all scenarios run)`   | Cucumber tag expression. See [Tagging conventions](#tagging-conventions).                                                            |
| `-Dlogs.extract`  | `false`                        | When `true`, writes per-scenario request/response logs to `apilogs/<feature>/<scenario>.log` and attaches them to the Allure report. |

### Output artefacts

After `./gradlew :cada:cucumber` completes, the Allure HTML report is generated at

`cada/build/reports/allure-report/index.html`

and the raw results are at

`cada/build/allure-results/`.

The `runZipper` task automatically zips the report after generation.

---

## Authentication

`TokenGeneratorUtility.getToken()` fetches an OAuth2 `client_credentials` Bearer token. Credential lookup follows this order:

1. **System environment variables** — checked first. If both are present the token is fetched immediately.

   This is the only mechanism available in CI pipelines; set these as secured Bitbucket repository variables.

| Variable                | Description             |
|-------------------------|-------------------------|
| `AUTH_CLIENT_ID`        | OAuth2 client ID        |
| `AUTH_CLIENT_SECRET`    | OAuth2 client secret    |

2. **`envconfig.yml` fallback** — used when env vars are absent (typical for local execution).

   The `auth_client_id` and `auth_client_secret` keys are read from the matching environment block.

3. **Restricted environment error** — if credentials are still `null` after both lookups, execution is halted with:

    ```text
    Test execution for environment '{env}' is restricted to pipeline-only.
    If you are executing on pipeline please check the authorization credentials
    are correctly mapped to repository variables.
   ```

This covers environments whose credentials are intentionally not stored in `envconfig.yml` (e.g. `prod`).
Those environments can only be executed from a pipeline with the repo variables set.

### Token endpoint URL

Configured per environment in `envconfig.yml`:

| Key                | Description                                                                           |
|--------------------|---------------------------------------------------------------------------------------|
| `auth_url`         | Full token endpoint URL, or base URL (scheme + host) if `auth_url_partial` is `true`  |
| `auth_url_partial` | When `true`, `/oauth2/token` is appended to `auth_url` to construct the full endpoint |

Example configuration:

```yaml
dev:
  auth_client_id: 'your-client-id'      # present → local execution allowed
  auth_client_secret: 'your-secret'
  auth_url: 'https://myapp-dev.auth.us-east-1.amazoncognito.com'
  auth_url_partial: 'true'

prod:
  # auth_client_id / auth_client_secret intentionally absent
  # credentials must be supplied via AUTH_CLIENT_ID / AUTH_CLIENT_SECRET pipeline variables
  auth_url: 'https://myapp.auth.us-east-1.amazoncognito.com'
  auth_url_partial: 'true'
```

For a non-Cognito application with a full fixed endpoint, omit `auth_url_partial` and set `auth_url` to the complete token URL.

---

## Execution Flow

```
./gradlew :cada:cucumber -Denv.type=dev -Dtags=@CADARegression
    |
    v
[CucumberListener.setEventPublisher]        -- listeners/CucumberListener.java
    TestRunStarted: delete build/allure-results
    |
    v
[TestManagerContext constructor]            -- stepdefs/TestManagerContext.java
    ConfigManager.initEnvProperties("dev")  -- httpservicemanager/ConfigManager.java
        -> loads config/envconfig.yml       -- SnakeYAML via YamlReaderUtils
        -> merges commonConfigs + dev blocks into static envProperties map
    new HttpRequestManager(configManager)
    new RestRequestManager(httpRequestManager)
    new HttpResponseManager(configManager, httpRequestManager, restRequestManager)
    new HttpServiceAssertion(httpResponseManager)
    new TestScenarioContext()
    new SoftAssertions()
    |
    v
[Hooks.beforeScenario]                      -- stepdefs/Hooks.java
    allureEnvironmentWriter(env, user, project)
    optionally: open apilogs/<feature>/<scenario>.log
    httpRequestManager.initNewSpecification() -- builds fresh REST Assured RequestSpecification
    |
    v
[Given] "I have authorization token"
    TokenGeneratorUtility.getToken()
        -> checks AUTH_CLIENT_ID / AUTH_CLIENT_SECRET env vars
        -> falls back to auth_credential_location in envconfig.yml (local or aws)
        -> resolves token URL from auth_url + auth_url_partial
        -> POST https://cada-dev.auth.us-east-1.amazoncognito.com/oauth2/token
        -> stores access_token in TestScenarioContext[ACCESS_TOKEN]
    |
    v
[Given] "I have API 'aacsFundName'"
    ApiUtilManager.getDefaults("aacsFundName")
        -> reads apischema/dev/aacsFundName/request.json
    ApiUtilManager.getBasePath(...)            -- extracts base_path field
    ApiUtilManager.setEntityHostURI(...)      -- sets entity_host_uri in ConfigManager if request.json overrides it
    TestScenarioContext[API_NAME] = "aacsFundName"
    TestScenarioContext[BASE_PATH] = <base_path value>
    |
    v
[And] "I set request body for 'VerifyAacsFundNameReturningLongNameForAFCode'"
    CommonGivenTestSteps.iSetRequestBodyAs(customer)
        -> iSetRequestHeader(): reads header map from request.json; injects Bearer Token
        -> iSetQueryParams(customer): reads query_params from request.json; populates RestRequestManager
        -> ApiUtilManager.getRequestBody(context, customer):
            getData() -> YamlReaderUtils reads testdata.yml -> DATASET stored in TestScenarioContext
            mapData(template, data): substitutes {placeholder} tokens in request.json template
            removeEmptyJsonElements(): strips null/empty params array entries
        -> RestRequestManager.setRequestBody(json)
        -> TestScenarioContext[REQUEST_BODY] = json
    |
    v
[When] "I call method 'POST'"
    CommonWhenTestSteps.iCallMethodPOST("POST")
        HttpResponseManager.doRequest("POST", basePath)
            -> authenticate(): checks AUTH_TYPE in envProperties
            -> resolves entity_host_uri, proxy from ConfigManager
            -> HttpRequestManager.value(): returns accumulated RequestSpecification
            -> beforeRequest(): applies body, headers, params, query params, form params to spec
            -> HttpOperations.POST.doRequest(spec, basePath)
                spec.basePath(basePath)
                spec.log().all(true)
                spec.post()      -> HTTP POST to configured base_uri + basePath
        iGetTheResponse(): stores response.asString() in TestScenarioContext[RESPONSE_BODY]
    |
    v
[Then] "I verify response code is 200"
    HttpServiceAssertion.statusCodeIs(200)
        -> assertThat(actual, is(equalTo(200)))
    |
    v
[Then] "I verify attribute values match 'VerifyAacsFundNameReturningLongNameForAFCode' in Response"
    ApiUtilManager.getSchema("VerifyAacsFundNameReturningLongNameForAFCode", "aacsFundName")
        -> reads apischema/dev/aacsFundName/validation_mapping.yml
        -> returns Map<assertKey, jsonPathInResponse>

    for each mapping entry:
        HttpServiceAssertion.bodyContainsPropertyWithExpectedValue(jsonPath, testdataValue)
            -> com.jayway.jsonpath.JsonPath.read(response, "$." + jsonPath)
            -> handles Double/Boolean/Integer types; strips whitespace
            -> assertThat(actual, is(equalTo(expected)))
    |
    v
[Hooks.afterScenario]                       -- stepdefs/Hooks.java
    optionally: read apilogs file -> scenario.attach(bytes, "text/plain", ...)
    if @PerformanceAnalysis: record timeInMs from response -> performanceMap
    softAssertions.assertAll()              -- flushes all deferred assertion failures
    |
    v
allureReport task -> allure generate cada/build/allure-results -> cada/build/reports/allure-report/
runZipper task    -> ZipUtility zips allure-report directory
```

**Concrete trace — scenario "Verify AacsFundName returning longName for AF Code":**

1. `TestManagerContext()` → `ConfigManager` reads `envconfig.yml`, loads `dev` block with  
   `host_uri=https://cada.coredata.dev.cambridgeassociates.cloud/cada`.

2. `TokenGeneratorUtility.getToken()` → `AUTH_CLIENT_ID` / `AUTH_CLIENT_SECRET` env vars absent locally → `auth_credential_location=local` →
   reads `auth_client_id` / `auth_client_secret` from `envconfig.yml` dev block → `auth_url_partial=true` → appends `/oauth2/token` →  
   `POST https://cada-dev.auth.us-east-1.amazoncognito.com/oauth2/token` → stores Bearer token.

3. `iHaveAPI("aacsFundName")` → `ApiUtilManager.getDefaults("aacsFundName")` reads  
   `apischema/dev/aacsFundName/request.json`. `base_path` is empty string. `entity_host_uri` is not set in the JSON, so the global
   `host_uri` from `envconfig.yml` is used.

4. `iSetRequestBodyAs("VerifyAacsFundNameReturningLongNameForAFCode")` → `testdata.yml` key `VerifyAacsFundNameReturningLongNameForAFCode`
   resolves `function=AacsFundName`, `cadaCode1=AF38538`.  
   `mapData` substitutes `{function}->AacsFundName`, `{cadaCode1}->AF38538` in the template; `{cadaCode2}` → `{cadaCode5}` become
   `EmptyObject` and are stripped, leaving a single-element `params` array.

5. `iCallMethodPOST("POST")` → REST Assured POSTs the JSON body with `Authorization: Bearer <token>` to  
   `https://cada.coredata.dev.cambridgeassociates.cloud/cada`.

6. `iVerifyAttributeValuesMatchResponse("VerifyAacsFundNameReturningLongNameForAFCode")` → `validation_mapping.yml` maps
   `AssertLongName -> params[0].longName`.  
   `JsonPath.read(response, "$.params[0].longName")` must equal `"Valinor Private Capital Partners, L.P."` from `testdata.yml`.

7. `Hooks.afterScenario` → `softAssertions.assertAll()` throws if any assertion failed.

---

## Contribution Guide

### Adding a new API under test

**1. Create the API schema folder** for each environment you want to cover:

```text
cada/src/main/resources/apischema/dev/<MyApiName>/
```

**2. Write `request.json`** → define the request template. Use `{placeholderName}` tokens for values that vary per test scenario. Use
`{{configKey}}` for values that come from `envconfig.yml`.

```json
{
  "base_path": "",
  "header": {
    "Content-Type": "application/json",
    "Authorization": ""
  },
  "request": {
    "function": "{function}",
    "params": [
      {
        "cadaCode": "{cadaCode1}"
      },
      {
        "cadaCode": "{cadaCode2}"
      }
    ]
  }
}
```

`base_path` is appended to `host_uri`. Leave it empty to use the bare `host_uri`. To override the host entirely, add:

```json
"host_uri": "https://other-service.example.com"
```

**3. Write `testdata.yml`** → one named block per scenario. Key names must match the `{placeholder}` names in `request.json` and the
`Assert*` keys must match what `validation_mapping.yml` references.

```yaml
VerifyMyApiHappyPath:
  function: "MyFunction"
  cadaCode1: "AF12345"
  AssertResult: "Expected Value"

VerifyMyApiErrorCase:
  function: "MyFunction"
  cadaCode1: null
  AssertErrorText: "Cada Code Incompatible With Function"
```

**4. Write `validation_mapping.yml`** → maps each `Assert*` key from `testdata.yml` to its JSONPath in the response body.

```yaml
VerifyMyApiHappyPath:
  "AssertResult": "params[0].result"

VerifyMyApiErrorCase:
  "AssertErrorText": "params[0].errorText"
```

**5. Write the feature file** at:

```text
cada/src/main/resources/features/non-prod/<MyApiName>/<my_api_name>_cada.feature
```

The `API` column value in the `Examples` table must exactly match the `<MyApiName>` folder name (case-sensitive).

```gherkin
@CADARegression @MyApiName
Scenario Outline: MyApiName validation for scenario: "<Scenario>"
Given I have authorization token
And I have API "<API>"
And I set request body for "<RequestBody>"
When I call method "POST"
Then I verify response code is 200
Then I verify attribute values match "<ValidationData>" in Response

Examples:
| Scenario                     | API        | RequestBody            | ValidationData         |
| Verify happy path result,   | myApiName  | VerifyMyApiHappyPath   | VerifyMyApiHappyPath   |
| Verify error for null input,| myApiName  | VerifyMyApiErrorCase   | VerifyMyApiErrorCase   |
```

**6. Repeat steps 1-5** for `uat` and `prod` environment folders, and add a matching feature under `features/prod/` for production
scenarios.

---

## Step Catalogue

### Given steps – setup

| Step                                                | Description                                                                                                                                                                          |
|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `I have authorization token`                        | Fetches an OAuth2 Bearer token using credentials and endpoint configured in `envconfig.yml`. Stores it in scenario context.                                                          |
| `I have API {string}`                               | Sets the active API name; loads `request.json` to extract `base_path` and optional `host_uri` override.                                                                              |
| `I set request body for {string}`                   | Loads the named data set from `testdata.yml`, substitutes `{placeholder}` tokens into `request.json`, and stores the result as the request body. Also sets headers and query params. |
| `I set content-type as <TYPE>`                      | Sets `Content-Type` header using REST Assured `ContentType` enum (e.g., `JSON`, `XML`).                                                                                              |
| `I set header {string} with a value of {string}`    | Sets an arbitrary request header key/value.                                                                                                                                          |
| `I set parameter {string} with a value of {string}` | Adds a URL path or form parameter.                                                                                                                                                   |
| `I set the new x-correlation-id header`             | Generates a UUID and sets it as `x-correlation-id`.                                                                                                                                  |
| `I set request to generate Auth Token`              | Sets auth credentials from `auth_params` in `request.json` and prepares the body for a token endpoint call.                                                                          |
| `I set authentication credentials`                  | Loads `auth_params` from `request.json` into `RestRequestManager`.                                                                                                                   |

### When steps – execution

| Step                                                   | Description                                                                                                   |
|--------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `the client performs <METHOD> request on API {string}` | Executes an HTTP request using the given method against the named API's base path.                            |
| `I call method {string}`                               | Executes an HTTP request using the stored `BASE_PATH` from scenario context. Stores the response immediately. |
| `I get the response`                                   | Copies the current REST Assured response body to `TestScenarioContext[RESPONSE_BODY]`.                        |
| `I save the initial response`                          | Copies the current response body to `TestScenarioContext[INITIAL_RESPONSE_BODY]` for later comparison.        |

### Then steps – assertions

| Step                                                          | Description                                                                                                                                                                                               |
|---------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `I verify response code is {int}`                             | Asserts the HTTP status code using Hamcrest `equalTo`.                                                                                                                                                    |
| `I verify response code is {string}`                          | Same assertion, accepting the code as a string.                                                                                                                                                           |
| `I verify fields in response`                                 | Asserts response fields against values extracted from the request body via a DataTable.                                                                                                                   |
| `I verify {string} in Response`                               | Reads `validation_mapping.yml` for the named key; asserts each mapped JSONPath value equals the corresponding request field value.                                                                        |
| `I verify attribute values match {string} in Response`        | Reads `validation_mapping.yml` and `testdata.yml` for the named key; asserts each mapped JSONPath value equals the corresponding test data value. Supports `contains` and `regex` assertion key prefixes. |
| `I verify attributes in response using {string} for {string}` | Asserts a DataTable of attribute → JSONPath pairs against stored test data values for the named dataset and API.                                                                                          |
| `I save the access token`                                     | Reads `$.access_token` from the response and stores it in `TestScenarioContext[ACCESS_TOKEN]`.                                                                                                            |
| `I clear the request body`                                    | Clears the accumulated request body in `RestRequestManager`.                                                                                                                                              |
| `I clear the request headers`                                 | Clears accumulated request headers.                                                                                                                                                                       |
| `I clear the query parameters`                                | Clears accumulated query parameters.                                                                                                                                                                      |

---

## Standardizations and Conventions

### File naming

| Artefact                 | Format                                                | Example                                        |
|--------------------------|-------------------------------------------------------|------------------------------------------------|
| Feature file             | `<api_name_snake_case>_cada.feature`                  | `aacs_fund_name_cada.feature`                  |
| API schema folder        | `<ApiNameCamelCase>`                                  | `aacsFundName`                                 |
| Test data key            | `<VerbPhrase><ApiName><CaseDescription>` (PascalCase) | `VerifyAacsFundNameReturningLongNameForAFCode` |
| Assert key in `testdata` | `Assert<FieldName>`                                   | `AssertLongName`, `AssertErrorText`            |

### Request template tokens

| Token syntax        | Resolved from                                           | Behaviour when no value                                                 |
|---------------------|---------------------------------------------------------|-------------------------------------------------------------------------|
| `{placeholderName}` | `testdata.yml` key matching `placeholderName`           | Replaced with `EmptyObject` sentinel; parent object/array entry removed |
| `{{configKey}}`     | `envconfig.yml` via `ConfigManager.getEnvProperty(key)` | Replaced with empty string                                              |
| `{RandomString}`    | Generated at runtime (10-char alpha)                    | N/A                                                                     |
| `{RandomUUID}`      | `UUID.randomUUID()` at runtime                          | N/A                                                                     |
| `{currentdate}`     | `DateUtils.getTodayDateInString()`                      | N/A                                                                     |
| `{ID}`              | `TestScenarioContext[ID]`                               | Replaced with stored ID value                                           |

### Assertion key prefixes in `validation_mapping.yml`

| Key prefix                                | Assertion type                                                       | Example key                 |
|-------------------------------------------|----------------------------------------------------------------------|-----------------------------|
| _(none)_                                  | Exact equality (`equalTo`)                                           | `AssertLongName`            |
| `contains` (case-insensitive in key name) | `containsString` → value may be comma-separated list                 | `AssertContainsDescription` |
| `regex` (case-insensitive in key name)    | `matchesPattern` → supports comma-separated multi-pattern for arrays | `AssertRegexFormat`         |

### `envconfig.yml` annotated

```yaml
# Shared settings applied to every environment
commonconfigs:
  relaxed_https: 'true'         # Accept self-signed / untrusted TLS certificates
  url_encoding_enabled: 'true'  # REST Assured URL-encodes query parameters
  max_timeout: '60'             # Maximum wait time in seconds for WaitCondition
  polling_time: '5'             # Polling interval in seconds for WaitCondition

dev:
  host_uri: 'https://...'       # Base URL for all requests — unless overridden in request.json
  follow_redirects: 'false'     # Do not follow HTTP 3xx automatically
  auth_client_id: '...'         # OAuth2 client ID — omit for pipeline-only environments
  auth_client_secret: '...'     # OAuth2 client secret — omit for pipeline-only environments
  auth_url: 'https://...'       # Token endpoint base URL (or full URL if auth_url_partial is absent/false)
  auth_url_partial: 'true'      # When true, /oauth2/token is appended to auth_url
```

The `uat` and `prod` blocks follow the same structure. For environments restricted to pipeline-only execution, omit `auth_client_id` and `auth_client_secret` — credentials must then be supplied via the `AUTH_CLIENT_ID` and `AUTH_CLIENT_SECRET` pipeline variables.

Attempting local execution against such an environment will produce an explicit error.

### Tagging conventions

| Tag                   | Scope                  | Effect                                                           |
|-----------------------|------------------------|------------------------------------------------------------------|
| `@CADARegression`     | All CADA APIs          | Full regression suite — used as the CI default                   |
| `@CMDBRegression`     | CMDB-backed endpoints  | Sub-suite of CADA APIs backed by CMDB                            |
| `@ImdbCADARegression` | IMDB-backed endpoints  | Sub-suite of CADA APIs backed by IMDB                            |
| `@MarsCADARegression` | MARS-backed endpoints  | Sub-suite of CADA APIs backed by MARS                            |
| `@<ApiName>`          | Per-API                | Runs only the scenarios for a single API (e.g., `@AacsFundName`) |
| `@UatTest`            | UAT-specific scenarios | Scenarios that only apply to UAT data                            |
| `@ErrorScenarios`     | Error path scenarios   | Error/negative test cases                                        |

---

## CI/CD Setup

Three manually triggered Bitbucket Pipelines pipelines manage the full lifecycle.

### Required Bitbucket repository variables

Set these in **Repository Settings → Repository Variables** before running any pipeline.

| Variable | When required | Description |
|----------|---------------|-------------|
| `AWS_ACCESS_KEY_ID` | Bootstrap only | IAM user access key with CloudFormation and S3 permissions |
| `AWS_SECRET_ACCESS_KEY` | Bootstrap only | IAM user secret key |
| `AWS_DEFAULT_REGION` | Bootstrap only | AWS region (e.g., `ap-south-1`) |
| `BITBUCKET_AWS_ROLE_ARN` | Deploy-Infra, Run-Tests | IAM Role ARN output by Bootstrap — set after Bootstrap completes |
| `AUTH_CLIENT_ID` | Run-Tests | OAuth2 client ID for the target environment — overrides `auth_client_id` in `envconfig.yml` |
| `AUTH_CLIENT_SECRET` | Run-Tests | OAuth2 client secret for the target environment — overrides `auth_client_secret` in `envconfig.yml` |

### Step 1 — Fill in `configuration/pipeline-config.env`

Edit the file and replace every placeholder value before running Bootstrap.

```env
WORKSPACE_SLUG=your-bitbucket-workspace    # Workspace slug from your Bitbucket URL
WORKSPACE_UUID={xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}  # From Workspace Settings > General
REPO_SLUG=coredata-test-automation         # Repository slug
AWS_REGION=ap-south-1                      # Your AWS region
S3_BUCKET_NAME=coredata-allure-reports     # Must be globally unique
STACK_PREFIX=coredata                      # Prefix for CloudFormation stack names
```

Commit and push this file.

### Step 2 — Run the Bootstrap pipeline

> **One-time action. Run this once per AWS account. It creates IAM resources and an S3 bucket.**

1. Go to **Pipelines** in Bitbucket → Click **Run pipeline**.
2. Select branch: the branch containing your updated `pipeline-config.env`.
3. Select pipeline: **Bootstrap**.
4. Click **Run**.

The pipeline deploys three CloudFormation stacks:

- `<STACK_PREFIX>-oidc-provider` → registers Bitbucket as an OIDC identity provider
- `<STACK_PREFIX>-oidc-role` → creates the IAM Role that pipelines will assume
- `<STACK_PREFIX>-s3-reports` → creates the public S3 website bucket

At the end of the Bootstrap log, the pipeline prints:

```text
===========================================================
BOOTSTRAP COMPLETE
ACTION REQUIRED — Save this as a Bitbucket repository variable:
    Name : BITBUCKET_AWS_ROLE_ARN
    Value: arn:aws:iam::123456789012:role/coredata-bitbucket-oidc-role

Your report portal URL (bookmark this):
    http://coredata-allure-reports.s3-website-ap-south-1.amazonaws.com
===========================================================
```

### Step 3 — Save `BITBUCKET_AWS_ROLE_ARN`

Copy the **Value** from the Bootstrap log output.

In Bitbucket go to **Repository Settings → Repository Variables** and add:

```text
BITBUCKET_AWS_ROLE_ARN=<value from Bootstrap output>
```

### Step 4 — Run tests

1. Go to **Pipelines** → **Run pipeline**.
2. Select pipeline: **Run-Tests**.
3. Fill in the variables:

| Variable | Default | Options |
|----------|---------|---------|
| `APP` | `cada` | Top-level S3 folder name for this run's reports |
| `ENV` | `dev` | `dev`, `uat`, `prod` |
| `TAGS` | `@CADARegression` | Any Cucumber tag expression |

4. Click **Run**.

Reports are published to:

```text
s3://<S3_BUCKET_NAME>/<APP>/<YYYY-MM-DD>/<BUILD_NUMBER>/
```

The portal URL printed at the end links directly to the Allure report.

### Updating infrastructure or the portal

Run the **Deploy-Infra** pipeline after any change to:

```text
configuration/cloudformation/
webApp/
```

It uses OIDC (no static credentials) and does not recreate the bucket.

### Successful setup looks like

- Bootstrap pipeline exits green with all three CloudFormation stacks in `CREATE_COMPLETE`.
- `BITBUCKET_AWS_ROLE_ARN` repository variable is set.
- Visiting the report portal URL shows the S3 directory listing portal.
- A Run-Tests pipeline run exits green and the portal shows a dated folder containing the Allure report.

---

## Troubleshooting

| Symptom                                                                                        | Cause                                                                                                               | Fix                                                                                                                                                                                                                              |
|------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `'IllegalStateException: Test execution for environment '...' is restricted to pipeline-only'` | Credentials are `null` → env vars absent and `auth_client_id` / `auth_client_secret` not present in `envconfig.yml` | For local execution: add `auth_client_id` / `auth_client_secret` to the relevant environment block in `envconfig.yml`. For pipeline execution: verify `AUTH_CLIENT_ID` and `AUTH_CLIENT_SECRET` are set as repository variables. |
| `Error in loading the environment yml file config/envconfig.yml`                               | The `cada` module cannot find `envconfig.yml` on the classpath                                                      | Ensure `src/main/resources/config/envconfig.yml` exists and that `./gradlew :cada:processResources` completes without error.                                                                                                     |
| `Tests pass locally but fail in CI with 401 Unauthorized`                                      | Credentials in `envconfig.yml` differ from the CI environment's actual Cognito client                               | Set `AUTH_CLIENT_ID` and `AUTH_CLIENT_SECRET` as Bitbucket repo variables. The token generator uses env vars first and only falls back to `envconfig.yml` when they are absent.                                                  |
| `'File not found' in Hooks.beforeScenario when -Dlogs.extract=true`                            | Scenario name format does not match the expected `split(":")` pattern                                               | Ensure scenario names follow the `<Prefix>: <scenario>` format used in all existing Outline descriptions.                                                                                                                        |
| `Allure trend graphs show no history`                                                          | `allure-report/history/` was not synced back to S3 on the previous run, or the S3 prefix does not match `APP`       | Verify the **Run-Tests** pipeline's `aws s3 sync allure-report/history/ ...` step completed. Confirm the `APP` variable is consistent across runs.                                                                               |
| `Bootstrap fails: ROLLBACK_COMPLETE on a stack`                                                | A previous CloudFormation deployment failed partially                                                               | `deploy-cfn.sh` detects `ROLLBACK_COMPLETE` and deletes the stack before redeploying. Re-run Bootstrap. If it fails again, check the CloudFormation Events tab for the root cause.                                               |
| `'Cannot infer argument types' on 'from components.java' in IDE`                               | IDE Groovy type inference limitation                                                                                | This is a display-only warning: the build compiles and runs correctly. The syntax `from components.java` (without parentheses) is the correct form.                                                                              |
| `softAssertions.assertAll()` throws at the end of a passing scenario with assertion failures   | `bodyContainsPropertyWithExpectedValue` defers failures via `SoftAssertions` rather than failing immediately        | This is by design. All assertions for a scenario accumulate and are reported together at teardown. Check the Allure report's assertion failure detail for the specific field mismatch.                                           |

---