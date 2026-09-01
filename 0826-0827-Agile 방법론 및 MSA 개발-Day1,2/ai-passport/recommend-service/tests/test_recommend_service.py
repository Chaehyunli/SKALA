from datetime import datetime, timedelta
from decimal import Decimal
from zoneinfo import ZoneInfo
from types import SimpleNamespace
from unittest import IsolatedAsyncioTestCase
from unittest.mock import AsyncMock, patch

from app.model.schemas import (
    ActivePassportResponse,
    AgentPermissions,
    CourseCategory,
    CourseResponse,
    MissionAnalysis,
    MissionResponse,
    PermissionResponse,
    ReuseDecision,
)
from app.service.recommend_service import RecommendService


class RecommendServiceTest(IsolatedAsyncioTestCase):
    now = datetime(2026, 8, 27, 12, 0, 0, tzinfo=ZoneInfo("Asia/Seoul"))

    async def check_reuse(self, mission, passports):
        course_client = SimpleNamespace(get_mission=AsyncMock(return_value=mission))
        enrollment_client = SimpleNamespace(
            get_active_passports=AsyncMock(return_value=passports)
        )
        with patch(
            "app.service.recommend_service.course_client",
            course_client,
        ), patch(
            "app.service.recommend_service.enrollment_client",
            enrollment_client,
        ):
            return await RecommendService().check_passport_reuse(2, 15, self.now)

    def mission(self, analysis):
        return MissionResponse(
            id=15,
            instructorId=2,
            usagePeriod="HOURS_24",
            analysis=analysis,
        )

    def passport(self, **changes):
        values = {
            "id": 42,
            "userId": 2,
            "status": "ACTIVE",
            "expiredAt": self.now + timedelta(hours=25),
            "agentList": [
                AgentPermissions(
                    agentCode="REVENUE_ANALYST",
                    permissions=[
                        PermissionResponse(code="CUSTOMER_READ"),
                        PermissionResponse(code="REVENUE_READ"),
                        PermissionResponse(code="MAIL_DRAFT_CREATE"),
                    ],
                )
            ],
        }
        values.update(changes)
        return ActivePassportResponse(**values)

    async def test_reuses_single_passport_that_fully_covers_required_permissions(self):
        analysis = MissionAnalysis(
            agentList=[
                AgentPermissions(
                    agentCode="REVENUE_ANALYST",
                    permissions=[
                        PermissionResponse(code="CUSTOMER_READ"),
                        PermissionResponse(code="REVENUE_READ"),
                    ],
                )
            ]
        )
        result = await self.check_reuse(self.mission(analysis), [self.passport()])

        self.assertEqual(ReuseDecision.REUSE, result.decision)
        self.assertTrue(result.reusable)
        self.assertEqual(42, result.passportId)
        self.assertEqual(self.now + timedelta(hours=24), result.requiredUntil)

    async def test_requires_every_agent_and_permission(self):
        analysis = MissionAnalysis(
            agentList=[
                AgentPermissions(
                    agentCode="REVENUE_ANALYST",
                    permissions=[PermissionResponse(code="CUSTOMER_READ")],
                ),
                AgentPermissions(
                    agentCode="DOCUMENT_ASSISTANT",
                    permissions=[PermissionResponse(code="DOCUMENT_READ")],
                ),
            ]
        )
        result = await self.check_reuse(self.mission(analysis), [self.passport()])

        self.assertEqual(ReuseDecision.NEW_ISSUE, result.decision)
        self.assertFalse(result.reusable)

    async def test_rejects_wrong_user_inactive_or_short_lived_passports(self):
        analysis = MissionAnalysis(
            agentList=[
                AgentPermissions(
                    agentCode="REVENUE_ANALYST",
                    permissions=[PermissionResponse(code="CUSTOMER_READ")],
                )
            ]
        )
        candidates = [
            self.passport(id=1, userId=3),
            self.passport(id=2, status="EXPIRED"),
            self.passport(id=3, expiredAt=self.now + timedelta(hours=23)),
            self.passport(id=4, expiredAt=None),
        ]
        result = await self.check_reuse(self.mission(analysis), candidates)

        self.assertEqual(ReuseDecision.NEW_ISSUE, result.decision)
        self.assertFalse(result.reusable)

    async def test_rejects_another_users_mission(self):
        mission = self.mission(
            MissionAnalysis(
                agentList=[
                    AgentPermissions(
                        agentCode="REVENUE_ANALYST",
                        permissions=[PermissionResponse(code="CUSTOMER_READ")],
                    )
                ]
            )
        ).model_copy(update={"instructorId": 3})

        with self.assertRaises(PermissionError):
            await self.check_reuse(mission, [self.passport()])

    async def test_does_not_combine_permissions_from_multiple_passports(self):
        analysis = MissionAnalysis(
            agentList=[
                AgentPermissions(
                    agentCode="REVENUE_ANALYST",
                    permissions=[
                        PermissionResponse(code="CUSTOMER_READ"),
                        PermissionResponse(code="REVENUE_READ"),
                    ],
                )
            ]
        )
        passports = [
            self.passport(
                id=1,
                agentList=[
                    AgentPermissions(
                        agentCode="REVENUE_ANALYST",
                        permissions=[PermissionResponse(code="CUSTOMER_READ")],
                    )
                ],
            ),
            self.passport(
                id=2,
                agentList=[
                    AgentPermissions(
                        agentCode="REVENUE_ANALYST",
                        permissions=[PermissionResponse(code="REVENUE_READ")],
                    )
                ],
            ),
        ]

        result = await self.check_reuse(self.mission(analysis), passports)

        self.assertEqual(ReuseDecision.NEW_ISSUE, result.decision)
        self.assertFalse(result.reusable)

    async def test_naive_expired_at_is_interpreted_in_app_timezone(self):
        # enrollment-service는 만료 시각을 오프셋 없이 내려준다.
        # now=2026-08-27 12:00 KST, HOURS_24 → required_until=2026-08-28 12:00 KST
        analysis = MissionAnalysis(
            agentList=[
                AgentPermissions(
                    agentCode="REVENUE_ANALYST",
                    permissions=[PermissionResponse(code="CUSTOMER_READ")],
                )
            ]
        )
        expires_before = datetime(2026, 8, 28, 11, 0, 0)  # naive == 11:00 KST → 부족
        expires_after = datetime(2026, 8, 28, 13, 0, 0)  # naive == 13:00 KST → 충족

        rejected = await self.check_reuse(
            self.mission(analysis), [self.passport(expiredAt=expires_before)]
        )
        self.assertEqual(ReuseDecision.NEW_ISSUE, rejected.decision)

        accepted = await self.check_reuse(
            self.mission(analysis), [self.passport(expiredAt=expires_after)]
        )
        self.assertEqual(ReuseDecision.REUSE, accepted.decision)

    async def test_returns_new_issue_when_analysis_is_missing(self):
        result = await self.check_reuse(self.mission(None), [self.passport()])

        self.assertEqual(ReuseDecision.NEW_ISSUE, result.decision)
        self.assertFalse(result.reusable)

    async def test_keeps_existing_recommendation_for_new_user(self):
        popular_course = CourseResponse(
            id=7,
            title="기존 추천 강의",
            category=CourseCategory.BACKEND,
            price=Decimal("1000"),
            instructorId=3,
            enrollmentCount=12,
            status="ACTIVE",
        )
        course_client = SimpleNamespace(
            get_all_courses=AsyncMock(return_value=[popular_course])
        )
        enrollment_client = SimpleNamespace(
            get_enrollment_history=AsyncMock(
                return_value=SimpleNamespace(activeCourseIds=[])
            )
        )
        with patch(
            "app.service.recommend_service.course_client",
            course_client,
        ), patch(
            "app.service.recommend_service.enrollment_client",
            enrollment_client,
        ):
            result = await RecommendService().get_recommendations(2)

        self.assertEqual(2, result.userId)
        self.assertEqual([popular_course], result.recommendedCourses)


if __name__ == "__main__":
    import unittest

    unittest.main()
