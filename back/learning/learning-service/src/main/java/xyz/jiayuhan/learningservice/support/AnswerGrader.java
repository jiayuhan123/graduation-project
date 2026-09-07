package xyz.jiayuhan.learningservice.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** 对小学计算题常见答案格式进行归一化比较。 */
@Component
@RequiredArgsConstructor
public class AnswerGrader {

    private final ObjectMapper objectMapper;

    public boolean isCorrect(String standardAnswerJson, String studentAnswer) {
        String expected = extractValue(standardAnswerJson);
        String actual = normalizeText(studentAnswer);
        if (isBooleanValue(expected)) {
            return normalizeBoolean(expected).equals(normalizeBoolean(actual));
        }
        BigDecimal expectedNumber = parseNumber(expected);
        BigDecimal actualNumber = parseNumber(actual);
        if (expectedNumber != null && actualNumber != null) {
            return expectedNumber.compareTo(actualNumber) == 0;
        }
        return normalizeText(expected).equalsIgnoreCase(actual);
    }

    public String extractValue(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode value = root == null ? null : root.get("value");
            if (value == null) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "答案缺少value字段");
            }
            return value.isString() ? value.asString() : value.toString();
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "答案JSON格式损坏");
        }
    }

    private BigDecimal parseNumber(String raw) {
        String value = normalizeText(raw).replace("％", "%");
        try {
            if (value.endsWith("%")) {
                return new BigDecimal(value.substring(0, value.length() - 1))
                        .divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP)
                        .stripTrailingZeros();
            }
            int mixedIndex = value.indexOf('又');
            if (mixedIndex > 0) {
                BigDecimal integer = new BigDecimal(value.substring(0, mixedIndex));
                BigDecimal fraction = parseFraction(value.substring(mixedIndex + 1));
                return fraction == null ? null : integer.add(fraction).stripTrailingZeros();
            }
            BigDecimal fraction = parseFraction(value);
            return fraction == null ? new BigDecimal(value).stripTrailingZeros() : fraction;
        } catch (ArithmeticException | NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal parseFraction(String value) {
        int slash = value.indexOf('/');
        if (slash <= 0 || slash == value.length() - 1 || value.indexOf('/', slash + 1) >= 0) {
            return null;
        }
        BigDecimal numerator = new BigDecimal(value.substring(0, slash));
        BigDecimal denominator = new BigDecimal(value.substring(slash + 1));
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, 12, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private boolean isBooleanValue(String value) {
        String normalized = normalizeBoolean(value);
        return "true".equals(normalized) || "false".equals(normalized);
    }

    private String normalizeBoolean(String value) {
        return switch (normalizeText(value).toLowerCase(Locale.ROOT)) {
            case "true", "对", "正确", "是", "√" -> "true";
            case "false", "错", "错误", "否", "×" -> "false";
            default -> normalizeText(value).toLowerCase(Locale.ROOT);
        };
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim()
                .replace(" ", "")
                .replace("　", "")
                .replace("，", ",")
                .replace("－", "-");
    }
}
