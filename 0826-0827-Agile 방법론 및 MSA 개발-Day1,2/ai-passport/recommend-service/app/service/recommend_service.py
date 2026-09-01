import logging
from collections import Counter
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Set
from zoneinfo import ZoneInfo

from app.client.course_client import course_client
from app.client.enrollment_client import enrollment_client
from app.config.settings import settings
from app.model.schemas import (
    ActivePassportResponse,
    AgentPermissions,
    CourseCategory,
    CourseResponse,
    MissionAnalysis,
    PassportReuseResponse,
    RecommendResponse,
    ReuseDecision,
)

logger = logging.getLogger(__name__)

_APP_TZ = ZoneInfo(settings.app_timezone)


def _aware(dt: datetime) -> datetime:
    """
    naive datetime을 앱 표준 존으로 간주해 aware로 변환한다.

    ponytail: enrollment-service가 만료 시각을 오프셋 없이(호스트 JVM 벽시계)
    직렬화하므로 여기서 그 존으로 해석한다. 근본 해결은 enrollment 응답을
    ISO-8601 offset 포함으로 바꾸는 것.
    """
    return dt if dt.tzinfo is not None else dt.replace(tzinfo=_APP_TZ)


class RecommendService:
    """
    규칙 기반 강의 추천 서비스

    추천 규칙:
    1. 사용자의 수강 중인 강의 카테고리 분석
    2. 가장 많이 수강한 카테고리 선택 (최빈 카테고리)
    3. 해당 카테고리에서 미수강 강의 조회
    4. 수강생 수 기준 내림차순 정렬하여 반환
    5. 수강 이력 없으면 전체 강의 중 인기순 반환
    """

    MAX_RECOMMEND_COUNT = 5  # 최대 추천 강의 수

    async def get_recommendations(self, user_id: int) -> RecommendResponse:
        logger.info(f"[RecommendService] 추천 시작 - userId: {user_id}")

        # 1. 수강 이력 조회
        history = await enrollment_client.get_enrollment_history(user_id)
        active_course_ids = history.activeCourseIds

        # 2. 수강 이력 없는 신규 사용자 처리
        if not active_course_ids:
            return await self._recommend_for_new_user(user_id)

        # 3. 수강한 강의의 카테고리 분석 → 최빈 카테고리 선택
        dominant_category = await self._find_dominant_category(active_course_ids)
        if not dominant_category:
            return await self._recommend_for_new_user(user_id)

        # 4. 최빈 카테고리 기반 미수강 강의 조회
        recommended = await course_client.get_recommend_courses(
            category=dominant_category,
            exclude_ids=active_course_ids
        )

        # 5. 최대 추천 수 제한
        recommended = recommended[:self.MAX_RECOMMEND_COUNT]

        logger.info(f"[RecommendService] 추천 완료 - userId: {user_id}, "
                    f"category: {dominant_category}, count: {len(recommended)}")

        return RecommendResponse(
            userId=user_id,
            recommendedCourses=recommended,
            basedOnCategory=dominant_category,
            message=f"{dominant_category.value} 카테고리 기반 추천 강의입니다"
        )

    async def _find_dominant_category(
        self, course_ids: List[int]
    ) -> Optional[CourseCategory]:
        """
        수강한 강의들의 카테고리 분석 → 최빈 카테고리 반환
        Course Service에서 각 강의 정보를 조회하여 카테고리 집계
        """
        all_courses = await course_client.get_all_courses()
        course_map = {c.id: c for c in all_courses}

        categories = [
            course_map[cid].category
            for cid in course_ids
            if cid in course_map
        ]

        if not categories:
            return None

        # Counter로 최빈 카테고리 선택
        most_common = Counter(categories).most_common(1)
        return most_common[0][0] if most_common else None

    async def _recommend_for_new_user(self, user_id: int) -> RecommendResponse:
        """
        신규 사용자: 수강생 수 기준 전체 인기 강의 추천
        """
        logger.info(f"[RecommendService] 신규 사용자 추천 - userId: {user_id}")

        all_courses = await course_client.get_all_courses()
        popular = sorted(
            all_courses,
            key=lambda c: c.enrollmentCount,
            reverse=True
        )[:self.MAX_RECOMMEND_COUNT]

        return RecommendResponse(
            userId=user_id,
            recommendedCourses=popular,
            basedOnCategory=None,
            message="인기 강의 추천입니다"
        )

    async def check_passport_reuse(
        self,
        user_id: int,
        mission_id: int,
        now: datetime | None = None,
    ) -> PassportReuseResponse:
        mission = await course_client.get_mission(mission_id)
        if mission.instructorId != user_id:
            raise PermissionError("다른 사용자의 미션입니다.")

        checked_at = _aware(now) if now is not None else datetime.now(_APP_TZ)
        required_until = self._required_until(mission.usagePeriod, checked_at)
        required_permissions = self._required_permissions(mission.analysis)

        if not required_permissions or not any(required_permissions.values()):
            return self._new_issue(
                mission_id,
                required_until,
                "권한 설계 결과가 없어 기존 Passport와 비교할 수 없습니다.",
            )

        passports = await enrollment_client.get_active_passports(user_id)
        for passport in passports:
            if not self._is_valid_candidate(passport, user_id, required_until):
                continue
            if self._covers(passport.agentList, required_permissions):
                logger.info(
                    "[RecommendService] Passport 재사용 가능 - userId: %s, missionId: %s, passportId: %s",
                    user_id,
                    mission_id,
                    passport.id,
                )
                return PassportReuseResponse(
                    missionId=mission_id,
                    decision=ReuseDecision.REUSE,
                    reusable=True,
                    passportId=passport.id,
                    requiredUntil=required_until,
                    message="기존 Passport로 이 미션을 수행할 수 있습니다. 새 발급 요청이 필요하지 않습니다.",
                )

        return self._new_issue(
            mission_id,
            required_until,
            "필요한 Agent와 권한 및 유효기간을 모두 충족하는 Passport가 없습니다.",
        )

    def _required_until(self, usage_period: str | None, now: datetime) -> datetime:
        if usage_period == "HOURS_24":
            return now + timedelta(hours=24)
        if usage_period == "DAYS_7":
            return now + timedelta(days=7)
        return now

    def _required_permissions(
        self,
        analysis: MissionAnalysis | None,
    ) -> Dict[str, Set[str]]:
        if analysis is None:
            return {}

        agents = analysis.agentList
        if not agents and analysis.agentCode:
            agents = [
                AgentPermissions(
                    agentCode=analysis.agentCode,
                    permissions=analysis.permissions,
                )
            ]

        return self._permission_map(agents)

    def _permission_map(
        self,
        agents: List[AgentPermissions],
    ) -> Dict[str, Set[str]]:
        permission_map: Dict[str, Set[str]] = {}
        for agent in agents:
            permission_map.setdefault(agent.agentCode, set()).update(
                permission.code for permission in agent.permissions
            )
        return permission_map

    def _is_valid_candidate(
        self,
        passport: ActivePassportResponse,
        user_id: int,
        required_until: datetime,
    ) -> bool:
        return (
            passport.userId == user_id
            and passport.status == "ACTIVE"
            and passport.expiredAt is not None
            and _aware(passport.expiredAt) >= required_until
        )

    def _covers(
        self,
        agents: List[AgentPermissions],
        required_permissions: Dict[str, Set[str]],
    ) -> bool:
        passport_permissions = self._permission_map(agents)
        return all(
            required_codes.issubset(passport_permissions.get(agent_code, set()))
            for agent_code, required_codes in required_permissions.items()
        )

    def _new_issue(
        self,
        mission_id: int,
        required_until: datetime,
        message: str,
    ) -> PassportReuseResponse:
        return PassportReuseResponse(
            missionId=mission_id,
            decision=ReuseDecision.NEW_ISSUE,
            reusable=False,
            passportId=None,
            requiredUntil=required_until,
            message=message,
        )


recommend_service = RecommendService()
