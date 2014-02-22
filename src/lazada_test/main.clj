(ns lazada-test.main
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [lazada-test.scrape :refer [render-tree scrape]]
            [hiccup.core :as hiccup]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]))

(def products-tree (let [tree (-> (scrape) tree->hiccup  hiccup/html)]
                     (println "Products tree written to results.html")
                     (spit "results.html" tree)
                     tree))

(defroutes app
  (GET "/" [] products-tree)
  (route/not-found "<h1>Page not found</h1>"))

(defn -main []
  (println "Results at http://localhost:3000/")
  (run-jetty app {:port 3000}))
