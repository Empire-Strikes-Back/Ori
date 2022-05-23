(ns Ori.beans
  (:require
   [clojure.core.async :as Little-Rock
    :refer [chan put! take! close! offer! to-chan! timeout thread
            sliding-buffer dropping-buffer
            go >! <! alt! alts! do-alts
            mult tap untap pub sub unsub mix unmix admix
            pipe pipeline pipeline-async]]
   [clojure.java.io :as Wichita.java.io]
   [clojure.string :as Wichita.string]

   [relative.trueskill :as Chip.trueskill]
   [relative.elo :as Chip.elo]
   [relative.rating :as Chip.rating]
   [glicko2.core :as Dale.core]

   [datahike.api :as Deep-Thought.api])
  (:import
   (javax.swing JFrame WindowConstants ImageIcon JPanel JScrollPane JTextArea BoxLayout JEditorPane ScrollPaneConstants SwingUtilities JDialog)
   (javax.swing JMenu JMenuItem JMenuBar KeyStroke JOptionPane JToolBar JButton JToggleButton JSplitPane JLabel JTextPane JTextField JTable JTabbedPane)
   (javax.swing DefaultListSelectionModel JCheckBox UIManager JTable ButtonGroup JPopupMenu)
   (javax.swing.border EmptyBorder)
   (javax.swing.table DefaultTableModel)
   (java.awt Canvas Graphics Graphics2D Shape Color Polygon Dimension BasicStroke Toolkit Insets BorderLayout)
   (java.awt.event KeyListener KeyEvent MouseListener MouseEvent ActionListener ActionEvent ComponentListener ComponentEvent ItemListener ItemEvent)
   (javax.swing.event DocumentListener DocumentEvent ListSelectionListener ListSelectionEvent MenuKeyListener MenuKeyEvent)
   (javax.swing.text SimpleAttributeSet StyleConstants JTextComponent)
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
   (java.nio.charset StandardCharsets)))

(do (set! *warn-on-reflection* true) (set! *unchecked-math* true))

(defn q-column-names
  [conn]
  (->>
   (Deep-Thought.api/q '[:find [?ident ...]
                         :where [_ :db/ident ?ident]]
                       @conn)
   (sort)
   (into []
         (comp
          (keep (fn [attr] (if (#{:id :name} attr) nil attr)))
          (map name)))))

(defn process
  [{:keys [^JPanel jpanel-tab
           path]
    :as opts}]
  (let [ops| (chan 10)
        jtable (JTable.)
        jtable-scroll-pane (JScrollPane.)
        table-model (DefaultTableModel.)
        button-group-tables (ButtonGroup.)
        jpanel-buttons (JPanel.)
        button-group-tables-action-listener
        (reify ActionListener
          (actionPerformed [_ event]
            #_(println (.getText ^JToggleButton (.getSource ^ActionEvent event)))))
        button-group-tables-item-listener
        (reify ItemListener
          (itemStateChanged [_ event]
            (let [state-change (.getStateChange ^ItemEvent event)]
              (put! ops| {:op :table-selected
                          :name (.getText ^JToggleButton (.getSource ^ItemEvent event))})
              #_(println :selected (= state-change ItemEvent/SELECTED))
              #_(println  (.getText ^JToggleButton (.getSource ^ItemEvent event))))))
        
        _ (Wichita.java.io/make-parents path)
        
        config-databases {:store {:backend :file :path path}
                          :keep-history? true
                          :name ":database"}
        _ (when-not (Deep-Thought.api/database-exists? config-databases)
            (Deep-Thought.api/create-database config-databases))
        conn-databases (Deep-Thought.api/connect config-databases)
        schema-databases (read-string (slurp (Wichita.java.io/resource "Ori/schema.edn")))]
    (let []
      (Deep-Thought.api/transact conn-databases schema-databases)
      (->>
       (Deep-Thought.api/q '[:find [?ident ...]
                             :where [_ :db/ident ?ident]]
                           @conn-databases)
       (sort)
       (println)))

    (let [buttons [(JToggleButton. ":database")]]
      (doseq [^JToggleButton button buttons]
        (.addActionListener button button-group-tables-action-listener)
        (.addItemListener button button-group-tables-item-listener)
        (.add button-group-tables button)
        (.add jpanel-buttons button)))

    (doto jtable
      (.setModel table-model)
      (.setRowSelectionAllowed true)
      (.setSelectionModel (doto (DefaultListSelectionModel.)
                            (.addListSelectionListener
                             (reify ListSelectionListener
                               (valueChanged [_ event]
                                 (when (not= -1 (.getSelectedRow jtable))
                                   (SwingUtilities/invokeLater
                                    (reify Runnable
                                      (run [_]
                                        #_(.setText jtext-field-frequency (.getValueAt jtable (.getSelectedRow jtable) 0)))))))))))
      #_(.setAutoCreateRowSorter true))

    (doto jtable-scroll-pane
      (.setViewportView jtable)
      (.setHorizontalScrollBarPolicy ScrollPaneConstants/HORIZONTAL_SCROLLBAR_NEVER))

    (doto jpanel-tab
      (.setLayout (MigLayout. "insets 10"))
      (.add jpanel-buttons "cell 0 0 1 1")
      (.add jtable-scroll-pane "cell 0 1 3 1, width 100%"))
    
    (go
      (loop []
        (when-let [value (<! ops|)]
          (condp = (:op value)
            :table-selected
            (let [{:keys [name]} value]

              (if (= name ":database")
                (let [^"[[Ljava.lang.Object;"
                      data (to-array-2d [["1" "2"]
                                         ["1" "2"]])
                      ^"[Ljava.lang.Object;"
                      columns (into-array ^Object (q-column-names conn-databases))]
                  (.setDataVector table-model data columns)))))
          (recur))))))


(comment

  (let [popup-menu (JPopupMenu.)]
    (doto popup-menu
      (.add (JMenuItem. "create"))
      (.add (JMenuItem. "remove"))
      (.addMenuKeyListener (reify MenuKeyListener
                             (menuKeyPressed [_ event]
                               (println (.getPath ^MenuKeyEvent event)))
                             (menuKeyReleased [_ event])
                             (menuKeyTyped [_ event]))))
    (.setComponentPopupMenu jtable popup-menu)
    (.setDataVector table-model
                    ^"[[Ljava.lang.Object;"
                    (to-array-2d [["1" "2"]
                                  ["1" "2"]])
                    ^"[Ljava.lang.Object;"
                    (into-array ^Object (q-column-names conn-databases))))

  ;
  )