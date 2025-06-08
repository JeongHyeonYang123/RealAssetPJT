# fix_collation_issue.py
import pymysql
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class CollationFixer:
    """Collation 충돌 문제 해결 클래스"""

    def __init__(self, db_config):
        self.db_config = db_config
        self.connection = None
        self.target_collation = 'utf8mb4_unicode_ci'  # 통일할 collation

    def connect(self):
        try:
            self.connection = pymysql.connect(**self.db_config, charset='utf8mb4', autocommit=False)
            logger.info("✅ DB 연결 성공")
        except Exception as e:
            logger.error(f"❌ DB 연결 실패: {e}")
            raise

    def fix_database_collation(self):
        """데이터베이스 기본 collation 변경"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info(f"🔧 데이터베이스 collation을 {self.target_collation}로 변경")

            cursor.execute(f"ALTER DATABASE ssafyhome CHARACTER SET utf8mb4 COLLATE {self.target_collation}")
            self.connection.commit()

            logger.info("✅ 데이터베이스 collation 변경 완료")

        except Exception as e:
            logger.error(f"❌ 데이터베이스 collation 변경 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def fix_table_collations(self):
        """모든 테이블의 collation 통일"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            tables_to_fix = ['housedeals', 'houseinfos', 'dong_code_superman', 'latest_housedeals']

            for table in tables_to_fix:
                try:
                    logger.info(f"🔧 {table} 테이블 collation 변경 중...")

                    # 테이블 전체 collation 변경
                    cursor.execute(f"""
                        ALTER TABLE {table} 
                        CONVERT TO CHARACTER SET utf8mb4 
                        COLLATE {self.target_collation}
                    """)

                    logger.info(f"✅ {table} 테이블 collation 변경 완료")

                except Exception as e:
                    logger.error(f"❌ {table} 테이블 변경 실패: {e}")
                    continue

            self.connection.commit()
            logger.info("🎉 모든 테이블 collation 통일 완료")

        except Exception as e:
            logger.error(f"❌ 테이블 collation 변경 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def fix_specific_columns(self):
        """특정 컬럼들의 collation 개별 수정"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            # 주요 키 컬럼들 개별 수정
            column_fixes = [
                ("housedeals", "apt_seq", "VARCHAR(50)"),
                ("houseinfos", "apt_seq", "VARCHAR(50)"),
                ("houseinfos", "sgg_cd", "VARCHAR(10)"),
                ("houseinfos", "umd_cd", "VARCHAR(10)"),
                ("dong_code_superman", "dong_code", "VARCHAR(20)")
            ]

            for table, column, column_type in column_fixes:
                try:
                    logger.info(f"🔧 {table}.{column} 컬럼 collation 수정 중...")

                    cursor.execute(f"""
                        ALTER TABLE {table} 
                        MODIFY COLUMN {column} {column_type} 
                        CHARACTER SET utf8mb4 
                        COLLATE {self.target_collation}
                    """)

                    logger.info(f"✅ {table}.{column} 수정 완료")

                except Exception as e:
                    logger.error(f"❌ {table}.{column} 수정 실패: {e}")
                    continue

            self.connection.commit()
            logger.info("🎉 주요 컬럼 collation 수정 완료")

        except Exception as e:
            logger.error(f"❌ 컬럼 collation 수정 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def close(self):
        if self.connection:
            self.connection.close()


def fix_all_collations():
    """전체 collation 문제 해결"""
    DB_CONFIG = {
        'host': 'localhost',
        'database': 'ssafyhome',
        'user': 'ssafy',
        'password': 'ssafy'
    }

    fixer = CollationFixer(DB_CONFIG)

    try:
        fixer.connect()

        # 1단계: 데이터베이스 기본 collation 변경
        fixer.fix_database_collation()

        # 2단계: 모든 테이블 collation 통일
        fixer.fix_table_collations()

        # 3단계: 주요 컬럼 개별 수정 (필요시)
        # fixer.fix_specific_columns()

        logger.info("🎉 모든 collation 문제 해결 완료!")

    except Exception as e:
        logger.error(f"❌ collation 수정 실패: {e}")
    finally:
        fixer.close()


if __name__ == "__main__":
    fix_all_collations()
