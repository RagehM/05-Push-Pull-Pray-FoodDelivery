package com.team05.fooddelivery.checkout.dto;

import com.team05.fooddelivery.checkout.enums.OfferDiscountType;

public class OfferUsageDTO {

    private Long offerId;
    private String code;
    private OfferDiscountType discountType;
    private Double discountValue;
    private Integer timesUsed;
    private Double totalDiscountGiven;
    private Boolean active;
    private Boolean expired;

    private OfferUsageDTO(Builder builder) {
        this.offerId = builder.offerId;
        this.code = builder.code;
        this.discountType = builder.discountType;
        this.discountValue = builder.discountValue;
        this.timesUsed = builder.timesUsed;
        this.totalDiscountGiven = builder.totalDiscountGiven;
        this.active = builder.active;
        this.expired = builder.expired;
    }

    public Long getOfferId() { return offerId; }
    public String getCode() { return code; }
    public OfferDiscountType getDiscountType() { return discountType; }
    public Double getDiscountValue() { return discountValue; }
    public Integer getTimesUsed() { return timesUsed; }
    public Double getTotalDiscountGiven() { return totalDiscountGiven; }
    public Boolean getActive() { return active; }
    public Boolean getExpired() { return expired; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long offerId;
        private String code;
        private OfferDiscountType discountType;
        private Double discountValue;
        private Integer timesUsed;
        private Double totalDiscountGiven;
        private Boolean active;
        private Boolean expired;

        public Builder offerId(Long offerId) { this.offerId = offerId; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder discountType(OfferDiscountType discountType) { this.discountType = discountType; return this; }
        public Builder discountValue(Double discountValue) { this.discountValue = discountValue; return this; }
        public Builder timesUsed(Integer timesUsed) { this.timesUsed = timesUsed; return this; }
        public Builder totalDiscountGiven(Double totalDiscountGiven) { this.totalDiscountGiven = totalDiscountGiven; return this; }
        public Builder active(Boolean active) { this.active = active; return this; }
        public Builder expired(Boolean expired) { this.expired = expired; return this; }

        public OfferUsageDTO build() {
            return new OfferUsageDTO(this);
        }
    }
}