;;   Copyright (c) Dragan Djuric. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns ^{:author "Dragan Djuric"}
    uncomplicate.neanderthal.opencl
  "Specialized constructors that use OpenCL engine by default, and convenient macros for
  creating and maintaining engines in appropriate OpenCL context. A convenience over agnostic
  [[uncomplicate.neanderthal.core]] functions."
  (:require [uncomplicate.commons.core
             :refer [release let-release wrap-float wrap-double]]
            [uncomplicate.clojurecl
             [core :refer [*context* *command-queue* cl-buffer?]]
             [info :refer [queue-context]]]
            [uncomplicate.neanderthal.core :refer [vctr ge tr]]
            [uncomplicate.neanderthal.internal.opencl
             [clblock :refer [->TypedCLAccessor cl-to-host host-to-cl]]
             [clblast :refer [clblast-double clblast-float]]])
  (:import [uncomplicate.neanderthal.internal.api Block DataAccessor]))

(def ^{:dynamic true
       :doc "Dynamically bound OpenCL factory that is used in vector and matrix constructors."}
  *opencl-factory*)

(def ^{:doc "Constructor of a single precision floating point OpenCL factory."}
  opencl-float clblast-float)

(def ^{:doc "Constructor of a double-precision floating point OpenCL factory."}
  opencl-double clblast-double)

(defmacro with-engine
  "Creates an OpenCL factory using the provided `factory` constructor function. The created factory
  will work using the provided queue and its context, and will be bound to [[*opencl-factory*]].
  Enables the use of [[clv]], [[clge]], [[cltr]], etc. in its body.

      (with-default
        (with-engine clblast-float *command-queue*
          (with-release [gpu-x (clv (range 3))]
            (sum gpu-x))))
  "
  ([factory queue & body]
   `(binding [*opencl-factory* (~factory (queue-context ~queue) ~queue)]
      (try
        ~@body
        (finally (release *opencl-factory*))))))

(defmacro with-default-engine
  "Creates an OpenCL factory using the default OpenCL factory (single precision floating point),
  that works in the default OpenCL queue and context acquired through ClojureCL's `*context*` and
  *command-queue*` bindings. The created factory will be bound to [[*opencl-factory*]].

      (with-default
        (with-default-engine
          (with-release [gpu-x (clv (range 3))]
            (sum gpu-x))))
  "
  [& body]
  `(binding [*opencl-factory* (opencl-float *context* *command-queue*)]
     (try
       ~@body
       (finally (release *opencl-factory*)))))

(defn clv
  "Creates a vector using GPU engine provided to the bound [[*opencl-factory*]]
  (see [[uncomplicate.neanderthal.core/vctr]])."
  ([source]
   (vctr *opencl-factory* source))
  ([x & xs]
   (clv (cons x xs))))

(defn clge
  "Creates a GE matrix using GPU engine provided to the bound [[*opencl-factory*]]
  (see [[uncomplicate.neanderthal.core/ge]])."
  ([^long m ^long n source options]
   (ge *opencl-factory* m n source options))
  ([^long m ^long n arg]
   (ge *opencl-factory* m n arg))
  ([^long m ^long n]
   (ge *opencl-factory* m n))
  ([a]
   (ge *opencl-factory* a)))

(defn cltr
  "Creates a TR matrix using GPU engine provided to the bound [[*opencl-factory*]]
  (see [[uncomplicate.neanderthal.core/tr]])."
  ([^long n source options]
   (tr *opencl-factory* n source options))
  ([^long n arg]
   (tr *opencl-factory* n arg))
  ([arg]
   (tr *opencl-factory* arg)))
