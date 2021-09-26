package com.example.batchmvn;

import com.example.batchmvn.dto.ExchangeDto;
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
                .next(step2())
                .build();
    }

    /**
     * 하나은행 rest api 호출 부분
     * @return Step
     */
    @Bean
    public Step step1() {       //TODO: POST 방식으로 바꿔야함, json 형태로 request 하는 부분도 필요
        ResponseEntity<ExchangeDto[]> response = restTemplate.getForEntity(bankApiUrl, ExchangeDto[].class );
        // Map rqData = new HashMap<String, Object>();

        // 하나은행에서 받아온 json 데이터를 엔티티로 변환
        ExchangeDto[] resultData = response.getBody();  // Body값을 받아서 buffer에 저장
        if (resultData != null) {
            exchangeData = Arrays.asList(resultData);       // LIST 형태로 변환
        }

        if (exchangeData.size() > 0) {
            testData = exchangeData.get(0).getName();
        }

        return stepBuilderFactory.get(BATCH_NAME + "Step1")
                .tasklet((stepContribution, chunkContext) -> {
                    log.info(BATCH_NAME + "@@@@@@@ Step1 Started :: testData :::::" + testData);
                    return RepeatStatus.FINISHED;
                }).build();
    }

    /**
     * XML 파일 생성하기
     * 일단 여기 메소드로 만들어서 단독 로직파일로 빼내기
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
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
            String fileNm = simpleDateFormat.format(new Date());
            StreamResult result = new StreamResult(    // 파일경로지정
                    new FileOutputStream(new File("D://Asianaiidt/batch-mvn/src/main/resources/response/exchangeData.xml")));

            transformer.transform(source, result);

            log.info((BATCH_NAME + "========= xml 파일 생성 완료 ========="));

            /* 20210919 todo
             * 1. ibs xml 파일 가지고오기 -> 파일구조
             * 2. sftp 서버 접속 및 업로드 하는 방법
             * 3. log 생성하는 방법
             *  1) 하나은행 api 호출 로그
             *  2) 업로드된 로그
             * 4.
             * 5. vdi 에서 구름 build run
             * + ibs api document
             */

        } catch (Exception e){
            e.printStackTrace();
        }

        return stepBuilderFactory.get(BATCH_NAME + "Step2")
                .tasklet((stepContribution, chunkContext) -> {
                    log.info((BATCH_NAME + "@@@@@@@  Step2 Started"));
                    return RepeatStatus.FINISHED;
                }).build();
    }

    /**
     * 최종 고시된 환율정보 데이터의 유무 확인
     * @return boolean
     */
    private boolean isNotInitialized() {
        return this.exchangeData == null;
    }

}
