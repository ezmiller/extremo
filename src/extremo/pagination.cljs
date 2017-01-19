(ns extremo.pagination)

(def per-page-default 50)

(defn- page-offset [page per-page]
  (* (dec page) per-page))

(defn- total-pages [total per-page]
  (-> (if (zero? total) 1 total)
      (/ per-page)
      (Math/ceil)
      (int)))


