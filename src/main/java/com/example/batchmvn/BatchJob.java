package com.example.batchmvn;

import com.example.batchmvn.dto.ExchangeDto;
import com.example.batchmvn.logic.AccessTokenMain;
import com.example.batchmvn.logic.AuthTokenMain;
import com.example.batchmvn.logic.RestController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;


@Slf4j
@RequiredArgsConstructor
@Configuration
public class BatchJob {

    private final String BATCH_NAME = "배치 시작합니다 by 승민";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final String bankApiUrl = "https://quotation-api-cdn.dunamu.com/v1/forex/recent?codes=FRX.KRWUSD";   // 환율정보 api
    private final RestTemplate restTemplate;
    private List<ExchangeDto> exchangeData = null; // 최종고시 환율정보 데이터
    private String testData = "";
    
    @Bean
    public Job job(){
        return jobBuilderFactory.get(BATCH_NAME)
                .start(step1())
//                .next(step2())
                .build();
    }

    /**
     * 하나은행 rest api 호출 부분
     * @return Step
     */
    @Bean
    public Step step1() {
//        ResponseEntity<ExchangeDto[]> response = restTemplate.getForEntity(bankApiUrl, ExchangeDto[].class );
//        // Map rqData = new HashMap<String, Object>();
//
//        // 하나은행에서 받아온 json 데이터를 엔티티로 변환
//        ExchangeDto[] resultData = response.getBody();  // Body값을 받아서 buffer에 저장
//        if (resultData != null) {
//            exchangeData = Arrays.asList(resultData);       // LIST 형태로 변환
//        }
//
//        if (exchangeData.size() > 0) {
//            testData = exchangeData.get(0).getName();
//        }

        // 하나은행 개발 가이드 적용
        AuthTokenMain authTokenMain = new AuthTokenMain();
        AccessTokenMain accessTokenMain = new AccessTokenMain();
        RestController restController = new RestController();

        // 에어서울용 개발키
        String clientId     = "f4422686-e717-4403-b4c7-aefd8204df68";
        String clientSecret = "e5a98f9b711b81afe6f98010c3014b93";
        String encKey       = "airseoul11R211007003";   // 20byte
        String entrCd       = "AIR4530155";     // 10byte

        String encKeyNew = encKey + entrCd + "@@"; // encKey 32byte 재조립 encKey + entrCd + @@

        // accessToken 발급
        String encToken = authTokenMain.createToken();
        String token = authTokenMain.decrypt(encKeyNew, encToken);
        token = token.split(":")[0];  // 이러면 client id가 담기잖아?

        // 환율api 호출
        String response = restController.callingApi(token);
        System.out.println("--------------------- 하나은행 API호출 완료 -----------------------" + response);
        return stepBuilderFactory.get(BATCH_NAME + "Step1")
                .tasklet((stepContribution, chunkContext) -> {
                    log.info(BATCH_NAME + "@@@@@@@ Step1 Started 하나은행 API호출 완료 :::::::");
                    return RepeatStatus.FINISHED;
                }).build();
    }

    /**
     * XML 파일 생성하기
     * @return Step
     */
    @Bean
    public Step step2() {

        ExchangeDto dtoData = exchangeData.get(0);
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            doc.setXmlStandalone(true);

            // 요소 생성시작
            Element exchangeRates = doc.createElement("ExchangeRates");
            doc.appendChild(exchangeRates);
            Element exchangeRate = doc.createElement("ExchangeRate");
            exchangeRates.appendChild(exchangeRate);
            Element from = doc.createElement("From");
            from.appendChild(doc.createTextNode(dtoData.getCurrencyCode()));
            exchangeRate.appendChild(from);    // 1. 통화코드
            Element to = doc.createElement("To");
            to.appendChild(doc.createTextNode("KRW"));
            exchangeRate.appendChild(to);
            Element effectiveDate = doc.createElement("Effective_Date");
            String dateStr = dtoData.getDate() + " " + dtoData.getTime();
            effectiveDate.appendChild(doc.createTextNode(dateStr));
            exchangeRate.appendChild(effectiveDate);
//            Element exchangeRate = doc.createElement("exchangeRate");
//            exchangeRate.appendChild(doc.createTextNode(dtoData.getTtBuyingPrice()));
//            exchangeRates.appendChild(exchangeRate);    // 2.통화코드

            // XML 파일로 쓰기
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //정렬 스페이스4칸
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //들여쓰기
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes"); //doc.setXmlStandalone(true); 했을때 붙어서 출력되는부분 개행
            DOMSource source = new DOMSource(doc);
            
            // 파일이름지정
//            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMddHHmmss");
//            String fileNm = simpleDateFormat.format(new Date());
//            StreamResult result = new StreamResult(    // 파일경로지정
//                    new FileOutputStream(new File("D://Asianaiidt/batch-mvn/src/main/resources/response/"+ fileNm + ".xml")));
//
//            transformer.transform(source, result);

            log.info((BATCH_NAME + "========= xml 파일 생성 완료 ========="));


        } catch (Exception e){
            e.printStackTrace();
        }

        return stepBuilderFactory.get(BATCH_NAME + "Step2")
                .tasklet((stepContribution, chunkContext) -> {
                    log.info((BATCH_NAME + "@@@@@@@  Step2 Started"));
                    return RepeatStatus.FINISHED;
                }).build();
    }

}
