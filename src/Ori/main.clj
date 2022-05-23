(ns Ori.main
  (:require
   [clojure.core.async :as Little-Rock
    :refer [chan put! take! close! offer! to-chan! timeout thread
            sliding-buffer dropping-buffer
            go >! <! alt! alts! do-alts
            mult tap untap pub sub unsub mix unmix admix
            pipe pipeline pipeline-async]]
   [clojure.core.async.impl.protocols :refer [closed?]]
   [clojure.java.io :as Wichita.java.io]
   [clojure.string :as Wichita.string]
   [clojure.pprint :as Wichita.pprint]
   [clojure.repl :as Wichita.repl]

   [aleph.http :as Simba.http]
   [manifold.deferred :as Nala.deferred]
   [manifold.stream :as Nala.stream]
   [byte-streams :as Rafiki]
   [cheshire.core :as Cheshire-Cat.core]

   [datahike.api :as Deep-Thought.api]

   [Ori.seed]
   [Ori.dates]
   [Ori.walnuts]
   [Ori.plumbs]
   [Ori.salt]
   [Ori.avocado]
   [Ori.corn]
   [Ori.beans])
  (:import
   (javax.swing JFrame WindowConstants ImageIcon JPanel JScrollPane JTextArea BoxLayout JEditorPane ScrollPaneConstants SwingUtilities JDialog)
   (javax.swing JMenu JMenuItem JMenuBar KeyStroke JOptionPane JToolBar JButton JToggleButton JSplitPane JLabel JTextPane JTextField JTable JTabbedPane)
   (javax.swing DefaultListSelectionModel JCheckBox UIManager)
   (javax.swing.border EmptyBorder)
   (java.awt Canvas Graphics Graphics2D Shape Color Polygon Dimension BasicStroke Toolkit Insets BorderLayout)
   (java.awt.event KeyListener KeyEvent MouseListener MouseEvent ActionListener ActionEvent ComponentListener ComponentEvent)
   (java.awt.event  WindowListener WindowAdapter WindowEvent)
   (java.awt.geom Ellipse2D Ellipse2D$Double Point2D$Double)
   (com.formdev.flatlaf FlatLaf FlatLightLaf)
   (com.formdev.flatlaf.extras FlatUIDefaultsInspector FlatDesktop FlatDesktop$QuitResponse FlatSVGIcon)
   (com.formdev.flatlaf.util SystemInfo UIScale)
   (java.util.function Consumer)
   (java.util ServiceLoader)
   (org.kordamp.ikonli Ikon)
   (org.kordamp.ikonli IkonProvider)
   (org.kordamp.ikonli.swing FontIcon)
   (org.kordamp.ikonli.codicons Codicons)
   (net.miginfocom.swing MigLayout)
   (net.miginfocom.layout ConstraintParser LC UnitValue)
   (java.io File)
   (java.lang Runnable)
   (io.ipfs.api IPFS)
   (java.util.stream Stream)
   (java.util Base64)
   (java.io BufferedReader)
   (java.nio.charset StandardCharsets))
  (:gen-class))

(do (set! *warn-on-reflection* true) (set! *unchecked-math* true))

(defonce stateA (atom nil))
(defonce settingsA (atom nil))
(defonce resize| (chan (sliding-buffer 1)))
(defonce ops| (chan 10))
(def ^:dynamic ^JFrame jframe nil)
(def ^:dynamic ^JPanel jroot-panel nil)
(def ^:const jframe-title "that's why we need a burglar!")

(defn reload
  []
  (require
   '[Ori.seed]
   '[Ori.dates]
   '[Ori.walnuts]
   '[Ori.plumbs]
   '[Ori.salt]
   '[Ori.avocado]
   '[Ori.corn]
   '[Ori.beans]
   '[Ori.main]
   :reload))

(defn encode-base64url-u
  [^String string]
  (-> (Base64/getUrlEncoder) (.withoutPadding)
      (.encodeToString (.getBytes string StandardCharsets/UTF_8)) (->> (str "u"))))

(defn decode-base64url-u
  [^String string]
  (-> (Base64/getUrlDecoder)
      (.decode (subs string 1))
      (String. StandardCharsets/UTF_8)))

