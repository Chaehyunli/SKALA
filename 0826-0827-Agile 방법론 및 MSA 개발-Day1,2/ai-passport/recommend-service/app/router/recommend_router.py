import logging
from fastapi import APIRouter, Depends, Header, HTTPException, status
from app.config.security import verify_token
from app.model.schemas import PassportReuseResponse, RecommendResponse
from app.service.recommend_service import recommend_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/recommend", tags=["recommend"])


@router.get("/{user_id}", response_model=RecommendResponse)
async def get_recommendations(
    user_id: int,
    token_payload: dict = Depends(verify_token)
):
    """
    GET /recommend/{userId} - 사용자 기반 강의 추천

    추천 규칙:
    - 수강 이력 있음: 최빈 카테고리 기반 미수강 강의 추천 (수강생 수 기준 정렬)
    - 수강 이력 없음: 전체 인기 강의 추천
    """
    logger.info(f"[Router] 추천 요청 - userId: {user_id}")
    return await recommend_service.get_recommendations(user_id)


@router.get("/passport-reuse/{mission_id}", response_model=PassportReuseResponse)
async def check_passport_reuse(
    mission_id: int,
    x_user_id: int = Header(alias="X-User-Id"),
    _token_payload: dict = Depends(verify_token),
):
    """현재 사용자의 단일 ACTIVE Passport가 미션 권한을 모두 포함하는지 확인한다."""
    logger.info(
        "[Router] Passport 재사용 확인 - userId: %s, missionId: %s",
        x_user_id,
        mission_id,
    )
    try:
        return await recommend_service.check_passport_reuse(x_user_id, mission_id)
    except PermissionError as exc:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail=str(exc)) from exc


@router.get("/health", include_in_schema=False)
async def health_check():
    return {"status": "UP", "service": "recommend-service"}
