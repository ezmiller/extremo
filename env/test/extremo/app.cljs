(ns extremo.app
  (:require
    [doo.runner :refer-macros [doo-tests]]
    [extremo.core-test]))

(doo-tests 'extremo.core-test)
