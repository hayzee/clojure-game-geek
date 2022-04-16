(ns clojure-game-geek.schema-test
  (:require [clojure.test :refer :all])
  (:require [clojure-game-geek.schema :as s])
  (:require [com.walmartlabs.lacinia :as lacinia]
            [clojure.walk :as walk]
            [clojure-game-geek.system :as system]
            [com.stuartsierra.component :as component])
  (:import (clojure.lang IPersistentMap)))

(defn simplify
  [m]
  (walk/postwalk
    (fn [node]
      (cond
        (instance? IPersistentMap node)
        (into {} node)

        (seq? node)
        (vec node)

        :else
        node))
    m))

(defonce system (system/new-system))

(defn q
  [query-string]
  (-> system
      :schema-provider
      :schema
      (lacinia/execute query-string nil nil)
      simplify))

(defn system-fixture [f]
  (alter-var-root #'system component/start-system)
  (f)
  (alter-var-root #'system component/stop-system))

(use-fixtures :once system-fixture)

(deftest can-run-a-graphql-query-game-by-id-1
  (is (= {:data {:game_by_id nil}}
         (q "{ game_by_id(id: \"foo\") { id name summary }}"))))

(deftest can-run-a-graphql-query-game-by-id-2
  (is (= {:data {:game_by_id {:id "1236",
                              :name "Tiny Epic Galaxies",
                              :summary "Fast dice-based sci-fi space game with a bit of chaos"}}}
         (q "{ game_by_id(id: \"1236\") { id name summary }}"))))


(deftest can-run-a-graphql-query-game-by-id-3
  (is (= {:data {:game_by_id {:name      "7 Wonders: Duel",
                              :designers [{:name "Antoine Bauza"}
                                          {:name "Bruno Cathala"}]}}}
         (q "{ game_by_id(id: \"1237\") { name designers { name }}}"))))

(deftest can-run-a-graphql-query-game-by-id-4
  (is (= {:errors [{:message   "Field `designers' (of type `Designer') must have at least one selection.",
                    :locations [{:line 1, :column 25}]}]}
         (q "{ game_by_id(id: \"1237\") { name designers }}"))))

(deftest can-run-a-graphql-query-game-by-id-5
  (is (= {:data {:game_by_id {:name      "Zertz",
                              :designers [{:name  "Kris Burm",
                                           :games [{:name "Zertz"}]}]}}}
         (q "{ game_by_id(id: \"1234\") { name designers { name games { name }}}}"))))

(deftest can-run-a-graphql-query-game-by-id-6
  (is (= {:data {:game_by_id {:name           "7 Wonders: Duel",
                              :rating_summary {:count   3,
                                               :average 4.333333333333333}}}}
         (q "{ game_by_id(id: \"1237\") { name rating_summary { count average }}}"))))

(deftest can-run-a-graphql-query-member-by-id-1
  (is (= {:data {:member_by_id {:member_name "bleedingedge",
                                :ratings     [{:game {:name "Zertz"}, :rating 5}
                                              {:game {:name "Tiny Epic Galaxies"}, :rating 4}
                                              {:game {:name "7 Wonders: Duel"}, :rating 4}]}}}
         (q "{ member_by_id(id: \"1410\") { member_name ratings { game { name } rating }}}"))))

(deftest can-run-a-graphql-query-member-by-id-1
  (is (= {:data {:member_by_id {:member_name "bleedingedge",
                                :ratings     [{:game   {:name           "Zertz",
                                                        :rating_summary {:count 2, :average 4.0},
                                                        :designers      [{:name "Kris Burm", :games [{:name "Zertz"}]}]},
                                               :rating 5}
                                              {:game   {:name           "Tiny Epic Galaxies",
                                                        :rating_summary {:count 1, :average 4.0},
                                                        :designers      [{:name "Scott Almes", :games [{:name "Tiny Epic Galaxies"}]}]},
                                               :rating 4}
                                              {:game   {:name           "7 Wonders: Duel",
                                                        :rating_summary {:count 3, :average 4.333333333333333},
                                                        :designers      [{:name "Antoine Bauza", :games [{:name "7 Wonders: Duel"}]}
                                                                         {:name "Bruno Cathala", :games [{:name "7 Wonders: Duel"}]}]},
                                               :rating 4}]}}}
         (q "{ member_by_id(id: \"1410\") { member_name ratings { game { name rating_summary { count average } designers { name  games { name }}} rating }}}"))))