(defn pubsub-sub
  [base-url topic message| cancel| raw-stream-connection-pool]
  (let [streamV (volatile! nil)]
    (->
     (Nala.deferred/chain
      (Simba.http/post (str base-url "/api/v0/pubsub/sub")
                       {:query-params {:arg topic}
                        :pool raw-stream-connection-pool})
      :body
      (fn [stream]
        (vreset! streamV stream)
        stream)
      #(Nala.stream/map Rafiki/to-string %)
      (fn [stream]
        (Nala.deferred/loop
         []
          (->
           (Nala.stream/take! stream :none)
           (Nala.deferred/chain
            (fn [message-string]
              (when-not (identical? message-string :none)
                (let [message (Cheshire-Cat.core/parse-string message-string true)]
                  #_(println :message message)
                  (put! message| message))
                (Nala.deferred/recur))))
           (Nala.deferred/catch Exception (fn [ex] (println ex)))))))
     (Nala.deferred/catch Exception (fn [ex] (println ex))))

    (go
      (<! cancel|)
      (Nala.stream/close! @streamV))
    nil))

(defn pubsub-pub
  [base-url topic message]
  (let []

    (->
     (Nala.deferred/chain
      (Simba.http/post (str base-url "/api/v0/pubsub/pub")
                       {:query-params {:arg topic}
                        :multipart [{:name "file" :content message}]})
      :body
      Rafiki/to-string
      (fn [response-string] #_(println :repsponse reresponse-stringsponse)))
     (Nala.deferred/catch
      Exception
      (fn [ex] (println ex))))

    nil))

(defn subscribe-process
  [{:keys [^String ipfs-api-multiaddress
           ^String ipfs-api-url
           frequency
           raw-stream-connection-pool
           sub|
           cancel|
           id|]
    :as opts}]
  (let [ipfs (IPFS. ipfs-api-multiaddress)
        base-url ipfs-api-url
        topic (encode-base64url-u frequency)
        id (-> ipfs (.id) (.get "ID"))
        message| (chan (sliding-buffer 10))]
    (put! id| {:peer-id id})
    (pubsub-sub base-url  topic message| cancel| raw-stream-connection-pool)

    (go
      (loop []
        (when-let [value (<! message|)]
          (put! sub| (merge value
                            {:message (-> (:data value) (decode-base64url-u) (read-string))}))
          #_(println (merge value
                            {:message (-> (:data value) (decode-base64url-u) (read-string))}))
          #_(when-not (= (:from value) id)

              #_(println (merge value
                                {:message (-> (:data value) (decode-base64url-u) (read-string))})))
          (recur))))

    #_(go
        (loop []
          (<! (timeout 2000))
          (pubsub-pub base-url topic (str {:id id
                                           :rand-int (rand-int 100)}))
          (recur)))))

