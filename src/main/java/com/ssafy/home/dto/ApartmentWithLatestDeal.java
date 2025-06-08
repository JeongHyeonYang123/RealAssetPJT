package com.ssafy.home.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ApartmentWithLatestDeal {
    private String aptSeq;
    private String sggCd;
    private String umdCd;
    private String umdNm;
    private String jibun;
    private String roadNmSggCd;
    private String roadNm;
    private String roadNmBonbun;
    private String roadNmBubun;
    private String aptNm;
    private Integer buildYear;
    private String latitude;
    private String longitude;

    // 최신 거래 정보
    private String latestDealAmount;
    private Double latestExcluUseAr;
    private Integer latestDealYear;
    private Integer latestDealMonth;
    private Integer latestDealDay;

    // getter/setter

    public static ApartmentWithLatestDeal from(com.ssafy.home.domain.HouseInfo info,
            com.ssafy.home.domain.HouseDeal deal) {
        ApartmentWithLatestDeal dto = new ApartmentWithLatestDeal();
        dto.setAptSeq(info.getAptSeq());
        dto.setSggCd(info.getSggCd());
        dto.setUmdCd(info.getUmdCd());
        dto.setUmdNm(info.getUmdNm());
        dto.setJibun(info.getJibun());
        dto.setRoadNmSggCd(info.getRoadNmSggCd());
        dto.setRoadNm(info.getRoadNm());
        dto.setRoadNmBonbun(info.getRoadNmBonbun());
        dto.setRoadNmBubun(info.getRoadNmBubun());
        dto.setAptNm(info.getAptNm());
        dto.setBuildYear(info.getBuildYear());
        dto.setLatitude(info.getLatitude());
        dto.setLongitude(info.getLongitude());
        if (deal != null) {
            dto.setLatestDealAmount(deal.getDealAmount());
            dto.setLatestExcluUseAr(deal.getExcluUseAr());
            dto.setLatestDealYear(deal.getDealYear());
            dto.setLatestDealMonth(deal.getDealMonth());
            dto.setLatestDealDay(deal.getDealDay());
        }
        return dto;
    }
}
