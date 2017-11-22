package controller;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Text;
import javafx.util.Duration;

import org.apache.commons.lang3.StringUtils;

import service.CommentService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import constant.LocalConstant;
import dto.CommentDto;
import dto.Delivery;

public class MainController implements Initializable{

    @FXML
    private TextArea commentArea;

    @FXML
    private Button send;

    @FXML
    private Button connect;

    @FXML
    private TextField urlField;

    @FXML
    private ListView<String> authrorListView;

    @FXML
    private TableView<CommentDto> commentTableView;

    @FXML
    private TableColumn<CommentDto, Integer> tableColumnCommentNumber;
    @FXML
    private TableColumn<CommentDto, String> tableColumnName;
    @FXML
    private TableColumn<CommentDto, String> tableColumnTime;
    @FXML
    private TableColumn<CommentDto, String> tableColumnComment;

    @FXML
    private Hyperlink urlLink;

    @FXML
    private Button findDeliveryButton;

    @FXML
    private Label info;

    private CommentService cs = new CommentService();

    private Map<String, String> keyId = new HashMap<String, String>();

    // link = ユーザ名/タイトル
    private Map<String, String> keyLink = new HashMap<String, String>();

    private String connectingStreamid;

    private int currentCommentNumber;

    private Timeline timer;

    // **********************************************************************
    // イベント群
    // **********************************************************************
    /**
     * 初期表示
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 配信情報取得
        if (!setDeliveryList()) {
            info.setText("配信情報が取得できませんでした");
            return;
        }

        // 配信情報一覧を変更したときのイベントを設定
        authrorListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                urlField.setText(new_val);
            }
        });

        //
        authrorListView.setOnMouseClicked(event -> {
            boolean isDoubleClick = event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2;
            if (isDoubleClick) {
                handleConnect(null);
            }
        });

        tableColumnCommentNumber.setCellValueFactory(new PropertyValueFactory<>("commentNumber"));
        tableColumnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        tableColumnTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        tableColumnComment.setCellValueFactory(new PropertyValueFactory<>("comment"));

        // comment列
        tableColumnName.setCellFactory(arg0 -> {
            TableCell<CommentDto, String> tableCell = new TableCell<CommentDto, String>() {
                @Override
                protected void updateItem(final String item, final boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) {
                        Text text = new Text(item);
                        // 改行許可
                        text.wrappingWidthProperty().bind(getTableColumn().widthProperty());
                        text.fontProperty().bind(fontProperty());

                        // TableCellはデフォルトでtable-textクラスが設定されている。
                        // 上記設定を行うとカスタムのTableCellとなるためtable-textクラスが効かなくなってしまう
                        // そのため、セル選択時もフォントカラーが黒のままとなるためクラスを付与する
                        // [参考]http://stackoverflow.com/questions/25181076/javafx-tablecolumn-wrong-font-color-when-using-text-class-and-selecting-rows
                        text.getStyleClass().add("table-text");
                        setGraphic(text);
                    }
                }
            };
            return tableCell;
        });

        // comment列
        tableColumnComment.setCellFactory(arg0 -> {
            TableCell<CommentDto, String> tableCell = new TableCell<CommentDto, String>() {
                @Override
                protected void updateItem(final String item, final boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) {
                        Text text = new Text(item);
                        // 改行許可
                        text.wrappingWidthProperty().bind(getTableColumn().widthProperty());
                        text.fontProperty().bind(fontProperty());
                        text.getStyleClass().add("table-text");
                        setGraphic(text);
                    }
                }
            };
            return tableCell;
        });

        // セルを選択できるように許可
        TableViewSelectionModel<CommentDto> selectionModel = commentTableView.getSelectionModel();
        selectionModel.setCellSelectionEnabled(true);

        // 非同期処理の設定のみを実施
        timeline();
    }

    /**
     * 送信ボタン押下
     * @param event
     */
    @FXML
    private void handleSend(ActionEvent event) {
        send();
    }

