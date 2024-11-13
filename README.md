[![Merge to Master Branch](https://github.com/neurowyzr/nw-apollo-dragon-service/actions/workflows/merge-to-master.yml/badge.svg?branch=master)](https://github.com/neurowyzr/nw-apollo-dragon-service/actions/workflows/merge-to-master.yml)
# nw-apollo-dragon-service

[This diagram](https://neurowyzr.atlassian.net/wiki/spaces/TECH/pages/322568194/Dragonfly-Apollo-Service) depicts the how this service integrates with Apollo's and NW's APIs.
 
# Dependencies
- Java 11

# &#127939; Running Locally
Create an _env file_ named `local.env` in the project root path and add the following content:
```
GITHUB_TOKEN
APP_TIMEZONE
CORE_MYSQL_HOST
CORE_MYSQL_PASSWORD
CORS_ORIGIN
CX_COGNIFYX_SVC_HOST
MAGIC_LINK_HOST
MYSQL_HOST
MYSQL_PASSWORD
MY_MYSQL_HOST
MY_MYSQL_PASSWORD
RABBIT_MQ_HOST
RABBIT_MQ_PASSWORD
REPORT_S3_PUBLIC_PATH
NW_API_KEY
GCHAT_ALERT_SPACE
GCHAT_ALERT_TOKEN
API_BASE_URL
API_AUTH_USERNAME
API_AUTH_PASSWORD
S3_AWS_REGION
S3_AWS_ACCESS_KEY_ID
S3_AWS_SECRET_ACCESS_KEY
AWS_S3_BUCKET
```

When executing the service within IntelliJ, use this [plug-in](https://plugins.jetbrains.com/plugin/7861-envfile) and configure it to load `local.env`.
On the other hand, when running from the terminal, `sbt run` will automatically load that env file.


Make sure you run `docker compose -f cicd/infra-docker-compose.yml -f cicd/infra-docker-compose.pr.yml up -d` to set up docker locally before running `sbt run`


&#128695; Do not commit the env file to the repository.

# Creating User and Test Session

Request
```
POST /v1/patients/{patient-id}/episodes/{episode-id}/test

header:
{
  "x-api-key": "nw_api_key",
  "request-id": "request_id"
}

body:
{
  "uid": "P01234567",
  "ahc_number": "E2345678901",
  "location_id": "10201",
  "first_name": "John",
  "last_name": "Doe",
  "dob": "1990-12-31",
  "gender": "MALE",
  "email": "john.doe@example.com",
  "mobile": 1234567890
}
```

Note: 
- Only `email` and `mobile` are optional fields
- `uid` and `ahc_number` must be equals to `patient-id` and `episode-id` respectively
- `email` should be unique for new users
- `request-id` cannot be duplicate
- Duplicate `episode-id` will return the magic link generated previously
- `location_id` must be valid


### For Apollo's endpoint:  (waiting for Apollo to finalise specs)

Send status and magic link
```
POST /v1/patients/{patient-id}/episodes/{episode-id}/status
```

Upload report
```
POST /v1/patients/{patient-id}/episodes/{episode-id}/report
```

This service wil make HTTP requests to the above endpoints once the respective messages are consumed.

# Message handling
This service listens to 3 types of messages, namely 
- `CreateTestSessionCmd`
- `NotifyClientTaskCmd`
- `UploadCmd`

# &#x1F4DD; Raising a Pull Request
- If it's a _feature_...
  - Checkout `master` branch or get latest from `master` branch.
  - Create a branch named `feature/...` from `master` branch.
- If it's a _hotfix_...
  - Checkout the tag corresponding to the version requiring the fix.
  - Create a branch named `hotfix/...` from the tag checkout.
- Apply your changes in the newly created branch and then commit and push.
- Run suitable tests to ensure that the changes are as expected.
- Create a PR from the created branch (using the `master` branch as the base).
- Merge the PR after getting it reviewed and approved.

# &#x1F680; Releasing a New Version
&#x26A0; Deployment is triggered via release of a new version, and versioning is an essential component of software revision management. Do NOT proceed if you are unable to follow these instructions!

- Ensure that the last [build](https://github.com/neurowyzr/nw-apollo-dragon-service/actions) when _merging to master_ was completed without errors.
- [Publish](https://github.com/neurowyzr/nw-apollo-dragon-service/releases/new) a new release based on below guidelines.
  - Specify a _tag_ for the **next** version as per [semantic versioning](https://semver.org):
      - pre-release - Check the **Set as a pre-release** option and use the tag format `vX.Y.Z-alpha01`.
      - release - Ensure the  **Set as the latest release** option is checked and use the tag format `vX.Y.Z`.
  - Select suitable _target branch_ as follows:
    - the `master` branch when deploying a feature (i.e. a _major_ or _minor_ version).
    - a `hotfix/...` branch when deploying a hotfix (i.e. a _patch_ version).
  - Input a title and information about the release, indicating the version tag and if it's a new feature or a hotfix.
  - Click the **Generate release notes** button.
  - Click the **Save Draft** button and share the draft with your team leader.
  - [Team Leader] Verify the release details, and then click the **Publish Release** button.
- Follow the deployment [progress](https://github.com/neurowyzr/nw-apollo-dragon-service/actions).
- After deployment is complete, verify that the roll-out is successful using the [ping](https://nw-apollo-dragon-service.dev.neurowyzr.com/ping) endpoint.

### Copyright Notice
Copyright (C) Neurowyzr Pte Ltd - All Rights Reserved

Unauthorized copying of the files (sourcecodes, text, images, etc.) of this project, or part thereof, via any medium, is strictly prohibited. All files and the concepts in this project are proprietary and confidential.


_Handcrafted with ♥ at Neurowyzr_
