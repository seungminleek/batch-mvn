package com.example.batchmvn.logic;

import com.example.batchmvn.dto.ExchangeDto;
import org.json.simple.parser.JSONParser;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import com.google.gson.Gson;

public class AuthTokenMain {

    /**
     * 하나은행 가이드 적용:  AccesToken 발급 API 샘플
     * @throws NoSuchAlgorithmException
     */
    public String createToken() {

        String access_token =  "";

        Date currentDate    = new Date();
        long unixTime = currentDate.getTime() / 1000;
        String serverUrl    = "https://t-apis.hanafnapimarket.com:22001/onegw/hbk/api/oauth/oauth/token";

        // 에어서울용 개발키
        String clientId     = "f4422686-e717-4403-b4c7-aefd8204df68";
        String clientSecret = "e5a98f9b711b81afe6f98010c3014b93";
        String encKey       = "airseoul11R211007003";   // 20byte
        String entrCd       = "AIR4530155";     // 10byte

        encKey = encKey + entrCd + "@@"; // encKey 32byte 재조립 encKey + entrCd + @@

        try {
            //----------------------------------------------
            // 01. Authorization 생성한다. clientId + clientSecret + unixTime (AES256 암호화, BASE64 인코딩)
            //----------------------------------------------
            String authorization = clientId + ":" + clientSecret + ":" + unixTime;
            authorization = "Basic " + encrypt(encKey, authorization);
            System.out.println(authorization);
            // Basic vzX1sWyHZC34xj+iiATJLZ63K14VmyIwa9ey9DkQC/rhx1Y5ZET13J3gnI5X/H24ROidNpL4hWs2rGKBVEDHgeD0kkN3umdPthnK1LBr/yoUr2KlnB7m25DFR14VJrTSJIalfGItsteXKH/ycPRhq4K4YR3EpuBwXFkM99gz1yNtUFdo

            //----------------------------------------------
            // 02. AccesToken 발급 API 호출
            //----------------------------------------------
            String response = getAccesTokenAPI(authorization, entrCd, serverUrl);
            System.out.println(response);

            // 20211026 손진영대리 추가
            Gson gson = new Gson();
            Map<String, Object> mapResult = gson.fromJson(response, Map.class);
            Map<String, String> mapBody = (Map<String, String>) mapResult.get("dataBody");
            access_token = mapBody.get("access_token");


        } catch(Exception e) {
            e.printStackTrace();
        }
        return access_token;
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

    public String getAccesTokenAPI(String authorization, String entrCd, String serverUrl) {

        String rtnData = "";
        HttpURLConnection  httpUrlConn  = null;
        BufferedReader     postRes      = null;
        String             resultData   = null;
        StringBuffer       resultBuffer = new StringBuffer();
        URL                url          = null;

        try {
            //연결시작하기
            url   = new URL(serverUrl);
            httpUrlConn = (HttpURLConnection)url.openConnection();

            //연결값 세팅하기
            httpUrlConn.setRequestMethod("POST");
            httpUrlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpUrlConn.setRequestProperty("ENC_NEW", "Y");
            httpUrlConn.setRequestProperty("ENTR_CD", entrCd);
            httpUrlConn.setRequestProperty("Authorization", authorization);
            httpUrlConn.setDoOutput(true);
            httpUrlConn.setDoInput(true);
            httpUrlConn.setConnectTimeout(1000 * 60);

            //메시지 전송
            String sendData = "grant_type=client_credentials";
            OutputStream os = httpUrlConn.getOutputStream();
            os.write(sendData.getBytes());
            os.flush();
            os.close();

            //전문 수신
            postRes = new BufferedReader(new InputStreamReader(httpUrlConn.getInputStream(), "utf-8"));
            while ((resultData = postRes.readLine()) != null) {
                resultBuffer.append(resultData);
            }


        } catch(Exception e) {
            e.printStackTrace();
        }
        return resultBuffer.toString();
    }

    public static String decrypt(String key, String msg) {

        byte[] bDecrypt = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(msg));

            byte[] saltBytes = new byte[20];
            buffer.get(saltBytes, 0, saltBytes.length);

            byte[] ivBytes = new byte[cipher.getBlockSize()];
            buffer.get(ivBytes, 0, ivBytes.length);
            byte[] encryptedTextBytes = new byte[buffer.capacity() - saltBytes.length - ivBytes.length];
            buffer.get(encryptedTextBytes);

            SecretKeySpec secret = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));

            bDecrypt = cipher.doFinal(encryptedTextBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(bDecrypt);
    }

}