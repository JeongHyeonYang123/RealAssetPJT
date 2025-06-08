package com.ssafy.home.mapper;

import com.ssafy.home.domain.DongCode;
import com.ssafy.home.domain.HouseDeal;
import com.ssafy.home.domain.HouseInfo;
import com.ssafy.home.dto.ApartmentWithLatestDeal;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface HouseMapper {

  @Select("SELECT * FROM houseinfos " +
      "WHERE CAST(latitude AS DOUBLE) BETWEEN #{swLat} AND #{neLat} " +
      "AND CAST(longitude AS DOUBLE) BETWEEN #{swLng} AND #{neLng}")
  @Results(id = "houseDealMap", value = {
      @Result(property = "aptSeq", column = "apt_seq"),
      @Result(property = "sggCd", column = "sgg_cd"),
      @Result(property = "umdCd", column = "umd_cd"),
      @Result(property = "umdNm", column = "umd_nm"),
      @Result(property = "jibun", column = "jibun"),
      @Result(property = "roadNmSggCd", column = "road_nm_sgg_cd"),
      @Result(property = "roadNm", column = "road_nm"),
      @Result(property = "roadNmBonbun", column = "road_nm_bonbun"),
      @Result(property = "roadNmBubun", column = "road_nm_bubun"),
      @Result(property = "aptNm", column = "apt_nm"),
      @Result(property = "buildYear", column = "build_year"),
      @Result(property = "latitude", column = "latitude"),
      @Result(property = "longitude", column = "longitude")
  })
  List<HouseInfo> findApartmentsInBounds(
      @Param("swLat") double swLat,
      @Param("swLng") double swLng,
      @Param("neLat") double neLat,
      @Param("neLng") double neLng);

  @Select("SELECT d.dong_name, AVG(CAST(h.deal_amount AS UNSIGNED)) as avgPrice, " +
      "COUNT(*) as dealCount, d.lat, d.lng " +
      "FROM housedeals h " +
      "JOIN dongcodes d ON h.dong_code = d.dong_code " +
      "WHERE d.lat BETWEEN #{swLat} AND #{neLat} " +
      "AND d.lng BETWEEN #{swLng} AND #{neLng} " +
      "GROUP BY d.dong_name, d.lat, d.lng")
  List<Map<String, Object>> findDistrictAveragePrice(
      @Param("swLat") double swLat,
      @Param("swLng") double swLng,
      @Param("neLat") double neLat,
      @Param("neLng") double neLng);

  @Select("SELECT hi.apt_name, AVG(CAST(hd.deal_amount AS UNSIGNED)) as avgPrice, " +
      "COUNT(*) as dealCount, hi.latitude, hi.longitude, " +
      "MAX(CONCAT(hd.deal_year, hd.deal_month, hd.deal_day)) as latestDeal " +
      "FROM houseinfos hi " +
      "JOIN housedeals hd ON hi.apt_seq = hd.apt_seq " +
      "WHERE hi.latitude BETWEEN #{swLat} AND #{neLat} " +
      "AND hi.longitude BETWEEN #{swLng} AND #{neLng} " +
      "GROUP BY hi.apt_name, hi.latitude, hi.longitude")
  List<Map<String, Object>> findDetailedApartmentInfo(
      @Param("swLat") double swLat,
      @Param("swLng") double swLng,
      @Param("neLat") double neLat,
      @Param("neLng") double neLng);

  DongCode findDongCodeByName(@Param("dongName") String dongName);

  /**
   * 위도/경도 기반으로 주변 아파트 검색
   * 
   * @param lat    위도
   * @param lng    경도
   * @param radius 반경(미터)
   * @return 검색된 아파트 목록
   */
  @Select("SELECT * FROM houseinfos " +
      "WHERE (6371 * acos(cos(radians(#{lat})) * cos(radians(CAST(latitude AS DOUBLE))) * " +
      "cos(radians(CAST(longitude AS DOUBLE)) - radians(#{lng})) + " +
      "sin(radians(#{lat})) * sin(radians(CAST(latitude AS DOUBLE))))) <= #{radius/1000} " +
      "ORDER BY (6371 * acos(cos(radians(#{lat})) * cos(radians(CAST(latitude AS DOUBLE))) * " +
      "cos(radians(CAST(longitude AS DOUBLE)) - radians(#{lng})) + " +
      "sin(radians(#{lat})) * sin(radians(CAST(latitude AS DOUBLE))))) ASC")
  List<HouseInfo> findApartmentsByLocation(@Param("lat") double lat, @Param("lng") double lng,
      @Param("radius") int radius);

  /**
   * 특정 아파트의 거래 내역 조회
   * 
   * @param aptSeq 아파트 일련번호
   * @return 해당 아파트의 거래 내역 목록
   */
  @Select("SELECT * FROM housedeals WHERE apt_seq = #{aptSeq} ORDER BY deal_year DESC, deal_month DESC, deal_day DESC")
  List<HouseDeal> findDealsByAptSeq(@Param("aptSeq") String aptSeq);

  @Select("""
      WITH latest_deal_per_apt AS (
          SELECT
              hi.apt_seq,
              hi.sgg_cd,
              hi.umd_cd,
              hi.latitude,
              hi.longitude,
              hd.deal_year,
              hd.deal_month,
              hd.deal_day,
              hd.deal_amount,
              ROW_NUMBER() OVER (PARTITION BY hi.apt_seq ORDER BY hd.deal_year DESC, hd.deal_month DESC, hd.deal_day DESC) AS rn
          FROM houseinfos hi
          JOIN housedeals hd ON hi.apt_seq = hd.apt_seq
          WHERE CAST(hi.latitude AS DECIMAL(10,7)) BETWEEN #{swLat} AND #{neLat}
            AND CAST(hi.longitude AS DECIMAL(10,7)) BETWEEN #{swLng} AND #{neLng}
      )
      SELECT
          ${regionColumn} AS region_code,
          AVG(CAST(REPLACE(deal_amount, ',', '') AS UNSIGNED)) AS avg_latest_deal_price,
          COUNT(*) AS apt_count
      FROM latest_deal_per_apt
      WHERE rn = 1
      GROUP BY ${regionColumn}
      """)
  List<Map<String, Object>> findLatestDealPriceByRegion(
      @Param("swLat") double swLat,
      @Param("swLng") double swLng,
      @Param("neLat") double neLat,
      @Param("neLng") double neLng,
      @Param("regionColumn") String regionColumn);

  List<ApartmentWithLatestDeal> findApartmentsWithLatestDeal(
      @Param("addressKeyword") String addressKeyword,
      @Param("minPrice") Integer minPrice,
      @Param("maxPrice") Integer maxPrice,
      @Param("minArea") Double minArea,
      @Param("maxArea") Double maxArea);

  // 시도별 평균가격 조회 (dong_code_superman 테이블 활용)
  @Select("""
      SELECT
        dong_code,
        sido_name,
        lat,
        lng,
        avg_price,
        apt_count,
        updated_at
      FROM dong_code_superman
      WHERE lat BETWEEN #{swLat} AND #{neLat}
        AND lng BETWEEN #{swLng} AND #{neLng}
        AND avg_price IS NOT NULL
        AND RIGHT(dong_code, 8) = '00000000'
      ORDER BY sido_name
      """)
  List<Map<String, Object>> findSidoAvgPriceInBounds(
      @Param("swLat") double swLat,
      @Param("swLng") double swLng,
      @Param("neLat") double neLat,
      @Param("neLng") double neLng);

  // 구군별 평균가격 조회 (dong_code_superman 테이블 활용)
  @Select("""
      SELECT
        dong_code,
        sido_name,
        gugun_name,
        lat,
        lng,
        avg_price,
        apt_count,
        updated_at
      FROM dong_code_superman
      WHERE lat BETWEEN #{swLat} AND #{neLat}
        AND lng BETWEEN #{swLng} AND #{neLng}
        AND avg_price IS NOT NULL
        AND RIGHT(dong_code, 5) = '00000'
        AND RIGHT(dong_code, 8) != '00000000'
      ORDER BY sido_name, gugun_name
      """)
  List<Map<String, Object>> findGugunAvgPriceInBounds(
      @Param("swLat") double swLat,
      @Param("swLng") double swLng,
      @Param("neLat") double neLat,
      @Param("neLng") double neLng);

  // 기존 동별 쿼리 수정 (dong_code_superman 테이블 활용)
  @Select("""
      SELECT
        dong_code,
        sido_name,
        gugun_name,
        dong_name,
        lat,
        lng,
        avg_price,
        apt_count,
        updated_at
      FROM dong_code_superman
      WHERE lat BETWEEN #{swLat} AND #{neLat}
        AND lng BETWEEN #{swLng} AND #{neLng}
        AND avg_price IS NOT NULL
        AND RIGHT(dong_code, 5) != '00000'
      ORDER BY sido_name, gugun_name, dong_name
      """)
  List<Map<String, Object>> findDongAvgPriceInBounds(
      @Param("swLat") double swLat,
      @Param("swLng") double swLng,
      @Param("neLat") double neLat,
      @Param("neLng") double neLng);

  @Select("SELECT dong_code, sido_name, gugun_name, dong_name, " +
      "avg_price, apt_count, lat, lng, updated_at " +
      "FROM dong_code_superman WHERE dong_code = #{dongCode}")
  Map<String, Object> findAvgPriceByDongCode(@Param("dongCode") String dongCode);

  // 평균가격만 간단히 조회
  @Select("SELECT avg_price FROM dong_code_superman WHERE dong_code = #{dongCode}")
  Long findSimpleAvgPrice(@Param("dongCode") String dongCode);

  // 지도 영역 내 아파트 + 최신 거래 정보 반환
  List<ApartmentWithLatestDeal> findApartmentsWithLatestDealInBounds(
      @Param("swLat") double swLat,
      @Param("swLng") double swLng,
      @Param("neLat") double neLat,
      @Param("neLng") double neLng);

}
