# magnificent

[![Build Status](https://travis-ci.org/zalando-stups/magnificent.svg?branch=master)](https://travis-ci.org/zalando-stups/magnificent)

Magnificent provides the API for user, team and account information to STUPS services. It also encapsulates common authorization logic among them.

## Configuring

* `HTTP_PORT`: Port to run magnificent on. Defaults to 8080.
* `ROBOT_PREFIX`: Prefix for robot users, defaults to `""`.
* `HTTP_TEAM_API`: Base URL of Team API, no default.
* `HTTP_USER_API`: Base URL of User API, no default.
* `HTTP_ACCOUNT_API`: Base URL of Account API, no default.
* `HTTP_TOKENINFO_URL`: URL where to check OAuth tokens, no default.

## Building

    $ lein uberjar
    $ lein docker build

## Releasing

    $ lein release :minor

## License

Copyright © 2015 Zalando SE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
