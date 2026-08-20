package com.skala.lab10;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 10장 미니 실습 — 점심 추천 도구 두 개.
 *
 * <p>모델이 보는 것은 {@code description} 뿐이다.
 * 설명을 "점심 추천" 네 글자로 줄이면 갑자기 도구를 안 부른다. 직접 해 볼 것.
 */
@Component
public class LunchTools {

    @Tool(description = """
          오늘의 점심 메뉴를 추천한다.
          '점심 뭐 먹지', '메뉴 추천해줘', '배고파' 같은 말에 사용한다.
          """)
    public String 점심추천(
            @ToolParam(description = "지금 기분이나 날씨. 예: 피곤, 더움") String 기분) {
        return switch (기분) {
            case "피곤" -> "국밥 (뜨끈하게 한 그릇)";
            case "더움" -> "냉면 (시원하게)";
            default -> "김치찌개 (무난하게)";
        };
    }

    @Tool(description = "지금 서울 날씨를 알려 준다.")
    public String 날씨() {
        return "맑음, 28도";
    }
}
