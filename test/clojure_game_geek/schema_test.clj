(ns clojure-game-geek.schema-test
  (:require [clojure.test :refer :all])
  (:require [clojure-game-geek.schema :as s])
  (:require [com.walmartlabs.lacinia :as lacinia]))

; Helper utility
(defn observe [element]
  (clojure.pprint/pprint element)
  element)

(deftest can-run-a-graphql-query
  (let [schema (s/load-schema)
        query-string "{ game_by_id(id: \"foo\") { id name summary }}"]
    (is (= {:data {:game_by_id nil}}
           (lacinia/execute schema query-string nil nil)))))

(deftest can-run-a-graphql-query
  (let [schema (s/load-schema)
        query-string "{ game_by_id(id: \"1236\") { id name summary }}"]
    (is (= {:data {:game_by_id {:id "1236",
                                :name "Tiny Epic Galaxies",
                                :summary "Fast dice-based sci-fi space game with a bit of chaos"}}}
           (lacinia/execute schema query-string nil nil)))))
