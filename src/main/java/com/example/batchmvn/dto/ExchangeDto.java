package com.example.batchmvn.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RestAPI로 넘어온 데이터를 담고 쓰기 위한 용도.
 *
 * @author leesm
 * @version 1.0
 * @see
 */
@Getter
@Setter
@NoArgsConstructor
public class ExchangeDto {

    private String code;
    private String currencyCode;
    private String currencyName;
    private String country;
    private String name;
    private String date;
    private String time;
    private long recurrenceCount;
    private double basePrice;
    private double openingPrice;
    private double highPrice;
    private double lowPrice;
    private String provider;
}