    /**
     * 送信ボタン押下（Enter）
     * @param keyEvent
     */
    @FXML
    private void onSend(KeyEvent keyEvent) {
        if(keyEvent.getCode() == KeyCode.ENTER) {
            send();
        }
    }

    /**
     * 接続ボタン押下
     * @param event
     */
    @FXML
    private void handleConnect(ActionEvent event) {
        // 配信チェック
        String inputAuthor = urlField.getText();
        String streamid = keyId.get(inputAuthor);
        if (StringUtils.isEmpty(streamid)) {
            if (!isDeliver(inputAuthor)) {
                info.setText("入力した配信者は現在配信していません");
                return;
            }
        }

        timer.stop();

        boolean connectResult = connect(streamid, LocalConstant.COMMENT_NUMBER_DEFAULT);
        if (connectResult) {
            try {
                urlLink.setText(String.format(LocalConstant.LIVETUBE_URL, URLDecoder.decode(keyLink.get(urlField.getText()),"UTF-8")));
                urlLink.setDisable(false);
                commentTableView.setDisable(false);
                send.setDisable(false);
                commentArea.setDisable(false);

                info.setText(null);
                timer.play();
            } catch (UnsupportedEncodingException e) {
                urlLink.setText("非接続");
                urlLink.setDisable(true);
                commentTableView.setDisable(true);
                send.setDisable(true);
                commentArea.setDisable(true);

                info.setText("接続に失敗しました");
            }
        } else {
            urlLink.setText("非接続");
            urlLink.setDisable(true);
            commentTableView.setDisable(true);
            send.setDisable(true);
            commentArea.setDisable(true);

            info.setText("接続に失敗しました");
        }
    }

    /**
     * 接続ボタン押下（Enter）
     * @param keyEvent
     */
    @FXML
    private void onConnect(KeyEvent keyEvent) {
        if(keyEvent.getCode() == KeyCode.ENTER) {
            handleConnect(new ActionEvent());
        }
    }

