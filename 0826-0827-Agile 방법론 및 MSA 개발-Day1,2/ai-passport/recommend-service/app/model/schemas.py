from pydantic import BaseModel, Field
from typing import List, Optional
from enum import Enum
from decimal import Decimal
from datetime import datetime


class CourseCategory(str, Enum):
    BACKEND = "BACKEND"
    FRONTEND = "FRONTEND"
    DEVOPS = "DEVOPS"
    DATA_SCIENCE = "DATA_SCIENCE"
    MOBILE = "MOBILE"
    SECURITY = "SECURITY"
    DATABASE = "DATABASE"
    OTHER = "OTHER"
    # 기존 추천 응답이 업무 요청 카테고리도 읽을 수 있도록 현재 Course 계약을 함께 유지한다.
    CUSTOMER_ANALYSIS = "CUSTOMER_ANALYSIS"
    ANALYSIS_TASK = "ANALYSIS_TASK"
    DATA_LOOKUP = "DATA_LOOKUP"
    COMMUNICATION = "COMMUNICATION"
    DOCUMENT = "DOCUMENT"


class CourseResponse(BaseModel):
    id: int
    title: str
    description: Optional[str] = None
    category: CourseCategory
    price: Decimal
    instructorId: int
    enrollmentCount: int = 0
    status: str
    createdAt: Optional[datetime] = None


class EnrollmentHistoryResponse(BaseModel):
    userId: int
    activeCourseIds: List[int]


class RecommendResponse(BaseModel):
    userId: int
    recommendedCourses: List[CourseResponse]
    basedOnCategory: Optional[CourseCategory] = None
    message: str


class ApiResponse(BaseModel):
    success: bool
    message: str
    data: Optional[dict] = None


class PermissionResponse(BaseModel):
    code: str


class AgentPermissions(BaseModel):
    agentCode: str
    permissions: List[PermissionResponse] = Field(default_factory=list)


class MissionAnalysis(BaseModel):
    # 이전 단일 Agent 결과와 현재 다중 Agent 결과를 모두 비교 대상으로 읽는다.
    agentCode: Optional[str] = None
    permissions: List[PermissionResponse] = Field(default_factory=list)
    agentList: List[AgentPermissions] = Field(default_factory=list)


class MissionResponse(BaseModel):
    id: int
    instructorId: int
    usagePeriod: Optional[str] = None
    analysis: Optional[MissionAnalysis] = None


class ActivePassportResponse(BaseModel):
    id: int
    userId: int
    agentList: List[AgentPermissions] = Field(default_factory=list)
    status: str
    expiredAt: Optional[datetime] = None


class ReuseDecision(str, Enum):
    REUSE = "REUSE"
    NEW_ISSUE = "NEW_ISSUE"


class PassportReuseResponse(BaseModel):
    missionId: int
    decision: ReuseDecision
    reusable: bool
    passportId: Optional[int] = None
    requiredUntil: datetime
    message: str