(defn menubar-process
  [{:keys [^JMenuBar jmenubar
           ^JFrame jframe
           menubar|]
    :as opts}]
  (let [on-menubar-item (fn [f]
                          (reify ActionListener
                            (actionPerformed [_ event]
                              (SwingUtilities/invokeLater
                               (reify Runnable
                                 (run [_]
                                   (f _ event)))))))

        on-menu-item-show-dialog (on-menubar-item (fn [_ event] (JOptionPane/showMessageDialog jframe (.getActionCommand ^ActionEvent event) "menu bar item" JOptionPane/PLAIN_MESSAGE)))]
    (doto jmenubar
      (.add (doto (JMenu.)
              (.setText "program")
              (.setMnemonic \F)
              (.add (doto (JMenuItem.)
                      (.setText "settings")
                      (.setAccelerator (KeyStroke/getKeyStroke KeyEvent/VK_S (-> (Toolkit/getDefaultToolkit) (.getMenuShortcutKeyMask))))
                      (.setMnemonic \S)
                      (.addActionListener
                       (on-menubar-item (fn [_ event]
                                          (put! menubar| {:op :settings}))))))
              (.add (doto (JMenuItem.)
                      (.setText "exit")
                      (.setAccelerator (KeyStroke/getKeyStroke KeyEvent/VK_Q (-> (Toolkit/getDefaultToolkit) (.getMenuShortcutKeyMask))))
                      (.setMnemonic \Q)
                      (.addActionListener (on-menubar-item (fn [_ event]
                                                             (.dispose jframe))))))))

      #_(.add (doto (JMenu.)
                (.setText "edit")
                (.setMnemonic \E)
                (.add (doto (JMenuItem.)
                        (.setText "undo")
                        (.setAccelerator (KeyStroke/getKeyStroke KeyEvent/VK_Z (-> (Toolkit/getDefaultToolkit) (.getMenuShortcutKeyMask))))
                        (.setMnemonic \U)
                        (.addActionListener on-menu-item-show-dialog)))
                (.add (doto (JMenuItem.)
                        (.setText "redo")
                        (.setAccelerator (KeyStroke/getKeyStroke KeyEvent/VK_Y (-> (Toolkit/getDefaultToolkit) (.getMenuShortcutKeyMask))))
                        (.setMnemonic \R)
                        (.addActionListener on-menu-item-show-dialog)))
                (.addSeparator)
                (.add (doto (JMenuItem.)
                        (.setText "cut")
                        (.setAccelerator (KeyStroke/getKeyStroke KeyEvent/VK_X (-> (Toolkit/getDefaultToolkit) (.getMenuShortcutKeyMask))))
                        (.setMnemonic \C)
                        (.addActionListener on-menu-item-show-dialog)))
                (.add (doto (JMenuItem.)
                        (.setText "copy")
                        (.setAccelerator (KeyStroke/getKeyStroke KeyEvent/VK_C (-> (Toolkit/getDefaultToolkit) (.getMenuShortcutKeyMask))))
                        (.setMnemonic \O)
                        (.addActionListener on-menu-item-show-dialog)))
                (.add (doto (JMenuItem.)
                        (.setText "paste")
                        (.setAccelerator (KeyStroke/getKeyStroke KeyEvent/VK_V (-> (Toolkit/getDefaultToolkit) (.getMenuShortcutKeyMask))))
                        (.setMnemonic \P)
                        (.addActionListener on-menu-item-show-dialog)))
                (.addSeparator)
                (.add (doto (JMenuItem.)
                        (.setText "delete")
                        (.setAccelerator (KeyStroke/getKeyStroke KeyEvent/VK_DELETE 0))
                        (.setMnemonic \D)
                        (.addActionListener on-menu-item-show-dialog)))))))
  nil)