    /**
     * コメントエリアアクション
     * @param keyEvent
     */
    @FXML
    private void onCommentArea(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.TAB && !keyEvent.isShiftDown()) {
            send.requestFocus();
        }
        // ctrl+enter or shift+enterで送信する
        if (keyEvent.getCode() == KeyCode.ENTER &&
                (keyEvent.isControlDown() || keyEvent.isShiftDown())) {
            send();
        }
    }

    /**
     * リンクアクション
     * @param event
     */
    @FXML
    private void onUrlLink(ActionEvent event) {
        Desktop desktop = Desktop.getDesktop();
        try {
            // URLエンコード出来ない文字を置換する
            String uriString = urlLink.getText().replaceAll("\\s", "+");
            uriString = uriString.replaceAll("　", URLEncoder.encode("　", "UTF-8"));
            uriString = uriString.replaceAll("\\*", URLEncoder.encode("*", "UTF-8"));
            uriString = uriString.replaceAll("-", URLEncoder.encode("-", "UTF-8"));
            uriString = uriString.replaceAll("_", URLEncoder.encode("_", "UTF-8"));

            URI uri = new URI(uriString);
            desktop.browse(uri);
            return;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        info.setText("リンク先が見つかりません");
    }

    /**
     * 配信一覧取得
     * @param event
     */
    @FXML
    private void findDelivery(ActionEvent event) {
        String infoText = null;
        if (!setDeliveryList()) {
            infoText = "配信情報が取得できませんでした";
        }
        info.setText(infoText);
    }

    /**
     * コメント欄アクション
     * @param keyEvent
     */
    @FXML
    private void onCommentView(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.C && keyEvent.isControlDown()) {
            System.out.println("onCommentView!");

            CommentDto test = commentTableView.getSelectionModel().getSelectedItem();
            System.out.println(test.getCommentNumber() + test.getName() + test.getTime() + test.getComment());

//            final Clipboard clipboard = Clipboard.getSystemClipboard();
//            final ClipboardContent content = new ClipboardContent();
//            content.putString("Some text");
//            content.putHtml("<b>Some</b> text");
//            clipboard.setContent(content);
        }
    }

    // **********************************************************************
    // 内部処理
    // **********************************************************************
    /**
     * 配信情報取得
     * @return
     */
    private List<Delivery> findChannel() {
        try {
            URL url = new URL(LocalConstant.LIVETUBE_URL_JSON);
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
                Type listType = new TypeToken<ArrayList<Delivery>>(){}.getType();
                List<Delivery> list = new Gson().fromJson(reader, listType);
                return list;
            } catch (Exception e) {
                System.err.println("Can't connect to " + LocalConstant.LIVETUBE_URL_JSON);
            }
        } catch (Exception e) {
            System.err.println("Can't connect to " + LocalConstant.LIVETUBE_URL_JSON);
        }
        return new ArrayList<>();
    }

    /**
     * コメント送信処理
     * @return true:コメント成功 / false:コメント失敗
     */
    private boolean send() {
        try {
            boolean result = this.cs.postComment(connectingStreamid, commentArea.getText());
            if (result) {
                commentArea.clear();
            }
            info.setText(null);
            return result;
        } catch(Exception e) {
            e.printStackTrace();
            info.setText("コメント送信に失敗しました");
            return false;
        }
    }

    /**
     * コメント接続処理
     * @param streamid
     * @param commentNumber
     */
    private boolean connect(String streamid, int commentNumber) {
        try {
            if (!streamid.equals(connectingStreamid)) {commentTableView.getItems().clear();}

            Map<Integer, CommentDto> comments = this.cs.getComment(streamid, commentNumber);

            int beforeNumberOfComments = commentTableView.getItems().size();
            for(Map.Entry<Integer, CommentDto> e : comments.entrySet()) {
                // 画面に反映されていないコメントがあればリストに追加する
                if (e.getValue().getCommentNumber() > beforeNumberOfComments) {
                    commentTableView.getItems().add(e.getValue());
                }
            }

            connectingStreamid = streamid;
            currentCommentNumber = commentTableView.getItems().size();
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
   }

    /**
     * 非同期処理
     */
    private void timeline() {
        timer = new Timeline(new KeyFrame(Duration.seconds(4), new EventHandler<ActionEvent>() {
//            int i;
            @Override
            public void handle(ActionEvent event) {
                // 引数のcurrentCommentNumberはコメント番号-2する。
                // ロングポーリング未対応
                connect(connectingStreamid, currentCommentNumber - 2);
//                System.out.println("timeline：" + ++i + "回、connectingStreamid：" + connectingStreamid + "、currentCommentNumber：" + currentCommentNumber);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
    }

    /**
     * 配信情報一覧をlistViewに設定する
     * @return true:設定成功 / false:設定失敗
     */
    private boolean setDeliveryList() {
        try {
            keyId = new HashMap<>();
            keyLink = new HashMap<>();
            authrorListView.getItems().clear();

            // 配信情報取得
            List<Delivery> channelList = findChannel();

            // コンボボックスに配信者名をセット
            List<String> authors = new ArrayList<>();
            for (Delivery channel : channelList) {
                String author = channel.getAuthor();
                String id     = channel.getId();
                String link   = channel.getLink();
                keyId.put(author, id);
                keyLink.put(author, link);
                authors.add(author);
            }

            // ソートする
            Collections.sort(authors, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return StringUtils.lowerCase(o1).compareTo(StringUtils.lowerCase(o2));
                }
            });

            // コンボボックスにセット
            authrorListView.setItems(FXCollections.observableArrayList(authors));

            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 配信されているかを確認する
     * @param inputAuthor
     * @return true:配信中 / false:未配信
     */
    private boolean isDeliver(String inputAuthor) {
        // 接続時の配信情報を取得
        List<Delivery> channelList = findChannel();
        // 配信者が配信中か確認
        for (Delivery channel : channelList) {
            if (channel.getAuthor().equals(inputAuthor)) {
                return true;
            }
        }
        return false;
    }
}
