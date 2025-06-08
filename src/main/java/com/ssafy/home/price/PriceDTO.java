// PriceDTO.java
package com.ssafy.home.price;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceDTO {
    private LocalDate date;
    private BigDecimal closingPrice;

    public PriceDTO() { }
    public PriceDTO(LocalDate date, BigDecimal closingPrice) {
        this.date = date;
        this.closingPrice = closingPrice;
    }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getClosingPrice() { return closingPrice; }
    public void setClosingPrice(BigDecimal closingPrice) { this.closingPrice = closingPrice; }
}