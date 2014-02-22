(ns lazada-test.core-test
  (:require [clojure.test :refer :all]
            [lazada-test.scrape :refer [tree->hiccup fetch-url scrape lazada-root]]
            [net.cgrand.enlive-html :as html]
            [hiccup.core :as hiccup]))

(def test-tree {"h" nil
                "a" {"g" nil
                     "e" {"f" nil}
                     "b" {"d" '("x" "y")
                          "c" '("x" "y")}}})

(def test-page (-> (list [:li.multiMenu {:data-sub-menu "1"} "a"]
                         [:li.multiMenu {:data-sub-menu "2"} "h"]
                         [:div#1
                          [:div.nav-title "b"]
                          [:div.nav-linklist
                           [:a {:href "c"} "c"]
                           [:a {:href "d"} "d"]]
                          [:div.nav-title "e"]
                          [:div.nav-linklist
                           [:a {:href "f"} "f"]]
                          [:div.nav-title "g"]])
                   hiccup/html
                   java.io.StringReader.
                   html/html-resource))

(def test-product-page (-> (list [:div#productsCatalog
                                  [:ul [:li [:em.itm-title "x"]]]
                                  [:ul [:li [:em.itm-title "y"]]]])
                           hiccup/html
                           java.io.StringReader.
                           html/html-resource))

(alter-var-root #'fetch-url (constantly #(condp = %
                                           lazada-root test-page
                                           (str lazada-root "f") (throw (java.io.FileNotFoundException.))
                                           test-product-page)))

(def test-tree-html "<ul><li><b>a</b></li><ul><li><b>b</b></li><ul><li><b>c</b></li><ul><li><i>x</i></li><li><i>y</i></li></ul><li><b>d</b></li><ul><li><i>x</i></li><li><i>y</i></li></ul></ul><li><b>e</b></li><ul><li><b>f</b></li></ul><li><b>g</b></li></ul><li><b>h</b></li></ul>")

(deftest scrape-test
  (testing "testing if tree rendering works"
    (is (= test-tree-html (hiccup/html (tree->hiccup test-tree)))))
  (testing "testing if scraping works"
    (is (= (scrape) test-tree))))
