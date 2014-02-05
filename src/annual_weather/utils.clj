(ns annual-weather.utils )

(defmacro ?
  [val]
  `(let [x# ~val]
           (print '~val "is " x#)
     x#
     ))
