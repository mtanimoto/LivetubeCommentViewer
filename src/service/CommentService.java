package service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dto.CommentDto;

public class CommentService {

    /**
     * コメント送信
     * @param id 配信ID
     * @param comment コメント
     * @return 結果(true/false)
     */
    public boolean postComment(String id, String comment) {
        String urlString = "http://livetube.cc/stream/" + id + ".comments";
        try {
            URL url = new URL(urlString);
            HttpURLConnection uc = (HttpURLConnection)url.openConnection();
            uc.setDoOutput(true);//POST可能にする
            uc.setUseCaches(false);
            uc.setChunkedStreamingMode(0);
            uc.setRequestProperty("Accept-Language", "jp");
            // リクエストヘッダを設定する
            // リクエストメソッドの設定
            uc.setRequestMethod("POST");
            uc.connect();
            // データを送信する
            String encC = URLEncoder.encode(comment, "UTF-8");  // コメント
            String encName = URLEncoder.encode("", "UTF-8");    // 名前
            String parameter = "c=" + encC + "&name=" + encName;
            DataOutputStream wr = new DataOutputStream(uc.getOutputStream());
            byte[] bytes = parameter.getBytes();
            for (int i=0;i<bytes.length;i++){
                wr.writeByte(bytes[i]);
            }
            wr.flush();
            wr.close();

        } catch (IOException e) {
            System.err.println("Can't connect to " + urlString);
            return false;
        }
        return true;
  }

    /**
     * コメント取得
     * @param streamid 配信ID
     * @param currentNo コメント番号（デフォルトは0）
     * @return コメントマップ（key=コメント番号、value=コメント）
     */
    public Map<Integer, CommentDto> getComment(String streamid, int currentNo) {
        Map<Integer, CommentDto> comments = new TreeMap<>();
        String url = "http://livetube.cc/stream/" + streamid  + ".comments." + currentNo;
        try {
            URL commenturl = new URL(url);
            URLConnection urlcon = commenturl.openConnection();

            try(BufferedReader reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream(), "UTF-8"))) {
                String line;
                int index = 1;
                while ((line = reader.readLine()) != null) {
                    line = URLDecoder.decode(line, "UTF-8");

                    // コメントNoを検索
                    Pattern p = Pattern.compile("^(\\d{5}|\\d{4}|\\d{3}|\\d{2}|\\d{1})");
                    Matcher m = p.matcher(line);
                    m.find();
                    int commentNumber = Integer.parseInt(line.substring(0, m.end()).trim());

                    // 日時を検索
                    p = Pattern.compile("(\\d{1}|\\d{2})/(\\d{1}|\\d{2})\\s(\\d{1}|\\d{2}):(\\d{1}|\\d{2})");
                    m = p.matcher(line);
                    m.find();

                    String time = line.substring(m.start(), m.end() + 1).trim();
                    String comment = line.substring(m.end() + 2);

                    // 名前を検索
                    p = Pattern.compile("\\s:\\s");
                    m = p.matcher(line);
                    m.find();
                    int nameStartIndex = m.end();

                    p = Pattern.compile("(\\d{1}|\\d{2})/(\\d{1}|\\d{2})");
                    m = p.matcher(line);
                    m.find();
                    int nameEndIndex = m.start();
                    String name = line.substring(nameStartIndex, nameEndIndex);

//                    System.out.println(line.substring(0, m.end() + 1) + "　" + line.substring(m.end() + 2));
                    comments.put(index++, new CommentDto(commentNumber, name, time, comment));
                }
            } catch(Exception e) {
                e.printStackTrace();
                return new HashMap<>();
            } finally {
                urlcon = null;
                commenturl = null;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return comments;
    }
}
