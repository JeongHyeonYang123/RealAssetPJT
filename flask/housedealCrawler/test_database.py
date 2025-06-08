# test_database.py
from database_saver import MonthlyDataSaver
import logging

logging.basicConfig(level=logging.INFO)


def test_database_operations():
    """데이터베이스 작업 테스트"""

    db_config = {
        'host': 'localhost',
        'database': 'ssafyhome',
        'user': 'ssafy',
        'password': 'ssafy'
    }

    saver = MonthlyDataSaver(db_config)

    try:
        # 1. 연결 테스트
        saver.connect()

        # 2. 테이블 생성 테스트
        saver.create_table_if_not_exists()

        # 3. 기존 데이터 확인
        count = saver.check_data_exists('202410')
        print(f"202410월 기존 데이터: {count}건")

        # 4. 통계 조회
        stats = saver.get_monthly_stats()
        print("월별 통계:")
        for stat in stats:
            print(f"  {stat['target_month']}: {stat['record_count']}건")

        print("✅ 데이터베이스 테스트 완료")

    except Exception as e:
        print(f"❌ 테스트 실패: {e}")
    finally:
        saver.disconnect()


if __name__ == "__main__":
    test_database_operations()
