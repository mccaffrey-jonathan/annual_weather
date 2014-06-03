(ns clj-utils.core )

; http://briancarper.net/blog/495/
(defmacro redir [filename & body]
  `(binding [*out* (clojure.java.io/writer ~filename)] ~@body)) 

; generic stuff
; TODO metadata and docstr
(defmacro def->
  [fn-name & fn-body]
  `(defn ~fn-name
     [x#]
     (-> x# ~@fn-body)))

; generic stuff
; TODO metadata and docstr
(defmacro def->>
  [fn-name & fn-body]
  `(defn ~fn-name
     [x#]
     (->> x# ~@fn-body)))

(defmacro ?
  [val]
  `(let [x# ~val]
           (print '~val "is " x#)
     x#))

(defn group-map-by-keys
  "TODO allow nested version, using something like get-in?"
  [m & keys-seq]
  {:pre [(let [all-keys (apply concat keys-seq)]
          (= (count all-keys) (count (distinct all-keys))))]}
    (conj (vec (for [ks keys-seq] (select-keys m ks)))
             (reduce #(apply dissoc %1 %2) m keys-seq)))

