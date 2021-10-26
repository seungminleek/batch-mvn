package com.example.batchmvn.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.json.simple.parser.JSONParser;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class RestController {

    public String callingApi(String access_token) {

        // 에어서울용 개발키
        String clientId     = "f4422686-e717-4403-b4c7-aefd8204df68";
        String clientSecret = "e5a98f9b711b81afe6f98010c3014b93";
        String encKey       = "airseoul11R211007003";   // 20byte
        String entrCd       = "AIR4530155";     // 10byte
        String appKey = "527183d2bcc54fc7a754f187c8719e0c";

        encKey = encKey + entrCd + "@@"; // encKey 32byte 재조립 encKey + entrCd + @@


        // 실제 API 호출
        try {
            // 20211026 손진영대리 코드 추가
            // 1. client unix 시간
            Date currentDate    = new Date();
            // pc 별로 환경에 맞게 적용
            long unixTime = (currentDate.getTime() / 1000) + 40;
            // 2. 요청 token 조립
            String StringToken = access_token + ":" + unixTime + ":" + clientId;
            // 3. 요청 token 암호화
            String bearer_token = "bearer " + encrypt(encKey, StringToken);


            System.out.println("---------------------API 통신 테스트 시작-----------------------");
            try {
                String apiURL = "https://t-apis.hanafnapimarket.com:22001/onegw/hbk/api/hbk-service/v1/exchangeRate";
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setConnectTimeout(5000); // Connection Timeout 설정
                con.setReadTimeout(5000); // Read Timeout 설정
                con.setDoOutput(true); // OutPutStream 사용
                con.setDoInput(true); // Input Stream 사용
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                con.setRequestProperty("Authorization", bearer_token);
    //            con.setRequestProperty("Authorization", bearerToken); // 이미 bearer 붙어있음
                con.setRequestProperty("ENTR_CD", entrCd);
                con.setRequestProperty("APP_KEY", appKey);

                // post request pram값 입력 - 에어서울용으로 변경
                String jsonDataBody = "{\n" +
                        "    \"dataBody\": {\n" +
                        "        \"curCd\": \"\",\n" +
                        "        \"notiDiv\": 0,\n" +
                        "        \"notiCnt\": \"\"\n" +
                        "    },\n" +
                        "    \"dataHeader\": {\n" +
                        "        \"CLNT_IP_ADDR\": \"14.32.95.222\",\n" +
                        "        \"CNTY_CD\": \"kr\",\n" +
                        "        \"ENTR_CD\": \"AIR4530155\",\n" +
                        "        \"DEV_CD\": \"T\"\n" +
                        "    }\n" +
                        "}";
                OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
                wr.write(jsonDataBody);
                wr.flush();

                StringBuilder sb = new StringBuilder();
                if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();

                JSONParser jsonParser = new JSONParser();
                    // import org.json.simple.parser.JSONParser; (json-simple 1.1 라이브러리 필요)

                Map responseBody = (Map) jsonParser.parse(sb.toString());

                System.out.println("[DataHeader] : " + responseBody.get("dataHeader"));
                System.out.println("[DataBody]: " + responseBody.get("dataBody"));
                } else {
                    System.out.println(con.getResponseMessage());
                    String msg = con.getResponseMessage();
                }
            } catch (Exception e) {
                System.out.println(e);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("---------------------API 통신 테스트 완료-----------------------");

        return "";
    }

    public String encrypt(String key, String msg) {

        byte[] bEncrypt = null;
        try {
            SecureRandom random = new SecureRandom();
            byte bytes[] = new byte[20];
            random.nextBytes(bytes);
            byte[] saltBytes = bytes;

            SecretKeySpec secret = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            AlgorithmParameters params = cipher.getParameters();

            byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();

            byte[] encByte = cipher.doFinal(msg.getBytes("UTF-8"));
            bEncrypt = new byte[saltBytes.length + ivBytes.length + encByte.length];
            System.arraycopy(saltBytes, 0, bEncrypt, 0, saltBytes.length);
            System.arraycopy(ivBytes, 0, bEncrypt, saltBytes.length, ivBytes.length);
            System.arraycopy(encByte, 0, bEncrypt, saltBytes.length + ivBytes.length, encByte.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Base64.getEncoder().encodeToString(bEncrypt);
    }
}
