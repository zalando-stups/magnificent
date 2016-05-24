(defproject magnificent "0.1.0-SNAPSHOT"
  :description "Authorization server for the STUPS.io infrastructure."
  :url "https://github.com/zalando-stups/magnificent"

  :license {:name "The Apache License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :min-lein-version "2.0.0"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "2.1.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.zalando.stups/friboo "1.7.0"]]

  :plugins [[io.sarnowski/lein-docker "1.1.0"]
            [org.zalando.stups/lein-scm-source "0.1.0"]]

  :docker {:image-name #=(eval (str (some-> (System/getenv "DEFAULT_DOCKER_REGISTRY")
                                                      (str "/"))
                                              "stups/magnificent"))}

  :main ^:skip-aot org.zalando.stups.magnificent.core
  :uberjar-name "magnificent.jar"

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[ring/ring-mock "0.3.0"]]}}

  :release-tasks [["vcs" "assert-committed"]
                  ["clean"]
                  ["test"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["uberjar"]
                  ["scm-source"]
                  ["docker" "build"]
                  ["docker" "push"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :pom-addition [:developers
                 [:developer {:id "sarnowski"}
                  [:name "Tobias Sarnowski"]
                  [:email "tobias.sarnowski@zalando.de"]
                  [:role "Maintainer"]]])
