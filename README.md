# magnificent

[![Build Status](https://travis-ci.org/zalando-stups/magnificent.svg?branch=master)](https://travis-ci.org/zalando-stups/magnificent)

magnificent is the reference implementation for the [STUPS authorization dependency spec](https://github.com/zalando-stups/dependency-spec).

## Download

Releases are pushed as Docker images in the [public Docker registry](https://registry.hub.docker.com/u/stups/magnificent/):

* Image: [stups/magnificent](https://registry.hub.docker.com/u/stups/magnificent/tags/manage/)

You can run magnificent by starting it with Docker:

    $ docker run -it stups/magnificent

## Configuration

At its heart, magnificent is a rule engine to authorize all API requests from the STUPS infrastructure. It is using
[core.logic](https://github.com/clojure/core.logic) for evaluating the rule set. Consider writing an own configuration
for your organisation and plug it into magnificent.

### How to write an own configuration

TODO: use "radical-agility" as base for modification, explain core.logic briefly, explain defrule syntax

### How to activate the new configuration

TODO: add to Docker image, pass "ruleset" configuration as start parameter

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
