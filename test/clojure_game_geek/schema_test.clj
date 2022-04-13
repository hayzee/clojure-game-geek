(ns clojure-game-geek.schema-test
  (:require [clojure.test :refer :all])
  (:require [clojure-game-geek.schema :as s])
  (:require [com.walmartlabs.lacinia :as lacinia]))

; Helper utility
(defn observe [element]
  (clojure.pprint/pprint element)
  element)

(def dummy-component nil)

(deftest can-run-a-graphql-query-game-by-id-1
  (let [schema (s/load-schema dummy-component)
        query-string "{ game_by_id(id: \"foo\") { id name summary }}"]
    (is (= {:data {:game_by_id nil}}
           (lacinia/execute schema query-string nil nil)))))

(deftest can-run-a-graphql-query-game-by-id-2
  (let [schema (s/load-schema dummy-component)
        query-string "{ game_by_id(id: \"1236\") { id name summary }}"]
    (is (= {:data {:game_by_id {:id "1236",
                                :name "Tiny Epic Galaxies",
                                :summary "Fast dice-based sci-fi space game with a bit of chaos"}}}
           (lacinia/execute schema query-string nil nil)))))

(deftest can-run-a-graphql-query-game-by-id-3
  (let [schema (s/load-schema dummy-component)
        query-string "{ game_by_id(id: \"1237\") { name designers { name }}}"]
    (is (= {:data {:game_by_id {:name "7 Wonders: Duel",
                                :designers [{:name "Antoine Bauza"}
                                            {:name "Bruno Cathala"}]}}}
           (lacinia/execute schema query-string nil nil)))))

(deftest can-run-a-graphql-query-game-by-id-4
  (let [schema (s/load-schema dummy-component)
        query-string "{ game_by_id(id: \"1237\") { name designers }}"]
    (is (= {:errors [{:message "Field `designers' (of type `Designer') must have at least one selection.",
                      :locations [{:line 1, :column 25}]}]}
           (lacinia/execute schema query-string nil nil)))))

(deftest can-run-a-graphql-query-game-by-id-5
  (let [schema (s/load-schema dummy-component)
        query-string "{ game_by_id(id: \"1234\") { name designers { name games { name }}}}"]
    (is (= {:data {:game_by_id {:name "Zertz",
                                :designers [{:name "Kris Burm",
                                             :games [{:name "Zertz"}]}]}}}
           (lacinia/execute schema query-string nil nil)))))
