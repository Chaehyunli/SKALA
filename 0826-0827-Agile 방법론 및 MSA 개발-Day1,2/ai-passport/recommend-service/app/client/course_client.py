import httpx
import logging
from typing import List
from app.config.settings import settings
from app.model.schemas import CourseResponse, CourseCategory, MissionResponse

logger = logging.getLogger(__name__)


class CourseServiceClient:
    """
    Course Service REST 클라이언트
    - 카테고리별 미수강 강의 목록 조회
    """

    def __init__(self):
        self.base_url = settings.course_service_url

    async def get_recommend_courses(
        self,
        category: CourseCategory,
        exclude_ids: List[int]
    ) -> List[CourseResponse]:
        """
        GET /courses/internal/recommend
        카테고리 기반 미수강 강의 목록 조회 (수강생 수 기준 정렬)
        """
        url = f"{self.base_url}/api/courses/internal/recommend"
        params = {"category": category.value}
        if exclude_ids:
            params["excludeIds"] = ",".join(str(i) for i in exclude_ids)

        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(url, params=params)
                response.raise_for_status()
                return [CourseResponse(**c) for c in response.json()]
        except httpx.HTTPError as e:
            logger.error(f"[CourseClient] 추천 강의 조회 실패 - category: {category}, error: {e}")
            return []

    async def get_all_courses(self) -> List[CourseResponse]:
        """
        GET /courses - 전체 강의 목록 조회
        수강 이력이 없는 신규 사용자 추천용
        """
        url = f"{self.base_url}/api/courses"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(url)
                response.raise_for_status()
                data = response.json()
                courses = data.get("data", [])
                return [CourseResponse(**c) for c in courses]
        except httpx.HTTPError as e:
            logger.error(f"[CourseClient] 전체 강의 조회 실패 - error: {e}")
            return []

    async def get_mission(self, mission_id: int) -> MissionResponse:
        """Passport 재사용 판정에 필요한 미션 분석 결과를 조회한다."""
        url = f"{self.base_url}/api/courses/internal/{mission_id}"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(url)
                response.raise_for_status()
                data = response.json()
                return MissionResponse(**data.get("data", data))
        except httpx.HTTPError as e:
            logger.error(
                f"[CourseClient] 미션 조회 실패 - missionId: {mission_id}, error: {e}"
            )
            # 재사용 불가와 하위 서비스 장애를 구분할 수 있도록 호출 실패는 상위로 전달한다.
            raise RuntimeError("Course Service 미션 조회에 실패했습니다.") from e


course_client = CourseServiceClient()
