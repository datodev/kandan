(ns kandan.client.components.plugins
  (:require [clojure.string :as string]
            [kandan.client.components.emoticons :as emoticons]
            [kandan.client.components.highlight :as highlight]
            [om-tools.dom :as dom]))

(defn mention [name]
  (dom/span
   (dom/span
    {:class "mention"}
    name) " "))

(defn mentions [activity-pieces users]
  (map (fn [piece]
         (if (string? piece)
           (let [[_ pre username post]  (re-find #"(.*)@(\w+)(.*)" piece)]
             (if-let [at (and (string? piece)
                              (some #{username} (map :user/nick users)))]
               (list pre (mention username) post)
               piece))
           piece))
       activity-pieces))

(defn emoticon [emoji]
  (dom/img
   {:class (str "emoticon-embed small " (:css emoji))
    :src   (:src emoji)
    :title (:title emoji)}))

(defn emoticons [activity-pieces]
  (let [helper (fn [piece]
                 (if (string? piece)
                   (if-let [emoji (get emoticons/known-emoticons (string/trim piece))]
                     (emoticon emoji)
                     piece)
                   piece))]
    (map helper activity-pieces)))

(defn links [activity-pieces]
  (let [helper (fn [piece]
                 (if-let [link (and (string? piece)
                                    (re-find #"https?://.*" piece))]
                   (dom/a
                    {:class "href"
                     :target "_blank"
                     :href link}
                    link)
                   piece))]
    (map helper activity-pieces)))

(defn code [activity-pieces]
  (let [max-preview-length 300
        max-preview-lines  4
        original           (string/join " " activity-pieces)]
    (if-let [[_ ?lang original] (re-find #"```(.*)?([\s\S]+)```" original)]
      [(dom/div
        {:class "code"}
        (let [preview-line-count (count (string/split #"\n" original))
              preview-src        (as-> original preview
                                   (if (> preview-line-count max-preview-lines)
                                     (string/join "\n" (take max-preview-lines (string/split #"\n" preview)))
                                     preview)
                                   (if (> (count preview) max-preview-length)
                                     (subs preview 0 max-preview-length)
                                     preview)
                                   (highlight/highlight preview))]
          (list (dom/pre #js{:dangerouslySetInnerHTML #js{:__html (aget preview-src "value")}})
                (dom/br)
                (dom/a
                 {:class    "pastie-link"
                  :href     "#"
                  :on-click (constantly false)}
                 "View pastie" (when (not= preview-line-count (count original)) "...")))))]
      activity-pieces)))

(defn pastie [activity-pieces]
  (let [max-preview-length 300
        max-preview-lines  4
        original           (string/join " " activity-pieces)]
    (if (re-find #"\n.*\n" original)
      [(dom/div
        {:class "pastie"}
        (let [preview-line-count (count (string/split #"\n" original))
              preview-src        (as-> original preview
                                   (if (> preview-line-count max-preview-lines)
                                     (string/join "\n" (take max-preview-lines (string/split #"\n" preview)))
                                     preview)
                                   (if (> (count preview) max-preview-length)
                                     (subs preview 0 max-preview-length)
                                     preview)
                                   preview)]
          (list (dom/pre preview-src)
                (dom/br)
                (dom/a
                 {:class    "pastie-link"
                  :href     "#"
                  :on-click (constantly false)}
                 "View pastie" (when (not= preview-line-count (count original)) "...")))))]
      activity-pieces)))

(defn slash-me [activity-pieces me users]
  (if (= (first activity-pieces) "/me")
    (assoc-in (vec activity-pieces) [0] (or (:user/nick  me)
                                            (:user/email me)))
    activity-pieces))

(defn slash-play [activity-pieces]
  ;; Should actually insert a audio player component
  (let [[command url & rest] activity-pieces]
    (if (= command "/play")
      (concat (list (dom/strong
                     (dom/a
                      {:class "audio-play"}
                      "Playing "
                      (dom/a
                       {:target "_blank"
                        :href   url} url))
                     (dom/audio
                      {:controls true
                       :src      url}))) rest)
      activity-pieces)))

(defn rgb-embed [activity-pieces]
  (map (fn [piece]
         (if-let [[_ pre r g b post] (and (string? piece)
                                          (re-find #"(.*)rgb\((\d{1,3}),(\d{1,3}),(\d{1,3})\)(.*)" piece))]
           (dom/pre
            (dom/span
             {:class "color-preview"
              :style {:backgroundColor (str "rgb(" r "," g "," b ")")}})
            post)
           piece)) activity-pieces))

(defn hex-embed [activity-pieces]
  (map (fn [piece]
         (if-let [[_ pre hex post] (and (string? piece)
                                        (re-find #"(.*)#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})(.*)" piece))]
           (dom/pre
            (dom/span 
             {:class "color-preview"
              :style {:backgroundColor (str "#" hex)}})
            post)
           piece)) activity-pieces))

(defn image-embed [activity-pieces]
  (map (fn [piece]
         (if (and (string? piece)
                  (re-find #"http.*\.(jpg|jpeg|gif|png)" piece))
           (dom/div
            {:class "image-preview"}
            (dom/a
             {:target "_blank"
              :href   piece}
             (dom/img
              {:class "image-embed"
               :src   piece}))
            (dom/div
             {:class "name"}
             piece))
           piece)) activity-pieces))

(defn youtube-embed [activity-pieces]
  (map (fn [piece]
         (if-let [[_ video-id] (and (string? piece)
                                    (re-find #"https?.+www.youtube.com.+watch" piece)
                                    (re-find #"\Wv=([\w|\-]*)" piece))]
           (dom/div
            {:class "youtube-preview"}
            (dom/iframe
             {:width           "560"
              :height          "315"
              :src             (str "http://www.youtube.com/embed/" video-id)
              :frameBorder     0
              :allowFullScreen true})
            (dom/div
             {:class "name"}
             piece))
           piece)) activity-pieces))

(defn vimeo-embed [activity-pieces]
  (map (fn [piece]
         (if-let [[_ video-id] (and (string? piece)
                                    (re-find #"^https?://vimeo.com/(\d+)" piece))]
           (dom/div
            {:class "vimeo-preview"}
            (dom/iframe
             {:width                 "500"
              :height                "281"
              :src                   (str "http://player.vimeo.com/video/" video-id)
              :frameBorder           0
              :webkitAllowFullScreen true
              :mozAllowFullScreen    true
              :allowFullScreen       true})
            (dom/div
             {:class "name"}))
           piece)) activity-pieces))
