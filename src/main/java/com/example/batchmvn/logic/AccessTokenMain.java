package com.example.batchmvn.logic;

import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AccessTokenMain {

    // API호출시  bearer Token생성 샘플
    public String createBearerToken(String access_token) {

//        String clientId     = "3729a787-4533-41f6-8e1b-d917c1cc1f83";
////        String access_token = "cd519d75-af9f-4195-a1b2-f0eaa781e9ac";
//        String encKey       = "@hanatest1R200108002";
//        String entrCd       = "HAN8260043";


        // 에어서울용 개발키
        String clientId     = "f4422686-e717-4403-b4c7-aefd8204df68";
        String encKey       = "airseoul11R211007003";   // 20byte
        String entrCd       = "AIR4530155";     // 10byte

        encKey = encKey + entrCd + "@@"; // encKey 32byte 재조립 encKey + entrCd + @@
        String bearerToken = "";

        try {

            // 1. client unix시간
            Date currentDate    = new Date();
//            long unixTime = currentDate.getTime() / 1000;
            // 20211026 손진영대리 추가
            long unixTime = (currentDate.getTime() / 1000) * 40;

            // 2. 요청 token 조립
            String StringToken = access_token + ":" + unixTime + ":" + clientId;

            // 3. 요청 token 암호화
//            bearerToken = "bearer " + encrypt(encKey, StringToken);
            bearerToken = encrypt(encKey, StringToken); // bearer 제거
            System.out.println("bearerToken 생성완료:: " + bearerToken);

        } catch(Exception e) {
            e.printStackTrace();
        }

        return bearerToken;
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

//    public static String encrypt(String key, String msg) {
//
//        byte[] bEncrypt = null;
//        try {
//            SecureRandom random = new SecureRandom();
//            byte bytes[] = new byte[20];
//            random.nextBytes(bytes);
//            byte[] saltBytes = bytes;
//
//            SecretKeySpec secret = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
//
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            cipher.init(Cipher.ENCRYPT_MODE, secret);
//            AlgorithmParameters params = cipher.getParameters();
//
//            byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
//
//            byte[] encByte = cipher.doFinal(msg.getBytes("UTF-8"));
//            bEncrypt = new byte[saltBytes.length + ivBytes.length + encByte.length];
//            System.arraycopy(saltBytes, 0, bEncrypt, 0, saltBytes.length);
//            System.arraycopy(ivBytes, 0, bEncrypt, saltBytes.length, ivBytes.length);
//            System.arraycopy(encByte, 0, bEncrypt, saltBytes.length + ivBytes.length, encByte.length);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return Base64.getEncoder().encodeToString(bEncrypt);
//    }

}
