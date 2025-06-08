-- 1단계: 동별 평균가격 업데이트
UPDATE dong_code_superman d
JOIN (
    SELECT
        CONCAT(hi.sgg_cd, hi.umd_cd) AS dong_code,
        AVG(CAST(REPLACE(lh.deal_amount, ',', '') AS UNSIGNED)) AS avg_price,
        COUNT(*) AS apt_count
    FROM houseinfos hi
    JOIN latest_housedeals lh ON hi.apt_seq = lh.apt_seq
    WHERE lh.deal_amount IS NOT NULL AND lh.deal_amount != ''
    GROUP BY CONCAT(hi.sgg_cd, hi.umd_cd)
) t ON d.dong_code = t.dong_code
SET d.avg_price = t.avg_price,
    d.apt_count = t.apt_count;

-- 2단계: 군별 평균가격 업데이트
UPDATE dong_code_superman d
JOIN (
    SELECT
        CONCAT(gun_prefix, '00000') AS gun_dong_code,
        AVG(avg_price) AS avg_price,
        SUM(apt_count) AS apt_count
    FROM (
        SELECT
            SUBSTR(CONCAT(hi.sgg_cd, hi.umd_cd), 1, 5) AS gun_prefix,
            AVG(CAST(REPLACE(lh.deal_amount, ',', '') AS UNSIGNED)) AS avg_price,
            COUNT(*) AS apt_count
        FROM houseinfos hi
        JOIN latest_housedeals lh ON hi.apt_seq = lh.apt_seq
        WHERE lh.deal_amount IS NOT NULL AND lh.deal_amount != ''
        GROUP BY SUBSTR(CONCAT(hi.sgg_cd, hi.umd_cd), 1, 5)
    ) sub
    GROUP BY gun_prefix
) t ON d.dong_code = t.gun_dong_code
SET d.avg_price = t.avg_price,
    d.apt_count = t.apt_count;

-- 3단계: 광역시도별 평균가격 업데이트
UPDATE dong_code_superman d
JOIN (
    SELECT
        CONCAT(sido_prefix, '00000000') AS sido_dong_code,
        AVG(avg_price) AS avg_price,
        SUM(apt_count) AS apt_count
    FROM (
        SELECT
            SUBSTR(CONCAT(hi.sgg_cd, hi.umd_cd), 1, 2) AS sido_prefix,
            AVG(CAST(REPLACE(lh.deal_amount, ',', '') AS UNSIGNED)) AS avg_price,
            COUNT(*) AS apt_count
        FROM houseinfos hi
        JOIN latest_housedeals lh ON hi.apt_seq = lh.apt_seq
        WHERE lh.deal_amount IS NOT NULL AND lh.deal_amount != ''
        GROUP BY SUBSTR(CONCAT(hi.sgg_cd, hi.umd_cd), 1, 2)
    ) sub
    GROUP BY sido_prefix
) t ON d.dong_code = t.sido_dong_code
SET d.avg_price = t.avg_price,
    d.apt_count = t.apt_count;