(defn toolbar-process
  [{:keys [^JToolBar jtoolbar]
    :as opts}]
  (let []
    (doto jtoolbar
      #_(.setMargin (Insets. 3 3 3 3))
      (.add (doto (JButton.)
              (.setToolTipText "new file")
              (.setIcon (FontIcon/of org.kordamp.ikonli.codicons.Codicons/NEW_FILE (UIScale/scale 16) Color/BLACK))))
      (.add (doto (JButton.)
              (.setToolTipText "open file")
              (.setIcon (FontIcon/of org.kordamp.ikonli.codicons.Codicons/FOLDER_OPENED (UIScale/scale 16) Color/BLACK))))
      (.add (doto (JButton.)
              (.setToolTipText "save")
              (.setIcon (FontIcon/of org.kordamp.ikonli.codicons.Codicons/SAVE (UIScale/scale 16) Color/BLACK))))
      (.add (doto (JButton.)
              (.setToolTipText "undo")
              (.setIcon (FontIcon/of org.kordamp.ikonli.codicons.Codicons/DISCARD (UIScale/scale 16) Color/BLACK))))
      (.add (doto (JButton.)
              (.setToolTipText "redo")
              (.setIcon (FontIcon/of org.kordamp.ikonli.codicons.Codicons/REDO (UIScale/scale 16) Color/BLACK))))
      #_(.addSeparator)))
  nil)

(defn settings-process
  [{:keys [^JFrame root-jframe
           ^JFrame jframe
           ops|
           settingsA]
    :or {}
    :as opts}]
  (let [root-panel (JPanel.)
        jscroll-pane (JScrollPane.)

        jcheckbox-apricotseed? (JCheckBox.)]

    (doto jscroll-pane
      (.setViewportView root-panel)
      (.setHorizontalScrollBarPolicy ScrollPaneConstants/HORIZONTAL_SCROLLBAR_NEVER))

    (doto jframe
      (.add root-panel))

    (doto root-panel
      (.setLayout (MigLayout. "insets 10"))
      (.add (JLabel. ":apricotseed?") "cell 0 0")
      (.add jcheckbox-apricotseed? "cell 0 0"))

    (.setPreferredSize jframe (Dimension. (* 0.8 (.getWidth root-jframe))
                                          (* 0.8 (.getHeight root-jframe))))

    (.addActionListener jcheckbox-apricotseed?
                        (reify ActionListener
                          (actionPerformed [_ event]
                            (SwingUtilities/invokeLater
                             (reify Runnable
                               (run [_]
                                 (put! ops| {:op :settings-value
                                             :apricotseed? (.isSelected jcheckbox-apricotseed?)})))))))

    (remove-watch settingsA :settings-process)
    (add-watch settingsA :settings-process
               (fn [ref wathc-key old-state new-state]
                 (SwingUtilities/invokeLater
                  (reify Runnable
                    (run [_]
                      (.setSelected jcheckbox-apricotseed? (:apricotseed? new-state)))))))

    (doto jframe
      (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE #_WindowConstants/EXIT_ON_CLOSE)
      (.pack)
      (.setLocationRelativeTo root-jframe)
      (.setVisible true)))
  nil)

(defn -main
  [& args]
  (println "he said he's an expert - hey hey!")
  (println "i dont want my next job")

  #_(alter-var-root #'*ns* (constantly (find-ns 'Ori.main)))

  (when SystemInfo/isMacOS
    (System/setProperty "apple.laf.useScreenMenuBar" "true")
    (System/setProperty "apple.awt.application.name" jframe-title)
    (System/setProperty "apple.awt.application.appearance" "system"))

  (when SystemInfo/isLinux
    (JFrame/setDefaultLookAndFeelDecorated true)
    (JDialog/setDefaultLookAndFeelDecorated true))

  (when (and
         (not SystemInfo/isJava_9_orLater)
         (= (System/getProperty "flatlaf.uiScale") nil))
    (System/setProperty "flatlaf.uiScale" "2x"))

  (FlatLaf/setGlobalExtraDefaults (java.util.Collections/singletonMap "@background" "#ffffff"))
  (FlatLightLaf/setup)

  #_(UIManager/put "background" Color/WHITE)
  (FlatLaf/updateUI)

  (FlatDesktop/setQuitHandler (reify Consumer
                                (accept [_ response]
                                  (.performQuit ^FlatDesktop$QuitResponse response))
                                (andThen [_ after] after)))

  (let [screenshotsMode? (Boolean/parseBoolean (System/getProperty "flatlaf.demo.screenshotsMode"))

        jframe (JFrame. jframe-title)
        jroot-panel (JPanel.)
        jmenubar (JMenuBar.)]

    (let [data-dir-path (or
                         (some-> (System/getenv "ORI_PATH")
                                 (.replaceFirst "^~" (System/getProperty "user.home")))
                         (.getCanonicalPath ^File (Wichita.java.io/file (System/getProperty "user.home") ".Ori")))
          state-file-path (.getCanonicalPath ^File (Wichita.java.io/file data-dir-path "Ori.edn"))]
      (Wichita.java.io/make-parents data-dir-path)
      (reset! stateA {})
      (reset! settingsA {:apricotseed? true})

      (let [jtabbed-pane (JTabbedPane.)
            jpanel-apples (JPanel.)
            jpanel-microwaved-turnips (JPanel.)
            jpanel-corn (JPanel.)
            jpanel-beans (JPanel.)]

        (doto jtabbed-pane
          (.setTabLayoutPolicy JTabbedPane/SCROLL_TAB_LAYOUT)
          (.addTab "rating" jpanel-beans)
          (.addTab "brackets" jpanel-corn)
          (.addTab "edit" jpanel-microwaved-turnips)
          (.addTab "query" jpanel-apples)
          (.setSelectedIndex 0))

        (Ori.beans/process {:jpanel-tab jpanel-beans
                            :path (.getCanonicalPath ^File (Wichita.java.io/file data-dir-path "Deep-Thought"))})

        (.add jroot-panel jtabbed-pane))

      (let [path-db (.getCanonicalPath ^File (Wichita.java.io/file data-dir-path "Deep-Thought"))]
        (Wichita.java.io/make-parents path-db)
        (let [config {:store {:backend :file :path path-db}
                      :keep-history? true
                      :name "main"}
              _ (when-not (Deep-Thought.api/database-exists? config)
                  (Deep-Thought.api/create-database config))
              conn (Deep-Thought.api/connect config)]

          (Deep-Thought.api/transact
           conn
           [{:db/cardinality :db.cardinality/one
             :db/ident :id
             :db/unique :db.unique/identity
             :db/valueType :db.type/uuid}
            {:db/ident :name
             :db/valueType :db.type/string
             :db/cardinality :db.cardinality/one}])

          (Deep-Thought.api/transact
           conn
           [{:id #uuid "3e7c14ce-5f00-4ac2-9822-68f7d5a60952"
             :name  "Deep-Thought"}
            {:id #uuid "f82dc4f3-59c1-492a-8578-6f01986cc4c2"
             :name  "Wichita"}
            {:id #uuid "5358b384-3568-47f9-9a40-a9a306d75b12"
             :name  "Little-Rock"}])

          (->>
           (Deep-Thought.api/q '[:find ?e ?n
                                 :where
                                 [?e :name ?n]]
                               @conn)
           (println))

          (->>
           (Deep-Thought.api/q '[:find [?ident ...]
                                 :where [_ :db/ident ?ident]]
                               @conn)
           (sort)
           (println)))))

    (SwingUtilities/invokeLater
     (reify Runnable
       (run [_]

         (doto jframe
           (.add jroot-panel)
           (.addComponentListener (let []
                                    (reify ComponentListener
                                      (componentHidden [_ event])
                                      (componentMoved [_ event])
                                      (componentResized [_ event] (put! resize| (.getTime (java.util.Date.))))
                                      (componentShown [_ event]))))
           (.addWindowListener (proxy [WindowAdapter] []
                                 (windowClosing [event]
                                   (let [event ^WindowEvent event]
                                     #_(println :window-closing)
                                     #_(put! host| true)
                                     (-> event (.getWindow) (.dispose)))))))

         (doto jroot-panel
           #_(.setLayout (BoxLayout. jroot-panel BoxLayout/Y_AXIS))
           (.setLayout (MigLayout. "insets 10"
                                   "[grow,shrink,fill]"
                                   "[grow,shrink,fill]")))

         (when-let [url (Wichita.java.io/resource "icon.png")]
           (.setIconImage jframe (.getImage (ImageIcon. url))))

         (menubar-process
          {:jmenubar jmenubar
           :jframe jframe
           :menubar| ops|})
         (.setJMenuBar jframe jmenubar)

         #_(Ori.plumbs/toolbar-process
            {:jtoolbar jtoolbar})
         #_(.add jroot-panel jtoolbar "dock north")


         (.setPreferredSize jframe
                            (let [size (-> (Toolkit/getDefaultToolkit) (.getScreenSize))]
                              (Dimension. (* 0.7 (.getWidth size)) (* 0.7 (.getHeight size)))
                              #_(Dimension. (UIScale/scale 1024) (UIScale/scale 576)))
                            #_(if SystemInfo/isJava_9_orLater
                                (Dimension. 830 440)
                                (Dimension. 1660 880)))

         #_(doto jframe
             (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE #_WindowConstants/EXIT_ON_CLOSE)
             (.setSize 2400 1600)
             (.setLocation 1300 200)
             #_(.add panel)
             (.setVisible true))

         #_(println :before (.getGraphics canvas))
         (doto jframe
           (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE #_WindowConstants/EXIT_ON_CLOSE)
           (.pack)
           (.setLocationRelativeTo nil)
           (.setVisible true))
         #_(println :after (.getGraphics canvas))

         (alter-var-root #'Ori.main/jframe (constantly jframe))

         (remove-watch stateA :watch-fn)
         (add-watch stateA :watch-fn
                    (fn [ref wathc-key old-state new-state]

                      (when (not= old-state new-state))))

         (remove-watch settingsA :main)
         (add-watch settingsA :main
                    (fn [ref wathc-key old-state new-state]
                      (SwingUtilities/invokeLater
                       (reify Runnable
                         (run [_])))))
         (reset! settingsA @settingsA))))


    (go
      (loop []
        (when-let [value (<! ops|)]
          (condp = (:op value)

            :settings
            (let [settings-jframe (JFrame. "settings")]
              (settings-process
               {:jframe settings-jframe
                :root-jframe jframe
                :ops| ops|
                :settingsA settingsA})
              (reset! settingsA @settingsA))

            :settings-value
            (let []
              (swap! settingsA merge value)))

          (recur)))))
  (println "Kuiil has spoken"))


(comment

  (.getName (class (make-array Object 1 1)))

  (.getName (class (make-array String 1)))

  ;
  )