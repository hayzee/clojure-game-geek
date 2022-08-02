(ns clojure-game-geek.schema-test
  (:require [clojure.test :refer :all]
            [com.walmartlabs.lacinia :as lacinia]
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

(use-fixtures :each system-fixture)

(deftest query-non-existent-game-returns-nil
  (is (= {:data {:game_by_id nil}}
         (q "{ game_by_id(id: \"foo\") { id name summary }}"))))

(deftest can-query-a-game-by-id
  (is (= {:data {:game_by_id {:id "1236",
                              :name "Tiny Epic Galaxies",
                              :summary "Fast dice-based sci-fi space game with a bit of chaos"}}}
         (q "{ game_by_id(id: \"1236\") { id name summary }}"))))

(deftest can-query-game->designers
  (is (= {:data {:game_by_id {:name      "7 Wonders: Duel",
                              :designers [{:name "Antoine Bauza"}
                                          {:name "Bruno Cathala"}]}}}
         (q "{ game_by_id(id: \"1237\") { name designers { name }}}"))))

(deftest must-query-at-least-one-member-of-designers
  (is (= {:errors [{:message   "Field `designers' (of type `Designer') must have at least one selection.",
                    :locations [{:line 1, :column 25}]}]}
         (q "{ game_by_id(id: \"1237\") { name designers }}"))))

(deftest can-query-game->designers->games
  (is (= {:data {:game_by_id {:name      "Zertz",
                              :designers [{:name  "Kris Burm",
                                           :games [{:name "Zertz"}]}]}}}
         (q "{ game_by_id(id: \"1234\") { name designers { name games { name }}}}"))))

(deftest can-query-game->rating-summary
  (is (= {:data {:game_by_id {:name           "7 Wonders: Duel",
                              :rating_summary {:count   3,
                                               :average 4.333333333333333}}}}
         (q "{ game_by_id(id: \"1237\") { name rating_summary { count average }}}"))))

(deftest can-query-member->ratings
  (is (= {:data {:member_by_id {:member_name "bleedingedge",
                                :ratings     [{:game {:name "Zertz"}, :rating 5}
                                              {:game {:name "Tiny Epic Galaxies"}, :rating 4}
                                              {:game {:name "7 Wonders: Duel"}, :rating 4}]}}}
         (q "{ member_by_id(id: \"1410\") { member_name ratings { game { name } rating }}}"))))

(deftest can-query-member->ratings->game
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

(deftest can-mutate-member->ratings->game
  (is (= {:data {:member_by_id {:member_name "bleedingedge",
                                :ratings [{:game {:id "1234", :name "Zertz"}, :rating 5}
                                          {:game {:id "1236", :name "Tiny Epic Galaxies"}, :rating 4}
                                          {:game {:id "1237", :name "7 Wonders: Duel"}, :rating 4}]}}}
         (q "{ member_by_id(id: \"1410\") { member_name ratings { game { id name } rating }}}")))
  (is (= {:data {:rate_game {:rating_summary {:count 1
                                              :average 3.0}}}}
         (q "mutation { rate_game(member_id: \"1410\", game_id: \"1236\", rating: 3) { rating_summary { count average }}}")))
  (is (= {:data {:member_by_id {:member_name "bleedingedge",
                                :ratings [{:game {:id "1236", :name "Tiny Epic Galaxies"}, :rating 3}
                                          {:game {:id "1234", :name "Zertz"}, :rating 5}
                                          {:game {:id "1237", :name "7 Wonders: Duel"}, :rating 4}]}}}
         (q "{ member_by_id(id: \"1410\") { member_name ratings { game { id name } rating }}}")))
  (is (= {:data {:rate_game {:name "Dominion"
                             :rating_summary {:count 1
                                              :average 4.0}}}}
        (q "mutation { rate_game(member_id: \"1410\", game_id: \"1235\", rating: 4) { name rating_summary { count average }}}")))
  (is (= {:data {:rate_game nil},
          :errors [{:message "Game not found.",
                    :status 404,
                    :locations [{:line 1, :column 9}],
                    :query-path [:rate_game],
                    :arguments {:member_id "1410", :game_id "9999", :rating "4"}}]}
       (q "mutation { rate_game(member_id: \"1410\", game_id: \"9999\", rating: 4) { name rating_summary { count average }}}")))
  (is (= {:errors [{:message "Exception applying arguments to field `rate_game': Not all non-nullable arguments have supplied values.",
                    :query-path [],
                    :locations [{:line 1, :column 9}],
                    :field :rate_game,
                    :missing-arguments [:rating]}]}
        (q "mutation { rate_game(member_id: \"1410\", game_id: \"9999\") { name rating_summary { count average }}}")))
  (is (= {:errors [{:message "Exception applying arguments to field `rate_game': For argument `rating', scalar value is not parsable as type `Int'.",
                    :query-path [],
                    :locations [{:line 1, :column 9}],
                    :field :rate_game,
                    :argument :rating,
                    :value "Great!",
                    :type-name :Int}]}
        (q "mutation { rate_game(member_id: \"1410\", game_id: \"9999\", rating: \"Great!\") { name rating_summary { count average }}}")))
  )
