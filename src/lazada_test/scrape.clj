(ns lazada-test.scrape
  (:require [net.cgrand.enlive-html :as html])
  (:import [java.io FileNotFoundException]))

(def lazada-root "http://www.lazada.com.ph")

(defn fetch-url
  "takes a url and cleans linefeeds and whitespace, then returns html map."
  [url]
  (-> (clojure.string/replace (slurp url) #"[\r\n]+ *" "")
      ;; we clean up the html page to make scraping easier and independent
      ;; of newlines and indentation
      java.io.StringReader.
      html/html-resource))

(defn tree->hiccup
  [map-or-vec]
  (cond
   (map? map-or-vec) [:ul (reduce concat (map (fn [[key val]]
                                                (list [:li [:b key]] (tree->hiccup val)))
                                              map-or-vec))]
   (seq? map-or-vec) [:ul (map (fn [s] [:li [:i s]]) map-or-vec)]))

(defn scrape*
  "Scrapes lazada's products tree into a map with 3 levels and 5 products."
  [page]
  (let [;; this collects all title and their child category nodes in a seq
        ;; the arg is id for a submenu
        submenu-seq #(html/select page [(html/id= %) #{(html/attr= :class "nav-title")
                                                       (html/attr= :class "nav-linklist")}])
        ;; returns seq of top 5 products for a page link
        href->top-5 (fn [cat-link]
                      (println "Scraping" cat-link)
                      (try (take 5 (map #(-> % html/text clojure.string/trim)
                                        (html/select
                                         (fetch-url (str lazada-root cat-link))
                                         [(html/id= "productsCatalog") :ul :li :em.itm-title])))
                           (catch FileNotFoundException e nil)))

        linklist-node->map #(reduce merge (map (fn [item]
                                                 {(html/text item)
                                                  (-> item :attrs :href href->top-5)})
                                               (:content %)))
        ;; this groups nodes with class nav-title and nav-linklist together
        f (let [last (atom nil)]
            #(if (= (-> % :attrs :class) "nav-linklist")
               @last
               (swap! last (constantly (not @last)))))

        submenu-id->map #(reduce merge (map (fn [[title-node list-node]]
                                              {(html/text title-node)
                                               (when list-node (linklist-node->map list-node))})
                                            (partition-by f (submenu-seq %))))

        cats (reduce merge (map (fn [menu-item]
                                  (let [name (clojure.string/trim (html/text menu-item))
                                        submenu-id (-> menu-item :attrs :data-sub-menu)
                                        submenu-map (submenu-id->map submenu-id)]
                                    {name submenu-map}))
                                (html/select page [:li.multiMenu])))]
    cats))

(defn scrape []
  (scrape* (fetch-url lazada-root)))
