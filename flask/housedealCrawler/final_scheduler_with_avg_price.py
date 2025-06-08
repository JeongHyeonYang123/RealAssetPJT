# final_scheduler_with_avg_price.py
import schedule
import time
import logging
from datetime import datetime, timedelta
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import smtplib

from region_codes import RegionCodeManager
from corrected_xml_crawler import CorrectedXMLCrawler
from simple_disable_foreign_key import SimpleDisableForeignKeySaver
from avg_price_updater import AvgPriceUpdater  # ✅ 새로운 모듈 추가

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('house_scheduler.log', encoding='utf-8'),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger(__name__)


class FinalHouseSchedulerWithAvgPrice:
    """평균가격 업데이트 기능이 추가된 최종 스케줄러"""

    def __init__(self, api_key: str, db_config: dict, email_config: dict = None):
        self.api_key = api_key
        self.db_config = db_config
        self.email_config = email_config

        # 기존 컴포넌트
        self.crawler = CorrectedXMLCrawler(api_key)
        self.db_saver = SimpleDisableForeignKeySaver(db_config)
        self.region_manager = RegionCodeManager()

        # ✅ 새로운 평균가격 업데이터 추가
        self.avg_price_updater = AvgPriceUpdater(db_config)

        # 마지막 실행 추적
        self.last_monthly_run = None

        # 통계
        self.stats = {
            'total_runs': 0,
            'successful_runs': 0,
            'failed_runs': 0,
            'total_records': 0,
            'avg_price_updates': 0,  # ✅ 평균가격 업데이트 횟수 추가
            'last_success': None,
            'last_failure': None
        }

        logger.info("🚀 평균가격 업데이트 기능이 포함된 최종 스케줄러 초기화 완료")

    def startup_collection(self):
        """시작 시 즉시 수집 + 평균가격 업데이트"""
        logger.info("🎯 === 시작 시 즉시 수집 + 평균가격 업데이트 실행 ===")

        try:
            print("\n📊 수집 옵션 선택:")
            print("1. 빠른 테스트 (서울 3개 구)")
            print("2. 주요 도시 (12개 지역)")
            print("3. 전체 수집 (전국)")
            print("4. 평균가격 업데이트만")
            print("5. 건너뛰기")

            choice = input("선택하세요 (1-5) [기본값: 1]: ").strip() or "1"

            if choice == "5":
                logger.info("⏭️ 즉시 수집 건너뛰기")
                return
            elif choice == "4":
                logger.info("📊 평균가격 업데이트만 실행")
                return self._update_avg_prices_only()

            # 데이터 수집 실행
            result = self._execute_collection_by_choice(choice)

            if result and result['total_saved'] > 0:
                logger.info(f"✅ 데이터 수집 완료: {result['total_saved']}건")

                # ✅ 수집 완료 후 평균가격 업데이트 실행
                logger.info("📊 수집 완료 후 평균가격 업데이트 시작...")
                avg_result = self._update_avg_prices_after_collection()

                # 통합 알림
                self._send_notification(
                    "🎯 즉시 수집 + 평균가격 업데이트 완료",
                    f"데이터 수집: {result['total_saved']}건\n"
                    f"평균가격 업데이트: {avg_result.get('total_updated', 0)}개 지역"
                )

            return result

        except Exception as e:
            logger.error(f"❌ 즉시 수집 실패: {e}")
            self._send_notification("❌ 즉시 수집 실패", str(e))
            return None

    def _execute_collection_by_choice(self, choice):
        """선택에 따른 데이터 수집 실행"""
        target_date = datetime.now() - timedelta(days=60)
        target_month = target_date.strftime('%Y%m')

        if choice == "1":
            region_codes = ['11680', '11650', '11215']
            collection_type = "빠른테스트"
        elif choice == "2":
            region_codes = self.region_manager.get_major_cities_codes()
            collection_type = "주요도시"
        elif choice == "3":
            region_codes = self.region_manager.get_all_codes()
            collection_type = "전체수집"
        else:
            region_codes = ['11680', '11650', '11215']
            collection_type = "기본테스트"

        logger.info(f"🎯 {collection_type}: {len(region_codes)}개 지역 - {target_month}")
        return self._collect_regions(region_codes, target_month, collection_type)

    def _update_avg_prices_only(self):
        """평균가격 업데이트만 실행"""
        try:
            result = self.avg_price_updater.update_all_avg_prices()
            self.stats['avg_price_updates'] += 1

            self._send_notification(
                "📊 평균가격 업데이트 완료",
                f"총 업데이트: {result['total_updated']}개 지역\n"
                f"처리 시간: {result['elapsed_time']}"
            )

            return result

        except Exception as e:
            logger.error(f"❌ 평균가격 업데이트 실패: {e}")
            self._send_notification("❌ 평균가격 업데이트 실패", str(e))
            return None
        finally:
            self.avg_price_updater.close()

    def _update_avg_prices_after_collection(self):
        """데이터 수집 후 평균가격 업데이트"""
        try:
            logger.info("📊 === 수집 완료 후 평균가격 업데이트 ===")

            result = self.avg_price_updater.update_all_avg_prices()
            self.stats['avg_price_updates'] += 1

            logger.info(f"✅ 평균가격 업데이트 완료: {result['total_updated']}개 지역")
            return result

        except Exception as e:
            logger.error(f"❌ 평균가격 업데이트 실패: {e}")
            return {'total_updated': 0, 'error': str(e)}
        finally:
            self.avg_price_updater.close()

    def daily_auto_collection(self):
        """일일 자동 수집 + 평균가격 업데이트"""
        logger.info("🌅 === 일일 자동 수집 + 평균가격 업데이트 시작 ===")

        try:
            # 월초 체크
            now = datetime.now()
            if now.day == 1:
                if (self.last_monthly_run is None or
                        self.last_monthly_run.month != now.month):
                    logger.info("📅 월초 전체 수집 실행")
                    self.monthly_full_collection()
                    self.last_monthly_run = now
                    return

            # 일반 일일 수집
            target_date = datetime.now() - timedelta(days=60)
            target_month = target_date.strftime('%Y%m')
            major_regions = self.region_manager.get_major_cities_codes()

            # 데이터 수집
            collection_result = self._collect_regions(major_regions, target_month, "일일자동")

            # ✅ 수집 후 평균가격 업데이트
            avg_result = self._update_avg_prices_after_collection()

            self.stats['total_runs'] += 1
            self.stats['last_success'] = datetime.now()

            # 통합 알림
            self._send_notification(
                "✅ 일일 자동 수집 + 평균가격 업데이트 완료",
                f"데이터 수집: {collection_result['total_saved']}건\n"
                f"평균가격 업데이트: {avg_result.get('total_updated', 0)}개 지역\n"
                f"대상 월: {target_month}"
            )

        except Exception as e:
            logger.error(f"❌ 일일 자동 수집 실패: {e}")
            self.stats['failed_runs'] += 1
            self.stats['last_failure'] = datetime.now()
            self._send_notification("❌ 일일 자동 수집 실패", str(e))

    def weekly_sync_collection(self):
        """주간 재동기화 수집 + 평균가격 업데이트"""
        logger.info("📅 === 주간 재동기화 + 평균가격 업데이트 시작 ===")

        try:
            all_regions = self.region_manager.get_all_codes()
            total_saved = 0

            # 최근 3개월 재수집
            for i in range(3):
                target_date = datetime.now() - timedelta(days=30 * (i + 1))
                target_month = target_date.strftime('%Y%m')

                logger.info(f"📊 주간 동기화 {i + 1}/3: {target_month}")
                result = self._collect_regions(all_regions, target_month, f"주간-{i + 1}")
                total_saved += result['total_saved']

                if i < 2:
                    logger.info("⏳ 다음 월 처리를 위해 30분 대기...")
                    time.sleep(1800)

            # ✅ 전체 수집 완료 후 평균가격 업데이트
            avg_result = self._update_avg_prices_after_collection()

            # 통합 알림
            self._send_notification(
                "📅 주간 재동기화 + 평균가격 업데이트 완료",
                f"3개월 총 수집: {total_saved}건\n"
                f"평균가격 업데이트: {avg_result.get('total_updated', 0)}개 지역\n"
                f"처리 지역: {len(all_regions)}개"
            )

        except Exception as e:
            logger.error(f"❌ 주간 재동기화 실패: {e}")
            self._send_notification("❌ 주간 재동기화 실패", str(e))

    def monthly_full_collection(self):
        """월간 전체 수집 + 평균가격 업데이트"""
        logger.info("📊 === 월간 전체 수집 + 평균가격 업데이트 시작 ===")

        try:
            last_month = datetime.now().replace(day=1) - timedelta(days=1)
            target_month = last_month.strftime('%Y%m')
            all_regions = self.region_manager.get_all_codes()

            # 전체 수집
            collection_result = self._collect_regions(all_regions, target_month, "월간전체")

            # ✅ 전체 수집 완료 후 평균가격 업데이트
            avg_result = self._update_avg_prices_after_collection()

            success_rate = (collection_result['successful_regions'] / len(all_regions)) * 100

            # 통합 알림
            self._send_notification(
                "📊 월간 전체 수집 + 평균가격 업데이트 완료",
                f"대상 월: {target_month}\n"
                f"데이터 수집: {collection_result['total_saved']}건\n"
                f"평균가격 업데이트: {avg_result.get('total_updated', 0)}개 지역\n"
                f"성공률: {success_rate:.1f}%"
            )

        except Exception as e:
            logger.error(f"❌ 월간 전체 수집 실패: {e}")
            self._send_notification("❌ 월간 전체 수집 실패", str(e))

    def _collect_regions(self, region_codes: list, target_month: str, job_type: str):
        """지역별 데이터 수집 (기존과 동일)"""
        logger.info(f"🚀 {job_type} 수집 시작: {len(region_codes)}개 지역")

        total_saved = 0
        successful_regions = 0
        failed_regions = 0

        self.db_saver.connect()

        for i, region_code in enumerate(region_codes):
            try:
                if i % 10 == 0 or len(region_codes) <= 20:
                    logger.info(f"📊 {job_type} 진행: {i + 1}/{len(region_codes)} - {region_code}")

                deals_data = self.crawler.crawl_region_data_corrected(region_code, target_month)

                if deals_data:
                    saved_count = self.db_saver.save_house_deals_no_fk(deals_data, target_month, region_code)
                    total_saved += saved_count

                    if saved_count > 0:
                        successful_regions += 1
                        logger.info(f"✅ {region_code}: {len(deals_data)}건 → {saved_count}건 저장")
                    else:
                        successful_regions += 1
                else:
                    successful_regions += 1

                time.sleep(0.3)

            except Exception as e:
                failed_regions += 1
                logger.error(f"❌ {region_code} 처리 실패: {e}")
                continue

        self.db_saver.close()

        result = {
            'total_saved': total_saved,
            'successful_regions': successful_regions,
            'failed_regions': failed_regions
        }

        logger.info(f"✅ {job_type} 완료: {total_saved}건 저장, {successful_regions}개 지역 성공")
        self.stats['total_records'] += total_saved

        return result

    def _send_notification(self, subject: str, message: str):
        """이메일 알림 발송 (기존과 동일)"""
        if not self.email_config:
            logger.info(f"📧 알림: {subject} - {message}")
            return

        try:
            msg = MIMEMultipart()
            msg['From'] = self.email_config['from_email']
            msg['To'] = self.email_config['to_email']
            msg['Subject'] = f"[부동산크롤러] {subject}"

            body = f"""
부동산 실거래가 자동 수집 + 평균가격 업데이트 시스템 알림

{message}

실행 시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

=== 시스템 현황 ===
총 실행: {self.stats['total_runs']}회
성공: {self.stats['successful_runs']}회
실패: {self.stats['failed_runs']}회
총 수집: {self.stats['total_records']:,}건
평균가격 업데이트: {self.stats['avg_price_updates']}회

---
부동산 데이터 자동 수집 + 평균가격 업데이트 시스템
            """

            msg.attach(MimeText(body, 'plain', 'utf-8'))

            with smtplib.SMTP(self.email_config['smtp_server'], self.email_config['smtp_port']) as server:
                server.starttls()
                server.login(self.email_config['username'], self.email_config['password'])
                server.send_message(msg)

            logger.info(f"📧 알림 발송 완료: {subject}")

        except Exception as e:
            logger.error(f"❌ 알림 발송 실패: {e}")

    def setup_schedules(self):
        """정기 스케줄 설정"""
        logger.info("⏰ 정기 스케줄 설정 중...")

        # 매일 새벽 2시 - 일일 자동 수집 + 평균가격 업데이트
        schedule.every().day.at("02:00").do(self.daily_auto_collection)

        # 매주 일요일 새벽 1시 - 주간 재동기화 + 평균가격 업데이트
        schedule.every().sunday.at("01:00").do(self.weekly_sync_collection)

        # ✅ 매일 새벽 3시 - 평균가격만 업데이트 (추가 스케줄)
        schedule.every().day.at("03:00").do(self._update_avg_prices_only)

        logger.info("✅ 정기 스케줄 설정 완료:")
        logger.info("  📅 매일 02:00 - 일일 수집 + 평균가격 업데이트")
        logger.info("  📅 매일 03:00 - 평균가격만 업데이트")
        logger.info("  📅 매주 일요일 01:00 - 주간 재동기화 + 평균가격 업데이트")

    def start(self, run_startup=True):
        """스케줄러 시작"""
        logger.info("🚀 평균가격 업데이트 기능이 포함된 최종 시스템 시작!")

        if run_startup:
            self.startup_collection()

        self.setup_schedules()

        logger.info("\n" + "=" * 60)
        logger.info("🎉 시스템 가동 완료! 자동 수집 + 평균가격 업데이트 시작")
        logger.info("🔄 정기 스케줄 실행 중... (Ctrl+C로 중지)")
        logger.info("=" * 60)

        try:
            while True:
                schedule.run_pending()
                time.sleep(60)
        except KeyboardInterrupt:
            logger.info("👋 사용자에 의해 시스템 중지")
            self._send_notification("🛑 시스템 중지", "사용자 요청에 의해 중지됨")
        finally:
            # 정리 작업
            self.db_saver.close()
            self.avg_price_updater.close()


def main():
    """메인 실행 함수"""

    API_KEY = "Tr5R4h7B1ooVneI7RvqRT7e/gww2bCjIovGfpJvwiEC3MgNNDCdPOwkfrx9UbXLZZtJedgWAl4mXCQNcD0tVAA=="
    DB_CONFIG = {
        'host': 'localhost',
        'database': 'ssafyhome',
        'user': 'ssafy',
        'password': 'ssafy'
    }

    # 평균가격 업데이트 기능이 포함된 최종 스케줄러 시작
    scheduler = FinalHouseSchedulerWithAvgPrice(API_KEY, DB_CONFIG)
    scheduler.start()


if __name__ == "__main__":
    main()
